package ly.img.editor.postcard

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import ly.img.editor.base.engine.LayoutAxis
import ly.img.editor.base.engine.resetHistory
import ly.img.editor.base.engine.showAllPages
import ly.img.editor.base.engine.showPage
import ly.img.editor.base.engine.zoomToScene
import ly.img.editor.base.ui.Block
import ly.img.editor.base.ui.EditorUiViewModel
import ly.img.editor.core.EditorScope
import ly.img.editor.core.ui.engine.BlockType
import ly.img.editor.core.ui.engine.Scope
import ly.img.editor.core.ui.engine.deselectAllBlocks
import ly.img.editor.core.ui.library.LibraryViewModel
import ly.img.engine.GlobalScope

class PostcardUiViewModel(
    onCreate: suspend EditorScope.() -> Unit,
    onLoaded: suspend EditorScope.() -> Unit,
    onExport: suspend EditorScope.() -> Unit,
    onClose: suspend EditorScope.(Boolean) -> Unit,
    onError: suspend EditorScope.(Throwable) -> Unit,
    libraryViewModel: LibraryViewModel,
) : EditorUiViewModel(
        onCreate = onCreate,
        onLoaded = onLoaded,
        onExport = onExport,
        onClose = onClose,
        onError = onError,
        libraryViewModel = libraryViewModel,
    ) {
    private var hasUnsavedChanges = false

    val uiState = merge(baseUiState, pageIndex, historyChangeTrigger).map {
        PostcardUiViewState(
            editorUiViewState = baseUiState.value,
            postcardMode = if (pageIndex.value == 0) PostcardMode.Design else PostcardMode.Write,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PostcardUiViewState(baseUiState.value),
    )

    override fun getBlockForEvents(): Block = super.getBlockForEvents() ?: Block(
        designBlock = engine.block.findByName("Greeting").first(),
        type = BlockType.Text,
    )

    override fun onPreCreate() {
        super.onPreCreate()
        engine.editor.setGlobalScope(Scope.EditorAdd, GlobalScope.DEFER)
    }

    override fun enterEditMode() {
        engine.showPage(pageIndex.value)
    }

    override fun enterPreviewMode() {
        engine.deselectAllBlocks()
        showAllPages()
        engine.zoomToScene(publicState.value.canvasInsets)
    }

    override fun handleBackPress(
        bottomSheetOffset: Float,
        bottomSheetMaxOffset: Float,
    ): Boolean {
        val handled = super.handleBackPress(
            bottomSheetOffset = bottomSheetOffset,
            bottomSheetMaxOffset = bottomSheetMaxOffset,
        )
        return if (handled.not()) {
            val page = pageIndex.value
            if (page > 0) {
                setPage(page - 1)
                true
            } else {
                false
            }
        } else {
            true
        }
    }

    override fun hasUnsavedChanges(): Boolean = super.hasUnsavedChanges() || hasUnsavedChanges

    override fun setPage(index: Int) {
        super.setPage(index)
        if (engine.editor.canUndo()) hasUnsavedChanges = true
        engine.resetHistory()
    }

    private fun showAllPages() {
        engine.showAllPages(if (inPortraitMode) LayoutAxis.Vertical else LayoutAxis.Horizontal)
    }
}
