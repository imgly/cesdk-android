package ly.img.editor.core.component.data

import androidx.compose.runtime.Immutable
import ly.img.editor.core.component.Timeline

/**
 * Controls the height of the [Timeline], expressed in track rows rather than pixels.
 */
@Immutable
sealed interface TimelineHeight {
    /**
     * The timeline auto-resizes to fit its tracks, growing to at most [maximumTracks] tracks tall.
     * This is the default and reproduces the standard timeline.
     */
    @Immutable
    data class Dynamic(
        val maximumTracks: Int = 3,
    ) : TimelineHeight

    /**
     * The timeline uses a fixed height sized to show exactly [tracks] tracks, without auto-resizing.
     *
     * [tracks] counts overlay tracks above the background track, so 0 still shows the background
     * track. Negative values are clamped to 0.
     */
    @Immutable
    data class Fixed(
        val tracks: Int,
    ) : TimelineHeight
}
