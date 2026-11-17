package ly.img.editor.plugin.voiceIsolation.deepFilterNet3

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.log10

/**
 * Compares the DeepFilterNet3 port, in the chunks the plugin uses, with the upstream Python package on a clip that
 * crosses the first chunk boundary at 10 s.
 */
class DeepFilterNet3ReferenceTest {
    private val modelFolder = File(
        checkNotNull(System.getProperty("voiceIsolation.modelDir")) {
            "Run this test through Gradle, whose unpackDeepFilterNet3 task downloads the model."
        },
    )

    // @requirement(voice-isolation/isolate-voice/speech-kept-music-removed)
    @Test
    fun matchesThePythonReference() = runBlocking {
        val input = resource("speech-and-music.wav")
        val output = File.createTempFile("enhanced", ".wav")
        try {
            DeepFilterNet3(modelFolder).use { model ->
                Pcm16WavReader(input).use { reader ->
                    Pcm16WavWriter(output, reader.sampleRate, reader.channels).use { writer ->
                        enhanceWav(input = reader, output = writer, newStream = { model.Stream() })
                    }
                }
            }
            val port = readMono(output)
            val reference = readMono(resource("speech-and-music-reference.wav"))
            assertEquals(reference.size, port.size)
            val skip = SAMPLE_RATE / 2
            val boundary = 10 * SAMPLE_RATE
            val first = siSdr(port.copyOfRange(skip, boundary), reference.copyOfRange(skip, boundary))
            val second = siSdr(port.copyOfRange(boundary, port.size), reference.copyOfRange(boundary, reference.size))
            assertTrue("SI-SDR before the chunk boundary is $first dB.", first >= MIN_SI_SDR_DB)
            assertTrue("SI-SDR after the chunk boundary is $second dB.", second >= MIN_SI_SDR_AFTER_BOUNDARY_DB)
        } finally {
            output.delete()
        }
    }

    // @requirement(voice-isolation/isolate-voice/speech-after-silence)
    @Test
    fun staysFiniteAndMatchesTheReferenceAfterLongDigitalSilence() {
        val clip = readMono(resource("speech-and-music.wav"))
        val reference = readMono(resource("speech-and-music-reference.wav"))
        // Every hop of 100 s of exact zeros is processed, so the unit norm decays to the smallest float.
        val silence = 100 * SAMPLE_RATE
        val port = DeepFilterNet3(modelFolder).use { model ->
            model.Stream().run { process(FloatArray(silence) + clip) + finish() }
        }
        assertTrue("An output sample is not finite.", port.all(Float::isFinite))
        // The GRUs start from the state of the silence, as at a chunk boundary, so the output differs from the
        // reference for seconds. It must still be far closer to the reference than the clip with its music is.
        val skip = SAMPLE_RATE / 2
        val target = reference.copyOfRange(skip, reference.size)
        val sdr = siSdr(port.copyOfRange(silence + skip, port.size), target)
        val unprocessed = siSdr(clip.copyOfRange(skip, clip.size), target)
        assertTrue("SI-SDR after the silence is $sdr dB, the clip has $unprocessed dB.", sdr >= unprocessed + MIN_IMPROVEMENT_DB)
    }

    private fun resource(name: String) = File(checkNotNull(javaClass.classLoader?.getResource("reference/$name")).toURI())

    private fun readMono(file: File) = Pcm16WavReader(file).use { it.read(0, it.frames.toInt()).single() }

    /** Scale-invariant signal-to-distortion ratio of [estimate] against [target], with the mean of both removed. */
    private fun siSdr(
        estimate: FloatArray,
        target: FloatArray,
    ): Double {
        val estimateMean = estimate.average()
        val targetMean = target.average()
        var dot = 0.0
        var targetEnergy = 0.0
        for (i in target.indices) {
            dot += (estimate[i] - estimateMean) * (target[i] - targetMean)
            targetEnergy += (target[i] - targetMean) * (target[i] - targetMean)
        }
        val scale = dot / targetEnergy
        var signal = 0.0
        var noise = 0.0
        for (i in target.indices) {
            val projected = scale * (target[i] - targetMean)
            signal += projected * projected
            noise += (estimate[i] - estimateMean - projected) * (estimate[i] - estimateMean - projected)
        }
        return 10 * log10(signal / noise)
    }

    private companion object {
        const val SAMPLE_RATE = 48_000

        // The Python package runs the PyTorch checkpoint, which the ONNX export matches only to about 40 dB: the
        // deep-filter 0.5.6 binary, which runs the same ONNX graphs as this port, scores 42.8 dB against it.
        const val MIN_SI_SDR_DB = 35.0

        // The GRUs of a chunk start from zero, and its warm-up lets them settle only in part.
        const val MIN_SI_SDR_AFTER_BOUNDARY_DB = 30.0

        // A model that no longer works scores as the unprocessed clip does.
        const val MIN_IMPROVEMENT_DB = 10.0
    }
}
