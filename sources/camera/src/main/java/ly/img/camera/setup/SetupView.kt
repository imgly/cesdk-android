package ly.img.camera.setup

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import ly.img.camera.components.CameraRotatedLayout
import ly.img.camera.components.cameraDialogModifier
import ly.img.editor.core.ui.permissions.PermissionsView

@Composable
internal fun SetupView(
    orientation: Int,
    rotation: () -> Float,
    modifier: Modifier,
    hasRequiredPermissions: Boolean,
    isLoading: Boolean,
    error: Throwable?,
    onClose: () -> Unit,
    onAllPermissionsGranted: () -> Unit,
    requestOnlyCameraPermission: Boolean = false,
) {
    // Dialogs use a separate window and must receive their own rotation.
    val dialogModifier = cameraDialogModifier(orientation, rotation)
    Box(modifier = modifier.padding(24.dp)) {
        CameraRotatedLayout(
            modifier = Modifier.align(Alignment.Center),
            sideways = orientation == 90 || orientation == 270,
            rotation = rotation,
            alpha = { 1f },
        ) {
            Column(
                modifier = Modifier
                    .widthIn(max = 264.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                when {
                    !hasRequiredPermissions -> PermissionsView(
                        requestOnlyCameraPermission = requestOnlyCameraPermission,
                        onAllPermissionsGranted = onAllPermissionsGranted,
                        onClose = onClose,
                        settingsDialogModifier = dialogModifier,
                        settingsDialogProperties = DialogProperties(usePlatformDefaultWidth = false),
                    )
                    isLoading -> CircularProgressIndicator(
                        modifier = Modifier.size(36.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    error != null -> ErrorView(error, onClose)
                }
            }
        }
    }
}
