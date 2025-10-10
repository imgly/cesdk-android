package ly.img.editor.core.ui.library.components.section

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import ly.img.editor.core.R
import ly.img.editor.core.library.AssetType
import ly.img.editor.core.library.LibraryContent
import ly.img.editor.core.library.data.SystemGalleryAssetSourceType
import ly.img.editor.core.ui.library.components.asset.AssetColumn
import ly.img.editor.core.ui.library.components.asset.AssetRow
import ly.img.editor.core.ui.library.state.WrappedAsset

@Composable
internal fun LibrarySectionContent(
    sectionItem: LibrarySectionItem.Content,
    onAssetClick: (WrappedAsset) -> Unit,
    onAssetLongClick: (WrappedAsset) -> Unit,
    onSeeAllClick: (LibraryContent) -> Unit,
    onPermissionChanged: () -> Unit,
) {
    val context = LocalContext.current
    val gallerySource = sectionItem.sourceTypes.find { it is SystemGalleryAssetSourceType } as? SystemGalleryAssetSourceType
    val permissionRequest = gallerySource?.let {
        rememberGalleryPermissionRequest(
            mimeTypeFilter = it.mimeTypeFilter,
            onPermissionChanged = onPermissionChanged,
        )
    }
    permissionRequest?.Dialogs?.invoke()
    val galleryPermissionGranted = permissionRequest?.let { it.hasPermission && it.hasSelections } ?: true
    val emptyText = if (!galleryPermissionGranted) {
        stringResource(R.string.ly_img_editor_asset_library_label_grant_permissions)
    } else {
        null
    }

    when (val assetType = sectionItem.assetType) {
        AssetType.Audio, AssetType.Text ->
            AssetColumn(
                wrappedAssets = sectionItem.wrappedAssets,
                assetType = assetType,
                onAssetClick = onAssetClick,
                onAssetLongClick = onAssetLongClick,
            )

        else ->
            AssetRow(
                wrappedAssets = sectionItem.wrappedAssets,
                expandContent = sectionItem.expandContent,
                assetType = assetType,
                onAssetClick = onAssetClick,
                onAssetLongClick = onAssetLongClick,
                onSeeAllClick = onSeeAllClick,
                emptyText = emptyText,
                onEmptyClick = permissionRequest?.requestPermission,
            )
    }
}
