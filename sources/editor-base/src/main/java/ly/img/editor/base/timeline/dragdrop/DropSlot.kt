package ly.img.editor.base.timeline.dragdrop

import androidx.compose.ui.geometry.Rect
import ly.img.editor.base.timeline.clip.Clip
import ly.img.editor.base.timeline.clip.ClipType
import ly.img.editor.base.timeline.state.TimelineConfiguration
import ly.img.editor.base.timeline.track.Track
import ly.img.engine.DesignBlock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Insert index for a dragged clip in a target track: flips when the pointer's time crosses
 * a sibling's centre. Pointer-anchored (not dragged-centre) so long clips drop between
 * small ones without overshooting by half their duration.
 *
 * @param clips Target track's clips; need not be sorted.
 * @param excluding Clip id to skip — the dragged clip's id when it's still in [clips].
 * @param pointerTime The pointer's time-position inside the target track.
 */
internal fun insertIndex(
    clips: List<Clip>,
    excluding: DesignBlock?,
    pointerTime: Duration,
): Int {
    var index = 0
    for (sibling in clips) {
        if (sibling.id == excluding) continue
        val siblingCentre = sibling.timeOffset + sibling.duration / 2
        if (siblingCentre < pointerTime) index++
    }
    return index
}

/**
 * Snapped drop slot for a dragged clip's preview position.
 *
 * @property insertIndex Resolved insert index in the target track's clip list.
 * @property dropStart The dragged clip's preview start time inside the target track.
 * @property effectiveDuration Duration the clip should have on commit; smaller than
 * `draggedDuration` for trim-to-fit drops, otherwise equal.
 */
internal data class DropSlot(
    val insertIndex: Int,
    val dropStart: Duration,
    val effectiveDuration: Duration,
)

/**
 * Resolves the dragged clip's snapped start time at [insertIndex] in a foreground track.
 *
 * Three outcomes:
 *  - **Free placement** — gap fits and no locked successor squeezes the cascade.
 *  - **Trim-to-fit** — cascade is too tight; the tail is shortened to fill the slot.
 *  - **Reject** (returns `null`) — slot below [TimelineConfiguration.minClipDuration],
 *    or a live-buffer recording can't be shortened. Caller snaps back to origin.
 *
 * Background tracks use [computeBackgroundDropSlot].
 *
 * @param sortedSiblings Target track's clips with the dragged clip excluded, sorted by
 * `timeOffset` ascending.
 * @param insertIndex Drop slot index in [sortedSiblings] (`0..size`).
 * @param desiredStart The dragged clip's would-be `timeOffset` if released now.
 * @param draggedDuration The dragged clip's duration.
 * @param isLiveBufferRecording See [Clip.isLiveBufferRecording] — live-buffer clips
 * can't be tail-shortened, so trim-to-fit slots return `null` for them.
 */
internal fun computeDropSlot(
    sortedSiblings: List<Clip>,
    insertIndex: Int,
    desiredStart: Duration,
    draggedDuration: Duration,
    isLiveBufferRecording: Boolean,
): DropSlot? {
    val prev = sortedSiblings.getOrNull(insertIndex - 1)
    val next = sortedSiblings.getOrNull(insertIndex)
    val prevEnd = prev?.let { it.timeOffset + it.duration } ?: 0.seconds
    val slotHasEnoughRoom = next?.let { it.timeOffset - prevEnd >= draggedDuration } ?: true

    // Nearest locked successor's start, minus the durations of unlocked siblings the
    // cascade would push right ahead of it — i.e. the wall the dragged clip's tail
    // can't cross.
    val lockedSuccessorWall: Duration = run {
        var unlockedDuration = Duration.ZERO
        for (i in insertIndex until sortedSiblings.size) {
            val sibling = sortedSiblings[i]
            if (sibling.isLocked) {
                return@run sibling.timeOffset - unlockedDuration
            }
            unlockedDuration += sibling.duration
        }
        Duration.INFINITE
    }

    // Walk predecessors backward to find the locked-predecessor wall (boundary unlocked
    // predecessors can be pulled left against) and the total duration of unlocked
    // predecessors that would pack against it.
    var lockedPredecessorWall: Duration = Duration.ZERO
    var unlockedBefore: Duration = Duration.ZERO
    for (i in (insertIndex - 1) downTo 0) {
        val sibling = sortedSiblings[i]
        if (sibling.isLocked) {
            lockedPredecessorWall = sibling.timeOffset + sibling.duration
            break
        }
        unlockedBefore += sibling.duration
    }

    val nextCap = next?.let { it.timeOffset - draggedDuration } ?: Duration.INFINITE
    val lockedCap = lockedSuccessorWall - draggedDuration
    val cap = minOf(nextCap, lockedCap)
    val canPlaceFreely = slotHasEnoughRoom && cap >= prevEnd

    return if (canPlaceFreely) {
        val unsnappedDropStart = maxOf(0.seconds, desiredStart).coerceIn(prevEnd, cap)
        DropSlot(
            insertIndex = insertIndex,
            dropStart = unsnappedDropStart,
            effectiveDuration = draggedDuration,
        )
    } else {
        val pulledLowerBound = lockedPredecessorWall + unlockedBefore
        trimToFit(
            lowerBound = pulledLowerBound,
            lockedSuccessorWall = lockedSuccessorWall,
            draggedDuration = draggedDuration,
            isLiveBufferRecording = isLiveBufferRecording,
        )?.let { resolved ->
            DropSlot(
                insertIndex = insertIndex,
                dropStart = resolved.dropStart,
                effectiveDuration = resolved.effectiveDuration,
            )
        }
    }
}

