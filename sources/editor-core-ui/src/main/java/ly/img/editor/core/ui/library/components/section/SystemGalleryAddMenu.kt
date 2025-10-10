package ly.img.editor.core.ui.library.components.section

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import ly.img.editor.core.R
import ly.img.editor.core.iconpack.AddCameraBackground
import ly.img.editor.core.library.data.GalleryPermissionManager
import ly.img.editor.core.ui.iconpack.IconPack
import ly.img.editor.core.ui.iconpack.Permission
import ly.img.editor.core.ui.iconpack.Photolibraryoutline
import ly.img.editor.core.ui.iconpack.Videolibraryoutline
import ly.img.editor.core.ui.library.components.ClipMenuItem
import ly.img.editor.core.ui.permissions.PermissionManager
import ly.img.editor.core.ui.utils.lifecycle.LifecycleEventEffect
import ly.img.editor.core.iconpack.IconPack as CoreIconPack

@Composable
internal fun SystemGalleryAddMenu(
    mimeTypeFilter: String?,
    launchCamera: (Boolean) -> Unit,
    onPermissionChanged: () -> Unit = {},
    trigger: @Composable ((() -> Unit) -> Unit),
) {
    val isVideoMimeType = mimeTypeFilter?.startsWith("video") == true || mimeTypeFilter?.startsWith("*") != false
    val isImageMimeType = mimeTypeFilter?.startsWith("image") == true || mimeTypeFilter?.startsWith("*") != false

    var showMenu by remember { mutableStateOf(false) }
    val context = LocalContext.current

    val currentPermission = { GalleryPermissionManager.hasPermission(context, mimeTypeFilter) }
    var lastPermissionState by remember { mutableStateOf(currentPermission()) }
    var resumeCheck by remember { mutableStateOf(false) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grantedMap ->
        val granted = grantedMap.values.any { it }
        if (granted) {
            onPermissionChanged()
            lastPermissionState = currentPermission()
        }
        showMenu = false
    }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        if (resumeCheck) {
            resumeCheck = false
            val current = currentPermission()
            if (current != lastPermissionState) {
                onPermissionChanged()
            }
            lastPermissionState = current
        }
    }

    trigger { showMenu = true }

    val showPermissionEntries = GalleryPermissionManager.mode != GalleryPermissionManager.Mode.ALL

    Box {
        trigger { showMenu = true }
        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            if (showPermissionEntries) {
                ClipMenuItem(
                    textResourceId = if (isVideoMimeType) {
                        R.string.ly_img_editor_asset_library_button_choose_video
                    } else {
                        R.string.ly_img_editor_asset_library_button_choose_photo
                    },
                    icon = if (isVideoMimeType) IconPack.Videolibraryoutline else IconPack.Photolibraryoutline,
                ) {
                    val perms = GalleryPermissionManager.requiredPermission(mimeTypeFilter)
                        .filterNotNull()
                        .toTypedArray()
                    permissionLauncher.launch(perms)
                }

                ClipMenuItem(
                    textResourceId = R.string.ly_img_editor_asset_library_button_change_permissions,
                    icon = IconPack.Permission,
                ) {
                    resumeCheck = true
                    lastPermissionState = currentPermission()
                    PermissionManager(context).openAppSettings()
                    showMenu = false
                }
            }

            if (isImageMimeType) {
                ClipMenuItem(
                    textResourceId = R.string.ly_img_editor_asset_library_button_take_photo,
                    icon = CoreIconPack.AddCameraBackground,
                    onClick = {
                        showMenu = false
                        launchCamera(false)
                    },
                )
            }

            if (isVideoMimeType) {
                ClipMenuItem(
                    textResourceId = R.string.ly_img_editor_asset_library_button_take_video,
                    icon = CoreIconPack.AddCameraBackground,
                    onClick = {
                        showMenu = false
                        launchCamera(true)
                    },
                )
            }
        }
    }
}
