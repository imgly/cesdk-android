package ly.img.editor.plugin.autoCaptions

import ly.img.editor.plugin.autoCaptions.AutoCaptionsGenerator.AudioReader
import org.junit.Assert.assertEquals
import org.junit.Test
import java.net.URI

/**
 * Reading every source as an engine buffer used to fail for committed voiceovers (`file://`) and library audio
 * (`https://`), which surfaced to the user as "no speech was detected".
 *
 * Uses [URI] rather than `android.net.Uri`, whose scheme parsing is stubbed out in unit tests.
 */
class AudioReaderTest {
    private fun reader(uri: String) = AudioReader.of(URI(uri).scheme)

    @Test
    fun `engine buffers are read from the buffer`() {
        assertEquals(AudioReader.Buffer, reader("buffer://ubq/12345"))
    }

    @Test
    fun `committed voiceover files are read as resources`() {
        assertEquals(AudioReader.Resource, reader("file:///tmp/voiceover.m4a"))
    }

    @Test
    fun `bundled and relative sources are read as resources`() {
        assertEquals(AudioReader.Resource, reader("bundle://audio/beat.mp3"))
        assertEquals(AudioReader.Resource, reader("audio/beat.mp3"))
    }

    @Test
    fun `library audio is downloaded`() {
        assertEquals(AudioReader.Remote, reader("https://cdn.img.ly/audio/beat.mp3"))
        assertEquals(AudioReader.Remote, reader("http://example.com/beat.mp3"))
    }

    @Test
    fun `scheme matching is case insensitive`() {
        assertEquals(AudioReader.Remote, reader("HTTPS://cdn.img.ly/audio/beat.mp3"))
        assertEquals(AudioReader.Buffer, reader("BUFFER://ubq/12345"))
    }
}
