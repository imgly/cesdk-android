package ly.img.editor.base.components.numeric

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** A 300 px track over −1…1 has 150 px per unit, so the 6 px capture zone is ±0.04 and the 12 px escape is ±0.08. */
class SnapDetentsTest {
    private val detents = SnapDetents(targets = listOf(0f))

    private fun advance(raw: Float) = detents.advance(
        rawValue = raw,
        pxPerValue = PX_PER_VALUE,
        captureZonePx = CAPTURE_PX,
        escapeDistancePx = ESCAPE_PX,
    )

    @Test
    fun `a drag that starts on the target does not latch`() {
        val start = advance(0.01f)

        assertEquals(0.01f, start.value)
        assertFalse(start.didCapture)
        assertEquals(SnapDetents.State.Disarmed, detents.state)
    }

    @Test
    fun `entering the capture zone snaps and reports the capture once`() {
        advance(0.3f)
        val capture = advance(0.03f)
        val held = advance(0.02f)

        assertEquals(0f, capture.value)
        assertTrue(capture.didCapture)
        assertEquals(0f, held.value)
        assertFalse(held.didCapture)
    }

    @Test
    fun `the target holds inside the escape distance and releases beyond it`() {
        advance(0.3f)
        advance(0.03f)

        val inside = advance(-0.07f)
        val outside = advance(-0.09f)

        assertEquals(0f, inside.value)
        assertEquals(-0.09f, outside.value)
        assertEquals(SnapDetents.State.Armed, detents.state)
    }

    @Test
    fun `it captures again after it re-arms`() {
        advance(0.3f)
        advance(0.03f)
        advance(-0.09f)

        val again = advance(-0.02f)

        assertTrue(again.didCapture)
        assertEquals(0f, again.value)
    }

    @Test
    fun `reset disarms`() {
        advance(0.3f)
        advance(0.03f)

        detents.reset()

        assertEquals(SnapDetents.State.Disarmed, detents.state)
        assertEquals(0.02f, advance(0.02f).value)
    }

    @Test
    fun `no width disarms and passes the raw value`() {
        advance(0.3f)

        val result = detents.advance(0.01f, pxPerValue = 0f, captureZonePx = CAPTURE_PX, escapeDistancePx = ESCAPE_PX)

        assertEquals(0.01f, result.value)
        assertEquals(SnapDetents.State.Disarmed, detents.state)
    }

    @Test
    fun `a coarse grid has no targets`() {
        // 0.1 × 150 px = 15 px, wider than the 6 px zone.
        val coarse = SnapDetents.targets(listOf(0f), step = 0.1f, range = -1f..1f, PX_PER_VALUE, CAPTURE_PX)
        // 0.01 × 150 px = 1.5 px.
        val fine = SnapDetents.targets(listOf(0f), step = 0.01f, range = -1f..1f, PX_PER_VALUE, CAPTURE_PX)

        assertEquals(emptyList<Float>(), coarse)
        assertEquals(listOf(0f), fine)
    }

    @Test
    fun `targets sit on the step grid inside the range`() {
        val targets = SnapDetents.targets(listOf(0.333f, 1.2f), step = 0.01f, range = -1f..1f, PX_PER_VALUE, CAPTURE_PX)

        assertEquals(0.33f, targets[0], 1e-6f)
        assertEquals(1f, targets[1], 1e-6f)
    }

    private companion object {
        const val PX_PER_VALUE = 150f
        const val CAPTURE_PX = 6f
        const val ESCAPE_PX = 12f
    }
}
