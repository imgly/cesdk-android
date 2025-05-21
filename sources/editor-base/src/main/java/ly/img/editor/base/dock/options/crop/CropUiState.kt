package ly.img.editor.base.dock.options.crop

import ly.img.engine.ContentFillMode
import ly.img.engine.DesignBlock
import ly.img.engine.Engine

data class CropUiState(
    val straightenAngle: Float,
    val cropScaleRatio: Float,
    val canResetCrop: Boolean,
)

internal fun createCropUiState(
    designBlock: DesignBlock,
    engine: Engine,
    initCropTranslationX: Float,
    initCropTranslationY: Float,
    cropScaleRatio: Float? = null,
): CropUiState = CropUiState(
    straightenAngle = getStraightenDegrees(engine, designBlock).toFloat(),
    cropScaleRatio = cropScaleRatio ?: engine.block.getCropScaleRatio(designBlock),
    canResetCrop = canResetCrop(engine, designBlock, initCropTranslationX, initCropTranslationY),
)

private fun canResetCrop(
    engine: Engine,
    block: DesignBlock,
    initCropTranslationX: Float,
    initCropTranslationY: Float,
): Boolean = engine.block.getContentFillMode(block) != ContentFillMode.CROP ||
    getStraightenDegrees(engine, block) != 0 ||
    getRotationDegrees(engine, block) != 0 ||
    engine.block.getCropScaleX(block) < 1f ||
    engine.block.getCropScaleY(block) < 1f ||
    engine.block.getCropScaleRatio(block) != 1f ||
    engine.block.getCropTranslationX(block) != initCropTranslationX ||
    engine.block.getCropTranslationY(block) != initCropTranslationY
