package ly.img.editor.core.ui.library

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import ly.img.editor.core.library.data.GalleryPermissionManager
import ly.img.editor.core.ui.library.components.section.rememberGalleryPermissionRequest
import ly.img.editor.core.ui.permissions.PermissionManager.Companion.hasAnyPermission

@Composable
internal fun RequireUserPermission(
    permissions: Array<String?>?,
    mimeTypeFilter: String?,
    permissionGranted: () -> Unit,
    content: @Composable () -> Unit,
) {
    val nonNullPermission = permissions?.filterNotNull()?.toTypedArray()
    val context = LocalContext.current
    val hasPermissionInitial = remember(mimeTypeFilter, nonNullPermission) {
        when {
            mimeTypeFilter != null -> GalleryPermissionManager.hasPermission(context, mimeTypeFilter)
            nonNullPermission != null -> nonNullPermission.isEmpty() || context.hasAnyPermission(nonNullPermission)
            else -> true
        }
    }
    var hasPermission by remember { mutableStateOf(hasPermissionInitial) }
    if (hasPermission) {
        content()
    } else {
        if (mimeTypeFilter != null) {
            val permissionRequest = rememberGalleryPermissionRequest(
                mimeTypeFilter = mimeTypeFilter,
                onPermissionChanged = {
                    hasPermission = true
                    permissionGranted()
                },
            )
            permissionRequest.Dialogs()
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .clickable {
                        if (GalleryPermissionManager.hasPermission(context, mimeTypeFilter)) {
                            hasPermission = true
                            permissionGranted()
                        } else {
                            permissionRequest.requestPermission()
                        }
                    },
            ) {
                content()
            }
        } else {
            val permissionLauncher = rememberLauncherForActivityResult(
                ActivityResultContracts.RequestMultiplePermissions(),
            ) { result ->
                val isGranted = result.entries.any { it.value }
                if (isGranted) {
                    permissionGranted()
                    hasPermission = true
                }
            }
            Box(
                modifier = Modifier
                    .wrapContentSize()
                    .clickable {
                        if (context.hasAnyPermission(nonNullPermission)) {
                            hasPermission = true
                            permissionGranted()
                        } else {
                            permissionLauncher.launch(nonNullPermission)
                        }
                    },
            ) {
                content()
            }
        }
    }
}
