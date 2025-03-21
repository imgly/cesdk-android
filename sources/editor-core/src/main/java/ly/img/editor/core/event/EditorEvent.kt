package ly.img.editor.core.event

import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import ly.img.editor.core.EditorScope
import ly.img.editor.core.library.data.UploadAssetSourceType
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.state.EditorViewMode
import ly.img.engine.DesignBlock
import kotlin.time.Duration

/**
 * An editor event that can be sent via [EditorEventHandler].
 * All the instances of classes that inherit from [EditorEvent] and are declared below are handled internally, i.e. [EditorEvent.CloseEditor],
 * [EditorEvent.Sheet.Open].
 * All the remaining events are considered as custom and need to be handled manually in [ly.img.editor.EditorConfiguration.onEvent].
 * However, no matter internal or custom, all events are forwarded to [ly.img.editor.EditorConfiguration.onEvent]
 * and can be useful to update your state, do action tracking etc.
 */
interface EditorEvent {
    /**
     * All sheet related events.
     */
    class Sheet {
        /**
         * An event for opening a new sheet.
         */
        class Open(
            val type: SheetType,
        ) : EditorEvent

        /**
         * An event for expanding the sheet that is currently open.
         */
        class Expand(
            val animate: Boolean,
        ) : EditorEvent

        /**
         * An event for half expanding the sheet that is currently open.
         */
        class HalfExpand(
            val animate: Boolean,
        ) : EditorEvent

        /**
         * An event for closing the sheet that is currently open.
         */
        class Close(
            val animate: Boolean,
        ) : EditorEvent

        /**
         * An event that is emitted when the sheet is fully expanded after calling [Expand] or the user manually does it.
         */
        class OnExpanded(
            val type: SheetType,
        ) : EditorEvent

        /**
         * An event that is emitted when the sheet is fully expanded after calling [HalfExpand] or the user manually does it.
         */
        class OnHalfExpanded(
            val type: SheetType,
        ) : EditorEvent

        /**
         * An event that is emitted when the sheet is fully expanded after calling [Close] or the user manually does it.
         */
        class OnClosed(
            val type: SheetType,
        ) : EditorEvent
    }

    /**
     * All events related to design block selection.
     */
    class Selection {
        /**
         * An event for entering text editing mode for the selected design block.
         */
        class EnterTextEditMode : EditorEvent

        /**
         * An event for duplicating currently selected design block.
         */
        class Duplicate : EditorEvent

        /**
         * An event for splitting currently selected design block in a video scene.
         */
        class Split : EditorEvent

        /**
         * An event for moving currently selected design block into the background track as clip in a video scene.
         */
        class MoveAsClip : EditorEvent

        /**
         * An event for moving currently selected design block from the background track to an overlay in a video scene.
         */
        class MoveAsOverlay : EditorEvent

        /**
         * An event for changing selection from a selected group to the first block within that group.
         */
        class EnterGroup : EditorEvent

        /**
         * An event for changing selection from currently selected design block to the group design block
         * that contains the selected design block.
         */
        class SelectGroup : EditorEvent

        /**
         * An event for deleting currently selected design block.
         */
        class Delete : EditorEvent

        /**
         * An event for bringing forward currently selected design block.
         */
        class BringForward : EditorEvent

        /**
         * An event for sending backward currently selected design block.
         */
        class SendBackward : EditorEvent
    }

    /**
     * All events related to navigation inside the editor.
     */
    class Navigation {
        /**
         * An event for navigating to the previous page.
         */
        class ToPreviousPage : EditorEvent

        /**
         * An event for navigating to the next page.
         */
        class ToNextPage : EditorEvent
    }

    /**
     * An event for triggering [ly.img.editor.EngineConfiguration.onClose] callback.
     */
    class OnClose : EditorEvent

    /**
     * An event for closing the editor. This force closes the editor without entering the
     * [ly.img.editor.EngineConfiguration.onClose] callback.
     *
     * @param throwable an optional parameter in case the editor is closed due to an error.
     */
    class CloseEditor(
        val throwable: Throwable? = null,
    ) : EditorEvent

    class Export {
        /**
         * An event for starting an export process. This event triggers the [ly.img.editor.EngineConfiguration.onExport] callback.
         */
        class Start : EditorEvent

        /**
         * An event for canceling the export job if it is running.
         */
        class Cancel : EditorEvent
    }

