package ly.img.camera.record

import android.content.Context
import android.graphics.RectF
import android.net.Uri
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import ly.img.camera.core.Capture
import ly.img.camera.core.Recording
import ly.img.camera.core.Video
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

internal class RecordingManager(
    private val maxDuration: Duration,
    private val allowExceedingMaxDuration: Boolean,
    private val photoClipDuration: Duration,
    private val coroutineScope: CoroutineScope,
    private val videoRecorder: VideoRecorder,
    private val photoCapture: PhotoCapture,
) {
    suspend fun takePhoto(context: Context): Uri = photoCapture.capture(context)

    var state by mutableStateOf(State(maxDuration = maxDuration))
        private set

    val hasNotRecordedYet by derivedStateOf { state.totalRecordedDuration == Duration.ZERO }

    val isRecording
        get() = state.status is Status.Recording
    val hasStartedRecording
        get() = state.status is Status.Recording || state.status is Status.StartRecording

    private var overridenMaxDuration: Duration? = null

    lateinit var cameraRect: RectF

    fun toggleRecording(context: Context) {
        when {
            state.hasReachedMaxDuration -> return
            state.status is Status.TimerRunning -> resetTimer()
            hasStartedRecording -> stop()
            state.timer != Timer.Off -> startTimer { startRecording(context) }
            else -> coroutineScope.launch { startRecording(context) }
        }
    }

    /**
     * Photo-side mirror of [toggleRecording]'s timer handling: tap during countdown
     * cancels it, otherwise the [Timer] setting runs before [takePhoto]. Unlike
     * [toggleRecording] there's no in-progress equivalent to stop — a tap while the photo
     * pipeline is mid-capture is ignored (`!is Idle`) rather than acting as a stop.
     */
    fun runWithTimerForPhoto(takePhoto: suspend () -> Unit) {
        when {
            state.hasReachedMaxDuration -> return
            state.status is Status.TimerRunning -> resetTimer()
            state.status !is Status.Idle -> return
            state.timer != Timer.Off -> startTimer(takePhoto)
            else -> coroutineScope.launch { takePhoto() }
        }
    }

    /**
     * Records now, bypassing any [Timer] countdown. Cancels [timerJob] directly instead
     * of via [resetTimer] so [Status] flips `TimerRunning → StartRecording` and the
     * countdown overlay morphs into the recording indicator (not the cancelled symbol).
     */
    fun startRecordingImmediately(context: Context) {
        if (state.hasReachedMaxDuration || hasStartedRecording) return
        timerJob?.cancel()
        timerJob = null
        coroutineScope.launch { startRecording(context) }
    }

    fun setTimer(timer: Timer) {
        state = state.copy(timer = timer)
    }

    // We intentionally use GlobalScope here so the deletion coroutine isn't cancelled on closing the camera.
    @OptIn(DelicateCoroutinesApi::class)
    fun deletePreviousRecording() {
        val capture = state.captures.lastOrNull() ?: return
        val captures = state.captures.dropLast(1)
        val updatedDuration = calculateUpdatedDuration(captures)
        state = state.copy(
            captures = captures,
            totalRecordedDuration = updatedDuration,
            hasReachedMaxDuration = hasReachedMaxDuration(updatedDuration),
        )
        GlobalScope.launch(Dispatchers.IO) {
            deleteSingleCapture(capture)
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    fun close() {
        videoRecorder.close()
        val captures = state.captures
        if (captures.isEmpty()) return
        GlobalScope.launch(Dispatchers.IO) {
            captures.forEach(::deleteSingleCapture)
        }
        state = state.copy(captures = emptyList())
    }

    fun stop() {
        if (state.status is Status.TimerRunning) {
            resetTimer()
        } else if (hasStartedRecording) {
            videoRecorder.stopRecording()
        }
    }

    fun enable() {
        state = state.copy(status = Status.Idle)
    }

    fun overrideMaxDuration(duration: Duration) {
        overridenMaxDuration = duration
        state = state.copy(maxDuration = duration)
    }

    internal suspend fun startRecording(context: Context) {
        state = state.copy(status = Status.StartRecording)
        var reachedMaxDuration = false
        videoRecorder.startRecording(context) { status ->
            when (status) {
                is VideoRecorder.RecordingStatus.Error -> {
                    state = state.copy(status = Status.Idle, totalRecordedDuration = calculateUpdatedDuration())
                    // Since the recording failed, delete the backing file if it exists.
                    GlobalScope.launch(Dispatchers.IO) {
                        deleteFileUri(status.outputUri)
                    }
                }

                is VideoRecorder.RecordingStatus.Finished -> {
                    // Compute the total BEFORE appending: the helper reads the in-flight
                    // `currentRecordingDuration` from the still-active Recording status, which
                    // stands in for the just-finished clip's duration. Appending first would
                    // double-count.
                    val updatedDuration = calculateUpdatedDuration()
                    val recording = Recording(
                        videos = listOf(Video(uri = status.outputUri, rect = cameraRect)),
                        duration = status.duration,
                    )
                    state = state.copy(
                        status = Status.Idle,
                        captures = state.captures + Capture.Video(recording),
                        totalRecordedDuration = updatedDuration,
                        hasReachedMaxDuration = hasReachedMaxDuration(updatedDuration),
                    )
                }

                is VideoRecorder.RecordingStatus.Recording -> {
                    val updatedDuration = calculateUpdatedDuration()
                    if (!reachedMaxDuration) {
                        state = state.copy(
                            status = Status.Recording(status.duration),
                            totalRecordedDuration = updatedDuration,
                        )
                    }
                    if (hasReachedMaxDuration(updatedDuration)) {
                        reachedMaxDuration = true
                        stop()
                    }
                }
            }
        }
    }

    private var timerJob: Job? = null

    private fun startTimer(onComplete: suspend () -> Unit) {
        var countDownValue = state.timer.duration
        state = state.copy(status = Status.TimerRunning(remainingTime = countDownValue, totalTime = countDownValue))
        timerJob = coroutineScope.launch {
            while (isActive && countDownValue > 0) {
                delay(1000)
                countDownValue--
                if (countDownValue != 0) {
                    val status = state.status as? Status.TimerRunning ?: break
                    state = state.copy(status = status.copy(remainingTime = countDownValue))
                }
            }
            yield()
            onComplete()
        }
    }

    private fun resetTimer() {
        timerJob?.cancel()
        timerJob = null
        state = state.copy(status = Status.Idle)
    }

    private fun deleteSingleCapture(capture: Capture) {
        when (capture) {
            is Capture.Photo -> deleteFileUri(capture.uri)
            is Capture.Video -> capture.recording.videos.forEach { deleteFileUri(it.uri) }
        }
    }

    private fun deleteFileUri(uri: Uri) {
        runCatching {
            val file = File(checkNotNull(uri.path))
            file.delete()
        }
    }

    private fun calculateUpdatedDuration(
        captures: List<Capture> = state.captures,
        currentRecordingDuration: Duration? = (state.status as? Status.Recording)?.currentRecordingDuration,
    ) = captures.fold(0.seconds) { total, capture ->
        total + when (capture) {
            is Capture.Photo -> capture.clipDuration
            is Capture.Video -> capture.recording.duration
        }
    } + (currentRecordingDuration ?: 0.seconds)

    private fun hasReachedMaxDuration(duration: Duration): Boolean = overridenMaxDuration?.let {
        duration >= it
    } ?: (!allowExceedingMaxDuration && duration >= maxDuration)

    fun setTakingPhoto() {
        state = state.copy(status = Status.TakingPhoto)
    }

    fun finishTakingPhoto() {
        if (state.status is Status.TakingPhoto) {
            state = state.copy(status = Status.Idle)
        }
    }

    fun addPhoto(uri: Uri) {
        val captures = state.captures + Capture.Photo(uri = uri, clipDuration = photoClipDuration)
        val updatedDuration = calculateUpdatedDuration(captures)
        state = state.copy(
            captures = captures,
            totalRecordedDuration = updatedDuration,
            hasReachedMaxDuration = hasReachedMaxDuration(updatedDuration),
        )
    }

    sealed interface Status {
        data object Disabled : Status

        data object Idle : Status

        data class TimerRunning(
            val remainingTime: Int,
            val totalTime: Int,
        ) : Status

        data object StartRecording : Status

        data class Recording(
            val currentRecordingDuration: Duration,
        ) : Status

        /**
         * The camera is mid-capture for a still photo (shutter fired, file write pending).
         * The UI shows a momentary white flash + plays a haptic during this status.
         */
        data object TakingPhoto : Status
    }

    data class State(
        val timer: Timer = Timer.Off,
        val captures: List<Capture> = emptyList(),
        val totalRecordedDuration: Duration = Duration.ZERO,
        val hasReachedMaxDuration: Boolean = false,
        val status: Status = Status.Disabled,
        val maxDuration: Duration,
    )
}
