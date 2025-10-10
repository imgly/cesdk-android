package ly.img.editor.core.ui.library.components.section

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.Lifecycle
import ly.img.editor.core.library.data.GalleryPermissionManager
import ly.img.editor.core.library.data.SystemGalleryAssetSourceType
import ly.img.editor.core.library.data.UploadAssetSourceType
import ly.img.editor.core.ui.library.RequireUserPermission
import ly.img.editor.core.ui.library.state.AssetLibraryUiState
import ly.img.editor.core.ui.library.state.WrappedAsset
import ly.img.editor.core.ui.library.util.LibraryEvent
import ly.img.editor.core.ui.utils.lifecycle.LifecycleEventEffect

@Composable
internal fun LibrarySectionColumn(
    uiState: AssetLibraryUiState,
    onAssetClick: (WrappedAsset) -> Unit,
    onLibraryEvent: (LibraryEvent) -> Unit,
    launchGetContent: (String, UploadAssetSourceType) -> Unit,
    launchCamera: (Boolean) -> Unit,
) {
    val context = LocalContext.current
    val nestedScrollConnection = remember(uiState.libraryCategory) {
        object : NestedScrollConnection {
            override fun onPreScroll(
                available: Offset,
                source: NestedScrollSource,
            ): Offset {
                onLibraryEvent(LibraryEvent.OnEnterSearchMode(false, uiState.libraryCategory))
                return Offset.Zero
            }
        }
    }

    var lastPermissionVersion by remember { mutableStateOf(GalleryPermissionManager.permissionVersion) }
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        val filters = uiState.sectionItems.flatMap { item ->
            when (item) {
                is LibrarySectionItem.Header -> listOfNotNull(item.systemGalleryAssetSourceType?.mimeTypeFilter)
                is LibrarySectionItem.Content -> item.sourceTypes.mapNotNull {
                    (it as? SystemGalleryAssetSourceType)?.mimeTypeFilter
                }
                else -> emptyList()
            }
        }.toSet()
        if (filters.isEmpty()) {
            GalleryPermissionManager.hasPermission(context, null)
        } else {
            filters.forEach { GalleryPermissionManager.hasPermission(context, it) }
        }
        val currentVersion = GalleryPermissionManager.permissionVersion
        if (currentVersion != lastPermissionVersion) {
            lastPermissionVersion = currentVersion
            onLibraryEvent(LibraryEvent.OnFetch(uiState.libraryCategory))
        }
    }

    LazyColumn(
        modifier = Modifier
            .nestedScroll(nestedScrollConnection)
            .testTag(tag = "LibrarySectionColumn")
            .fillMaxSize(),
    ) {
        items(uiState.sectionItems, key = { it.id }, contentType = { it.javaClass }) { sectionItem ->
            when (sectionItem) {
                is LibrarySectionItem.Header ->
                    LibrarySectionHeader(
                        item = sectionItem,
                        onDrillDown = { content ->
                            LibraryEvent.OnDrillDown(
                                libraryCategory = uiState.libraryCategory,
                                expandContent = content,
                            ).let { onLibraryEvent(it) }
                        },
                        launchGetContent = launchGetContent,
                        launchCamera = launchCamera,
                        onPermissionChanged = {
                            onLibraryEvent(LibraryEvent.OnFetch(uiState.libraryCategory))
                        },
                    )

                is LibrarySectionItem.Content -> {
                    val galleryAssetSource = sectionItem.sourceTypes.find { it is SystemGalleryAssetSourceType }
                    val permissions = (galleryAssetSource as? SystemGalleryAssetSourceType)?.let {
                        GalleryPermissionManager.requiredPermission(it.mimeTypeFilter)
                    }
                    RequireUserPermission(
                        permissions = permissions,
                        mimeTypeFilter = (galleryAssetSource as? SystemGalleryAssetSourceType)?.mimeTypeFilter,
                        permissionGranted = {
                            onLibraryEvent(LibraryEvent.OnFetch(uiState.libraryCategory))
                        },
                    ) {
                        LibrarySectionContent(
                            sectionItem = sectionItem,
                            onAssetClick = onAssetClick,
                            onAssetLongClick = { wrappedAsset ->
                                onLibraryEvent(LibraryEvent.OnAssetLongClick(wrappedAsset))
                            },
                            onSeeAllClick = { content ->
                                LibraryEvent.OnDrillDown(
                                    libraryCategory = uiState.libraryCategory,
                                    expandContent = content,
                                ).let { onLibraryEvent(it) }
                            },
                            onPermissionChanged = {
                                onLibraryEvent(LibraryEvent.OnFetch(uiState.libraryCategory))
                            },
                        )
                    }
                }

                is LibrarySectionItem.ContentLoading ->
                    LibrarySectionContentLoadingContent(
                        assetType = sectionItem.section.assetType,
                    )

                is LibrarySectionItem.Error ->
                    LibrarySectionErrorContent(
                        assetType = sectionItem.assetType,
                    )

                is LibrarySectionItem.Loading -> {}
            }
        }
    }
}