/** Resolved drop start and post-trim duration from [trimToFit]. */
private data class TrimToFitResult(
    val dropStart: Duration,
    val effectiveDuration: Duration,
)

/**
 * Returns `null` when room is below [TimelineConfiguration.minClipDuration] (better to
 * bounce than leave a sliver) or the clip is a live-buffer recording whose duration the
 * buffer owns.
 */
private fun trimToFit(
    lowerBound: Duration,
    lockedSuccessorWall: Duration,
    draggedDuration: Duration,
    isLiveBufferRecording: Boolean,
): TrimToFitResult? {
    val availableRoom = lockedSuccessorWall - lowerBound
    if (availableRoom < TimelineConfiguration.minClipDuration) return null
    val effectiveDuration = if (availableRoom < draggedDuration) {
        if (isLiveBufferRecording) return null
        availableRoom
    } else {
        draggedDuration
    }
    return TrimToFitResult(dropStart = lowerBound, effectiveDuration = effectiveDuration)
}

/**
 * Background variant of [computeDropSlot]. Bg is engine-packed on commit, so
 * [DropSlot.dropStart] collapses to the cumulative duration of preceding siblings and
 * trim-to-fit doesn't apply — [DropSlot.effectiveDuration] always equals [draggedDuration].
 *
 * [insertIndex] is clamped via [computeBackgroundLockWalls] so every locked sibling keeps
 * its bg-list index — the dragged clip can be pushed against a locked wall but not through.
 *
 * @param draggedOriginalIndex Dragged clip's pre-drag index in [sortedSiblings].
 */
internal fun computeBackgroundDropSlot(
    sortedSiblings: List<Clip>,
    insertIndex: Int,
    draggedDuration: Duration,
    draggedOriginalIndex: Int,
): DropSlot {
    val (lockedLeft, lockedRight) = computeBackgroundLockWalls(sortedSiblings, draggedOriginalIndex)
    val clampedIndex = insertIndex.coerceIn(lockedLeft + 1, lockedRight)
    var packedDropStart = Duration.ZERO
    for (i in 0 until clampedIndex) {
        packedDropStart += sortedSiblings[i].duration
    }
    return DropSlot(
        insertIndex = clampedIndex,
        dropStart = packedDropStart,
        effectiveDuration = draggedDuration,
    )
}

/**
 * Locked walls confining a bg reorder's insert index to `[lockedLeft + 1, lockedRight]`.
 * `lockedLeft`: largest locked sibling index `< draggedOriginalIndex`, or `-1` if none.
 * `lockedRight`: smallest locked sibling index `>= draggedOriginalIndex`, or
 * [Int.MAX_VALUE] if none.
 */
private fun computeBackgroundLockWalls(
    sortedSiblings: List<Clip>,
    draggedOriginalIndex: Int,
): Pair<Int, Int> {
    var lockedLeft = -1
    for (i in (draggedOriginalIndex - 1) downTo 0) {
        if (sortedSiblings[i].isLocked) {
            lockedLeft = i
            break
        }
    }
    var lockedRight = Int.MAX_VALUE
    for (i in draggedOriginalIndex until sortedSiblings.size) {
        if (sortedSiblings[i].isLocked) {
            lockedRight = i
            break
        }
    }
    return lockedLeft to lockedRight
}

