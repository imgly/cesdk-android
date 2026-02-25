package ly.img.editor.base.timeline.clip

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.valentinilk.shimmer.shimmer
import ly.img.editor.base.timeline.clip.audio.AudioWaveformView
import ly.img.editor.base.timeline.clip.trim.ClipDragType
import ly.img.editor.base.timeline.state.TimelineConfiguration
import ly.img.editor.base.timeline.state.TimelineState
import ly.img.editor.base.timeline.thumbnail.ThumbnailsAudioProvider
import ly.img.editor.base.timeline.thumbnail.ThumbnailsImageProvider
import ly.img.editor.base.timeline.thumbnail.ThumbnailsTextProvider
import ly.img.editor.base.timeline.thumbnail.ThumbnailsView
import ly.img.editor.core.theme.LocalExtendedColorScheme
import ly.img.editor.core.ui.utils.ifTrue

private val ClipBackgroundColor = Color(0x29ACAAAF)
private val ClipBackgroundGradient = Brush.linearGradient(listOf(Color(0x14FEFBFF), Color(0x141B1B1F)))

/**
 * Renders the background of a clip in the timeline.
 * Switches on provider type to render the appropriate content:
 * - ThumbnailsImageProvider: Shows image thumbnails (single or multiple)
 * - ThumbnailsTextProvider: Shows text content directly
 * - ThumbnailsAudioProvider: Shows real audio waveform visualization
 *
 * @param clip The clip data.
 * @param timelineState The timeline state containing thumbnail providers.
 * @param modifier Optional modifier.
 * @param inTimeline Whether this is rendered in the timeline (affects border styling).
 * @param labelWidth The width of the label container.
 * @param clipWidth The total width of the clip (passed from parent to avoid nested BoxWithConstraints).
 * @param clipDragType The current drag type during a trim gesture, or null when idle.
 */
@Composable
fun ClipBackgroundView(
    clip: Clip,
    timelineState: TimelineState,
    modifier: Modifier = Modifier,
    inTimeline: Boolean = false,
    labelWidth: Dp = 0.dp,
    clipWidth: Dp = 0.dp,
    clipDragType: ClipDragType? = null,
    pinOffset: Dp = 0.dp,
) {
    val backgroundColor = when (clip.clipType) {
        ClipType.Audio -> LocalExtendedColorScheme.current.purple.colorContainer
        else -> null
    }

    val provider = timelineState.getThumbnailProvider(clip.id)
    val isLoading = provider?.isLoading ?: false

    Box(
        modifier
            .ifTrue(isLoading) { shimmer() }
            .fillMaxSize()
            .clip(MaterialTheme.shapes.small)
            .background(
                when {
                    backgroundColor != null -> backgroundColor
                    else -> ClipBackgroundColor
                },
            )
            .ifTrue(backgroundColor == null) { background(ClipBackgroundGradient) }
            .ifTrue(inTimeline) {
                if (clip.allowsSelecting) {
                    border(
                        border = BorderStroke(
                            width = 0.5.dp,
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
                        ),
                        shape = MaterialTheme.shapes.small,
                    )
                } else {
                    alpha(0.3f).dashedBorder(
                        color = MaterialTheme.colorScheme.outlineVariant,
                        strokeWidth = 1.dp,
                        shape = MaterialTheme.shapes.small,
                    )
                }
            },
    ) {
        when (provider) {
            is ThumbnailsTextProvider -> {
                ClipTextContentView(
                    textContent = provider.text,
                    labelWidth = labelWidth,
                    pinOffset = pinOffset,
                )
            }
            is ThumbnailsImageProvider -> {
                ThumbnailsView(
                    thumbnails = provider.thumbnails,
                    isStillContent = provider.isStillContent,
                    labelWidth = labelWidth,
                    clipWidth = clipWidth,
                    pinOffset = pinOffset,
                )
            }
            is ThumbnailsAudioProvider -> {
                val clipDurationDp = timelineState.zoomState.toDp(clip.duration)
                // During a trim gesture the clip's visual width changes but engine data hasn't
                // been committed yet. Offset the frozen waveform so bars stay at their timeline
                // positions — the trim handle appears to pass through the static waveform.
                // After gesture end (clipDragType == null) compensate for the stale loaded data
                // until new samples arrive.
                val audioDrawOffset = when (clipDragType) {
                    ClipDragType.Leading -> clipWidth - clipDurationDp
                    ClipDragType.Trailing -> 0.dp
                    else -> timelineState.zoomState.toDp(provider.loadedTrimOffset) -
                        timelineState.zoomState.toDp(clip.trimOffset)
                }
                AudioWaveformView(
                    samples = provider.samples,
                    drawOffsetDp = audioDrawOffset,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            horizontal = TimelineConfiguration.audioWaveformBarWidth,
                            vertical = TimelineConfiguration.clipPadding,
                        )
                        .align(Alignment.Center),
                )
            }
            null -> {
                // Provider not yet created - will be created on next refresh
            }
        }
    }
}
