package ly.img.editor.base.dock.options.fillstroke

import androidx.compose.ui.graphics.Color
import ly.img.editor.base.ui.Block
import ly.img.editor.core.ui.engine.BlockType
import ly.img.editor.core.ui.engine.getStrokeColor
import ly.img.engine.Engine
import ly.img.engine.StrokeCornerGeometry
import ly.img.engine.StrokePosition
import ly.img.engine.StrokeStyle
import kotlin.math.ln

data class StrokeUiState(
    val colorPalette: List<Color>,
    val isStrokeEnabled: Boolean,
    val strokeColor: Color,
    val strokeWidth: Float,
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
    return StrokeUiState(
        colorPalette = colorPalette,
        isStrokeEnabled = isEnabled,
        strokeColor = checkNotNull(engine.getStrokeColor(designBlock)),
        strokeWidth = engine.block.getStrokeWidth(designBlock).takeIf {
            it > 0
        }?.let { ln(it) } ?: STROKE_WIDTH_LOWER_BOUND,
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

const val STROKE_WIDTH_UPPER_BOUND = 3f
const val STROKE_WIDTH_LOWER_BOUND = -3f
