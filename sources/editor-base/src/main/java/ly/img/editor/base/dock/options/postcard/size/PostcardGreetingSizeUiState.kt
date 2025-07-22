package ly.img.editor.base.dock.options.postcard.size

import androidx.compose.ui.graphics.vector.ImageVector
import ly.img.editor.base.dock.options.postcard.size.PostcardGreetingSizeUiState.Size.Large
import ly.img.editor.core.iconpack.SizeL
import ly.img.editor.core.iconpack.SizeM
import ly.img.editor.core.iconpack.SizeS
import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import ly.img.editor.core.iconpack.IconPack as CoreIconPack

data class PostcardGreetingSizeUiState(
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
        fun create(engine: Engine): PostcardGreetingSizeUiState {
            val designBlock = engine.block.findByName("Greeting").first()
            val size = engine.block.getFloat(designBlock, "text/fontSize")
            return PostcardGreetingSizeUiState(
                designBlock = designBlock,
                selectedSize = Size.entries.firstOrNull { it.size == size } ?: Large,
            )
        }
    }
}
