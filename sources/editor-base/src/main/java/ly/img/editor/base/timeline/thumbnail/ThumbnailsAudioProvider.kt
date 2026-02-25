package ly.img.editor.base.timeline.thumbnail

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ly.img.editor.base.timeline.clip.Clip
import ly.img.editor.base.timeline.state.TimelineConfiguration
import ly.img.engine.Engine
import ly.img.engine.EngineException
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/**
 * Provider for audio clip waveform data.
 * Fetches real audio samples from the engine to render actual waveform visualization.
 *
 * The provider fetches audio samples from the engine via generateAudioThumbnailSequence
 * and the view renders the samples as vertical bars representing audio amplitude.
 */
class ThumbnailsAudioProvider(
    private val engine: Engine,
    private val scope: CoroutineScope,
) : ThumbnailsProvider {
    private var _samples = mutableStateOf(emptyList<Float>())

    /**
     * The audio waveform samples. Each value represents the amplitude (0.0 to 1.0)
     * at that point in time. Used to render waveform bars.
     */
    val samples: List<Float>
        get() = _samples.value

    private var _loadedTrimOffset = mutableStateOf(0.seconds)

    /**
     * The trim offset at which the current waveform samples were generated.
     * Used to compute a visual offset during trim gestures so the waveform
     * stays in place until the gesture ends and new samples are fetched.
     */
    val loadedTrimOffset: Duration
        get() = _loadedTrimOffset.value

    private var loadedDuration = mutableStateOf(0.seconds)

    /**
     * Loading state is true when samples haven't been fetched yet.
     */
    override val isLoading: Boolean
        get() = _samples.value.isEmpty()

    private var job: Job? = null
    private var requestedTrimOffset: Duration = 0.seconds
    private var requestedDuration: Duration = 0.seconds
    private var requestedNumberOfSamples: Int = 0

    override fun loadContent(
        clip: Clip,
        width: Dp,
    ) {
        val barStride = TimelineConfiguration.audioWaveformBarWidth + TimelineConfiguration.audioWaveformBarGap
        val numberOfSamples = (width / barStride).toInt().coerceAtLeast(1)

        val sameRequest = clip.trimOffset == requestedTrimOffset &&
            clip.duration == requestedDuration &&
            numberOfSamples == requestedNumberOfSamples

        if (sameRequest) {
            if (job?.isActive == true) return
            if (_loadedTrimOffset.value == clip.trimOffset && loadedDuration.value == clip.duration) return
        }

        job?.cancel()
        requestedTrimOffset = clip.trimOffset
        requestedDuration = clip.duration
        requestedNumberOfSamples = numberOfSamples

        job = scope.launch {
            try {
                val trimOffsetSeconds = clip.trimOffset.toDouble(DurationUnit.SECONDS)
                val durationSeconds = clip.duration.toDouble(DurationUnit.SECONDS)
                val timeEnd = trimOffsetSeconds + durationSeconds
                engine.block.generateAudioThumbnailSequence(
                    block = clip.id,
                    samplesPerChunk = numberOfSamples,
                    timeBegin = trimOffsetSeconds,
                    timeEnd = timeEnd,
                    numberOfSamples = numberOfSamples,
                    numberOfChannels = 1,
                ).collect { result ->
                    _samples.value = result.samples
                    _loadedTrimOffset.value = clip.trimOffset
                    loadedDuration.value = clip.duration
                }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: EngineException) {
                _samples.value = emptyList()
            }
        }
    }

    override fun cancel() {
        job?.cancel()
    }
}
