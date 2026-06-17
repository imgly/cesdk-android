package ly.img.editor.plugin.backgroundRemoval

import android.net.Uri
import androidx.core.net.toUri
import ly.img.editor.plugin.backgroundRemoval.remover.BackgroundRemover
import ly.img.editor.plugin.backgroundRemoval.remover.IMGLYBackgroundRemover
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Uses IMG.LY's ONNX Runtime based background removal implementation.
 *
 * @property model The model variant used for segmentation.
 * @property modelBaseUri Base URI used to resolve model assets.
 * @property loadMode Controls when the model is downloaded and loaded into memory.
 * @property httpClient the HTTP client to make network calls.
 */
data class IMGLYBackgroundRemovalConfig(
    /**
     * The model variant used for segmentation.
     */
    val model: Model = Model.FP16,
    /**
     * Base URI used to resolve model assets.
     */
    val modelBaseUri: Uri = "https://staticimgly.com/imgly/plugin-mobile-background-removal/1.0.0".toUri(),
    /**
     * Controls when the model is downloaded and loaded into memory.
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
) : BackgroundRemovalConfig {
    /**
     * Available IMG.LY background removal model variants.
     *
     * @property key File-name key used to resolve the model artifact.
     */
    enum class Model(
        /**
         * File-name key used to resolve the model artifact.
         */
        val key: String,
    ) {
        /**
         * Full-precision FP32 model.
         */
        FP32(key = "isnet.onnx"),

        /**
         * Half-precision FP16 model.
         */
        FP16(key = "isnet_fp16.onnx"),

        /**
         * Quantized unsigned 8-bit model.
         */
        QUINT8(key = "isnet_quint8.onnx"),
    }

    /**
     * Controls when the IMG.LY model is loaded.
     */
    enum class LoadMode {
        /**
         * Load the model during plugin initialization.
         */
        EAGER,

        /**
         * Load the model on first use.
         */
        LAZY,
    }

    /**
     * IMG.LY ONNX Runtime remover configured by this instance.
     */
    override val remover: BackgroundRemover<*> = IMGLYBackgroundRemover(this)
}
