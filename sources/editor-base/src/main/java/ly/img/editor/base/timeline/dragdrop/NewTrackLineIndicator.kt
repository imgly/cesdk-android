package ly.img.editor.base.timeline.dragdrop

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ly.img.editor.base.timeline.state.TimelineConfiguration
import ly.img.editor.base.timeline.state.TimelineState
import ly.img.editor.core.theme.LocalExtendedColorScheme
import kotlin.math.roundToInt

/**
 * Thin horizontal line that previews a new track drop in the gap above, below, or between
 * existing foreground tracks. Visible only while the active drop target is a
 * [DropTarget.NewTrack]; renders nothing otherwise.
 */
@Composable
fun NewTrackLineIndicator(timelineState: TimelineState) {
    var indicatorCoords: LayoutCoordinates? by remember { mutableStateOf(null) }

    val insertAtState = remember(timelineState) {
        derivedStateOf {
            val ctx = (timelineState.dragDrop.phase as? DragDropState.Dragging)?.context
            (ctx?.dropTarget as? DropTarget.NewTrack)?.insertAt
        }
    }
    val insertAt = insertAtState.value
    if (insertAt == null) {
        // Pre-warm `indicatorCoords` for the active branch's first frame — without it the
        // line would paint at origin=(0,0) before `onGloballyPositioned` fires.
        Box(modifier = Modifier.onGloballyPositioned { indicatorCoords = it })
        return
    }

    val sortedCandidates = timelineState.dragDrop.candidatesSortedByY
    val density = LocalDensity.current
    val halfSpacingPx = with(density) { TimelineConfiguration.clipPadding.toPx() / 2f }
    val rawLineYWindow = if (sortedCandidates.isNotEmpty()) {
        resolveLineYWindow(insertAt, sortedCandidates, halfSpacingPx) ?: return
    } else {
        val viewport = timelineState.dragDrop.verticalViewportFrame
        if (viewport.height <= 0f) return
        viewport.top + halfSpacingPx
    }
    // Pin to the visible LazyColumn area: an off-viewport candidate (scrolled past,
    // prefetch buffer, partially clipped during auto-scroll) could otherwise place the
    // line outside the timeline content. No-op when frames are on-screen.
    val verticalFrame = timelineState.dragDrop.verticalViewportFrame
    val lineYWindow = if (verticalFrame.height > 0f) {
        rawLineYWindow.coerceIn(verticalFrame.top + halfSpacingPx, verticalFrame.bottom - halfSpacingPx)
    } else {
        rawLineYWindow
    }

    val zoomState = timelineState.zoomState
    val totalDurationPx = zoomState.toPx(timelineState.totalDuration)

    val activeColor = LocalExtendedColorScheme.current.yellow.color
    val lineHeight = 3.dp
    val halfLineHeightPx = with(density) { lineHeight.toPx() } / 2f

    Box(modifier = Modifier.onGloballyPositioned { indicatorCoords = it }) {
        val origin = indicatorCoords?.localToWindow(Offset.Zero) ?: Offset.Zero
        // `lineYWindow` is the geometric midpoint of the gap; subtract half the line
        // thickness so the line is centered on it.
        val lineYLocal = lineYWindow - origin.y - halfLineHeightPx

        Box(
            modifier = Modifier
                .offset { IntOffset(x = 0, y = lineYLocal.roundToInt()) }
                .fillMaxWidth()
                .height(lineHeight)
                .drawBehind {
                    // Faint full-width base.
                    drawRoundRect(
                        color = activeColor.copy(alpha = 0.3f),
                        size = size,
                        cornerRadius = CornerRadius(size.height / 2f),
                    )
                    // Local X of time=0. The timeline leading-pads its ruler + LazyColumn
                    // by `viewport / 2` so time=0 starts under the centred playhead;
                    // `scrollOffset` then shifts it left as the user scrolls.
                    if (totalDurationPx <= 0f) return@drawBehind
                    val scrollOffsetPx = timelineState.dragDrop.horizontalScrollOffsetPx.toFloat()
                    val timeZeroLocalX = size.width / 2f - scrollOffsetPx
                    val playableStartX = timeZeroLocalX.coerceIn(0f, size.width)
                    val playableEndX = (timeZeroLocalX + totalDurationPx).coerceIn(0f, size.width)
                    val playableWidthPx = (playableEndX - playableStartX).coerceAtLeast(0f)
                    if (playableWidthPx <= 0f) return@drawBehind
                    drawRoundRect(
                        color = activeColor,
                        topLeft = Offset(playableStartX, 0f),
                        size = Size(playableWidthPx, size.height),
                        cornerRadius = CornerRadius(size.height / 2f),
                    )
                },
        )
    }
}

/**
 * Window space Y for [insertAt]: just above the topmost candidate, just below the
 * bottom most, or the midpoint of the matching gap between adjacent candidates. Returns
 * `null` if [insertAt] doesn't match any candidate (defensive — shouldn't happen).
 *
 * @param insertAt foreground tracks index where the new track would land.
 * @param sortedCandidates ascending by `frame.top` — see [DragDropStore.candidatesSortedByY].
 * @param halfSpacingPx half the inter-track gap; offsets the line so it sits at the gap centre.
 */
private fun resolveLineYWindow(
    insertAt: Int,
    sortedCandidates: List<DropCandidate>,
    halfSpacingPx: Float,
): Float? {
    val topMost = sortedCandidates.first()
    val bottomMost = sortedCandidates.last()
    if (insertAt <= topMost.tracksIndex) {
        return topMost.frame.top - halfSpacingPx
    }
    if (insertAt >= bottomMost.tracksIndex + 1) {
        return bottomMost.frame.bottom + halfSpacingPx
    }
    for (i in 0 until sortedCandidates.size - 1) {
        val upper = sortedCandidates[i]
        val lower = sortedCandidates[i + 1]
        if (upper.tracksIndex + 1 == insertAt) {
            return (upper.frame.bottom + lower.frame.top) / 2f
        }
    }
    return null
}
