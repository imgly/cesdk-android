package ly.img.editor.plugin.voiceIsolation.audio

import android.content.Context
import android.media.AudioFormat
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import ly.img.editor.plugin.voiceIsolation.VoiceIsolationException
import java.io.Closeable
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The sample rate of the files that a [ly.img.editor.plugin.voiceIsolation.VoiceIsolator] reads and writes.
 */
internal const val ISOLATOR_SAMPLE_RATE = 48_000

/**
 * Decodes the audio track number [audioTrack] of [source], counted among its audio tracks only, into [output]: a WAV
 * file with 16-bit PCM samples at [ISOLATOR_SAMPLE_RATE] and the channels of the track.
 *
 * @param source a `file` or `content` URI.
 * @throws VoiceIsolationException.NoAudio when [source] has no such track, or the track has no samples.
 */
internal suspend fun decodeAudioTrack(
    context: Context,
    source: Uri,
    audioTrack: Int,
    output: File,
) = withContext(Dispatchers.IO) {
    val extractor = MediaExtractor()
    try {
        extractor.setDataSource(context, source, null)
        val track = (0 until extractor.trackCount)
            .filter { extractor.getTrackFormat(it).getString(MediaFormat.KEY_MIME).orEmpty().startsWith("audio/") }
            .getOrNull(audioTrack)
            ?: throw VoiceIsolationException.NoAudio()
        extractor.selectTrack(track)
        val format = extractor.getTrackFormat(track)
        val decoder = MediaCodec.createDecoderByType(requireNotNull(format.getString(MediaFormat.KEY_MIME)))
        try {
            decoder.configure(format, null, null, 0)
            decoder.start()
            val sink = PcmSink(output)
            sink.use {
                val info = MediaCodec.BufferInfo()
                var inputDone = false
                var outputDone = false
                while (!outputDone) {
                    ensureActive()
                    if (!inputDone) {
                        val index = decoder.dequeueInputBuffer(TIMEOUT_US)
                        if (index >= 0) {
                            val size = extractor.readSampleData(requireNotNull(decoder.getInputBuffer(index)), 0)
                            if (size < 0) {
                                decoder.queueInputBuffer(index, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                                inputDone = true
                            } else {
                                decoder.queueInputBuffer(index, 0, size, extractor.sampleTime, 0)
                                extractor.advance()
                            }
                        }
                    }
                    val index = decoder.dequeueOutputBuffer(info, TIMEOUT_US)
                    if (index >= 0) {
                        if (info.size > 0) {
                            val buffer = requireNotNull(decoder.getOutputBuffer(index))
                            buffer.position(info.offset)
                            buffer.limit(info.offset + info.size)
                            sink.write(buffer, decoder.getOutputFormat(index), info.presentationTimeUs)
                        }
                        decoder.releaseOutputBuffer(index, false)
                        outputDone = info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0
                    }
                }
            }
            if (sink.frames == 0L) throw VoiceIsolationException.NoAudio()
        } finally {
            decoder.release()
        }
    } finally {
        extractor.release()
    }
}

/**
 * Converts decoded PCM to [ISOLATOR_SAMPLE_RATE] and writes it to a WAV file, which it creates on the first buffer,
 * because a decoder reports its output format only with its output.
 */
private class PcmSink(
    private val output: File,
) : Closeable {
    private var writer: WavWriter? = null
    private var resamplers = emptyList<Resampler>()
    private var channels = 0
    private var framesToSkip = 0L

    /** The number of frames written so far. */
    var frames = 0L
        private set

    fun write(
        buffer: ByteBuffer,
        format: MediaFormat,
        presentationTimeUs: Long,
    ) {
        if (writer == null) start(format, presentationTimeUs)
        val samples = toFloats(buffer, format)
        val skipped = minOf(framesToSkip, (samples.size / channels).toLong()).toInt()
        framesToSkip -= skipped
        writeFrames(samples, from = skipped * channels)
    }

    override fun close() {
        val writer = writer ?: return
        val tails = resamplers.map { it.flush() }
        writer.write(interleave(tails))
        frames += tails.first().size
        writer.close()
    }

    private fun start(
        format: MediaFormat,
        presentationTimeUs: Long,
    ) {
        channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        writer = WavWriter(output, ISOLATOR_SAMPLE_RATE, channels)
        resamplers = List(channels) { Resampler(sampleRate, ISOLATOR_SAMPLE_RATE) }
        // A track can start after or, with an edit list, before zero, so the cleaned audio would lose its sync with
        // the clip without the silence or the skip.
        val offsetFrames = presentationTimeUs * sampleRate / 1_000_000
        if (offsetFrames > 0) {
            writeFrames(FloatArray((offsetFrames * channels).toInt()), from = 0)
        } else {
            framesToSkip = -offsetFrames
        }
    }

    private fun writeFrames(
        samples: FloatArray,
        from: Int,
    ) {
        val count = (samples.size - from) / channels
        val resampled = List(channels) { channel ->
            resamplers[channel].process(FloatArray(count) { samples[from + it * channels + channel] })
        }
        requireNotNull(writer).write(interleave(resampled))
        frames += resampled.first().size
    }

    private fun interleave(channelSamples: List<FloatArray>): FloatArray {
        val count = channelSamples.minOf { it.size }
        val samples = FloatArray(count * channels)
        for (frame in 0 until count) {
            for (channel in 0 until channels) samples[frame * channels + channel] = channelSamples[channel][frame]
        }
        return samples
    }

    private fun toFloats(
        buffer: ByteBuffer,
        format: MediaFormat,
    ): FloatArray {
        val encoding = if (format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
            format.getInteger(MediaFormat.KEY_PCM_ENCODING)
        } else {
            AudioFormat.ENCODING_PCM_16BIT
        }
        val ordered = buffer.order(ByteOrder.nativeOrder())
        return when (encoding) {
            AudioFormat.ENCODING_PCM_FLOAT -> ordered.asFloatBuffer().let { floats ->
                FloatArray(floats.remaining()).also { floats.get(it) }
            }
            AudioFormat.ENCODING_PCM_16BIT -> ordered.asShortBuffer().let { shorts ->
                FloatArray(shorts.remaining()) {
                    shorts.get(it) /
                        PCM_SCALE
                }
            }
            else -> throw IllegalStateException("The decoder returned PCM encoding $encoding.")
        }
    }

    private companion object {
        const val PCM_SCALE = 32768f
    }
}

private const val TIMEOUT_US = 10_000L
