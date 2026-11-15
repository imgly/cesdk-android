package ly.img.editor.base.clipboard

import android.net.Uri
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputScope
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import ly.img.editor.base.ui.Event
import kotlin.math.roundToInt

/** The clipboard commands that the canvas sends to the editor. */
internal sealed interface ClipboardEvent : Event {
    /** Copies the selected blocks to the system clipboard. A cut deletes them after the copy. */
    data class Copy(
        val cut: Boolean = false,
    ) : ClipboardEvent

    /** Pastes what the system clipboard holds onto the current page. */
    data object Paste : ClipboardEvent

    /** Adds the files of a drop on the canvas, as a paste of them does. */
    data class Drop(
        val uris: List<Uri>,
    ) : ClipboardEvent
}

/** What the clipboard menu offers now. */
internal data class ClipboardActions(
    val canCopy: Boolean,
    val canCut: Boolean,
    val canPaste: Boolean,
) {
    val isEmpty: Boolean
        get() = !canCopy && !canCut && !canPaste

    companion object {
        val None = ClipboardActions(canCopy = false, canCut = false, canPaste = false)
    }
}

/**
 * Cut, Copy and Paste with Ctrl+X, Ctrl+C and Ctrl+V on a hardware keyboard, and on ChromeOS with the same keys, and
 * as accessibility actions for TalkBack and Switch Access, which have no long press on the canvas. The canvas takes the
 * keys while [enabled] and no text field inside it takes them first. The engine's text input takes them while a text
 * block is in edit mode.
 */
internal fun Modifier.clipboardShortcuts(
    enabled: Boolean,
    focusRequester: FocusRequester,
    actionLabels: Triple<String, String, String>,
    onCopy: (cut: Boolean) -> Unit,
    onPaste: () -> Unit,
): Modifier = this
    .semantics {
        if (enabled) {
            customActions = listOf(
                CustomAccessibilityAction(actionLabels.first) { true.also { onCopy(true) } },
                CustomAccessibilityAction(actionLabels.second) { true.also { onCopy(false) } },
                CustomAccessibilityAction(actionLabels.third) { true.also { onPaste() } },
            )
        }
    }
    .focusRequester(focusRequester)
    .onKeyEvent { event ->
        if (!enabled || event.type != KeyEventType.KeyDown || !(event.isCtrlPressed || event.isMetaPressed)) {
            return@onKeyEvent false
        }
        when (event.key) {
            Key.C -> onCopy(false)
            Key.X -> onCopy(true)
            Key.V -> onPaste()
            else -> return@onKeyEvent false
        }
        true
    }
    .focusable(enabled)

/** Takes the keys for the canvas whenever [enabled] turns on, such as when the text editing ends. */
@Composable
internal fun ClipboardFocus(
    enabled: Boolean,
    focusRequester: FocusRequester,
) {
    LaunchedEffect(enabled) {
        if (enabled) runCatching { focusRequester.requestFocus() }
    }
}

/**
 * Calls [onLongPress] when a finger rests on the canvas for the long press timeout. It consumes nothing, so the engine
 * receives the same touches and still selects and moves blocks.
 */
internal suspend fun PointerInputScope.detectClipboardLongPress(onLongPress: (Offset) -> Unit) {
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
        val released = withTimeoutOrNull(viewConfiguration.longPressTimeoutMillis) {
            var released = false
            while (!released) {
                val event = awaitPointerEvent(PointerEventPass.Final)
                val change = event.changes.firstOrNull { it.id == down.id }
                released = event.changes.size > 1 ||
                    change == null ||
                    !change.pressed ||
                    (change.position - down.position).getDistance() > viewConfiguration.touchSlop
            }
        }
        if (released == null) onLongPress(down.position)
    }
}

/** The menu of Cut, Copy and Paste that a long press on the canvas opens at [offset], in pixels of the canvas. */
@Composable
internal fun ClipboardMenu(
    offset: Offset?,
    actions: ClipboardActions,
    onCopy: (cut: Boolean) -> Unit,
    onPaste: () -> Unit,
    onDismiss: () -> Unit,
) {
    Box(Modifier.offset { IntOffset(offset?.x?.roundToInt() ?: 0, offset?.y?.roundToInt() ?: 0) }) {
        DropdownMenu(
            expanded = offset != null && !actions.isEmpty,
            onDismissRequest = onDismiss,
            modifier = Modifier.testTag(MENU_TEST_TAG),
        ) {
            if (actions.canCut) {
                DropdownMenuItem(text = { Text(stringResource(android.R.string.cut)) }, onClick = {
                    onDismiss()
                    onCopy(true)
                })
            }
            if (actions.canCopy) {
                DropdownMenuItem(text = { Text(stringResource(android.R.string.copy)) }, onClick = {
                    onDismiss()
                    onCopy(false)
                })
            }
            if (actions.canPaste) {
                DropdownMenuItem(text = { Text(stringResource(android.R.string.paste)) }, onClick = {
                    onDismiss()
                    onPaste()
                })
            }
        }
    }
}

internal const val MENU_TEST_TAG = "ly.img.editor.clipboard.menu"
