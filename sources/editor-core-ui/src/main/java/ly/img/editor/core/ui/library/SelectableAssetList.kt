package ly.img.editor.core.ui.library

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentSize
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ly.img.editor.core.R
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
    selectedIcon: (WrappedAsset) -> ImageVector?,
    onAssetSelected: (WrappedAsset?) -> Unit,
    onAssetReselected: (WrappedAsset) -> Unit,
    onAssetLongClick: (WrappedAsset?) -> Unit,
    thumbnail: (WrappedAsset) -> String = { it.asset.getThumbnailUri() },
) {
    val viewModel = viewModel<LibraryViewModel>()
    val uiState = remember(libraryCategory) {
        viewModel.getAssetLibraryUiState(libraryCategory)
    }.collectAsState()
    var isInitialPositionSettled by remember(libraryCategory) {
        mutableStateOf(false)
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
        uiState.value.sectionItems.forEach { section ->
            if (section !is LibrarySectionItem.Content) return@forEach
            val assetIndex = section.wrappedAssets.indexOfFirst { wrappedAsset ->
                selectionKey(wrappedAsset) == selection
            }
            if (assetIndex != -1) {
                centerSelectedItem(index = sectionStartIndex + assetIndex, animate = true)
                return@LaunchedEffect
            }
            sectionStartIndex += section.wrappedAssets.size + 1
        }
        centerSelectedItem(index = sectionStartIndex, animate = true)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight(),
    ) {
        LazyRow(
            modifier = Modifier
                .height(130.dp)
                .padding(top = 8.dp, bottom = 8.dp)
                .fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(0.dp),
            state = listState,
        ) {
            item {
                Column {
                    Column(
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(bottom = 8.dp),
                    ) {
                        SelectableAssetWrapper(
                            isSelected = selection == null,
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
                    }
                    Text(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        text = stringResource(R.string.ly_img_editor_remove),
                        style = MaterialTheme.typography.labelMedium,
                    )
                }
            }

            uiState.value.sectionItems.forEach { sectionItem ->
                item {
                    Spacer(modifier = Modifier.width(16.dp))
                }
                if (sectionItem is LibrarySectionItem.Content) {
                    val assetType = sectionItem.assetType
                    val wrappedAssets = sectionItem.wrappedAssets
                    items(wrappedAssets) { wrappedAsset ->
                        Column {
                            Column(
                                modifier = Modifier
                                    .wrapContentSize()
                                    .padding(bottom = 8.dp),
                            ) {
                                val isSelected = remember(wrappedAsset, selection, isInitialPositionSettled) {
                                    isInitialPositionSettled && selectionKey(wrappedAsset) == selection
                                }
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

                            Text(
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                text = wrappedAsset.asset.label ?: "",
                                style = MaterialTheme.typography.labelMedium,
                            )
                        }
                    }
                }
            }
        }
    }
}
