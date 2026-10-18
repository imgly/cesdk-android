package ly.img.editor.base.timeline.view

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.isActive
import ly.img.editor.base.components.scrollbar.LazyColumnScrollbar
import ly.img.editor.base.components.scrollbar.RowScrollbar
import ly.img.editor.base.components.scrollbar.ScrollbarSettings
import ly.img.editor.base.timeline.clip.ClipOverlay
import ly.img.editor.base.timeline.dragdrop.DragDropState
import ly.img.editor.base.timeline.dragdrop.DropTarget
import ly.img.editor.base.timeline.dragdrop.NewTrackLineIndicator
import ly.img.editor.base.timeline.dragdrop.context
import ly.img.editor.base.timeline.state.LiveTrimState
import ly.img.editor.base.timeline.state.TimelineConfiguration
import ly.img.editor.base.timeline.state.TimelineState
import ly.img.editor.base.timeline.track.TrackView
import ly.img.editor.core.LocalEditorScope
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.theme.surface3
import ly.img.editor.core.ui.utils.roundToPx
import kotlin.math.roundToInt
import kotlin.time.Duration.Companion.ZERO

private val DRAG_AUTO_SCROLL_HORIZONTAL_THRESHOLD = 48.dp
private val DRAG_AUTO_SCROLL_VERTICAL_THRESHOLD = 24.dp
private val DRAG_AUTO_SCROLL_BASE_SPEED = 2.dp
private const val DRAG_AUTO_SCROLL_DISTANCE_MULTIPLIER = 0.2f

