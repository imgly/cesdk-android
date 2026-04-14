package ly.img.editor

import android.util.Log
import ly.img.editor.core.EditorScope
import ly.img.editor.core.UnstableEditorApi
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.ui.engine.Scope
import ly.img.editor.core.ui.engine.overrideAndRestoreAsync
import ly.img.editor.core.ui.library.CropAssetSourceType
import ly.img.engine.Asset
import ly.img.engine.AssetTransformPreset
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.DesignUnit
import kotlin.math.abs

internal const val PAGE_BLOCK_TYPE = "//ly.img.ubq/page"
internal const val ALLOW_RESIZE_INTERACTION_KEY = "page/allowResizeInteraction"
internal const val RESTRICT_RESIZE_INTERACTION_KEY = "page/restrictResizeInteractionToFixedAspectRatio"
private const val SCENE_DPI_KEY = "scene/dpi"
private const val SCENE_PIXEL_SCALE_FACTOR_KEY = "scene/pixelScaleFactor"
private const val DEFAULT_IF_NEEDED_THRESHOLD = 0.0001f
private const val MILLIMETERS_PER_INCH = 25.4f
private const val FORCE_CROP_LOG_TAG = "ForceCrop"

/**
 * Configuration for applying a force crop preset.
 *
 * @param sourceId ID of the asset source that contains the crop preset.
 * @param presetId ID of the crop preset that should be applied.
 * @param mode The mode that controls how the crop preset is applied.
 */
data class ForceCropConfiguration(
    val sourceId: String = "",
    val presetId: String = "",
    val mode: ForceCropMode = ForceCropMode.Silent,
    val presetCandidates: List<ForceCropPresetCandidate> = emptyList(),
)

data class ForceCropPresetCandidate(
    val sourceId: String,
    val presetId: String,
)

data class ForceCropResult(
    val candidate: ForceCropPresetCandidate,
    val applied: Boolean,
    val transformPreset: AssetTransformPreset?,
)

/**
 * Defines how the force crop preset should be applied.
 */
sealed class ForceCropMode {
    /** Applies the preset silently without opening the crop UI. */
    data object Silent : ForceCropMode()

    /** Applies the preset and always opens the crop UI afterwards. */
    data object Always : ForceCropMode()

    /**
     * Applies the preset only if needed. When applied the crop UI is opened.
     *
     * @param threshold The allowed difference when comparing the current dimensions with the preset.
     */
    data class IfNeeded(
        val threshold: Float = DEFAULT_IF_NEEDED_THRESHOLD,
    ) : ForceCropMode()
}

/**
 * Applies a crop preset to the given [block] according to [configuration].
 */
