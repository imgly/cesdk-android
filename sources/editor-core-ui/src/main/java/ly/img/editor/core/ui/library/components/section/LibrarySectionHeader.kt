package ly.img.editor.core.ui.library.components.section

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import ly.img.editor.core.R
import ly.img.editor.core.iconpack.AddCameraBackground
import ly.img.editor.core.iconpack.Plus
import ly.img.editor.core.library.LibraryContent
import ly.img.editor.core.library.data.GalleryPermissionManager
import ly.img.editor.core.library.data.UploadAssetSourceType
import ly.img.editor.core.ui.iconpack.Arrowright
import ly.img.editor.core.ui.iconpack.IconPack
import ly.img.editor.core.ui.iconpack.Permission
import ly.img.editor.core.ui.iconpack.Photolibraryoutline
import ly.img.editor.core.ui.iconpack.Videolibraryoutline
import ly.img.editor.core.ui.library.components.ClipMenuItem
import ly.img.editor.core.ui.permissions.PermissionManager
import ly.img.editor.core.ui.utils.lifecycle.LifecycleEventEffect
import ly.img.editor.core.iconpack.IconPack as CoreIconPack

@Composable
internal fun LibrarySectionHeader(
    item: LibrarySectionItem.Header,
    onDrillDown: (LibraryContent) -> Unit,
    launchGetContent: (String, UploadAssetSourceType) -> Unit,
    launchCamera: (Boolean) -> Unit,
    onPermissionChanged: () -> Unit = {},
) {
    val context = LocalContext.current
    Row(
        modifier = Modifier
            .padding(start = 16.dp, end = 8.dp)
            .fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = stringResource(id = item.titleRes),
            style = MaterialTheme.typography.titleMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )

        val uploadAssetSource = item.uploadAssetSourceType
        if (uploadAssetSource != null) {
            UploadButton(
                uploadAssetSource = uploadAssetSource,
                launchGetContent = launchGetContent,
                launchCamera = launchCamera,
                mimeTypeFilter = uploadAssetSource.mimeTypeFilter,
                modifier = Modifier.padding(end = 8.dp),
            )
        }

        val galleryType = item.systemGalleryAssetSourceType
        val galleryPermissionGranted = galleryType?.let {
            GalleryPermissionManager.hasPermission(context, it.mimeTypeFilter)
        } ?: true
        val galleryPermissionRequest = if (!galleryPermissionGranted && galleryType != null) {
            rememberGalleryPermissionRequest(
                mimeTypeFilter = galleryType.mimeTypeFilter,
                onPermissionChanged = onPermissionChanged,
            )
        } else {
            null
        }
        galleryPermissionRequest?.Dialogs?.invoke()

        if (galleryType != null && galleryPermissionGranted) {
            SystemGalleryAddButton(
                mimeTypeFilter = galleryType.mimeTypeFilter,
                launchCamera = launchCamera,
                onPermissionChanged = onPermissionChanged,
                openSettings = galleryPermissionRequest?.openSettings,
                modifier = Modifier.padding(end = 8.dp),
            )
        }

        if (item.expandContent != null) {
            val lacksPermission = galleryType != null && !galleryPermissionGranted
            TextButton(
                onClick = {
                    if (lacksPermission) {
                        galleryPermissionRequest?.requestPermission?.invoke()
                    } else {
                        onDrillDown(item.expandContent)
                    }
                },
            ) {
                val countText = when {
                    lacksPermission -> stringResource(R.string.ly_img_editor_asset_library_button_permissions)
                    item.count != null -> {
                        val count = item.count
                        if (count!! > 999) stringResource(id = R.string.ly_img_editor_asset_library_button_more) else count.toString()
                    }
                    else -> ""
                }
                Text(
                    text = countText,
                    style = MaterialTheme.typography.labelLarge,
                )
                Spacer(modifier = Modifier.width(10.dp))
                Icon(IconPack.Arrowright, contentDescription = null)
            }
        }
    }
}

