package ly.img.editor.plugin.autoCaptions

import ly.img.editor.plugin.autoCaptions.AutoCaptionsGenerator.Source
import ly.img.editor.plugin.autoCaptions.AutoCaptionsGenerator.TranscribedBlock
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Covers the pure timeline mapping — per-block offset shift, trim-window clipping and the cross-block merge-sort.
 */
class TimelineCuesTest {
    private fun block(
        cues: List<SubtitleCue>,
        timeOffset: Double,
        windowStart: Double = 0.0,
        windowEnd: Double = Double.POSITIVE_INFINITY,
        speed: Double = 1.0,
        source: Source = Source.Video,
    ) = TranscribedBlock(
        srt = Srt.serialize(cues),
        source = source,
        timeOffset = timeOffset,
        windowStart = windowStart,
        windowEnd = windowEnd,
        speed = speed,
    )

    @Test
    fun `empty input produces no cues`() {
        assertEquals(emptyList<SubtitleCue>(), AutoCaptionsGenerator.timelineCues(emptyList()))
    }

    @Test
    fun `a zero offset over the full window is the identity`() {
        val cues = listOf(SubtitleCue(1.0, 2.0, "a"), SubtitleCue(3.0, 4.0, "b"))
        assertEquals(cues, AutoCaptionsGenerator.timelineCues(listOf(block(cues, timeOffset = 0.0))))
    }

    @Test
    fun `an offset shifts both endpoints`() {
        val cues = listOf(SubtitleCue(1.0, 2.5, "a"))
        assertEquals(
            listOf(SubtitleCue(11.0, 12.5, "a")),
            AutoCaptionsGenerator.timelineCues(listOf(block(cues, timeOffset = 10.0))),
        )
    }

    @Test
    fun `two blocks are merged and sorted by start`() {
        // The second block sits earlier on the timeline but is passed second — blocks transcribe in parallel and
        // finish in nondeterministic order, so the merge has to re-order by start.
        val late = block(listOf(SubtitleCue(0.0, 1.0, "late")), timeOffset = 100.0)
        val early = block(listOf(SubtitleCue(0.0, 1.0, "early")), timeOffset = 5.0)
        assertEquals(
            listOf(SubtitleCue(5.0, 6.0, "early"), SubtitleCue(100.0, 101.0, "late")),
            AutoCaptionsGenerator.timelineCues(listOf(late, early)),
        )
    }

    @Test
    fun `the trim window drops cues outside it and rebases the rest`() {
        // A clip trimmed to source [2, 5) placed at timeline 10: source [1, 1.5) is entirely before the in-point,
        // source [6, 6.5) entirely past the out-point, and source 3 maps to 10 + (3 - 2) = 11.
        val cues = listOf(
            SubtitleCue(1.0, 1.5, "before"),
            SubtitleCue(3.0, 3.5, "inside"),
            SubtitleCue(6.0, 6.5, "after"),
        )
        assertEquals(
            listOf(SubtitleCue(11.0, 11.5, "inside")),
            AutoCaptionsGenerator.timelineCues(
                listOf(block(cues, timeOffset = 10.0, windowStart = 2.0, windowEnd = 5.0)),
            ),
        )
    }

    @Test
    fun `cues straddling the trim window are kept and clamped to both edges`() {
        // A cue half outside the window is still half spoken, so it is clamped rather than dropped. Window [2, 5)
        // at timeline 10: [1, 3) clamps to start at the in-point, [4, 6) clamps to end at the out-point.
        val cues = listOf(
            SubtitleCue(1.0, 3.0, "straddles-start"),
            SubtitleCue(4.0, 6.0, "straddles-end"),
        )
        assertEquals(
            listOf(SubtitleCue(10.0, 11.0, "straddles-start"), SubtitleCue(12.0, 13.0, "straddles-end")),
            AutoCaptionsGenerator.timelineCues(
                listOf(block(cues, timeOffset = 10.0, windowStart = 2.0, windowEnd = 5.0)),
            ),
        )
    }

    @Test
    fun `the trim window boundaries are half open`() {
        // A cue touching the window only at an edge contributes no audible time and is dropped: [1, 2) ends
        // exactly at the in-point, [5, 6) starts exactly at the out-point.
        val cues = listOf(
            SubtitleCue(1.0, 2.0, "ends-at-start"),
            SubtitleCue(2.0, 2.5, "at-start"),
            SubtitleCue(5.0, 5.5, "at-end"),
        )
        assertEquals(
            listOf(SubtitleCue(10.0, 10.5, "at-start")),
            AutoCaptionsGenerator.timelineCues(
                listOf(block(cues, timeOffset = 10.0, windowStart = 2.0, windowEnd = 5.0)),
            ),
        )
    }

