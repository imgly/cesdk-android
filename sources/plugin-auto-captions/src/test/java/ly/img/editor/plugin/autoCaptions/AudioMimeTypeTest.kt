package ly.img.editor.plugin.autoCaptions

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The engine answers `audio/mp3` for an MP3, which the IMG.LY Gateway rejects — it only accepts the registered
 * `audio/mpeg`. Every alias the engine can report has to be canonicalized before it reaches a provider.
 */
class AudioMimeTypeTest {
    private fun canonical(mimeType: String?) = with(AutoCaptionsGenerator) { mimeType.canonicalAudioMimeType() }

    @Test
    fun `mp3 aliases become the registered mpeg type`() {
        assertEquals("audio/mpeg", canonical("audio/mp3"))
        assertEquals("audio/mpeg", canonical("audio/x-mp3"))
        assertEquals("audio/mpeg", canonical("audio/mpeg3"))
        assertEquals("audio/mpeg", canonical("audio/x-mpeg3"))
    }

    @Test
    fun `m4a aliases become the mp4 container type`() {
        assertEquals("audio/mp4", canonical("audio/m4a"))
        assertEquals("audio/mp4", canonical("audio/x-m4a"))
    }

    @Test
    fun `already registered types pass through`() {
        assertEquals("audio/mpeg", canonical("audio/mpeg"))
        assertEquals("audio/mp4", canonical("audio/mp4"))
        assertEquals("audio/wav", canonical("audio/wav"))
        assertEquals("audio/aac", canonical("audio/aac"))
        assertEquals("audio/ogg", canonical("audio/ogg"))
    }

    @Test
    fun `parameters and casing are normalized away`() {
        assertEquals("audio/mpeg", canonical("Audio/MP3"))
        assertEquals("audio/mpeg", canonical("audio/mpeg; charset=binary"))
        assertEquals("audio/wav", canonical("  audio/wav  "))
    }

    @Test
    fun `an unknown or absent type falls back to mpeg`() {
        assertEquals("audio/mpeg", canonical(null))
        assertEquals("audio/mpeg", canonical(""))
        // Anything else is passed on as-is; the service is the authority on what it accepts.
        assertEquals("audio/flac", canonical("audio/flac"))
    }
}
