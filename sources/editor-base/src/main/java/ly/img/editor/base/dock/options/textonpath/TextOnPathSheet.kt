package ly.img.editor.base.dock.options.textonpath

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ly.img.editor.base.components.PropertySlider
import ly.img.editor.base.components.ToggleIconButton
import ly.img.editor.base.dock.BottomSheetContent
import ly.img.editor.base.dock.options.format.AlignmentButton
import ly.img.editor.base.dock.options.format.VerticalAlignment
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.core.R
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.ui.SheetHeader
import ly.img.editor.core.ui.UiDefaults
import ly.img.editor.core.ui.iconpack.IconPack
import ly.img.editor.core.ui.iconpack.Reset
import ly.img.editor.core.ui.library.SimpleSelectableAssetList
import ly.img.editor.core.ui.sheetScrollableContentModifier

@Composable
fun TextOnPathSheet(
    uiState: TextOnPathUiState,
    onEvent: (EditorEvent) -> Unit,
) {
    Column(
        modifier = Modifier.navigationBarsPadding(),
    ) {
        SheetHeader(
            title = stringResource(id = R.string.ly_img_editor_sheet_text_on_path_title),
            onClose = { onEvent(EditorEvent.Sheet.Close(animate = true)) },
        )
        SimpleSelectableAssetList(
            modifier = Modifier,
            listState = rememberLazyListState(),
            listId = "curves",
            assets = uiState.curves,
            selectedIcon = { null },
            onAssetSelected = {
                BlockEvent.OnSelectTextOnPath(
                    asset = it.asset.takeUnless { _ -> it.isNone },
                ).let(onEvent)
            },
            onAssetReselected = {},
            onAssetLongClick = {},
        )
        if (uiState.hasPath) {
            Column(
                modifier = Modifier.sheetScrollableContentModifier(),
            ) {
                Card(
                    colors = UiDefaults.cardColors,
                ) {
                    LabeledButtonRow(
                        label = stringResource(R.string.ly_img_editor_sheet_text_on_path_label_path_position),
                    ) {
                        VerticalAlignment.entries.forEach {
                            AlignmentButton(
                                alignment = it,
                                currentAlignment = uiState.verticalAlignment,
                                changeAlignment = { alignment ->
                                    onEvent(BlockEvent.OnChangeVerticalAlignment(alignment))
                                },
                            )
                        }
                    }
                    Divider(Modifier.padding(horizontal = 16.dp))
                    LabeledButtonRow(
                        label = stringResource(R.string.ly_img_editor_sheet_text_on_path_label_direction),
                    ) {
                        ToggleIconButton(
                            checked = !uiState.isFlipped,
                            onCheckedChange = { onEvent(BlockEvent.OnChangeTextOnPathFlipped(flipped = false)) },
                        ) {
                            Icon(
                                IconPack.Reset,
                                contentDescription = stringResource(
                                    R.string.ly_img_editor_sheet_text_on_path_direction_option_forward,
                                ),
                                // Mirror the counterclockwise arrow into a clockwise one.
                                modifier = Modifier.scale(scaleX = -1f, scaleY = 1f),
                            )
                        }
                        ToggleIconButton(
                            checked = uiState.isFlipped,
                            onCheckedChange = { onEvent(BlockEvent.OnChangeTextOnPathFlipped(flipped = true)) },
                        ) {
                            Icon(
                                IconPack.Reset,
                                contentDescription = stringResource(
                                    R.string.ly_img_editor_sheet_text_on_path_direction_option_reversed,
                                ),
                            )
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                // The offset is a proportion of the path length.
                PropertySlider(
                    title = stringResource(R.string.ly_img_editor_sheet_text_on_path_label_offset),
                    value = uiState.offset,
                    valueRange = -1f..1f,
                    onValueChange = { onEvent(BlockEvent.OnChangeTextOnPathOffset(it)) },
                    onValueChangeFinished = { onEvent(BlockEvent.OnChangeFinish) },
                )
            }
        }
    }
}

@Composable
private fun LabeledButtonRow(
    label: String,
    buttons: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 0.dp, top = 4.dp, bottom = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyLarge,
        )
        Row {
            buttons()
        }
    }
}

class TextOnPathBottomSheetContent(
    override val type: SheetType,
    val uiState: TextOnPathUiState,
) : BottomSheetContent
