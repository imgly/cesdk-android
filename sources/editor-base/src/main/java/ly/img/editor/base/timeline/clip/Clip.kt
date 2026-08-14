package ly.img.editor.base.timeline.clip

import androidx.compose.ui.unit.Dp
import ly.img.engine.DesignBlock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds

data class Clip(
    val id: DesignBlock,
    val clipType: ClipType,
    val isVoiceOver: Boolean = false,
    val hasAudioResource: Boolean = false,
    val trimmableId: DesignBlock = id,
    val fillId: DesignBlock? = null,
    val shapeId: DesignBlock? = null,
    val blurId: DesignBlock? = null,
    val effectIds: List<DesignBlock>? = null,
    val title: String = "",
    val duration: Duration = 5.seconds,
    /** Engine duration before transition trims are projected into [duration]. */
    val rawDuration: Duration = duration,
    val footageDuration: Duration? = null,
    val playbackSpeed: Float = 1f,
    val timeOffset: Duration = 0.seconds,
    /** Engine time offset before a leading transition trim is projected into [timeOffset]. */
    val rawTimeOffset: Duration = timeOffset,
    val allowsTrimming: Boolean = false,
    val allowsSelecting: Boolean = true,
    /**
     * Whether the clip is locked in place.
     *
     * Always `false` for now; engine-backed lock support will be implemented in the future.
     */
    val isLocked: Boolean = false,
    val trimOffset: Duration = 0.seconds,
    val isMuted: Boolean = false,
    val isLooping: Boolean = false,
    val volume: Float = 1.0f,
    val isInBackgroundTrack: Boolean = false,
    val hasLoaded: Boolean = false,
    val hasAnimation: Boolean = false,
    val isLiveBufferRecording: Boolean = false,
    val leadingTransitionSeamSize: Dp? = null,
    val transitionTrimLead: Duration = ZERO,
    val transitionTrimTail: Duration = ZERO,
) {
    val effectiveFootageDuration: Duration?
        get() = footageDuration?.let {
            if (playbackSpeed > 0f) {
                it / playbackSpeed.toDouble()
            } else {
                it
            }
        }
}

enum class ClipType {
    Audio,
    Image,
    Shape,
    Sticker,
    Text,
    Video,
    Group,
}
