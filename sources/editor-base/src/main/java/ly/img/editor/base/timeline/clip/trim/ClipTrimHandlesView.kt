package ly.img.editor.base.timeline.clip.trim

import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import ly.img.editor.base.timeline.clip.Clip
import ly.img.editor.base.timeline.state.LiveTrimState
import ly.img.editor.base.timeline.state.TimelineConfiguration
import ly.img.editor.base.timeline.state.TimelineState
import ly.img.editor.base.timeline.state.trimBounds
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.base.ui.Event
import ly.img.editor.core.theme.LocalExtendedColorScheme
import ly.img.editor.core.ui.utils.almostEquals
import ly.img.editor.core.ui.utils.formatForClip
import ly.img.editor.core.ui.utils.toDp
import kotlin.math.absoluteValue
import kotlin.time.Duration.Companion.seconds

/**
 * Leading + trailing trim handles overhanging the clip body, shown while the clip is selected.
 *
 * The wrapper uses `wrapContentSize(unbounded = true)` so the handles can sit outside the body.
 * [widthState], [offsetState] and [clipDragTypeState] are hoisted because the move-drag gesture
 * also reads and writes them; trim-only state stays local.
 */
@Composable
internal fun ClipTrimHandlesView(
    clip: Clip,
    timelineState: TimelineState,
    clipBodyWidth: Dp,
    clipBodyHeight: Dp,
    widthState: MutableState<Float>,
    offsetState: MutableState<Float>,
    clipDragTypeState: MutableState<ClipDragType?>,
    onClipDurationTextChange: (String) -> Unit,
    onTrimChange: (LiveTrimState) -> Unit,
    onEvent: (Event) -> Unit,
) {
    val zoomState = timelineState.zoomState
    val trimBounds by remember(clip) {
        derivedStateOf { timelineState.dataSource.trimBounds(clip) }
    }
    val handleWidth = 20.dp
    val verticalInset = 2.dp

    var width by widthState
    var offset by offsetState
    var clipDragType by clipDragTypeState

    val draggingColor = LocalExtendedColorScheme.current.yellow.color
    val selectedColor = MaterialTheme.colorScheme.primary
    val draggingIconColor = LocalExtendedColorScheme.current.yellow.onColor
    val selectedIconColor = MaterialTheme.colorScheme.onPrimary
    val selectionColor = if (clipDragType == null) selectedColor else draggingColor
    val handleIconColor = if (clipDragType == null) selectedIconColor else draggingIconColor

    val leadingTrimHandleOvershoot = remember { mutableStateOf(0f) }
    val trailingTrimHandleOvershoot = remember { mutableStateOf(0f) }

    val animatedLeadingTrimHandleOvershoot by animateOvershoot(
        overshootValue = leadingTrimHandleOvershoot,
        shouldBounce = clipDragType == null,
    )
    val animatedTrailingTrimHandleOvershoot by animateOvershoot(
        overshootValue = trailingTrimHandleOvershoot,
        shouldBounce = clipDragType == null,
    )

    HapticFeedbackOnOvershoot(overshootValue = leadingTrimHandleOvershoot, clipDragType = clipDragType)
    HapticFeedbackOnOvershoot(overshootValue = trailingTrimHandleOvershoot, clipDragType = clipDragType)

    Box(
        modifier = Modifier
            .wrapContentSize(
                unbounded = true,
                align = when {
                    animatedLeadingTrimHandleOvershoot != 0f -> Alignment.CenterEnd
                    animatedTrailingTrimHandleOvershoot != 0f -> Alignment.CenterStart
                    else -> Alignment.Center
                },
            )
            .offset(
                x = handleWidth *
                    when {
                        animatedLeadingTrimHandleOvershoot != 0f -> 1
                        animatedTrailingTrimHandleOvershoot != 0f -> -1
                        else -> 0
                    },
            )
            .size(
                width = clipBodyWidth + (handleWidth * 2) +
                    (animatedLeadingTrimHandleOvershoot + animatedTrailingTrimHandleOvershoot).toDp(),
                height = clipBodyHeight + (verticalInset * 2),
            ),
    ) {
        ClipSelectionView(
            modifier = Modifier.fillMaxSize(),
            handleWidth = handleWidth,
            color = selectionColor,
            showHandles = clipDragType != ClipDragType.Move,
        )

        fun determineLeadingTrimIconStyle(hasReachedMaxWidth: Boolean? = null): IconStyle {
            if (!clip.hasLoaded || clip.isLooping) return IconStyle.Neutral
            if (!clip.allowsTrimming) return IconStyle.Left
            if (hasReachedMaxWidth != null) {
                return if (hasReachedMaxWidth) IconStyle.Neutral else IconStyle.Left
            }
            return if (clip.trimOffset.almostEquals(0.seconds)) IconStyle.Neutral else IconStyle.Left
        }

        fun determineTrailingTrimIconStyle(hasReachedMaxWidth: Boolean? = null): IconStyle {
            if (!clip.hasLoaded) return IconStyle.Neutral
            if (clip.isLooping || !clip.allowsTrimming) return IconStyle.Right
            if (hasReachedMaxWidth != null) {
                return if (hasReachedMaxWidth) IconStyle.Neutral else IconStyle.Right
            }
            val maxDuration = (clip.effectiveFootageDuration ?: 0.seconds) - clip.trimOffset - clip.duration
            return if (maxDuration.almostEquals(0.seconds)) {
                IconStyle.Neutral
            } else {
                IconStyle.Right
            }
        }

        var playerWasPlayingBeforeDragStart by remember { mutableStateOf(false) }
        var leadingTrimIconStyle by remember(clip) { mutableStateOf(determineLeadingTrimIconStyle()) }
        var trailingTrimIconStyle by remember(clip) { mutableStateOf(determineTrailingTrimIconStyle()) }

        fun onDragStart(type: ClipDragType) {
            clipDragType = type
            val playerState = timelineState.playerState
            playerWasPlayingBeforeDragStart = playerState.isPlaying
            playerState.pause()
        }

        fun onDragEnd() {
            clipDragType = null
            if (playerWasPlayingBeforeDragStart) {
                timelineState.playerState.play()
            }
        }

        fun onDrag(newWidth: Float) {
            onClipDurationTextChange(zoomState.toSeconds(newWidth).formatForClip())
        }

        // Leading trim handle
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(handleWidth)
                .align(Alignment.CenterStart)
                .pointerInput(clip, zoomState.zoomLevel) {
                    if (!clip.hasLoaded) return@pointerInput
                    val minWidth = zoomState.toPx(minOf(clip.duration, TimelineConfiguration.minClipDuration))
                        .coerceAtLeast(zoomState.toPx(trimBounds.leadingMin))
                    val footageMaxWidth = if (clip.footageDuration != null) {
                        zoomState.toPx(clip.duration + clip.trimOffset)
                    } else if (clip.isInBackgroundTrack || clip.isLooping) {
                        Float.POSITIVE_INFINITY
                    } else {
                        zoomState.toPx(clip.duration + clip.timeOffset)
                    }
                    val trackCap = zoomState.toPx(trimBounds.leadingMax)
                    val maxWidth = minOf(footageMaxWidth, trackCap)
                    var initialWidth = 0f
                    detectHorizontalDragGestures(
                        onDragStart = {
                            initialWidth = width
                            onDragStart(type = ClipDragType.Leading)
                        },
                        onHorizontalDrag = { _, drag ->
                            val oldWidth = width
                            val newProposedWidth = oldWidth - drag
                            if (leadingTrimHandleOvershoot.value == 0f) {
                                width = newProposedWidth.coerceIn(minWidth, maxWidth)
                                onDrag(width)
                            }
                            if (width != newProposedWidth) {
                                with(leadingTrimHandleOvershoot) {
                                    value = (value + drag).coerceAtMost(0f)
                                }
                            }

                            // Compare against `footageMaxWidth`, not `maxWidth`: the icon reflects hidden source content,
                            // not whether a sibling blocks further movement right now.
                            leadingTrimIconStyle = determineLeadingTrimIconStyle(width == footageMaxWidth)

                            // we only want to consume as much drag that doesn't
                            // move the trailing handle at all from its original position
                            val consumeDragAmount = oldWidth - width
                            offset += consumeDragAmount

                            onTrimChange(
                                LiveTrimState(
                                    clipId = clip.id,
                                    start = zoomState.toSeconds(offset),
                                    end = zoomState.toSeconds(offset + width),
                                ),
                            )
                        },
                        onDragEnd = {
                            onDragEnd()
                            leadingTrimHandleOvershoot.value = 0f

                            var trimOffset = clip.trimOffset
                            var timeOffset = clip.timeOffset
                            var duration = clip.duration

                            val delta = zoomState.toSeconds(initialWidth - width)

                            if (!clip.isInBackgroundTrack) {
                                timeOffset = (timeOffset + delta).coerceAtLeast(0.seconds)
                            }
                            trimOffset += delta
                            duration -= delta

                            onEvent(
                                BlockEvent.OnUpdateTrim(
                                    trimOffset = trimOffset,
                                    timeOffset = timeOffset,
                                    duration = duration,
                                ),
                            )
                            // Note: `liveTrim` is cleared by TrackView once the engine refresh lands. Clearing it here would
                            // flicker because `track.clips` hasn't yet reflected the committed shift.
                        },
                    )
                },
        ) {
            if (clipDragType != ClipDragType.Move) {
                ClipTrimHandleIconView(
                    style = leadingTrimIconStyle,
                    modifier = Modifier.align(Alignment.Center),
                    color = handleIconColor,
                )
            }
        }

        // Trailing trim handle
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(handleWidth)
                .align(Alignment.CenterEnd)
                .pointerInput(clip, zoomState.zoomLevel) {
                    if (!clip.hasLoaded) return@pointerInput
                    val minWidth = zoomState.toPx(minOf(clip.duration, TimelineConfiguration.minClipDuration))
                        .coerceAtLeast(zoomState.toPx(trimBounds.trailingMin))
                    val effectiveFootageDuration = clip.effectiveFootageDuration
                    val footageMaxWidth = if (effectiveFootageDuration != null) {
                        zoomState.toPx(effectiveFootageDuration - clip.trimOffset).coerceAtLeast(minWidth)
                    } else {
                        Float.POSITIVE_INFINITY
                    }
                    val trackCap = zoomState.toPx(trimBounds.trailingMax)
                    val maxWidth = minOf(footageMaxWidth, trackCap)
                        .coerceAtLeast(minWidth)
                    detectHorizontalDragGestures(
                        onDragStart = {
                            onDragStart(ClipDragType.Trailing)
                        },
                        onHorizontalDrag = { _, drag ->
                            val newProposedWidth = width + drag

                            if (trailingTrimHandleOvershoot.value == 0f) {
                                width = if (maxWidth.isFinite()) {
                                    newProposedWidth.coerceIn(minWidth, maxWidth)
                                } else {
                                    newProposedWidth.coerceAtLeast(minWidth)
                                }
                                onDrag(width)
                            }
                            if (width != newProposedWidth) {
                                with(trailingTrimHandleOvershoot) {
                                    value = (value + drag).coerceAtLeast(0f)
                                }
                            }

                            // See leading handle comment: compare against `footageMaxWidth`
                            // so a sibling-imposed `trackCap` doesn't hide the directional icon.
                            trailingTrimIconStyle = determineTrailingTrimIconStyle(width == footageMaxWidth)

                            onTrimChange(
                                LiveTrimState(
                                    clipId = clip.id,
                                    start = zoomState.toSeconds(offset),
                                    end = zoomState.toSeconds(offset + width),
                                ),
                            )
                        },
                        onDragEnd = {
                            trailingTrimHandleOvershoot.value = 0f
                            onDragEnd()
                            val newDuration = zoomState.toSeconds(width)
                            onEvent(BlockEvent.OnUpdateDuration(newDuration))
                            // `liveTrim` is cleared by TrackView on engine refresh; see
                            // comment in the leading trim onDragEnd above.
                        },
                    )
                },
        ) {
            if (clipDragType != ClipDragType.Move) {
                ClipTrimHandleIconView(
                    style = trailingTrimIconStyle,
                    modifier = Modifier.align(Alignment.Center),
                    color = handleIconColor,
                )
            }
        }
    }
}

