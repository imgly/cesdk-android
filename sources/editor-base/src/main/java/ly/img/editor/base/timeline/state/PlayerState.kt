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
                    playheadPosition = 0.seconds
                } else {
                    pause()
                    isPlaying = false
                    setPlaybackTime(maxDuration)
                    playheadPosition = maxDuration
                }
            } else {
                setPlaybackTime(maxDuration)
                playheadPosition = maxDuration
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
    }
}
