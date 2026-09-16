package ly.img.editor.plugin.autoCaptions

import org.junit.Assert.assertEquals
import org.junit.Test

/** Covers the word grouping that turns a provider's word-level timestamps into subtitle cues. */
class WordsToCuesTest {
    private fun words(vararg texts: String) = texts.mapIndexed { index, text ->
        TimedWord(text, start = index.toDouble(), end = index + 1.0)
    }

    @Test
    fun `empty words produce no cues`() {
        assertEquals(emptyList<SubtitleCue>(), cuesFrom(emptyList(), maxLineLength = 37, maxLines = 1))
    }

    @Test
    fun `a single short line becomes one cue`() {
        assertEquals(
            listOf(SubtitleCue(0.0, 3.0, "one two three")),
            cuesFrom(words("one", "two", "three"), maxLineLength = 37, maxLines = 1),
        )
    }

    @Test
    fun `cue timestamps span the first to the last word`() {
        val words = listOf(TimedWord("a", 1.5, 2.0), TimedWord("b", 2.0, 4.25))
        assertEquals(
            listOf(SubtitleCue(1.5, 4.25, "a b")),
            cuesFrom(words, maxLineLength = 37, maxLines = 1),
        )
    }

    @Test
    fun `a full line starts a new cue when maxLines is one`() {
        // "aaaa bbbb" is 9 code units and the limit is 8, so the second word starts a second cue.
        val words = listOf(TimedWord("aaaa", 0.0, 1.0), TimedWord("bbbb", 1.0, 2.0))
        assertEquals(
            listOf(SubtitleCue(0.0, 1.0, "aaaa"), SubtitleCue(1.0, 2.0, "bbbb")),
            cuesFrom(words, maxLineLength = 8, maxLines = 1),
        )
    }

    @Test
    fun `a full line wraps into a second line when maxLines is two`() {
        val words = listOf(TimedWord("aaaa", 0.0, 1.0), TimedWord("bbbb", 1.0, 2.0))
        assertEquals(
            listOf(SubtitleCue(0.0, 2.0, "aaaa\nbbbb")),
            cuesFrom(words, maxLineLength = 8, maxLines = 2),
        )
    }

    @Test
    fun `a full two line cue flushes before the next word`() {
        val words = listOf(
            TimedWord("aaaa", 0.0, 1.0),
            TimedWord("bbbb", 1.0, 2.0),
            TimedWord("cccc", 2.0, 3.0),
        )
        assertEquals(
            listOf(SubtitleCue(0.0, 2.0, "aaaa\nbbbb"), SubtitleCue(2.0, 3.0, "cccc")),
            cuesFrom(words, maxLineLength = 8, maxLines = 2),
        )
    }

    @Test
    fun `an overlong single word gets its own line`() {
        // A word longer than the limit cannot be broken, so it stays whole rather than being dropped.
        val words = listOf(TimedWord("short", 0.0, 1.0), TimedWord("aaaaaaaaaaaaaaa", 1.0, 2.0))
        assertEquals(
            listOf(SubtitleCue(0.0, 1.0, "short"), SubtitleCue(1.0, 2.0, "aaaaaaaaaaaaaaa")),
            cuesFrom(words, maxLineLength = 8, maxLines = 1),
        )
    }

    @Test
    fun `line length is measured in UTF-16 code units`() {
        // An emoji outside the BMP is two code units, so "ab 😀" is 5 and overflows a limit of 4.
        val words = listOf(TimedWord("ab", 0.0, 1.0), TimedWord("😀", 1.0, 2.0))
        assertEquals(
            listOf(SubtitleCue(0.0, 1.0, "ab"), SubtitleCue(1.0, 2.0, "😀")),
            cuesFrom(words, maxLineLength = 4, maxLines = 1),
        )
    }

    @Test
    fun `a candidate exactly at maxLineLength stays on one line`() {
        // "aaaa bbb" is exactly 8, so it must not wrap — the check is strictly greater than.
        val words = listOf(TimedWord("aaaa", 0.0, 1.0), TimedWord("bbb", 1.0, 2.0))
        assertEquals(
            listOf(SubtitleCue(0.0, 2.0, "aaaa bbb")),
            cuesFrom(words, maxLineLength = 8, maxLines = 1),
        )
    }

    @Test
    fun `the default limits are 37 characters on one line`() {
        val options = TranscriptionOptions()
        assertEquals(37, options.maxLineLength)
        assertEquals(1, options.maxLines)
        assertEquals(null, options.language)
    }
}