@UnstableEditorApi
suspend fun EditorScope.applyForceCrop(
    block: DesignBlock,
    configuration: ForceCropConfiguration,
): ForceCropResult {
    val engine = editorContext.engine
    require(engine.block.isValid(block)) {
        "The provided design block is not valid."
    }
    require(engine.block.supportsCrop(block)) {
        "The provided design block does not support cropping."
    }
    val presetCandidates = buildList {
        if (configuration.sourceId.isNotBlank() && configuration.presetId.isNotBlank()) {
            add(ForceCropPresetCandidate(configuration.sourceId, configuration.presetId))
        }
        configuration.presetCandidates.forEach { candidate ->
            if (candidate.sourceId.isNotBlank() && candidate.presetId.isNotBlank()) {
                add(candidate)
            }
        }
    }.distinct()

    require(presetCandidates.isNotEmpty()) {
        "Force crop requires at least one preset candidate."
    }

    val availableSources = engine.asset.findAllSources().toSet()
    val resolvedPresets = presetCandidates.mapNotNull { candidate ->
        if (!availableSources.contains(candidate.sourceId)) {
            Log.w(FORCE_CROP_LOG_TAG, "Preset source ${candidate.sourceId} is not available. Skipping.")
            return@mapNotNull null
        }
        val asset = engine.asset.fetchAsset(candidate.sourceId, candidate.presetId)
        if (asset == null) {
            Log.w(
                FORCE_CROP_LOG_TAG,
                "Preset ${candidate.presetId} not found in ${candidate.sourceId}. Skipping.",
            )
            return@mapNotNull null
        }
        ForceCropResolvedPreset(candidate, asset)
    }

    require(resolvedPresets.isNotEmpty()) {
        "None of the requested crop presets are available."
    }

    val selectedPreset = resolvedPresets.bestMatch(engine, block)
    val preset = selectedPreset.asset

    val shouldApply = when (val mode = configuration.mode) {
        ForceCropMode.Silent, ForceCropMode.Always -> true
        is ForceCropMode.IfNeeded -> isPresetNeeded(engine, block, preset, mode.threshold)
    }

    if (!shouldApply) {
        return ForceCropResult(selectedPreset.candidate, applied = false, transformPreset = preset.payload.transformPreset)
    }

    val isPage = engine.block.getType(block) == PAGE_BLOCK_TYPE
    engine.withTemporaryPageResizeInteraction(enable = isPage) {
        engine.overrideAndRestoreAsync(block, Scope.LayerCrop, Scope.LayerResize, Scope.LayerMove) {
            engine.asset.applyAssetSourceAsset(selectedPreset.candidate.sourceId, preset, block)
        }
        Log.d(
            FORCE_CROP_LOG_TAG,
            "Applied preset ${selectedPreset.candidate.presetId} from ${selectedPreset.candidate.sourceId} in mode ${configuration.mode}",
        )
    }

    val shouldOpenSheet = when (configuration.mode) {
        ForceCropMode.Silent -> false
        ForceCropMode.Always -> true
        is ForceCropMode.IfNeeded -> true
    }

    if (shouldOpenSheet) {
        val prefersImageCropMode = selectedPreset.candidate.sourceId != CropAssetSourceType.Page.sourceId
        openCropSheet(block, disableSizeInputs = true, prefersImageCropMode = prefersImageCropMode)
    }

    return ForceCropResult(selectedPreset.candidate, applied = true, transformPreset = preset.payload.transformPreset)
}

private data class ForceCropResolvedPreset(
    val candidate: ForceCropPresetCandidate,
    val asset: Asset,
)

private fun List<ForceCropResolvedPreset>.bestMatch(
    engine: ly.img.engine.Engine,
    block: DesignBlock,
): ForceCropResolvedPreset {
    if (size == 1) {
        return first()
    }

    return minByOrNull { resolved ->
        calculateFitScore(engine, block, resolved.asset)
    } ?: first()
}

private fun calculateFitScore(
    engine: ly.img.engine.Engine,
    block: DesignBlock,
    preset: Asset,
): Float {
    val transformPreset = preset.payload.transformPreset ?: return Float.MAX_VALUE

    val frameWidth = engine.block.getFrameWidth(block)
    val frameHeight = engine.block.getFrameHeight(block)

    return when (transformPreset) {
        is AssetTransformPreset.FreeAspectRatio -> Float.MAX_VALUE
        is AssetTransformPreset.FixedAspectRatio -> {
            val frameAspectRatio = frameHeight / frameWidth
            val presetAspectRatio = transformPreset.height / transformPreset.width
            abs(frameAspectRatio - presetAspectRatio)
        }
        is AssetTransformPreset.FixedSize -> {
            val harmonized = harmonizeDimensions(engine, frameWidth, frameHeight, transformPreset.designUnit)
            abs(harmonized.width - transformPreset.width) + abs(harmonized.height - transformPreset.height)
        }
        is AssetTransformPreset.Representation -> Float.MAX_VALUE
    }
}

private suspend fun EditorScope.openCropSheet(
    block: DesignBlock,
    disableSizeInputs: Boolean,
    prefersImageCropMode: Boolean,
) {
    val engine = editorContext.engine
    val blockType = engine.block.getType(block)
    val cropMode = when (blockType) {
        DesignBlockType.Page.key -> if (prefersImageCropMode) {
            SheetType.Crop.Mode.ImageCrop
        } else {
            SheetType.Crop.Mode.PageCrop
        }
        else -> SheetType.Crop.Mode.Element
    }

    val previousResizeOption = cropMode.hasResizeOption
    if (disableSizeInputs) {
        cropMode.hasResizeOption = false
    }

    try {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Crop(mode = cropMode)))
    } finally {
        if (disableSizeInputs) {
            cropMode.hasResizeOption = previousResizeOption
        }
    }
}

