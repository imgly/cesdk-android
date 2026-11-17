package ly.img.editor.plugin.voiceIsolation

/**
 * An error of the voice isolation plugin.
 */
sealed class VoiceIsolationException(
    message: String,
    cause: Throwable? = null,
) : Exception(message, cause) {
    /**
     * The block is not a video or audio clip whose audio the plugin can clean, for example a video that has loaded
     * without an audio track. Check it with [canIsolateVoice] first.
     */
    class UnsupportedBlock : VoiceIsolationException("The block is not a video or audio clip that can be cleaned.")

    /**
     * The clip turns out to have no audio when it loads, or its audio has no samples.
     */
    class NoAudio : VoiceIsolationException("The clip has no audio.")

    /**
     * The clip was deleted or changed while the plugin worked on it, for example it got another file or its audio was
     * separated. The plugin leaves the clip unchanged.
     */
    class BlockChanged : VoiceIsolationException("The clip changed while its voice was being isolated.")

    /**
     * The model could not be downloaded or loaded.
     */
    class ModelDownloadFailed(
        cause: Throwable,
    ) : VoiceIsolationException("Could not load the voice isolation model: ${cause.message}", cause)

    /**
     * The audio could not be decoded, cleaned or written.
     */
    class ProcessingFailed(
        cause: Throwable,
    ) : VoiceIsolationException("Could not isolate the voice: ${cause.message}", cause)
}
