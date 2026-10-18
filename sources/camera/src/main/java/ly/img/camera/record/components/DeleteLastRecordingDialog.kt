package ly.img.camera.record.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ly.img.camera.core.R

@Composable
internal fun DeleteLastRecordingDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(
                    R.string.ly_img_camera_dialog_delete_last_recording_title,
                ),
            )
        },
        text = {
            Text(
                text = stringResource(
                    R.string.ly_img_camera_dialog_delete_last_recording_text,
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.error,
                ),
            ) {
                Text(stringResource(R.string.ly_img_camera_dialog_delete_last_recording_button_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
            ) {
                Text(stringResource(R.string.ly_img_camera_dialog_delete_last_recording_button_dismiss))
            }
        },
    )
}
