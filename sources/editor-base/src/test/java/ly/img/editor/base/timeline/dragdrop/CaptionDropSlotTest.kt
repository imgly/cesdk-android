package ly.img.editor.base.timeline.dragdrop

import androidx.compose.ui.geometry.Rect
import ly.img.editor.base.timeline.clip.Clip
import ly.img.editor.base.timeline.clip.ClipType
import ly.img.editor.base.timeline.state.TimelineDataSource
import ly.img.editor.base.timeline.track.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * The caption lane is a closed partition: captions cannot leave it and nothing else may enter.
 * Both directions are enforced here rather than in the UI, so they are covered without a scene.
 */
class CaptionDropSlotTest {
    // region isTypeCompatible

    @Test
    fun `a caption is compatible with the caption lane`() {
        assertTrue(isTypeCompatible(caption(id = 1), captionTrack(caption(id = 2))))
    }

    @Test
    fun `a caption is not compatible with an ordinary foreground track`() {
        assertFalse(isTypeCompatible(caption(id = 1), engineTrack(clip(id = 2, type = ClipType.Video))))
    }

    @Test
    fun `a caption is not compatible with an empty ordinary track`() {
        // An empty track has no example clip to compare against, which used to answer "compatible".
        assertFalse(isTypeCompatible(caption(id = 1), engineTrack()))
    }

    @Test
    fun `a video is not compatible with the caption lane`() {
        val video = clip(id = 1, type = ClipType.Video)
        assertFalse(isTypeCompatible(video, captionTrack(caption(id = 2))))
    }

    @Test
    fun `a video is not compatible with an empty caption lane`() {
        // The empty-track trap in the other direction: the partition is keyed off the track, not
        // its first clip, so an emptied lane still refuses foreign clips.
        assertFalse(isTypeCompatible(clip(id = 1, type = ClipType.Video), captionTrack()))
    }

    @Test
    fun `the audio partition still holds`() {
        val audio = clip(id = 1, type = ClipType.Audio)
        val video = clip(id = 2, type = ClipType.Video)
        assertTrue(isTypeCompatible(audio, engineTrack(clip(id = 3, type = ClipType.Audio))))
        assertFalse(isTypeCompatible(audio, engineTrack(video)))
        assertTrue(isTypeCompatible(video, engineTrack(clip(id = 4, type = ClipType.Video))))
    }

    // endregion
    // region isBackgroundCompatible

    @Test
    fun `a caption may not be dropped into the background track`() {
        assertFalse(isBackgroundCompatible(ClipType.Caption))
        assertFalse(isBackgroundCompatible(ClipType.Audio))
        assertTrue(isBackgroundCompatible(ClipType.Video))
        assertTrue(isBackgroundCompatible(ClipType.Image))
    }

    // endregion
    // region resolveDropZone — captions stay in the lane

    @Test
    fun `a caption resolves to the lane wherever the pointer goes`() {
        val lane = captionTrack(caption(id = 1))
        val candidates = listOf(candidate(lane, index = 0, top = 0f, bottom = 40f))

        for (pointerY in listOf(-500f, 20f, 500f, 5000f)) {
            val zone = resolveDropZone(
                pointerY = pointerY,
                sourceTrackId = lane.id,
                draggedClipType = ClipType.Caption,
                backgroundTrack = Track.background(),
                backgroundFrame = Rect(0f, 300f, 1000f, 360f),
                captionTrack = lane,
                captionFrame = Rect(0f, 0f, 1000f, 40f),
                sortedCandidates = candidates,
            )
            assertEquals("pointerY=$pointerY", DropZone.ExistingTrack(lane), zone)
        }
    }

