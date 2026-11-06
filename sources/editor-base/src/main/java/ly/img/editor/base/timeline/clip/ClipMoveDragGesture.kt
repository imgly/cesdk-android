package ly.img.editor.base.timeline.clip

import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import ly.img.editor.base.timeline.dragdrop.DragContext
import ly.img.editor.base.timeline.dragdrop.DragDropState
import ly.img.editor.base.timeline.dragdrop.context
import ly.img.editor.base.timeline.dragdrop.recomputeDragPreview
import ly.img.editor.base.timeline.state.TimelineState
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.base.ui.Event

/**
 * Long-press-then-drag gesture surface covering the clip body. Renders nothing — the
 * lifted-clip visual is drawn at the timeline root, so this Box only catches pointer
 * events (hence `Gesture`, not `View`). `BoxScope` extension so `matchParentSize` resolves.
 */
@Composable
internal fun BoxScope.ClipMoveDragGesture(
    clip: Clip,
    timelineState: TimelineState,
    isBeingDragged: Boolean,
    onEvent: (Event) -> Unit,
    onStartMoveDrag: () -> Unit,
    onEndMoveDrag: (cancelled: Boolean) -> Unit,
) {
    val zoomState = timelineState.zoomState
    val viewForHapticFeedback = LocalView.current
    var moveLayoutCoords: LayoutCoordinates? by remember { mutableStateOf(null) }

    // Auto-scroll moves the timeline under a stationary finger; recompute so the
    // drop slot and floating clip keep tracking the finger's content-space position.
    // Gated on `isBeingDragged` so non-dragged clips pay zero cost.
    if (isBeingDragged) {
        LaunchedEffect(timelineState.dragDrop.horizontalScrollOffsetPx) {
            val ctx = timelineState.dragDrop.phase.context ?: return@LaunchedEffect
            recomputeDragPreview(
                clip = clip,
                zoomState = zoomState,
                timelineState = timelineState,
                viewForHapticFeedback = viewForHapticFeedback,
                pointerInWindow = ctx.currentTouchLocation,
            )
        }
    }

    Box(
        modifier = Modifier
            .matchParentSize()
            .onGloballyPositioned { moveLayoutCoords = it }
            // Field-level keying — keying on the whole `clip` data class would restart the
            // gesture on any field change (e.g. an async resource load resolving mid-drag).
            .pointerInput(
                clip.id,
                clip.allowsSelecting,
                clip.timeOffset,
                clip.duration,
                zoomState.zoomLevel,
            ) {
                if (!clip.allowsSelecting) return@pointerInput

                val sourceTrack = timelineState.dataSource.findTrack(clip)

                detectDragGesturesAfterLongPress(
                    onDragStart = { localOffset ->
                        val coords = moveLayoutCoords
                            ?: return@detectDragGesturesAfterLongPress
                        val pointerInWindow = coords.localToWindow(localOffset)
                        val moveBodyOriginInWindow = coords.localToWindow(Offset.Zero)

                        // Auto-select before publishing Dragging so the gesture reads
                        // stable state. Read `selectedClip` inline rather than via a
                        // captured parameter — `OnToggleSelectBlock` is a toggle, and a
                        // stale value would flip an already-selected clip off.
                        val isCurrentlySelected = timelineState.selectedClip?.id == clip.id
                        if (!isCurrentlySelected) {
                            onEvent(BlockEvent.OnToggleSelectBlock(clip.id))
                        }

                        onStartMoveDrag()

                        timelineState.dragDrop.phase = DragDropState.Dragging(
                            DragContext(
                                clipId = clip.id,
                                sourceTrackId = sourceTrack.id,
                                initialTimeOffset = clip.timeOffset,
                                initialTouchLocation = pointerInWindow,
                                currentTouchLocation = pointerInWindow,
                                initialScrollOffset = timelineState.dragDrop.horizontalScrollOffsetPx,
                                // Pointer offset within the clip body, fed to the floating overlay
                                // so it lifts under the finger without snapping to the clip origin.
                                grabOffsetX = pointerInWindow.x - moveBodyOriginInWindow.x,
                                grabOffsetY = pointerInWindow.y - moveBodyOriginInWindow.y,
                                dropTarget = null,
                                targetSortedSiblings = mapOf(
                                    sourceTrack.id to sourceTrack.clips
                                        .filter { it.id != clip.id }
                                        .sortedBy { it.timeOffset },
                                ),
                            ),
                        )

                        viewForHapticFeedback.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val coords = moveLayoutCoords
                            ?: return@detectDragGesturesAfterLongPress
                        val pointerInWindow = coords.localToWindow(change.position)
                        recomputeDragPreview(
                            clip = clip,
                            zoomState = zoomState,
                            timelineState = timelineState,
                            viewForHapticFeedback = viewForHapticFeedback,
                            pointerInWindow = pointerInWindow,
                        )
                    },
                    onDragEnd = { onEndMoveDrag(false) },
                    onDragCancel = { onEndMoveDrag(true) },
                )
            },
    )
}
