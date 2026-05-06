package ly.img.editor.core.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import ly.img.editor.core.ui.GradientCard
import ly.img.editor.core.ui.iconpack.IconPack
import ly.img.editor.core.ui.iconpack.None
import ly.img.editor.core.ui.library.components.LibraryImageCard
import ly.img.editor.core.ui.library.components.asset.SelectableAssetWrapper
import ly.img.editor.core.ui.library.state.WrappedAsset
import ly.img.editor.core.ui.utils.centerSelectedItem

@Composable
fun SimpleSelectableAssetList(
    modifier: Modifier,
    listId: String,
    listState: LazyListState,
    assets: List<WrappedAsset>,
    selectedIcon: (WrappedAsset) -> ImageVector?,
    onAssetSelected: (WrappedAsset) -> Unit,
    onAssetReselected: (WrappedAsset) -> Unit,
    onAssetLongClick: (WrappedAsset) -> Unit,
    thumbnail: (WrappedAsset) -> String = { it.asset.getThumbnailUri() },
) {
    val isInitialPositionSettled = remember(listId) {
        mutableStateOf(false)
    }

    LaunchedEffect(listState, assets) {
        listState.centerSelectedItem(
            index = assets.indexOfFirst { it.asset.active },
            animate = true,
            isInitialPositionSettled = isInitialPositionSettled,
        )
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
            items(assets) { wrappedAsset ->
                Column(
                    modifier = Modifier.padding(end = if (wrappedAsset.isNone) 16.dp else 0.dp),
                ) {
                    Column(
                        modifier = Modifier
                            .wrapContentSize()
                            .padding(bottom = 8.dp),
                    ) {
                        val isSelected = remember(wrappedAsset, isInitialPositionSettled.value) {
                            isInitialPositionSettled.value && wrappedAsset.asset.active
                        }
                        SelectableAssetWrapper(
                            isSelected = isSelected,
                            selectedIcon = selectedIcon(wrappedAsset),
                            selectedIconTint = Color.White,
                        ) {
                            if (wrappedAsset.isNone) {
                                GradientCard(
                                    modifier = Modifier.size(80.dp),
                                    onClick = {
                                        if (isSelected) {
                                            onAssetReselected(wrappedAsset)
                                        } else {
                                            onAssetSelected(wrappedAsset)
                                        }
                                    },
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
                            } else {
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
                                    contentScale = ContentScale.Fit,
                                    tintImages = false,
                                )
                            }
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
