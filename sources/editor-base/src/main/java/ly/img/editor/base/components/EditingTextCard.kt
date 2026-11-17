package ly.img.editor.base.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconToggleButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.base.ui.Event
import ly.img.editor.core.LocalEditorScope
import ly.img.editor.core.R
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.ui.ColorButton
import ly.img.editor.core.ui.UiDefaults
import ly.img.editor.core.ui.iconpack.Capitalizecasing
import ly.img.editor.core.ui.iconpack.Check
import ly.img.editor.core.ui.iconpack.DefaultNone
import ly.img.editor.core.ui.iconpack.Formatbold
import ly.img.editor.core.ui.iconpack.Formatitalic
import ly.img.editor.core.ui.iconpack.Formatstrikethrough
import ly.img.editor.core.ui.iconpack.Formatunderlined
import ly.img.editor.core.ui.iconpack.IconPack
import ly.img.editor.core.ui.iconpack.Listbullet
import ly.img.editor.core.ui.iconpack.Listnumber
import ly.img.editor.core.ui.iconpack.Lowercasing
import ly.img.editor.core.ui.iconpack.Uppercasing
import ly.img.engine.ListStyle
import ly.img.engine.TextCase
import ly.img.engine.UnstableEngineApi

@Composable
fun EditingTextCard(
    modifier: Modifier,
    uiState: EditingTextCardUiState,
    onEvent: (EditorEvent) -> Unit,
) {
    Surface(
        modifier = modifier.imePadding(),
        shape = UiDefaults.CornerLargeTop,
    ) {
        when (uiState) {
            EditingTextCardUiState.Disabled -> DisabledHeader(onEvent)
            is EditingTextCardUiState.Formatting -> FormattingRow(uiState, onEvent)
        }
    }
}

@Composable
private fun FormattingRow(
    uiState: EditingTextCardUiState.Formatting,
    onEvent: (EditorEvent) -> Unit,
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(end = checkButtonWidth + checkButtonEndMargin)
                .horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            TextColorButton(uiState, onEvent)
            TextFormatToggles(uiState, onEvent)
            TextCaseButton(uiState, onEvent)
            TextListStyleButton(uiState, onEvent)
        }

        // Scrim that fades the scrolling row into the surface colour just
        // before it reaches the check button, so icons appear to disappear
        // under it.
        val gradientColor = MaterialTheme.colorScheme.surface
        val gradient = remember(gradientColor) {
            Brush.horizontalGradient(
                listOf(
                    gradientColor.copy(alpha = 0f),
                    gradientColor,
                ),
            )
        }
        val gradientWidth = 16.dp
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = checkButtonWidth + checkButtonEndMargin)
                .size(gradientWidth, checkButtonHeight)
                .background(gradient),
        )

        CheckButton(
            onClose = { onEvent(Event.OnKeyboardClose) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = checkButtonEndMargin),
        )
    }
}

@Composable
private fun DisabledHeader(onEvent: (EditorEvent) -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp),
    ) {
        Text(
            text = stringResource(R.string.ly_img_editor_edit_text_title),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.align(Alignment.Center),
        )
        CheckButton(
            onClose = { onEvent(Event.OnKeyboardClose) },
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = checkButtonEndMargin),
        )
    }
}

@Composable
private fun CheckButton(
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClose,
        modifier = modifier.size(width = checkButtonWidth, height = checkButtonHeight),
        shape = IconButtonDefaults.filledShape,
        contentColor = MaterialTheme.colorScheme.primary,
        shadowElevation = 2.dp,
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                IconPack.Check,
                contentDescription = stringResource(R.string.ly_img_editor_edit_text_title),
            )
        }
    }
}

private val checkButtonWidth = 56.dp
private val checkButtonHeight = 40.dp
private val checkButtonEndMargin = 4.dp

@Composable
private fun TextColorButton(
    uiState: EditingTextCardUiState.Formatting,
    onEvent: (EditorEvent) -> Unit,
) {
    ColorButton(
        colors = uiState.textColors,
        onClick = { onEvent(EditorEvent.Sheet.Open(SheetType.FillStroke(fillOnly = true))) },
    )
}

