package ly.img.editor.base.dock.options.fontSize

import androidx.compose.ui.graphics.vector.ImageVector
import ly.img.editor.base.dock.options.fontSize.FontSizeUiState.Size.Large
import ly.img.editor.base.engine.textFontSize
import ly.img.editor.core.iconpack.SizeL
import ly.img.editor.core.iconpack.SizeM
import ly.img.editor.core.iconpack.SizeS
import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import ly.img.editor.core.iconpack.IconPack as CoreIconPack

data class FontSizeUiState(
    val designBlock: DesignBlock,
    val selectedSize: Size,
) {
    enum class Size(
        val size: Float,
        val icon: ImageVector,
    ) {
        Small(14f, CoreIconPack.SizeS),
        Medium(18f, CoreIconPack.SizeM),
        Large(22f, CoreIconPack.SizeL),
    }

    companion object {
        fun create(
            designBlock: DesignBlock,
            engine: Engine,
        ): FontSizeUiState {
            // A run's size wins over the block property, so a caption — whose preset stamps the run — reports
            // a stale size when read straight from `text/fontSize`, and falls through to Large.
            val size = engine.block.textFontSize(designBlock)
            return FontSizeUiState(
                designBlock = designBlock,
                selectedSize = Size.entries.firstOrNull { it.size == size } ?: Large,
            )
        }
    }
}
