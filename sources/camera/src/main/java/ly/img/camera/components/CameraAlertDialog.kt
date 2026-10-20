package ly.img.camera.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties

@Composable
internal fun CameraAlertDialog(
    orientation: Int,
    rotation: () -> Float,
    onDismissRequest: () -> Unit,
    confirmButton: @Composable () -> Unit,
    title: @Composable () -> Unit,
    text: @Composable () -> Unit,
    dismissButton: (@Composable () -> Unit)? = null,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
) {
    AlertDialog(
        modifier = cameraDialogModifier(orientation, rotation),
        // The platform's portrait width limit would constrain the rotated dialog's height.
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside,
            usePlatformDefaultWidth = false,
        ),
        onDismissRequest = onDismissRequest,
        confirmButton = confirmButton,
        dismissButton = dismissButton,
        title = title,
        text = text,
    )
}

internal fun cameraDialogModifier(
    orientation: Int,
    rotation: () -> Float,
): Modifier = Modifier
    .padding(24.dp)
    .cameraRotation(
        sideways = orientation == 90 || orientation == 270,
        rotation = rotation,
        alpha = { 1f },
    )
