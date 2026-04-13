package ly.img.editor.core.state

import androidx.compose.runtime.Immutable
import ly.img.editor.core.component.data.Insets
import ly.img.editor.core.component.data.Size
import ly.img.editor.core.sheet.SheetState
import ly.img.editor.core.sheet.SheetType
import kotlin.time.Duration

/**
 * Current state of the editor.
 *
 * @param insets the insets of the canvas in screen space. It is a combination of system insets, IMG.LY internal logic insets and [extraInsets].
 * @param extraInsets the extra insets set via [ly.img.editor.core.event.EditorEvent.Insets.SetExtra].
 * @param activeSheet the sheet that is being displayed currently.
 * @param activeSheetState the state of the active sheet. Use [androidx.compose.runtime.snapshotFlow] to observe its properties.
 * @param isTouchActive whether there is an ongoing touch action on the canvas.
 * @param isHistoryEnabled whether history changes (undo/redo) are enabled at the moment.
 * @param isBackHandlerEnabled whether the editor's internal [androidx.activity.compose.BackHandler] is currently enabled. If `false`,
 * it indicates that the editor will not intercept back presses. This can be used
 * by higher-level components (such as overlays) to register their own
 * [androidx.activity.compose.BackHandler] when the editor is not handling back navigation internally.
 *
 * **Note:** If an overlay registers a [androidx.activity.compose.BackHandler] while `isBackHandlerEnabled` is `true`, it
 * can override the editor's own back handling logic and lead to incorrect behavior.
 *
 * @param viewMode the current view mode of the editor.
 * @param dimensions the dimensions of various editor components.
 * @param minVideoDuration the minimum video duration constraint applied to video scenes.
 * @param maxVideoDuration the maximum video duration constraint applied to video scenes.
 */
@Immutable
data class EditorState(
    val insets: Insets = Insets.Zero,
    val extraInsets: Insets = Insets.Zero,
    val activeSheet: SheetType? = null,
    val activeSheetState: SheetState? = null,
    val isTouchActive: Boolean = false,
    val isHistoryEnabled: Boolean = true,
    val isBackHandlerEnabled: Boolean = false,
    val viewMode: EditorViewMode = EditorViewMode.Edit(),
    val dimensions: Dimensions = Dimensions(),
    val minVideoDuration: Duration? = null,
    val maxVideoDuration: Duration? = null,
)

/**
 * Class representing the view mode of the editor.
 */
@Immutable
sealed interface EditorViewMode {
    /**
     * Editing mode of the editor.
     */
    @Immutable
    class Edit : EditorViewMode

    /**
     * Preview mode of the editor that previews the current design.
     */
    @Immutable
    class Preview : EditorViewMode

    /**
     * Pages mode of the editor that displays thumbnails of all the pages in a grid.
     */
    @Immutable
    class Pages : EditorViewMode
}

/**
 * Dimensions of various editor components.
 *
 * @param editor current dimensions of the editor itself.
 * @param navigationBar current dimensions of the navigation bar.
 * @param bottomPanel current dimensions of the bottom panel.
 * @param dock current dimensions of the dock.
 * @param inspectorBar current dimensions of the inspector bar.
 */
data class Dimensions(
    val editor: Size = Size.Zero,
    val navigationBar: Size = Size.Zero,
    val bottomPanel: Size = Size.Zero,
    val dock: Size = Size.Zero,
    val inspectorBar: Size = Size.Zero,
)
