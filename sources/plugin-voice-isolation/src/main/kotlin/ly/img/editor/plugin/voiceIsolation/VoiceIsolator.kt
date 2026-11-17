package ly.img.editor.plugin.voiceIsolation

import android.content.Context
import java.io.File

/**
 * Removes music and background noise from an audio file, and keeps the voice.
 *
 * The plugin decodes the audio of a clip into [isolateVoice]'s input file and puts its output file into the scene.
 * Implement this interface to use your own model or a service. To chain your model with IMG.LY's, write your result to
 * a file in the input format and pass it to `IMGLYVoiceIsolator.isolateVoice`.
 */
interface VoiceIsolator {
    /**
     * Prepares the isolator, for example by downloading a model. The plugin calls it when the editor starts.
     *
     * @param context the context of the application.
     */
    suspend fun prewarm(context: Context) {}

    /**
     * Writes the cleaned audio of [input] to [output].
     *
     * Throw a [VoiceIsolationException] for an error that the user can act on. The plugin wraps any other exception in
     * [VoiceIsolationException.ProcessingFailed].
     *
     * @param context the context of the application.
     * @param input a WAV file with 16-bit PCM samples at 48 kHz and the channels of the clip.
     * @param output the file to write. Write a WAV file with 16-bit PCM samples at 48 kHz and the channels of [input],
     * with the duration of [input], so that the cleaned audio stays in sync with the clip.
     */
    suspend fun isolateVoice(
        context: Context,
        input: File,
        output: File,
    )
}