    /**
     * An event for setting the view mode of the editor to a new value.
     *
     * Note that some view modes may look weird or cause unexpected behaviors in some of the solutions.
     * The following mapping shows the best [EditorViewMode] - solution combinations:
     * [EditorViewMode.Edit] - best used in all solutions.
     * [EditorViewMode.Preview] - best used in [ly.img.editor.PhotoEditor], [ly.img.editor.ApparelEditor] and [ly.img.editor.PostcardEditor].
     * [EditorViewMode.Pages] - best used in [ly.img.editor.DesignEditor].
     */
    class SetViewMode(
        val viewMode: EditorViewMode,
    ) : EditorEvent

    /**
     * An event for launching any contract via [ActivityResultContract] API.
     * IMPORTANT: Do not capture any values in the [onOutput]. In case the activity of the editor is killed
     * when returning back it may cause issues.
     *
     * @param contract the contract that should be launched.
     * @param input the input parameter of the contract.
     * @param onOutput the callback that is invoked when the contract returns.
     */
    class LaunchContract<I, O>(
        val contract: ActivityResultContract<I, O>,
        val input: I,
        val onOutput: EditorScope.(O) -> Unit,
    ) : EditorEvent {
        var launched = false

        companion object {
            var current: LaunchContract<*, *>? = null
        }
    }

    /**
     * An event for adding a uri to the scene.
     * In addition, the uri will be transformed into an [ly.img.engine.AssetDefinition] and will be added to the asset source
     * represented by [uploadAssetSourceType].
     *
     * @param uploadAssetSourceType the asset source where [uri] should be added.
     * Check [ly.img.editor.core.library.data.AssetSourceType] for available [UploadAssetSourceType]s.
     * @param uri the uri which content should be added to the scene.
     */
    class AddUriToScene(
        val uploadAssetSourceType: UploadAssetSourceType,
        val uri: Uri,
    ) : EditorEvent

    /**
     * An event for replacing the content of the [designBlock] with uri content.
     * In addition, the uri will be transformed into an [ly.img.engine.AssetDefinition] and will be added to the asset source
     * represented by [uploadAssetSourceType].
     *
     * @param uploadAssetSourceType the asset source where [uri] should be added.
     * Check [ly.img.editor.core.library.data.AssetSourceType] for available [UploadAssetSourceType]s.
     * @param uri the uri which content should be added to the scene.
     * @param designBlock the design block which content should be replaced with the [uri].
     */
    class ReplaceUriAtScene(
        val uploadAssetSourceType: UploadAssetSourceType,
        val uri: Uri,
        val designBlock: DesignBlock,
    ) : EditorEvent

    /**
     * An event for adding camera recordings to the scene.
     * In addition, the recordings will be transformed into [ly.img.engine.AssetDefinition]s and will be added to the asset source
     * represented by [uploadAssetSourceType].
     *
     * @param uploadAssetSourceType the asset source where [recordings] should be added.
     * @param recordings the list of the recordings.
     */
    class AddCameraRecordingsToScene(
        val uploadAssetSourceType: UploadAssetSourceType,
        val recordings: List<Pair<Uri, Duration>>,
    ) : EditorEvent

    /**
     * An event for canceling the export job if it is running.
     */
    @Deprecated(
        message = "Use EditorEvent.Export.Cancel instead",
        replaceWith = ReplaceWith("Export.Cancel"),
    )
    class CancelExport : EditorEvent
}

/**
 * An interface for sending editor events that can be captured in [ly.img.editor.EditorConfiguration.onEvent].
 */
interface EditorEventHandler {
    /**
     * A function for sending [EditorEvent]s. If the event is an instance of [EditorEvent.Internal] then it will be handled
     * by the editor automatically. All other events are forwarded to [ly.img.editor.EditorConfiguration.onEvent].
     */
    fun send(event: EditorEvent)

    /**
     * A special function for closing the editor. This force closes the editor without entering the
     * [ly.img.editor.EngineConfiguration.onClose] callback.
     *
     * @param throwable an optional parameter in case the editor is closed due to an error.
     */
    @Deprecated(
        message = "Use EditorEventHandler.send(EditorEvent.CloseEditor(throwable) instead",
        replaceWith = ReplaceWith("send(EditorEvent.CloseEditor(throwable))"),
    )
    fun sendCloseEditorEvent(throwable: Throwable? = null) = send(EditorEvent.CloseEditor(throwable))

    /**
     * A special function for canceling the export job if it is running.
     */
    @Deprecated(
        message = "Use EditorEventHandler.send(EditorEvent.Export.Cancel() instead",
        replaceWith = ReplaceWith("send(EditorEvent.Export.Cancel())"),
    )
    fun sendCancelExportEvent() = send(EditorEvent.Export.Cancel())
}
