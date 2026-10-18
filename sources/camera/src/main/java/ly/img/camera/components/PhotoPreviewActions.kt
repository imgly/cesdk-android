package ly.img.camera.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
    onBack: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
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
        Spacer(modifier = Modifier.weight(1f))
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
