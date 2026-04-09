package ly.img.editor

import android.net.Uri
import ly.img.editor.core.event.EditorEvent
import ly.img.engine.MimeType
import java.io.File
import kotlin.time.Duration

/**
 * An event that is invoked in the default implementations of [EngineConfiguration.onCreate] and [EngineConfiguration.onExport]
 * for showing a loading overlay while load/export operations are running.
 */
object ShowLoading : EditorEvent

/**
 * An event that is invoked for hiding the loading overlay previously triggered by the [ShowLoading] event.
 */
object HideLoading : EditorEvent

/**
 * An event that is invoked in the default implementations of [EngineConfiguration.onCreate] when the scene load is finished.
 */
class OnSceneLoaded : EditorEvent

/**
 * An event that is invoked in the default implementation of [EngineConfiguration.onExport] for a video scene
 * for showing the progress of the export.
 */
class ShowVideoExportProgressEvent(
    val progress: Float,
) : EditorEvent

/**
 * An event that is invoked in the default implementation of [EngineConfiguration.onExport] for a video scene
 * for showing an error during export.
 */
object ShowVideoExportErrorEvent : EditorEvent

/**
 * An event that is invoked in the default implementation of [EngineConfiguration.onExport] for a video scene
 * for showing that the export was successful. The [uri] is the uri of the exported file and the [mimeType] is the mime type of the file.
 */
class ShowVideoExportSuccessEvent(
    val uri: Uri,
    val mimeType: String,
) : EditorEvent

/**
 * An event that is invoked in the default implementation of [EngineConfiguration.onExport] for a video scene
 * when the scene duration is below the minimum length requirement.
 */
class ShowVideoMinLengthDialogEvent(
    val minimumDuration: Duration,
) : EditorEvent

/**
 * An event that is invoked for hiding the minimum video length dialog previously triggered by the
 * [ShowVideoMinLengthDialogEvent] event.
 */
object DismissVideoMinLengthDialogEvent : EditorEvent

/**
 * An event that is invoked for canceling the export of a video scene.
 */
object DismissVideoExportEvent : EditorEvent

/**
 * An event that is invoked in the default implementations of [EngineConfiguration.onError] for showing an error dialog.
 */
class ShowErrorDialogEvent(
    val error: Throwable,
) : EditorEvent

/**
 * An event that is invoked for hiding the error dialog previously triggered by the [ShowErrorDialogEvent] event.
 */
object HideErrorDialogEvent : EditorEvent

/**
 * An event that is invoked in the default implementation of [EngineConfiguration.onExport] after the export is done.
 * By default, the event is captured in [EditorConfiguration.onEvent] and a system level popup is displayed to share the [file]
 * that was exported. The [mimeType] is the mime type of the file being shared.
 */
class ShareFileEvent(
    val file: File,
    val mimeType: String = MimeType.PDF.key,
) : EditorEvent

/**
 * An event that is invoked in the default implementation of [EngineConfiguration.onExport] after the export is done.
 * By default, the event is captured in [EditorConfiguration.onEvent] and a system level popup is displayed to share the [uri].
 * The [mimeType] is the mime type of the content being shared.
 */
class ShareUriEvent(
    val uri: Uri,
    val mimeType: String = MimeType.PDF.key,
) : EditorEvent

/**
 * An event that is invoked in the default implementations of [EngineConfiguration.onClose] for showing a confirmation dialog
 * when there are unsaved changes in the editor.
 */
object ShowCloseConfirmationDialogEvent : EditorEvent

/**
 * An event that is invoked for hiding the confirmation dialog previously triggered by the [ShowCloseConfirmationDialogEvent] event.
 */
object DismissCloseConfirmationDialogEvent : EditorEvent
