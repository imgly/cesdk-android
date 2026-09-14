package ly.img.editor.base.dock.options.fillstroke

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import ly.img.editor.base.ui.Block
import ly.img.editor.core.R
import ly.img.editor.core.ui.engine.Scope
import ly.img.editor.core.ui.engine.getFillType
import ly.img.engine.Engine
import ly.img.engine.FillType

data class FillStrokeUiState(
    @StringRes val titleRes: Int,
    val fillUiState: FillUiState?,
    val strokeUiState: StrokeUiState?,
) {
    companion object Factory {
        private fun getFillStrokeTitleRes(
            showFill: Boolean,
            showStroke: Boolean,
        ) = when {
            showFill && showStroke -> {
                R.string.ly_img_editor_sheet_fill_stroke_title_fill_stroke
            }

            showFill -> {
                R.string.ly_img_editor_sheet_fill_stroke_title_fill
            }

            showStroke -> {
                R.string.ly_img_editor_sheet_fill_stroke_title_stroke
            }

            else -> {
                throw IllegalArgumentException(
                    "getFillStrokeTitleRes() should not be called when showFill and showStroke both are false.",
                )
            }
        }

        fun create(
            block: Block,
            engine: Engine,
            colorPalette: List<Color>,
        ): FillStrokeUiState {
            val designBlock = block.designBlock
            val fillType = engine.block.getFillType(designBlock)
            val hasSolidOrGradientFill = fillType == FillType.Color ||
                fillType == FillType.LinearGradient ||
                fillType == FillType.RadialGradient ||
                fillType == FillType.ConicalGradient
            val showFill = hasSolidOrGradientFill && engine.block.isAllowedByScope(designBlock, Scope.FillChange)
            val showStroke = engine.block.supportsStroke(designBlock) && engine.block.isAllowedByScope(designBlock, Scope.StrokeChange)

            val palette = colorPalette.take(6)
            return FillStrokeUiState(
                titleRes = getFillStrokeTitleRes(showFill, showStroke),
                fillUiState = if (showFill) {
                    createFillUiState(block, engine, palette)
                } else {
                    null
                },
                strokeUiState = if (showStroke) createStrokeUiState(block, engine, palette) else null,
            )
        }
    }
}
