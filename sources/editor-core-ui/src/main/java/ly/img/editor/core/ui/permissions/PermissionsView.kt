package ly.img.editor.core.ui.permissions

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import ly.img.editor.core.R
import ly.img.editor.core.theme.LocalExtendedColorScheme
import ly.img.editor.core.ui.iconpack.Check
import ly.img.editor.core.ui.iconpack.IconPack
import ly.img.editor.core.ui.iconpack.Mic
import ly.img.editor.core.ui.iconpack.Photocameraoutline
import ly.img.editor.core.ui.iconpack.Warning
import ly.img.editor.core.ui.permissions.PermissionManager.Companion.hasCameraPermission
import ly.img.editor.core.ui.permissions.PermissionManager.Companion.hasMicPermission
import ly.img.editor.core.ui.utils.lifecycle.LifecycleEventEffect

@Composable
fun PermissionsView(
    requestOnlyCameraPermission: Boolean = false,
    onAllPermissionsGranted: () -> Unit,
    onClose: () -> Unit,
) {
    CompositionLocalProvider(
        LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Icon(
            IconPack.Warning,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(
                if (requestOnlyCameraPermission) {
                    R.string.ly_img_editor_permission_view_title_camera
                } else {
                    R.string.ly_img_editor_permission_view_title_camera_and_microphone
                },
            ),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
        )
    }
    Spacer(modifier = Modifier.height(56.dp))

    val context = LocalContext.current
    val permissionManager = remember { PermissionManager(context) }
    var isCameraPermissionGranted by remember { mutableStateOf(context.hasCameraPermission()) }
    var isMicPermissionGranted by remember { mutableStateOf(context.hasMicPermission()) }
    var currentRequestedPermission: String? by remember { mutableStateOf(null) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    if (showSettingsDialog) {
        fun dismissDialog() {
            showSettingsDialog = false
            currentRequestedPermission = null
        }
        currentRequestedPermission?.let {
            SettingsDialog(
                permission = it,
                onDismissRequest = ::dismissDialog,
                onConfirmClick = permissionManager::openAppSettings,
            )
        }
    }

    fun refresh() {
        isMicPermissionGranted = context.hasMicPermission()
        isCameraPermissionGranted = context.hasCameraPermission()
        if ((requestOnlyCameraPermission && isCameraPermissionGranted) || (isCameraPermissionGranted && isMicPermissionGranted)) {
            onAllPermissionsGranted()
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) {
        // refresh() is already being handled below.
        currentRequestedPermission?.let {
            showSettingsDialog = permissionManager.shouldOpenSettings(it)
        }
    }

    // The user could enable the permissions from settings
    LifecycleEventEffect(event = Lifecycle.Event.ON_RESUME) {
        refresh()
    }

    fun requestPermission(permission: String) {
        currentRequestedPermission = permission
        permissionLauncher.launch(permission)
    }

    PermissionButton(
        onClick = {
            requestPermission(permission = Manifest.permission.CAMERA)
        },
        icon = IconPack.Photocameraoutline,
        text = R.string.ly_img_editor_permission_view_button_camera,
        isPermissionGranted = isCameraPermissionGranted,
    )
    if (!requestOnlyCameraPermission) {
        Spacer(modifier = Modifier.height(16.dp))
        PermissionButton(
            onClick = {
                requestPermission(permission = Manifest.permission.RECORD_AUDIO)
            },
            icon = IconPack.Mic,
            text = R.string.ly_img_editor_permission_view_button_microphone,
            isPermissionGranted = isMicPermissionGranted,
        )
    }
    Spacer(modifier = Modifier.height(16.dp))
    TextButton(
        onClick = onClose,
        colors = ButtonDefaults.textButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    ) {
        Text(text = stringResource(R.string.ly_img_editor_permission_view_button_cancel))
    }
}

@Composable
private fun PermissionButton(
    onClick: () -> Unit,
    icon: ImageVector,
    @StringRes text: Int,
    isPermissionGranted: Boolean,
    disabledContainerColor: Color = LocalExtendedColorScheme.current.green.colorContainer.copy(alpha = 0.5f),
    disabledContentColor: Color = LocalExtendedColorScheme.current.green.onColor.copy(alpha = 0.5f),
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = !isPermissionGranted,
        colors = ButtonDefaults.filledTonalButtonColors(
            disabledContainerColor = disabledContainerColor,
            disabledContentColor = disabledContentColor,
        ),
    ) {
        Icon(
            if (isPermissionGranted) IconPack.Check else icon,
            contentDescription = null,
            modifier = Modifier.size(18.dp),
            tint = if (isPermissionGranted) disabledContentColor else MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(text = stringResource(text))
    }
}

@Composable
private fun SettingsDialog(
    permission: String,
    onDismissRequest: () -> Unit,
    onConfirmClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(
                text = stringResource(
                    when (permission) {
                        Manifest.permission.CAMERA -> R.string.ly_img_editor_dialog_permission_camera_title
                        Manifest.permission.RECORD_AUDIO -> R.string.ly_img_editor_dialog_permission_microphone_title
                        else -> throw IllegalArgumentException()
                    },
                ),
            )
        },
        text = {
            Text(
                text = stringResource(
                    when (permission) {
                        Manifest.permission.CAMERA -> R.string.ly_img_editor_dialog_permission_camera_text
                        Manifest.permission.RECORD_AUDIO -> R.string.ly_img_editor_dialog_permission_microphone_text
                        else -> throw IllegalArgumentException()
                    },
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirmClick()
                    onDismissRequest()
                },
            ) {
                Text(
                    text = stringResource(
                        when (permission) {
                            Manifest.permission.CAMERA -> R.string.ly_img_editor_dialog_permission_camera_button_confirm
                            Manifest.permission.RECORD_AUDIO -> R.string.ly_img_editor_dialog_permission_microphone_button_confirm
                            else -> throw IllegalArgumentException()
                        },
                    ),
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
            ) {
                Text(
                    text = stringResource(
                        when (permission) {
                            Manifest.permission.CAMERA -> R.string.ly_img_editor_dialog_permission_camera_button_dismiss
                            Manifest.permission.RECORD_AUDIO -> R.string.ly_img_editor_dialog_permission_microphone_button_dismiss
                            else -> throw IllegalArgumentException()
                        },
                    ),
                )
            }
        },
    )
}
