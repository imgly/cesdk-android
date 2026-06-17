package ly.img.editor.base.dock.options.fillstroke

import androidx.compose.ui.graphics.Color
import ly.img.editor.base.engine.effectiveTextRange
import ly.img.editor.base.ui.Block
import ly.img.editor.core.R
import ly.img.editor.core.component.data.Fill
import ly.img.editor.core.component.data.SolidFill
import ly.img.editor.core.ui.engine.BlockType
import ly.img.editor.core.ui.engine.getFill
import ly.img.editor.core.ui.engine.getFillType
import ly.img.editor.core.ui.engine.toComposeColor
import ly.img.editor.core.ui.engine.toRGBColor
import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import ly.img.engine.FillType

data class FillUiState(
    val isFillEnabled: Boolean,
    val supportFillTypes: Boolean,
    val colorPalette: List<Color>,
    val fillTypeRes: Int,
    val fillState: Fill?,
)

internal fun createFillUiState(
    block: Block,
    engine: Engine,
    colorPalette: List<Color>,
): FillUiState {
    val designBlock = block.designBlock
    val isEnabled = engine.block.isFillEnabled(designBlock)
    val fillType = engine.block.getFillType(designBlock)
    val supportFillTypes = block.type != BlockType.Text
    val fillState = if (block.type == BlockType.Text && fillType == FillType.Color) {
        textRangeFill(designBlock, engine)
    } else {
        checkNotNull(engine.getFill(designBlock))
    }
    return FillUiState(
        colorPalette = colorPalette,
        isFillEnabled = isEnabled,
        supportFillTypes = supportFillTypes,
        fillState = fillState,
        fillTypeRes = getFillTypeRes(fillType.takeIf { isEnabled }),
    )
}

// getTextColors returns the ordered unique colours of the effective range, so a single-colour
// SolidFill means a uniform selection and several colours mean a mixed one.
private fun textRangeFill(
    designBlock: DesignBlock,
    engine: Engine,
): Fill {
    val colors = runCatching {
        val range = engine.block.effectiveTextRange(designBlock)
        engine.block.getTextColors(designBlock, range.first, range.last)
            .map { it.toRGBColor(engine).toComposeColor() }
    }.getOrDefault(emptyList())
    return colors.takeIf { it.isNotEmpty() }?.let { SolidFill(it) }
        ?: checkNotNull(engine.getFill(designBlock))
}

private fun getFillTypeRes(fillType: FillType?): Int = if (fillType == null) {
    R.string.ly_img_editor_sheet_fill_stroke_type_option_none
} else {
    when (fillType) {
        FillType.Color -> R.string.ly_img_editor_sheet_fill_stroke_type_option_solid
        FillType.RadialGradient -> R.string.ly_img_editor_sheet_fill_stroke_type_option_gradient_radial
        FillType.LinearGradient -> R.string.ly_img_editor_sheet_fill_stroke_type_option_gradient_linear
        FillType.ConicalGradient -> R.string.ly_img_editor_sheet_fill_stroke_type_option_gradient_conical
        else -> throw IllegalArgumentException("Unknown fill type: $fillType")
    }
}