/**
 * Drop cascade: target-track siblings that shift to make room for the dragged clip.
 *
 * **Suffix push** from `dropStart + draggedDuration`: stops at the first sibling already
 * past the cursor, or at a locked sibling.
 *
 * **Prefix pull**: only when `dropStart` sits earlier than the immediate predecessor's
 * original end — i.e. the dragged clip needs the slack. Skipped otherwise so unrelated
 * neighbours don't surprise-shift.
 *
 * Bg variant: [computeBackgroundDropOverrides].
 */
internal fun computeDropOverrides(
    sortedSiblings: List<Clip>,
    insertIndex: Int,
    dropStart: Duration,
    draggedDuration: Duration,
): Map<DesignBlock, Duration> {
    var overrides: MutableMap<DesignBlock, Duration>? = null

    val originalPrevEnd: Duration = if (insertIndex > 0) {
        val prev = sortedSiblings[insertIndex - 1]
        prev.timeOffset + prev.duration
    } else {
        Duration.ZERO
    }
    val needsPredecessorPull = dropStart < originalPrevEnd

    if (needsPredecessorPull) {
        var beforeCursor = Duration.ZERO
        for (i in 0 until insertIndex) {
            val sibling = sortedSiblings[i]
            val target: Duration = if (sibling.isLocked) {
                // Locked predecessor stays put; cursor jumps to its end so trailing
                // unlocked siblings pack against it.
                beforeCursor = maxOf(beforeCursor, sibling.timeOffset + sibling.duration)
                sibling.timeOffset
            } else {
                val packed = beforeCursor
                beforeCursor += sibling.duration
                packed
            }
            if (sibling.timeOffset != target) {
                val map = overrides ?: mutableMapOf<DesignBlock, Duration>().also { overrides = it }
                map[sibling.id] = target
            }
        }
    }

    if (insertIndex < sortedSiblings.size) {
        var cursor = dropStart + draggedDuration
        for (i in insertIndex until sortedSiblings.size) {
            val sibling = sortedSiblings[i]
            if (sibling.isLocked) break
            if (sibling.timeOffset >= cursor) break
            val map = overrides ?: mutableMapOf<DesignBlock, Duration>().also { overrides = it }
            map[sibling.id] = cursor
            cursor += sibling.duration
        }
    }
    return overrides ?: emptyMap()
}

/**
 * Background track variant of [computeDropOverrides]. The engine auto-packs the bg track
 * on commit, so the live preview packs siblings before [insertIndex] from `0` instead of
 * leaving them at their authored offsets — without this, dragging rightward past a
 * sibling overlaps the dragged clip's drop slot until release.
 *
 * Two passes:
 *  - **Prefix pack-from-zero** over `[0, insertIndex)`: each sibling renders at the
 *    cumulative duration of preceding siblings.
 *  - **Suffix forward cascade** over `[insertIndex, size)`: identical to
 *    [computeDropOverrides].
 *
 * Prefix walk packs unconditionally because the engine auto-packs on commit; the suffix
 * walk's lock-break matches [computeDropOverrides] and serves as defence in case a bg
 * clip is ever marked [Clip.isLocked].
 *
 * @param sortedSiblings Bg track's clips with the dragged clip excluded, sorted by
 * `timeOffset` ascending.
 * @param insertIndex Drop slot index in [sortedSiblings] (`0..size`).
 * @param dropStart The dragged clip's resolved start.
 * @param draggedDuration The dragged clip's duration.
 */
internal fun computeBackgroundDropOverrides(
    sortedSiblings: List<Clip>,
    insertIndex: Int,
    dropStart: Duration,
    draggedDuration: Duration,
): Map<DesignBlock, Duration> {
    var overrides: MutableMap<DesignBlock, Duration>? = null

    var beforeCursor = Duration.ZERO
    for (i in 0 until insertIndex) {
        val sibling = sortedSiblings[i]
        if (sibling.timeOffset != beforeCursor) {
            val map = overrides ?: mutableMapOf<DesignBlock, Duration>().also { overrides = it }
            map[sibling.id] = beforeCursor
        }
        beforeCursor += sibling.duration
    }

    var cursor = dropStart + draggedDuration
    for (i in insertIndex until sortedSiblings.size) {
        val sibling = sortedSiblings[i]
        if (sibling.isLocked) break
        if (sibling.timeOffset >= cursor) break
        val map = overrides ?: mutableMapOf<DesignBlock, Duration>().also { overrides = it }
        map[sibling.id] = cursor
        cursor += sibling.duration
    }

    return overrides ?: emptyMap()
}