    @Test
    fun `playback speed rebases cues and keeps the whole clip`() {
        // An untrimmed 20s clip at 2x at timeline 0. The engine reports the window in timeline seconds (20 / 2)
        // while the transcript covers all 20s of source, so every cue must survive at half its source time.
        val cues = listOf(SubtitleCue(5.0, 6.0, "first half"), SubtitleCue(15.0, 16.0, "second half"))
        assertEquals(
            listOf(SubtitleCue(2.5, 3.0, "first half"), SubtitleCue(7.5, 8.0, "second half")),
            AutoCaptionsGenerator.timelineCues(
                listOf(block(cues, timeOffset = 0.0, windowEnd = 10.0, speed = 2.0)),
            ),
        )
    }

    @Test
    fun `playback speed scales the trim window and the cue durations`() {
        // Source [4, 12) at 2x is reported as trim offset 2 and length 4, placed at timeline 10. The source-2 cue
        // is before the in-point; source [8, 10) maps to 10 + (8 - 4) / 2 = 12 and is on screen for 1s, not 2s.
        val cues = listOf(SubtitleCue(2.0, 3.0, "before"), SubtitleCue(8.0, 10.0, "inside"))
        assertEquals(
            listOf(SubtitleCue(12.0, 13.0, "inside")),
            AutoCaptionsGenerator.timelineCues(
                listOf(block(cues, timeOffset = 10.0, windowStart = 2.0, windowEnd = 6.0, speed = 2.0)),
            ),
        )
    }

    @Test
    fun `a playback speed of one leaves cue timings untouched`() {
        val cues = listOf(SubtitleCue(5.0, 6.0, "first half"), SubtitleCue(15.0, 16.0, "second half"))
        assertEquals(
            cues,
            AutoCaptionsGenerator.timelineCues(
                listOf(block(cues, timeOffset = 0.0, windowEnd = 20.0, speed = 1.0)),
            ),
        )
    }

    // region Source ranking

    @Test
    fun `a voiceover suppresses the video cues it speaks over`() {
        // A 60s video with its own audio and a voiceover recorded over seconds 10-20, narrating for the first
        // second of it. Only the second the narrator actually speaks is taken from the video.
        val video = block(
            listOf(
                SubtitleCue(1.0, 2.0, "video before"),
                SubtitleCue(10.2, 10.8, "video under the narration"),
                SubtitleCue(30.0, 31.0, "video after"),
            ),
            timeOffset = 0.0,
        )
        val voiceover = block(listOf(SubtitleCue(0.0, 1.0, "narration")), timeOffset = 10.0, source = Source.Voiceover)
        assertEquals(
            listOf(
                SubtitleCue(1.0, 2.0, "video before"),
                SubtitleCue(10.0, 11.0, "narration"),
                SubtitleCue(30.0, 31.0, "video after"),
            ),
            AutoCaptionsGenerator.timelineCues(listOf(video, voiceover)),
        )
    }

    @Test
    fun `a voiceover gives up the stretches it is silent for`() {
        // The reported gap. A voiceover clip runs from 10 to 40 but the narrator only speaks for its first second,
        // so the video keeps everything it says in the remaining 29 — claiming the whole clip would leave a long
        // stretch with no captions at all even though only one source is talking.
        val video = block(
            listOf(
                SubtitleCue(12.0, 13.0, "video while the narrator is silent"),
                SubtitleCue(25.0, 26.0, "video later in the same clip"),
            ),
            timeOffset = 0.0,
        )
        val voiceover = block(listOf(SubtitleCue(0.0, 1.0, "narration")), timeOffset = 10.0, source = Source.Voiceover)
        assertEquals(
            listOf(
                SubtitleCue(10.0, 11.0, "narration"),
                SubtitleCue(12.0, 13.0, "video while the narrator is silent"),
                SubtitleCue(25.0, 26.0, "video later in the same clip"),
            ),
            AutoCaptionsGenerator.timelineCues(listOf(video, voiceover)),
        )
    }

    @Test
    fun `a short pause in narration stays claimed`() {
        // The beat between two sentences (1s, inside the tolerance) must not let a video caption flash in and
        // straight back out — the passage of narration is claimed as one run.
        val video = block(listOf(SubtitleCue(11.2, 11.8, "video mid-pause")), timeOffset = 0.0)
        val voiceover = block(
            listOf(SubtitleCue(0.0, 1.0, "first sentence"), SubtitleCue(2.0, 3.0, "second sentence")),
            timeOffset = 10.0,
            source = Source.Voiceover,
        )
        assertEquals(
            listOf(SubtitleCue(10.0, 11.0, "first sentence"), SubtitleCue(12.0, 13.0, "second sentence")),
            AutoCaptionsGenerator.timelineCues(listOf(video, voiceover)),
        )
    }