    @Test
    fun `a caption never spawns a new track`() {
        // Without the caption short-circuit, a pointer below the bottommost candidate resolves to
        // `NewTrack`, which would lift the caption out of its lane into a track of its own.
        val lane = captionTrack(caption(id = 1), caption(id = 2))
        val zone = resolveDropZone(
            pointerY = 900f,
            sourceTrackId = lane.id,
            draggedClipType = ClipType.Caption,
            backgroundTrack = Track.background(),
            backgroundFrame = Rect(0f, 1000f, 1000f, 1060f),
            captionTrack = lane,
            captionFrame = Rect(0f, 0f, 1000f, 40f),
            sortedCandidates = listOf(candidate(lane, index = 0, top = 0f, bottom = 40f)),
        )
        assertEquals(DropZone.ExistingTrack(lane), zone)
    }

    @Test
    fun `a caption still resolves when the lane is scrolled out of the viewport`() {
        // Candidates are filtered to the visible viewport and the frame goes unpublished once the
        // row is recycled. Resolving from the candidate list would turn the whole gesture into a
        // silent no-op; the lane is the only possible target, so it is taken directly.
        val lane = captionTrack(caption(id = 1), caption(id = 2))
        val zone = resolveDropZone(
            pointerY = 20f,
            sourceTrackId = lane.id,
            draggedClipType = ClipType.Caption,
            backgroundTrack = Track.background(),
            backgroundFrame = Rect(0f, 300f, 1000f, 360f),
            captionTrack = lane,
            captionFrame = null,
            sortedCandidates = emptyList(),
        )
        assertEquals(DropZone.ExistingTrack(lane), zone)
    }

    @Test
    fun `a caption resolves to nothing when the scene has no lane`() {
        val zone = resolveDropZone(
            pointerY = 20f,
            sourceTrackId = "caption-1",
            draggedClipType = ClipType.Caption,
            backgroundTrack = Track.background(),
            backgroundFrame = Rect(0f, 300f, 1000f, 360f),
            captionTrack = null,
            captionFrame = null,
            sortedCandidates = emptyList(),
        )
        assertNull(zone)
    }

    // endregion
    // region resolveDropZone — foreign clips stay out

    @Test
    fun `a video is rejected anywhere at or above the lane's bottom edge`() {
        val lane = captionTrack(caption(id = 1))
        val videoTrack = engineTrack(clip(id = 9, type = ClipType.Video))
        // Over the lane's own band, and above it. The second case also costs the gap zone that
        // would otherwise open a new row beneath the lane.
        for (pointerY in listOf(-30f, 0f, 20f, 40f)) {
            val zone = resolveDropZone(
                pointerY = pointerY,
                sourceTrackId = videoTrack.id,
                draggedClipType = ClipType.Video,
                backgroundTrack = Track.background(),
                backgroundFrame = Rect(0f, 300f, 1000f, 360f),
                captionTrack = lane,
                captionFrame = Rect(0f, 0f, 1000f, 40f),
                sortedCandidates = listOf(candidate(videoTrack, index = 1, top = 50f, bottom = 90f)),
            )
            assertNull("pointerY=$pointerY", zone)
        }
    }

    @Test
    fun `a video below the lane resolves normally`() {
        val lane = captionTrack(caption(id = 1))
        val videoTrack = engineTrack(clip(id = 9, type = ClipType.Video))
        val zone = resolveDropZone(
            pointerY = 70f,
            sourceTrackId = videoTrack.id,
            draggedClipType = ClipType.Video,
            backgroundTrack = Track.background(),
            backgroundFrame = Rect(0f, 300f, 1000f, 360f),
            captionTrack = lane,
            captionFrame = Rect(0f, 0f, 1000f, 40f),
            sortedCandidates = listOf(candidate(videoTrack, index = 1, top = 50f, bottom = 90f)),
        )
        assertEquals(DropZone.ExistingTrack(videoTrack), zone)
    }

