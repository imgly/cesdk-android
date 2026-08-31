package ly.img.editor.plugin.autoCaptions

import java.io.File

/**
 * Options that control how audio is transcribed and formatted into subtitles.
 *
 * The two formatting limits are honoured by the built-in provider; a custom one is free to apply or ignore them.
 *
 * @param language the BCP-47 code of the spoken audio, e.g. `en`; `null` auto-detects.
 * @param maxLineLength the characters a line may hold before the next word wraps.
 * @param maxLines the lines a cue may span before the next words start a new cue.
 */
data class TranscriptionOptions(
    val language: String? = null,
    val maxLineLength: Int = DEFAULT_MAX_LINE_LENGTH,
    val maxLines: Int = DEFAULT_MAX_LINES,
) {
    companion object {
        const val DEFAULT_MAX_LINE_LENGTH = 37
        const val DEFAULT_MAX_LINES = 1
    }
}

/**
 * A speech-to-text backend that turns audio into SRT subtitles.
 *
 * Implement this to plug any transcription service into [AutoCaptionsPlugin]; the built-in
 * [ly.img.editor.plugin.autoCaptions.gateway.GatewayTranscriptionProvider] runs ElevenLabs Scribe v2 through the
 * IMG.LY AI Gateway.
 */
interface TranscriptionProvider {
    /** A human-readable name, included in the generation-failure log to identify which provider failed. */
    val name: String

    /**
     * Transcribes audio into SRT subtitle text.
     *
     * The surrounding coroutine is cancelled when the caller abandons the transcription, so stay cooperatively
     * cancellable.
     *
     * @param audio one audible block's whole source track, staged in the editor's cache directory. Stream it —
     * a scene's audio is unbounded, and reading the file into memory is what this parameter exists to avoid. It
     * is deleted once the generation ends, so do not keep a reference past this call.
     * @param mimeType the source track's own type — commonly `audio/mp4` (a video's extracted track), `audio/wav`
     * or `audio/mpeg` (a standalone audio block).
     * @param options the language to transcribe in and the line limits to format cues to.
     * @return SRT with timings relative to the start of the audio; an empty string when no speech was detected.
     * @throws Exception any transport or service error, for the caller to report as it sees fit.
     */
    suspend fun transcribe(
        audio: File,
        mimeType: String,
        options: TranscriptionOptions,
    ): String
}
