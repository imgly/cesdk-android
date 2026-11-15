package ly.img.editor.base.timeline.state

import ly.img.editor.base.timeline.clip.Clip
import ly.img.editor.base.timeline.clip.ClipType
import ly.img.engine.DesignBlock
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds

/**
 * A trim keeps the clips of a transition joined on an overlay track. The timeline draws a clip without the halves of
 * its transitions, so two clips that a transition joins touch in the drawing, and each one has a trim of half the
 * overlap at the shared end.
 */
class LiveTrimTest {
    @Test
    fun `the clip that a transition joins after the trimmed clip follows its end`() {
        val a = clip(id = 1, start = 0.seconds, tail = HALF_OVERLAP)
        val b = clip(id = 2, start = 4.seconds, lead = HALF_OVERLAP)

        assertEquals(mapOf(b.id to 3.seconds), computeLiveTrimOverrides(listOf(a, b), trim(a, end = 3.seconds)))
        assertEquals(mapOf(b.id to 5.seconds), computeLiveTrimOverrides(listOf(a, b), trim(a, end = 5.seconds)))
    }

    @Test
    fun `the clips that transitions join follow together and a clip after a gap stays`() {
        val a = clip(id = 1, start = 0.seconds, tail = HALF_OVERLAP)
        val b = clip(id = 2, start = 4.seconds, lead = HALF_OVERLAP, tail = HALF_OVERLAP)
        val c = clip(id = 3, start = 8.seconds, lead = HALF_OVERLAP)
        val d = clip(id = 4, start = 13.seconds)

        val overrides = computeLiveTrimOverrides(listOf(a, b, c, d), trim(a, end = 3.seconds))

        assertEquals(mapOf(b.id to 3.seconds, c.id to 7.seconds), overrides)
    }

    @Test
    fun `a clip next to the trimmed clip without a transition stays when the trimmed clip gets shorter`() {
        val a = clip(id = 1, start = 0.seconds)
        val b = clip(id = 2, start = 4.seconds)

        assertEquals(emptyMap<DesignBlock, Duration>(), computeLiveTrimOverrides(listOf(a, b), trim(a, end = 3.seconds)))
    }

    @Test
    fun `a clip that the engine already joined stays and the clip it now overlaps moves`() {
        // Engine timing after A grew from 4s to 5s: the engine moved B, which a 1s transition joins to A, to 4s, so B
        // ends at 8s. D has no transition and starts at 7.5s, where B ended at 7s before the trim.
        val a = clip(id = 1, start = 0.seconds, duration = 5.seconds, tail = HALF_OVERLAP)
        val b = clip(id = 2, start = 4.seconds, lead = HALF_OVERLAP)
        val d = clip(id = 4, start = 7.5.seconds)

        val overrides = computeLiveTrimOverrides(
            sorted = listOf(a, b, d),
            trim = trim(a, end = 5.seconds),
            transitionOverlap = { outgoing, incoming -> if (outgoing == a && incoming == b) 1.seconds else ZERO },
        )

        assertEquals(mapOf(d.id to 8.seconds), overrides)
    }

    @Test
    fun `a fixed transition also follows without a hidden inset`() {
        val a = clip(1, ZERO)
        val b = clip(2, 4.seconds).copy(joinedPredecessor = a.id)
        assertEquals(mapOf(b.id to 3.seconds), computeLiveTrimOverrides(listOf(a, b), trim(a, end = 3.seconds)))
    }

    private fun trim(
        clip: Clip,
        end: Duration,
    ) = LiveTrimState(clipId = clip.id, start = clip.timeOffset, end = end)

    private fun clip(
        id: Int,
        start: Duration,
        duration: Duration = 4.seconds,
        lead: Duration = ZERO,
        tail: Duration = ZERO,
    ) = Clip(
        id = id,
        clipType = ClipType.Video,
        timeOffset = start,
        duration = duration,
        joinedPredecessor = if (lead > ZERO) id - 1 else null,
        transitionTrimLead = lead,
        transitionTrimTail = tail,
    )

    private companion object {
        val HALF_OVERLAP = 0.5.seconds
    }
}
