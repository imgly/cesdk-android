package ly.img.editor.plugin.autoCaptions

/** A single word with its start and end timestamps, in seconds. */
internal data class TimedWord(
    val text: String,
    val start: Double,
    val end: Double,
)

/**
 * Groups timed words into cues within the line-length and line-count limits, timing each cue from its first word's
 * start to its last word's end.
 */
internal fun cuesFrom(
    words: List<TimedWord>,
    maxLineLength: Int,
    maxLines: Int,
): List<SubtitleCue> {
    val cues = mutableListOf<SubtitleCue>()
    var cueWords = mutableListOf<TimedWord>()
    var lines = mutableListOf<String>()
    var currentLine = ""

    fun flushCue() {
        val first = cueWords.firstOrNull() ?: return
        val last = cueWords.last()
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        cues.add(SubtitleCue(start = first.start, end = last.end, text = lines.joinToString("\n")))
        cueWords = mutableListOf()
        lines = mutableListOf()
        currentLine = ""
    }

    for (word in words) {
        val candidate = if (currentLine.isEmpty()) word.text else "$currentLine ${word.text}"

        // Measured in UTF-16 code units, not graphemes: emoji and combining marks count as several, so grapheme
        // counting would wrap such text on a different word.
        if (candidate.length > maxLineLength && currentLine.isNotEmpty()) {
            lines.add(currentLine)
            currentLine = ""

            if (lines.size >= maxLines) {
                // The cue is full — flush it before starting this word.
                flushCue()
                currentLine = word.text
                cueWords = mutableListOf(word)
            } else {
                currentLine = word.text
                cueWords.add(word)
            }
        } else {
            currentLine = candidate
            cueWords.add(word)
        }
    }

    flushCue()
    return cues
}
