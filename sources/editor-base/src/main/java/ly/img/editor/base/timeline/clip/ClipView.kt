package ly.img.editor.base.timeline.clip

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.absolutePadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.LocalPinnableContainer
import androidx.compose.ui.layout.PinnableContainer
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ly.img.editor.base.dock.options.voiceover.isVoiceOverRecordingInProgress
import ly.img.editor.base.timeline.clip.trim.ClipDragType
import ly.img.editor.base.timeline.clip.trim.ClipTrimHandlesView
import ly.img.editor.base.timeline.clip.trim.ClipTrimOutlineView
import ly.img.editor.base.timeline.dragdrop.DragDropState
import ly.img.editor.base.timeline.dragdrop.DropTarget
import ly.img.editor.base.timeline.dragdrop.context
import ly.img.editor.base.timeline.state.LiveTrimState
import ly.img.editor.base.timeline.state.TimelineConfiguration
import ly.img.editor.base.timeline.state.TimelineState
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.base.ui.Event
import ly.img.editor.core.LocalEditorScope
import ly.img.editor.core.theme.LocalExtendedColorScheme
import ly.img.editor.core.ui.utils.formatForClip
import ly.img.editor.core.ui.utils.toDp
import kotlin.math.roundToInt
import kotlin.time.Duration

