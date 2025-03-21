package ly.img.editor.core.state

import androidx.compose.ui.geometry.Rect
import ly.img.editor.core.sheet.SheetType

/**
 * Current state of the editor.
 *
 * @param canvasInsets the insets of the canvas in screen space.
 * @param activeSheet the sheet that is being displayed currently.
 * @param isTouchActive whether there is an ongoing touch action on the canvas.
 * @param isHistoryEnabled whether history changes (undo/redo) are enabled at the moment.
 * @param viewMode the current view mode of the editor.
 */
data class EditorState(
    val canvasInsets: Rect = Rect.Zero,
    val activeSheet: SheetType? = null,
    val isTouchActive: Boolean = false,
    val isHistoryEnabled: Boolean = true,
    val viewMode: EditorViewMode = EditorViewMode.Edit(),
)

/**
 * Class representing the view mode of the editor.
 */
sealed interface EditorViewMode {
    /**
     * Editing mode of the editor.
     */
    class Edit : EditorViewMode

    /**
     * Preview mode of the editor that previews the current design.
     */
    class Preview : EditorViewMode

    /**
     * Pages mode of the editor that displays thumbnails of all the pages in a grid.
     */
    class Pages : EditorViewMode
}
