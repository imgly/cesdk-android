package ly.img.editor.base.dock.options.adjustment

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ly.img.editor.base.dock.options.properties.PropertiesBlock
import ly.img.editor.core.R
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.ui.SheetHeader

@Composable
fun AdjustmentOptionsSheet(
    uiState: AdjustmentUiState,
    onEvent: (EditorEvent) -> Unit,
) {
    Column {
        SheetHeader(
            title = stringResource(id = R.string.ly_img_editor_adjustments),
            onClose = { onEvent(EditorEvent.Sheet.Close(animate = true)) },
        )
        PropertiesBlock(
            designBlockWithProperties = uiState.designBlockWithProperties,
            onEvent = onEvent,
            onOpenColorPicker = { /* Not applicable here */ },
        )
    }
}