/**
 * Whether the dragged clip can be dropped onto the target track. The engine refuses
 * audio clips inside video tracks (and vice versa), so the drag preview rejects those
 * targets before anything user-visible happens.
 */
internal fun isTypeCompatible(
    draggedClip: Clip,
    targetTrack: Track,
): Boolean {
    val example = targetTrack.clips.firstOrNull() ?: return true
    return (draggedClip.clipType == ClipType.Audio) == (example.clipType == ClipType.Audio)
}

/** Whether [clipType] is allowed in the background track (everything except audio). */
internal fun isBackgroundCompatible(clipType: ClipType): Boolean = clipType != ClipType.Audio

/**
 * Type-compatible drop zone candidate for [resolveDropZone].
 */
internal data class DropCandidate(
    val track: Track,
    val tracksIndex: Int,
    val frame: Rect,
)

/**
 * High-level zone the dragged clip currently sits over.
 */
internal sealed interface DropZone {
    /** Pointer is over [track] (or nearest to it, in fallback). */
    data class ExistingTrack(
        val track: Track,
    ) : DropZone

    /** Pointer is in a gap. [insertAt] is the index where the new track will land. */
    data class NewTrack(
        val insertAt: Int,
    ) : DropZone
}

/**
 * Maps the pointer position to a drop zone — the background track, an existing foreground
 * track, a new foreground track in a gap, or `null` when nothing is compatible.
 *
 * The background track only accepts image/video from foreground sources (and any clip
 * from itself); other types fall through to a foreground target so the release isn't lost.
 *
 * @param pointerY Pointer Y in window space.
 * @param sourceTrackId Source track of the dragged clip.
 * @param draggedClipType Dragged clip's type.
 * @param backgroundTrack The background track.
 * @param backgroundFrame Background track frame in window space.
 * @param sortedCandidates Type-compatible foreground tracks with published frames,
 *  ascending by `frame.top`.
 */
internal fun resolveDropZone(
    pointerY: Float,
    sourceTrackId: String,
    draggedClipType: ClipType,
    backgroundTrack: Track,
    backgroundFrame: Rect,
    sortedCandidates: List<DropCandidate>,
): DropZone? {
    val sourceIsBackground = sourceTrackId == backgroundTrack.id
    if ((sourceIsBackground || isBackgroundCompatible(draggedClipType)) && pointerY >= backgroundFrame.top) {
        return DropZone.ExistingTrack(backgroundTrack)
    }
    resolveDirectCandidateHit(pointerY, sortedCandidates)?.let { return it }
    if (sortedCandidates.isEmpty()) {
        // No foreground candidates: drag-out from background lands at index 0.
        return DropZone.NewTrack(insertAt = 0)
    }
    return resolveGapZone(pointerY, sourceTrackId, sortedCandidates)
}

/**
 * Returns a candidate's track as the drop zone when [pointerY] falls inside its frame,
 * or `null` when the pointer sits in a gap between candidates.
 */
private fun resolveDirectCandidateHit(
    pointerY: Float,
    candidates: List<DropCandidate>,
): DropZone? = candidates
    .firstOrNull { pointerY in it.frame.top..it.frame.bottom }
    ?.let { DropZone.ExistingTrack(it.track) }

/**
 * Returns a [DropZone.NewTrack] for the gap [pointerY] sits in (above the topmost
 * candidate, below the bottommost, or between two adjacent ones), or the nearest
 * existing candidate when the drop would be a visual no-op — dragged clip solo in its
 * source and the gap is adjacent to that source.
 *
 * @param sortedCandidates ascending by `frame.top`.
 */
private fun resolveGapZone(
    pointerY: Float,
    sourceTrackId: String,
    sortedCandidates: List<DropCandidate>,
): DropZone? {
    // Source track is always in `sortedCandidates` here — it carries the dragged clip
    // and has a published frame — so the solo check can use the candidate set directly.
    val sourceIsSolo = sortedCandidates.find { it.track.id == sourceTrackId }?.track?.clips?.size == 1
    val topmost = sortedCandidates.first()
    val bottommost = sortedCandidates.last()

    if (pointerY < topmost.frame.top) {
        return if (sourceIsSolo && topmost.track.id == sourceTrackId) {
            nearestCandidate(pointerY, sortedCandidates)
        } else {
            DropZone.NewTrack(insertAt = topmost.tracksIndex)
        }
    }
    if (pointerY > bottommost.frame.bottom) {
        return if (sourceIsSolo && bottommost.track.id == sourceTrackId) {
            nearestCandidate(pointerY, sortedCandidates)
        } else {
            DropZone.NewTrack(insertAt = bottommost.tracksIndex + 1)
        }
    }
    // Between two adjacent candidates. The list runs top-to-bottom, so `upper` has the
    // smaller `tracksIndex`; the new track slots at `upper.tracksIndex + 1`.
    for (i in 0 until sortedCandidates.size - 1) {
        val upper = sortedCandidates[i]
        val lower = sortedCandidates[i + 1]
        if (pointerY > upper.frame.bottom && pointerY < lower.frame.top) {
            val sourceAdjacent = upper.track.id == sourceTrackId || lower.track.id == sourceTrackId
            return if (sourceIsSolo && sourceAdjacent) {
                nearestCandidate(pointerY, sortedCandidates)
            } else {
                DropZone.NewTrack(insertAt = upper.tracksIndex + 1)
            }
        }
    }
    return nearestCandidate(pointerY, sortedCandidates)
}

