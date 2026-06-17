package ly.img.camera.record.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.drag
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ly.img.camera.ActiveMixedSubMode
import ly.img.camera.core.R
import ly.img.editor.core.ui.iconpack.IconPack
import ly.img.editor.core.ui.iconpack.Photocamera
import ly.img.editor.core.ui.iconpack.Photocameraoutline
import ly.img.editor.core.ui.iconpack.Videocam
import ly.img.editor.core.ui.iconpack.Videocamoutline
import kotlin.math.roundToInt

/**
 * Capsule-pill segmented control (96×48 dp) that flips [ActiveMixedSubMode] while
 * `CaptureType.Mixed` is active. Tap a segment or press-and-drag horizontally.
 */
@Composable
internal fun PhotoVideoToggle(
    activeMixedSubMode: ActiveMixedSubMode,
    onSubModeChange: (ActiveMixedSubMode) -> Unit,
    modifier: Modifier = Modifier,
) {
    val haptic = LocalHapticFeedback.current
    val scope = rememberCoroutineScope()
    val currentActive by rememberUpdatedState(activeMixedSubMode)
    val currentOnChange by rememberUpdatedState(onSubModeChange)

    val progress = remember {
        Animatable(if (activeMixedSubMode == ActiveMixedSubMode.Photo) 0f else 1f)
    }
    var isDragging by remember { mutableStateOf(false) }

    // Skip during drag so `snapTo` isn't overridden by `animateTo`.
    LaunchedEffect(activeMixedSubMode) {
        if (!isDragging) {
            val target = if (activeMixedSubMode == ActiveMixedSubMode.Photo) 0f else 1f
            if (progress.value != target) {
                progress.animateTo(target, animationSpec = tween(durationMillis = 200))
            }
        }
    }

    // `derivedStateOf` so consumers recompose only on midline cross, not every frame.
    val photoActive by remember { derivedStateOf { progress.value < 0.5f } }

    Box(
        modifier = modifier
            .size(width = PILL_WIDTH.dp, height = PILL_HEIGHT.dp)
            .background(color = Color.White.copy(alpha = 0.15f), shape = CircleShape)
            .pointerInput(Unit) {
                // Circle center follows finger x, clamped to the rails.
                val circleCenterMinPx = (CIRCLE_X_START_DP + ACTIVE_CIRCLE_SIZE / 2f).dp.toPx()
                val circleCenterMaxPx = circleCenterMinPx + CIRCLE_TRAVEL_DP.dp.toPx()
                val travelPx = circleCenterMaxPx - circleCenterMinPx
                awaitEachGesture {
                    val down = awaitFirstDown(requireUnconsumed = false)
                    isDragging = true
                    var hasMoved = false
                    drag(down.id) { change ->
                        if (change.position == change.previousPosition) return@drag
                        hasMoved = true
                        val clampedX = change.position.x.coerceIn(circleCenterMinPx, circleCenterMaxPx)
                        val newProgress = (clampedX - circleCenterMinPx) / travelPx
                        val crossedMidline = (progress.value < 0.5f) != (newProgress < 0.5f)
                        scope.launch { progress.snapTo(newProgress) }
                        if (crossedMidline) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        change.consume()
                    }
                    isDragging = false
                    if (hasMoved) {
                        val target = if (progress.value < 0.5f) 0f else 1f
                        val targetMode = if (target == 0f) ActiveMixedSubMode.Photo else ActiveMixedSubMode.Video
                        scope.launch { progress.animateTo(target, tween(durationMillis = 200)) }
                        if (targetMode != currentActive) currentOnChange(targetMode)
                    }
                }
            },
    ) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset {
                    val xPx = (CIRCLE_X_START_DP + CIRCLE_TRAVEL_DP * progress.value).dp.toPx()
                    IntOffset(xPx.roundToInt(), 0)
                }
                .size(ACTIVE_CIRCLE_SIZE.dp)
                .background(Color.White, CircleShape),
        )

        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SegmentIcon(
                isActive = photoActive,
                activeIcon = IconPack.Photocamera,
                inactiveIcon = IconPack.Photocameraoutline,
                contentDescription = stringResource(R.string.ly_img_camera_button_photo_mode),
                onClick = { onSubModeChange(ActiveMixedSubMode.Photo) },
            )
            SegmentIcon(
                isActive = !photoActive,
                activeIcon = IconPack.Videocam,
                inactiveIcon = IconPack.Videocamoutline,
                contentDescription = stringResource(R.string.ly_img_camera_button_video_mode),
                onClick = { onSubModeChange(ActiveMixedSubMode.Video) },
            )
        }
    }
}

@Composable
private fun SegmentIcon(
    isActive: Boolean,
    activeIcon: ImageVector,
    inactiveIcon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
) {
    val haptic = LocalHapticFeedback.current
    val iconColor by animateColorAsState(
        targetValue = if (isActive) Color.Black else Color.White,
        animationSpec = tween(durationMillis = 200),
        label = "PhotoVideoToggleSegmentIcon",
    )
    val interactionSource = remember { MutableInteractionSource() }

    Box(
        modifier = Modifier
            .size(SEGMENT_SIZE.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    if (!isActive) {
                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        onClick()
                    }
                },
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isActive) activeIcon else inactiveIcon,
            contentDescription = contentDescription,
            tint = iconColor,
        )
    }
}

private const val PILL_WIDTH = 96
private const val PILL_HEIGHT = 48
private const val SEGMENT_SIZE = 48
private const val ACTIVE_CIRCLE_SIZE = 40
private const val CIRCLE_X_START_DP = 4
private const val CIRCLE_TRAVEL_DP = 48
