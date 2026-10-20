package ly.img.editor.base.engine

import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import androidx.compose.ui.geometry.Rect
import androidx.core.graphics.createBitmap
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import kotlin.coroutines.resume
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.roundToInt

private const val MAX_SNAPSHOT_DIMENSION = 2048f

internal data class PageSnapshot(
    val bitmap: Bitmap,
    val bounds: Rect,
)

/** Screen-space engine coordinates are dp, relative to the render view. */
internal suspend fun capturePageSnapshot(
    engine: Engine,
    view: View,
    page: DesignBlock,
): PageSnapshot? {
    return try {
        if (!engine.isEngineRunning() || !engine.block.isValid(page) || view.width <= 0 || view.height <= 0) return null
        val rect = engine.block.getScreenSpaceBoundingBoxRect(listOf(page))
        val density = view.resources.displayMetrics.density
        val bounds = Rect(rect.left * density, rect.top * density, rect.right * density, rect.bottom * density)
        if (bounds.isEmpty ||
            !listOf(bounds.left, bounds.top, bounds.right, bounds.bottom, bounds.width, bounds.height).all { it.isFinite() }
        ) {
            return null
        }
        val location = IntArray(2).also(view::getLocationInWindow)
        val windowBounds = bounds.translate(androidx.compose.ui.geometry.Offset(location[0].toFloat(), location[1].toFloat()))
        val source = android.graphics.Rect(
            floor(bounds.left).toInt(),
            floor(bounds.top).toInt(),
            ceil(bounds.right).toInt(),
            ceil(bounds.bottom).toInt(),
        )
        val fullyVisible = source.left >= 0 && source.top >= 0 && source.right <= view.width && source.bottom <= view.height
        val scale = (MAX_SNAPSHOT_DIMENSION / maxOf(bounds.width, bounds.height)).coerceAtMost(1f)
        val width = (bounds.width * scale).roundToInt().coerceAtLeast(1)
        val height = (bounds.height * scale).roundToInt().coerceAtLeast(1)
        val bitmap = if (fullyVisible) copySurface(view, source, width, height) else null
        if (bitmap != null) return PageSnapshot(bitmap, windowBounds)

        // A surface only contains visible pixels. Generate the complete page when zoomed or unavailable.
        val frame = withTimeoutOrNull(500) {
            engine.block.generateVideoThumbnailSequence(
                block = page,
                thumbnailHeight = height,
                timeBegin = 0.0,
                timeEnd = 0.1,
                numberOfFrames = 1,
            ).firstOrNull()
        } ?: return null
        // Allocate after the timeout completes, so cancellation cannot discard an owned bitmap.
        val fallback = createBitmap(frame.width, frame.height)
        try {
            fallback.copyPixelsFromBuffer(frame.imageData)
            PageSnapshot(fallback, windowBounds)
        } catch (e: Exception) {
            fallback.recycle()
            throw e
        }
    } catch (e: CancellationException) {
        throw e
    } catch (_: Exception) {
        null
    }
}

internal suspend fun copySurface(
    view: View,
    source: android.graphics.Rect,
    width: Int,
    height: Int,
): Bitmap? {
    val viewWidth = view.width
    val viewHeight = view.height
    val scale = (MAX_SNAPSHOT_DIMENSION / maxOf(viewWidth, viewHeight)).coerceAtMost(1f)
    val bufferWidth = (viewWidth * scale).roundToInt().coerceAtLeast(1)
    val bufferHeight = (viewHeight * scale).roundToInt().coerceAtLeast(1)
    if (view is TextureView) {
        return try {
            val bitmap = view.getBitmap(bufferWidth, bufferHeight) ?: return null
            cropSurfaceBitmap(bitmap, source, viewWidth, viewHeight)
        } catch (_: IllegalStateException) {
            null
        }
    }
    if (view !is SurfaceView || !view.holder.surface.isValid) return null
    // API 24-25 can copy the full surface; crop the scaled buffer after completion.
    val croppedCopy = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
    val bitmap = createBitmap(if (croppedCopy) width else bufferWidth, if (croppedCopy) height else bufferHeight)
    val copied = awaitPixelCopy(bitmap) { listener ->
        val handler = Handler(Looper.getMainLooper())
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PixelCopy.request(view, source, bitmap, listener, handler)
        } else {
            PixelCopy.request(view, bitmap, listener, handler)
        }
    } ?: return null
    return if (croppedCopy) copied else cropSurfaceBitmap(copied, source, viewWidth, viewHeight)
}

/** Owns [bitmap] until delivery; an in-flight PixelCopy keeps ownership until its callback. */
@OptIn(ExperimentalCoroutinesApi::class)
internal suspend fun awaitPixelCopy(
    bitmap: Bitmap,
    request: (PixelCopy.OnPixelCopyFinishedListener) -> Unit,
): Bitmap? {
    var deliveredBitmap: Bitmap? = null
    try {
        val result = withTimeoutOrNull(250) {
            suspendCancellableCoroutine { continuation ->
                val listener = PixelCopy.OnPixelCopyFinishedListener { result ->
                    if (result != PixelCopy.SUCCESS || !continuation.isActive) {
                        bitmap.recycle()
                        if (continuation.isActive) continuation.resume(null)
                    } else {
                        // PixelCopy cannot be cancelled. Its late callback owns the buffer until completion.
                        continuation.resume(bitmap, onCancellation = { bitmap.recycle() })
                    }
                }
                try {
                    request(listener)
                } catch (_: IllegalArgumentException) {
                    bitmap.recycle()
                    continuation.resume(null)
                }
            }.also { deliveredBitmap = it }
        }
        if (result != null) deliveredBitmap = null
        return result
    } finally {
        // withTimeout can cancel after the continuation delivered its value but before returning it.
        deliveredBitmap?.recycle()
    }
}

private fun cropSurfaceBitmap(
    bitmap: Bitmap,
    source: android.graphics.Rect,
    viewWidth: Int,
    viewHeight: Int,
): Bitmap {
    val scaleX = bitmap.width.toFloat() / viewWidth
    val scaleY = bitmap.height.toFloat() / viewHeight
    val left = floor(source.left * scaleX).toInt().coerceIn(0, bitmap.width - 1)
    val top = floor(source.top * scaleY).toInt().coerceIn(0, bitmap.height - 1)
    val right = ceil(source.right * scaleX).toInt().coerceIn(left + 1, bitmap.width)
    val bottom = ceil(source.bottom * scaleY).toInt().coerceIn(top + 1, bitmap.height)
    if (left == 0 && top == 0 && right == bitmap.width && bottom == bitmap.height) return bitmap
    var output: Bitmap? = null
    try {
        output = Bitmap.createBitmap(bitmap, left, top, right - left, bottom - top)
        return output
    } finally {
        if (output !== bitmap) bitmap.recycle()
    }
}
