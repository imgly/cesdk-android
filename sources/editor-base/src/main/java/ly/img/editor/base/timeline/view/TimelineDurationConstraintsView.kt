package ly.img.editor.base.timeline.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.delay
import ly.img.editor.base.timeline.modifier.OffsetDirection
import ly.img.editor.base.timeline.modifier.offsetByWidth
import ly.img.editor.base.timeline.state.TimelineState
import ly.img.editor.core.R
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO

@Composable
internal fun TimelineDurationConstraintsView(
    modifier: Modifier = Modifier,
    timelineState: TimelineState,
    scrollState: ScrollState,
    viewportWidth: Dp,
    showMaxTooltipWhileSticky: Boolean,
    minDuration: Duration?,
    maxDuration: Duration?,
    overlayWidth: Dp,
    rulerHeight: Dp,
) {
    if (minDuration == null && maxDuration == null) return

    val totalDuration = timelineState.totalDuration
    val zoomState = timelineState.zoomState

    val isBelowMin = minDuration?.let { totalDuration < it } == true
    val isAboveMax = maxDuration?.let { totalDuration > it } == true
    val minLineColor = if (isBelowMin) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)
    }
    val maxLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.75f)

    var showMaxTooltip by remember { mutableStateOf(false) }
    var wasAtOrAboveMax by remember { mutableStateOf(false) }
    var tooShortLabelWidthPx by remember { mutableStateOf(0) }
    var maxLabelWidthPx by remember { mutableStateOf(0) }

    LaunchedEffect(totalDuration, maxDuration) {
        if (maxDuration == null || maxDuration <= ZERO) {
            showMaxTooltip = false
            wasAtOrAboveMax = false
            return@LaunchedEffect
        }

        val isCurrentlyAtOrAboveMax = totalDuration >= maxDuration
        if (isCurrentlyAtOrAboveMax && !wasAtOrAboveMax) {
            showMaxTooltip = true
            try {
                delay(1500)
            } finally {
                showMaxTooltip = false
            }
        }
        wasAtOrAboveMax = isCurrentlyAtOrAboveMax
    }

    val showMinLine = isBelowMin
    val showMaxLine = isAboveMax || showMaxTooltip || showMaxTooltipWhileSticky
    val showMaxTooltipActual = showMaxTooltip || showMaxTooltipWhileSticky
    val showTooShortTooltip = isBelowMin

    val density = LocalDensity.current
    val scrollDp = with(density) { scrollState.value.toDp() }
    val viewportEnd = scrollDp + viewportWidth
    val lineWidth = 1.dp
    val maxEdge = (viewportEnd - lineWidth).coerceAtLeast(scrollDp)
    val tooShortLabelWidthDp = with(density) { tooShortLabelWidthPx.toDp() }
    val maxLabelWidthDp = with(density) { maxLabelWidthPx.toDp() }

    fun clampIndicatorX(rawX: Dp): Dp = when {
        rawX < scrollDp -> scrollDp
        rawX > viewportEnd -> maxEdge
        else -> rawX
    }

    fun clampIndicatorLeft(rawX: Dp): Dp = if (rawX < scrollDp) scrollDp else rawX

    fun clampTooltipCenterX(
        rawX: Dp,
        tooltipWidth: Dp,
    ): Dp {
        if (tooltipWidth <= 0.dp) return rawX
        val halfWidth = tooltipWidth / 2
        val minCenter = scrollDp + halfWidth
        val maxCenter = (viewportEnd - halfWidth).coerceAtLeast(minCenter)
        return rawX.coerceIn(minCenter, maxCenter)
    }

    fun clampTooltipCenterLeft(
        rawX: Dp,
        tooltipWidth: Dp,
    ): Dp {
        if (tooltipWidth <= 0.dp) return rawX
        val halfWidth = tooltipWidth / 2
        val minCenter = scrollDp + halfWidth
        return if (rawX < minCenter) minCenter else rawX
    }

    Box(modifier = modifier.zIndex(1f)) {
        if (minDuration != null && minDuration > ZERO) {
            val minX = clampIndicatorX(overlayWidth + zoomState.toDp(minDuration))
            val tooShortLabelCenterX = clampTooltipCenterX(minX, tooShortLabelWidthDp)
            if (showMinLine) {
                TimelineConstraintLine(
                    modifier = Modifier
                        .offset(x = minX)
                        .fillMaxHeight(),
                    color = minLineColor,
                )
            }

            Tooltip(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = tooShortLabelCenterX)
                    .offsetByWidth(OffsetDirection.Left)
                    .heightIn(min = rulerHeight)
                    .onSizeChanged { tooShortLabelWidthPx = it.width },
                visible = showTooShortTooltip,
                backgroundColor = MaterialTheme.colorScheme.error,
                textColor = MaterialTheme.colorScheme.onError,
                text = stringResource(R.string.ly_img_editor_timeline_video_length_too_short),
            )
        }

        if (maxDuration != null && maxDuration > ZERO) {
            val rawMaxX = overlayWidth + zoomState.toDp(maxDuration)
            val maxX = clampIndicatorLeft(rawMaxX)
            val maxLabelCenterX = clampTooltipCenterLeft(maxX, maxLabelWidthDp)
            if (showMaxLine) {
                TimelineConstraintLine(
                    modifier = Modifier
                        .offset(x = maxX)
                        .fillMaxHeight(),
                    color = maxLineColor,
                )
            }

            Tooltip(
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = maxLabelCenterX)
                    .offsetByWidth(OffsetDirection.Left)
                    .heightIn(min = rulerHeight)
                    .onSizeChanged { maxLabelWidthPx = it.width },
                visible = showMaxTooltipActual,
                backgroundColor = MaterialTheme.colorScheme.inverseSurface,
                textColor = MaterialTheme.colorScheme.inverseOnSurface,
                text = stringResource(R.string.ly_img_editor_timeline_maximum_video_length),
            )
        }
    }
}

@Composable
private fun Tooltip(
    modifier: Modifier,
    visible: Boolean,
    backgroundColor: Color,
    textColor: Color,
    text: String,
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(animationSpec = tween(durationMillis = 150)),
        exit = fadeOut(animationSpec = tween(durationMillis = 200)),
        modifier = modifier,
    ) {
        Box(
            modifier = Modifier
                .background(backgroundColor, shape = RoundedCornerShape(4.dp))
                .padding(horizontal = 6.dp)
                .heightIn(min = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                color = textColor,
                style = MaterialTheme.typography.bodySmall,
            )
        }
    }
}

@Composable
private fun TimelineConstraintLine(
    modifier: Modifier,
    color: Color,
) {
    Box(
        modifier = modifier
            .width(1.dp)
            .background(color),
    )
}
