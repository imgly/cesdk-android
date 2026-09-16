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

    /** The caption lane, or `null` when the scene has no captions. Always `tracks[0]`. */
    val captionTrack: Track?
        get() = _tracks.firstOrNull()?.takeIf { it.isCaptionTrack }

    /**
     * Inserts the caption lane as the topmost row. Load-bearing: drag & drop rejects a foreign clip
     * by testing the pointer against the lane's bottom edge alone.
     *
     * An existing lane is replaced rather than stacked under a second one. Importing captions over a caption track
     * that is already there leaves both attached to the page for as long as it takes to style the first caption,
     * and that styling suspends on an asset fetch, so a rebuild can run while both exist. Stacking would draw two
     * lanes and — because [addTrack] puts foreground tracks directly below the caption lane — sandwich every other
     * track between them until the next refresh corrected it.
     */
    fun addCaptionTrack(track: Track) {
        if (captionTrack == null) _tracks.add(0, track) else _tracks[0] = track
    }

    /** Prepends a foreground track, but never above the caption lane. */
    fun addTrack(track: Track) {
        _tracks.add(if (captionTrack != null) 1 else 0, track)
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

    fun allTracks(): Sequence<Track> = sequenceOf(backgroundTrack) + tracks.asSequence()

    /**
     * Latest end time across all clips, ignoring the caption lane — a caption may legitimately
     * outlast the footage it annotates, and must not stretch the ruler past it.
     */
    fun maxClipEnd(): Duration {
        var max: Duration = ZERO
        backgroundTrack.clips.forEach { clip ->
            val end = clip.timeOffset + clip.duration
            if (end > max) max = end
        }
        tracks.forEach { track ->
            if (track.isCaptionTrack) return@forEach
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
        backgroundTrack.transitionSeams.clear()
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
