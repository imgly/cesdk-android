package ly.img.editor.base.timeline.dragdrop

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ly.img.editor.base.timeline.state.TimelineState
import ly.img.editor.core.theme.LocalExtendedColorScheme
import ly.img.editor.core.ui.utils.toDp
import kotlin.math.roundToInt

/**
 * Translucent rounded-rect "ghost" rendered at the snapped drop slot of the currently
 * dragged clip, sized to the clip's footprint at [DropTarget.ExistingTrack.timeOffset].
 * Visible only while the drop target points at this view's track; renders nothing otherwise.
 */
@Composable
fun DropSlotIndicatorView(
    trackId: String,
    timelineState: TimelineState,
) {
    val isActiveTarget by remember(timelineState, trackId) {
        derivedStateOf {
            val target = (timelineState.dragDrop.phase as? DragDropState.Dragging)
                ?.context?.dropTarget as? DropTarget.ExistingTrack
            target?.trackId == trackId
        }
    }
    if (!isActiveTarget) return

    val draggedClip = timelineState.dragDrop.draggedClip ?: return

    val zoomState = timelineState.zoomState
    val previewDuration by remember(timelineState, trackId, draggedClip.duration) {
        derivedStateOf {
            val target = (timelineState.dragDrop.phase as? DragDropState.Dragging)
                ?.context?.dropTarget as? DropTarget.ExistingTrack
            if (target?.trackId == trackId) {
                target.effectiveDuration ?: draggedClip.duration
            } else {
                draggedClip.duration
            }
        }
    }
    val widthPx = zoomState.toPx(previewDuration)
    val shape = MaterialTheme.shapes.small
    val activeBorderColor = LocalExtendedColorScheme.current.yellow.color

    Box(
        modifier = Modifier
            .offset {
                // Read `target.timeOffset` in placement scope so per-pointer updates
                // re-run only this lambda, not the composable. Defensive null/track
                // check guards the rare snapshot race where the drop target leaves
                // this track between the `isActiveTarget` refresh and this layout pass.
                val target = (timelineState.dragDrop.phase as? DragDropState.Dragging)
                    ?.context?.dropTarget as? DropTarget.ExistingTrack
                val x = if (target?.trackId == trackId) {
                    zoomState.toPx(target.timeOffset).roundToInt()
                } else {
                    0
                }
                IntOffset(x = x, y = 0)
            }
            .width(widthPx.toDp())
            .fillMaxHeight()
            .alpha(0.55f)
            .background(MaterialTheme.colorScheme.surfaceVariant, shape)
            .border(2.dp, activeBorderColor, shape),
    )
}
