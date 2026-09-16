package ly.img.editor.base.timeline.state

import ly.img.editor.base.timeline.clip.Clip
import ly.img.editor.base.timeline.clip.ClipType
import ly.img.editor.base.timeline.track.Track
import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * A caption is trimmed inside the gap its neighbours leave, and the gaps themselves are authored
 * content — the ordinary foreground walk, which lets a clip claim every unlocked neighbour's
 * room, would erase them.
 */
class CaptionTrimBoundsTest {
    @Test
    fun `the leading edge stops at the previous caption's end`() {
        // [0,2) … [5,8) … : the middle caption may grow left by 3s, to a total of 6s.
        val bounds = boundsForMiddleCaption(
            previous = caption(id = 1, offset = 0.seconds, duration = 2.seconds),
            pivot = caption(id = 2, offset = 5.seconds, duration = 3.seconds),
            next = caption(id = 3, offset = 12.seconds, duration = 2.seconds),
        )
        assertEquals(6.seconds, bounds.leadingMax)
    }

    @Test
    fun `the trailing edge stops at the next caption's start`() {
        val bounds = boundsForMiddleCaption(
            previous = caption(id = 1, offset = 0.seconds, duration = 2.seconds),
            pivot = caption(id = 2, offset = 5.seconds, duration = 3.seconds),
            next = caption(id = 3, offset = 12.seconds, duration = 2.seconds),
        )
        // From the pivot's own start at 5s out to the next start at 12s.
        assertEquals(7.seconds, bounds.trailingMax)
    }

    @Test
    fun `a gap two captions away is not claimed`() {
        // The ordinary walk accumulates every unlocked predecessor's duration, which would let
        // this caption swallow the first one as well. Only the immediate neighbour counts.
        val dataSource = TimelineDataSource()
        val track = Track.caption(engineTrackId = 100)
        val pivot = caption(id = 3, offset = 10.seconds, duration = 2.seconds)
        track.clips.addAll(
            listOf(
                caption(id = 1, offset = 0.seconds, duration = 1.seconds),
                caption(id = 2, offset = 8.seconds, duration = 1.seconds),
                pivot,
            ),
        )
        dataSource.addCaptionTrack(track)

        // Room is 10s − 9s = 1s, so the total may reach 3s — not 10s+ from packing everything left.
        assertEquals(3.seconds, dataSource.trimBounds(pivot).leadingMax)
    }

    @Test
    fun `the last caption may grow without bound`() {
        val dataSource = TimelineDataSource()
        val track = Track.caption(engineTrackId = 100)
        val pivot = caption(id = 2, offset = 5.seconds, duration = 2.seconds)
        track.clips.addAll(listOf(caption(id = 1, offset = 0.seconds, duration = 2.seconds), pivot))
        dataSource.addCaptionTrack(track)

        // Captions legitimately outlast the footage they annotate, so nothing caps the tail.
        assertEquals(Duration.INFINITE, dataSource.trimBounds(pivot).trailingMax)
    }

    @Test
    fun `overlapping imported cues yield a pinned bound, never an inverted one`() {
        // SRT and VTT files do contain overlapping cues. An unguarded clamp would go negative
        // here and let the handle jump instead of holding still.
        val dataSource = TimelineDataSource()
        val track = Track.caption(engineTrackId = 100)
        val pivot = caption(id = 2, offset = 1.seconds, duration = 3.seconds)
        track.clips.addAll(
            listOf(
                caption(id = 1, offset = 0.seconds, duration = 5.seconds),
                pivot,
                caption(id = 3, offset = 2.seconds, duration = 5.seconds),
            ),
        )
        dataSource.addCaptionTrack(track)

        val bounds = dataSource.trimBounds(pivot)
        assertEquals(pivot.duration, bounds.leadingMax)
        assertEquals(pivot.duration, bounds.trailingMax)
    }

    @Test
    fun `a caption trims against a sub-second floor`() {
        // Spoken lines routinely last well under a second, so the generic one-second floor would
        // refuse timings that SRT/VTT files and real speech produce.
        assertEquals(100.milliseconds, TimelineConfiguration.minDuration(ClipType.Caption))
        assertEquals(1.seconds, TimelineConfiguration.minDuration(ClipType.Video))
        assertEquals(1.seconds, TimelineConfiguration.minDuration(ClipType.Text))
    }

    @Test
    fun `a lone caption is unbounded`() {
        val dataSource = TimelineDataSource()
        val track = Track.caption(engineTrackId = 100)
        val pivot = caption(id = 1, offset = 3.seconds, duration = 2.seconds)
        track.clips.add(pivot)
        dataSource.addCaptionTrack(track)

        val bounds = dataSource.trimBounds(pivot)
        assertEquals(Duration.INFINITE, bounds.leadingMax)
        assertEquals(Duration.INFINITE, bounds.trailingMax)
    }

    @Test
    fun `an ordinary foreground track still packs against its neighbours`() {
        // Regression guard: the caption branch must not change the shared walk. Here the pivot
        // can claim its predecessor's whole duration, which is exactly what captions must not do.
        val dataSource = TimelineDataSource()
        val track = Track.engine(engineTrackId = 200)
        val pivot = Clip(id = 2, clipType = ClipType.Video, timeOffset = 5.seconds, duration = 3.seconds)
        track.clips.addAll(
            listOf(
                Clip(id = 1, clipType = ClipType.Video, timeOffset = 0.seconds, duration = 2.seconds),
                pivot,
            ),
        )
        dataSource.addTrack(track)

        // Predecessor is pulled to 0, so the pivot may start at 2s: a total of 3s + 3s = 6s.
        assertEquals(6.seconds, dataSource.trimBounds(pivot).leadingMax)
    }

    private fun boundsForMiddleCaption(
        previous: Clip,
        pivot: Clip,
        next: Clip,
    ): TrimBounds {
        val dataSource = TimelineDataSource()
        val track = Track.caption(engineTrackId = 100)
        track.clips.addAll(listOf(previous, pivot, next))
        dataSource.addCaptionTrack(track)
        return dataSource.trimBounds(pivot)
    }

    private fun caption(
        id: Int,
        offset: Duration,
        duration: Duration,
    ) = Clip(id = id, clipType = ClipType.Caption, timeOffset = offset, duration = duration)
}
