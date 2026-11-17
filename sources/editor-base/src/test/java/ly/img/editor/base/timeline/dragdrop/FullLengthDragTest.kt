package ly.img.editor.base.timeline.dragdrop

import ly.img.editor.base.timeline.clip.Clip
import ly.img.editor.base.timeline.clip.ClipType
import ly.img.editor.base.timeline.state.releaseTransitionTrim
import ly.img.editor.base.timeline.state.withReleasedTrim
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds

/**
 * A dragged clip is drawn at its full duration, and the clips it shares a transition with show the end they share
 * with it at full length. Every clip here is 5s long in the engine, and each transition is 2s, so it trims 1s from
 * each of its clips. Engine: P 0s → D 3s → N 6s, then E at 11s without a transition. Drawn: P 0s-4s, D 4s-7s,
 * N 7s-11s, E 11s-16s.
 */
class FullLengthDragTest {
    private val p = clip(id = 1, rawOffset = 0.seconds, tail = 1.seconds)
    private val d = clip(id = 2, rawOffset = 3.seconds, lead = 1.seconds, tail = 1.seconds)
    private val n = clip(id = 3, rawOffset = 6.seconds, lead = 1.seconds)
    private val e = clip(id = 4, rawOffset = 11.seconds)

    private val partners = releaseTransitionTrim(listOf(p, d, n, e), d.id)
    private val siblings = listOf(p, n, e).map { clip -> partners[clip.id]?.let(clip::withReleasedTrim) ?: clip }

    @Test
    fun `the partners of a dragged clip show the ends they share with it at full length`() {
        assertEquals(setOf(p.id, n.id), partners.keys)
        val previous = p.withReleasedTrim(partners.getValue(p.id))
        assertEquals(0.seconds, previous.timeOffset)
        assertEquals(5.seconds, previous.duration)
        val next = n.withReleasedTrim(partners.getValue(n.id))
        assertEquals(6.seconds, next.timeOffset)
        assertEquals(11.seconds, next.timeOffset + next.duration)
        assertEquals(ZERO, next.transitionTrimLead)
    }

    @Test
    fun `a partner that lost its transition after the drop shows its own length`() {
        // The drop removed P's transition, so the timeline holds P untrimmed while the preview still releases it.
        val untrimmed = p.copy(duration = 5.seconds, transitionTrimTail = ZERO)

        val shown = untrimmed.withReleasedTrim(partners.getValue(p.id))

        assertEquals(0.seconds, shown.timeOffset)
        assertEquals(5.seconds, shown.duration)
    }

    @Test
    fun `a dragged clip takes its slot and pushes its neighbours at its full duration`() {
        val slot = checkNotNull(
            computeTrackDropSlot(
                sortedSiblings = siblings,
                insertIndex = 1,
                desiredStart = d.timeOffset,
                dragged = d,
                isBackgroundTarget = false,
                isBackgroundSource = false,
            ),
        )

        assertEquals(DropSlot(insertIndex = 1, dropStart = 5.seconds, effectiveDuration = 5.seconds), slot)
        assertEquals(
            mapOf(n.id to 10.seconds, e.id to 15.seconds),
            computeDropOverrides(
                sortedSiblings = siblings,
                insertIndex = 1,
                dropStart = slot.dropStart,
                draggedDuration = slot.effectiveDuration,
            ),
        )
    }

    @Test
    fun `a drop against the full end of the previous partner is the drop without moving`() {
        // The drawn start, 4s, lies inside the previous partner's full length, so the slot pins at its end.
        val target = DropTarget.ExistingTrack(trackId = TRACK, insertIndex = 1, timeOffset = 5.seconds)

        assertTrue(isUnmovedDrop(target, TRACK, siblings, d, isBackgroundTrack = false))
    }

    @Test
    fun `a drop beside the start of the dragged clip is a move`() {
        val target = DropTarget.ExistingTrack(trackId = TRACK, insertIndex = 1, timeOffset = 6.seconds)

        assertFalse(isUnmovedDrop(target, TRACK, siblings, d, isBackgroundTrack = false))
    }

    @Test
    fun `a background clip dropped back into its slot is the drop without moving`() {
        // The background track packs its clips, so the slot starts at the full end of the previous partner.
        val target = DropTarget.ExistingTrack(trackId = BACKGROUND, insertIndex = 1, timeOffset = 5.seconds)

        assertTrue(isUnmovedDrop(target, BACKGROUND, siblings, d, isBackgroundTrack = true))
    }

    private fun clip(
        id: Int,
        rawOffset: Duration,
        lead: Duration = ZERO,
        tail: Duration = ZERO,
    ) = Clip(
        id = id,
        clipType = ClipType.Video,
        timeOffset = rawOffset + lead,
        duration = 5.seconds - lead - tail,
        rawTimeOffset = rawOffset,
        rawDuration = 5.seconds,
        transitionTrimLead = lead,
        transitionTrimTail = tail,
    )

    private companion object {
        const val TRACK = "engine-10"
        const val BACKGROUND = "background"
    }
}
