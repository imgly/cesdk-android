package ly.img.editor.base.dock.options.captions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import kotlinx.coroutines.launch

/**
 * The editing field of a caption row, where Return and Backspace carry structural meaning.
 *
 * Two channels are needed: the value diff catches Return (input methods commit it as a newline with no key
 * event), `onPreviewKeyEvent` catches Backspace (deleting nothing at offset 0 leaves the value unchanged).
 * Both share one latch, so a keystroke seen on both only runs its operation once.
 */
@Composable
internal fun CaptionTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    /** Runs a structural operation with the field's live text; returns whether it happened. */
    onOperation: (operation: CaptionKeyOperation, liveText: String) -> Boolean,
    /** Whether a caption exists above, which decides if backspace-at-start can merge. */
    hasPreviousCaption: Boolean,
    /** Bumped whenever the caller rewrites [value] itself, including caret-only moves. */
    externalRevision: Int,
    placeholder: String,
    focusRequester: FocusRequester,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val coroutineScope = rememberCoroutineScope()

    // Released on the next frame rather than when the text changes: an operation can leave the text untouched,
    // and a latch waiting for a rewrite would never open again.
    var isOperationInFlight by remember { mutableStateOf(false) }

    // Diff against the last *reported* value, not the composed one: two changes within a frame would otherwise
    // both be compared against the same stale value.
    val lastReported = remember { mutableStateOf(value) }
    val lastRevision = remember { mutableStateOf(externalRevision) }
    // A caller rewrite wins over what the input method last reported — a caret it moved is the real one.
    if (lastRevision.value != externalRevision || lastReported.value.text != value.text) {
        lastRevision.value = externalRevision
        lastReported.value = value
    }

    fun run(
        operation: CaptionKeyOperation,
        liveText: String,
    ): Boolean {
        if (operation is CaptionKeyOperation.PassThrough) return false
        // Consume the key either way: a structural key must never also type a character.
        if (isOperationInFlight) return true
        if (operation !is CaptionKeyOperation.Ignore && onOperation(operation, liveText)) {
            isOperationInFlight = true
            coroutineScope.launch {
                withFrameNanos { }
                isOperationInFlight = false
            }
        }
        return true
    }

    BasicTextField(
        value = value,
        onValueChange = { new ->
            val change = CaptionKeyboard.valueChangeOperation(
                old = lastReported.value.toCaptionFieldValue(),
                new = new.toCaptionFieldValue(),
            )
            lastReported.value = new
            // Swallow the newline — including for `Ignore`, so a Return at the very start leaves no stray break.
            if (!run(change.operation, change.text)) {
                onValueChange(new)
            }
        },
        modifier = modifier
            .focusRequester(focusRequester)
            .onFocusChanged { onFocusChanged(it.isFocused) }
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val key = when (event.key) {
                    Key.Enter, Key.NumPadEnter -> CaptionKey.Return
                    Key.Backspace -> CaptionKey.Backspace
                    else -> return@onPreviewKeyEvent false
                }
                val live = lastReported.value
                run(
                    CaptionKeyboard.keyOperation(
                        key = key,
                        value = live.toCaptionFieldValue(),
                        hasPreviousCaption = hasPreviousCaption,
                    ),
                    live.text,
                )
            },
        textStyle = MaterialTheme.typography.bodyLarge.copy(color = LocalContentColor.current),
        cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Sentences,
            // Keeps the Return key a Return; a "Done" label would compete with the action bar's own Done.
            imeAction = ImeAction.Default,
        ),
        maxLines = CAPTION_MAX_VISIBLE_LINES,
        decorationBox = { innerTextField ->
            Box {
                if (value.text.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                innerTextField()
            }
        },
    )
}

/** The field's contents in the terms [CaptionKeyboard] decides in. */
private fun TextFieldValue.toCaptionFieldValue() = CaptionFieldValue(
    text = text,
    selectionStart = selection.min,
    selectionEnd = selection.max,
    isComposing = composition != null,
)
