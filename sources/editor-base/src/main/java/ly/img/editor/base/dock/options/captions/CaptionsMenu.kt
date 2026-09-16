package ly.img.editor.base.dock.options.captions

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties

/**
 * The keyboard action bar's overflow menu.
 *
 * Not a `DropdownMenu`: that popup is focusable and would take window focus, closing the keyboard the bar is
 * attached to. The scrim absorbs the outside taps a non-focusable popup lets through, and the explicit
 * position provider avoids Material 3 1.1.0 mispositioning inside an `imePadding`-shifted container.
 */
@Composable
internal fun CaptionsActionMenu(
    expanded: Boolean,
    /** Menu bottom edge in window coordinates; not derivable from the anchor button, since the bar is itself a popup. */
    menuBottomPx: Int,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return
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
        popupPositionProvider = remember(menuBottomPx) { AnchorStartAbovePositionProvider(menuBottomPx) },
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

/**
 * Places the keyboard action bar directly on top of the input method.
 *
 * The bar is a popup rather than sheet content: hosting it in the sheet pushes the sheet's measured height
 * past its expanded anchor budget, driving the offset negative. Window coordinates keep it on the keys.
 */
internal class ImeTopPositionProvider(
    private val windowHeightPx: Int,
    private val imeHeightPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset(
        x = 0,
        // Height comes from the root view: `windowSize` is the popup's own content window, not the screen.
        y = (windowHeightPx - imeHeightPx - popupContentSize.height).coerceAtLeast(0),
    )
}

/** Pins the outside-tap scrim to the window's origin, so it covers the whole screen. */
private object FullScreenScrimPositionProvider : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset = IntOffset.Zero
}

/** Puts the menu's bottom edge at [menuBottomPx] and aligns its start with the anchor button. */
private class AnchorStartAbovePositionProvider(
    private val menuBottomPx: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val xStart = if (layoutDirection == LayoutDirection.Ltr) {
            anchorBounds.left
        } else {
            anchorBounds.right - popupContentSize.width
        }
        val x = xStart.coerceIn(0, (windowSize.width - popupContentSize.width).coerceAtLeast(0))
        val y = (menuBottomPx - popupContentSize.height).coerceAtLeast(0)
        return IntOffset(x, y)
    }
}
