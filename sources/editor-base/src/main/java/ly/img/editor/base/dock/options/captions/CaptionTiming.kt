package ly.img.editor.base.dock.options.captions

import java.text.BreakIterator

/** The timing arithmetic behind splitting and merging captions. Kept out of [CaptionsEngine] so it can be tested without a scene. */
internal object CaptionTiming {
    /**
     * Whether [offset] falls between two grapheme clusters of [text], and so is safe to cut at.
     *
     * A UTF-16 offset can land inside a single user-perceived character — a surrogate pair, a combining mark, a
     * ZWJ emoji sequence, a regional-indicator flag — and splitting there corrupts the glyph on both sides.
     */
    fun isGraphemeBoundary(
        text: String,
        offset: Int,
    ): Boolean {
        if (offset <= 0 || offset >= text.length) return offset == 0 || offset == text.length
        return BreakIterator.getCharacterInstance().apply { setText(text) }.isBoundary(offset)
    }

    /**
     * The text either side of a UTF-16 offset, or `null` when the offset doesn't divide the text in two — at
     * either end, out of range, or mid-grapheme, where cutting would corrupt the glyph on both sides.
     */
    fun splitTexts(
        text: String,
        offset: Int,
    ): Pair<String, String>? {
        if (offset !in text.indices) return null
        if (!isGraphemeBoundary(text, offset)) return null
        val left = text.substring(0, offset)
        val right = text.substring(offset)
        if (left.isEmpty() || right.isEmpty()) return null
        return left to right
    }

    /**
     * How a caption's duration divides when its text is cut at [leftLength] (UTF-16 units).
     *
     * The tail takes the *remainder* rather than its own proportion, so the two always sum back to [totalDuration] exactly.
     */
    fun splitDurations(
        totalDuration: Double,
        leftLength: Int,
        totalLength: Int,
    ): Pair<Double, Double> {
        if (totalLength <= 0) return 0.0 to totalDuration.coerceAtLeast(0.0)
        val left = (totalDuration * leftLength / totalLength).coerceAtLeast(0.0)
        val right = (totalDuration - left).coerceAtLeast(0.0)
        return left to right
    }

    /**
     * Whether a cut at [leftDuration] leaves both halves at or above [MINIMUM_CAPTION_DURATION_SECONDS].
     *
     * A split that doesn't clear it is refused rather than nudged, so the pair keeps tiling exactly the
     * range the original occupied and the cut stays where the user put it.
     */
    fun splitClearsFloor(
        totalDuration: Double,
        leftDuration: Double,
    ): Boolean = leftDuration >= MINIMUM_CAPTION_DURATION_SECONDS &&
        totalDuration - leftDuration >= MINIMUM_CAPTION_DURATION_SECONDS

    /**
     * The UTF-16 offset the playhead sits at — the inverse of [splitDurations], so splitting there divides the
     * duration exactly where the playhead is. Clamped into `0..textLength`.
     */
    fun splitOffsetAtTime(
        textLength: Int,
        start: Double,
        duration: Double,
        playhead: Double,
    ): Int {
        if (textLength <= 0 || duration <= 0.0) return 0
        val played = ((playhead - start) / duration).coerceIn(0.0, 1.0)
        return Math.round(textLength * played).toInt().coerceIn(0, textLength)
    }

    /**
     * The offset of the word gap closest to [nearestTo], searching outwards in both directions.
     *
     * A gap is the position immediately *after* a space, so the trailing caption never starts with one. Text
     * with no gaps falls back to the nearest interior grapheme boundary. Only a time-derived split is snapped:
     * a caret split stays where the user put it.
     *
     * @return an offset that divides the text in two — never one a split would refuse — or `null` when nothing
     * does.
     */
    fun wordBoundary(
        text: String,
        nearestTo: Int,
    ): Int? {
        // A combining mark or ZWJ after the space keeps its cluster open.
        fun isGap(offset: Int) = offset > 0 &&
            offset < text.length &&
            text[offset - 1] == ' ' &&
            isGraphemeBoundary(text, offset)
        if (isGap(nearestTo)) return nearestTo
        for (distance in 1..text.length) {
            // The earlier gap wins a tie, landing on the word the playhead has passed.
            if (isGap(nearestTo - distance)) return nearestTo - distance
            if (isGap(nearestTo + distance)) return nearestTo + distance
        }
        return nearestInteriorGraphemeBoundary(text, nearestTo)
    }

    /** Whether [text] has the two user-perceived characters a split needs. */
    fun isDivisible(text: String): Boolean = nearestInteriorGraphemeBoundary(text, offset = text.length / 2) != null

    /**
     * The grapheme boundary closest to [offset] that leaves text on *both* sides — the ends are excluded
     * because a split there has an empty half, which the split refuses.
     */
    private fun nearestInteriorGraphemeBoundary(
        text: String,
        offset: Int,
    ): Int? {
        fun isInterior(candidate: Int) = candidate in 1 until text.length && isGraphemeBoundary(text, candidate)
        val clamped = offset.coerceIn(0, text.length)
        if (isInterior(clamped)) return clamped
        for (distance in 1..text.length) {
            if (isInterior(clamped - distance)) return clamped - distance
            if (isInterior(clamped + distance)) return clamped + distance
        }
        return null
    }

    /**
     * How long a caption inserted at [start] may be: the gap up to [nextStart], capped at the default length
     * and floored at the timeline's minimum.
     *
     * Fitting the gap is what keeps the rest of the track still — the caption track preserves gaps but ripples
     * anything that *overlaps* forward, so a fixed default length dropped between two cues would shove every
     * later caption onwards.
     *
     * Exactly contiguous siblings have no gap to fit, so they fall back to the default and accept that ripple:
     * the alternative is a zero-length caption the timeline will not let the user grab back.
     */
    fun insertedDuration(
        start: Double,
        nextStart: Double?,
    ): Double {
        if (nextStart == null || nextStart <= start) return DEFAULT_CAPTION_DURATION_SECONDS
        return (nextStart - start).coerceIn(MINIMUM_CAPTION_DURATION_SECONDS, DEFAULT_CAPTION_DURATION_SECONDS)
    }

    /**
     * The duration a merged caption spans: from the previous caption's start to the current one's end.
     *
     * `max` keeps the previous duration when the current caption is nested inside it, so the merged caption never shrinks.
     */
    fun mergedDuration(
        previousStart: Double,
        previousDuration: Double,
        currentStart: Double,
        currentDuration: Double,
    ): Double = maxOf(previousDuration, (currentStart + currentDuration) - previousStart)

    /** The text of a merged caption: the two joined by a single space, with an empty side contributing no separator. */
    fun mergedText(
        previousText: String,
        currentText: String,
    ): String = listOf(previousText, currentText).filter { it.isNotEmpty() }.joinToString(" ")

    /** Where the caret lands when [currentText] is merged into [previousText]: shifted right by the previous text and separator. */
    fun mergedCaret(
        previousText: String,
        currentText: String,
        caretInCurrent: Int,
    ): Int {
        val separator = if (previousText.isEmpty() || currentText.isEmpty()) 0 else 1
        return previousText.length + separator + caretInCurrent
    }
}
