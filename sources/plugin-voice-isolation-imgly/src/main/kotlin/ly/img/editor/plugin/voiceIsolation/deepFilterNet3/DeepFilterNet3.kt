// Ports libDF of DeepFilterNet (https://github.com/Rikorose/DeepFilterNet).
// Copyright (c) 2021 Hendrik Schröter
// SPDX-License-Identifier: MIT OR Apache-2.0

package ly.img.editor.plugin.voiceIsolation.deepFilterNet3

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import org.jtransforms.fft.FloatFFT_1D
import java.io.File
import java.nio.FloatBuffer
import kotlin.math.PI
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.ln1p
import kotlin.math.log10
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * DeepFilterNet3 (Rikorose/DeepFilterNet, MIT or Apache-2.0) with ONNX Runtime and a Kotlin port of
 * libDF's signal processing: `DFState` (lib.rs) and `DfTract::process` (tract.rs) of the deep-filter
 * 0.5.6 binary, with that binary's defaults: no attenuation limit, no post-filter, local-SNR
 * thresholds -15, 35 and 35 dB. Unlike that binary, it processes every hop, also a silent one, like
 * the Python package.
 *
 * The upstream ONNX graphs take a whole frame sequence and have no state inputs, so a [Stream] runs
 * them on chunks of frames instead of once per hop. One run over a whole clip gives the same result
 * as libDF's pulsed models, because their convolutions are causal and their GRUs start from zero.
 *
 * @param modelDir folder with `enc.onnx`, `erb_dec.onnx`, `df_dec.onnx` and `config.ini` of
 *   `DeepFilterNet3_onnx.tar.gz`.
 */
