package ly.img.editor.base.dock.options.crop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ly.img.editor.base.components.PropertyItem
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.core.event.EditorEvent
import ly.img.engine.ContentFillMode

@Composable
fun ContentFillModePicker(
    uiState: CropUiState,
    onEvent: (EditorEvent) -> Unit,
) {
    var isFillModeSheetOpen by remember { mutableStateOf(false) }
    Box {
        DropdownMenu(
            modifier = Modifier
                .width(240.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(MaterialTheme.colorScheme.surface),
            expanded = isFillModeSheetOpen,
            onDismissRequest = { isFillModeSheetOpen = false },
        ) {
            ContentFillMode.entries.forEach { mode ->
                PropertyItem(
                    checked = mode == uiState.contentFillMode,
                    textRes = uiState.contentFillModeTextRes(mode),
                    icon = uiState.contentFillModeIcon(mode),
                    onClick = {
                        onEvent(
                            BlockEvent.OnChangeFillMode(
                                mode,
                            ),
                        )
                        isFillModeSheetOpen = false
                    },
                )
            }
        }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { isFillModeSheetOpen = true }
                .background(
                    color = if (isFillModeSheetOpen) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent,
                    shape = RoundedCornerShape(20.dp),
                )
                .padding(start = 12.dp, end = 8.dp),
        ) {
            Icon(
                imageVector = uiState.contentFillModeIcon(uiState.contentFillMode),
                contentDescription = null,
                modifier = Modifier
                    .size(18.dp)
                    .align(Alignment.CenterVertically),
            )
            Text(
                text = stringResource(uiState.contentFillModeTextRes(uiState.contentFillMode)),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier
                    .padding(horizontal = 8.dp, vertical = 10.dp)
                    .align(Alignment.CenterVertically),
            )
        }
    }
}
