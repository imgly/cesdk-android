package ly.img.editor.core.ui.library

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ly.img.editor.core.R
import ly.img.editor.core.library.AssetType
import ly.img.editor.core.library.LibraryCategory
import ly.img.editor.core.ui.GradientCard
import ly.img.editor.core.ui.iconpack.IconPack
import ly.img.editor.core.ui.iconpack.None
import ly.img.editor.core.ui.library.components.LibraryImageCard
import ly.img.editor.core.ui.library.components.asset.SelectableAssetWrapper
import ly.img.editor.core.ui.library.components.section.LibrarySectionItem
import ly.img.editor.core.ui.library.state.CategoryLoadState
import ly.img.editor.core.ui.library.state.WrappedAsset
import ly.img.editor.core.ui.library.util.AssetLibraryUiConfig
import ly.img.editor.core.ui.library.util.LibraryEvent

@Composable
fun SelectableAssetList(
    modifier: Modifier,
    selection: String?,
    selectionKey: (WrappedAsset) -> String,
    libraryCategory: LibraryCategory,
    listState: LazyListState,
    selectedIcon: (WrappedAsset) -> ImageVector? = { null },
    onAssetSelected: (WrappedAsset?) -> Unit,
    onAssetReselected: (WrappedAsset) -> Unit = { _ -> },
    onAssetLongClick: (WrappedAsset?) -> Unit = { _ -> },
    addSeparator: Boolean = true,
    hasNoneItem: Boolean = true,
    groupFilter: String? = null,
    thumbnail: (WrappedAsset) -> String = { it.asset.getThumbnailUri() },
) = CustomAssetList(
    modifier = modifier,
    selection = selection,
    selectionKey = selectionKey,
    libraryCategory = libraryCategory,
    listState = listState,
    selectedIcon = selectedIcon,
    onAssetSelected = onAssetSelected,
    onAssetReselected = onAssetReselected,
    onAssetLongClick = onAssetLongClick,
    groupFilter = groupFilter,
    itemContent = {
        ItemContent()
    },
    addSeparator = addSeparator,
    hasNoneItem = hasNoneItem,
    thumbnail = thumbnail,
)

data class GroupItems(
    val assetType: AssetType,
    val assets: List<WrappedAsset>,
)

@Composable
fun CustomAssetList(
    modifier: Modifier,
    selection: String?,
    selectionKey: (WrappedAsset) -> String,
    libraryCategory: LibraryCategory,
    listState: LazyListState,
    selectedIcon: (WrappedAsset) -> ImageVector? = { null },
    onAssetSelected: (WrappedAsset?) -> Unit,
    onAssetReselected: (WrappedAsset) -> Unit = { _ -> },
    onAssetLongClick: (WrappedAsset?) -> Unit = { _ -> },
    addSeparator: Boolean = true,
    hasNoneItem: Boolean = true,
    groupFilter: String? = null,
    contentPadding: PaddingValues = PaddingValues(horizontal = 16.dp),
    thumbnail: (WrappedAsset) -> String = { it.asset.getThumbnailUri() },
    itemContent: @Composable ItemContentPayload.() -> Unit = {
        ItemContent()
    },
) {
    val viewModel = viewModel<LibraryViewModel>()
    val uiState = remember(libraryCategory) {
        viewModel.getAssetLibraryUiState(libraryCategory)
    }.collectAsState()
    var isInitialPositionSettled by remember(libraryCategory) {
        mutableStateOf(false)
    }
    val list = remember(uiState.value.sectionItems, groupFilter) {
        uiState.value.sectionItems.mapNotNull { section ->
            if (section is LibrarySectionItem.Content) {
                val list = if (groupFilter != null) {
                    section.wrappedAssets.filter { it.asset.groups?.contains(groupFilter) == true }
                } else {
                    section.wrappedAssets
                }
                if (list.isNotEmpty()) {
                    GroupItems(
                        assetType = section.assetType,
                        assets = list,
                    )
                } else {
                    null
                }
            } else {
                null
            }
        }
    }
    LaunchedEffect(libraryCategory) {
        if (uiState.value.loadState == CategoryLoadState.Idle) {
            viewModel.onEvent(LibraryEvent.OnFetch(libraryCategory))
        }
    }

    suspend fun centerSelectedItem(
        index: Int,
        animate: Boolean = true,
    ) {
        val center = listState.layoutInfo.viewportEndOffset / 2
        val target = listState.layoutInfo.visibleItemsInfo.firstOrNull {
            it.index == index
        } ?: run {
            listState.scrollToItem(index)
            if (animate) {
                centerSelectedItem(index = index, animate = false)
            }
            return
        }
        val childCenter = target.offset + target.size / 2
        val diff = (childCenter - center).toFloat()
        if (animate && isInitialPositionSettled) {
            listState.animateScrollBy(diff)
        } else {
            listState.scrollBy(diff)
            if (isInitialPositionSettled.not()) {
                isInitialPositionSettled = true
            }
        }
    }

    LaunchedEffect(libraryCategory, selection, uiState.value) {
        if (uiState.value.loadState != CategoryLoadState.Success) return@LaunchedEffect
        if (selection == null) {
            centerSelectedItem(index = 0, animate = true)
            return@LaunchedEffect
        }
        var sectionStartIndex = 2 // "None" element + first section spacer
        list.forEach { section ->
            val assetIndex = section.assets.indexOfFirst { wrappedAsset ->
                selectionKey(wrappedAsset) == selection
            }
            if (assetIndex != -1) {
                centerSelectedItem(index = sectionStartIndex + assetIndex, animate = true)
                return@LaunchedEffect
            }
            sectionStartIndex += section.assets.size + 1
        }
        centerSelectedItem(index = sectionStartIndex, animate = true)
    }

    Surface(
        modifier = modifier
            .height(136.dp)
            .fillMaxWidth()
            .wrapContentHeight(),
    ) {
        LazyRow(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            state = listState,
        ) {
            if (hasNoneItem) {
                item {
                    val payload = remember(selection) {
                        ItemContentPayload(
                            scope = this,
                            wrappedAsset = null,
                            isSelected = selection == null,
                            selectedIcon = selectedIcon,
                            onAssetSelected = onAssetSelected,
                            onAssetReselected = onAssetReselected,
                            onAssetLongClick = onAssetLongClick,
                            thumbnail = thumbnail,
                            assetType = null,
                        )
                    }
                    itemContent(payload)
                }
            }

            list.forEachIndexed { index, groupItem ->
                if (addSeparator && (hasNoneItem || index != 0)) {
                    item {
                        Spacer(modifier = Modifier.width(16.dp))
                    }
                }
                val assetType = groupItem.assetType
                items(groupItem.assets) { wrappedAsset ->
                    val isSelected = remember(wrappedAsset, selection, isInitialPositionSettled) {
                        isInitialPositionSettled && selectionKey(wrappedAsset) == selection
                    }
                    val payload = remember(wrappedAsset, isSelected) {
                        ItemContentPayload(
                            scope = this,
                            wrappedAsset = wrappedAsset,
                            isSelected,
                            selectedIcon = selectedIcon,
                            onAssetSelected = onAssetSelected,
                            onAssetReselected = onAssetReselected,
                            onAssetLongClick = onAssetLongClick,
                            thumbnail = thumbnail,
                            assetType = assetType,
                        )
                    }
                    itemContent(payload)
                }
            }
        }
    }
}