@Composable
private fun TextFormatToggles(
    uiState: EditingTextCardUiState.Formatting,
    onEvent: (EditorEvent) -> Unit,
) {
    val colors = editingTextCardToggleColors()
    IconToggleButton(
        checked = uiState.isBold,
        onCheckedChange = { onEvent(BlockEvent.OnBoldToggle) },
        enabled = uiState.canToggleBold,
        colors = colors,
    ) {
        Icon(
            IconPack.Formatbold,
            contentDescription = stringResource(R.string.ly_img_editor_edit_text_button_bold),
        )
    }
    IconToggleButton(
        checked = uiState.isItalic,
        onCheckedChange = { onEvent(BlockEvent.OnItalicToggle) },
        enabled = uiState.canToggleItalic,
        colors = colors,
    ) {
        Icon(
            IconPack.Formatitalic,
            contentDescription = stringResource(R.string.ly_img_editor_edit_text_button_italic),
        )
    }
    IconToggleButton(
        checked = uiState.isUnderline,
        onCheckedChange = { onEvent(BlockEvent.OnUnderlineToggle) },
        colors = colors,
    ) {
        Icon(
            IconPack.Formatunderlined,
            contentDescription = stringResource(R.string.ly_img_editor_edit_text_button_underline),
        )
    }
    IconToggleButton(
        checked = uiState.isStrikethrough,
        onCheckedChange = { onEvent(BlockEvent.OnStrikethroughToggle) },
        colors = colors,
    ) {
        Icon(
            IconPack.Formatstrikethrough,
            contentDescription = stringResource(R.string.ly_img_editor_edit_text_button_strikethrough),
        )
    }
}

@Composable
private fun editingTextCardToggleColors() = IconButtonDefaults.iconToggleButtonColors(
    contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
)

@Composable
private fun TextCaseButton(
    uiState: EditingTextCardUiState.Formatting,
    onEvent: (EditorEvent) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val buttonIcon = when (uiState.casing) {
        TextCase.LOWER_CASE -> IconPack.Lowercasing
        TextCase.TITLE_CASE -> IconPack.Capitalizecasing
        else -> IconPack.Uppercasing
    }
    Box {
        IconToggleButton(
            checked = uiState.casing != TextCase.NORMAL,
            onCheckedChange = { menuExpanded = true },
            colors = editingTextCardToggleColors(),
        ) {
            Icon(
                buttonIcon,
                contentDescription = stringResource(R.string.ly_img_editor_edit_text_label_letter_case),
            )
        }
        EditingTextCardDropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            PropertyItem(
                checked = uiState.casing == TextCase.NORMAL,
                textRes = R.string.ly_img_editor_edit_text_letter_case_option_none,
                icon = IconPack.DefaultNone,
                onClick = {
                    menuExpanded = false
                    onEvent(BlockEvent.OnChangeLetterCasing(TextCase.NORMAL))
                },
            )
            PropertyItem(
                checked = uiState.casing == TextCase.UPPER_CASE,
                textRes = R.string.ly_img_editor_edit_text_letter_case_option_uppercase,
                icon = IconPack.Uppercasing,
                onClick = {
                    menuExpanded = false
                    onEvent(BlockEvent.OnChangeLetterCasing(TextCase.UPPER_CASE))
                },
            )
            PropertyItem(
                checked = uiState.casing == TextCase.LOWER_CASE,
                textRes = R.string.ly_img_editor_edit_text_letter_case_option_lowercase,
                icon = IconPack.Lowercasing,
                onClick = {
                    menuExpanded = false
                    onEvent(BlockEvent.OnChangeLetterCasing(TextCase.LOWER_CASE))
                },
            )
            PropertyItem(
                checked = uiState.casing == TextCase.TITLE_CASE,
                textRes = R.string.ly_img_editor_edit_text_letter_case_option_title_case,
                icon = IconPack.Capitalizecasing,
                onClick = {
                    menuExpanded = false
                    onEvent(BlockEvent.OnChangeLetterCasing(TextCase.TITLE_CASE))
                },
            )
        }
    }
}

