package ly.img.editor.core.ui.library.components.section

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
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
    val manualMode = GalleryPermissionManager.isManualMode

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

    Layout(
        modifier = Modifier
            .padding(start = 16.dp, end = 8.dp)
            .fillMaxWidth(),
        content = {
            Text(
                text = stringResource(id = item.titleRes),
                style = MaterialTheme.typography.titleMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(end = 8.dp),
            )

            val uploadAssetSource = item.uploadAssetSourceType
            if (uploadAssetSource != null) {
                UploadButton(
                    uploadAssetSource = uploadAssetSource,
                    launchGetContent = launchGetContent,
                    launchCamera = launchCamera,
                    mimeTypeFilter = uploadAssetSource.mimeTypeFilter,
                    modifier = Modifier,
                )
            }

            if (galleryType != null && galleryPermissionGranted && !manualMode) {
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
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Icon(IconPack.Arrowright, contentDescription = null)
                }
            }
        },
    ) { measurables, constraints ->
        // measurables: [0] = Text, [1] = UploadButton?, [2] = GalleryButton?, [3] = CountButton?
        val textMeasurable = measurables[0]
        val buttonMeasurables = measurables.drop(1)

        // Get preferred widths
        val textPreferredWidth = textMeasurable.maxIntrinsicWidth(constraints.maxHeight)
        val buttonPreferredWidths = buttonMeasurables.map { it.maxIntrinsicWidth(constraints.maxHeight) }
        val totalButtonsPreferredWidth = buttonPreferredWidths.sum()

        val totalAvailable = constraints.maxWidth
        val totalPreferred = textPreferredWidth + totalButtonsPreferredWidth

        val (textWidth, buttonWidths) = if (totalPreferred <= totalAvailable) {
            // Case 1: Everything fits - give all their preferred width
            textPreferredWidth to buttonPreferredWidths
        } else {
            // Case 2: Not enough space - smart allocation
            // Step 1: Each button gets min(natural width, 25% of total) - whichever is SMALLER
            val twentyFivePercent = (totalAvailable * 0.25f).toInt()
            val buttonMinWidths = buttonPreferredWidths.map { it.coerceAtMost(twentyFivePercent) }
            val totalButtonsMinWidth = buttonMinWidths.sum()

            // Step 2: Text gets remainder, capped at its natural width
            val remainingAfterButtons = totalAvailable - totalButtonsMinWidth
            val textWidth = textPreferredWidth.coerceAtMost(remainingAfterButtons).coerceAtLeast(0)

            // Step 3: Any leftover space goes to buttons that need it, proportionally
            val leftover = totalAvailable - textWidth - totalButtonsMinWidth
            val buttonWidths = if (leftover > 0) {
                // Only distribute to buttons that haven't reached their natural width
                val buttonsNeedingMore = buttonMinWidths.mapIndexed { index, minWidth ->
                    buttonPreferredWidths[index] - minWidth
                }.map { it.coerceAtLeast(0) }
                val totalNeedingMore = buttonsNeedingMore.sum()

                if (totalNeedingMore > 0) {
                    buttonMinWidths.mapIndexed { index, minWidth ->
                        val needed = buttonsNeedingMore[index]
                        if (needed > 0) {
                            val proportion = needed.toFloat() / totalNeedingMore
                            val extra = (leftover * proportion).toInt().coerceAtMost(needed)
                            minWidth + extra
                        } else {
                            minWidth
                        }
                    }
                } else {
                    buttonMinWidths
                }
            } else {
                buttonMinWidths
            }

            textWidth to buttonWidths
        }

        // Measure all elements
        val textPlaceable = textMeasurable.measure(
            constraints.copy(minWidth = 0, maxWidth = textWidth),
        )
        val buttonPlaceables = buttonMeasurables.mapIndexed { index, measurable ->
            measurable.measure(
                constraints.copy(minWidth = 0, maxWidth = buttonWidths[index]),
            )
        }

        val height = maxOf(textPlaceable.height, buttonPlaceables.maxOfOrNull { it.height } ?: 0)

        layout(constraints.maxWidth, height) {
            // Place text at start
            textPlaceable.place(0, (height - textPlaceable.height) / 2)

            // Place buttons at end, right-to-left
            var xOffset = constraints.maxWidth
            buttonPlaceables.asReversed().forEach { placeable ->
                xOffset -= placeable.width
                placeable.place(xOffset, (height - placeable.height) / 2)
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
    val manualMode = GalleryPermissionManager.isManualMode
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

    val pickVisualLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) {
            GalleryPermissionManager.addSelected(uri, context)
            onPermissionChanged()
        }
        showMenu = false
    }

    val isVideoMimeType = mimeTypeFilter?.startsWith("video") == true || mimeTypeFilter?.startsWith("*") != false
    val isImageMimeType = mimeTypeFilter?.startsWith("image") == true || mimeTypeFilter?.startsWith("*") != false

    val manualPickRequest = remember(mimeTypeFilter, isVideoMimeType, isImageMimeType) {
        when {
            isVideoMimeType && !isImageMimeType -> PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly)
            isImageMimeType && !isVideoMimeType -> PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
            else -> PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageAndVideo)
        }
    }

    Box {
        TextButton(
            modifier = modifier,
            onClick = {
                if (manualMode) {
                    pickVisualLauncher.launch(manualPickRequest)
                } else {
                    showMenu = true
                }
            },
        ) {
            Icon(CoreIconPack.Plus, contentDescription = null, modifier = Modifier.size(18.dp))
            Text(
                text = stringResource(R.string.ly_img_editor_asset_library_button_add),
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        if (!manualMode) {
            DropdownMenu(
                expanded = showMenu,
                onDismissRequest = { showMenu = false },
            ) {
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
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
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
