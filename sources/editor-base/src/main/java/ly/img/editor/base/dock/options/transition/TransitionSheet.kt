package ly.img.editor.base.dock.options.transition

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ly.img.editor.base.dock.BottomSheetContent
import ly.img.editor.base.dock.options.properties.PropertiesSheet
import ly.img.editor.base.dock.options.properties.PropertyColorPicker
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.core.R
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.ui.SheetHeader
import ly.img.editor.core.ui.iconpack.Filteradjustments
import ly.img.editor.core.ui.iconpack.IconPack
import ly.img.editor.core.ui.library.SimpleSelectableAssetList
import ly.img.editor.core.ui.library.localizedLabel

@Composable
fun TransitionSheet(
    uiState: TransitionUiState,
    onColorPickerActiveChanged: (active: Boolean) -> Unit,
    onEvent: (EditorEvent) -> Unit,
) {
    val listState = rememberLazyListState()
    var screenState by remember { mutableStateOf<ScreenState>(ScreenState.Main) }
    val selected = uiState.selectedTransition
    LaunchedEffect(selected) {
        if ((selected == null || selected.properties.isEmpty()) && screenState !is ScreenState.Main) {
            screenState = ScreenState.Main
        }
    }
    Column(
        modifier = Modifier.navigationBarsPadding(),
    ) {
        when {
            screenState is ScreenState.Main -> {
                SheetHeader(
                    title = stringResource(R.string.ly_img_editor_sheet_transition_title),
                    onClose = {
                        onEvent(EditorEvent.Sheet.Close(animate = true))
                    },
                )
                SimpleSelectableAssetList(
                    modifier = Modifier,
                    listState = listState,
                    listId = "transitions",
                    assets = uiState.transitions,
                    thumbnail = { "${uiState.thumbnailsBaseUri}/${it.asset.id.substringAfterLast(".")}.png" },
                    selectedIcon = { if (selected?.properties?.isNotEmpty() == true) IconPack.Filteradjustments else null },
                    onAssetSelected = {
                        onEvent(BlockEvent.OnReplaceTransition(TransitionUiState.SOURCE_ID, uiState.outgoingBlock, it.asset))
                    },
                    onAssetReselected = {
                        if (selected?.properties?.isNotEmpty() == true) {
                            screenState = ScreenState.PropertiesPage
                        }
                    },
                    onAssetLongClick = {},
                )
                when {
                    selected != null -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            onClick = { onEvent(BlockEvent.OnApplyTransitionToTrack(uiState.outgoingBlock)) },
                        ) {
                            Text(stringResource(R.string.ly_img_editor_sheet_transition_button_apply_to_all_clips_in_track))
                        }
                    }
                    uiState.hasTransitionsInTrack -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            modifier = Modifier
                                .padding(horizontal = 16.dp)
                                .fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError,
                            ),
                            onClick = { onEvent(BlockEvent.OnRemoveTransitionsFromTrack(uiState.outgoingBlock)) },
                        ) {
                            Text(stringResource(R.string.ly_img_editor_sheet_transition_button_remove_all_transitions_in_track))
                        }
                    }
                }
            }

            screenState is ScreenState.PropertiesPage && selected != null -> {
                PropertiesSheet(
                    title = selected.asset?.localizedLabel().orEmpty(),
                    designBlockWithProperties = selected,
                    onBack = { screenState = ScreenState.Main },
                    onEvent = { event ->
                        onEvent(event)
                        if (event is BlockEvent.OnChangeFinish) {
                            onEvent(BlockEvent.OnPreviewTransition(uiState.outgoingBlock))
                        }
                    },
                    onOpenColorPicker = {
                        onColorPickerActiveChanged(true)
                        screenState = ScreenState.ColorPicker(it.property.keys.first())
                    },
                )
            }

            screenState is ScreenState.ColorPicker && selected != null -> {
                val localScreenState = screenState as ScreenState.ColorPicker
                val propertyAndValue = remember(selected, localScreenState.propertyKey) {
                    selected.properties.firstOrNull { it.property.keys.first() == localScreenState.propertyKey }
                }
                if (propertyAndValue == null) {
                    LaunchedEffect(localScreenState) {
                        onColorPickerActiveChanged(false)
                        screenState = ScreenState.PropertiesPage
                    }
                } else {
                    PropertyColorPicker(
                        designBlock = selected.designBlock,
                        propertyAndValue = propertyAndValue,
                        onBack = {
                            onColorPickerActiveChanged(false)
                            screenState = ScreenState.PropertiesPage
                        },
                        onEvent = { event ->
                            onEvent(event)
                            if (event is BlockEvent.OnChangeFinish) {
                                onEvent(BlockEvent.OnPreviewTransition(uiState.outgoingBlock))
                            }
                        },
                    )
                }
            }
        }
    }
}

private sealed interface ScreenState {
    data object Main : ScreenState

    data object PropertiesPage : ScreenState

    data class ColorPicker(
        val propertyKey: String,
    ) : ScreenState
}

class TransitionBottomSheetContent(
    override val type: SheetType,
    val uiState: TransitionUiState,
) : BottomSheetContent