@Composable
private fun TextListStyleButton(
    uiState: EditingTextCardUiState.Formatting,
    onEvent: (EditorEvent) -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val buttonIcon = when (uiState.listStyle) {
        ListStyle.ORDERED -> IconPack.Listnumber
        else -> IconPack.Listbullet
    }
    Box {
        IconToggleButton(
            checked = uiState.listStyle != ListStyle.NONE,
            onCheckedChange = { menuExpanded = true },
            enabled = !uiState.isTextOnPath,
            colors = editingTextCardToggleColors(),
        ) {
            Icon(
                buttonIcon,
                contentDescription = stringResource(R.string.ly_img_editor_edit_text_label_list_style),
            )
        }
        EditingTextCardDropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            PropertyItem(
                checked = uiState.listStyle == ListStyle.NONE,
                textRes = R.string.ly_img_editor_edit_text_list_style_option_none,
                icon = IconPack.DefaultNone,
                onClick = {
                    menuExpanded = false
                    if (uiState.listStyle != ListStyle.NONE) {
                        onEvent(BlockEvent.OnChangeListStyle(ListStyle.NONE))
                    }
                },
            )
            PropertyItem(
                checked = uiState.listStyle == ListStyle.UNORDERED,
                textRes = R.string.ly_img_editor_edit_text_list_style_option_bulleted,
                icon = IconPack.Listbullet,
                onClick = {
                    menuExpanded = false
                    if (uiState.listStyle != ListStyle.UNORDERED) {
                        onEvent(BlockEvent.OnChangeListStyle(ListStyle.UNORDERED))
                    }
                },
            )
            PropertyItem(
                checked = uiState.listStyle == ListStyle.ORDERED,
                textRes = R.string.ly_img_editor_edit_text_list_style_option_numbered,
                icon = IconPack.Listnumber,
                onClick = {
                    menuExpanded = false
                    if (uiState.listStyle != ListStyle.ORDERED) {
                        onEvent(BlockEvent.OnChangeListStyle(ListStyle.ORDERED))
                    }
                },
            )
        }
    }
}

/**
 * - Non-focusable popups keep the IME attached to the underlying EditText.
 * - The full-screen scrim absorbs outside taps that [PopupProperties.focusable] = false
 *   would otherwise propagate to the canvas (FLAG_NOT_TOUCH_MODAL), exiting text-edit mode.
 * - Explicit position provider works around M3 1.1.0's DropdownMenu mispositioning
 *   when its anchor sits inside an imePadding-shifted Surface.
 * - The floating text-selection ActionMode lives in a window class above any compose
 *   [Popup], so we ask the engine to suppress it for the duration of the menu.
 */
@OptIn(UnstableEngineApi::class)
@Composable
private fun EditingTextCardDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return
    val engine = with(LocalEditorScope.current) { editorContext.engine }
    DisposableEffect(engine) {
        engine.isTextActionModeHidden = true
        onDispose { engine.isTextActionModeHidden = false }
    }
    val configuration = LocalConfiguration.current
    Popup(
        popupPositionProvider = FullScreenScrimPositionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = false),
    ) {
        Box(
            modifier = Modifier
                .size(configuration.screenWidthDp.dp, configuration.screenHeightDp.dp)
                .pointerInput(onDismissRequest) {
                    detectTapGestures { onDismissRequest() }
                },
        )
    }
    Popup(
        popupPositionProvider = AnchorTopEndAbovePositionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = false),
    ) {
        Surface(
            shape = MaterialTheme.shapes.extraSmall,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 3.dp,
            shadowElevation = 3.dp,
        ) {
            Column(
                modifier = Modifier
                    .width(IntrinsicSize.Max)
                    .padding(vertical = 8.dp),
                content = content,
            )
        }
    }
}

private object FullScreenScrimPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset.Zero
}

private object AnchorTopEndAbovePositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        // Anchor the menu's bottom-end corner to the button's top-end corner,
        // clamped horizontally to stay within the window.
        val xEnd = if (layoutDirection == LayoutDirection.Ltr) {
            anchorBounds.right - popupContentSize.width
        } else {
            anchorBounds.left
        }
        val x = xEnd.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val y = anchorBounds.top - popupContentSize.height
        return IntOffset(x, y)
    }
}
