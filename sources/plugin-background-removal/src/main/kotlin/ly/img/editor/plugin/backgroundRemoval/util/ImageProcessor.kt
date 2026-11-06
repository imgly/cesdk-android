package ly.img.editor.plugin.backgroundRemoval.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ly.img.editor.plugin.backgroundRemoval.BackgroundRemovalMask
import ly.img.engine.Source
import ly.img.engine.internal.api.bitmap.BitmapNativeApi
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

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
     * Decodes [bytes], with the pixels turned as the EXIF orientation says. The engine shows an image that way, while
     * `BitmapFactory` ignores the orientation, so a portrait camera photo would come back sideways.
     */
    fun decodeUpright(bytes: ByteArray): Bitmap? {
        val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        val orientation = runCatching {
            ExifInterface(ByteArrayInputStream(bytes))
                .getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val matrix = Matrix()
        when (orientation) {
            ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
            ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
            ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.setScale(1f, -1f)
            ExifInterface.ORIENTATION_TRANSPOSE -> {
                matrix.setRotate(90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
            ExifInterface.ORIENTATION_TRANSVERSE -> {
                matrix.setRotate(-90f)
                matrix.postScale(-1f, 1f)
            }
            ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
            else -> return bitmap
        }
        val upright = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (upright != bitmap) {
            bitmap.recycle()
        }
        return upright
    }

    /**
     * The processed image as PNG files: one at its own size, and one for each source set entry, in its order.
     */
    class ImageFiles(
        val image: Uri,
        val sourceSet: List<Uri>,
    )

    /**
     * Writes [bitmap] as PNG at its own size and at the size of each entry of [sourceSet]. An entry without a size gets
     * the full-size file. The scene refers to the files, so they stay in the cache directory.
     */
    suspend fun savePngs(
        bitmap: Bitmap,
        sourceSet: List<Source>,
        context: Context,
    ): ImageFiles = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, RESULTS_DIRECTORY)
        check(directory.isDirectory || directory.mkdirs()) { "Could not create $directory." }
        val image = savePng(bitmap, directory)
        val sourceSetFiles = sourceSet.map { source ->
            val isFullSize = source.width == bitmap.width && source.height == bitmap.height
            if (isFullSize || source.width == 0 || source.height == 0) {
                image
            } else {
                val scaled = Bitmap.createScaledBitmap(bitmap, source.width, source.height, true)
                try {
                    savePng(scaled, directory)
                } finally {
                    scaled.recycle()
                }
            }
        }
        ImageFiles(image = image, sourceSet = sourceSetFiles)
    }

    /**
     * Deletes the files that [savePngs] wrote, for a result that is not used.
     */
    suspend fun delete(files: ImageFiles) = withContext(Dispatchers.IO) {
        (listOf(files.image) + files.sourceSet).distinct().forEach { uri ->
            if (!File(requireNotNull(uri.path)).delete()) {
                Log.w(BackgroundRemovalConstants.TAG, "Could not delete $uri.")
            }
        }
    }

    private fun savePng(
        bitmap: Bitmap,
        directory: File,
    ): Uri {
        val file = File(directory, "${UUID.randomUUID()}.png")
        FileOutputStream(file).use {
            check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)) { "Could not encode the processed image as PNG." }
        }
        return Uri.fromFile(file)
    }

    private const val RESULTS_DIRECTORY = "ly.img.plugin.backgroundRemoval/results"
}
