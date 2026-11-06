package ly.img.editor.plugin.voiceIsolation.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.sound.sampled.AudioFormat
import javax.sound.sampled.AudioSystem

class WavWriterTest {
    @Test
    fun writesA16BitPcmFileThatOtherReadersOpen() {
        val file = File.createTempFile("writer", ".wav")
        try {
            // Interleaved stereo; the last frame is out of range and gets clipped.
            val samples = floatArrayOf(0f, 0.5f, -0.5f, 1f / 32768, 1.5f, -1.5f)
            // Two writes, and more samples than the buffer holds, so the sizes in the header add up.
            WavWriter(file, sampleRate = 48_000, channels = 2).use { writer ->
                writer.write(samples)
                writer.write(FloatArray(40_000))
            }
            AudioSystem.getAudioInputStream(file).use { stream ->
                val format = stream.format
                assertEquals(AudioFormat.Encoding.PCM_SIGNED, format.encoding)
                assertEquals(48_000f, format.sampleRate)
                assertEquals(2, format.channels)
                assertEquals(16, format.sampleSizeInBits)
                assertEquals(false, format.isBigEndian)
                assertEquals((samples.size + 40_000) / 2L, stream.frameLength)
                val bytes = stream.readBytes()
                val shorts = ShortArray(samples.size).also { ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(it) }
                assertArrayEquals(shortArrayOf(0, 16384, -16384, 1, Short.MAX_VALUE, Short.MIN_VALUE), shorts)
            }
        } finally {
            file.delete()
        }
    }
}
