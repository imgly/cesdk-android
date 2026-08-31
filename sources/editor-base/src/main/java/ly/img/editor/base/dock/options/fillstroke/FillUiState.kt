package ly.img.editor.base.dock.options.fillstroke

import androidx.compose.ui.graphics.Color
import ly.img.editor.base.engine.effectiveTextRange
import ly.img.editor.base.ui.Block
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
    val fillType: FillType?,
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
    val isTextLike = block.type == BlockType.Text || block.type == BlockType.Caption
    val supportFillTypes = !isTextLike
    val fillState = if (isTextLike && fillType == FillType.Color) {
        textRangeFill(designBlock, engine)
    } else {
        checkNotNull(engine.getFill(designBlock))
    }
    return FillUiState(
        colorPalette = colorPalette,
        isFillEnabled = isEnabled,
        supportFillTypes = supportFillTypes,
        fillState = fillState,
        fillType = fillType.takeIf { isEnabled },
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