@Composable
fun TimelineContentView(
    timelineState: TimelineState,
    verticalScrollState: LazyListState,
    onEvent: (EditorEvent) -> Unit,
) {
    TimelineBaseView(
        timelineState = timelineState,
        onEvent = onEvent,
    ) { horizontalScrollState ->
        val editorScope = LocalEditorScope.current
        val editorContext = editorScope.run { editorContext }
        val state by editorContext.state.collectAsState()
        val minDuration = remember(state.minVideoDuration) {
            state.minVideoDuration?.takeIf { it > ZERO }
        }
        val maxDuration = remember(minDuration, state.maxVideoDuration) {
            state.maxVideoDuration
                ?.takeIf { it > ZERO }
                ?.takeIf { minDuration == null || it >= minDuration }
        }
        val overlayWidth = maxWidth / 2
        val viewportWidth = maxWidth
        val overlayWidthPx = overlayWidth.roundToPx()

        // Expose horizontal scroll to the `DragDropStore` via a provider so consumers read
        // the current value within the same frame; mirroring through a LaunchedEffect
        // dispatches after composition and lags by a frame during auto-scroll.
        DisposableEffect(horizontalScrollState) {
            timelineState.dragDrop.horizontalScrollProvider = { horizontalScrollState.value }
            onDispose {
                timelineState.dragDrop.horizontalScrollProvider = { 0 }
            }
        }

        val density = LocalDensity.current
        val autoScrollHorizontalThresholdPx = with(density) { DRAG_AUTO_SCROLL_HORIZONTAL_THRESHOLD.toPx() }
        val autoScrollVerticalThresholdPx = with(density) { DRAG_AUTO_SCROLL_VERTICAL_THRESHOLD.toPx() }
        val autoScrollBaseSpeedPx = with(density) { DRAG_AUTO_SCROLL_BASE_SPEED.toPx() }
        // `phase` is replaced on every pointer move in `recomputeDragPreview`. Read through
        // a derived boolean so this composable only recomposes on Idle ↔ Dragging
        // transitions instead of per drag frame.
        val isDragging by remember {
            derivedStateOf { timelineState.dragDrop.phase is DragDropState.Dragging }
        }
        LaunchedEffect(isDragging) {
            if (!isDragging) return@LaunchedEffect
            val backgroundTrackId = timelineState.dataSource.backgroundTrack.id
            while (isActive) {
                val ctx = timelineState.dragDrop.phase.context ?: break
                val horizontalFrame = timelineState.dragDrop.viewportFrame
                val horizontalSpeed = computeAutoScrollSpeed(
                    pointerCoord = ctx.currentTouchLocation.x,
                    frameMin = horizontalFrame.left,
                    frameMax = horizontalFrame.right,
                    thresholdPx = autoScrollHorizontalThresholdPx,
                    baseSpeedPx = autoScrollBaseSpeedPx,
                    distanceMultiplier = DRAG_AUTO_SCROLL_DISTANCE_MULTIPLIER,
                )
                if (horizontalSpeed != 0f) {
                    horizontalScrollState.scrollBy(horizontalSpeed)
                }

                val backgroundFrame = timelineState.dragDrop.trackFrames[backgroundTrackId]
                val pointerInBackgroundLane = backgroundFrame != null &&
                    backgroundFrame.height > 0f &&
                    ctx.currentTouchLocation.y >= backgroundFrame.top
                val verticalFrame = timelineState.dragDrop.verticalViewportFrame
                // Skip vertical auto-scroll while the pointer sits at/below the bg row
                // top, it would otherwise read as an unbounded overshoot and pull
                // the foreground LazyColumn every tick.
                if (!pointerInBackgroundLane && verticalFrame.height > 0f) {
                    val verticalSpeed = computeAutoScrollSpeed(
                        pointerCoord = ctx.currentTouchLocation.y,
                        frameMin = verticalFrame.top,
                        frameMax = verticalFrame.bottom,
                        thresholdPx = autoScrollVerticalThresholdPx,
                        baseSpeedPx = autoScrollBaseSpeedPx,
                        distanceMultiplier = DRAG_AUTO_SCROLL_DISTANCE_MULTIPLIER,
                    )
                    if (verticalSpeed != 0f) {
                        verticalScrollState.scrollBy(verticalSpeed)
                    }
                }

                withFrameNanos { }
            }
        }
        val isPlayheadStickyToMax by remember(maxDuration) {
            derivedStateOf {
                val maxPlaybackPx = maxDuration?.let { timelineState.zoomState.toPx(it) } ?: return@derivedStateOf false
                horizontalScrollState.value.toFloat() >= maxPlaybackPx
            }
        }

        val scrollContentOffset: () -> Int = remember(overlayWidthPx) {
            { horizontalScrollState.value - overlayWidthPx }
        }

        val backgroundTrackGuideline = TimelineConfiguration.clipHeight + TimelineConfiguration.clipPadding * 2

        BackgroundTrackDivider(
            modifier = Modifier
                .padding(bottom = backgroundTrackGuideline)
                .align(Alignment.BottomStart),
        )

        val timelineRulerHeight = TimelineConfiguration.rulerHeight
        var backgroundLiveTrim by remember { mutableStateOf<LiveTrimState?>(null) }

        Row(
            modifier = Modifier.horizontalScroll(horizontalScrollState),
        ) {
            Box {
                val backgroundTrack = timelineState.dataSource.backgroundTrack
                val durationWidth = timelineState.zoomState.toDp(timelineState.totalDuration)
                // Anchor to the live bg trailing edge: packed length + live-trim delta +
                // FG → BG drop preview delta. Tracks in-flight trims and drop previews
                // before the engine commits.
                val addClipButtonOffset by remember(backgroundTrack) {
                    derivedStateOf {
                        val packedLength = backgroundTrack.clips.fold(ZERO) { acc, clip -> acc + clip.duration }
                        val trimDelta = backgroundLiveTrim?.let { trim ->
                            val dragged = backgroundTrack.clips.find { it.id == trim.clipId } ?: return@let null
                            val originalEnd = dragged.timeOffset + dragged.duration
                            trim.end - originalEnd
                        } ?: ZERO
                        // FG → BG drop preview: dragged clip's duration extends bg's
                        // trailing edge before commit. Zero for bg reorders and idle.
                        val dropDelta = (timelineState.dragDrop.phase as? DragDropState.Dragging)
                            ?.context?.let { ctx ->
                                val target = ctx.dropTarget as? DropTarget.ExistingTrack
                                if (target?.trackId != backgroundTrack.id) return@let null
                                val dragged = timelineState.dragDrop.draggedClip ?: return@let null
                                if (dragged.isInBackgroundTrack) return@let null
                                dragged.duration
                            } ?: ZERO
                        timelineState.zoomState.toDp((packedLength + trimDelta + dropDelta).coerceAtLeast(ZERO))
                    }
                }
                Column {
                    Box(Modifier.offset(x = overlayWidth)) {
                        Box(
                            modifier = Modifier
                                .width(durationWidth)
                                .height(timelineRulerHeight)
                                .background(MaterialTheme.colorScheme.surface3),
                        )
                        TimelineRulerView(
                            duration = timelineState.totalDuration,
                            zoomState = timelineState.zoomState,
                            height = timelineRulerHeight,
                            extraWidth = overlayWidth,
                        )
                    }

                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .width(overlayWidth + durationWidth + overlayWidth)
                            // Publish the foreground tracks' viewport frame so drag auto-scroll
                            // can compute pointer distance from the top/bottom edges.
                            .onGloballyPositioned { coordinates ->
                                val rect = coordinates.boundsInWindow()
                                if (timelineState.dragDrop.verticalViewportFrame != rect) {
                                    timelineState.dragDrop.verticalViewportFrame = rect
                                }
                            },
                        state = verticalScrollState,
                        contentPadding = PaddingValues(
                            start = overlayWidth,
                            top = TimelineConfiguration.clipPadding,
                            bottom = TimelineConfiguration.clipPadding,
                        ),
                        verticalArrangement = Arrangement.spacedBy(TimelineConfiguration.clipPadding),
                    ) {
                        val tracks = timelineState.dataSource.tracks

                        items(tracks, key = { it.id }) { track ->
                            TrackView(
                                track = track,
                                timelineState = timelineState,
                                scrollContentOffset = scrollContentOffset,
                                onEvent = onEvent,
                            )
                        }

                        item {
                            val audioButtonOffset by remember {
                                derivedStateOf {
                                    (horizontalScrollState.value - overlayWidthPx).coerceAtLeast(0)
                                }
                            }

                            AddAudioButton(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(x = audioButtonOffset, y = 0)
                                    }
                                    .padding(start = 1.dp),
                            )
                        }
                    }
                    Box(
                        modifier = Modifier
                            .offset(x = overlayWidth)
                            .width(durationWidth + overlayWidth)
                            .padding(vertical = TimelineConfiguration.clipPadding),
                    ) {
                        val maxOverlayWidth = if (maxDuration != null && timelineState.totalDuration > maxDuration) {
                            (durationWidth - timelineState.zoomState.toDp(maxDuration)).coerceAtLeast(0.dp)
                        } else {
                            0.dp
                        }
                        val selectionInBackgroundTrack = timelineState.selectedClip?.isInBackgroundTrack == true
                        TrackView(
                            modifier = Modifier.zIndex(if (selectionInBackgroundTrack) 1f else 0f),
                            track = backgroundTrack,
                            timelineState = timelineState,
                            scrollContentOffset = scrollContentOffset,
                            onLiveTrimChange = { backgroundLiveTrim = it },
                            onEvent = onEvent,
                        )
                        if (maxOverlayWidth > 0.dp) {
                            ClipOverlay(
                                modifier = Modifier
                                    .offset(x = timelineState.zoomState.toDp(maxDuration!!))
                                    .height(TimelineConfiguration.clipHeight)
                                    .width(maxOverlayWidth)
                                    .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                                    .zIndex(if (selectionInBackgroundTrack) 0f else 1f),
                            )
                        }
                        AddClipButton(
                            // 1.dp aligns the button with the Add Audio button when the background track is empty
                            modifier = Modifier.offset(x = addClipButtonOffset.coerceAtLeast(1.dp)),
                            onEvent = onEvent,
                        )
                    }
                }
                TimelineDurationConstraintsView(
                    modifier = Modifier
                        .width(overlayWidth + durationWidth + overlayWidth)
                        .fillMaxHeight(),
                    timelineState = timelineState,
                    scrollState = horizontalScrollState,
                    viewportWidth = viewportWidth,
                    showMaxTooltipWhileSticky = isPlayheadStickyToMax,
                    minDuration = minDuration,
                    maxDuration = maxDuration,
                    overlayWidth = overlayWidth,
                    rulerHeight = timelineRulerHeight,
                )
            }
        }

        val scrollbarSettings = ScrollbarSettings(
            thumbColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
            alwaysShowScrollbar = false,
        )

        LazyColumnScrollbar(
            state = verticalScrollState,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(bottom = backgroundTrackGuideline + 2.dp, top = timelineRulerHeight + 2.dp),
            settings = scrollbarSettings.copy(
                thumbThickness = 2.dp,
                scrollbarPadding = 2.dp,
            ),
        )

        RowScrollbar(
            state = horizontalScrollState,
            settings = scrollbarSettings.copy(
                thumbThickness = 2.dp,
                scrollbarPadding = 1.dp,
            ),
            visibleLengthDp = maxWidth,
        )

        val playheadOffsetPx by remember {
            derivedStateOf {
                val playheadPositionPx = timelineState.zoomState.toPx(timelineState.playerState.playheadPosition)
                val maxPlaybackPx = timelineState.playerState.maxPlaybackDuration?.let {
                    timelineState.zoomState.toPx(it)
                }
                val scrollPx = horizontalScrollState.value.toFloat()
                // Pin to viewport centre during manual scrubbing — but not during drag-drop.
                // During drag, the playhead stays at its content-space position.
                val isManualScroll = horizontalScrollState.isScrollInProgress &&
                    timelineState.dragDrop.phase !is DragDropState.Dragging
                val offsetPx = if (maxPlaybackPx != null && scrollPx >= maxPlaybackPx) {
                    maxPlaybackPx - scrollPx
                } else if (isManualScroll) {
                    0f
                } else {
                    playheadPositionPx - scrollPx
                }
                offsetPx.roundToInt()
            }
        }
        PlayheadView(
            modifier = Modifier
                .align(Alignment.Center)
                .offset { IntOffset(x = playheadOffsetPx, y = 0) }
                .padding(top = timelineRulerHeight - 1.dp),
        )
        NewTrackLineIndicator(timelineState = timelineState)
    }
}

/**
 * Auto-scroll speed (px/tick) for one axis during drag. Returns a signed value: negative
 * scrolls toward the leading/top edge, positive toward the trailing/bottom edge, zero
 * inside the central comfort zone. Speed ramps from `baseSpeedPx` at the threshold line
 * with `distanceMultiplier` px per pixel of overshoot beyond it.
 */
private fun computeAutoScrollSpeed(
    pointerCoord: Float,
    frameMin: Float,
    frameMax: Float,
    thresholdPx: Float,
    baseSpeedPx: Float,
    distanceMultiplier: Float,
): Float {
    val distanceFromMin = pointerCoord - frameMin
    val distanceFromMax = frameMax - pointerCoord
    return when {
        distanceFromMin < thresholdPx -> {
            val overshoot = (thresholdPx - distanceFromMin).coerceAtLeast(0f)
            -(baseSpeedPx + overshoot * distanceMultiplier)
        }
        distanceFromMax < thresholdPx -> {
            val overshoot = (thresholdPx - distanceFromMax).coerceAtLeast(0f)
            baseSpeedPx + overshoot * distanceMultiplier
        }
        else -> 0f
    }
}
