package ly.img.editor.plugin.backgroundRemoval.remover

import android.graphics.Bitmap
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.Segmentation
import com.google.mlkit.vision.segmentation.selfie.SelfieSegmenterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import ly.img.editor.core.EditorScope
import ly.img.editor.plugin.backgroundRemoval.BackgroundRemovalMask
import ly.img.editor.plugin.backgroundRemoval.GoogleBackgroundRemovalConfig
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * ML Kit segmentation implementation for background removal.
 */
open class GoogleBackgroundRemover : BackgroundRemover<GoogleBackgroundRemovalConfig> {
    /**
     * Performs no setup because ML Kit clients are created when processing images.
     */
    override fun EditorScope.initialize() = Unit

    /**
     * Runs ML Kit selfie segmentation for [bitmap] and returns its foreground mask.
     */
    override suspend fun EditorScope.processImage(bitmap: Bitmap): BackgroundRemovalMask = withContext(Dispatchers.Default) {
        val image = InputImage.fromBitmap(bitmap, 0)
        suspendCancellableCoroutine { continuation ->
            val selfieOptions = SelfieSegmenterOptions.Builder()
                .setDetectorMode(SelfieSegmenterOptions.SINGLE_IMAGE_MODE)
                .enableRawSizeMask()
                .build()

            val selfieSegmenter = Segmentation.getClient(selfieOptions)

            selfieSegmenter.process(image)
                .addOnSuccessListener { segmentationMask ->
                    if (continuation.isActive) {
                        val result = BackgroundRemovalMask(
                            buffer = segmentationMask.buffer,
                            width = segmentationMask.width,
                            height = segmentationMask.height,
                        )
                        continuation.resume(result)
                    }
                }
                .addOnFailureListener { exception ->
                    if (continuation.isActive) {
                        continuation.resumeWithException(exception)
                    }
                }

            continuation.invokeOnCancellation {
                selfieSegmenter.close()
            }
        }
    }
}
