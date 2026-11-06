package ly.img.editor.plugin.voiceIsolation.deepFilterNet3

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive

/**
 * Enhances each channel of [input] with its own [DeepFilterNet3.Stream] from [newStream] and writes the result to
 * [output], one block at a time.
 */
internal suspend fun enhanceWav(
    input: Pcm16WavReader,
    output: Pcm16WavWriter,
    newStream: () -> DeepFilterNet3.Stream,
) {
    // Each stream gets the same number of samples, so each returns the same number of samples.
    val streams = List(input.channels) { newStream() }
    var start = 0L
    while (start < input.frames) {
        val count = minOf(BLOCK_FRAMES.toLong(), input.frames - start).toInt()
        val block = input.read(start, count)
        output.write(
            Array(streams.size) { channel ->
                currentCoroutineContext().ensureActive()
                streams[channel].process(block[channel])
            },
        )
        start += count
    }
    output.write(
        Array(streams.size) { channel ->
            currentCoroutineContext().ensureActive()
            streams[channel].finish()
        },
    )
}

// One second at 48 kHz.
private const val BLOCK_FRAMES = 48_000
