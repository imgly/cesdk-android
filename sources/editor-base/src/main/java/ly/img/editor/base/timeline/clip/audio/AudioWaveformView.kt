package ly.img.editor.base.timeline.clip.audio

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ly.img.editor.base.timeline.state.TimelineConfiguration
import ly.img.editor.core.theme.LocalExtendedColorScheme

/**
 * Renders audio waveform visualization using real audio samples.
 *
 * - Each sample value (0.0 to 1.0) is rendered as a vertical bar
 * - Bar height represents audio amplitude at that point in time
 * - Bars are evenly spaced with rounded corners
 * - Minimum bar height of 1dp ensures silent parts are still visible
 *
 * @param samples List of audio amplitude values (0.0 to 1.0)
 * @param modifier Modifier for the canvas
 * @param drawOffsetDp Horizontal offset in dp for drawing bars within the canvas.
 *   Used during trim gestures to keep bars at their timeline positions while the
 *   clip shrinks. Negative values shift bars left, positive values shift right.
 * @param barColor Color of the waveform bars
 */
@Composable
fun AudioWaveformView(
    samples: List<Float>,
    modifier: Modifier = Modifier,
    drawOffsetDp: Dp = 0.dp,
    barColor: Color = LocalExtendedColorScheme.current.purple.color,
) {
    Canvas(
        modifier = modifier,
    ) {
        if (samples.isEmpty()) return@Canvas

        val barWidthPx = TimelineConfiguration.audioWaveformBarWidth.toPx()
        val barGapPx = TimelineConfiguration.audioWaveformBarGap.toPx()
        val drawOffsetPx = drawOffsetDp.toPx()
        val variableHeightPercentage = 0.8f
        val minHeightPx = 1.dp.toPx() // Minimum 1dp height for visibility

        samples.forEachIndexed { index, sample ->
            // Clamp sample to 0..1 range
            val clampedSample = sample.coerceIn(0f, 1f)

            // Calculate bar height: sample * 0.8 * height, with minimum of 1dp
            val height = maxOf(
                clampedSample * variableHeightPercentage * size.height,
                minHeightPx,
            )

            val xOffset = drawOffsetPx + index * (barWidthPx + barGapPx)

            // Skip bars that are off-screen to the left
            if (xOffset + barWidthPx < 0) return@forEachIndexed

            // Stop drawing if we exceed the canvas width
            if (xOffset >= size.width) return@Canvas

            drawRoundRect(
                color = barColor,
                topLeft = Offset(x = xOffset, y = (size.height - height) / 2),
                size = Size(width = barWidthPx, height = height),
                cornerRadius = CornerRadius(0.5f),
            )
        }
    }
}
