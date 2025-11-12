package ly.img.editor.core.ui.library.fetcher

import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.CancellationSignal
import android.os.SystemClock
import android.util.Log
import android.util.Size
import androidx.core.graphics.drawable.toDrawable
import coil.ImageLoader
import coil.decode.DataSource
import coil.fetch.DrawableResult
import coil.fetch.FetchResult
import coil.fetch.Fetcher
import coil.request.Options
import coil.size.pxOrElse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.withContext
import ly.img.editor.core.library.data.SystemGalleryThumbnailUris
import coil.size.Size as CoilSize

class MediaStoreThumbnailFetcher(
    private val context: Context,
    private val source: Uri,
    private val options: Options,
) : Fetcher {
    override suspend fun fetch(): FetchResult {
        require(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            "MediaStore#loadThumbnail requires API 29+."
        }
        val resolver = context.contentResolver
        val cancellationSignal = CancellationSignal()
        currentCoroutineContext()[Job]?.invokeOnCompletion { cancellationSignal.cancel() }
        val targetSize = resolveTargetSize(options.size)
        val fetchStart = SystemClock.elapsedRealtime()
        val bitmap = withContext(Dispatchers.IO) {
            resolver.loadThumbnail(source, targetSize, cancellationSignal)
        }
        val fetchDuration = SystemClock.elapsedRealtime() - fetchStart
        Log.d(
            "MediaStoreThumbFetcher",
            "loadThumbnail uri=$source size=${targetSize.width}x${targetSize.height} duration=${fetchDuration}ms",
        )
        val drawable = bitmap.toDrawable(context.resources)
        return DrawableResult(
            drawable = drawable,
            isSampled = true,
            dataSource = DataSource.DISK,
        )
    }

    private fun resolveTargetSize(requestSize: CoilSize): Size {
        val width = requestSize.width.pxOrElse { DEFAULT_THUMBNAIL_SIZE_PX }
        val height = requestSize.height.pxOrElse { DEFAULT_THUMBNAIL_SIZE_PX }
        val clampedWidth = width.coerceIn(MIN_THUMBNAIL_SIZE_PX, MAX_THUMBNAIL_SIZE_PX)
        val clampedHeight = height.coerceIn(MIN_THUMBNAIL_SIZE_PX, MAX_THUMBNAIL_SIZE_PX)
        return Size(clampedWidth, clampedHeight)
    }

    companion object {
        private const val DEFAULT_THUMBNAIL_SIZE_PX = 512
        private const val MIN_THUMBNAIL_SIZE_PX = 128
        private const val MAX_THUMBNAIL_SIZE_PX = 1024
    }

    class Factory : Fetcher.Factory<Uri> {
        override fun create(
            data: Uri,
            options: Options,
            imageLoader: ImageLoader,
        ): Fetcher? {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) {
                return null
            }
            val source = SystemGalleryThumbnailUris.resolve(data) ?: return null
            return MediaStoreThumbnailFetcher(options.context, source, options)
        }
    }
}
