package ly.img.editor.base.timeline.thumbnail

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import ly.img.editor.base.timeline.state.TimelineConfiguration
import ly.img.engine.VideoThumbnailResult

/**
 * Displays thumbnails for a clip in the timeline.
 *
 * For still content (image/sticker/shape): Shows a single thumbnail with smart positioning.
 * For video content: Shows multiple thumbnails filling the entire clip width.
 *
 * @param thumbnails The list of video thumbnails to display.
 * @param isStillContent If true, displays a single thumbnail with smart positioning.
 * @param labelWidth The width of the label container.
 * @param clipWidth The total width of the clip.
 */
@Composable
fun ThumbnailsView(
    thumbnails: List<VideoThumbnailResult>,
    isStillContent: Boolean = false,
    labelWidth: Dp = 0.dp,
    clipWidth: Dp = 0.dp,
    pinOffset: Dp = 0.dp,
) {
    when {
        isStillContent && thumbnails.isNotEmpty() -> {
            StillContentThumbnailView(
                thumbnail = thumbnails.first(),
                labelWidth = labelWidth,
                clipWidth = clipWidth,
                pinOffset = pinOffset,
            )
        }
        thumbnails.isNotEmpty() -> {
            VideoThumbnailsRow(thumbnails = thumbnails)
        }
    }
}

/**
 * Displays a single thumbnail for still content (image/sticker/shape).
 *
 * Positioning logic:
 * - Wide clip (enough space after label): Thumbnail positioned immediately after the label
 * - Narrow clip (not enough space): Thumbnail aligned to the right, gradually going under the label
 */
@Composable
private fun StillContentThumbnailView(
    thumbnail: VideoThumbnailResult,
    labelWidth: Dp,
    clipWidth: Dp,
    pinOffset: Dp = 0.dp,
) {
    val thumbnailWidth = remember(thumbnail.width, thumbnail.height) {
        calculateThumbnailWidth(thumbnail)
    }

    val effectiveLabelWidth = labelWidth + pinOffset
    val availableSpaceAfterLabel = clipWidth - effectiveLabelWidth
    val hasEnoughSpace = availableSpaceAfterLabel >= thumbnailWidth + TimelineConfiguration.clipPadding

    Box(
        modifier = Modifier
            .fillMaxSize()
            .then(if (hasEnoughSpace) Modifier.padding(start = effectiveLabelWidth + TimelineConfiguration.clipPadding) else Modifier),
        contentAlignment = if (hasEnoughSpace) Alignment.CenterStart else Alignment.CenterEnd,
    ) {
        ThumbnailImage(thumbnail = thumbnail)
    }
}

/**
 * Displays multiple thumbnails in a row for video content.
 */
@Composable
private fun VideoThumbnailsRow(thumbnails: List<VideoThumbnailResult>) {
    Row(modifier = Modifier.fillMaxSize()) {
        thumbnails.forEach { thumbnail ->
            ThumbnailImage(thumbnail = thumbnail)
        }
    }
}

private val thumbnailFetcherFactory = VideoThumbnailResultFetcher.Factory()

/**
 * Reusable thumbnail image component with proper caching.
 */
@Composable
private fun ThumbnailImage(thumbnail: VideoThumbnailResult) {
    val cacheKey = remember(thumbnail.width, thumbnail.height, thumbnail.imageData) {
        "VideoThumbnailResult(w=${thumbnail.width},h=${thumbnail.height},hash=${thumbnail.imageData.hashCode()})"
    }

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .fetcherFactory(thumbnailFetcherFactory)
            .memoryCacheKey(cacheKey)
            .data(thumbnail)
            .build(),
        modifier = Modifier.fillMaxHeight(),
        alignment = Alignment.CenterStart,
        contentScale = ContentScale.FillHeight,
        contentDescription = null,
    )
}

/**
 * Calculates the display width of a thumbnail based on its aspect ratio
 * and the timeline clip height.
 */
private fun calculateThumbnailWidth(thumbnail: VideoThumbnailResult): Dp {
    if (thumbnail.height == 0) return 0.dp
    val aspectRatio = thumbnail.width.toFloat() / thumbnail.height.toFloat()
    return TimelineConfiguration.clipHeight * aspectRatio
}
