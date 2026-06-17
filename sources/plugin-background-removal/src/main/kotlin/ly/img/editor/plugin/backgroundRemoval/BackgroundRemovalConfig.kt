package ly.img.editor.plugin.backgroundRemoval

import ly.img.editor.plugin.backgroundRemoval.remover.BackgroundRemover
import okhttp3.OkHttpClient

/**
 * Configuration for the background removal plugin.
 *
 * Use one of the concrete implementations to choose which segmentation backend should remove image backgrounds.
 */
interface BackgroundRemovalConfig {
    /**
     * HTTP client used to load input images and model assets.
     */
    val httpClient: OkHttpClient

    /**
     * Backend implementation that produces segmentation masks for source images.
     */
    val remover: BackgroundRemover<*>
}
