package ly.img.editor.base.timeline.track

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import ly.img.editor.base.timeline.clip.ClipView
import ly.img.editor.base.timeline.state.TimelineConfiguration
import ly.img.editor.base.timeline.state.TimelineState
import ly.img.editor.base.ui.Event
import ly.img.editor.core.theme.LocalExtendedColorScheme
import kotlin.time.Duration.Companion.ZERO

private const val VOICE_OVER_DRAFT_PLACEHOLDER_MIN_ALPHA = 0.16f
private const val VOICE_OVER_DRAFT_PLACEHOLDER_MAX_ALPHA = 0.34f
private const val VOICE_OVER_DRAFT_PLACEHOLDER_PULSE_DURATION_MS = 1100

@Composable
fun TrackView(
    track: Track,
    timelineState: TimelineState,
    modifier: Modifier = Modifier,
    scrollContentOffset: () -> Int = { 0 },
    onEvent: (Event) -> Unit,
) {
    val draftVoiceOverPlaceholderClip = track.clips.firstOrNull { clip ->
        clip.isVoiceOver &&
            !clip.hasAudioResource &&
            clip.duration <= ZERO
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TimelineConfiguration.clipHeight),
    ) {
        if (draftVoiceOverPlaceholderClip != null) {
            val placeholderAlpha by rememberInfiniteTransition(label = "VoiceOverDraftPlaceholderTransition")
                .animateFloat(
                    initialValue = VOICE_OVER_DRAFT_PLACEHOLDER_MIN_ALPHA,
                    targetValue = VOICE_OVER_DRAFT_PLACEHOLDER_MAX_ALPHA,
                    animationSpec = infiniteRepeatable(
                        animation = tween(durationMillis = VOICE_OVER_DRAFT_PLACEHOLDER_PULSE_DURATION_MS),
                        repeatMode = RepeatMode.Reverse,
                    ),
                    label = "VoiceOverDraftPlaceholderAlpha",
                )
            BoxWithConstraints(
                modifier = Modifier.fillMaxSize(),
            ) {
                val startOffset = timelineState.zoomState
                    .toDp(timelineState.playerState.playheadPosition)
                    .coerceIn(0.dp, maxWidth)
                val contentWidth = timelineState.zoomState
                    .toDp(timelineState.totalDuration)
                    .coerceIn(0.dp, maxWidth)
                val placeholderWidth = (contentWidth - startOffset).coerceAtLeast(0.dp)
                if (placeholderWidth > 0.dp) {
                    Box(
                        modifier = Modifier
                            .offset(x = startOffset)
                            .width(placeholderWidth)
                            .fillMaxHeight()
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                LocalExtendedColorScheme.current.rose.colorContainer.copy(alpha = placeholderAlpha),
                            ),
                    )
                }
            }
        }
        track.clips.forEach { clip ->
            ClipView(
                clip = clip,
                timelineState = timelineState,
                scrollContentOffset = scrollContentOffset,
                onEvent = onEvent,
            )
        }
    }
}
