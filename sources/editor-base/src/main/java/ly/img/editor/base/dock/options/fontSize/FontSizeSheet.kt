package ly.img.editor.base.dock.options.fontSize

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ly.img.editor.base.components.ToggleIconButton
import ly.img.editor.base.dock.BottomSheetContent
import ly.img.editor.base.ui.Event
import ly.img.editor.core.R
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.ui.SheetHeader
import ly.img.editor.core.ui.UiDefaults
import ly.img.editor.core.ui.sheetScrollableContentModifier

@Composable
fun FontSizeSheet(
    uiState: FontSizeUiState,
    onEvent: (EditorEvent) -> Unit,
) {
    Column {
        SheetHeader(
            title = stringResource(id = R.string.ly_img_editor_sheet_font_size_title),
            onClose = { onEvent(EditorEvent.Sheet.Close(animate = true)) },
        )

        Card(
            Modifier.sheetScrollableContentModifier(),
            colors = UiDefaults.cardColors,
        ) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    stringResource(R.string.ly_img_editor_sheet_font_size_label_message),
                    style = MaterialTheme.typography.bodyLarge,
                )
                Row {
                    FontSizeUiState.Size.entries.forEach { size ->
                        ToggleIconButton(
                            checked = uiState.selectedSize == size,
                            onCheckedChange = {
                                onEvent(Event.OnFontSizeChange(uiState.designBlock, size.size))
                            },
                        ) {
                            Icon(
                                imageVector = size.icon,
                                contentDescription = null,
                            )
                        }
                    }
                }
            }
        }
    }
}

class FontSizeBottomSheetContent(
    override val type: SheetType,
    val uiState: FontSizeUiState,
) : BottomSheetContent
