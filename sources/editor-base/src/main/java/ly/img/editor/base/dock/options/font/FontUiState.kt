package ly.img.editor.base.dock.options.font

import ly.img.editor.core.library.LibraryCategory
import ly.img.editor.core.ui.library.TypefaceLibraryCategory
import ly.img.engine.DesignBlock
import ly.img.engine.Engine

data class FontUiState(
    val designBlock: DesignBlock,
    val libraryCategory: LibraryCategory,
    val fontFamily: String,
    val filter: List<String>,
) {
    companion object {
        fun create(
            designBlock: DesignBlock,
            fontFamilies: List<String>?,
            engine: Engine,
        ): FontUiState {
            val typeface = runCatching { engine.block.getTypeface(designBlock) }.getOrNull()
            return FontUiState(
                designBlock = designBlock,
                libraryCategory = TypefaceLibraryCategory,
                fontFamily = typeface?.name ?: "Default",
                filter = fontFamilies ?: listOf(
                    "//ly.img.typeface/caveat",
                    "//ly.img.typeface/amaticsc",
                    "//ly.img.typeface/courier_prime",
                    "//ly.img.typeface/archivo",
                    "//ly.img.typeface/roboto",
                    "//ly.img.typeface/parisienne",
                ),
            )
        }
    }
}
