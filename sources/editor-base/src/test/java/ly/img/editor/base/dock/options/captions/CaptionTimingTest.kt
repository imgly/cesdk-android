package ly.img.editor.base.dock.options.captions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class CaptionTimingTest {
    // region Split

    @Test
    fun `split divides the duration in proportion to the text`() {
        val (left, right) = CaptionTiming.splitDurations(totalDuration = 4.0, leftLength = 3, totalLength = 12)
        assertEquals(1.0, left, EPSILON)
        assertEquals(3.0, right, EPSILON)
    }

    @Test
    fun `the two halves always tile the original exactly`() {
        // A length that does not divide evenly is where a second proportional computation would drift.
        val total = 7.0
        for (leftLength in 1 until 13) {
            val (left, right) = CaptionTiming.splitDurations(total, leftLength, totalLength = 13)
            assertEquals("left=$leftLength", total, left + right, 0.0)
        }
    }

    @Test
    fun `split of a zero-length text does not divide by zero`() {
        val (left, right) = CaptionTiming.splitDurations(totalDuration = 3.0, leftLength = 0, totalLength = 0)
        assertEquals(0.0, left, EPSILON)
        assertEquals(3.0, right, EPSILON)
    }

    @Test
    fun `split never produces a negative duration`() {
        val (left, right) = CaptionTiming.splitDurations(totalDuration = -1.0, leftLength = 2, totalLength = 4)
        assertTrue(left >= 0.0)
        assertTrue(right >= 0.0)
    }

    // endregion
    // region Split at the playhead

    @Test
    fun `the playhead offset is the inverse of the duration split`() {
        val start = 2.0
        val duration = 6.0
        val textLength = 12
        for (step in 1 until 12) {
            val playhead = start + duration * step / 12.0
            val offset = CaptionTiming.splitOffsetAtTime(textLength, start, duration, playhead)
            val (left, _) = CaptionTiming.splitDurations(duration, leftLength = offset, totalLength = textLength)
            assertEquals("step=$step", playhead - start, left, EPSILON)
        }
    }

    @Test
    fun `the playhead offset is clamped into the text`() {
        assertEquals(0, CaptionTiming.splitOffsetAtTime(textLength = 10, start = 2.0, duration = 4.0, playhead = 0.0))
        assertEquals(10, CaptionTiming.splitOffsetAtTime(textLength = 10, start = 2.0, duration = 4.0, playhead = 9.0))
    }

    @Test
    fun `a caption with no duration or no text maps to the start of the text`() {
        assertEquals(0, CaptionTiming.splitOffsetAtTime(textLength = 10, start = 0.0, duration = 0.0, playhead = 1.0))
        assertEquals(0, CaptionTiming.splitOffsetAtTime(textLength = 0, start = 0.0, duration = 4.0, playhead = 2.0))
    }

    @Test
    fun `the playhead offset rounds to the nearest character`() {
        // Half way through a 5-character caption is 2.5 characters in, which rounds up.
        assertEquals(3, CaptionTiming.splitOffsetAtTime(textLength = 5, start = 0.0, duration = 4.0, playhead = 2.0))
    }

    @Test
    fun `the word snap moves the cut away from the proportional offset`() {
        // Why a playhead split cannot take its duration from the character ratio: the snap routinely lands
        // several characters from where the playhead pointed, and a ratio computed from the snapped text
        // would put the clip edge there instead of under the playhead.
        val text = "a very long word here"
        val start = 0.0
        val duration = 10.0
        val playhead = 4.9
        val proportional = CaptionTiming.splitOffsetAtTime(text.length, start, duration, playhead)
        val snapped = checkNotNull(CaptionTiming.wordBoundary(text, nearestTo = proportional))
        assertNotEquals(proportional, snapped)
        val (fromRatio, _) = CaptionTiming.splitDurations(duration, leftLength = snapped, totalLength = text.length)
        // The two bases disagree by more than a rounding error, so the caller has to pick deliberately.
        assertTrue("ratio=$fromRatio playhead=${playhead - start}", abs(fromRatio - (playhead - start)) > 0.5)
    }

    @Test
    fun `every playhead the split control offers clears the caption floor`() {
        // The Split button gates the playhead to a margin equal to the timeline's caption floor, and the cut is
        // taken from the playhead — so the gate is what keeps both halves grabbable on the timeline.
        val margin = MINIMUM_CAPTION_DURATION_SECONDS
        for (duration in listOf(0.25, 1.0, 3.0, 8.0)) {
            var offered = 0
            var leftDuration = margin
            while (leftDuration < duration - margin) {
                assertTrue(
                    "duration=$duration leftDuration=$leftDuration",
                    CaptionTiming.splitClearsFloor(totalDuration = duration, leftDuration = leftDuration),
                )
                offered++
                leftDuration += 0.01
            }
            assertTrue("duration=$duration offered nothing", offered > 0)
        }
    }

    // endregion
    // region Word gap snapping

    @Test
    fun `a split snaps to the position after a space`() {
        assertEquals(6, CaptionTiming.wordBoundary("hello world again", nearestTo = 8))
    }

    @Test
    fun `a target already on a gap is kept`() {
        assertEquals(2, CaptionTiming.wordBoundary("a bc", nearestTo = 2))
    }

    @Test
    fun `the earlier gap wins a tie`() {
        assertEquals(3, CaptionTiming.wordBoundary("ab cd ef", nearestTo = 4))
        assertEquals(2, CaptionTiming.wordBoundary("a b c", nearestTo = 3))
    }

    @Test
    fun `text with no gaps falls back to the target`() {
        assertEquals(8, CaptionTiming.wordBoundary("supercalifragilistic", nearestTo = 8))
    }

    @Test
    fun `a gapless fallback never lands inside a grapheme cluster`() {
        // Every odd offset is mid-surrogate-pair, and there is no gap to snap to.
        val text = "😀😀😀😀"
        val offset = checkNotNull(CaptionTiming.wordBoundary(text, nearestTo = 3))
        assertTrue(CaptionTiming.isGraphemeBoundary(text, offset))
    }

    @Test
    fun `snapping never returns an offset the caret split would refuse`() {
        // The Split control gates on the playhead and on the text being divisible, so an offset with an empty
        // side would turn an enabled button into a silent no-op.
        val texts = listOf(
            "hello world",
            " leading and trailing ",
            "a  double  space",
            "one",
            "he",
            "😀 emoji 😀",
            "😀😀",
            "trailing space ",
            // A combining mark, ZWJ or variation selector after a space keeps its cluster open.
            "hello \u0301world",
            "ab \u0300\u0301 cd",
            "a \u200Db",
            "a \uFE0Fb",
        )
        for (text in texts) {
            for (target in 0..text.length) {
                val offset = CaptionTiming.wordBoundary(text, nearestTo = target)
                assertNotNull("$text@$target", offset)
                assertTrue("$text@$target -> $offset", offset!! in 1 until text.length)
                assertTrue("$text@$target -> $offset", CaptionTiming.isGraphemeBoundary(text, offset))
            }
        }
    }

    @Test
    fun `a caption with nothing to divide has no split offset`() {
        for (text in listOf("", "a", "😀", "👩‍💻")) {
            assertFalse(text, CaptionTiming.isDivisible(text))
            assertNull(text, CaptionTiming.wordBoundary(text, nearestTo = text.length / 2))
        }
    }

    @Test
    fun `two characters are divisible however they are encoded`() {
        for (text in listOf("ab", "a ", " a", "😀😀", "a😀", "👩‍💻x")) {
            assertTrue(text, CaptionTiming.isDivisible(text))
            assertNotNull(text, CaptionTiming.wordBoundary(text, nearestTo = 1))
        }
    }

    // endregion
    // region Split texts

    @Test
    fun `an offset divides the text in two`() {
        assertEquals("hello" to " world", CaptionTiming.splitTexts("hello world", 5))
    }

    @Test
    fun `an offset with an empty side is refused`() {
        assertNull(CaptionTiming.splitTexts("hello", 0))
        assertNull(CaptionTiming.splitTexts("hello", 5))
        assertNull(CaptionTiming.splitTexts("", 0))
    }

    @Test
    fun `an out-of-range offset is refused`() {
        assertNull(CaptionTiming.splitTexts("hello", -1))
        assertNull(CaptionTiming.splitTexts("hello", 6))
    }

    @Test
    fun `an offset inside a grapheme cluster is refused`() {
        assertNull(CaptionTiming.splitTexts("a😀b", 2))
        assertNull(CaptionTiming.splitTexts("👩‍💻x", 2))
    }

    // endregion
    // region Merge

    @Test
    fun `merging spans from the previous start to the current end`() {
        // [0-2] and [3-5] with a gap between them merge into [0-5], swallowing the gap.
        val duration = CaptionTiming.mergedDuration(
            previousStart = 0.0,
            previousDuration = 2.0,
            currentStart = 3.0,
            currentDuration = 2.0,
        )
        assertEquals(5.0, duration, EPSILON)
    }

    @Test
    fun `merging a caption nested inside the previous one keeps the previous duration`() {
        // [0-10] swallowing [2-4] must stay 10 long rather than shrinking to 4.
        val duration = CaptionTiming.mergedDuration(
            previousStart = 0.0,
            previousDuration = 10.0,
            currentStart = 2.0,
            currentDuration = 2.0,
        )
        assertEquals(10.0, duration, EPSILON)
    }

    @Test
    fun `merging overlapping captions does not overcount the overlap`() {
        // [0-3] and [2-5] overlap by 1s; the result spans 0-5, not 0-6.
        val duration = CaptionTiming.mergedDuration(
            previousStart = 0.0,
            previousDuration = 3.0,
            currentStart = 2.0,
            currentDuration = 3.0,
        )
        assertEquals(5.0, duration, EPSILON)
    }

    @Test
    fun `merged text joins with a single space`() {
        assertEquals("Hello world", CaptionTiming.mergedText("Hello", "world"))
    }

    @Test
    fun `merging with an empty side adds no separator`() {
        assertEquals("world", CaptionTiming.mergedText("", "world"))
        assertEquals("Hello", CaptionTiming.mergedText("Hello", ""))
        assertEquals("", CaptionTiming.mergedText("", ""))
    }

    // endregion
    // region Caret

    @Test
    fun `the caret keeps its character across a merge`() {
        // Backspace at the start of "world" lands exactly on the join.
        assertEquals(6, CaptionTiming.mergedCaret(previousText = "Hello", currentText = "world", caretInCurrent = 0))
        // A caret mid-word stays mid-word.
        assertEquals(8, CaptionTiming.mergedCaret(previousText = "Hello", currentText = "world", caretInCurrent = 2))
    }

    @Test
    fun `the caret skips the separator when one side is empty`() {
        assertEquals(0, CaptionTiming.mergedCaret(previousText = "", currentText = "world", caretInCurrent = 0))
        assertEquals(5, CaptionTiming.mergedCaret(previousText = "Hello", currentText = "", caretInCurrent = 0))
    }

    // endregion

    // region Inserted duration

    @Test
    fun `a caption with no follower gets the full default duration`() {
        assertEquals(DEFAULT_CAPTION_DURATION_SECONDS, CaptionTiming.insertedDuration(start = 4.0, nextStart = null), EPSILON)
    }

    @Test
    fun `a gap wider than the default still only gets the default`() {
        assertEquals(DEFAULT_CAPTION_DURATION_SECONDS, CaptionTiming.insertedDuration(start = 1.0, nextStart = 9.0), EPSILON)
    }

    @Test
    fun `a narrow gap is filled exactly, so nothing downstream moves`() {
        assertEquals(1.0, CaptionTiming.insertedDuration(start = 1.0, nextStart = 2.0), EPSILON)
    }

    @Test
    fun `exactly contiguous siblings get the default and let the track ripple`() {
        // There is no gap to fit, so a zero-width one would only produce a caption the timeline cannot grab.
        assertEquals(DEFAULT_CAPTION_DURATION_SECONDS, CaptionTiming.insertedDuration(start = 3.0, nextStart = 3.0), EPSILON)
    }

    @Test
    fun `a gap too small to be usable is floored rather than filled exactly`() {
        assertEquals(MINIMUM_CAPTION_DURATION_SECONDS, CaptionTiming.insertedDuration(start = 3.0, nextStart = 3.05), EPSILON)
    }

    @Test
    fun `a follower that starts before the insertion point falls back to the default`() {
        assertEquals(DEFAULT_CAPTION_DURATION_SECONDS, CaptionTiming.insertedDuration(start = 5.0, nextStart = 2.0), EPSILON)
    }

    // endregion

    // region Grapheme boundaries

    @Test
    fun `the ends of the text are always splittable`() {
        assertTrue(CaptionTiming.isGraphemeBoundary("Hi", 0))
        assertTrue(CaptionTiming.isGraphemeBoundary("Hi", 2))
        assertTrue(CaptionTiming.isGraphemeBoundary("", 0))
    }

    @Test
    fun `a split inside a single user-perceived character is refused`() {
        // Surrogate pair, combining mark, ZWJ sequence, regional-indicator flag.
        assertFalse(CaptionTiming.isGraphemeBoundary("a\uD83D\uDE00b", 2))
        assertFalse(CaptionTiming.isGraphemeBoundary("ae\u0301b", 2))
        assertFalse(CaptionTiming.isGraphemeBoundary("\uD83D\uDC69\u200D\uD83D\uDCBB", 2))
        assertFalse(CaptionTiming.isGraphemeBoundary("\uD83C\uDDF5\uD83C\uDDF9", 2))
    }

    @Test
    fun `a split between two characters is allowed`() {
        assertTrue(CaptionTiming.isGraphemeBoundary("a\uD83D\uDE00b", 1))
        assertTrue(CaptionTiming.isGraphemeBoundary("a\uD83D\uDE00b", 3))
        assertTrue(CaptionTiming.isGraphemeBoundary("Hello world", 5))
    }

    // endregion
    // region splitClearsFloor

    @Test
    fun `a cut too close to the start is refused`() {
        assertFalse(CaptionTiming.splitClearsFloor(totalDuration = 1.0, leftDuration = 0.025))
    }

    @Test
    fun `a cut too close to the end is refused`() {
        assertFalse(CaptionTiming.splitClearsFloor(totalDuration = 1.0, leftDuration = 0.975))
    }

    @Test
    fun `a cut with room on both sides is allowed`() {
        assertTrue(CaptionTiming.splitClearsFloor(totalDuration = 4.0, leftDuration = 2.0))
    }

    @Test
    fun `a cut exactly on the floor is allowed`() {
        val total = 2 * MINIMUM_CAPTION_DURATION_SECONDS
        assertTrue(CaptionTiming.splitClearsFloor(totalDuration = total, leftDuration = MINIMUM_CAPTION_DURATION_SECONDS))
    }

    @Test
    fun `a caption too short for two halves is refused wherever it is cut`() {
        val total = MINIMUM_CAPTION_DURATION_SECONDS
        assertFalse(CaptionTiming.splitClearsFloor(totalDuration = total, leftDuration = total / 2))
        assertFalse(CaptionTiming.splitClearsFloor(totalDuration = 0.0, leftDuration = 0.0))
    }

    // endregion

    private companion object {
        const val EPSILON = 1e-9
    }
}