class ItemContentPayload(
    val scope: LazyItemScope,
    val wrappedAsset: WrappedAsset?,
    val isSelected: Boolean,
    val selectedIcon: (WrappedAsset) -> ImageVector?,
    val onAssetSelected: (WrappedAsset?) -> Unit,
    val onAssetReselected: (WrappedAsset) -> Unit,
    val onAssetLongClick: (WrappedAsset?) -> Unit,
    val thumbnail: (WrappedAsset) -> String = { it.asset.getThumbnailUri() },
    val assetType: AssetType? = null,
) : LazyItemScope by scope

val ItemContent: @Composable ItemContentPayload.() -> Unit = {
    Column {
        Column(
            modifier = Modifier
                .width(88.dp)
                .wrapContentHeight(),
        ) {
            if (wrappedAsset == null || assetType == null) {
                SelectableAssetWrapper(
                    isSelected = isSelected,
                    selectedIcon = null,
                ) {
                    GradientCard(
                        modifier = Modifier.size(80.dp),
                        onClick = { onAssetSelected(null) },
                        onLongClick = { },
                    ) {
                        Icon(
                            IconPack.None,
                            contentDescription = null,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(12.dp)
                                .align(Alignment.Center),
                        )
                    }
                }
            } else {
                SelectableAssetWrapper(
                    isSelected = isSelected,
                    selectedIcon = selectedIcon(wrappedAsset),
                    selectedIconTint = Color.White,
                ) {
                    LibraryImageCard(
                        modifier = Modifier.width(80.dp),
                        uri = thumbnail(wrappedAsset),
                        onClick = {
                            if (isSelected) {
                                onAssetReselected(wrappedAsset)
                            } else {
                                onAssetSelected(wrappedAsset)
                            }
                        },
                        onLongClick = { onAssetLongClick(wrappedAsset) },
                        contentPadding = AssetLibraryUiConfig.contentPadding(assetType),
                        contentScale = AssetLibraryUiConfig.contentScale(assetType),
                        tintImages = AssetLibraryUiConfig.shouldTintImages(assetType),
                    )
                }
            }
        }
        Text(
            modifier = Modifier
                .width(88.dp)
                .padding(start = 4.dp, end = 4.dp),
            textAlign = TextAlign.Center,
            text = wrappedAsset?.asset?.label ?: stringResource(R.string.ly_img_editor_asset_library_label_none),
            style = MaterialTheme.typography.labelMedium,
        )
    }
}
