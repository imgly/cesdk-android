package ly.img.editor.base.timeline.dragdrop

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import ly.img.editor.base.timeline.clip.Clip
import ly.img.editor.base.timeline.state.TimelineDataSource
import ly.img.editor.base.timeline.state.TimelineState
import ly.img.editor.base.timeline.track.Track
import ly.img.engine.DesignBlock
import kotlin.time.Duration

/**
 * Holder for ephemeral UI state owned by the timeline drag-drop interaction. The
 * lambda parameters keep this store decoupled from [TimelineDataSource]
 * while still letting derived state subscribe to the underlying snapshot lists.
 *
 * @param foregroundTracks Live foreground tracks list.
 * @param findClip Resolves a clip from its [DesignBlock] id.
 */
class DragDropStore(
    private val foregroundTracks: () -> List<Track>,
    private val findClip: (DesignBlock) -> Clip?,
) {
    /** Top-level state machine — `Idle` while no drag is active, `Dragging(ctx)` otherwise. */
    var phase: DragDropState by mutableStateOf(DragDropState.Idle)

    /**
     * Id of the clip currently being dragged, or `null` when idle. Identity signal —
     * gates "is this the dragged one?" reads against a state that only changes twice
     * per drag, instead of subscribing to [phase] (which ticks per pointer event).
     * For clip data (duration, type, …), use [draggedClip] instead.
     */
    val draggedClipId: DesignBlock? by derivedStateOf { phase.context?.clipId }

    /**
     * Currently dragged Clip resolved from [draggedClipId], or `null` when idle. Data
     * signal — cached here so the O(tracks × clips) `findClip` scan runs at most once
     * per drag instead of per pointer event. Re-derives when [draggedClipId] flips and
     * when the underlying snapshot lists mutate, so a mid-drag UPDATED refresh still
     * picks up changes. For identity-only comparisons, use [draggedClipId].
     */
    val draggedClip: Clip? by derivedStateOf {
        val id = draggedClipId ?: return@derivedStateOf null
        findClip(id)
    }

    /**
     * Type-compatible drop candidates for the active drag — one per foreground track
     * with a published frame and a compatible clip type. Empty when not dragging or
     * when nothing qualifies. Cached so the walk + sort run at most once per drag
     * instead of per pointer event.
     *
     * **Order:** ascending by `frame.top` (topmost on screen first), so consumers can
     * do gap/edge math directly without re-sorting.
     */
    internal val candidatesSortedByY: List<DropCandidate> by derivedStateOf {
        val draggedClip = draggedClip ?: return@derivedStateOf emptyList()
        // Only trust frames that intersect the visible viewport — drops stale frames
        // (e.g. published once during AnimatedVisibility expand), pinned source tracks
        // pushed past the edge by auto-scroll, and prefetch-buffer items just outside
        // the visible range. Skipped while the viewport is still zero-sized (not yet
        // published), so the first drag isn't blocked waiting for layout.
        val viewport = verticalViewportFrame
        val viewportValid = viewport.height > 0f
        foregroundTracks()
            .mapIndexedNotNull { index, track ->
                val frame = _trackFrames[track.id] ?: return@mapIndexedNotNull null
                if (viewportValid && (frame.bottom <= viewport.top || frame.top >= viewport.bottom)) {
                    return@mapIndexedNotNull null
                }
                if (!isTypeCompatible(draggedClip, track)) return@mapIndexedNotNull null
                DropCandidate(track = track, tracksIndex = index, frame = frame)
            }
            .sortedBy { it.frame.top }
    }

    private val _trackFrames = mutableStateMapOf<String, Rect>()

    /**
     * Window space frames of every foreground track, keyed by [Track.id].
     * Read only view — write through [updateTrackFrame] / [removeTrackFrame].
     */
    val trackFrames: Map<String, Rect> get() = _trackFrames

    /**
     * Preview `timeOffset` per clip applied by the drag cascade — where to render
     * each affected clip while the gesture is in flight. Spans **all** tracks (the
     * dragged clip plus any siblings shoved aside). Mutated by the gesture; cleared
     * once `dataSource` reflects committed engine state.
     */
    val overrides: MutableMap<DesignBlock, Duration> = mutableStateMapOf()

    /**
     * Provider for the live horizontal scroll offset (px) of the timeline content.
     * The drag-drop machinery reads it to keep state in sync as the timeline scrolls
     * mid-drag — the drag preview cascade re-computes on scroll changes, and overlay
     * placement translates window space positions back into scrolled content space.
     *
     * Defaults to zero so reads are safe whenever the timeline view isn't currently
     * providing one — before first composition, between mounts, or while the timeline
     * is collapsed.
     *
     * Consumers should read [horizontalScrollOffsetPx] rather than calling this directly.
     */
    var horizontalScrollProvider: () -> Int = { 0 }

    /**
     * Live horizontal scroll offset (px) of the timeline content. Backed by
     * [horizontalScrollProvider] — reads in a Composable / placement / drawBehind
     * context subscribe to the underlying `ScrollState` (no cache lag); reads outside
     * composition return the current value directly.
     */
    val horizontalScrollOffsetPx: Int get() = horizontalScrollProvider()

    /**
     * Window space frame of the timeline viewport. Used to compute pointer distance
     * from the leading/trailing edges for horizontal auto-scroll. Distinct from
     * [verticalViewportFrame] so horizontal auto-scroll stays anchored to
     * the outer container — needed for scenes with only a background track,
     * where the foreground LazyColumn is empty and can't be
     * trusted as a horizontal anchor.
     */
    var viewportFrame: Rect by mutableStateOf(Rect.Zero)

    /**
     * Window space frame of the foreground tracks `LazyColumn`. Used for vertical
     * auto-scroll edge detection. Distinct from [viewportFrame] because the
     * `LazyColumn` excludes the ruler and background-track row, which shouldn't count
     * as edge zones.
     */
    var verticalViewportFrame: Rect by mutableStateOf(Rect.Zero)

    private var suppressNextEngineEventRefresh: Boolean = false

    /**
     * Arm a one-shot suppression of the next async [TimelineState.refresh] call.
     * Cross-track and new-track drops do a sync rebuild to close a one-frame flicker (engine has moved the clip;
     * `dataSource` hasn't caught up, so it briefly renders in its source track before relocating).
     * The engine still emits events for those writes — this flag tells the async path to skip them so
     * `dataSource` isn't rebuilt twice for one drop.
     */
    fun markSuppressNextEngineEventRefresh() {
        suppressNextEngineEventRefresh = true
    }

    /**
     * Read and reset the one-shot suppression flag. Returns `true` exactly once after
     * each [markSuppressNextEngineEventRefresh] call, then `false` until armed again.
     */
    fun consumeSuppressNextEngineEventRefresh(): Boolean {
        if (!suppressNextEngineEventRefresh) return false
        suppressNextEngineEventRefresh = false
        return true
    }

    /** Idempotent: only writes when the new rect differs from the stored one. */
    fun updateTrackFrame(
        id: String,
        rect: Rect,
    ) {
        if (_trackFrames[id] != rect) _trackFrames[id] = rect
    }

    /** Removes the track's frame entry; safe if the id is unknown. */
    fun removeTrackFrame(id: String) {
        _trackFrames.remove(id)
    }
}
