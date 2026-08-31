package ly.img.editor.plugin.autoCaptions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** Covers the lenient SRT parser and the serializer the generated file is written with. */
class SrtTest {
    // region Parsing

    @Test
    fun `parses numbered cues`() {
        val srt = """
            1
            00:00:01,000 --> 00:00:02,500
            Hello

            2
            00:00:03,000 --> 00:00:04,000
            World
        """.trimIndent()
        assertEquals(
            listOf(SubtitleCue(1.0, 2.5, "Hello"), SubtitleCue(3.0, 4.0, "World")),
            Srt.parse(srt),
        )
    }

    @Test
    fun `parses cues without an index line`() {
        val srt = "00:00:01,000 --> 00:00:02,000\nHello"
        assertEquals(listOf(SubtitleCue(1.0, 2.0, "Hello")), Srt.parse(srt))
    }

    @Test
    fun `parses multiline cue text`() {
        val srt = "1\n00:00:01,000 --> 00:00:02,000\nfirst\nsecond"
        assertEquals(listOf(SubtitleCue(1.0, 2.0, "first\nsecond")), Srt.parse(srt))
    }

    @Test
    fun `parses carriage return line endings`() {
        val srt = "1\r\n00:00:01,000 --> 00:00:02,000\r\nHello"
        assertEquals(listOf(SubtitleCue(1.0, 2.0, "Hello")), Srt.parse(srt))
    }

    @Test
    fun `parses a dot millisecond separator`() {
        val srt = "1\n00:00:01.250 --> 00:00:02.750\nHello"
        assertEquals(listOf(SubtitleCue(1.25, 2.75, "Hello")), Srt.parse(srt))
    }

    @Test
    fun `parses timestamps without an hours component`() {
        val srt = "1\n01:30,000 --> 02:00,000\nHello"
        assertEquals(listOf(SubtitleCue(90.0, 120.0, "Hello")), Srt.parse(srt))
    }

    @Test
    fun `parses hours past a day`() {
        val srt = "1\n25:00:00,000 --> 25:00:01,000\nHello"
        assertEquals(listOf(SubtitleCue(90000.0, 90001.0, "Hello")), Srt.parse(srt))
    }

    @Test
    fun `skips malformed blocks and keeps the rest`() {
        val srt = """
            1
            not a timing line
            Skipped

            2
            00:00:03,000 --> 00:00:04,000
            Kept
        """.trimIndent()
        assertEquals(listOf(SubtitleCue(3.0, 4.0, "Kept")), Srt.parse(srt))
    }

    @Test
    fun `skips cues without text`() {
        val srt = "1\n00:00:01,000 --> 00:00:02,000\n\n2\n00:00:03,000 --> 00:00:04,000\nKept"
        assertEquals(listOf(SubtitleCue(3.0, 4.0, "Kept")), Srt.parse(srt))
    }

    @Test
    fun `parses an empty string to no cues`() {
        assertEquals(emptyList<SubtitleCue>(), Srt.parse(""))
    }

    // endregion
    // region Serializing

    @Test
    fun `serializes and renumbers cues`() {
        val cues = listOf(SubtitleCue(1.0, 2.0, "a"), SubtitleCue(3.0, 4.0, "b"))
        assertEquals(
            "1\n00:00:01,000 --> 00:00:02,000\na\n\n2\n00:00:03,000 --> 00:00:04,000\nb",
            Srt.serialize(cues),
        )
    }

    @Test
    fun `round trips cues`() {
        val cues = listOf(SubtitleCue(1.5, 2.25, "a\nb"), SubtitleCue(3.0, 4.125, "c"))
        assertEquals(cues, Srt.parse(Srt.serialize(cues)))
    }

    @Test
    fun `formats timestamps`() {
        assertEquals("00:00:00,000", Srt.timestamp(0.0))
        assertEquals("01:02:03,456", Srt.timestamp(3723.456))
    }

    @Test
    fun `rounds milliseconds when formatting`() {
        assertEquals("00:00:01,235", Srt.timestamp(1.23456))
    }

    // endregion

    @Test
    fun `rejects malformed timestamps`() {
        assertNull(Srt.seconds(""))
        assertNull(Srt.seconds("12"))
        assertNull(Srt.seconds("00:00:00:00,000"))
        assertNull(Srt.seconds("aa:00,000"))
        assertNull(Srt.seconds("00:bb,000"))
        assertNull(Srt.seconds("-1:00,000"))
        assertNull(Srt.seconds("00:-1,000"))
    }
}
