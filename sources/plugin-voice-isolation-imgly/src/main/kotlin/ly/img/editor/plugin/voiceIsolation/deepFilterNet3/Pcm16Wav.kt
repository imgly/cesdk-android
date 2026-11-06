package ly.img.editor.plugin.voiceIsolation.deepFilterNet3

import java.io.BufferedOutputStream
import java.io.Closeable
import java.io.File
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Reads ranges of frames from a WAV file with 16-bit PCM samples.
 */
internal class Pcm16WavReader(
    file: File,
) : Closeable {
    private val input = RandomAccessFile(file, "r")
    private val dataOffset: Long

    /** The sample rate in Hz. */
    val sampleRate: Int

    /** The number of channels. */
    val channels: Int

    /** The number of frames, one sample per channel each. */
    val frames: Long

    init {
        val header = ByteBuffer.allocate(RIFF_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
        input.readFully(header.array())
        require(header.tag(0) == "RIFF" && header.tag(8) == "WAVE") { "$file is not a WAV file." }
        var format: ByteBuffer? = null
        var dataSize = -1L
        var data = 0L
        var offset = RIFF_HEADER_SIZE.toLong()
        // A WAV file is a list of chunks; only "fmt " and "data" matter here.
        while (dataSize < 0 && offset + CHUNK_HEADER_SIZE <= input.length()) {
            val chunkHeader = ByteBuffer.allocate(CHUNK_HEADER_SIZE).order(ByteOrder.LITTLE_ENDIAN)
            input.seek(offset)
            input.readFully(chunkHeader.array())
            val size = chunkHeader.getInt(4).toLong() and 0xFFFFFFFFL
            when (chunkHeader.tag(0)) {
                "fmt " -> format = ByteBuffer.allocate(size.toInt()).order(ByteOrder.LITTLE_ENDIAN).also { input.readFully(it.array()) }
                "data" -> {
                    data = offset + CHUNK_HEADER_SIZE
                    dataSize = minOf(size, input.length() - data)
                }
            }
            offset += CHUNK_HEADER_SIZE + size + size % 2
        }
        val fmt = requireNotNull(format) { "$file has no format chunk." }
        require(dataSize >= 0) { "$file has no data chunk." }
        require(fmt.getShort(0).toInt() == PCM_FORMAT && fmt.getShort(14).toInt() == BITS_PER_SAMPLE) {
            "$file does not hold 16-bit PCM samples."
        }
        channels = fmt.getShort(2).toInt()
        sampleRate = fmt.getInt(4)
        require(channels > 0) { "$file has no channels." }
        dataOffset = data
        frames = dataSize / (channels * BYTES_PER_SAMPLE)
    }

    /**
     * Reads [count] frames from frame [start] on, as one array of samples in [-1, 1] per channel.
     */
    fun read(
        start: Long,
        count: Int,
    ): Array<FloatArray> {
        val bytes = ByteBuffer.allocate(count * channels * BYTES_PER_SAMPLE).order(ByteOrder.LITTLE_ENDIAN)
        input.seek(dataOffset + start * channels * BYTES_PER_SAMPLE)
        input.readFully(bytes.array())
        val samples = bytes.asShortBuffer()
        return Array(channels) { channel -> FloatArray(count) { samples.get(it * channels + channel) / PCM_SCALE } }
    }

    override fun close() = input.close()

    private fun ByteBuffer.tag(index: Int) = String(array(), index, 4, Charsets.US_ASCII)
}

/**
 * Writes a WAV file with 16-bit PCM samples. The header gets its sizes on [close]. `plugin-voice-isolation` writes the
 * isolator's input with the same format, but the two modules share no internal code.
 */
internal class Pcm16WavWriter(
    private val file: File,
    private val sampleRate: Int,
    private val channels: Int,
) : Closeable {
    private val output = BufferedOutputStream(file.outputStream(), BUFFER_SIZE)
    private var dataBytes = 0L

    init {
        output.write(header(dataBytes = 0))
    }

    /**
     * Writes [channelSamples], one array of samples per channel, interleaved. A sample outside [-1, 1] is clipped.
     */
    fun write(channelSamples: Array<FloatArray>) {
        val frames = channelSamples.first().size
        val bytes = ByteBuffer.allocate(frames * channels * BYTES_PER_SAMPLE).order(ByteOrder.LITTLE_ENDIAN)
        for (frame in 0 until frames) {
            for (channel in 0 until channels) {
                val value = (channelSamples[channel][frame] * PCM_SCALE).roundToInt()
                bytes.putShort(value.coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort())
            }
        }
        output.write(bytes.array())
        dataBytes += bytes.capacity()
    }

    override fun close() {
        output.close()
        RandomAccessFile(file, "rw").use { it.write(header(dataBytes)) }
    }

    private fun header(dataBytes: Long): ByteArray = ByteBuffer.allocate(WAV_HEADER_SIZE)
        .order(ByteOrder.LITTLE_ENDIAN)
        .apply {
            put("RIFF".toByteArray(Charsets.US_ASCII))
            putInt((WAV_HEADER_SIZE - CHUNK_HEADER_SIZE + dataBytes).toInt())
            put("WAVE".toByteArray(Charsets.US_ASCII))
            put("fmt ".toByteArray(Charsets.US_ASCII))
            putInt(16)
            putShort(PCM_FORMAT.toShort())
            putShort(channels.toShort())
            putInt(sampleRate)
            putInt(sampleRate * channels * BYTES_PER_SAMPLE)
            putShort((channels * BYTES_PER_SAMPLE).toShort())
            putShort(BITS_PER_SAMPLE.toShort())
            put("data".toByteArray(Charsets.US_ASCII))
            putInt(dataBytes.toInt())
        }.array()
}

private const val RIFF_HEADER_SIZE = 12
private const val WAV_HEADER_SIZE = 44
private const val CHUNK_HEADER_SIZE = 8
private const val PCM_FORMAT = 1
private const val BITS_PER_SAMPLE = 16
private const val BYTES_PER_SAMPLE = 2
private const val BUFFER_SIZE = 64 * 1024
private const val PCM_SCALE = 32768f
