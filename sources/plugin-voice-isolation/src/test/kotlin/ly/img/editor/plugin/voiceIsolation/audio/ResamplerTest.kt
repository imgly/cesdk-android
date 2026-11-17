package ly.img.editor.plugin.voiceIsolation.audio

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

class ResamplerTest {
    @Test
    fun keepsATone() {
        for (inRate in listOf(44_100, 22_050, 96_000)) {
            val output = resample(tone(inRate, seconds = 1.0), inRate, blockSize = 1234)
            assertEquals("Length from $inRate Hz.", OUT_RATE, output.size)
            // The middle of the output, where the filter has input on both sides, against the ideal tone.
            val expected = tone(OUT_RATE, seconds = 1.0)
            val middle = 1000 until OUT_RATE - 1000
            val error = rms(FloatArray(middle.count()) { output[middle.first + it] - expected[middle.first + it] })
            val snr = 20 * log10(rms(expected) / error)
            assertTrue("SNR from $inRate Hz is $snr dB.", snr > MIN_TONE_SNR_DB)
        }
    }

    @Test
    fun removesFrequenciesAboveTheOutputNyquist() {
        val inRate = 96_000
        val output = resample(tone(inRate, seconds = 1.0, frequency = 30_000.0), inRate, blockSize = 4096)
        val level = 20 * log10(rms(output.copyOfRange(1000, output.size - 1000)) / rms(tone(inRate, seconds = 1.0, frequency = 30_000.0)))
        assertTrue("A 30 kHz tone comes out at $level dB.", level < MAX_ALIAS_DB)
    }

    @Test
    fun keepsTheSamplesAtTheSameRate() {
        val input = tone(OUT_RATE, seconds = 0.1)
        assertArrayEquals(input, resample(input, OUT_RATE, blockSize = 100), 0f)
    }

    private fun resample(
        input: FloatArray,
        inRate: Int,
        blockSize: Int,
    ): FloatArray {
        val resampler = Resampler(inRate, OUT_RATE)
        var output = FloatArray(0)
        for (start in input.indices step blockSize) {
            output += resampler.process(input.copyOfRange(start, minOf(input.size, start + blockSize)))
        }
        return output + resampler.flush()
    }

    private fun tone(
        rate: Int,
        seconds: Double,
        frequency: Double = 1000.0,
    ) = FloatArray((rate * seconds).toInt()) { (0.5 * sin(2 * PI * frequency * it / rate)).toFloat() }

    private fun rms(samples: FloatArray) = sqrt(samples.sumOf { it.toDouble() * it } / samples.size)

    private companion object {
        const val OUT_RATE = 48_000
        const val MIN_TONE_SNR_DB = 60.0
        const val MAX_ALIAS_DB = -60.0
    }
}
