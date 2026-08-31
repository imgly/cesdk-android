package ly.img.editor.base.dock.options.captions

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ly.img.editor.core.R

/** Confirmation dialog for discarding every caption at once. */
@Composable
internal fun DeleteAllCaptionsDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = stringResource(R.string.ly_img_editor_dialog_captions_delete_all_title)) },
        text = { Text(text = stringResource(R.string.ly_img_editor_dialog_captions_delete_all_text)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.ly_img_editor_dialog_captions_delete_all_button_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.ly_img_editor_dialog_captions_delete_all_button_dismiss))
            }
        },
    )
}
