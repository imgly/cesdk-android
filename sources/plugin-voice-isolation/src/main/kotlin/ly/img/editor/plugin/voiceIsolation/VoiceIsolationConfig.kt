package ly.img.editor.plugin.voiceIsolation

import okhttp3.OkHttpClient

/**
 * Configuration for the voice isolation plugin.
 *
 * Use `IMGLYVoiceIsolationConfig` from `plugin-voice-isolation-imgly`, or implement this interface to run your own
 * [VoiceIsolator].
 */
interface VoiceIsolationConfig {
    /**
     * HTTP client used to load the audio of clips that come from a remote URI.
     */
    val httpClient: OkHttpClient

    /**
     * Backend that writes the cleaned audio of a clip.
     */
    val isolator: VoiceIsolator
}
