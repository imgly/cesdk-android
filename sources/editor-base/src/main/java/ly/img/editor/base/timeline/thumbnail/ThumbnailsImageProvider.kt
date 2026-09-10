package ly.img.editor.base.timeline.thumbnail

import android.content.res.Resources
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import ly.img.editor.base.engine.getAspectRatio
import ly.img.editor.base.timeline.clip.Clip
import ly.img.editor.base.timeline.clip.ClipType
import ly.img.editor.base.timeline.state.TimelineConfiguration
import ly.img.engine.Engine
import ly.img.engine.EngineException
import ly.img.engine.VideoThumbnailResult
import kotlin.math.ceil
import kotlin.math.roundToInt
import kotlin.time.DurationUnit

/**
 * Provider for image/video thumbnail generation.
 * Handles video, image, sticker, shape, and group clips.
 *
 * For still content (image, sticker, shape), generates a single thumbnail.
 * For video content, generates multiple thumbnails to fill the clip width.
 */
class ThumbnailsImageProvider(
    private val engine: Engine,
    private val scope: CoroutineScope,
) : ThumbnailsProvider {
    private var _thumbnails = mutableStateOf(emptyList<VideoThumbnailResult>())
    val thumbnails: List<VideoThumbnailResult>
        get() = _thumbnails.value

    /**
     * Indicates whether this clip has still content (single frame).
     * Still content clips (Image, Sticker, Shape) show only one thumbnail
     * instead of repeated frames across the clip width.
     */
    private var _isStillContent = mutableStateOf(false)
    val isStillContent: Boolean
        get() = _isStillContent.value

    /**
     * Loading state is true when thumbnails haven't been generated yet.
     */
    override val isLoading: Boolean
        get() = _thumbnails.value.isEmpty()

    private var thumbHeight = TimelineConfiguration.clipHeight

    private var job: Job? = null

    override fun loadContent(
        clip: Clip,
        width: Dp,
    ) {
        val aspectRatio = engine.block.getAspectRatio(clip.id)
        val thumbWidth = thumbHeight * aspectRatio

        // Determine if this is still content (single frame) or video (multiple frames)
        val stillContent = isStillContent(clip.clipType)
        _isStillContent.value = stillContent

        // For still content, only generate 1 frame. For video, generate enough to fill the width.
        val numberOfFrames = if (stillContent) {
            1
        } else {
            ceil(width / thumbWidth).toInt()
        }

        job?.cancel()
        job = scope.launch {
            try {
                engine.block.generateVideoThumbnailSequence(
                    block = clip.id,
                    thumbnailHeight = (thumbHeight.value * Resources.getSystem().displayMetrics.density).roundToInt(),
                    timeBegin = 0.0,
                    timeEnd = clip.duration.toDouble(DurationUnit.SECONDS),
                    numberOfFrames = numberOfFrames,
                ).toList().let {
                    _thumbnails.value = it
                }
            } catch (_: EngineException) {
                // do nothing, can happen in case the block is deleted while thumbs were being generated
            }
        }
    }

    override fun cancel() {
        job?.cancel()
    }

    /**
     * Determines if the clip type represents still content (single frame).
     * Image, Sticker, and Shape clips are considered still content.
     * Video and Group clips have dynamic content with multiple frames.
     */
    private fun isStillContent(clipType: ClipType): Boolean = when (clipType) {
        ClipType.Image, ClipType.Sticker, ClipType.Shape -> true
        ClipType.Video, ClipType.Group -> false
        // Audio and Text are handled by different providers
        ClipType.Audio, ClipType.Text -> false
    }
}
