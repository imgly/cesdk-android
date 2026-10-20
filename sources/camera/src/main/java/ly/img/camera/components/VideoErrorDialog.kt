package ly.img.camera.components

import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ly.img.camera.core.R

@Composable
internal fun VideoErrorDialog(
    orientation: Int,
    rotation: () -> Float,
    onClose: () -> Unit,
) {
    CameraAlertDialog(
        orientation = orientation,
        rotation = rotation,
        onDismissRequest = { },
        title = {
            Text(text = stringResource(R.string.ly_img_camera_dialog_video_error_title))
        },
        text = {
            Text(text = stringResource(R.string.ly_img_camera_dialog_video_error_text))
        },
        confirmButton = {
            TextButton(onClick = onClose) {
                Text(stringResource(R.string.ly_img_camera_dialog_video_error_button_dismiss))
            }
        },
        dismissOnBackPress = false,
        dismissOnClickOutside = false,
    )
}
