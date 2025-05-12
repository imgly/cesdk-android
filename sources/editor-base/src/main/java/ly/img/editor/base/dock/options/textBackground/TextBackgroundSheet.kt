package ly.img.editor.base.dock.options.textBackground

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import ly.img.editor.base.dock.BottomSheetContent
import ly.img.editor.base.dock.options.properties.PropertiesBlock
import ly.img.editor.base.dock.options.properties.PropertyColorPicker
import ly.img.editor.base.engine.PropertyAndValue
import ly.img.editor.core.R
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.ui.SheetHeader

@Composable
fun TextBackgroundBottomSheet(
    uiState: TextBackgroundUiState,
    onColorPickerActiveChanged: (active: Boolean) -> Unit,
    onEvent: (EditorEvent) -> Unit,
) {
    var screenState by remember { mutableStateOf<ScreenState>(ScreenState.Main) }
    when (screenState) {
        ScreenState.Main -> {
            SheetHeader(
                title = stringResource(id = R.string.ly_img_editor_background),
                onClose = { onEvent(EditorEvent.Sheet.Close(animate = true)) },
            )
            PropertiesBlock(
                designBlockWithProperties = uiState.designBlockWithProperties,
                onEvent = onEvent,
                onOpenColorPicker = {
                    onColorPickerActiveChanged(true)
                    screenState = ScreenState.ColorPicker(
                        propertyAndValue = it,
                    )
                },
            )
        }
        is ScreenState.ColorPicker -> {
            val localScreenState = screenState as ScreenState.ColorPicker
            PropertyColorPicker(
                designBlock = uiState.designBlockWithProperties.designBlock,
                propertyAndValue = localScreenState.propertyAndValue,
                onBack = {
                    onColorPickerActiveChanged(false)
                    screenState = ScreenState.Main
                },
                onEvent = onEvent,
            )
        }
    }
}

sealed interface ScreenState {
    data object Main : ScreenState

    data class ColorPicker(
        val propertyAndValue: PropertyAndValue,
    ) : ScreenState
}

class TextBackgroundBottomSheetContent(
    override val type: SheetType,
    val uiState: TextBackgroundUiState,
) : BottomSheetContent
