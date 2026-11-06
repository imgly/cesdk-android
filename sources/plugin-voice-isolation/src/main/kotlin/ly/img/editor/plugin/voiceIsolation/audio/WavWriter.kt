package ly.img.editor.plugin.voiceIsolation.audio

import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Writes a WAV file with 16-bit PCM samples. The header gets its sizes on [close].
 */
internal class WavWriter(
    private val file: File,
    private val sampleRate: Int,
    private val channels: Int,
) : Closeable {
    private val output = BufferedOutputStream(file.outputStream(), BUFFER_SIZE)
    private var dataBytes = 0L
    private val bytes = ByteBuffer.allocate(BUFFER_SIZE).order(ByteOrder.LITTLE_ENDIAN)

    init {
        output.write(header(dataBytes = 0))
    }

    /**
     * Writes the first [count] interleaved samples of [samples]. A sample outside [-1, 1] is clipped.
     */
    fun write(
        samples: FloatArray,
        count: Int = samples.size,
    ) {
        for (sample in 0 until count) {
            if (!bytes.hasRemaining()) flush()
            val value = (samples[sample] * PCM_SCALE).roundToInt().coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt())
            bytes.putShort(value.toShort())
        }
    }

    override fun close() {
        flush()
        output.close()
        RandomAccessFile(file, "rw").use { it.write(header(dataBytes)) }
    }

    private fun flush() {
        output.write(bytes.array(), 0, bytes.position())
        dataBytes += bytes.position()
        bytes.clear()
    }

    private fun header(dataBytes: Long): ByteArray = ByteBuffer.allocate(HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN).apply {
        put("RIFF".toByteArray(Charsets.US_ASCII))
        putInt((HEADER_SIZE - 8 + dataBytes).toInt())
        put("WAVE".toByteArray(Charsets.US_ASCII))
        put("fmt ".toByteArray(Charsets.US_ASCII))
        putInt(16)
        putShort(1)
        putShort(channels.toShort())
        putInt(sampleRate)
        putInt(sampleRate * channels * BYTES_PER_SAMPLE)
        putShort((channels * BYTES_PER_SAMPLE).toShort())
        putShort((BYTES_PER_SAMPLE * 8).toShort())
        put("data".toByteArray(Charsets.US_ASCII))
        putInt(dataBytes.toInt())
    }.array()

    private companion object {
        const val HEADER_SIZE = 44
        const val BYTES_PER_SAMPLE = 2
        const val BUFFER_SIZE = 64 * 1024
        const val PCM_SCALE = 32768f
    }
}
