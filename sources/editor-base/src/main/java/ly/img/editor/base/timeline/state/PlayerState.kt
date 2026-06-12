package ly.img.editor.base.timeline.state

import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import ly.img.editor.core.ui.engine.getCurrentPage
import ly.img.editor.core.ui.utils.formatForPlayer
import ly.img.engine.Engine
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

class PlayerState(
    private val engine: Engine,
) {
    var isPlaying: Boolean by mutableStateOf(false)
        private set

    var playheadPosition: Duration by mutableStateOf(0.seconds)
        private set

    var isLooping: Boolean by mutableStateOf(false)

    var maxPlaybackDuration: Duration? by mutableStateOf(null)

    val formattedPlayheadPosition by derivedStateOf {
        playheadPosition.formatForPlayer()
    }

    private val page = engine.getCurrentPage()

    fun refresh() {
        val playbackTime = engine.block.getPlaybackTime(page).seconds
        val maxDuration = maxPlaybackDuration
        isPlaying = engine.block.isPlaying(page)
        isLooping = engine.block.isLooping(page)
        if (maxDuration != null && playbackTime > maxDuration) {
            if (isPlaying) {
                if (isLooping) {
                    setPlaybackTime(0.seconds)
                } else {
                    pause()
                    isPlaying = false
                    setPlaybackTime(maxDuration)
                }
            } else {
                setPlaybackTime(maxDuration)
            }
        } else {
            playheadPosition = playbackTime
        }
    }

    fun play() {
        val duration = maxPlaybackDuration ?: engine.block.getDuration(page).seconds
        if (duration == ZERO) return
        if (playheadPosition >= duration) {
            setPlaybackTime(0.seconds)
        }
        engine.block.setPlaying(page, true)
    }

    fun pause() {
        // Only stop playback if actually playing. `setPlaying(page, false)` is not a no-op when
        // idle: the engine flips edit mode to TRANSFORM, which would tear down an active text-edit
        // session (e.g. when opening a sheet over the keyboard). Skipping it when idle keeps pause
        // purely about playback. Query the engine directly rather than the cached `isPlaying` flag,
        // which lags `play()` until the next `refresh()`.
        if (!engine.block.isPlaying(page)) return
        engine.block.setPlaying(page, false)
    }

    fun togglePlayback() {
        if (isPlaying) {
            pause()
        } else {
            play()
        }
    }

    fun toggleLooping() {
        engine.block.setLooping(page, !isLooping)
    }

    fun setPlaybackTime(duration: Duration) {
        val clampedDuration = maxPlaybackDuration?.let { duration.coerceAtMost(it) } ?: duration
        engine.block.setPlaybackTime(page, clampedDuration.toDouble(DurationUnit.SECONDS))
        // Optimistic mirror so the playhead doesn't visibly jump back for one tick before the
        // engine refresh catches up.
        playheadPosition = clampedDuration
    }
}
