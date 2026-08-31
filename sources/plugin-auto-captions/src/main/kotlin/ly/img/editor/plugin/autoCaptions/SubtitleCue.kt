package ly.img.editor.plugin.autoCaptions

import java.util.Locale

/** A single subtitle cue with timings in seconds. */
internal data class SubtitleCue(
    val start: Double,
    val end: Double,
    val text: String,
)

/**
 * SRT parsing and serialization for the generate flow: a provider's timings are relative to its own audio, so they
 * have to be shifted and merged across blocks before the editor imports them.
 *
 * The parser is deliberately lenient — the engine's strict one validates the final file on import — so the index
 * line is optional and both `,` and `.` millisecond separators are accepted.
 */
internal object Srt {
    /** Parses SRT text into cues, skipping malformed blocks. */
    fun parse(srt: String): List<SubtitleCue> = srt
        .replace("\r\n", "\n")
        .split("\n\n")
        .mapNotNull { block ->
            val lines = block.split("\n").filter { it.isNotBlank() }
            val timingIndex = lines.indexOfFirst { it.contains("-->") }
            if (timingIndex < 0) return@mapNotNull null
            val timings = lines[timingIndex].split("-->")
            if (timings.size != 2) return@mapNotNull null
            val start = seconds(timings[0]) ?: return@mapNotNull null
            val end = seconds(timings[1]) ?: return@mapNotNull null
            val text = lines.subList(timingIndex + 1, lines.size).joinToString("\n")
            if (text.isEmpty()) return@mapNotNull null
            SubtitleCue(start = start, end = end, text = text)
        }

    /** Serializes cues into SRT text, numbering them in the given order. */
    fun serialize(cues: List<SubtitleCue>): String = cues
        .mapIndexed { index, cue -> "${index + 1}\n${timestamp(cue.start)} --> ${timestamp(cue.end)}\n${cue.text}" }
        .joinToString("\n\n")

    /** Formats seconds as an SRT timestamp: `HH:MM:SS,mmm`. */
    fun timestamp(seconds: Double): String {
        val totalMilliseconds = Math.round(seconds * 1000)
        val milliseconds = totalMilliseconds % 1000
        val totalSeconds = totalMilliseconds / 1000
        val secondsPart = totalSeconds % 60
        val totalMinutes = totalSeconds / 60
        val minutes = totalMinutes % 60
        val hours = totalMinutes / 60
        return String.format(Locale.ROOT, "%02d:%02d:%02d,%03d", hours, minutes, secondsPart, milliseconds)
    }

    /** Parses `HH:MM:SS,mmm` — also tolerating `MM:SS,mmm` and a `.` separator — into seconds; `null` if malformed. */
    fun seconds(timestamp: String): Double? {
        val parts = timestamp.trim().split(":")
        if (parts.size !in 2..3) return null

        val seconds = parts.last().replace(',', '.').toDoubleOrNull() ?: return null
        if (seconds < 0 || !seconds.isFinite()) return null

        val units = parts.dropLast(1).mapNotNull { it.toIntOrNull()?.takeIf { unit -> unit >= 0 } }
        if (units.size != parts.size - 1) return null

        // A `MM:SS` timestamp has no hours component.
        val hours = if (units.size == 1) 0 else units[0]
        val minutes = units.last()
        return hours * 3600.0 + minutes * 60.0 + seconds
    }
}
