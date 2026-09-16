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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch
import ly.img.editor.base.timeline.clip.Clip
import ly.img.editor.base.timeline.clip.ClipView
import ly.img.editor.base.timeline.dragdrop.DragDropState
import ly.img.editor.base.timeline.dragdrop.DropSlotIndicatorView
import ly.img.editor.base.timeline.dragdrop.DropTarget
import ly.img.editor.base.timeline.state.LiveTrimState
import ly.img.editor.base.timeline.state.TimelineConfiguration
import ly.img.editor.base.timeline.state.TimelineState
import ly.img.editor.base.timeline.state.computeLiveTrimOverrides
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.theme.LocalExtendedColorScheme
import kotlin.math.roundToInt
import kotlin.time.Duration
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
    onEvent: (EditorEvent) -> Unit,
) {
    val coroutineScope = rememberCoroutineScope()
    val draftVoiceOverPlaceholderClip by remember(track) {
        derivedStateOf {
            track.clips.firstOrNull { clip ->
                clip.isVoiceOver &&
                    !clip.hasAudioResource &&
                    clip.duration <= ZERO
            }
        }
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

        val sortedClips by remember(track) {
            derivedStateOf {
                track.sortedClips()
            }
        }
        val trimOverrides = remember(track) {
            derivedStateOf {
                val trim = liveTrim
                // Captions are clamped to their neighbours instead of displacing them, so the lane has no live
                // cascade — previewing one would slide siblings that then snap back.
                if (!track.isCaptionTrack && trim != null && sortedClips.size >= 2) {
                    computeLiveTrimOverrides(
                        sorted = sortedClips,
                        trim = trim,
                        clampStartToZero = !isBackgroundTrack,
                        packFollowingClips = isBackgroundTrack,
                    )
                } else {
                    emptyMap()
                }
            }
        }
        val density = LocalDensity.current
        val clipEndGapPx = remember(density) {
            with(density) { TimelineConfiguration.clipEndGap.roundToPx() }
        }
        // Only the active target track observes pointer-driven target updates. Other tracks keep
        // a stable `null` value for the duration of the drag and therefore do not recompose.
        val dragTarget by remember(timelineState.dragDrop, track.id) {
            derivedStateOf {
                ((timelineState.dragDrop.phase as? DragDropState.Dragging)?.context?.dropTarget as? DropTarget.ExistingTrack)
                    ?.takeIf { it.trackId == track.id }
            }
        }
        val targetSortedSiblings by remember(timelineState.dragDrop, track.id) {
            derivedStateOf {
                val context = (timelineState.dragDrop.phase as? DragDropState.Dragging)?.context
                val target = context?.dropTarget as? DropTarget.ExistingTrack
                if (target?.trackId == track.id) context.targetSortedSiblings[target.trackId].orEmpty() else emptyList()
            }
        }
        val seamOccupiedByDrag = remember(dragTarget, targetSortedSiblings) {
            dragTarget?.let { target ->
                val outgoing = targetSortedSiblings.getOrNull(target.insertIndex - 1)
                val incoming = targetSortedSiblings.getOrNull(target.insertIndex)
                if (outgoing != null && incoming != null) outgoing.id to incoming.id else null
            }
        }

        fun projectedDragTarget(clip: Clip) = dragTarget?.takeIf {
            timelineState.dragDrop.draggedClipId == clip.id
        }

        fun projectedOffset(clip: Clip): Duration {
            val trim = liveTrim
            if (trim != null && trim.clipId == clip.id) return trim.start
            return projectedDragTarget(clip)?.timeOffset
                ?: timelineState.dragDrop.overrides[clip.id]
                ?: trimOverrides.value[clip.id]
                ?: clip.timeOffset
        }

        fun projectedDuration(clip: Clip): Duration {
            val trim = liveTrim
            if (trim != null && trim.clipId == clip.id) return trim.end - trim.start
            return projectedDragTarget(clip)?.effectiveDuration ?: clip.duration
        }

        track.transitionSeams.forEach { seam ->
            val outgoing = seam.outgoingClip
            val incoming = seam.incomingClip
            if (seamOccupiedByDrag?.first == outgoing.id && seamOccupiedByDrag.second == incoming.id) {
                return@forEach
            }
            key(outgoing.id, incoming.id) {
                TransitionSeamView(
                    // Center between the adjacent clips' live rendered edges. The outgoing clip's
                    // background is inset by [TimelineConfiguration.clipEndGap]. Evaluated in the
                    // placement phase so per-frame trim/drag updates skip recomposition.
                    offsetPx = {
                        (
                            timelineState.zoomState.toPx(projectedOffset(outgoing) + projectedDuration(outgoing)).roundToInt() -
                                clipEndGapPx +
                                timelineState.zoomState.toPx(projectedOffset(incoming)).roundToInt()
                        ) / 2f
                    },
                    hasTransition = seam.hasTransition,
                    isCompact = seam.isCompact,
                    onClick = {
                        onEvent(BlockEvent.OnSelectBlock(outgoing.id))
                        coroutineScope.launch {
                            withFrameNanos { }
                            onEvent(EditorEvent.Sheet.Open(SheetType.Transition(outgoing.id)))
                        }
                    },
                )
            }
        }

        // A caption track holds one clip per cue, and an imported SRT file routinely has hundreds. Every clip is a
        // real subcomposition with its own gesture handlers, so composing the whole lane turned scrolling into a
        // slideshow — measured at 2.4 s per frame with 800 captions. The lane therefore composes only the clips
        // near the viewport. Every other track holds a handful of clips and is left alone.
        val clipsToCompose = if (track.isCaptionTrack) {
            visibleClips(track, timelineState, scrollContentOffset)
        } else {
            track.clips
        }

        clipsToCompose.forEach { clip ->
            // Key each slot by stable clip id so a reorder moves the existing ClipView's
            // compose slot rather than reusing it positionally. Otherwise per-position
            // `remember` blocks invalidate, AsyncImage thumbnails reset, and the user sees
            // a visible jump before the new state lands.
            key(clip.id) {
                val overrideState = remember(clip.id, trimOverrides) {
                    derivedStateOf {
                        timelineState.dragDrop.overrides[clip.id] ?: trimOverrides.value[clip.id]
                    }
                }
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

/**
 * The clips of [track] that are close enough to the viewport to be worth composing.
 *
 * The filter itself re-runs on every scroll frame, but it is a cheap arithmetic pass and the result only differs
 * when a clip enters or leaves, so the rows are not recomposed while scrolling inside the composed window. A
 * screen of margin on each side means a clip is composed before it is scrolled into view. The clip being dragged
 * is always kept: disposing it would cancel the drag ([ClipView] pins itself for the same reason).
 */
@Composable
private fun visibleClips(
    track: Track,
    timelineState: TimelineState,
    scrollContentOffset: () -> Int,
): List<Clip> {
    val viewportPx = with(LocalDensity.current) { LocalConfiguration.current.screenWidthDp.dp.toPx() }
    val clips by remember(track, timelineState, viewportPx) {
        derivedStateOf {
            val zoomState = timelineState.zoomState
            val draggedClipId = timelineState.dragDrop.draggedClipId
            val overrides = timelineState.dragDrop.overrides
            val offset = scrollContentOffset()
            val windowStart = offset - viewportPx
            val windowEnd = offset + viewportPx * 2
            track.clips.filter { clip ->
                if (clip.id == draggedClipId) return@filter true
                // A clip with a live override is drawn where the drag put it, not where the engine still thinks it
                // is. Measure the drawn position: a clip dropped more than a screen from its old offset would
                // otherwise be culled the frame the drag ends, and blink out until the engine refresh lands.
                val clipStart = zoomState.toPx(overrides[clip.id] ?: clip.timeOffset)
                clipStart <= windowEnd && clipStart + zoomState.toPx(clip.duration) >= windowStart
            }
        }
    }
    return clips
}
