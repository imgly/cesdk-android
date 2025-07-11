package ly.img.editor.base.dock.options.postcard.colors

import androidx.compose.ui.graphics.Color
import ly.img.editor.core.component.data.GradientFill
import ly.img.editor.core.component.data.SolidFill
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.ui.engine.getFill
import ly.img.editor.core.ui.engine.getStrokeColor
import ly.img.engine.Engine

data class PostcardColorsUiState(
    val colorPalette: List<Color>,
    val colorMapping: Map<String, Color>,
) {
    companion object {
        fun create(
            engine: Engine,
            colorPalette: List<Color>,
            sheetType: SheetType.PostcardColors,
        ) = PostcardColorsUiState(
            colorPalette = colorPalette,
            colorMapping = sheetType.colorMapping.mapValues { (name, color) ->
                val block = engine.block.findByName(name).firstOrNull() ?: return@mapValues color
                val updatedColor = if (engine.block.supportsFill(block) && engine.block.isFillEnabled(block)) {
                    when (val fillInfo = engine.getFill(block)) {
                        is SolidFill, is GradientFill -> fillInfo.mainColor
                        else -> null
                    }
                } else if (engine.block.supportsStroke(block) && engine.block.isStrokeEnabled(block)) {
                    engine.getStrokeColor(block)
                } else {
                    null
                }
                updatedColor ?: color
            },
        )
    }
}
