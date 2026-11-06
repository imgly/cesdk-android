package ly.img.editor.plugin.voiceIsolation

import android.net.Uri
import androidx.core.net.toUri
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Uses IMG.LY's voice isolation, which runs the DeepFilterNet3 model on the device with ONNX Runtime.
 *
 * @property modelBaseUri Base URI of the folder that holds the model files `enc.onnx`, `erb_dec.onnx`, `df_dec.onnx`
 * and `config.ini`. Use `file:///android_asset/<folder>` to bundle the model with your app.
 * @property loadMode Controls when the model is downloaded.
 * @property httpClient the HTTP client to make network calls.
 */
data class IMGLYVoiceIsolationConfig(
    /**
     * Base URI of the folder that holds the model files.
     */
    val modelBaseUri: Uri = "https://staticimgly.com/imgly/plugin-mobile-voice-isolation/1.0.0".toUri(),
    /**
     * Controls when the model is downloaded.
     */
    val loadMode: LoadMode = LoadMode.EAGER,
    /**
     * Makes network calls.
     */
    override val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build(),
) : VoiceIsolationConfig {
    /**
     * Controls when the IMG.LY model is downloaded.
     */
    enum class LoadMode {
        /**
         * Download the model when the editor starts.
         */
        EAGER,

        /**
         * Download the model on first use.
         */
        LAZY,
    }

    /**
     * IMG.LY voice isolator configured by this instance.
     */
    override val isolator: VoiceIsolator = IMGLYVoiceIsolator(this)
}
