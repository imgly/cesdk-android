package ly.img.editor.base.dock.options.animation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ly.img.editor.base.dock.BottomSheetContent
import ly.img.editor.core.R
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.ui.SheetHeader

@Composable
fun AnimationSheet(
    state: AnimationUiState,
    onEvent: (EditorEvent) -> Unit,
) {
    Column {
        SheetHeader(
            title = stringResource(id = R.string.ly_img_editor_animation),
            onClose = { onEvent(EditorEvent.Sheet.Close(animate = true)) },
        )
        // todo replace spacer with state
        Spacer(Modifier.height(200.dp))
    }
}

class AnimationBottomSheetContent(
    override val type: SheetType,
    val uiState: AnimationUiState,
) : BottomSheetContent
