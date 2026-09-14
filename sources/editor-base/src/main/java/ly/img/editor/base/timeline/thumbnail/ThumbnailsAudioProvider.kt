package ly.img.editor.base.timeline.thumbnail
import android.os.SystemClock
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ly.img.editor.base.timeline.clip.Clip
import ly.img.editor.base.timeline.state.TimelineConfiguration
import ly.img.engine.Engine
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
    private var requestedHasAudioResource: Boolean = false
    private var requestedAudioUri: String = ""
    private var lastLiveBufferRefreshRealtimeMs: Long = 0L

    override fun loadContent(
        clip: Clip,
        width: Dp,
    ) {
        val barStride = TimelineConfiguration.audioWaveformBarWidth + TimelineConfiguration.audioWaveformBarGap
        val numberOfSamples = (width / barStride).toInt().coerceAtLeast(1)
        val audioUri = if (clip.hasAudioResource) {
            runCatching { engine.block.getString(clip.id, "audio/fileURI") }.getOrDefault("")
        } else {
            ""
        }
        val isLiveBufferResource = audioUri.startsWith("buffer://")
        val now = SystemClock.elapsedRealtime()

        val sameRequest = clip.trimOffset == requestedTrimOffset &&
            clip.duration == requestedDuration &&
            numberOfSamples == requestedNumberOfSamples &&
            clip.hasAudioResource == requestedHasAudioResource &&
            audioUri == requestedAudioUri

        if (sameRequest) {
            if (job?.isActive == true) return
            if (_loadedTrimOffset.value == clip.trimOffset && loadedDuration.value == clip.duration) return
        }

        if (
            isLiveBufferResource &&
            _samples.value.isNotEmpty() &&
            now - lastLiveBufferRefreshRealtimeMs < LIVE_BUFFER_THUMBNAIL_REFRESH_INTERVAL_MS
        ) {
            return
        }

        job?.cancel()
        requestedTrimOffset = clip.trimOffset
        requestedDuration = clip.duration
        requestedNumberOfSamples = numberOfSamples
        requestedHasAudioResource = clip.hasAudioResource
        requestedAudioUri = audioUri

        // Draft voiceover clips do not have an audio resource yet.
        // Avoid hammering engine thumbnail generation with failing requests.
        if (!clip.hasAudioResource) {
            _samples.value = listOf(0f)
            _loadedTrimOffset.value = clip.trimOffset
            loadedDuration.value = clip.duration
            return
        }

        val trimOffset = clip.trimOffset
        val duration = clip.duration
        val trimOffsetSeconds = trimOffset.toDouble(DurationUnit.SECONDS)
        val durationSeconds = duration.toDouble(DurationUnit.SECONDS)
        val timeEnd = trimOffsetSeconds + durationSeconds

        suspend fun loadWaveformSamples(): Boolean {
            val samples = buildList<Float> {
                engine.block.generateAudioThumbnailSequence(
                    block = clip.trimmableId,
                    samplesPerChunk = numberOfSamples,
                    timeBegin = trimOffsetSeconds,
                    timeEnd = timeEnd,
                    numberOfSamples = numberOfSamples,
                    numberOfChannels = 1,
                ).collect { result ->
                    addAll(result.samples)
                }
            }
            if (samples.isEmpty()) return false
            _samples.value = if (samples.size <= numberOfSamples) {
                samples
            } else {
                samples.subList(0, numberOfSamples)
            }
            _loadedTrimOffset.value = trimOffset
            loadedDuration.value = duration
            return true
        }

        suspend fun tryLoadWaveformSamples(): Boolean = runCatching {
            loadWaveformSamples()
        }.getOrElse { throwable ->
            if (throwable is CancellationException) throw throwable
            false
        }

        job = scope.launch {
            if (isLiveBufferResource) {
                if (tryLoadWaveformSamples()) {
                    lastLiveBufferRefreshRealtimeMs = SystemClock.elapsedRealtime()
                }
                return@launch
            }

            if (tryLoadWaveformSamples()) return@launch

            runCatching { engine.block.forceLoadAVResource(clip.trimmableId) }

            repeat(20) { attempt ->
                if (attempt > 0) {
                    delay(100)
                }
                val isResourceLoaded = runCatching {
                    engine.block.isAVResourceLoaded(clip.trimmableId)
                }.getOrDefault(false)
                if (!isResourceLoaded) {
                    return@repeat
                }
                if (tryLoadWaveformSamples()) return@launch
            }

            _samples.value = emptyList()
        }
    }

    override fun cancel() {
        job?.cancel()
    }
}

private const val LIVE_BUFFER_THUMBNAIL_REFRESH_INTERVAL_MS = 400L
