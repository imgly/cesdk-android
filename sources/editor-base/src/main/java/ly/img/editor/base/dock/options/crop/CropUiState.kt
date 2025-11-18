package ly.img.editor.base.dock.options.crop

import ly.img.editor.core.R
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.ui.engine.getPage
import ly.img.editor.core.ui.engine.getScene
import ly.img.editor.core.ui.iconpack.FillModeCover
import ly.img.editor.core.ui.iconpack.FillModeCrop
import ly.img.editor.core.ui.iconpack.FillModeFit
import ly.img.editor.core.ui.iconpack.IconPack
import ly.img.editor.core.ui.library.CropAssetSourceType
import ly.img.engine.ContentFillMode
import ly.img.engine.DesignBlock
import ly.img.engine.DesignUnit
import ly.img.engine.Engine
import kotlin.math.roundToInt

suspend fun createAllPageResizeUiState(
    engine: Engine,
    pageAssetSourceIds: String?,
    initCropTranslationX: Float,
    initCropTranslationY: Float,
    selectedAssetKey: String? = null,
    allowContentFillMode: Boolean = true,
    allowResizeOption: Boolean = true,
) = createCropUiState(
    designBlock = engine.getScene(),
    engine = engine,
    cropMode = SheetType.Crop.Mode.ResizeAll,
    pageAssetSourceId = pageAssetSourceIds ?: CropAssetSourceType.Page.sourceId,
    cropAssetSourceId = null,
    initCropTranslationX = initCropTranslationX,
    initCropTranslationY = initCropTranslationY,
    selectedAssetKey = selectedAssetKey,
    allowContentFillMode = allowContentFillMode,
    allowResizeOption = allowResizeOption,
)

suspend fun createCropUiState(
    designBlock: DesignBlock,
    engine: Engine,
    initCropTranslationX: Float,
    initCropTranslationY: Float,
    cropScaleRatio: Float? = null,
    cropMode: SheetType.Crop.Mode,
    cropAssetSourceId: String?,
    pageAssetSourceId: String?,
    selectedAssetKey: String? = null,
    allowContentFillMode: Boolean = true,
    allowResizeOption: Boolean = true,
): CropUiState = CropUiState(
    straightenAngle = getStraightenDegrees(engine, designBlock),
    cropScaleRatio = cropScaleRatio ?: engine.block.getCropScaleRatio(designBlock),
    canResetCrop = canResetCrop(engine, designBlock, initCropTranslationX, initCropTranslationY),
    selectedAssetKey,
    contentFillMode = engine.block.getContentFillMode(designBlock),
    resizeState = ResizeUiState(
        width = engine.block.getWidth(engine.getPage(0)),
        height = engine.block.getHeight(engine.getPage(0)),
        dpi = engine.block.getFloat(engine.getScene(), SCENE_DPI).roundToInt(),
        pixelScaleFactor = engine.block.getFloat(engine.getScene(), SCENE_PIXEL_SCALE_FACTOR),
        unit = engine.scene.getDesignUnit().let { unit -> UNIT_ENTRIES.find { it.native == unit } ?: UNIT_ENTRIES.last() },
    ),
    cropMode = cropMode,
    groups = getGroups(
        engine,
        listOfNotNull(
            cropAssetSourceId.takeIf {
                cropMode.hasCropAsset
            },
            pageAssetSourceId.takeIf {
                cropMode.hasPageAsset
            },
        ),
    ),
    cropAssetSourceId = cropAssetSourceId,
    pageAssetSourceId = pageAssetSourceId,
    allowContentFillMode = allowContentFillMode,
    allowResizeOption = allowResizeOption,
)

suspend fun getGroups(
    engine: Engine,
    sourceIds: List<String>,
) = sourceIds.flatMap { sourceId ->
    (engine.asset.getGroups(sourceId) ?: emptyList()).map { groupId ->
        CropGroup(
            id = groupId,
            sourceId = sourceId,
        )
    }
}

