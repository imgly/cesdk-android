package ly.img.editor.base.timeline.state

import ly.img.engine.DesignBlock
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * The timeline draws a clip without the halves of its transitions, and the drag draws the dragged clip and the ends it
 * shares a transition with at full length. The engine stores raw offsets, in which two clips with a transition overlap
 * by the transition duration.
 */
class DropOffsetsTest {
    @Test
    fun `a clip dropped between two clips sits next to them after their transitions are removed`() {
        // Three 5s clips with 1.2s transitions at both cuts: A 0s, B 3.8s, C 7.6s. C is dropped between A and B,
        // so both cuts changed and both transitions are gone. The drag drew C from A's drawn end, 4.4s, and pushed B
        // to C's full end.
        val a = dropClip(id = 1, offset = 0.seconds, drawnStart = 0.seconds, drawnDuration = 4.4.seconds)
        val c = dropClip(id = 3, offset = 7.6.seconds, drawnStart = 4.4.seconds)
        val b = dropClip(id = 2, offset = 3.8.seconds, drawnStart = 9.4.seconds, drawnDuration = 4.4.seconds)

        val offsets = computeDropOffsets(listOf(a, c, b), c.id, setOf(c.id, b.id)) { _, _ -> ZERO }

        assertNear(5.seconds, offsets.getValue(c.id))
        assertNear(10.seconds, offsets.getValue(b.id))
        assertEquals(null, offsets[a.id])
    }

    @Test
    fun `a transition that keeps its cut keeps its overlap`() {
        // B and C keep a 1.2s transition and are pushed right together by a 2s clip N dropped in front of them.
        val n = dropClip(id = 4, offset = 20.seconds, drawnStart = ZERO, duration = 2.seconds)
        val b = dropClip(id = 2, offset = 0.seconds, drawnStart = 2.seconds, drawnDuration = 4.4.seconds)
        val c = dropClip(id = 3, offset = 3.8.seconds, drawnStart = 6.4.seconds, drawnDuration = 4.4.seconds)

        val offsets = computeDropOffsets(listOf(n, b, c), n.id, setOf(n.id, b.id, c.id)) { outgoing, incoming ->
            if (outgoing == b.id && incoming == c.id) 1.2.seconds else ZERO
        }

        assertNear(ZERO, offsets.getValue(n.id))
        assertNear(2.seconds, offsets.getValue(b.id))
        assertNear(5.8.seconds, offsets.getValue(c.id))
    }

    @Test
    fun `a dragged clip lands where its card was dropped`() {
        // P and D overlap by a 2s transition: P 0s-5s, D 3s-8s. The drag drew P to its full end, and D is dropped
        // in its own slot at 6s.
        val p = dropClip(id = 1, offset = 0.seconds, drawnStart = 0.seconds)
        val d = dropClip(id = 2, offset = 3.seconds, drawnStart = 6.seconds)

        val offsets = computeDropOffsets(listOf(p, d), d.id, setOf(d.id)) { _, _ -> 2.seconds }

        assertNear(6.seconds, offsets.getValue(d.id))
    }

    @Test
    fun `the next partner that a dragged clip pushes keeps the overlap of their transition`() {
        // P, D and N overlap by 2s transitions: P 0s-5s, D 3s-8s, N 6s-11s, then E 11s-16s. The drag draws P to its
        // full end, so D is dropped at 6.5s, drawn to 11.5s, and pushes N, drawn from its raw start, to 11.5s and E
        // to 16.5s.
        val p = dropClip(id = 1, offset = 0.seconds, drawnStart = 0.seconds)
        val d = dropClip(id = 2, offset = 3.seconds, drawnStart = 6.5.seconds)
        val n = dropClip(id = 3, offset = 6.seconds, drawnStart = 11.5.seconds)
        val e = dropClip(id = 4, offset = 11.seconds, drawnStart = 16.5.seconds)

        val offsets = computeDropOffsets(listOf(p, d, n, e), d.id, setOf(d.id, n.id, e.id)) { _, incoming ->
            if (incoming == e.id) ZERO else 2.seconds
        }

        assertEquals(null, offsets[p.id])
        assertNear(6.5.seconds, offsets.getValue(d.id))
        assertNear(9.5.seconds, offsets.getValue(n.id))
        assertNear(14.5.seconds, offsets.getValue(e.id))
    }

    @Test
    fun `a clip that a pushed clip ends against stays against it`() {
        // As above, but E sits at 16.5s, where the pushed N ends in the drawing, so the drop does not push E.
        val p = dropClip(id = 1, offset = 0.seconds, drawnStart = 0.seconds)
        val d = dropClip(id = 2, offset = 3.seconds, drawnStart = 6.5.seconds)
        val n = dropClip(id = 3, offset = 6.seconds, drawnStart = 11.5.seconds)
        val e = dropClip(id = 4, offset = 16.5.seconds, drawnStart = 16.5.seconds)

        val offsets = computeDropOffsets(listOf(p, d, n, e), d.id, setOf(d.id, n.id)) { _, incoming ->
            if (incoming == e.id) ZERO else 2.seconds
        }

        assertNear(9.5.seconds, offsets.getValue(n.id))
        assertNear(14.5.seconds, offsets.getValue(e.id))
    }

    @Test
    fun `a dragged clip dropped against its next partner keeps its place`() {
        // D and N overlap by a 2s transition: D 3s-8s, N 6s-11s. The drag draws N from its raw start, so D's slot
        // against N starts at 1s. A drop on that slot, or less than 1ms before it, writes nothing.
        val n = dropClip(id = 3, offset = 6.seconds, drawnStart = 6.seconds)
        for (drawnStart in listOf(1.seconds, 0.9995.seconds)) {
            val d = dropClip(id = 2, offset = 3.seconds, drawnStart = drawnStart)

            val offsets = computeDropOffsets(listOf(d, n), d.id, setOf(d.id)) { _, _ -> 2.seconds }

            assertEquals(emptyMap<DesignBlock, Duration>(), offsets)
        }
    }

    @Test
    fun `a gap that the drop leaves stays`() {
        val a = dropClip(id = 1, offset = 0.seconds, drawnStart = 0.seconds)
        val c = dropClip(id = 3, offset = 12.seconds, drawnStart = 7.seconds)

        val offsets = computeDropOffsets(listOf(a, c), c.id, setOf(c.id)) { _, _ -> ZERO }

        assertNear(7.seconds, offsets.getValue(c.id))
    }

    private fun dropClip(
        id: DesignBlock,
        offset: Duration,
        drawnStart: Duration,
        duration: Duration = 5.seconds,
        drawnDuration: Duration = duration,
    ) = DropClip(
        id = id,
        timeOffset = offset,
        duration = duration,
        drawnStart = drawnStart,
        drawnEnd = drawnStart + drawnDuration,
    )

    private fun assertNear(
        expected: Duration,
        actual: Duration,
    ) {
        assertTrue("expected $expected, was $actual", (expected - actual).absoluteValue < 1.milliseconds)
    }
}
