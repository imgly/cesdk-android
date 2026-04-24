package ly.img.editor.base.dock.options.animation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ly.img.editor.base.components.Tabs
import ly.img.editor.base.dock.BottomSheetContent
import ly.img.editor.base.dock.options.properties.PropertiesSheet
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.core.R
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.ui.SheetHeader
import ly.img.editor.core.ui.iconpack.Filteradjustments
import ly.img.editor.core.ui.iconpack.IconPack
import ly.img.editor.core.ui.library.SimpleSelectableAssetList
import ly.img.editor.core.ui.library.getMeta

@Composable
fun AnimationSheet(
    uiState: AnimationUiState,
    onEvent: (EditorEvent) -> Unit,
) {
    val listState = rememberLazyListState()
    var screenState by remember { mutableStateOf<ScreenState>(ScreenState.Main) }
    var selectedTab by remember { mutableStateOf(0) }
    val animationCategory = remember(selectedTab, uiState.categories) {
        uiState.categories[selectedTab].data
    }
    val selectedAnimation = remember(animationCategory) {
        uiState.categories[selectedTab].data.selectedAnimation
    }
    LaunchedEffect(selectedAnimation) {
        if ((selectedAnimation == null || selectedAnimation.properties.isEmpty()) && screenState !is ScreenState.Main) {
            // Close adjustments when new animation has no properties.
            screenState = ScreenState.Main
        }
    }
    Column {
        when {
            screenState is ScreenState.Main -> {
                SheetHeader(
                    title = stringResource(id = R.string.ly_img_editor_sheet_animations_title),
                    onClose = { onEvent(EditorEvent.Sheet.Close(animate = true)) },
                )
                AnimationCategory(
                    category = animationCategory,
                    listState = listState,
                    onEvent = onEvent,
                    onShowProperties = {
                        screenState = ScreenState.PropertiesPage
                    },
                )
                Tabs(
                    modifier = Modifier.navigationBarsPadding(),
                    items = uiState.categories,
                    selectedIndex = selectedTab,
                    onTabSelected = { _, index -> selectedTab = index },
                )
            }
            screenState is ScreenState.PropertiesPage && selectedAnimation != null -> {
                PropertiesSheet(
                    title = selectedAnimation.asset?.label ?: "",
                    designBlockWithProperties = selectedAnimation,
                    onBack = { screenState = ScreenState.Main },
                    onEvent = onEvent,
                    onOpenColorPicker = { },
                )
            }
        }
    }
}

@Composable
private fun AnimationCategory(
    category: AnimationUiState.Category,
    listState: LazyListState,
    onEvent: (EditorEvent) -> Unit,
    onShowProperties: () -> Unit,
) {
    SimpleSelectableAssetList(
        modifier = Modifier,
        listState = listState,
        listId = category.group,
        assets = category.animations,
        thumbnail = { category.thumbnailsBaseUri + "/" + it.asset.getMeta("type") + ".png" },
        selectedIcon = {
            if (category.selectedAnimation?.properties?.isNotEmpty() == true) {
                IconPack.Filteradjustments
            } else {
                null
            }
        },
        onAssetSelected = {
            BlockEvent.OnReplaceAnimation(
                sourceId = category.sourceId,
                asset = it.asset,
            ).let(onEvent)
        },
        onAssetReselected = {
            if (category.selectedAnimation != null && category.selectedAnimation.properties.isNotEmpty()) {
                onShowProperties()
            }
        },
        onAssetLongClick = {},
    )
}

sealed interface ScreenState {
    data object Main : ScreenState

    data object PropertiesPage : ScreenState
}

class AnimationBottomSheetContent(
    override val type: SheetType,
    val uiState: AnimationUiState,
) : BottomSheetContent
