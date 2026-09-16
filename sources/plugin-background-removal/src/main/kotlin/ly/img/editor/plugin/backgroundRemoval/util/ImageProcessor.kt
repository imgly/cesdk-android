package ly.img.editor.plugin.backgroundRemoval.util

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ly.img.editor.plugin.backgroundRemoval.BackgroundRemovalMask
import ly.img.engine.internal.api.bitmap.BitmapNativeApi
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Utilities for processing images and applying masks
 */
internal object ImageProcessor {
    /**
     * Applies a segmentation mask to a bitmap to remove the background
     */
    suspend fun applyMaskToBitmap(
        srcBitmap: Bitmap,
        mask: BackgroundRemovalMask,
    ): Bitmap = withContext(Dispatchers.Default) {
        val originalWidth = srcBitmap.width
        val originalHeight = srcBitmap.height
        val dstBitmap = createBitmap(originalWidth, originalHeight)
        mask.buffer.rewind()
        BitmapNativeApi.applyMaskToBitmap(
            srcBitmap = srcBitmap,
            dstBitmap = dstBitmap,
            maskBuffer = mask.buffer,
            maskWidth = mask.width,
            maskHeight = mask.height,
        )
        dstBitmap
    }

    /**
     * Saves a bitmap to a temporary file and returns the URI.
     * The temporary file will be automatically deleted when the JVM exits.
     */
    suspend fun saveBitmapAsTempFile(
        bitmap: Bitmap,
        context: Context,
    ): Uri? = withContext(Dispatchers.IO) {
        try {
            val tempFile = File.createTempFile("background_removed_", ".png", context.cacheDir)
            tempFile.deleteOnExit()
            FileOutputStream(tempFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                Uri.fromFile(tempFile)
            }
        } catch (e: IOException) {
            Log.e(BackgroundRemovalConstants.TAG, "Failed to save bitmap to temp file", e)
            null
        }
    }
}
