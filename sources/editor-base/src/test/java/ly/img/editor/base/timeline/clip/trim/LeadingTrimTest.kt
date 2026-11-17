package ly.img.editor.base.timeline.clip.trim

import ly.img.editor.base.timeline.clip.Clip
import ly.img.editor.base.timeline.clip.ClipType
import ly.img.editor.base.timeline.state.TransitionTrimCaps
import ly.img.editor.base.ui.BlockEvent
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * The clip is 3s long in the engine from 7s, with a 1s trim offset, and transitions trim 0.5s from each end. The
 * timeline draws it from 7.5s for 2s.
 */
class LeadingTrimTest {
    private val clip = Clip(
        id = 1,
        clipType = ClipType.Video,
        timeOffset = 7.5.seconds,
        duration = 2.seconds,
        rawTimeOffset = 7.seconds,
        rawDuration = 3.seconds,
        trimOffset = 1.seconds,
        transitionTrimLead = 0.5.seconds,
        transitionTrimTail = 0.5.seconds,
    )

    private val caps = TransitionTrimCaps(0.5.seconds, 0.5.seconds)

    @Test
    fun `a leading trim commits the start and duration that the timeline draws`() {
        assertEquals(
            BlockEvent.OnUpdateTrim(trimOffset = 1.5.seconds, timeOffset = 8.seconds, duration = 1.5.seconds),
            leadingTrim(clip, 1.5.seconds, caps),
        )
    }

    @Test
    fun `a leading trim on the background track keeps the start`() {
        assertEquals(
            BlockEvent.OnUpdateTrim(trimOffset = 1.5.seconds, timeOffset = 7.5.seconds, duration = 1.5.seconds),
            leadingTrim(clip.copy(isInBackgroundTrack = true), 1.5.seconds, caps),
        )
    }
}
