package ly.img.editor.core.ui.library.components.section

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ly.img.editor.core.R
import ly.img.editor.core.iconpack.Plus
import ly.img.editor.core.library.AssetType
import ly.img.editor.core.library.LibraryContent
import ly.img.editor.core.library.data.GalleryPermissionManager
import ly.img.editor.core.library.data.SystemGalleryAssetSourceType
import ly.img.editor.core.ui.GradientCard
import ly.img.editor.core.ui.library.components.asset.AssetColumn
import ly.img.editor.core.ui.library.components.asset.AssetRow
import ly.img.editor.core.ui.library.state.WrappedAsset
import ly.img.editor.core.ui.library.util.AssetLibraryUiConfig
import ly.img.editor.core.iconpack.IconPack as CoreIcons

@Composable
internal fun LibrarySectionContent(
    sectionItem: LibrarySectionItem.Content,
    onAssetClick: (WrappedAsset) -> Unit,
    onAssetLongClick: (WrappedAsset) -> Unit,
    onSeeAllClick: (LibraryContent) -> Unit,
    onPermissionChanged: () -> Unit,
    launchCamera: (Boolean) -> Unit,
) {
    val gallerySource = sectionItem.sourceTypes.find { it is SystemGalleryAssetSourceType } as? SystemGalleryAssetSourceType
    val permissionRequest = gallerySource?.let {
        rememberGalleryPermissionRequest(
            mimeTypeFilter = it.mimeTypeFilter,
            onPermissionChanged = onPermissionChanged,
        )
    }
    permissionRequest?.Dialogs?.invoke()
    val isManualGallery = GalleryPermissionManager.isManualMode
    val galleryPermissionGranted = permissionRequest?.let { it.hasPermission && it.hasSelections } ?: true
    val useManualAddTile = isManualGallery && gallerySource != null
    val emptyText = when {
        useManualAddTile -> null
        !galleryPermissionGranted -> stringResource(R.string.ly_img_editor_asset_library_label_grant_permissions)
        else -> null
    }
    val emptyClick: (() -> Unit)? = when {
        useManualAddTile -> null
        !galleryPermissionGranted -> permissionRequest?.requestPermission
        else -> null
    }
    val manualLeadingContent: (@Composable () -> Unit)? = if (useManualAddTile) {
        {
            ManualGalleryAddTile(
                mimeTypeFilter = gallerySource?.mimeTypeFilter ?: "*/*",
                onPermissionChanged = onPermissionChanged,
                launchCamera = launchCamera,
                assetType = sectionItem.assetType,
            )
        }
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
                onEmptyClick = emptyClick,
                leadingContent = manualLeadingContent,
            )
    }
}

@Composable
private fun ManualGalleryAddTile(
    mimeTypeFilter: String,
    onPermissionChanged: () -> Unit,
    launchCamera: (Boolean) -> Unit,
    assetType: AssetType,
) {
    SystemGalleryAddMenu(
        mimeTypeFilter = mimeTypeFilter,
        launchCamera = launchCamera,
        onPermissionChanged = onPermissionChanged,
    ) { openTrigger ->
        ManualGalleryAddCard(
            onClick = openTrigger,
            assetType = assetType,
        )
    }
}

@Composable
private fun ManualGalleryAddCard(
    onClick: () -> Unit,
    assetType: AssetType,
) {
    val cardHeight = AssetLibraryUiConfig.contentRowHeight(assetType)
    GradientCard(
        modifier = Modifier
            .height(cardHeight)
            .aspectRatio(1f),
        onClick = onClick,
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Icon(imageVector = CoreIcons.Plus, contentDescription = null)
            Text(
                modifier = Modifier.padding(vertical = 2.dp),
                text = stringResource(R.string.ly_img_editor_asset_library_button_add),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
