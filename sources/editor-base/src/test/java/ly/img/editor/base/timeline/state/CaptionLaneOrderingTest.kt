package ly.img.editor.base.timeline.state

import ly.img.editor.base.timeline.clip.Clip
import ly.img.editor.base.timeline.clip.ClipType
import ly.img.editor.base.timeline.track.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import kotlin.time.Duration.Companion.seconds

/**
 * The caption lane must be the topmost row. Drag & drop rejects a foreign clip by testing the
 * pointer against the lane's bottom edge alone, which only means "inside the lane" while nothing
 * is drawn above it — so the ordering is a correctness invariant, not a cosmetic one.
 */
class CaptionLaneOrderingTest {
    @Test
    fun `the caption lane stays topmost when a foreground track is added after it`() {
        val dataSource = TimelineDataSource()
        val lane = Track.caption(engineTrackId = 100)
        dataSource.addCaptionTrack(lane)
        dataSource.addTrack(Track.engine(engineTrackId = 200))
        dataSource.addTrack(Track.standalone(clipBlock = 300))

        assertSame(lane, dataSource.tracks.first())
        assertSame(lane, dataSource.captionTrack)
    }

    @Test
    fun `a second caption lane replaces the first instead of stacking`() {
        // An import over existing captions leaves both caption tracks on the page while the first caption is
        // styled, and that styling suspends — so a rebuild can see two of them.
        val dataSource = TimelineDataSource()
        dataSource.addCaptionTrack(Track.caption(engineTrackId = 100))
        val replacement = Track.caption(engineTrackId = 200)
        dataSource.addCaptionTrack(replacement)

        assertEquals(1, dataSource.tracks.count { it.isCaptionTrack })
        assertSame(replacement, dataSource.tracks.first())
        assertSame(replacement, dataSource.captionTrack)
    }

    @Test
    fun `a replaced caption lane still keeps foreground tracks below it`() {
        val dataSource = TimelineDataSource()
        dataSource.addCaptionTrack(Track.caption(engineTrackId = 100))
        val foreground = Track.engine(engineTrackId = 200)
        dataSource.addTrack(foreground)
        val replacement = Track.caption(engineTrackId = 300)
        dataSource.addCaptionTrack(replacement)

        // Never sandwiched: the foreground row stays directly below the single lane.
        assertSame(replacement, dataSource.tracks[0])
        assertSame(foreground, dataSource.tracks[1])
        assertEquals(2, dataSource.tracks.size)
    }

    @Test
    fun `foreground tracks still stack newest-first below the lane`() {
        val dataSource = TimelineDataSource()
        dataSource.addCaptionTrack(Track.caption(engineTrackId = 100))
        val first = Track.engine(engineTrackId = 200)
        val second = Track.engine(engineTrackId = 300)
        dataSource.addTrack(first)
        dataSource.addTrack(second)

        // Ordering among ordinary rows is unchanged — the most recent one leads.
        assertSame(second, dataSource.tracks[1])
        assertSame(first, dataSource.tracks[2])
    }

    @Test
    fun `audio tracks stay at the bottom`() {
        val dataSource = TimelineDataSource()
        dataSource.addCaptionTrack(Track.caption(engineTrackId = 100))
        dataSource.addTrack(Track.engine(engineTrackId = 200))
        val audio = Track.engine(engineTrackId = 300)
        dataSource.addAudioTrack(audio)

        assertSame(audio, dataSource.tracks.last())
    }

    @Test
    fun `there is no caption lane when the scene has no captions`() {
        val dataSource = TimelineDataSource()
        dataSource.addTrack(Track.engine(engineTrackId = 200))

        assertNull(dataSource.captionTrack)
    }

    @Test
    fun `captions do not stretch the timeline past the footage they annotate`() {
        val dataSource = TimelineDataSource()
        val lane = Track.caption(engineTrackId = 100)
        lane.clips.add(Clip(id = 1, clipType = ClipType.Caption, timeOffset = 20.seconds, duration = 5.seconds))
        dataSource.addCaptionTrack(lane)
        val video = Track.engine(engineTrackId = 200)
        video.clips.add(Clip(id = 2, clipType = ClipType.Video, timeOffset = 0.seconds, duration = 8.seconds))
        dataSource.addTrack(video)

        // A caption running to 25s must not grow the ruler past the 8s of video it describes.
        assertEquals(8.seconds, dataSource.maxClipEnd())
    }
}
