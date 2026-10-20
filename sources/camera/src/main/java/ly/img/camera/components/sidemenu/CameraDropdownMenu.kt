package ly.img.camera.components.sidemenu

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import ly.img.camera.components.CameraRotatedLayout
import ly.img.camera.components.rememberCameraPositionTransition
import kotlin.math.roundToInt

@Composable
internal fun BoxScope.CameraDropdownMenu(
    expanded: Boolean,
    orientation: Int,
    rotation: () -> Float,
    onDismissRequest: () -> Unit,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (!expanded) return

    var anchor by remember { mutableStateOf<IntRect?>(null) }
    Box(
        modifier = Modifier
            .matchParentSize()
            .onGloballyPositioned { coordinates ->
                // Popup anchors otherwise use the unrotated parent's size.
                val bounds = coordinates.boundsInWindow()
                anchor = IntRect(
                    bounds.left.roundToInt(),
                    bounds.top.roundToInt(),
                    bounds.right.roundToInt(),
                    bounds.bottom.roundToInt(),
                )
            },
    )

    val anchorBounds = anchor ?: return

    val positionTransition = rememberCameraPositionTransition(orientation, orientation)
    val displayedOrientation = positionTransition.position
    var displayedAnchor by remember { mutableStateOf(anchorBounds) }
    LaunchedEffect(anchorBounds, displayedOrientation, orientation) {
        if (displayedOrientation == orientation) {
            displayedAnchor = anchorBounds
        }
    }
    val margin = with(LocalDensity.current) { 8.dp.roundToPx() }
    val positionProvider = remember(displayedAnchor, displayedOrientation, margin) {
        CameraMenuPositionProvider(displayedAnchor, displayedOrientation, margin)
    }
    Popup(
        popupPositionProvider = positionProvider,
        onDismissRequest = onDismissRequest,
        properties = PopupProperties(focusable = true),
    ) {
        // A popup is a separate window, so apply the camera rotation again here.
        CameraRotatedLayout(
            sideways = displayedOrientation == 90 || displayedOrientation == 270,
            rotation = { positionTransition.rotationFor(orientation, rotation) },
            alpha = { positionTransition.alpha.value },
        ) {
            Surface(
                shape = MaterialTheme.shapes.extraSmall,
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
            ) {
                Column(
                    modifier = Modifier
                        .width(IntrinsicSize.Max)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = 8.dp),
                    content = content,
                )
            }
        }
    }
}

private class CameraMenuPositionProvider(
    private val anchor: IntRect,
    private val orientation: Int,
    private val margin: Int,
) : PopupPositionProvider {
    override fun calculatePosition(
        anchorBounds: IntRect,
        windowSize: IntSize,
        layoutDirection: LayoutDirection,
        popupContentSize: IntSize,
    ): IntOffset {
        val width = popupContentSize.width
        val height = popupContentSize.height
        // Open visually below the control, extending toward the preview's center.
        // If there is no room below, open visually above instead.
        val position = when (orientation) {
            90 -> IntOffset(
                if (anchor.left - width >= margin) anchor.left - width else anchor.right,
                anchor.top,
            )
            180 -> IntOffset(
                anchor.right - width,
                if (anchor.top - height >= margin) anchor.top - height else anchor.bottom,
            )
            270 -> IntOffset(
                if (anchor.right + width <= windowSize.width - margin) anchor.right else anchor.left - width,
                anchor.top,
            )
            else -> IntOffset(
                anchor.left,
                if (anchor.bottom + height <= windowSize.height - margin) anchor.bottom else anchor.top - height,
            )
        }
        return IntOffset(
            position.x.coerceIn(margin, (windowSize.width - width - margin).coerceAtLeast(margin)),
            position.y.coerceIn(margin, (windowSize.height - height - margin).coerceAtLeast(margin)),
        )
    }
}
