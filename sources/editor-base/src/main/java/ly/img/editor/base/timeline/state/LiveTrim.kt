package ly.img.editor.base.timeline.state

import ly.img.editor.base.timeline.clip.Clip
import ly.img.engine.DesignBlock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Live state for an in-progress trim gesture. The timeline keeps one per track and feeds it
 * to [computeLiveTrimOverrides] to derive sibling positions while the gesture runs.
 *
 * @property clipId The clip being trimmed.
 * @property start The clip's [Clip.timeOffset] during the gesture.
 * @property end The clip's end during the gesture. Always `>= start`.
 */
data class LiveTrimState(
    val clipId: DesignBlock,
    val start: Duration,
    val end: Duration,
)

/**
 * Computes sibling shifts that keep the dragged clip from overlapping its neighbors. Unlocked
 * clips on each side are pushed outward by the minimum amount; the cascade stops at the first
 * locked clip ([Clip.isLocked] `true`), which never moves.
 *
 * @param sorted Track clips sorted by `timeOffset` ascending; must include the dragged clip.
 * @param trim Dragged clip bounds.
 * @param clampStartToZero When `true` (default) floors predecessor offsets at `0.seconds`.
 * Background track previews pass `false` so predecessors can visually slide past zero — the
 * engine re-packs them on commit regardless.
 * @return Overridden positions keyed by clip id; clips absent keep their engine offset, and
 * the dragged clip is never included. Empty when the clip being trimmed is not in [sorted].
 */
internal fun computeLiveTrimOverrides(
    sorted: List<Clip>,
    trim: LiveTrimState,
    clampStartToZero: Boolean = true,
): Map<DesignBlock, Duration> {
    val pivotIndex = sorted.indexOfFirst { it.id == trim.clipId }
    if (pivotIndex < 0) return emptyMap()

    val overrides = mutableMapOf<DesignBlock, Duration>()

    // [sorted] is assumed non-overlapping: once a neighbor doesn't need pushing, no further
    // neighbor will either, so the walk stops.

    // Walk left
    var endLimit = trim.start
    for (i in pivotIndex - 1 downTo 0) {
        val neighbor = sorted[i]
        if (neighbor.isLocked) break
        val neighborEnd = neighbor.timeOffset + neighbor.duration
        if (neighborEnd <= endLimit) break
        val newOffset = (endLimit - neighbor.duration)
            .let { if (clampStartToZero) it.coerceAtLeast(0.seconds) else it }
        overrides[neighbor.id] = newOffset
        endLimit = newOffset
    }

    // Walk right
    var startLimit = trim.end
    for (i in pivotIndex + 1 until sorted.size) {
        val neighbor = sorted[i]
        if (neighbor.isLocked) break
        if (neighbor.timeOffset >= startLimit) break
        overrides[neighbor.id] = startLimit
        startLimit += neighbor.duration
    }

    return overrides
}
