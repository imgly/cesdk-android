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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.drop
import ly.img.editor.base.timeline.clip.ClipView
import ly.img.editor.base.timeline.dragdrop.DropSlotIndicatorView
import ly.img.editor.base.timeline.state.LiveTrimState
import ly.img.editor.base.timeline.state.TimelineConfiguration
import ly.img.editor.base.timeline.state.TimelineState
import ly.img.editor.base.timeline.state.computeLiveTrimOverrides
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
    onLiveTrimChange: ((LiveTrimState?) -> Unit)? = null,
    onEvent: (Event) -> Unit,
) {
    val draftVoiceOverPlaceholderClip = track.clips.firstOrNull { clip ->
        clip.isVoiceOver &&
            !clip.hasAudioResource &&
            clip.duration <= ZERO
    }
    val isBackgroundTrack = track === timelineState.dataSource.backgroundTrack

    DisposableEffect(track.id) {
        onDispose { timelineState.dragDrop.removeTrackFrame(track.id) }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(TimelineConfiguration.clipHeight)
            // Publish the track's window space frame so drag & drop can hit-test the pointer against it.
            .onGloballyPositioned { coordinates ->
                timelineState.dragDrop.updateTrackFrame(track.id, coordinates.boundsInWindow())
            },
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
        var liveTrim by remember { mutableStateOf<LiveTrimState?>(null) }

        // Keep sibling overrides visible until the engine refresh propagates committed
        // positions back into `track.clips`. Clearing eagerly would recompose with stale
        // offsets for a frame — siblings would snap back, then jump forward once the
        // async refresh lands.
        LaunchedEffect(track) {
            snapshotFlow { track.clips.map { it.id to (it.timeOffset to it.duration) } }
                .drop(1)
                .collect { liveTrim = null }
        }

        val currentOnLiveTrimChange by rememberUpdatedState(onLiveTrimChange)
        LaunchedEffect(Unit) {
            snapshotFlow { liveTrim }.collect { currentOnLiveTrimChange?.invoke(it) }
        }

        val isMultiClip = track.clips.size >= 2
        val sortedClips by remember(track) {
            derivedStateOf {
                track.sortedClips()
            }
        }
        val currentTrim = liveTrim
        val trimOverrides = remember(currentTrim) {
            if (isMultiClip && currentTrim != null) {
                computeLiveTrimOverrides(
                    sorted = sortedClips,
                    trim = currentTrim,
                    clampStartToZero = !isBackgroundTrack,
                )
            } else {
                emptyMap()
            }
        }

        track.clips.forEach { clip ->
            // Key each slot by stable clip id so a reorder moves the existing ClipView's
            // compose slot rather than reusing it positionally. Otherwise per-position
            // `remember` blocks invalidate, AsyncImage thumbnails reset, and the user sees
            // a visible jump before the new state lands.
            key(clip.id) {
                val overrideState = rememberUpdatedState(
                    timelineState.dragDrop.overrides[clip.id] ?: trimOverrides[clip.id],
                )
                ClipView(
                    clip = clip,
                    timelineState = timelineState,
                    scrollContentOffset = scrollContentOffset,
                    liveOffsetOverride = overrideState,
                    onTrimChange = { liveTrim = it },
                    onEvent = onEvent,
                )
            }
        }

        DropSlotIndicatorView(trackId = track.id, timelineState = timelineState)
    }
}
