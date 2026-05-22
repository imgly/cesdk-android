package ly.img.editor.base.timeline.dragdrop

import androidx.compose.ui.geometry.Offset
import ly.img.editor.base.timeline.clip.Clip
import ly.img.editor.base.timeline.track.Track
import ly.img.engine.DesignBlock
import kotlin.time.Duration

/**
 * Where a dragged clip would land if the user released the pointer right now.
 *
 * Resolved each frame from the pointer's window space position. `null` while a drag is in
 * progress means the pointer is over an incompatible target (e.g. audio over video) and a
 * release should cancel without writing to the engine.
 */
sealed interface DropTarget {
    /**
     * Insert the dragged clip into an existing foreground track at [insertIndex],
     * positioned at [timeOffset]. [timeOffset] is the dragged clip's preview start time
     * inside the target track.
     *
     * [effectiveDuration] non-null means trim-to-fit: shrink the tail to this length on
     * commit so the clip fits between locked walls. Only ever set on existing track
     * drops — new track drops have no locked siblings to fit between.
     */
    data class ExistingTrack(
        val trackId: String,
        val insertIndex: Int,
        val timeOffset: Duration,
        val effectiveDuration: Duration? = null,
    ) : DropTarget

    /**
     * Create a new engine track at UI index [insertAt] in
     * [ly.img.editor.base.timeline.state.TimelineDataSource.tracks].
     *
     * The tracks list renders top-to-bottom in natural array order, so `insertAt = 0`
     * puts the new track at the visual top, `insertAt = tracks.size` at the visual
     * bottom, and anything in between slots between existing tracks.
     */
    data class NewTrack(
        val insertAt: Int,
        val timeOffset: Duration,
    ) : DropTarget
}

/**
 * Snapshot of an in-flight drag interaction.
 *
 * Built once on drag start and copied on each pointer move.
 *
 * @property clipId The clip being dragged.
 * @property sourceTrackId [Track.id] of the track the clip started in.
 * @property initialTimeOffset The clip's pre-drag [Clip.timeOffset]; used to fall back when a drag cancels
 * and to compute the desired drop position from the gesture's translation delta.
 * @property initialTouchLocation Pointer position in window coords at drag start.
 * @property currentTouchLocation Pointer position in window coords; replaced on every pointer move.
 * @property initialScrollOffset Horizontal scroll value (px) at drag start. Combined with the live
 * scroll offset to give finger-relative-to-content position even while auto-scroll is moving the timeline.
 * @property grabOffsetX Window space distance from the finger to the dragged clip's leading edge at drag start.
 * Preserved for the floating clip overlay so the clip doesn't snap to centre under the finger, and used to
 * derive the pointer's time-position on the timeline for insert-index decisions.
 * @property grabOffsetY Window space distance from the finger to the dragged clip's top edge at drag start.
 * @property dropTarget The current resolved drop target, or `null` if the pointer is over an incompatible / no-op zone.
 * @property targetSortedSiblings Per-target-track cache of sibling clips with the dragged clip excluded,
 * sorted by [Clip.timeOffset] ascending, keyed by [Track.id]. Captured eagerly for the source track at
 * drag start and lazily for each foreground track on first entry, then reused as the input
 * to [computeDropSlot] / [computeBackgroundDropSlot] / [computeDropOverrides] for the
 * rest of the drag. Safe to reuse because sibling [Clip.timeOffset] values can't change
 * during a drag — preview offsets live in [DragDropStore.overrides], not on [Clip].
 */
data class DragContext(
    val clipId: DesignBlock,
    val sourceTrackId: String,
    val initialTimeOffset: Duration,
    val initialTouchLocation: Offset,
    val currentTouchLocation: Offset,
    val initialScrollOffset: Int,
    val grabOffsetX: Float,
    val grabOffsetY: Float,
    val dropTarget: DropTarget? = null,
    val targetSortedSiblings: Map<String, List<Clip>> = emptyMap(),
)

/** Top-level drag-drop state machine. */
sealed interface DragDropState {
    data object Idle : DragDropState

    data class Dragging(
        val context: DragContext,
    ) : DragDropState
}

/** Convenience accessor: returns the live [DragContext] when dragging, `null` when idle. */
val DragDropState.context: DragContext?
    get() = (this as? DragDropState.Dragging)?.context
