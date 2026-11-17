package ly.img.editor.plugin.voiceIsolation.audio

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Converts one channel of audio from [inRate] to [outRate] in blocks, with a Kaiser-windowed sinc low-pass filter.
 * Output sample n sits at input time n * inRate / outRate, so the conversion holds samples back but adds no delay.
 */
internal class Resampler(
    private val inRate: Int,
    private val outRate: Int,
) {
    // The cut-off lies below the lower of the two Nyquist frequencies, in cycles per input sample.
    private val cutoff = 0.5 * min(1.0, outRate.toDouble() / inRate) * ROLLOFF
    private val halfWidth = ceil(ZERO_CROSSINGS / (2 * cutoff)).toInt()
    private val kernel = FloatArray(halfWidth * TABLE_RESOLUTION + 2) {
        val t = it.toDouble() / TABLE_RESOLUTION
        if (t > halfWidth) 0f else (2 * cutoff * sinc(2 * cutoff * t) * kaiser(t / halfWidth)).toFloat()
    }

    // Input samples from the absolute index historyStart on.
    private var history = FloatArray(4096)
    private var historySize = 0
    private var historyStart = 0L
    private var inputCount = 0L
    private var outputCount = 0L

    /**
     * Returns the output that [input] completes. The rest comes from the next call or from [flush].
     */
    fun process(input: FloatArray): FloatArray {
        if (inRate == outRate) return input
        append(input)
        inputCount += input.size
        return produce(available = historyStart + historySize)
    }

    /**
     * Returns the output that is still held back, so that the whole output lasts as long as the whole input.
     */
    fun flush(): FloatArray {
        if (inRate == outRate) return FloatArray(0)
        val expected = inputCount * outRate / inRate
        append(FloatArray(halfWidth + 1))
        val tail = produce(available = historyStart + historySize)
        return tail.copyOf(maxOf(0L, minOf(tail.size.toLong(), expected - (outputCount - tail.size))).toInt())
    }

    // Output n needs the input samples within halfWidth of its time.
    private fun produce(available: Long): FloatArray {
        var result = FloatArray(maxOf(0L, ((available - halfWidth) * outRate / inRate) - outputCount + 1).toInt())
        var produced = 0
        while (produced < result.size) {
            val position = outputCount * inRate
            val base = position / outRate
            if (base + halfWidth >= available) break
            val fraction = (position % outRate).toDouble() / outRate
            var sum = 0f
            for (k in base - halfWidth + 1..base + halfWidth) {
                if (k < 0) continue
                val distance = abs(base + fraction - k) * TABLE_RESOLUTION
                val index = distance.toInt()
                val weight = kernel[index] + (kernel[index + 1] - kernel[index]) * (distance - index).toFloat()
                sum += history[(k - historyStart).toInt()] * weight
            }
            result[produced++] = sum
            outputCount++
        }
        if (produced < result.size) result = result.copyOf(produced)
        dropUnneeded()
        return result
    }

    private fun dropUnneeded() {
        val keepFrom = maxOf(historyStart, outputCount * inRate / outRate - halfWidth)
        val dropped = (keepFrom - historyStart).toInt()
        System.arraycopy(history, dropped, history, 0, historySize - dropped)
        historySize -= dropped
        historyStart = keepFrom
    }

    private fun append(input: FloatArray) {
        if (historySize + input.size > history.size) history = history.copyOf(maxOf(history.size * 2, historySize + input.size))
        System.arraycopy(input, 0, history, historySize, input.size)
        historySize += input.size
    }

    private fun sinc(x: Double) = if (x == 0.0) 1.0 else sin(PI * x) / (PI * x)

    private fun kaiser(x: Double) = besselI0(KAISER_BETA * sqrt(maxOf(0.0, 1 - x * x))) / besselI0(KAISER_BETA)

    private fun besselI0(x: Double): Double {
        var sum = 1.0
        var term = 1.0
        var k = 1
        while (term > 1e-12 * sum) {
            term *= (x / (2 * k)) * (x / (2 * k))
            sum += term
            k++
        }
        return sum
    }

    private companion object {
        const val ROLLOFF = 0.95

        // 16 zero crossings on each side give a transition band of about 2.6 kHz at 48 kHz, which lies above speech.
        const val ZERO_CROSSINGS = 16
        const val TABLE_RESOLUTION = 512
        const val KAISER_BETA = 8.6
    }
}