    @Test
    fun `a scene without captions still opens a new track above the topmost row`() {
        // Two clips, so the drag is not a "solo clip into an adjacent gap" visual no-op. This is
        // precisely the zone the caption rejection suppresses; without a lane it must survive.
        val videoTrack = engineTrack(
            clip(id = 9, type = ClipType.Video),
            clip(id = 10, type = ClipType.Video, offset = 5.seconds),
        )
        val zone = resolveDropZone(
            pointerY = 10f,
            sourceTrackId = videoTrack.id,
            draggedClipType = ClipType.Video,
            backgroundTrack = Track.background(),
            backgroundFrame = Rect(0f, 300f, 1000f, 360f),
            captionTrack = null,
            captionFrame = null,
            sortedCandidates = listOf(candidate(videoTrack, index = 0, top = 50f, bottom = 90f)),
        )
        assertEquals(DropZone.NewTrack(insertAt = 0), zone)
    }

    // endregion
    // region computeDropSlot — gaps are preserved

    @Test
    fun `a caption drop is clamped between its neighbours`() {
        // Siblings at [0,2) and [8,10); the dragged 2s caption may sit anywhere in [2,6].
        val siblings = listOf(
            caption(id = 1, offset = 0.seconds, duration = 2.seconds),
            caption(id = 2, offset = 8.seconds, duration = 2.seconds),
        )
        val slot = computeDropSlot(
            sortedSiblings = siblings,
            insertIndex = 1,
            desiredStart = 30.seconds,
            draggedDuration = 2.seconds,
            isLiveBufferRecording = false,
            allowTrimToFit = false,
        )
        assertEquals(6.seconds, slot?.dropStart)
        assertEquals(2.seconds, slot?.effectiveDuration)

        val clampedLeft = computeDropSlot(
            sortedSiblings = siblings,
            insertIndex = 1,
            desiredStart = (-5).seconds,
            draggedDuration = 2.seconds,
            isLiveBufferRecording = false,
            allowTrimToFit = false,
        )
        assertEquals(2.seconds, clampedLeft?.dropStart)
    }

    @Test
    fun `a caption is rejected rather than shortened when the gap is too small`() {
        // A 4s caption dropped into a 2s gap. Trim-to-fit would left-pack it and cut it to 2s,
        // closing an authored silence and retiming a cue the user did not touch.
        val siblings = listOf(
            caption(id = 1, offset = 0.seconds, duration = 2.seconds),
            caption(id = 2, offset = 4.seconds, duration = 2.seconds),
        )
        val slot = computeDropSlot(
            sortedSiblings = siblings,
            insertIndex = 1,
            desiredStart = 2.seconds,
            draggedDuration = 4.seconds,
            isLiveBufferRecording = false,
            allowTrimToFit = false,
        )
        assertNull(slot)
    }

    @Test
    fun `ordinary tracks still place the clip rather than bouncing`() {
        val siblings = listOf(
            clip(id = 1, type = ClipType.Video, offset = 0.seconds, duration = 2.seconds),
            clip(id = 2, type = ClipType.Video, offset = 4.seconds, duration = 2.seconds),
        )
        val slot = computeDropSlot(
            sortedSiblings = siblings,
            insertIndex = 1,
            desiredStart = 2.seconds,
            draggedDuration = 4.seconds,
            isLiveBufferRecording = false,
        )
        // Same too-small gap that the caption lane rejects. An ordinary track instead packs the
        // clip against its predecessor and pushes the successor along, keeping its full duration.
        assertEquals(2.seconds, slot?.dropStart)
        assertEquals(4.seconds, slot?.effectiveDuration)
    }

    // endregion
    // region resolveAnchorPageIndex — the pinned lane must not shift the mapping

