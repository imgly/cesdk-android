package ly.img.editor.base.dock.options.colors

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import ly.img.editor.core.component.data.GradientFill
import ly.img.editor.core.component.data.SolidFill
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.ui.engine.getFill
import ly.img.editor.core.ui.engine.getStrokeColor
import ly.img.engine.DesignBlock
import ly.img.engine.Engine

@Immutable
data class ColorsUiState(
    val colorPalette: List<Color>,
    val items: List<Item>,
) {
    data class Item(
        val designBlock: DesignBlock,
        val name: String?,
        val selectedColor: Color,
    )

    companion object {
        fun create(
            colorPalette: List<Color>,
            sheetType: SheetType.Colors,
            engine: Engine,
        ) = ColorsUiState(
            colorPalette = colorPalette,
            items = sheetType.designBlocks.mapNotNull { designBlock ->
                val selectedColor = if (engine.block.supportsFill(designBlock) && engine.block.isFillEnabled(designBlock)) {
                    when (val fillInfo = engine.getFill(designBlock)) {
                        is SolidFill, is GradientFill -> fillInfo.mainColor
                        else -> null
                    }
                } else if (engine.block.supportsStroke(designBlock) && engine.block.isStrokeEnabled(designBlock)) {
                    engine.getStrokeColor(designBlock)
                } else {
                    null
                }
                Item(
                    selectedColor = selectedColor ?: return@mapNotNull null,
                    designBlock = designBlock,
                    name = engine.block.getName(designBlock).takeIf { it.isNotEmpty() },
                )
            },
        )
    }
}
