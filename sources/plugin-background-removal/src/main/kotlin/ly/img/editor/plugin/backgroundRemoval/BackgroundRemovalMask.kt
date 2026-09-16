package ly.img.editor.plugin.backgroundRemoval

import java.nio.ByteBuffer

/**
 * Segmentation mask returned by a background removal backend.
 *
 * @property buffer raw mask values in row-major order.
 * @property width width of the mask in pixels.
 * @property height height of the mask in pixels.
 */
data class BackgroundRemovalMask(
    val buffer: ByteBuffer,
    val width: Int,
    val height: Int,
)