// Adds a bouncy animation when trim handles are released after being overshot
@Composable
private fun animateOvershoot(
    overshootValue: State<Float>,
    shouldBounce: Boolean,
): State<Float> = animateFloatAsState(
    targetValue = overshootValue.value.absoluteValue / 2.45f,
    animationSpec = if (shouldBounce) {
        spring(
            dampingRatio = 0.35f,
            stiffness = 800f,
        )
    } else {
        snap()
    },
    label = "animatedTrimHandleOvershoot",
)

// Haptic feedback when a trim handle hits or releases past its limit.
@Composable
private fun HapticFeedbackOnOvershoot(
    overshootValue: State<Float>,
    clipDragType: ClipDragType?,
) {
    val viewForHapticFeedback = LocalView.current
    LaunchedEffect(Unit) {
        snapshotFlow { overshootValue.value }
            .drop(1)
            .map { it == 0f }
            .distinctUntilChanged()
            .collect { overshootAtZero ->
                if (clipDragType == null && overshootAtZero) {
                    viewForHapticFeedback.performHapticFeedback(
                        HapticFeedbackConstants.LONG_PRESS,
                    )
                } else if (!overshootAtZero) {
                    viewForHapticFeedback.performHapticFeedback(
                        HapticFeedbackConstants.CLOCK_TICK,
                    )
                }
            }
    }
}
