package ly.img.editor.base.dock.options.postcard.font

import ly.img.editor.core.library.LibraryCategory
import ly.img.editor.core.ui.library.TypefaceLibraryCategory
import ly.img.engine.DesignBlock
import ly.img.engine.Engine

data class PostcardGreetingFontUiState(
    val designBlock: DesignBlock,
    val libraryCategory: LibraryCategory,
    val fontFamily: String,
    val filter: List<String>,
) {
    companion object {
        fun create(engine: Engine): PostcardGreetingFontUiState {
            val designBlock = engine.block.findByName("Greeting").first()
            val typeface = runCatching { engine.block.getTypeface(designBlock) }.getOrNull()
            return PostcardGreetingFontUiState(
                designBlock = designBlock,
                libraryCategory = TypefaceLibraryCategory,
                fontFamily = typeface?.name ?: "Default",
                filter = listOf(
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
