package ly.img.editor.base.dock.options.captions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ly.img.editor.base.dock.BottomSheetContent
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.core.R
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.ui.SheetHeader
import ly.img.editor.core.ui.library.SimpleSelectableAssetList

/**
 * The caption style preset grid. A preset is applied to the selected caption and the engine syncs the style
 * across the track, so a tile applies straight away and there is no "None" tile.
 */
@Composable
internal fun CaptionStyleSheet(
    uiState: CaptionStyleUiState,
    onEvent: (EditorEvent) -> Unit,
) {
    val listState = rememberLazyListState()
    Column(
        modifier = Modifier.navigationBarsPadding(),
    ) {
        SheetHeader(
            title = stringResource(R.string.ly_img_editor_inspector_bar_button_caption_style),
            onClose = { onEvent(EditorEvent.Sheet.Close(animate = true)) },
        )
        SimpleSelectableAssetList(
            modifier = Modifier,
            listState = listState,
            listId = "captionPresets",
            assets = uiState.presets,
            selectedIcon = { null },
            onAssetSelected = {
                onEvent(BlockEvent.OnApplyCaptionPreset(sourceId = CAPTION_PRESETS_SOURCE_ID, asset = it.asset))
            },
            onAssetReselected = {},
            onAssetLongClick = {},
        )
    }
}

/** Hosts [CaptionStyleSheet] in the editor's bottom sheet. */
internal class CaptionStyleBottomSheetContent(
    override val type: SheetType,
    val uiState: CaptionStyleUiState,
) : BottomSheetContent
