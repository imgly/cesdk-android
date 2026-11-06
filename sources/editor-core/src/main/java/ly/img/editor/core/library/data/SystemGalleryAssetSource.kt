package ly.img.editor.core.library.data

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.SystemClock
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ly.img.engine.Asset
import ly.img.engine.AssetContext
import ly.img.engine.AssetPayload
import ly.img.engine.AssetSource
import ly.img.engine.FindAssetsQuery
import ly.img.engine.FindAssetsResult
import java.util.Locale

/**
 * Asset source that exposes media from the user's device gallery.
 *
 * @param context Android context used to access the [MediaStore].
 * @param type Configuration for filtering gallery mime types, e.g. "image/\*" or "video/\*".
 */
class SystemGalleryAssetSource(
    context: Context,
    type: SystemGalleryAssetSourceType,
) : AssetSource(type.sourceId) {
    private val mimeTypeFilters =
        type.mimeTypeFilter.ifEmpty { listOf("image/*", "video/*") }
    private val applicationContext: Context = context.applicationContext

    override val supportedMimeTypes: List<String> = mimeTypeFilters

    override suspend fun findAssets(query: FindAssetsQuery): FindAssetsResult = withContext(Dispatchers.IO) {
        val allowMediaStoreAccess = !SystemGalleryPermission.isManualMode
        val hasPermission = if (allowMediaStoreAccess) {
            SystemGalleryPermission.hasPermissionForMimeTypes(applicationContext, mimeTypeFilters)
        } else {
            false
        }

        // Ignore MediaStore URIs only when we also enumerate the MediaStore; otherwise we keep everything.
        val selectedEntries = SystemGalleryPermission.selectedForMimeTypes(mimeTypeFilters)
        val extraSelected = if (allowMediaStoreAccess) {
            selectedEntries.filterNot { isMediaStoreContentUri(it.uri) }
        } else {
            selectedEntries
        }
        val extraCount = extraSelected.size

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.MEDIA_TYPE,
            MediaStore.MediaColumns.WIDTH,
            MediaStore.MediaColumns.HEIGHT,
            MediaStore.Video.VideoColumns.DURATION,
            MediaStore.MediaColumns.MIME_TYPE,
        )

        val normalizedFilters = mimeTypeFilters.map { it.lowercase(Locale.US) }
        val wantsVideo = normalizedFilters.isEmpty() ||
            normalizedFilters.any { it.startsWith("video/") || it.startsWith("*") }
        val wantsImage = normalizedFilters.isEmpty() ||
            normalizedFilters.any { it.startsWith("image/") || it.startsWith("*") }

        val selection: String
        val selectionArgs: Array<String>?
        when {
            wantsImage && !wantsVideo -> {
                selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
                selectionArgs = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString())
            }
            wantsVideo && !wantsImage -> {
                selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
                selectionArgs = arrayOf(MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString())
            }
            else -> {
                selection = "${MediaStore.Files.FileColumns.MEDIA_TYPE}=? OR ${MediaStore.Files.FileColumns.MEDIA_TYPE}=?"
                selectionArgs = arrayOf(
                    MediaStore.Files.FileColumns.MEDIA_TYPE_IMAGE.toString(),
                    MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO.toString(),
                )
            }
        }

        val limit = query.perPage
        val offset = query.page * query.perPage
        val sortOrder = "${MediaStore.MediaColumns.DATE_MODIFIED} DESC"

        val overallStart = SystemClock.elapsedRealtime()
        val assets = mutableListOf<Asset>()
        val seenIds = mutableSetOf<String>()
        val baseUri = MediaStore.Files.getContentUri("external")
        val start = offset
        val end = start + limit
        val mediaOffset: Int
        var mediaLimit: Int
        var hasMoreExtra = false
        var extrasAdded = 0
        if (start < extraCount) {
            // take slice from extraSelected
            val extraEnd = minOf(end, extraCount)
            for (i in start until extraEnd) {
                val entry = extraSelected[i]
                val resolvedMimeType = entry.mimeType
                    ?: applicationContext.contentResolver.getType(entry.uri)
                val typeIsVideo = resolvedMimeType?.startsWith("video/") == true
                val assetId = entry.uri.toString()
                if (seenIds.add(assetId)) {
                    val meta = resolveExtraMetadata(entry.uri, typeIsVideo)
                    assets += buildAsset(
                        uri = entry.uri,
                        isVideo = typeIsVideo,
                        width = meta.width,
                        height = meta.height,
                        duration = meta.duration,
                        mimeType = resolvedMimeType,
                    )
                    extrasAdded += 1
                }
            }
            mediaOffset = 0
            mediaLimit = (limit - extrasAdded).coerceAtLeast(0)
            hasMoreExtra = extraEnd < extraCount
        } else {
            mediaOffset = start - extraCount
            mediaLimit = limit
        }
        if (!allowMediaStoreAccess || !hasPermission) {
            mediaLimit = 0
        }
        Log.d(
            "SystemGalleryAssetSource",
            "findAssets(page=${query.page}, perPage=${query.perPage}) extrasAdded=$extrasAdded extraCount=$extraCount extraOnly=${mediaLimit <= 0}",
        )
        var mediaStoreItemsFetched = 0
        if (mediaLimit > 0 && allowMediaStoreAccess && hasPermission) {
            try {
                val queryStart = SystemClock.elapsedRealtime()
                val requestedLimit = mediaLimit + extrasAdded
                val cursor = when {
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> {
                        val args = Bundle().apply {
                            putString(BundleKeys.SELECTION, selection)
                            putStringArray(BundleKeys.SELECTION_ARGS, selectionArgs)
                            putString(BundleKeys.SORT_ORDER, sortOrder)
                            putInt(BundleKeys.LIMIT, requestedLimit)
                            putInt(BundleKeys.OFFSET, mediaOffset)
                        }
                        applicationContext.contentResolver.query(baseUri, projection, args, null)
                    }
                    Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q -> {
                        val args = Bundle().apply {
                            putString(BundleKeys.SELECTION, selection)
                            putStringArray(BundleKeys.SELECTION_ARGS, selectionArgs)
                            putString(BundleKeys.SORT_ORDER, sortOrder)
                            putInt(BundleKeys.LIMIT, mediaOffset + requestedLimit)
                        }
                        applicationContext.contentResolver.query(baseUri, projection, args, null)
                    }
                    else -> {
                        val uri = baseUri.buildUpon()
                            .appendQueryParameter("limit", "$mediaOffset,$requestedLimit")
                            .build()
                        applicationContext.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)
                    }
                }

                cursor?.use { resultCursor ->
                    val idIndex = resultCursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                    val typeIndex = resultCursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MEDIA_TYPE)
                    val widthIndex = resultCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.WIDTH)
                    val heightIndex = resultCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.HEIGHT)
                    val durationIndex = resultCursor.getColumnIndex(MediaStore.Video.VideoColumns.DURATION)
                    val mimeTypeIndex = resultCursor.getColumnIndexOrThrow(MediaStore.MediaColumns.MIME_TYPE)

                    var skipped = 0
                    while (resultCursor.moveToNext()) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
                            Build.VERSION.SDK_INT < Build.VERSION_CODES.R &&
                            skipped < mediaOffset
                        ) {
                            skipped += 1
                            continue
                        }
                        val id = resultCursor.getLong(idIndex)
                        val type = resultCursor.getInt(typeIndex)
                        val contentUri = when (type) {
                            MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO ->
                                ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                            else ->
                                ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)
                        }
                        val isVideo = type == MediaStore.Files.FileColumns.MEDIA_TYPE_VIDEO
                        val width = runCatching { resultCursor.getInt(widthIndex) }.getOrNull()?.takeIf { it > 0 }
                        val height = runCatching { resultCursor.getInt(heightIndex) }.getOrNull()?.takeIf { it > 0 }
                        val duration = if (isVideo && durationIndex != -1) {
                            runCatching { resultCursor.getLong(durationIndex) / 1000 }.getOrNull()
                        } else {
                            null
                        }
                        val mimeType = runCatching { resultCursor.getString(mimeTypeIndex) }.getOrNull()
                        val assetId = contentUri.toString()
                        if (seenIds.add(assetId)) {
                            assets += buildAsset(contentUri, isVideo, width, height, duration, mimeType)
                            mediaStoreItemsFetched += 1
                        }
                        if (assets.size >= limit) {
                            break
                        }
                    }
                } ?: run {
                    Log.e("SystemGalleryAssetSource", "Asset query returned null cursor")
                }
                val queryDuration = SystemClock.elapsedRealtime() - queryStart
                Log.d(
                    "SystemGalleryAssetSource",
                    "MediaStore query page=${query.page} offset=$mediaOffset limit=$mediaLimit fetched=$mediaStoreItemsFetched duration=${queryDuration}ms",
                )
            } catch (e: SecurityException) {
                Log.e("SystemGalleryAssetSource", "SecurityException in asset query: ${e.message}", e)
            } catch (e: Exception) {
                Log.e("SystemGalleryAssetSource", "Exception in asset query: ${e.message}", e)
            }
        }
        val hasMoreMediaStore = mediaLimit > 0 && mediaStoreItemsFetched >= mediaLimit
        val hasMore = hasMoreExtra || hasMoreMediaStore
        val nextPage = if (hasMore) query.page + 1 else -1
        val totalEstimate = if (hasMore) Int.MAX_VALUE else (offset + assets.size).coerceAtLeast(assets.size)
        val totalDuration = SystemClock.elapsedRealtime() - overallStart
        Log.d(
            "SystemGalleryAssetSource",
            "findAssets(page=${query.page}) resultCount=${assets.size} mediaFetched=$mediaStoreItemsFetched hasMore=$hasMore totalEstimate=$totalEstimate duration=${totalDuration}ms",
        )
        FindAssetsResult(assets, query.page, nextPage, totalEstimate)
    }

    override suspend fun getGroups(): List<String>? = null

    private fun buildAsset(
        uri: Uri,
        isVideo: Boolean,
        width: Int?,
        height: Int?,
        duration: Long?,
        mimeType: String?,
    ): Asset {
        val meta = mutableMapOf(
            "uri" to uri.toString(),
            "kind" to if (isVideo) "video" else "image",
        )
        val resolvedMimeType = mimeType?.takeIf { it.isNotBlank() }
            ?: if (isVideo) "video/mp4" else "image/jpeg"
        meta["mimeType"] = resolvedMimeType
        width?.takeIf { it > 0 }?.let { meta["width"] = it.toString() }
        height?.takeIf { it > 0 }?.let { meta["height"] = it.toString() }
        duration?.takeIf { it > 0 }?.let { meta["duration"] = it.toString() }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && isMediaStoreContentUri(uri)) {
            meta["thumbUri"] = SystemGalleryThumbnailUris.create(uri)
        }
        return Asset(
            id = uri.toString(),
            context = AssetContext(sourceId),
            locale = "en",
            meta = meta,
            payload = AssetPayload(),
        )
    }

    private fun resolveExtraMetadata(
        uri: Uri,
        isVideo: Boolean,
    ): ExtraMetadata = if (isVideo) {
        resolveVideoMetadata(uri)
    } else {
        resolveImageMetadata(uri)
    }

    private fun resolveImageMetadata(uri: Uri): ExtraMetadata = try {
        applicationContext.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor, null, options)
            ExtraMetadata(
                width = options.outWidth.takeIf { it > 0 },
                height = options.outHeight.takeIf { it > 0 },
                duration = null,
            )
        } ?: ExtraMetadata()
    } catch (e: Exception) {
        Log.w("SystemGalleryAssetSource", "Failed to resolve image metadata for $uri", e)
        ExtraMetadata()
    }

    private fun resolveVideoMetadata(uri: Uri): ExtraMetadata {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(applicationContext, uri)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)?.toIntOrNull()
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)?.toIntOrNull()
            val durationSeconds = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull()
                ?.let { (it / 1000).takeIf { value -> value > 0 } }
            ExtraMetadata(
                width = width?.takeIf { it > 0 },
                height = height?.takeIf { it > 0 },
                duration = durationSeconds,
            )
        } catch (e: Exception) {
            Log.w("SystemGalleryAssetSource", "Failed to resolve video metadata for $uri", e)
            ExtraMetadata()
        } finally {
            runCatching { retriever.release() }
        }
    }

    private data class ExtraMetadata(
        val width: Int? = null,
        val height: Int? = null,
        val duration: Long? = null,
    )

    private object BundleKeys {
        const val SELECTION = ContentResolver.QUERY_ARG_SQL_SELECTION
        const val SELECTION_ARGS = ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS
        const val SORT_ORDER = ContentResolver.QUERY_ARG_SQL_SORT_ORDER
        const val LIMIT = ContentResolver.QUERY_ARG_LIMIT
        const val OFFSET = ContentResolver.QUERY_ARG_OFFSET
    }

    // No local thumbnail generation; rely on image loader video extension

    private fun isMediaStoreContentUri(uri: Uri): Boolean =
        uri.scheme == ContentResolver.SCHEME_CONTENT && uri.authority == MediaStore.AUTHORITY
}
