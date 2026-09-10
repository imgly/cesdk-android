package ly.img.editor.base.timeline.dragdrop

import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.ui.geometry.Offset
import ly.img.editor.base.timeline.clip.Clip
import ly.img.editor.base.timeline.state.TimelineState
import ly.img.editor.base.timeline.state.TimelineZoomState
import ly.img.engine.DesignBlock
import kotlin.time.Duration

/**
 * Recompute the active drop preview for an in-flight move drag.
 *
 * `effectiveDeltaPx` folds the scroll delta into the pointer translation so a finger held
 * still in window-space tracks the content as horizontal auto-scroll moves the timeline
 * underneath it.
 */
internal fun recomputeDragPreview(
    clip: Clip,
    zoomState: TimelineZoomState,
    timelineState: TimelineState,
    viewForHapticFeedback: View,
    pointerInWindow: Offset,
) {
    val ctx = timelineState.dragDrop.phase.context ?: return

    val translationX = pointerInWindow.x - ctx.initialTouchLocation.x
    val scrollDeltaPx = (timelineState.dragDrop.horizontalScrollOffsetPx - ctx.initialScrollOffset).toFloat()
    val effectiveDeltaPx = translationX + scrollDeltaPx
    val desiredStart = clip.timeOffset + zoomState.toSeconds(effectiveDeltaPx)
    val pointerTime = desiredStart + zoomState.toSeconds(ctx.grabOffsetX)

    val backgroundTrack = timelineState.dataSource.backgroundTrack
    // Skip the tick if the bg frame hasn't been published yet — should never happen ideally
    val backgroundFrame = timelineState.dragDrop.trackFrames[backgroundTrack.id] ?: return
    val zone = resolveDropZone(
        pointerY = pointerInWindow.y,
        sourceTrackId = ctx.sourceTrackId,
        draggedClipType = clip.clipType,
        backgroundTrack = backgroundTrack,
        backgroundFrame = backgroundFrame,
        sortedCandidates = timelineState.dragDrop.candidatesSortedByY,
    )

    val updatedSortedSiblings = captureTargetSiblings(ctx, zone, clip.id)

    val newDropTarget: DropTarget? = when (zone) {
        is DropZone.ExistingTrack -> {
            val targetTrack = zone.track
            val sortedSiblings = updatedSortedSiblings[targetTrack.id] ?: emptyList()
            val idx = insertIndex(
                clips = sortedSiblings,
                excluding = null,
                pointerTime = pointerTime,
            )
            val backgroundTrackId = timelineState.dataSource.backgroundTrack.id
            val isBgTarget = targetTrack.id == backgroundTrackId
            val isBgSource = ctx.sourceTrackId == backgroundTrackId
            val slot = if (isBgTarget) {
                val draggedOriginalIndex = if (isBgSource) {
                    // Bg is packed and sorted by timeOffset, so the clip's index in
                    // sortedSiblings equals the count of bg clips before it in time.
                    sortedSiblings.count { it.timeOffset < clip.timeOffset }
                } else {
                    // FG-source clip wasn't in bg pre-drag — pass `size` so every locked
                    // bg sibling acts as a left wall and the clip can land anywhere past
                    // the rightmost lock.
                    sortedSiblings.size
                }
                computeBackgroundDropSlot(
                    sortedSiblings = sortedSiblings,
                    insertIndex = idx,
                    draggedDuration = clip.duration,
                    draggedOriginalIndex = draggedOriginalIndex,
                )
            } else {
                computeDropSlot(
                    sortedSiblings = sortedSiblings,
                    insertIndex = idx,
                    desiredStart = desiredStart,
                    draggedDuration = clip.duration,
                    isLiveBufferRecording = clip.isLiveBufferRecording,
                )
            }
            if (slot == null) {
                // Reject: slot below `minClipDuration` or a live-buffer clip can't
                // shrink. Clear overrides so the snap-back is clean.
                if (timelineState.dragDrop.overrides.isNotEmpty()) {
                    timelineState.dragDrop.overrides.clear()
                }
                null
            } else {
                // Cascade contents only depend on `(targetTrackId, insertIndex)`, so
                // only rebuild when the slot identity changes — `overrides` is a
                // SnapshotStateMap and an unconditional clear+putAll would invalidate
                // every reader at pointer-event rate. `slot.dropStart` is allowed to
                // slide per tick because it lives on `DropTarget`, not in this map.
                // Same applies to `slot.effectiveDuration`.
                //
                // Use `slot.insertIndex` (clamped at locked walls in bg, equal to `idx` in fg).
                val prevCascadeKey = (ctx.dropTarget as? DropTarget.ExistingTrack)
                    ?.let { it.trackId to it.insertIndex }
                val newCascadeKey = targetTrack.id to slot.insertIndex
                if (prevCascadeKey != newCascadeKey) {
                    timelineState.dragDrop.overrides.clear()
                    val cascade = if (isBgTarget) {
                        computeBackgroundDropOverrides(
                            sortedSiblings = sortedSiblings,
                            insertIndex = slot.insertIndex,
                            dropStart = slot.dropStart,
                            draggedDuration = slot.effectiveDuration,
                        )
                    } else {
                        computeDropOverrides(
                            sortedSiblings = sortedSiblings,
                            insertIndex = slot.insertIndex,
                            dropStart = slot.dropStart,
                            draggedDuration = slot.effectiveDuration,
                        )
                    }
                    timelineState.dragDrop.overrides.putAll(cascade)
                }
                DropTarget.ExistingTrack(
                    trackId = targetTrack.id,
                    insertIndex = slot.insertIndex,
                    timeOffset = slot.dropStart,
                    effectiveDuration = slot.effectiveDuration.takeIf { it != clip.duration },
                )
            }
        }
        is DropZone.NewTrack -> {
            // Drop any cascade overrides from a prior existing-track zone; gated on
            // `isNotEmpty()` so a NewTrack→NewTrack transition doesn't churn the SnapshotStateMap.
            if (timelineState.dragDrop.overrides.isNotEmpty()) {
                timelineState.dragDrop.overrides.clear()
            }
            DropTarget.NewTrack(
                insertAt = zone.insertAt,
                timeOffset = maxOf(Duration.ZERO, desiredStart),
            )
        }
        null -> {
            if (timelineState.dragDrop.overrides.isNotEmpty()) {
                timelineState.dragDrop.overrides.clear()
            }
            null
        }
    }

    if (isSnapTransition(ctx.dropTarget, newDropTarget)) {
        viewForHapticFeedback.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    timelineState.dragDrop.phase = DragDropState.Dragging(
        ctx.copy(
            currentTouchLocation = pointerInWindow,
            dropTarget = newDropTarget,
            targetSortedSiblings = updatedSortedSiblings,
        ),
    )
}

/**
 * Lazy capture the target track's siblings on first entry so the cascade computes against
 * a stable pre-drag baseline. Preview offsets live in overrides rather than on [Clip]
 * instances, so siblings can't shift during a drag and re-sorting per pointer event would
 * only re-allocate.
 *
 * New track zones have no siblings to cache.
 */
private fun captureTargetSiblings(
    ctx: DragContext,
    zone: DropZone?,
    draggedId: DesignBlock,
): Map<String, List<Clip>> {
    val target = (zone as? DropZone.ExistingTrack)?.track ?: return ctx.targetSortedSiblings
    if (target.id in ctx.targetSortedSiblings) return ctx.targetSortedSiblings
    return ctx.targetSortedSiblings + (
        target.id to target.clips
            .filter { it.id != draggedId }
            .sortedBy { it.timeOffset }
    )
}

/**
 * Whether the drop target's slot identity has changed in a way that warrants a snap haptic tick.
 *
 * Returns `false` on null edges so acquiring or losing a target mid-drag stays silent
 * (the start haptic covers acquisition).
 */
private fun isSnapTransition(
    prev: DropTarget?,
    new: DropTarget?,
): Boolean {
    if (prev == null || new == null) return false
    return when {
        prev::class != new::class -> true
        prev is DropTarget.ExistingTrack && new is DropTarget.ExistingTrack ->
            prev.trackId != new.trackId || prev.insertIndex != new.insertIndex
        prev is DropTarget.NewTrack && new is DropTarget.NewTrack ->
            prev.insertAt != new.insertAt
        else -> false
    }
}