private fun isPresetNeeded(
    engine: ly.img.engine.Engine,
    block: DesignBlock,
    preset: Asset,
    threshold: Float,
): Boolean {
    val transformPreset = preset.payload.transformPreset as? AssetTransformPreset
        ?: error("The selected preset does not define a transform preset.")

    val frameWidth = engine.block.getFrameWidth(block)
    val frameHeight = engine.block.getFrameHeight(block)

    return when (transformPreset) {
        is AssetTransformPreset.FreeAspectRatio -> true
        is AssetTransformPreset.FixedAspectRatio -> {
            val frameAspectRatio = frameHeight / frameWidth
            val presetAspectRatio = transformPreset.height / transformPreset.width
            !almostEqual(frameAspectRatio, presetAspectRatio, threshold)
        }
        is AssetTransformPreset.FixedSize -> {
            val harmonized = harmonizeDimensions(engine, frameWidth, frameHeight, transformPreset.designUnit)
            if (
                !almostEqual(harmonized.width, transformPreset.width, threshold) ||
                !almostEqual(harmonized.height, transformPreset.height, threshold)
            ) {
                engine.scene.setDesignUnit(transformPreset.designUnit)
                true
            } else {
                false
            }
        }
        is AssetTransformPreset.Representation -> error("The selected preset does not have a valid transform preset.")
    }
}

private data class Dimensions(
    val width: Float,
    val height: Float,
)

private fun harmonizeDimensions(
    engine: ly.img.engine.Engine,
    width: Float,
    height: Float,
    targetUnit: DesignUnit,
): Dimensions {
    val scene = engine.scene.get() ?: return Dimensions(width, height)
    val currentUnit = engine.scene.getDesignUnit()

    if (currentUnit == targetUnit) {
        return Dimensions(width, height)
    }

    val dpi = engine.block.getFloat(scene, SCENE_DPI_KEY)
    val pixelScale = engine.block.getFloat(scene, SCENE_PIXEL_SCALE_FACTOR_KEY)

    val convertedWidth = convertUnit(currentUnit, targetUnit, dpi, pixelScale, width)
    val convertedHeight = convertUnit(currentUnit, targetUnit, dpi, pixelScale, height)

    return Dimensions(convertedWidth, convertedHeight)
}

private fun convertUnit(
    from: DesignUnit,
    to: DesignUnit,
    dpi: Float,
    pixelScale: Float,
    value: Float,
): Float {
    val valueInInches = when (from) {
        DesignUnit.INCH -> value
        DesignUnit.MILLIMETER -> value / MILLIMETERS_PER_INCH
        DesignUnit.PIXEL -> value / (dpi * pixelScale)
    }

    return when (to) {
        DesignUnit.INCH -> valueInInches
        DesignUnit.MILLIMETER -> valueInInches * MILLIMETERS_PER_INCH
        DesignUnit.PIXEL -> valueInInches * dpi * pixelScale
    }
}

private fun almostEqual(
    a: Float,
    b: Float,
    threshold: Float,
): Boolean = abs(a - b) <= threshold

internal inline fun <T> ly.img.engine.Engine.withTemporaryPageResizeInteraction(
    enable: Boolean,
    action: () -> T,
): T {
    if (!enable) {
        return action()
    }
    val previousAllowResize = editor.getSettingBoolean(ALLOW_RESIZE_INTERACTION_KEY)
    val previousRestrictResize = editor.getSettingBoolean(RESTRICT_RESIZE_INTERACTION_KEY)
    editor.setSettingBoolean(ALLOW_RESIZE_INTERACTION_KEY, true)
    // Temporarily lift the fixed-aspect restriction so free presets can apply; restored afterwards.
    editor.setSettingBoolean(RESTRICT_RESIZE_INTERACTION_KEY, false)
    return try {
        action()
    } finally {
        editor.setSettingBoolean(ALLOW_RESIZE_INTERACTION_KEY, previousAllowResize)
        editor.setSettingBoolean(RESTRICT_RESIZE_INTERACTION_KEY, previousRestrictResize)
    }
}