    /**
     * The lane sits at `tracks[0]` however the engine orders the page's children, so it is
     * excluded from the tracks ↔ pageChildren mapping. Each case drops a new track at
     * `insertAt`, then replays the data source's build order to check it lands there.
     */
    @Test
    fun `a new track lands at the requested row while a caption lane is pinned on top`() {
        // pageChildren: [bg, videoA, captionTrack, videoB] — the lane is not last, which is what
        // a previous new-track drop leaves behind.
        val pageChildren = listOf(BACKGROUND, VIDEO_A, CAPTION_TRACK, VIDEO_B)
        val tracks = listOf(
            Track.caption(engineTrackId = CAPTION_TRACK),
            engineTrack(clip(id = 21, type = ClipType.Video)).copy(id = "engine-$VIDEO_B"),
            engineTrack(clip(id = 22, type = ClipType.Video)).copy(id = "engine-$VIDEO_A"),
        )

        for (insertAt in 1..3) {
            val anchor = resolveAnchorPageIndex(
                dragged = clip(id = 99, type = ClipType.Video),
                tracks = tracks,
                pageChildren = pageChildren,
                insertAt = insertAt,
                isAudioBlock = { false },
                isBackgroundTrack = { it == BACKGROUND },
                isCaptionTrack = { it == CAPTION_TRACK },
            )
            val rebuilt = rebuildTrackOrder(
                pageChildren.toMutableList().apply { add(anchor.coerceIn(0, size), NEW_TRACK) },
            )
            assertEquals("insertAt=$insertAt", insertAt, rebuilt.indexOf(NEW_TRACK))
        }
    }

    @Test
    fun `a new track lands at the requested row when there is no caption lane`() {
        val pageChildren = listOf(BACKGROUND, VIDEO_A, VIDEO_B)
        val tracks = listOf(
            engineTrack(clip(id = 21, type = ClipType.Video)).copy(id = "engine-$VIDEO_B"),
            engineTrack(clip(id = 22, type = ClipType.Video)).copy(id = "engine-$VIDEO_A"),
        )

        for (insertAt in 0..2) {
            val anchor = resolveAnchorPageIndex(
                dragged = clip(id = 99, type = ClipType.Video),
                tracks = tracks,
                pageChildren = pageChildren,
                insertAt = insertAt,
                isAudioBlock = { false },
                isBackgroundTrack = { it == BACKGROUND },
                isCaptionTrack = { false },
            )
            val rebuilt = rebuildTrackOrder(
                pageChildren.toMutableList().apply { add(anchor.coerceIn(0, size), NEW_TRACK) },
            )
            assertEquals("insertAt=$insertAt", insertAt, rebuilt.indexOf(NEW_TRACK))
        }
    }

    // endregion

    /**
     * Replays `TimelineDataSource`'s insertion rules over page children, returning the resulting
     * row order — the background track is excluded because it lives outside `tracks`.
     */
    private fun rebuildTrackOrder(pageChildren: List<Int>): List<Int> {
        val dataSource = TimelineDataSource()
        pageChildren.forEach { child ->
            when (child) {
                BACKGROUND -> Unit
                CAPTION_TRACK -> dataSource.addCaptionTrack(Track.caption(engineTrackId = child))
                else -> dataSource.addTrack(Track.engine(engineTrackId = child))
            }
        }
        return dataSource.tracks.map { checkNotNull(it.engineTrackId) }
    }

    private fun clip(
        id: Int,
        type: ClipType,
        offset: Duration = Duration.ZERO,
        duration: Duration = 2.seconds,
    ) = Clip(id = id, clipType = type, timeOffset = offset, duration = duration)

    private fun caption(
        id: Int,
        offset: Duration = Duration.ZERO,
        duration: Duration = 2.seconds,
    ) = clip(id = id, type = ClipType.Caption, offset = offset, duration = duration)

    private fun captionTrack(vararg clips: Clip) = Track.caption(engineTrackId = 100).also { it.clips.addAll(clips) }

    private fun engineTrack(vararg clips: Clip) = Track.engine(engineTrackId = 200).also { it.clips.addAll(clips) }

    private fun candidate(
        track: Track,
        index: Int,
        top: Float,
        bottom: Float,
    ) = DropCandidate(track = track, tracksIndex = index, frame = Rect(0f, top, 1000f, bottom))

    private companion object {
        const val BACKGROUND = 10
        const val VIDEO_A = 11
        const val CAPTION_TRACK = 12
        const val VIDEO_B = 13
        const val NEW_TRACK = 14
    }
}
