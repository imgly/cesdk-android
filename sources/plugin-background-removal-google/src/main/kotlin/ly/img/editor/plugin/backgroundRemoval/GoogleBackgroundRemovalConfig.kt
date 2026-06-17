package ly.img.editor.plugin.backgroundRemoval

import ly.img.editor.plugin.backgroundRemoval.remover.BackgroundRemover
import ly.img.editor.plugin.backgroundRemoval.remover.GoogleBackgroundRemover
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Uses Google's on-device background segmentation implementation.
 *
 * @property httpClient the HTTP client to make network calls.
 */
data class GoogleBackgroundRemovalConfig(
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
     * Google ML Kit remover configured by this instance.
     */
    override val remover: BackgroundRemover<*> = GoogleBackgroundRemover()
}