@Composable
fun ClipView(
    modifier: Modifier = Modifier,
    clip: Clip,
    timelineState: TimelineState,
    scrollContentOffset: () -> Int = { 0 },
    liveOffsetOverride: State<Duration?>,
    onTrimChange: (LiveTrimState) -> Unit,
    onEvent: (Event) -> Unit,
) {
    val editorContext = with(LocalEditorScope.current) { editorContext }
    val isVoiceOverRecordingInProgress = editorContext.isVoiceOverRecordingInProgress
    val isSelected by remember(clip.id) {
        derivedStateOf {
            clip.id == timelineState.selectedClip?.id
        }
    }
    val showSelectionUi by remember(
        isSelected,
        clip.id,
        clip.isVoiceOver,
        clip.hasAudioResource,
        isVoiceOverRecordingInProgress,
    ) {
        derivedStateOf {
            isSelected &&
                !(clip.isVoiceOver && (!clip.hasAudioResource || isVoiceOverRecordingInProgress))
        }
    }

    val isBeingDragged by remember(clip.id) {
        derivedStateOf { timelineState.dragDrop.draggedClipId == clip.id }
    }

    // Pin the enclosing LazyColumn item slot for the duration of a drag.
    // Otherwise vertical auto-scroll could push the source track out of the
    // composition window mid-gesture, disposing this ClipView and silently
    // killing its pointerInput coroutine — neither onDragEnd nor onDragCancel
    // would fire and dragDrop.phase would stay stuck in Dragging.
    val pinnableContainer = LocalPinnableContainer.current
    DisposableEffect(isBeingDragged, pinnableContainer) {
        val handle: PinnableContainer.PinnedHandle? = if (isBeingDragged) {
            pinnableContainer?.pin()
        } else {
            null
        }
        onDispose { handle?.release() }
    }

    // Defensive cleanup if this ClipView is the active drag subject and gets
    // disposed for reasons unrelated to LazyColumn culling (e.g. engine
    // removes the clip mid-drag via undo/redo). Without this, dragDrop.phase
    // stays in `Dragging` forever and the floating overlay remains visible
    // after the user lifts their finger.
    DisposableEffect(Unit) {
        onDispose {
            if (timelineState.dragDrop.phase.context?.clipId == clip.id) {
                timelineState.dragDrop.overrides.clear()
                timelineState.dragDrop.phase = DragDropState.Idle
            }
        }
    }

    Box(
        modifier = modifier
            .zIndex(if (showSelectionUi) 1f else 0f)
            .alpha(if (isBeingDragged) 0f else 1f),
    ) {
        val zoomState = timelineState.zoomState

        // These three states are hoisted so they can be shared between the
        // trim handles and the move-drag helpers below; both paths need
        // write access.
        val widthState = remember(clip.duration, zoomState.zoomLevel) {
            mutableStateOf(zoomState.toPx(clip.duration))
        }
        val width by widthState

        val offsetState = remember(clip.timeOffset, zoomState.zoomLevel, clip.duration) {
            mutableStateOf(zoomState.toPx(clip.timeOffset))
        }
        var offset by offsetState

        val clipDragTypeState: MutableState<ClipDragType?> = remember { mutableStateOf(null) }
        var clipDragType by clipDragTypeState

        val overlayDuration = timelineState.playerState.maxPlaybackDuration
            ?.takeIf { it < timelineState.totalDuration }
            ?: timelineState.totalDuration
        val overlayDurationWidth = zoomState.toPx(overlayDuration)

        BoxWithConstraints(
            Modifier
                .offset {
                    val overrideOffset = liveOffsetOverride.value
                        ?.takeIf { clipDragType == null }
                        ?.let { zoomState.toPx(it) }
                    IntOffset(x = (overrideOffset ?: offset).roundToInt(), y = 0)
                }
                .height(TimelineConfiguration.clipHeight)
                .width(width.toDp())
                .zIndex(if (showSelectionUi) 1f else 0f)
                .absolutePadding(right = 1.dp)
                .pointerInput(clip.id, clip.allowsSelecting) {
                    if (clip.allowsSelecting) {
                        detectTapGestures {
                            onEvent(BlockEvent.OnToggleSelectBlock(clip.id))
                        }
                    }
                },
        ) {
            var clipDurationText by remember(showSelectionUi, clip.duration) {
                mutableStateOf(
                    if (showSelectionUi) {
                        clip.duration.formatForClip()
                    } else {
                        ""
                    },
                )
            }

            val draggingColor = LocalExtendedColorScheme.current.yellow.color

            ClipTrimOutlineView(
                clip = clip,
                zoomState = zoomState,
                clipDragType = clipDragType,
                dashColor = draggingColor,
                realtimeWidth = width,
                height = maxHeight,
            )

            // SubcomposeLayout coordinates the label, background, and right-edge overlay in one pass
            // so the still-thumbnail layout can pad past the label width without a state round-trip via
            // `onGloballyPositioned`. The label is composed and measured first, then its width is fed into the background.
            SubcomposeLayout(modifier = Modifier.fillMaxSize()) { constraints ->
                val labelPlaceable = subcompose(ClipContentSlot.Label) {
                    ClipLabelView(
                        modifier = Modifier,
                        clip = clip,
                        duration = clipDurationText,
                        isSelected = showSelectionUi,
                    )
                }.first().measure(
                    // Bound the label by the clip's own width so the wrapper clamps to the clip and its rounded-corner clip masks any
                    // overflow from the inner Row's `wrapContentWidth(unbounded = true)`. Without the maxWidth bound the Row's natural
                    // width would spill past the clip's right edge into the trim handle area.
                    Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight),
                )
                val labelWidthPx = labelPlaceable.width
                val labelWidthDp = labelWidthPx.toDp()

                // Slide the label rightward when the clip's left edge has scrolled offscreen
                val clipLeftCutoffPx = (scrollContentOffset() - offset).coerceAtLeast(0f)
                val rightEdgePaddingPx = TimelineConfiguration.clipPadding.toPx()
                val maxPinOffsetPx = (width - labelWidthPx - rightEdgePaddingPx).coerceAtLeast(0f)
                val pinOffsetPx = minOf(clipLeftCutoffPx, maxPinOffsetPx)
                val pinOffsetDp = pinOffsetPx.toDp()

                val backgroundPlaceable = subcompose(ClipContentSlot.Background) {
                    ClipBackgroundView(
                        clip = clip,
                        timelineState = timelineState,
                        inTimeline = true,
                        labelWidth = labelWidthDp,
                        clipWidth = constraints.maxWidth.toDp(),
                        clipDragType = clipDragType,
                        pinOffset = pinOffsetDp,
                    )
                }.first().measure(constraints)

                // Right-edge translucent overlay for non-bg clips that extend past the page's max playback duration.
                val overlayWidthPx = (width + offset - overlayDurationWidth).coerceAtMost(width)
                val overlayPlaceable = if (overlayWidthPx > 0f) {
                    subcompose(ClipContentSlot.Overlay) {
                        val overlayShape = if (offset > overlayDurationWidth) {
                            MaterialTheme.shapes.small
                        } else {
                            MaterialTheme.shapes.small.copy(
                                topStart = CornerSize(0.dp),
                                bottomStart = CornerSize(0.dp),
                            )
                        }
                        ClipOverlay(
                            modifier = Modifier
                                .clip(overlayShape)
                                .width(overlayWidthPx.toDp())
                                .fillMaxHeight(),
                        )
                    }.firstOrNull()?.measure(
                        Constraints(maxWidth = constraints.maxWidth, maxHeight = constraints.maxHeight),
                    )
                } else {
                    null
                }

                layout(constraints.maxWidth, constraints.maxHeight) {
                    // Z-order: background under, overlay above background, label on top.
                    backgroundPlaceable.place(0, 0)
                    overlayPlaceable?.place(constraints.maxWidth - overlayPlaceable.width, 0)
                    labelPlaceable.place(
                        x = pinOffsetPx.roundToInt(),
                        y = (constraints.maxHeight - labelPlaceable.height) / 2,
                    )
                }
            }

            val viewForHapticFeedback = LocalView.current
            val dropFinishHaptic = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.LONG_PRESS
            }

            var movePlayerWasPlaying by remember { mutableStateOf(false) }

            fun startMoveDrag() {
                clipDragType = ClipDragType.Move
                movePlayerWasPlaying = timelineState.playerState.isPlaying
                timelineState.playerState.pause()
            }

            // The commit branch leaves `dragDrop.overrides` populated; they're cleared downstream
            // after OnApplyDrop's engine refresh so siblings stay at their preview positions until then.
            fun endMoveDrag(cancelled: Boolean) {
                clipDragType = null
                if (movePlayerWasPlaying) {
                    timelineState.playerState.play()
                }

                val target = if (cancelled) {
                    null
                } else {
                    timelineState.dragDrop.phase.context?.dropTarget
                }

                if (target == null) {
                    // No drop — `offset` already matches engine truth, so just clear overrides.
                    timelineState.dragDrop.overrides.clear()
                } else {
                    // Pre-snap to bridge the frame between alpha → 1.0 (phase = Idle below) and the
                    // async refresh updating `clip.timeOffset` — otherwise the clip flashes at its
                    // source position before jumping to the target.
                    val snapTime = when (target) {
                        is DropTarget.ExistingTrack -> target.timeOffset
                        is DropTarget.NewTrack -> target.timeOffset
                    }
                    offset = zoomState.toPx(snapTime)

                    val backgroundTrack = timelineState.dataSource.backgroundTrack
                    val isBgNoOpReorder = timelineState.dataSource.findTrack(clip) === backgroundTrack &&
                        target is DropTarget.ExistingTrack &&
                        target.trackId == backgroundTrack.id &&
                        snapTime == clip.timeOffset

                    if (isBgNoOpReorder) {
                        // Skip dispatch — `insertChild(idx = current)` is a no-op in the engine
                        // and would only add an empty undo step.
                        timelineState.dragDrop.overrides.clear()
                    } else {
                        onEvent(
                            BlockEvent.OnApplyDrop(
                                clip = clip,
                                target = target,
                                siblingOffsets = timelineState.dragDrop.overrides.toMap(),
                            ),
                        )
                        // Finish haptic on successful drop only; cancels and no-target paths stay silent.
                        viewForHapticFeedback.performHapticFeedback(dropFinishHaptic)
                    }
                }
                timelineState.dragDrop.phase = DragDropState.Idle
            }

            ClipMoveDragGesture(
                clip = clip,
                timelineState = timelineState,
                isBeingDragged = isBeingDragged,
                onEvent = onEvent,
                onStartMoveDrag = ::startMoveDrag,
                onEndMoveDrag = ::endMoveDrag,
            )

            if (showSelectionUi) {
                ClipTrimHandlesView(
                    clip = clip,
                    timelineState = timelineState,
                    clipBodyWidth = maxWidth,
                    clipBodyHeight = maxHeight,
                    widthState = widthState,
                    offsetState = offsetState,
                    clipDragTypeState = clipDragTypeState,
                    onClipDurationTextChange = { clipDurationText = it },
                    onTrimChange = onTrimChange,
                    onEvent = onEvent,
                )
            }
        }

        if (!clip.isInBackgroundTrack) {
            ClipOverlay(
                modifier = Modifier
                    .offset(overlayDurationWidth.toDp())
                    .height(TimelineConfiguration.clipHeight)
                    .fillMaxWidth()
                    .align(Alignment.TopEnd),
            )
        }
    }
}

private enum class ClipContentSlot { Label, Background, Overlay }
