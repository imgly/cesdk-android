package ly.img.editor.video

import ly.img.editor.base.engine.CROP_EDIT_MODE
import ly.img.editor.base.engine.TOUCH_ACTION_SCALE
import ly.img.editor.base.engine.showPage
import ly.img.editor.base.ui.Block
import ly.img.editor.base.ui.EditorUiViewModel
import ly.img.editor.core.EditorScope
import ly.img.editor.core.ui.engine.BlockType
import ly.img.editor.core.ui.engine.getCurrentPage
import ly.img.editor.core.ui.library.LibraryViewModel

class VideoUiViewModel(
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
    val uiState = baseUiState

    override val verticalPageInset
        get() = if (engine.editor.getEditMode() == CROP_EDIT_MODE) CROP_MODE_INSET else 1f

    override fun getBlockForEvents(): Block = super.getBlockForEvents() ?: Block(
        designBlock = engine.getCurrentPage(),
        type = BlockType.Page,
    )

    override fun onPreCreate() {
        super.onPreCreate()
        with(engine.editor) {
            setSettingEnum("touch/pinchAction", TOUCH_ACTION_SCALE)
            setSettingBoolean("controlGizmo/showRotateHandles", false)
            setSettingBoolean("controlGizmo/showScaleHandles", false)
            setSettingBoolean("controlGizmo/showMoveHandles", false)
            setSettingBoolean("touch/singlePointPanning", false)
            setSettingBoolean("features/removeForegroundTracksOnSceneLoad", true)
            setSettingColor("page/innerBorderColor", ly.img.engine.Color.fromRGBA(0.67f, 0.67f, 0.67f, 0.5f))
        }
    }

    override fun enterEditMode() {
        engine.showPage(pageIndex.value)
    }

    override fun enterPreviewMode() = throw UnsupportedOperationException()

    private companion object {
        const val CROP_MODE_INSET = 24F
    }
}
