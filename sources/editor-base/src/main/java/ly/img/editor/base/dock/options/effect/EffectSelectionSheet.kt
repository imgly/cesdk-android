package ly.img.editor.base.dock.options.effect

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ly.img.editor.base.dock.options.properties.PropertiesSheet
import ly.img.editor.base.dock.options.properties.PropertyColorPicker
import ly.img.editor.base.engine.Property
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.ui.SheetHeader
import ly.img.editor.core.ui.iconpack.Filteradjustments
import ly.img.editor.core.ui.iconpack.IconPack
import ly.img.editor.core.ui.library.SelectableAssetList
import ly.img.editor.core.ui.library.SelectableAssetListProvider
import ly.img.editor.core.ui.library.state.WrappedAsset

@Composable
fun EffectSelectionSheet(
    uiState: EffectUiState,
    onColorPickerActiveChanged: (active: Boolean) -> Unit,
    onEvent: (EditorEvent) -> Unit,
) {
    var screenState by remember { mutableStateOf<ScreenState>(ScreenState.Main) }
    val listState = rememberLazyListState()
    var assets by remember {
        mutableStateOf<List<WrappedAsset>>(emptyList())
    }
    var selectedAsset by remember {
        mutableStateOf<WrappedAsset?>(null)
    }
    // SelectableAssetListProvider is required to update title when in AdjustmentPage mode
    SelectableAssetListProvider(uiState.libraryCategory) {
        assets = it
    }
    LaunchedEffect(assets, uiState.selectedAssetKey) {
        selectedAsset = assets.firstOrNull { uiState.getAssetKey(it) == uiState.selectedAssetKey }
    }
    LaunchedEffect(uiState.selectedAssetKey) {
        val effect = uiState.effect
        if ((effect == null || effect.properties.isEmpty()) && screenState !is ScreenState.Main) {
            // Close adjustments when new effect has no properties.
            screenState = ScreenState.Main
        }
    }
    Column {
        when {
            screenState is ScreenState.Main -> {
                SheetHeader(
                    title = stringResource(id = uiState.titleRes),
                    onClose = { onEvent(EditorEvent.Sheet.Close(animate = true)) },
                )
                SelectableAssetList(
                    modifier = Modifier.navigationBarsPadding(),
                    selection = uiState.selectedAssetKey,
                    selectionKey = { uiState.getAssetKey(it) ?: "" },
                    libraryCategory = uiState.libraryCategory,
                    listState = listState,
                    selectedIcon = {
                        if (uiState.effect?.properties?.isNotEmpty() == true) {
                            IconPack.Filteradjustments
                        } else {
                            null
                        }
                    },
                    onAssetSelected = {
                        BlockEvent.OnReplaceEffect(
                            wrappedAsset = it,
                            libraryCategory = uiState.libraryCategory,
                        ).let(onEvent)
                    },
                    onAssetReselected = {
                        if (uiState.effect != null && uiState.effect.properties.isNotEmpty()) {
                            screenState = ScreenState.AdjustmentPage
                        }
                    },
                    onAssetLongClick = {},
                )
            }

            screenState is ScreenState.AdjustmentPage && uiState.effect != null -> {
                PropertiesSheet(
                    title = selectedAsset?.asset?.label ?: "",
                    designBlockWithProperties = uiState.effect,
                    onBack = { screenState = ScreenState.Main },
                    onEvent = onEvent,
                    onOpenColorPicker = {
                        onColorPickerActiveChanged(true)
                        screenState = ScreenState.ColorPicker(
                            property = it.property,
                        )
                    },
                )
            }

            screenState is ScreenState.ColorPicker && uiState.effect != null -> {
                val localScreenState = screenState as ScreenState.ColorPicker
                val propertyAndValue = remember(uiState.effect, localScreenState.property) {
                    uiState.effect.properties.first { it.property == localScreenState.property }
                }
                PropertyColorPicker(
                    designBlock = uiState.effect.designBlock,
                    propertyAndValue = propertyAndValue,
                    onBack = {
                        onColorPickerActiveChanged(false)
                        screenState = ScreenState.AdjustmentPage
                    },
                    onEvent = onEvent,
                )
            }
        }
    }
}

sealed interface ScreenState {
    data object Main : ScreenState

    data object AdjustmentPage : ScreenState

    data class ColorPicker(
        val property: Property,
    ) : ScreenState
}
