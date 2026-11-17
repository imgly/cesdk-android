package ly.img.editor.plugin.voiceIsolation.deepFilterNet3

import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.random.Random

class DeepFilterNet3StreamTest {
    private val model = DeepFilterNet3(
        File(
            checkNotNull(System.getProperty("voiceIsolation.modelDir")) {
                "Run this test through Gradle, whose unpackDeepFilterNet3 task downloads the model."
            },
        ),
    )

    // 3 s of the reference clip, so that short chunks cross several boundaries.
    private val clip = Pcm16WavReader(File(checkNotNull(javaClass.classLoader?.getResource("reference/speech-and-music.wav")).toURI()))
        .use { it.read(0, 3 * SAMPLE_RATE).single() }

    @After
    fun close() = model.close()

    @Test
    fun givesTheSameOutputForBlocksOfAnySize() {
        val whole = model.Stream(chunkFrames = 100, warmupFrames = 20).run { process(clip) + finish() }
        val inBlocks = model.Stream(chunkFrames = 100, warmupFrames = 20).run {
            var output = FloatArray(0)
            for (start in clip.indices step 4801) output += process(clip.copyOfRange(start, minOf(clip.size, start + 4801)))
            output + finish()
        }
        assertArrayEquals(whole, inBlocks, 0f)
    }

    @Test
    fun givesAClipOfOneChunkTheOutputOfOneRun() {
        val chunked = model.Stream(chunkFrames = 400, warmupFrames = 20).run { process(clip) + finish() }
        val oneRun = model.Stream(chunkFrames = Int.MAX_VALUE, warmupFrames = 20).run { process(clip) + finish() }
        // 400 frames are 4 s, longer than the clip with the frames that push out its last samples.
        assertArrayEquals(oneRun, chunked, 0f)
        val split = model.Stream(chunkFrames = 100, warmupFrames = 20).run { process(clip) + finish() }
        assertTrue("A chunk boundary changes the output.", !split.contentEquals(oneRun))
    }

    // @requirement(voice-isolation/isolate-voice/audio-clip-cleaned-in-place)
    @Test
    fun keepsTheLengthOfEveryClipAndTheSilenceOfASilentOne() {
        val random = Random(1)
        for (length in listOf(0, 1, 479, 480, 481, 1439, 1440, 1441, 4800)) {
            val noise = model.process(FloatArray(length) { random.nextFloat() * 0.2f - 0.1f })
            assertEquals(length, noise.size)
            assertTrue("Length $length gives a sample that is not finite.", noise.all(Float::isFinite))
            assertArrayEquals(FloatArray(length), model.process(FloatArray(length)), 0f)
        }
    }

    // @requirement(voice-isolation/isolate-voice/audio-clip-cleaned-in-place)
    @Test
    fun enhancesEachChannelOfAWavFileOnItsOwn() = runBlocking {
        val right = FloatArray(clip.size) { clip[clip.size - 1 - it] / 2 }
        val input = File.createTempFile("stereo", ".wav")
        val output = File.createTempFile("enhanced", ".wav")
        try {
            Pcm16WavWriter(input, SAMPLE_RATE, 2).use { it.write(arrayOf(clip, right)) }
            Pcm16WavReader(input).use { reader ->
                Pcm16WavWriter(output, SAMPLE_RATE, 2).use { writer -> enhanceWav(reader, writer) { model.Stream() } }
            }
            val enhanced = Pcm16WavReader(output).use { it.read(0, it.frames.toInt()) }
            // The file holds 16-bit samples, so compare with the mono output after the same rounding.
            val (left, rightInput) = Pcm16WavReader(input).use { it.read(0, it.frames.toInt()) }
            assertArrayEquals(quantised(model.process(left)), enhanced[0], 0f)
            assertArrayEquals(quantised(model.process(rightInput)), enhanced[1], 0f)
        } finally {
            input.delete()
            output.delete()
        }
    }

    /** One stream over a whole clip, as each channel of [enhanceWav] gets. */
    private fun DeepFilterNet3.process(input: FloatArray) = Stream().run { process(input) + finish() }

    private fun quantised(samples: FloatArray): FloatArray {
        val file = File.createTempFile("mono", ".wav")
        try {
            Pcm16WavWriter(file, SAMPLE_RATE, 1).use { it.write(arrayOf(samples)) }
            return Pcm16WavReader(file).use { it.read(0, it.frames.toInt()).single() }
        } finally {
            file.delete()
        }
    }

    private companion object {
        const val SAMPLE_RATE = 48_000
    }
}
