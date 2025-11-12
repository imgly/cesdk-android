package ly.img.editor.core.ui.library.components.section

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import ly.img.editor.core.R
import ly.img.editor.core.library.data.GalleryPermissionManager
import ly.img.editor.core.ui.permissions.PermissionManager
import ly.img.editor.core.ui.utils.lifecycle.LifecycleEventEffect

@Composable
internal fun GalleryPermissionSettingsDialog(
    onDismissRequest: () -> Unit,
    onConfirmClick: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Text(text = stringResource(R.string.ly_img_editor_dialog_permission_gallery_title))
        },
        text = {
            Text(text = stringResource(R.string.ly_img_editor_dialog_permission_gallery_text))
        },
        confirmButton = {
            TextButton(onClick = onConfirmClick) {
                Text(text = stringResource(R.string.ly_img_editor_dialog_permission_gallery_button_confirm))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismissRequest) {
                Text(text = stringResource(R.string.ly_img_editor_dialog_permission_gallery_button_dismiss))
            }
        },
    )
}

data class GalleryPermissionStatus(
    val hasPermission: Boolean,
    val hasSelections: Boolean,
)

@Composable
internal fun rememberGalleryPermissionRequest(
    mimeTypeFilter: String?,
    onPermissionChanged: () -> Unit,
): GalleryPermissionRequestState {
    val context = LocalContext.current
    if (mimeTypeFilter == null) {
        return GalleryPermissionRequestState(
            statusState = mutableStateOf(GalleryPermissionStatus(hasPermission = true, hasSelections = true)),
            requestPermission = {},
            openSettings = {},
            Dialogs = {},
        )
    }

    val permissionManager = remember { PermissionManager(context) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var lastRequestedPermissions by remember { mutableStateOf(emptyArray<String>()) }
    var shouldHandleOnResume by remember { mutableStateOf(false) }

    fun computeStatus(): GalleryPermissionStatus {
        val hasPermission = GalleryPermissionManager.hasPermission(context, mimeTypeFilter)
        val hasSelections = GalleryPermissionManager.mode == GalleryPermissionManager.Mode.ALL ||
            GalleryPermissionManager.selectedUris.isNotEmpty()
        return GalleryPermissionStatus(hasPermission, hasSelections)
    }

    val statusState = remember { mutableStateOf(computeStatus()) }

    fun updateStatus() {
        statusState.value = computeStatus()
    }

    val currentPermissionChanged by rememberUpdatedState(newValue = onPermissionChanged)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grantedMap ->
        val granted = grantedMap.values.any { it }
        if (granted) {
            updateStatus()
            currentPermissionChanged()
        } else {
            if (lastRequestedPermissions.any { permissionManager.shouldOpenSettings(it) }) {
                showSettingsDialog = true
            }
        }
    }

    val requestPermission: () -> Unit = {
        val permissions = GalleryPermissionManager.requiredPermission(mimeTypeFilter)
            ?.filterNotNull()
            ?.toTypedArray()
            ?: emptyArray()
        if (permissions.isEmpty()) {
            showSettingsDialog = true
        } else {
            lastRequestedPermissions = permissions
            shouldHandleOnResume = true
            permissionLauncher.launch(permissions)
        }
    }

    val openSettings: () -> Unit = {
        shouldHandleOnResume = true
        permissionManager.openAppSettings()
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (shouldHandleOnResume) {
            shouldHandleOnResume = false
            val previous = statusState.value
            updateStatus()
            val current = statusState.value
            if (current != previous && current.hasPermission && current.hasSelections) {
                currentPermissionChanged()
            }
        }
    }

    val dialogs: @Composable () -> Unit = {
        if (showSettingsDialog) {
            GalleryPermissionSettingsDialog(
                onDismissRequest = { showSettingsDialog = false },
                onConfirmClick = {
                    showSettingsDialog = false
                    openSettings()
                },
            )
        }
    }

    return GalleryPermissionRequestState(
        statusState = statusState,
        requestPermission = requestPermission,
        openSettings = openSettings,
        Dialogs = dialogs,
    )
}

@Stable
internal class GalleryPermissionRequestState internal constructor(
    private val statusState: State<GalleryPermissionStatus>,
    val requestPermission: () -> Unit,
    val openSettings: () -> Unit,
    val Dialogs: @Composable () -> Unit,
) {
    val hasPermission: Boolean get() = statusState.value.hasPermission
    val hasSelections: Boolean get() = statusState.value.hasSelections
}
