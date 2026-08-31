package ly.img.editor.plugin.autoCaptions

import ly.img.editor.plugin.autoCaptions.gateway.GatewayTranscriptionProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Pins the gateway `generation.completed` response shape `parseWords` depends on: a key drift in
 * `output[0].data.words` would otherwise silently yield no captions.
 */
class GatewayTranscriptionProviderTest {
    @Test
    fun `parses words from a completed payload`() {
        val json = """
            {
              "request_id": "gw_1",
              "output": [
                {
                  "type": "transcript",
                  "data": {
                    "text": "Hello there",
                    "words": [
                      { "text": "Hello", "start": 0.1, "end": 0.4, "speaker": "speaker_0" },
                      { "text": "there", "start": 0.5, "end": 0.9, "speaker": "speaker_0" }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()
        val words = GatewayTranscriptionProvider.parseWords(json)
        assertEquals(2, words.size)
        assertEquals(TimedWord("Hello", 0.1, 0.4), words.first())
        assertEquals(TimedWord("there", 0.5, 0.9), words.last())
    }

    @Test
    fun `an empty output yields no words`() {
        assertTrue(GatewayTranscriptionProvider.parseWords("""{"output":[]}""").isEmpty())
    }

    @Test
    fun `empty words yield no words`() {
        val json = """{"output":[{"type":"transcript","data":{"text":"","words":[]}}]}"""
        assertTrue(GatewayTranscriptionProvider.parseWords(json).isEmpty())
    }

    @Test
    fun `spacing and audio events are dropped`() {
        // Scribe tags a space as its own `spacing` entry, and `cuesFrom` adds a space between words itself —
        // letting them through would double the spacing in every caption.
        val json = """
            {
              "output": [
                {
                  "data": {
                    "words": [
                      { "text": "Hello", "start": 0.0, "end": 0.5, "type": "word" },
                      { "text": " ", "start": 0.5, "end": 0.5, "type": "spacing" },
                      { "text": "(laughter)", "start": 0.5, "end": 0.6, "type": "audio_event" },
                      { "text": "world", "start": 0.6, "end": 1.0, "type": "word" }
                    ]
                  }
                }
              ]
            }
        """.trimIndent()
        assertEquals(
            listOf(TimedWord("Hello", 0.0, 0.5), TimedWord("world", 0.6, 1.0)),
            GatewayTranscriptionProvider.parseWords(json),
        )
    }

    @Test
    fun `entries without a type are kept`() {
        // A gateway that normalises the shape may drop the field; dropping those entries would yield no captions.
        val json = """{"output":[{"data":{"words":[{"text":"Hello","start":0.0,"end":0.5}]}}]}"""
        assertEquals(listOf(TimedWord("Hello", 0.0, 0.5)), GatewayTranscriptionProvider.parseWords(json))
    }

    @Test
    fun `a payload without the expected keys yields no words`() {
        assertTrue(GatewayTranscriptionProvider.parseWords("""{"request_id":"gw_1"}""").isEmpty())
        assertTrue(GatewayTranscriptionProvider.parseWords("""{"output":[{"type":"transcript"}]}""").isEmpty())
    }
}