/**
 * Returns the candidate whose frame is vertically closest to [pointerY] as the drop
 * zone, or `null` when [candidates] is empty. Ties broken by list order.
 */
private fun nearestCandidate(
    pointerY: Float,
    candidates: List<DropCandidate>,
): DropZone? {
    val nearest = candidates.minByOrNull { cand ->
        maxOf(0f, maxOf(cand.frame.top - pointerY, pointerY - cand.frame.bottom))
    } ?: return null
    return DropZone.ExistingTrack(nearest.track)
}

/**
 * Returns the engine `pageChildren` index where a new track must be inserted so that,
 * after the data source rebuilds, the new track lands at `tracks[insertAt]`. Targets
 * new foreground or audio tracks only — the background track is a singleton and lives
 * outside [tracks].
 *
 * The data source prepends foreground (non-audio) tracks and appends audio tracks,
 * breaking any monotonic `tracks-index ↔ pageChildren-index` mapping, so the walk
 * classifies each child by kind:
 *  - **Foreground drag**: position just after the `(foregroundCount - insertAt)`-th
 *    foreground child.
 *  - **Audio drag**: position just before the `(insertAt - foregroundCount)`-th audio,
 *    or after all audios when `insertAt == tracks.size`.
 *
 * @param isAudioBlock Classifies each `pageChildren` entry by kind. Predicate-based to
 * keep the engine reference out of `dragdrop/`.
 * @param isBackgroundTrack Identifies the background `Track`, which lives outside
 * [tracks] and is skipped during the foreground walk.
 */
internal fun resolveAnchorPageIndex(
    dragged: Clip,
    tracks: List<Track>,
    pageChildren: List<DesignBlock>,
    insertAt: Int,
    isAudioBlock: (DesignBlock) -> Boolean,
    isBackgroundTrack: (DesignBlock) -> Boolean,
): Int {
    if (pageChildren.isEmpty()) return 0
    // Data-source invariant: all foreground tracks come before all audio tracks, so the
    // first audio track's index in [tracks] equals the foreground track count.
    val foregroundCount = tracks.indexOfFirst { it.clips.firstOrNull()?.clipType == ClipType.Audio }
        .let { if (it < 0) tracks.size else it }
    val clamped = insertAt.coerceIn(0, tracks.size)
    val isAudioDrag = dragged.clipType == ClipType.Audio
    return if (isAudioDrag) {
        // Audio: place NEW just before the `(clamped - foregroundCount)`-th audio in
        // pageChildren-audio order. Position past the last audio when there's no such
        // audio (insertAt == tracks.size case).
        val targetAudioOrdinal = (clamped - foregroundCount).coerceAtLeast(0)
        var seenAudios = 0
        var lastAudioPos = -1
        for ((i, child) in pageChildren.withIndex()) {
            if (isAudioBlock(child)) {
                if (seenAudios == targetAudioOrdinal) return i
                seenAudios++
                lastAudioPos = i
            }
        }
        // No audio at the target ordinal — insert just after the last audio (or at end if none).
        if (lastAudioPos >= 0) lastAudioPos + 1 else pageChildren.size
    } else {
        // Foreground: place NEW just after the `(foregroundCount - clamped)`-th foreground
        // child in pageChildren order. `foregroundCount - clamped == 0` means NEW is the
        // very first foreground child → start of pageChildren.
        val skipForeground = (foregroundCount - clamped).coerceAtLeast(0)
        var seenForeground = 0
        for ((i, child) in pageChildren.withIndex()) {
            if (seenForeground == skipForeground) return i
            if (!isAudioBlock(child) && !isBackgroundTrack(child)) seenForeground++
        }
        pageChildren.size
    }
}
