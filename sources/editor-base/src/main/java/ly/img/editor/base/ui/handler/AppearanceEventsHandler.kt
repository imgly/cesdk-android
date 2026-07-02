package ly.img.editor.base.ui.handler

import ly.img.editor.base.engine.EffectGroup
import ly.img.editor.base.engine.getEffectOrCreateAndAppend
import ly.img.editor.base.engine.getGroup
import ly.img.editor.base.engine.removeEffectByType
import ly.img.editor.base.engine.setBlurType
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.core.library.LibraryCategory
import ly.img.editor.core.ui.EventsHandler
import ly.img.editor.core.ui.inject
import ly.img.editor.core.ui.library.AppearanceLibraryCategory
import ly.img.editor.core.ui.library.getMeta
import ly.img.editor.core.ui.library.getUri
import ly.img.editor.core.ui.library.state.WrappedAsset
import ly.img.editor.core.ui.register
import ly.img.engine.Asset
import ly.img.engine.BlockApi
import ly.img.engine.BlurType
import ly.img.engine.Color
import ly.img.engine.DesignBlock
import ly.img.engine.EffectType
import ly.img.engine.Engine
import kotlin.math.roundToInt

/**
 * Register all events related to appearance, like adding/removing filters, fx effects, blur effects etc.
 * @param engine Lambda returning the engine instance
 * @param block Lambda returning the block instance
 */
@Suppress("NAME_SHADOWING")
fun EventsHandler.appearanceEvents(
    engine: () -> Engine,
    block: () -> DesignBlock,
) {
    val engine by inject(engine)
    val block by inject(block)

    register<BlockEvent.OnReplaceEffect> {
        replaceEffect(
            engine = engine,
            block = block,
            wrappedAsset = it.wrappedAsset,
            libraryCategory = it.libraryCategory,
        )
        engine.editor.addUndoStep()
    }
}

private fun replaceEffect(
    engine: Engine,
    block: DesignBlock,
    wrappedAsset: WrappedAsset?,
    libraryCategory: LibraryCategory,
) {
    val asset = wrappedAsset?.asset
    when (libraryCategory) {
        AppearanceLibraryCategory.Filters -> {
            replaceFilter(
                blockApi = engine.block,
                block = block,
                asset = asset,
            )
        }
        AppearanceLibraryCategory.FxEffects -> {
            val effectType = asset?.getMeta("effectType")
            replaceFxEffect(
                blockApi = engine.block,
                block = block,
                effect = effectType?.let { EffectType.getOrNull(it) },
            )
        }
        AppearanceLibraryCategory.Blur -> {
            val blurType = asset?.getMeta("blurType")
            engine.block.setBlurType(
                designBlock = block,
                type = blurType?.let { BlurType.getOrNull(it) },
            )
        }
        else -> throw IllegalArgumentException("Unsupported library category: $libraryCategory")
    }
}

private fun replaceFxEffect(
    blockApi: BlockApi,
    block: DesignBlock,
    effect: EffectType?,
) {
    EffectType.values().forEach {
        if (effect != it && it.getGroup() == EffectGroup.FxEffect) {
            blockApi.removeEffectByType(block, it)
        }
    }

    if (effect != null) {
        blockApi.getEffectOrCreateAndAppend(block, effect)
    }
}

private fun replaceFilter(
    blockApi: BlockApi,
    block: DesignBlock,
    asset: Asset?,
) {
    // The merged `ly.img.filter` source contains both LUT and duotone assets. Duotone assets
    // carry `lightColor` / `darkColor` metadata — LUT assets don't.
    val isDuotone = asset?.getMeta("lightColor") != null && asset.getMeta("darkColor") != null
    val keepEffect = if (asset == null) {
        null
    } else if (isDuotone) {
        EffectType.DuoToneFilter
    } else {
        EffectType.LutFilter
    }

    EffectType.values().filter {
        it != keepEffect && it.getGroup() == EffectGroup.Filter
    }.forEach {
        blockApi.removeEffectByType(block, it)
    }

    asset ?: return

    if (isDuotone) {
        val path = EffectType.DuoToneFilter.key
        blockApi.getEffectOrCreateAndAppend(block, EffectType.DuoToneFilter).also { effect ->
            blockApi.setColor(
                effect,
                "$path/darkColor",
                Color.fromHex(asset.getMeta("darkColor")!!),
            )
            blockApi.setFloat(effect, "$path/intensity", asset.getMeta("intensity", "0").toFloat())
            blockApi.setColor(
                effect,
                "$path/lightColor",
                Color.fromHex(asset.getMeta("lightColor")!!),
            )
        }
    } else {
        val path = EffectType.LutFilter.key
        blockApi.getEffectOrCreateAndAppend(block, EffectType.LutFilter).also { effect ->
            val verticalTileCountMeta = asset.getMeta("verticalTileCount")?.toFloat()?.roundToInt()
            val horizontalTileCountMeta = asset.getMeta("horizontalTileCount")?.toFloat()?.roundToInt()

            val verticalTileCount = verticalTileCountMeta ?: horizontalTileCountMeta ?: 5
            val horizontalTileCount = horizontalTileCountMeta ?: verticalTileCountMeta ?: 5

            blockApi.setFloat(effect, "$path/intensity", asset.getMeta("intensity", "1").toFloat())
            blockApi.setString(effect, "$path/lutFileURI", asset.getUri())
            // Set the filterId immediately so the UI can highlight the selected filter
            // before the engine downloads the LUT file
            blockApi.setString(effect, "$path/filterId", asset.id)
            blockApi.setInt(effect, "$path/horizontalTileCount", horizontalTileCount)
            blockApi.setInt(effect, "$path/verticalTileCount", verticalTileCount)
        }
    }
}
