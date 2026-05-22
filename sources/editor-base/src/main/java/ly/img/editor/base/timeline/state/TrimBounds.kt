package ly.img.editor.base.timeline.state

import ly.img.editor.base.timeline.clip.Clip
import ly.img.editor.base.timeline.track.sortedClips
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Total-duration caps on a clip after trimming, imposed by locked neighbors on the same
 * track. `Duration.INFINITE` (max) / `Duration.ZERO` (min) means no neighbor-imposed cap.
 * A clip is locked when [Clip.isLocked] is `true`.
 *
 * @property leadingMin Floor when dragging the leading handle.
 * @property leadingMax Ceiling when dragging the leading handle.
 * @property trailingMin Floor when dragging the trailing handle.
 * @property trailingMax Ceiling when dragging the trailing handle.
 */
internal data class TrimBounds(
    val leadingMax: Duration,
    val trailingMax: Duration,
    val leadingMin: Duration = Duration.ZERO,
    val trailingMin: Duration = Duration.ZERO,
)

/**
 * Computes [TrimBounds] for [clip]. Returns unbounded when any of these hold:
 * - [clip] isn't in any track.
 * - The track is foreground but isn't engine-backed (standalone foreground clips are direct
 *   page children).
 * - The track has fewer than 2 clips.
 *
 * Bg track: width is pinned to [Clip.duration] when any later bg sibling is locked
 * (any duration change cascades through them on engine pack). Unbounded otherwise.
 */
internal fun TimelineDataSource.trimBounds(clip: Clip): TrimBounds {
    val unbounded = TrimBounds(Duration.INFINITE, Duration.INFINITE)

    if (backgroundTrack.clips.any { it.id == clip.id }) {
        return backgroundTrimBounds(clip) ?: unbounded
    }

    val track = tracks.find { it.clips.contains(clip) } ?: return unbounded
    if (track.engineTrackId == null || track.clips.size < 2) return unbounded

    val sorted = track.sortedClips()
    val pivotIndex = sorted.indexOfFirst { it.id == clip.id }
    if (pivotIndex < 0) return unbounded

    // Leading: walk predecessors nearest-first, stop at first locked one.
    var durationBefore = 0.seconds
    var wallFloor: Duration? = null
    for (i in pivotIndex - 1 downTo 0) {
        val neighbor = sorted[i]
        if (neighbor.isLocked) {
            wallFloor = neighbor.timeOffset + neighbor.duration + durationBefore
            break
        }
        durationBefore += neighbor.duration
    }
    val earliestStart = wallFloor ?: durationBefore
    val leadingMax = (clip.duration + (clip.timeOffset - earliestStart))
        .coerceAtLeast(clip.duration)

    // Trailing: walk successors nearest-first, stop at first locked one.
    var durationAfter = 0.seconds
    var wallCeiling: Duration? = null
    for (i in pivotIndex + 1 until sorted.size) {
        val neighbor = sorted[i]
        if (neighbor.isLocked) {
            wallCeiling = neighbor.timeOffset - durationAfter
            break
        }
        durationAfter += neighbor.duration
    }
    val trailingMax = wallCeiling?.minus(clip.timeOffset)
        ?.coerceAtLeast(clip.duration)
        ?: Duration.INFINITE

    return TrimBounds(
        leadingMax = leadingMax,
        trailingMax = trailingMax,
    )
}

/** Width-pinned bounds when any later bg sibling is locked; `null` otherwise. */
private fun TimelineDataSource.backgroundTrimBounds(clip: Clip): TrimBounds? {
    val sorted = backgroundTrack.sortedClips()
    val pivotIndex = sorted.indexOfFirst { it.id == clip.id }
    if (pivotIndex < 0) return null
    for (i in pivotIndex + 1 until sorted.size) {
        if (sorted[i].isLocked) {
            return TrimBounds(
                leadingMax = clip.duration,
                trailingMax = clip.duration,
                leadingMin = clip.duration,
                trailingMin = clip.duration,
            )
        }
    }
    return null
}
