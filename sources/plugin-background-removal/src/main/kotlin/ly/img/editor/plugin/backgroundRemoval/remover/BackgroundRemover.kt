package ly.img.editor.plugin.backgroundRemoval.remover

import android.graphics.Bitmap
import ly.img.editor.core.EditorScope
import ly.img.editor.plugin.backgroundRemoval.BackgroundRemovalConfig
import ly.img.editor.plugin.backgroundRemoval.BackgroundRemovalMask

/**
 * Produces foreground masks for images that should have their background removed.
 *
 * @param Config configuration type used by the remover implementation.
 */
interface BackgroundRemover<Config : BackgroundRemovalConfig> {
    /**
     * Prepares the remover for use.
     *
     * Implementations can use this hook to warm up local models or initialize third-party clients.
     */
    fun EditorScope.initialize()

    /**
     * Processes [bitmap] and returns a mask where foreground pixels should remain visible.
     *
     * @param bitmap source image to segment.
     * @return segmentation mask for [bitmap].
     */
    suspend fun EditorScope.processImage(bitmap: Bitmap): BackgroundRemovalMask
}
