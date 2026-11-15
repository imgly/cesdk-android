package ly.img.editor.base.dock.options.fillstroke

import androidx.compose.ui.graphics.Color
import ly.img.editor.base.engine.abbreviation
import ly.img.editor.base.engine.lengthDecimals
import ly.img.editor.base.ui.Block
import ly.img.editor.core.component.data.ConicalGradientFill
import ly.img.editor.core.component.data.GradientFill
import ly.img.editor.core.component.data.LinearGradientFill
import ly.img.editor.core.component.data.RadialGradientFill
import ly.img.editor.core.ui.engine.BlockType
import ly.img.editor.core.ui.engine.canPaintStrokeWithFill
import ly.img.editor.core.ui.engine.getStrokeColor
import ly.img.editor.core.ui.engine.getStrokeGradientFill
import ly.img.engine.Engine
import ly.img.engine.FillType
import ly.img.engine.StrokeCornerGeometry
import ly.img.engine.StrokePosition
import ly.img.engine.StrokeStyle

data class StrokeUiState(
    val colorPalette: List<Color>,
    val isStrokeEnabled: Boolean,
    val strokeColor: Color,
    // Whether a gradient can paint the stroke, so the stroke offers the fill types.
    val supportFillTypes: Boolean,
    // The type of the stroke paint, or null when the stroke is disabled.
    val fillType: FillType?,
    // The gradient that paints the stroke, or null when the stroke color paints it.
    val gradientFill: GradientFill?,
    // The width in the scene's design unit.
    val strokeWidth: Float,
    // The block's longer side, or the width when that is larger, so a loaded width is not cut to fit.
    val strokeWidthMax: Float,
    val strokeWidthUnit: String,
    val strokeWidthDecimals: Int?,
    val strokeStyle: StrokeStyle,
    val isStrokePositionEnabled: Boolean,
    val isStrokeJointEnabled: Boolean = true,
    // Position and Join pickers don't apply to a 1-D primitive.
    val showPositionAndJoin: Boolean,
    val strokePosition: StrokePosition,
    val strokeJoin: StrokeCornerGeometry,
)

internal fun createStrokeUiState(
    block: Block,
    engine: Engine,
    colorPalette: List<Color>,
): StrokeUiState {
    val designBlock = block.designBlock
    val isEnabled = engine.block.isStrokeEnabled(designBlock)
    val isLineOrigin = engine.block.isLineOrigin(designBlock)
    val gradientFill = engine.getStrokeGradientFill(designBlock)
    val fillType = when (gradientFill) {
        is LinearGradientFill -> FillType.LinearGradient
        is RadialGradientFill -> FillType.RadialGradient
        is ConicalGradientFill -> FillType.ConicalGradient
        else -> FillType.Color
    }
    val strokeWidth = engine.block.getStrokeWidth(designBlock)
    val designUnit = engine.scene.getDesignUnit()
    return StrokeUiState(
        colorPalette = colorPalette,
        isStrokeEnabled = isEnabled,
        strokeColor = checkNotNull(engine.getStrokeColor(designBlock)),
        supportFillTypes = engine.block.canPaintStrokeWithFill(designBlock),
        fillType = fillType.takeIf { isEnabled },
        gradientFill = gradientFill,
        strokeWidth = strokeWidth,
        strokeWidthMax = maxOf(
            engine.block.getFrameWidth(designBlock),
            engine.block.getFrameHeight(designBlock),
            strokeWidth,
        ),
        strokeWidthUnit = designUnit.abbreviation,
        strokeWidthDecimals = designUnit.lengthDecimals,
        strokeStyle = engine.block.getStrokeStyle(designBlock),
        isStrokePositionEnabled = block.type != BlockType.Text &&
            block.type != BlockType.Caption &&
            block.type != BlockType.Page,
        isStrokeJointEnabled = block.type != BlockType.Page,
        showPositionAndJoin = !isLineOrigin,
        strokePosition = engine.block.getStrokePosition(designBlock),
        strokeJoin = engine.block.getStrokeCornerGeometry(designBlock),
    )
}