    @Test
    fun `a long silence between narration is given back`() {
        // The counterpart: a 9s silence is well past the tolerance, so it splits into two runs and the video
        // captions the gap between them.
        val video = block(listOf(SubtitleCue(15.0, 16.0, "video in the silence")), timeOffset = 0.0)
        val voiceover = block(
            listOf(SubtitleCue(0.0, 1.0, "first sentence"), SubtitleCue(10.0, 11.0, "second sentence")),
            timeOffset = 10.0,
            source = Source.Voiceover,
        )
        assertEquals(
            listOf(
                SubtitleCue(10.0, 11.0, "first sentence"),
                SubtitleCue(15.0, 16.0, "video in the silence"),
                SubtitleCue(20.0, 21.0, "second sentence"),
            ),
            AutoCaptionsGenerator.timelineCues(listOf(video, voiceover)),
        )
    }

    @Test
    fun `ranking is independent of the order blocks finish transcribing`() {
        // Blocks transcribe in parallel, so the input order is nondeterministic.
        val video = block(listOf(SubtitleCue(0.0, 5.0, "video")), timeOffset = 0.0)
        val voiceover = block(listOf(SubtitleCue(0.0, 5.0, "narration")), timeOffset = 0.0, source = Source.Voiceover)
        val expected = listOf(SubtitleCue(0.0, 5.0, "narration"))
        assertEquals(expected, AutoCaptionsGenerator.timelineCues(listOf(video, voiceover)))
        assertEquals(expected, AutoCaptionsGenerator.timelineCues(listOf(voiceover, video)))
    }

    @Test
    fun `a silent voiceover suppresses nothing`() {
        // Otherwise recording silence over a clip would wipe that clip's captions.
        val video = block(listOf(SubtitleCue(2.0, 3.0, "video")), timeOffset = 0.0)
        val silentVoiceover = block(emptyList(), timeOffset = 0.0, source = Source.Voiceover)
        assertEquals(
            listOf(SubtitleCue(2.0, 3.0, "video")),
            AutoCaptionsGenerator.timelineCues(listOf(video, silentVoiceover)),
        )
    }

    @Test
    fun `a video outranks a music track playing under it`() {
        // A song's transcribed lyrics must not displace the dialogue of the clip they play under.
        val music = block(listOf(SubtitleCue(1.0, 4.0, "lyrics")), timeOffset = 0.0, source = Source.Audio)
        val video = block(listOf(SubtitleCue(2.0, 3.0, "dialogue")), timeOffset = 0.0)
        assertEquals(
            listOf(SubtitleCue(2.0, 3.0, "dialogue")),
            AutoCaptionsGenerator.timelineCues(listOf(music, video)),
        )
    }

    @Test
    fun `sources that do not overlap all keep their cues`() {
        // Ranking only resolves collisions.
        val voiceover = block(listOf(SubtitleCue(0.0, 1.0, "narration")), timeOffset = 0.0, source = Source.Voiceover)
        val video = block(listOf(SubtitleCue(0.0, 1.0, "dialogue")), timeOffset = 20.0)
        assertEquals(
            listOf(SubtitleCue(0.0, 1.0, "narration"), SubtitleCue(20.0, 21.0, "dialogue")),
            AutoCaptionsGenerator.timelineCues(listOf(video, voiceover)),
        )
    }

    @Test
    fun `two overlapping clips of equal rank are resolved by timeline position`() {
        // The earlier clip wins the moments it is speaking; the later one still captions what it says outside
        // them, both inside the overlap and past it.
        val first = block(listOf(SubtitleCue(4.0, 6.0, "first")), timeOffset = 0.0)
        val second = block(
            listOf(
                SubtitleCue(0.0, 1.0, "overlapped"),
                SubtitleCue(2.0, 3.0, "inside the overlap but in a gap"),
                SubtitleCue(7.0, 8.0, "past the first clip"),
            ),
            timeOffset = 5.0,
        )
        assertEquals(
            listOf(
                SubtitleCue(4.0, 6.0, "first"),
                SubtitleCue(7.0, 8.0, "inside the overlap but in a gap"),
                SubtitleCue(12.0, 13.0, "past the first clip"),
            ),
            AutoCaptionsGenerator.timelineCues(listOf(second, first)),
        )
    }

    // endregion
}