private fun canResetCrop(
    engine: Engine,
    block: DesignBlock,
    initCropTranslationX: Float,
    initCropTranslationY: Float,
): Boolean = engine.block.getContentFillMode(block) != ContentFillMode.CROP ||
    getStraightenDegrees(engine, block) != 0f ||
    getRotationDegrees(engine, block) != 0f ||
    engine.block.getCropScaleX(block) < 1f ||
    engine.block.getCropScaleY(block) < 1f ||
    engine.block.getCropScaleRatio(block) != 1f ||
    engine.block.getCropTranslationX(block) != initCropTranslationX ||
    engine.block.getCropTranslationY(block) != initCropTranslationY

private const val SCENE_WIDTH = "scene/pageDimensions/width"
private const val SCENE_HEIGHT = "scene/pageDimensions/height"
private const val SCENE_DPI = "scene/dpi"
private const val SCENE_PIXEL_SCALE_FACTOR = "scene/pixelScaleFactor"

private val UNIT_ENTRIES: List<DesignUnitEntry> = listOf(
    DesignUnitEntry(
        titleRes = R.string.ly_img_editor_dialog_resize_unit_option_inch,
        native = DesignUnit.INCH,
        values = listOf(72f, 150f, 300f, 600f, 1200f, 2400f),
        unitSuffix = "inch",
        defaultValue = 300f,
        valueSuffix = " dpi",
        unitValueNameRes = R.string.ly_img_editor_dialog_resize_label_resolution,
    ),
    DesignUnitEntry(
        titleRes = R.string.ly_img_editor_dialog_resize_unit_option_millimeter,
        native = DesignUnit.MILLIMETER,
        values = listOf(72f, 150f, 300f, 600f, 1200f, 2400f),
        unitSuffix = "mm",
        defaultValue = 300f,
        valueSuffix = " dpi",
        unitValueNameRes = R.string.ly_img_editor_dialog_resize_label_resolution,
    ),
    DesignUnitEntry(
        titleRes = R.string.ly_img_editor_dialog_resize_unit_option_pixel,
        native = DesignUnit.PIXEL,
        unitSuffix = "px",
        values = listOf(0.5f, 1f, 1.5f, 2f, 3f, 4f),
        defaultValue = 1f,
        valueSuffix = "×",
        unitValueNameRes = R.string.ly_img_editor_dialog_resize_label_pixel_scale,
    ),
)

data class CropUiState(
    val straightenAngle: Float,
    val cropScaleRatio: Float,
    val canResetCrop: Boolean,
    val selectedAssetKey: String?,
    val contentFillMode: ContentFillMode,
    val groups: List<CropGroup>,
    val cropMode: SheetType.Crop.Mode,
    val cropAssetSourceId: String?,
    val pageAssetSourceId: String?,
    val resizeState: ResizeUiState,
    val allowContentFillMode: Boolean,
    val allowResizeOption: Boolean,
) {
    fun contentFillModeTextRes(mode: ContentFillMode) = when (mode) {
        ContentFillMode.CROP -> R.string.ly_img_editor_sheet_crop_fill_mode_option_crop
        ContentFillMode.COVER -> R.string.ly_img_editor_sheet_crop_fill_mode_option_cover
        ContentFillMode.CONTAIN -> R.string.ly_img_editor_sheet_crop_fill_mode_option_fit
    }

    fun contentFillModeIcon(mode: ContentFillMode) = when (mode) {
        ContentFillMode.CROP -> IconPack.FillModeCrop
        ContentFillMode.COVER -> IconPack.FillModeCover
        ContentFillMode.CONTAIN -> IconPack.FillModeFit
    }
}

data class ResizeUiState(
    val width: Float,
    val height: Float,
    val dpi: Int,
    val pixelScaleFactor: Float,
    val unit: DesignUnitEntry,
    val units: List<DesignUnitEntry> = UNIT_ENTRIES,
)

data class DesignUnitEntry(
    val titleRes: Int,
    val native: DesignUnit,
    val values: List<Float>,
    val unitSuffix: String,
    val defaultValue: Float,
    val valueSuffix: String = "",
    val unitValueNameRes: Int,
)

data class CropGroup(
    val id: String,
    val sourceId: String,
) {
    // Returns a camel case name for the group
    val name = id.split("_").joinToString(" ") { it.replaceFirstChar { char -> char.uppercase() } }
}
