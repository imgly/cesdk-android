package ly.img.editor.base.timeline.state

import ly.img.editor.base.timeline.clip.Clip
import ly.img.editor.base.timeline.clip.ClipType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The timeline renders a clip without the halves of its transitions, and a drop reports rendered offsets. The engine
 * stores raw offsets, in which two clips with a transition overlap by the transition duration.
 */
class DropOffsetsTest {
    @Test
    fun `a clip dropped between two clips sits next to them after their transitions are removed`() {
        // Three 5s clips with 1.2s transitions at both cuts: A 0s, B 3.8s, C 7.6s. C is dropped between A and B,
        // so both cuts changed and both transitions are gone. The preview put C at A's rendered end, 4.4s, and
        // pushed B to 8.8s.
        val a = clip(id = 1, offset = 0.seconds)
        val c = clip(id = 3, offset = 7.6.seconds)
        val b = clip(id = 2, offset = 3.8.seconds)

        val offsets = computeDropOffsets(
            clips = listOf(a, c, b),
            renderedOffsets = mapOf(c.id to 4.4.seconds, b.id to 8.8.seconds),
            transitionOverlap = { _, _ -> ZERO },
        )

        assertNear(5.seconds, offsets.getValue(c.id))
        assertNear(10.seconds, offsets.getValue(b.id))
        assertEquals(null, offsets[a.id])
    }

    @Test
    fun `a transition that keeps its cut keeps its overlap`() {
        // B and C keep a 1.2s transition and are pushed right together by a 2s clip N dropped in front of them.
        // Rendered, B starts at 2s and C at 2s + 4.4s.
        val n = clip(id = 4, offset = 20.seconds, duration = 2.seconds)
        val b = clip(id = 2, offset = 0.seconds)
        val c = clip(id = 3, offset = 3.8.seconds)

        val offsets = computeDropOffsets(
            clips = listOf(n, b, c),
            renderedOffsets = mapOf(n.id to ZERO, b.id to 2.seconds, c.id to 6.4.seconds),
            transitionOverlap = { outgoing, incoming -> if (outgoing == b && incoming == c) 1.2.seconds else ZERO },
        )

        assertNear(ZERO, offsets.getValue(n.id))
        assertNear(2.seconds, offsets.getValue(b.id))
        assertNear(5.8.seconds, offsets.getValue(c.id))
    }

    @Test
    fun `a gap that the drop leaves stays`() {
        val a = clip(id = 1, offset = 0.seconds)
        val c = clip(id = 3, offset = 12.seconds)

        val offsets = computeDropOffsets(
            clips = listOf(a, c),
            renderedOffsets = mapOf(c.id to 7.seconds),
            transitionOverlap = { _, _ -> ZERO },
        )

        assertNear(7.seconds, offsets.getValue(c.id))
    }

    private fun clip(
        id: Int,
        offset: Duration,
        duration: Duration = 5.seconds,
    ) = Clip(id = id, clipType = ClipType.Video, timeOffset = offset, duration = duration)

    private fun assertNear(
        expected: Duration,
        actual: Duration,
    ) {
        assertTrue("expected $expected, was $actual", (expected - actual).absoluteValue < 1.milliseconds)
    }
}
