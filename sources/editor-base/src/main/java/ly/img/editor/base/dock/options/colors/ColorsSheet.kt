package ly.img.editor.base.dock.options.colors

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ly.img.editor.base.components.SectionHeader
import ly.img.editor.base.dock.BottomSheetContent
import ly.img.editor.base.dock.options.fillstroke.ColorOptions
import ly.img.editor.base.dock.options.fillstroke.ColorPickerSheet
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.base.ui.Event
import ly.img.editor.core.R
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.ui.SheetHeader
import ly.img.editor.core.ui.UiDefaults
import ly.img.editor.core.ui.sheetScrollableContentModifier

@Composable
fun ColorsSheet(
    uiState: ColorsUiState,
    onColorPickerActiveChanged: (active: Boolean) -> Unit,
    onEvent: (EditorEvent) -> Unit,
) {
    var screenState: ScreenState by remember { mutableStateOf(ScreenState.Main) }

    BackHandler(enabled = screenState != ScreenState.Main) {
        screenState = ScreenState.Main
    }

    when (screenState) {
        ScreenState.Main -> {
            Column {
                SheetHeader(
                    title = stringResource(id = R.string.ly_img_editor_sheet_colors_title),
                    onClose = { onEvent(EditorEvent.Sheet.Close(animate = true)) },
                )
                Column(
                    Modifier.sheetScrollableContentModifier(),
                ) {
                    uiState.items.forEachIndexed { index, item ->
                        SectionHeader(item.name)
                        Card(
                            colors = UiDefaults.cardColors,
                        ) {
                            ColorOptions(
                                enabled = true,
                                allowDisableColor = false,
                                selectedColor = item.selectedColor,
                                onNoColorSelected = { },
                                onColorSelected = {
                                    onEvent(Event.OnColorChange(item.name, it))
                                    onEvent(BlockEvent.OnChangeFinish)
                                },
                                openColorPicker = {
                                    onColorPickerActiveChanged(true)
                                    screenState = ScreenState.SectionColorPicker(item)
                                },
                                colors = uiState.colorPalette,
                            )
                        }
                        if (index < uiState.items.lastIndex) {
                            Spacer(Modifier.height(16.dp))
                        }
                    }
                }
            }
        }

        is ScreenState.SectionColorPicker -> {
            val state = screenState as? ScreenState.SectionColorPicker ?: return
            ColorPickerSheet(
                color = state.item.selectedColor,
                title = stringResource(
                    id = R.string.ly_img_editor_sheet_color_picker_title,
                    state.item.name,
                ).trim(),
                onBack = {
                    onColorPickerActiveChanged(false)
                    screenState = ScreenState.Main
                },
                onColorChange = {
                    onEvent(Event.OnColorChange(state.item.name, it))
                },
                onEvent = onEvent,
            )
        }
    }
}

private sealed interface ScreenState {
    data object Main : ScreenState

    data class SectionColorPicker(
        val item: ColorsUiState.Item,
    ) : ScreenState
}

class ColorsBottomSheetContent(
    override val type: SheetType,
    val uiState: ColorsUiState,
) : BottomSheetContent
