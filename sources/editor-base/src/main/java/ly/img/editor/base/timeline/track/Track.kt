package ly.img.editor.base.timeline.track

import androidx.compose.runtime.mutableStateListOf
import ly.img.editor.base.timeline.clip.Clip
import ly.img.engine.DesignBlock

/**
 * A timeline row of [clips]. Construct via [background], [engine], or [standalone] so [id] stays
 * stable across rebuilds.
 *
 * [engineTrackId] is the backing engine foreground [ly.img.engine.DesignBlockType.Track] or
 * [ly.img.engine.DesignBlockType.CaptionTrack] block.
 */
data class Track private constructor(
    val id: String,
    val clips: MutableList<Clip> = mutableStateListOf(),
    val engineTrackId: DesignBlock? = null,
) {
    companion object {
        /** The singleton background track row at the bottom of the timeline. */
        fun background(): Track = Track(id = "background")

        /** A foreground track backed by an engine Track / CaptionTrack block. */
        fun engine(engineTrackId: DesignBlock): Track = Track(id = "engine-$engineTrackId", engineTrackId = engineTrackId)

        /** A virtual foreground track hosting a single direct page child standalone clip. */
        fun standalone(clipBlock: DesignBlock): Track = Track(id = "standalone-$clipBlock")
    }
}

/** [clips] sorted by [Clip.timeOffset] ascending. */
internal fun Track.sortedClips(): List<Clip> = clips.sortedBy { it.timeOffset }