@Composable
private fun SystemGalleryAddButton(
    mimeTypeFilter: String?,
    launchCamera: (Boolean) -> Unit,
    onPermissionChanged: () -> Unit,
    openSettings: (() -> Unit)? = null,
    modifier: Modifier,
) {
    val context = LocalContext.current
    var showMenu by remember { mutableStateOf(false) }
    val currentPermission = { GalleryPermissionManager.hasPermission(context, mimeTypeFilter) }
    var lastPermissionState by remember { mutableStateOf(currentPermission()) }
    var resumeCheck by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions(),
    ) { grantedMap ->
        val granted = grantedMap.values.any { it }
        if (granted) onPermissionChanged()
        lastPermissionState = currentPermission()
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

    Box {
        TextButton(
            modifier = modifier,
            onClick = { showMenu = true },
        ) {
            Icon(CoreIconPack.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = stringResource(R.string.ly_img_editor_asset_library_button_add),
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        DropdownMenu(
            expanded = showMenu,
            onDismissRequest = { showMenu = false },
        ) {
            val isVideoMimeType = mimeTypeFilter?.startsWith("video") == true || mimeTypeFilter?.startsWith("*") != false
            val isImageMimeType = mimeTypeFilter?.startsWith("image") == true || mimeTypeFilter?.startsWith("*") != false

            // Select more Photos (only when not full access)
            if (GalleryPermissionManager.mode != GalleryPermissionManager.Mode.ALL) {
                ClipMenuItem(
                    textResourceId = if (isVideoMimeType) {
                        R.string.ly_img_editor_asset_library_button_choose_video
                    } else {
                        R.string.ly_img_editor_asset_library_button_choose_photo
                    },
                    icon = if (isVideoMimeType) IconPack.Videolibraryoutline else IconPack.Photolibraryoutline,
                ) {
                    val perms = GalleryPermissionManager.requiredPermission(mimeTypeFilter)
                        ?.filterNotNull()
                        ?.toTypedArray()
                        ?: emptyArray()
                    permissionLauncher.launch(perms)
                }

                ClipMenuItem(
                    textResourceId = R.string.ly_img_editor_asset_library_button_change_permissions,
                    icon = IconPack.Permission,
                ) {
                    if (openSettings != null) {
                        openSettings()
                    } else {
                        resumeCheck = true
                        lastPermissionState = currentPermission()
                        PermissionManager(context).openAppSettings()
                    }
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

@Composable
private fun UploadButton(
    uploadAssetSource: UploadAssetSourceType,
    launchGetContent: (String, UploadAssetSourceType) -> Unit,
    launchCamera: (Boolean) -> Unit,
    mimeTypeFilter: String,
    modifier: Modifier,
) {
    val isAudioMimeType = mimeTypeFilter.isAudioMimeType()
    var showUploadMenu by remember { mutableStateOf(false) }
    Box {
        TextButton(
            modifier = modifier,
            onClick = {
                if (isAudioMimeType) {
                    launchGetContent(mimeTypeFilter, uploadAssetSource)
                } else {
                    showUploadMenu = true
                }
            },
        ) {
            Icon(CoreIconPack.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = stringResource(R.string.ly_img_editor_asset_library_button_add),
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }

        if (!isAudioMimeType) {
            DropdownMenu(
                expanded = showUploadMenu,
                onDismissRequest = {
                    showUploadMenu = false
                },
            ) {
                val isVideoMimeType = mimeTypeFilter.isVideoMimeType()
                ClipMenuItem(
                    textResourceId = if (isVideoMimeType) {
                        R.string.ly_img_editor_asset_library_button_choose_video
                    } else {
                        R.string.ly_img_editor_asset_library_button_choose_photo
                    },
                    icon = if (isVideoMimeType) IconPack.Videolibraryoutline else IconPack.Photolibraryoutline,
                ) {
                    launchGetContent(mimeTypeFilter, uploadAssetSource)
                }
                ClipMenuItem(
                    textResourceId = if (isVideoMimeType) {
                        R.string.ly_img_editor_asset_library_button_take_video
                    } else {
                        R.string.ly_img_editor_asset_library_button_take_photo
                    },
                    icon = CoreIconPack.AddCameraBackground,
                    onClick = {
                        showUploadMenu = false
                        launchCamera(isVideoMimeType)
                    },
                )
            }
        }
    }
}

private fun String.isAudioMimeType() = startsWith("audio")

private fun String.isVideoMimeType() = startsWith("video")
