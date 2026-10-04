package ly.img.camera.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ly.img.camera.core.R
import ly.img.editor.core.theme.LocalExtendedColorScheme

/** Bottom action bar for the photo preview: `Back` discards, `Done` commits. */
@Composable
internal fun PhotoPreviewActions(
    orientation: Int,
    rotation: () -> Float,
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        CameraRotatedLayout(
            modifier = Modifier.align(Alignment.CenterStart),
            sideways = orientation == 90 || orientation == 270,
            rotation = rotation,
            alpha = { 1f },
        ) {
            FilledTonalButton(
                onClick = onBack,
                shape = CircleShape,
                colors = ButtonDefaults.filledTonalButtonColors(
                    contentColor = LocalExtendedColorScheme.current.white,
                ),
            ) {
                Text(text = stringResource(R.string.ly_img_camera_button_photo_preview_back))
            }
        }
        CameraRotatedLayout(
            modifier = Modifier.align(Alignment.CenterEnd),
            sideways = orientation == 90 || orientation == 270,
            rotation = rotation,
            alpha = { 1f },
        ) {
            Button(
                onClick = onDone,
                shape = CircleShape,
                // Brand-saturated blue; dark-theme `primary` is desaturated for contrast and looks pale here.
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.inversePrimary,
                    contentColor = LocalExtendedColorScheme.current.white,
                ),
            ) {
                Text(text = stringResource(R.string.ly_img_camera_button_photo_preview_done))
            }
        }
    }
}