internal class DeepFilterNet3(
    modelDir: File,
) : AutoCloseable {
    val sampleRate: Int
    private val fftSize: Int
    private val hopSize: Int
    private val nbErb: Int
    private val nbDf: Int
    private val dfOrder: Int
    private val convLookahead: Int
    private val lookahead: Int
    private val alpha: Float
    private val erbWidths: IntArray
    private val freqSize: Int
    private val window: FloatArray
    private val wnorm: Float

    private val environment = OrtEnvironment.getEnvironment()
    private val encoder: OrtSession
    private val erbDecoder: OrtSession
    private val dfDecoder: OrtSession

    init {
        val config = readIni(File(modelDir, "config.ini"))
        val df = config["df"] ?: error("config.ini has no [df] section")
        val net = config["deepfilternet"] ?: error("config.ini has no [deepfilternet] section")
        val model = config["train"]?.get("model")
        require(model == "deepfilternet3") { "Unsupported model type $model" }

        fun value(key: String) = df[key] ?: net[key] ?: error("config.ini has no $key")
        sampleRate = value("sr").toInt()
        fftSize = value("fft_size").toInt()
        hopSize = value("hop_size").toInt()
        nbErb = value("nb_erb").toInt()
        nbDf = value("nb_df").toInt()
        dfOrder = value("df_order").toInt()
        convLookahead = net["conv_lookahead"]?.toInt() ?: error("config.ini has no conv_lookahead")
        lookahead = maxOf(convLookahead, value("df_lookahead").toInt())
        alpha = df["norm_alpha"]?.toFloat() ?: calcNormAlpha(sampleRate, hopSize, value("norm_tau").toFloat())
        require(hopSize * 2 <= fftSize && fftSize % 2 == 0) { "Unsupported fft_size $fftSize and hop_size $hopSize" }
        freqSize = fftSize / 2 + 1
        erbWidths = erbFb(sampleRate, fftSize, nbErb, value("min_nb_erb_freqs").toInt())
        val half = fftSize / 2
        window = FloatArray(fftSize) {
            val s = sin(0.5 * PI * (it + 0.5) / half)
            sin(0.5 * PI * s * s).toFloat()
        }
        wnorm = 1f / (fftSize.toFloat() * fftSize / (2 * hopSize))

        val sessions = mutableListOf<OrtSession>()
        try {
            OrtSession.SessionOptions().use { options ->
                options.setIntraOpNumThreads(1)
                // Without the arena, ONNX Runtime returns the activations of a clip after each run; the arena keeps them.
                options.setCPUArenaAllocator(false)
                for (name in listOf("enc.onnx", "erb_dec.onnx", "df_dec.onnx")) {
                    sessions += environment.createSession(File(modelDir, name).path, options)
                }
            }
        } catch (exception: Exception) {
            sessions.forEach(OrtSession::close)
            throw exception
        }
        encoder = sessions[0]
        erbDecoder = sessions[1]
        dfDecoder = sessions[2]
    }

    /**
     * Enhances one channel of 48 kHz float samples in [-1, 1], block by block, so that memory stays
     * bounded for a long clip: one graph run over 30 s raised the peak footprint by about 240 MB, over
     * 2 min by about 900 MB (macOS arm64, ONNX Runtime 1.26, one thread).
     *
     * The graphs run on chunks of [chunkFrames] frames, each after [warmupFrames] frames of the chunk
     * before, whose output is dropped, so that the GRUs settle. The graphs are causal, so a chunk needs
     * no later frames. The signal processing runs on every frame in order, as on a whole clip. A clip of
     * up to [chunkFrames] frames gets the result of one run over the whole clip.
     */
    inner class Stream(
        private val chunkFrames: Int = CHUNK_FRAMES,
        private val warmupFrames: Int = WARMUP_FRAMES,
    ) {
        private val fft = FloatFFT_1D(fftSize.toLong())
        private val buffer = FloatArray(fftSize)
        private val analysisMem = FloatArray(fftSize - hopSize)
        private val synthesisMem = FloatArray(fftSize - hopSize)
        private val meanNorm = FloatArray(nbErb) { MEAN_NORM_INIT_MIN + it * ((MEAN_NORM_INIT_MAX - MEAN_NORM_INIT_MIN) / (nbErb - 1)) }
        private val unitNorm = FloatArray(nbDf) { UNIT_NORM_INIT_MIN + it * ((UNIT_NORM_INIT_MAX - UNIT_NORM_INIT_MIN) / (nbDf - 1)) }
        private val enhanced = FloatArray(freqSize * 2)
        private val delay = fftSize - hopSize + lookahead * hopSize
        private val hopInput = FloatArray(hopSize)
        private var hopFill = 0
        private var inputLength = 0L
        private var nextHop = 0L
        private val pending = ArrayDeque<Frame>()
        private var history = emptyList<Frame>()

        // Output from the absolute sample index `emitted` on.
        private var output = FloatArray(0)
        private var emitted = 0L

        init {
            // The enhancement of a frame reads the spectra of the frames before it.
            require(chunkFrames > 0 && warmupFrames >= maxOf(dfOrder, lookahead)) {
                "Unsupported chunk of $chunkFrames and warm-up of $warmupFrames frames"
            }
        }

        /** Takes the next [samples] of the clip; returns the output that is final, aligned with the input. */
        fun process(samples: FloatArray): FloatArray {
            for (sample in samples) {
                hopInput[hopFill++] = sample
                if (hopFill == hopSize) {
                    analyseHop()
                    hopFill = 0
                }
            }
            inputLength += samples.size
            while (pending.size >= chunkFrames) runChunk(chunkFrames)
            // A hop that is pending or not analysed yet can still write its output.
            return take(until = minOf(pending.firstOrNull()?.hop ?: nextHop, nextHop) * hopSize - delay)
        }

        /** Returns the rest of the output, so that the whole output has the length of the whole input. */
        fun finish(): FloatArray {
            if (inputLength == 0L) return FloatArray(0)
            if (hopFill > 0) {
                hopInput.fill(0f, hopFill, hopSize)
                analyseHop()
                hopFill = 0
            }
            // Extra hops after the input push out its last `delay` samples.
            val totalHops = (inputLength + delay + hopSize - 1) / hopSize
            hopInput.fill(0f)
            while (nextHop < totalHops) analyseHop()
            while (pending.isNotEmpty()) runChunk(minOf(chunkFrames, pending.size))
            return take(until = inputLength)
        }

        private fun analyseHop() {
            val hop = nextHop++
            val frame = Frame(hop = hop, spec = FloatArray(freqSize * 2), erb = FloatArray(nbErb), dfSpec = FloatArray(2 * nbDf))
            analysis(hopInput, 0, analysisMem, buffer, fft, frame.spec, 0)
            features(frame.spec, 0, meanNorm, unitNorm, frame.erb, frame.dfSpec, 1)
            pending.addLast(frame)
        }

        private fun runChunk(count: Int) {
            val frames = history + List(count) { pending.removeFirst() }
            val size = frames.size
            val spec = FloatArray(size * freqSize * 2)
            val featErb = FloatArray(size * nbErb)
            val featSpec = FloatArray(2 * size * nbDf)
            frames.forEachIndexed { index, frame ->
                System.arraycopy(frame.spec, 0, spec, index * freqSize * 2, freqSize * 2)
                System.arraycopy(frame.erb, 0, featErb, index * nbErb, nbErb)
                // feat_spec is [1, 2, S, nb_df]: real parts in channel 0, imaginary parts in channel 1.
                System.arraycopy(frame.dfSpec, 0, featSpec, index * nbDf, nbDf)
                System.arraycopy(frame.dfSpec, nbDf, featSpec, (size + index) * nbDf, nbDf)
            }
            val (gains, coefs) = runModels(featErb, featSpec, size)
            for (index in history.size until size) {
                enhance(spec, gains, coefs, index, enhanced)
                synthesis(enhanced, buffer, fft, synthesisMem)
                write(start = frames[index].hop * hopSize - delay)
            }
            history = frames.takeLast(warmupFrames)
        }

        /** Writes the first hop of [buffer] at the absolute sample index [start]. */
        private fun write(start: Long) {
            for (i in 0 until hopSize) {
                val target = start + i - emitted
                if (target < 0) continue
                if (target >= output.size) output = output.copyOf(maxOf(output.size * 2, target.toInt() + hopSize))
                output[target.toInt()] = buffer[i]
            }
        }

        /** Returns the output up to the absolute sample index [until]. */
        private fun take(until: Long): FloatArray {
            val count = (until - emitted).coerceAtLeast(0).toInt()
            val result = output.copyOf(count)
            output = if (count < output.size) output.copyOfRange(count, output.size) else FloatArray(0)
            emitted += count
            return result
        }
    }

    /** One analysed hop: its index in the clip, its spectrum and its features. */
    private class Frame(
        val hop: Long,
        val spec: FloatArray,
        val erb: FloatArray,
        val dfSpec: FloatArray,
    )

    override fun close() {
        encoder.close()
        erbDecoder.close()
        dfDecoder.close()
    }

    private fun analysis(
        input: FloatArray,
        start: Int,
        mem: FloatArray,
        buffer: FloatArray,
        fft: FloatFFT_1D,
        spec: FloatArray,
        offset: Int,
    ) {
        val hop = hopSize
        val split = mem.size
        for (i in 0 until split) buffer[i] = mem[i] * window[i]
        for (i in 0 until hop) {
            val position = start + i
            buffer[split + i] = (if (position < input.size) input[position] else 0f) * window[split + i]
        }
        System.arraycopy(mem, hop, mem, 0, split - hop)
        for (i in 0 until hop) {
            val position = start + i
            mem[split - hop + i] = if (position < input.size) input[position] else 0f
        }
        fft.realForward(buffer)
        // JTransforms packs the real parts of DC and Nyquist into indexes 0 and 1.
        spec[offset] = buffer[0] * wnorm
        spec[offset + 1] = 0f
        spec[offset + (freqSize - 1) * 2] = buffer[1] * wnorm
        spec[offset + (freqSize - 1) * 2 + 1] = 0f
        for (i in 2 until fftSize) spec[offset + i] = buffer[i] * wnorm
    }

    private fun features(
        spec: FloatArray,
        frame: Int,
        meanNorm: FloatArray,
        unitNorm: FloatArray,
        featErb: FloatArray,
        featSpec: FloatArray,
        frames: Int,
    ) {
        val offset = frame * freqSize * 2
        val oneMinusAlpha = 1f - alpha
        var bin = 0
        for (b in 0 until nbErb) {
            val width = erbWidths[b]
            val k = 1f / width
            var power = 0f
            for (i in bin until bin + width) {
                val re = spec[offset + 2 * i]
                val im = spec[offset + 2 * i + 1]
                power += (re * re + im * im) * k
            }
            bin += width
            val db = log10(power + 1e-10f) * 10f
            meanNorm[b] = db * oneMinusAlpha + meanNorm[b] * alpha
            featErb[frame * nbErb + b] = (db - meanNorm[b]) / 40f
        }
        // feat_spec is [1, 2, S, nb_df]: real parts in channel 0, imaginary parts in channel 1.
        val imagOffset = frames * nbDf
        for (f in 0 until nbDf) {
            val re = spec[offset + 2 * f]
            val im = spec[offset + 2 * f + 1]
            unitNorm[f] = hypot(re, im) * oneMinusAlpha + unitNorm[f] * alpha
            val scale = sqrt(unitNorm[f])
            featSpec[frame * nbDf + f] = re / scale
            featSpec[imagOffset + frame * nbDf + f] = im / scale
        }
    }

    private fun runModels(
        featErb: FloatArray,
        featSpec: FloatArray,
        frames: Int,
    ): Pair<FloatArray, FloatArray> {
        val s = frames.toLong()
        OnnxTensor.createTensor(environment, FloatBuffer.wrap(featErb), longArrayOf(1, 1, s, nbErb.toLong())).use { erb ->
            OnnxTensor.createTensor(environment, FloatBuffer.wrap(featSpec), longArrayOf(1, 2, s, nbDf.toLong())).use { cplx ->
                encoder.run(mapOf("feat_erb" to erb, "feat_spec" to cplx)).use { encoded ->
                    fun tensor(name: String) = encoded.get(name).get() as OnnxTensor
                    val gainInputs = listOf("emb", "e3", "e2", "e1", "e0").associateWith(::tensor)
                    val gains = erbDecoder.run(gainInputs, setOf("m")).use { it.floats("m") }
                    val dfInputs = mapOf("emb" to tensor("emb"), "c0" to tensor("c0"))
                    val coefs = dfDecoder.run(dfInputs, setOf("coefs")).use { it.floats("coefs") }
                    check(gains.size == frames * nbErb) { "erb_dec returned ${gains.size} values for $frames frames" }
                    check(coefs.size == frames * nbDf * dfOrder * 2) { "df_dec returned ${coefs.size} values for $frames frames" }
                    return gains to coefs
                }
            }
        }
    }

    /**
     * The enhanced spectrum that libDF synthesises at step [frame]: the ERB gains of this step on the
     * spectrum `conv_lookahead` steps back, and below `nb_df` the deep filter of this step over the
     * last `max(df_order, lookahead)` spectra, oldest first.
     */
    private fun enhance(
        spec: FloatArray,
        gains: FloatArray,
        coefs: FloatArray,
        frame: Int,
        out: FloatArray,
    ) {
        val masked = frame - convLookahead
        if (masked >= 0) {
            System.arraycopy(spec, masked * freqSize * 2, out, 0, freqSize * 2)
            var bin = 0
            for (b in 0 until nbErb) {
                val gain = gains[frame * nbErb + b]
                for (i in bin until bin + erbWidths[b]) {
                    out[2 * i] *= gain
                    out[2 * i + 1] *= gain
                }
                bin += erbWidths[b]
            }
        } else {
            out.fill(0f)
        }
        for (i in 0 until 2 * nbDf) out[i] = 0f
        val oldest = frame - maxOf(dfOrder, lookahead) + 1
        for (k in 0 until dfOrder) {
            val source = oldest + k
            if (source < 0) continue
            val specOffset = source * freqSize * 2
            for (f in 0 until nbDf) {
                val c = ((frame * nbDf + f) * dfOrder + k) * 2
                val sRe = spec[specOffset + 2 * f]
                val sIm = spec[specOffset + 2 * f + 1]
                val cRe = coefs[c]
                val cIm = coefs[c + 1]
                out[2 * f] += sRe * cRe - sIm * cIm
                out[2 * f + 1] += sRe * cIm + sIm * cRe
            }
        }
    }

    /** Inverse FFT without scaling, window, overlap-add. The first `hop_size` samples of [buffer] are the output. */
    private fun synthesis(
        spec: FloatArray,
        buffer: FloatArray,
        fft: FloatFFT_1D,
        mem: FloatArray,
    ) {
        val hop = hopSize
        // The packed format has no slot for the imaginary parts of DC and Nyquist; realfft ignores them too.
        buffer[0] = spec[0]
        buffer[1] = spec[(freqSize - 1) * 2]
        System.arraycopy(spec, 2, buffer, 2, fftSize - 2)
        fft.realInverse(buffer, false)
        for (i in 0 until fftSize) buffer[i] *= window[i]
        for (i in 0 until hop) buffer[i] += mem[i]
        val split = mem.size - hop
        System.arraycopy(mem, hop, mem, 0, split)
        for (i in 0 until split) mem[i] += buffer[hop + i]
        System.arraycopy(buffer, hop + split, mem, split, fftSize - hop - split)
    }

    private fun OrtSession.Result.floats(name: String): FloatArray {
        val buffer = (get(name).get() as OnnxTensor).floatBuffer
        return FloatArray(buffer.remaining()).also { buffer.get(it) }
    }

    private companion object {
        // 10 s per graph run. The GRUs settle slowly: on the 12 s reference clip of the tests, the 2 s
        // after a boundary score 26.8 dB SI-SDR against the Python reference with 2 s of warm-up, 34.3 dB
        // with 5 s and 36.1 dB with 8 s, and 41.4 dB in one run (JVM, macOS arm64).
        const val CHUNK_FRAMES = 1000
        const val WARMUP_FRAMES = 500
        const val MEAN_NORM_INIT_MIN = -60f
        const val MEAN_NORM_INIT_MAX = -90f
        const val UNIT_NORM_INIT_MIN = 0.001f
        const val UNIT_NORM_INIT_MAX = 0.0001f

        fun readIni(file: File): Map<String, Map<String, String>> {
            val sections = mutableMapOf<String, MutableMap<String, String>>()
            var section = sections.getOrPut("") { mutableMapOf() }
            file.forEachLine { raw ->
                val line = raw.trim()
                when {
                    line.isEmpty() || line.startsWith("#") || line.startsWith(";") -> Unit
                    line.startsWith("[") && line.endsWith("]") ->
                        section =
                            sections.getOrPut(line.substring(1, line.length - 1).trim()) { mutableMapOf() }
                    "=" in line -> section[line.substringBefore("=").trim()] = line.substringAfter("=").trim()
                }
            }
            return sections
        }

        /** libDF rounds exp(-hop / sr / tau) to the fewest decimals that keep it below 1: 0.99 for 48 kHz, hop 480, tau 1. */
        fun calcNormAlpha(
            sampleRate: Int,
            hopSize: Int,
            tau: Float,
        ): Float {
            val alpha = exp(-(hopSize.toFloat() / sampleRate.toFloat()) / tau)
            var a = 1f
            var precision = 3
            while (a >= 1f) {
                val scale = pow10(precision).toFloat()
                a = roundHalfAway(alpha * scale) / scale
                precision++
            }
            return a
        }

        /** Band widths in FFT bins of libDF's ERB filter bank; they sum to fft_size / 2 + 1. */
        fun erbFb(
            sampleRate: Int,
            fftSize: Int,
            bands: Int,
            minBins: Int,
        ): IntArray {
            fun freq2erb(hz: Float) = 9.265f * ln1p(hz / (24.7f * 9.265f))

            fun erb2freq(erb: Float) = 24.7f * 9.265f * (exp(erb / 9.265f) - 1f)
            val freqWidth = sampleRate.toFloat() / fftSize.toFloat()
            val low = freq2erb(0f)
            val high = freq2erb((sampleRate / 2).toFloat())
            val step = (high - low) / bands
            val widths = IntArray(bands)
            var previous = 0
            var over = 0
            for (i in 1..bands) {
                val bin = roundHalfAway(erb2freq(low + i * step) / freqWidth).toInt()
                var count = bin - previous - over
                if (count < minBins) {
                    over = minBins - count
                    count = minBins
                } else {
                    over = 0
                }
                widths[i - 1] = count
                previous = bin
            }
            widths[bands - 1] += 1
            val tooLarge = widths.sum() - (fftSize / 2 + 1)
            if (tooLarge > 0) widths[bands - 1] -= tooLarge
            check(widths.sum() == fftSize / 2 + 1) { "ERB bands cover ${widths.sum()} bins" }
            return widths
        }

        /** Rust's f32::round rounds half away from zero, so Math.round, not kotlin.math.round (half to even). Positive x only. */
        fun roundHalfAway(x: Float): Float = Math.round(x).toFloat()

        fun pow10(n: Int): Int {
            var value = 1
            repeat(n) { value *= 10 }
            return value
        }
    }
}
