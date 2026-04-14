package ly.img.editor.postcard

import ly.img.editor.base.ui.EditorUiViewState

data class PostcardUiViewState(
    val editorUiViewState: EditorUiViewState,
    val postcardMode: PostcardMode = PostcardMode.Design,
)

enum class PostcardMode {
    Design,
    Write,
}
