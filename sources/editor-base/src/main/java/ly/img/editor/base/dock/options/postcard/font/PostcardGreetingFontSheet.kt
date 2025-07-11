package ly.img.editor.base.dock.options.postcard.font

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ly.img.editor.base.dock.BottomSheetContent
import ly.img.editor.base.dock.options.format.FontListUi
import ly.img.editor.base.ui.Event
import ly.img.editor.core.R
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.ui.SheetHeader

@Composable
fun PostcardGreetingFontSheet(
    uiState: PostcardGreetingFontUiState,
    onEvent: (EditorEvent) -> Unit,
) {
    Column {
        SheetHeader(
            title = stringResource(id = R.string.ly_img_editor_postcard_sheet_font_title),
            onClose = { onEvent(EditorEvent.Sheet.Close(animate = true)) },
        )

        FontListUi(
            libraryCategory = uiState.libraryCategory,
            fontFamily = uiState.fontFamily,
            filter = uiState.filter,
            onSelectFont = { fontData ->
                onEvent(Event.OnPostcardGreetingTypefaceChange(uiState.designBlock, fontData.typeface))
            },
        )
    }
}

class PostcardGreetingFontBottomSheetContent(
    override val type: SheetType,
    val uiState: PostcardGreetingFontUiState,
) : BottomSheetContent
