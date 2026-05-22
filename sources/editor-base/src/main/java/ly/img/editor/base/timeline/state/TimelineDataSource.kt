package ly.img.editor.base.timeline.state

import androidx.compose.runtime.mutableStateListOf
import ly.img.editor.base.timeline.clip.Clip
import ly.img.editor.base.timeline.track.Track
import ly.img.engine.DesignBlock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

class TimelineDataSource {
    private val _tracks = mutableStateListOf<Track>()
    val tracks: List<Track>
        get() = _tracks

    val backgroundTrack = Track.background()

    fun addTrack(track: Track) {
        _tracks.add(0, track)
    }

    fun addAudioTrack(track: Track) {
        _tracks.add(track)
    }

    fun findClip(block: DesignBlock): Clip? {
        for (track in _tracks) {
            val result = track.clips.find { it.matches(block) }
            if (result != null) return result
        }
        return backgroundTrack.clips.find { it.matches(block) }
    }

    fun allClips(): Sequence<Clip> = backgroundTrack.clips.asSequence() +
        tracks.asSequence().flatMap { it.clips.asSequence() }

    fun maxClipEnd(): Duration {
        var max: Duration = ZERO
        backgroundTrack.clips.forEach { clip ->
            val end = clip.timeOffset + clip.duration
            if (end > max) max = end
        }
        tracks.forEach { track ->
            track.clips.forEach { clip ->
                val end = clip.timeOffset + clip.duration
                if (end > max) max = end
            }
        }
        return max
    }

    /**
     * Returns the track holding [clip].
     * @throws IllegalStateException if no track contains the clip.
     */
    internal fun findTrack(clip: Clip): Track = tracks.find { track -> track.clips.any { it.id == clip.id } }
        ?: backgroundTrack.takeIf { track -> track.clips.any { it.id == clip.id } }
        ?: error("Clip ${clip.id} not found in any track")

    fun indexOf(clip: Clip): Int = tracks.indexOfFirst {
        it.clips.contains(clip)
    }

    fun reset() {
        _tracks.clear()
        backgroundTrack.clips.clear()
    }

    override fun toString(): String = "TimelineDataSource: \n tracks=${
        tracks.flatMap { it.clips }.joinToString("\n")
    } \n backgroundTrack=${
        backgroundTrack.clips.joinToString("\n")
    }"
}

private fun Clip.matches(block: DesignBlock): Boolean = id == block ||
    trimmableId == block ||
    fillId == block ||
    shapeId == block ||
    blurId == block ||
    effectIds?.contains(block) == true
