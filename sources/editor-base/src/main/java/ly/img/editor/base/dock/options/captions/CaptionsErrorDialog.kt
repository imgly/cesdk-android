package ly.img.editor.base.dock.options.captions

import android.content.Context
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ly.img.editor.core.R
import ly.img.editor.core.getDisplayMessage
import ly.img.engine.EngineException

/** Reports a failed import or generation. The scene is unchanged by the time this shows, so there is nothing to undo. */
@Composable
internal fun CaptionsErrorDialog(
    title: String,
    message: String,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = title) },
        text = { Text(text = message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ly_img_editor_dialog_error_confirm_text))
            }
        },
    )
}

/**
 * What to tell the user about a failed SRT/VTT import.
 *
 * Resolved outside composition because the failure arrives from a coroutine, and the engine's own copy is only
 * a last resort: these codes are shared with the export pipeline, whose wording would be wrong here.
 */
internal fun Context.captionImportErrorMessage(throwable: Throwable): String = when (CaptionImport.failure(throwable)) {
    CaptionImportFailure.ParseEmpty -> getString(R.string.ly_img_editor_dialog_captions_import_error_parse_empty)
    CaptionImportFailure.UnsupportedFormat -> getString(R.string.ly_img_editor_dialog_captions_import_error_unsupported_format)
    CaptionImportFailure.FileDamaged -> getString(R.string.ly_img_editor_dialog_captions_import_error_file_damaged)
    CaptionImportFailure.FileUnreadable -> getString(R.string.ly_img_editor_dialog_captions_import_error_file_unreadable)
    // Only an EngineException maps to Unknown, so its own message is always the one to show here.
    CaptionImportFailure.Unknown -> (throwable as EngineException).getDisplayMessage(this)
}
