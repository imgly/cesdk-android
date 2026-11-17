package ly.img.editor.base.components

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ly.img.editor.base.components.numeric.SnapDetents
import ly.img.editor.compose.material3.Slider
import ly.img.editor.core.ui.UiDefaults
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private const val MAX_TICK_STEPS = 20
private val DetentCaptureZone = 6.dp
private val DetentEscapeDistance = 12.dp

/**
 * A titled slider that moves in steps, with its value at the end.
 *
 * @param step the amount one step moves the value. Null steps by 1% of the range, see [PercentageSliderHelper.stepFromMinMax].
 * @param disableAutoPercentage shows the plain value on a range inside ±1, such as `0.1..1`, where a percentage would
 * read a false `0%` at the minimum.
 * @param detents values that capture the drag, such as `0`, with one haptic tick. Ignored while one step is wider than
 * the 6dp capture zone, because such a step lands on the target anyway.
 * @param showsReadout false hides the value next to the track. TalkBack still reads it.
 */
@Composable
fun PropertySlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    step: Float? = null,
    disableAutoPercentage: Boolean = false,
    enabled: Boolean = true,
    detents: List<Float> = emptyList(),
    showsReadout: Boolean = true,
) {
    Column {
        SectionHeader(text = title)
        var sliderValue by remember(value) { mutableFloatStateOf(value) }
        Card(
            colors = UiDefaults.cardColors,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 16.dp),
            ) {
                SteppedSlider(
                    title = title,
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it
                        onValueChange(it)
                    },
                    onValueChangeFinished = {
                        if (sliderValue != value) {
                            onValueChangeFinished()
                        }
                    },
                    valueRange = valueRange,
                    step = step,
                    disableAutoPercentage = disableAutoPercentage,
                    enabled = enabled,
                    detents = detents,
                    showsReadout = showsReadout,
                )
            }
        }
    }
}

/**
 * The track and readout of [PropertySlider], for a row that adds its own controls, such as the volume's mute button.
 * It keeps no value of its own, so the caller holds the dragged [value].
 */
@Composable
internal fun RowScope.SteppedSlider(
    title: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    step: Float? = null,
    disableAutoPercentage: Boolean = false,
    enabled: Boolean = true,
    detents: List<Float> = emptyList(),
    showsReadout: Boolean = true,
) {
    val min = valueRange.start
    val max = valueRange.endInclusive
    val effectiveStep = remember(step, min, max) {
        step ?: PercentageSliderHelper.stepFromMinMax(min, max)
    }
    val steps = remember(effectiveStep, min, max) {
        (((max - min) / effectiveStep).roundToInt() - 1).coerceAtLeast(0)
    }
    // A percentage range centred on zero fills from zero. Wider symmetric ranges are not neutral at zero.
    val isCentered = PercentageSliderHelper.isSymmetricalPercentageSlider(min, max)

    fun displayText(value: Float): String = PercentageSliderHelper.readoutText(
        value = value,
        min = min,
        max = max,
        step = effectiveStep,
        disableAutoPercentage = disableAutoPercentage,
    )

    val view = LocalView.current
    val density = LocalDensity.current
    var sliderWidthPx by remember { mutableIntStateOf(0) }
    // From the rendered width, so the detent zones stay a fixed screen distance.
    val pxPerValue = if (max > min) sliderWidthPx / (max - min) else 0f
    val captureZonePx = with(density) { DetentCaptureZone.toPx() }
    val escapeDistancePx = with(density) { DetentEscapeDistance.toPx() }
    val snapDetents = remember(detents, effectiveStep, valueRange, pxPerValue, captureZonePx) {
        SnapDetents(SnapDetents.targets(detents, effectiveStep, valueRange, pxPerValue, captureZonePx))
    }
    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource, snapDetents) {
        // Reset on both edges: a gesture that the system cancels may never deliver the release.
        interactionSource.interactions.collect { if (it is DragInteraction.Start) snapDetents.reset() }
    }

    val text = displayText(value)
    val spokenText = PercentageSliderHelper.spokenText(value, min, max, effectiveStep, disableAutoPercentage)
    Slider(
        value = value,
        onValueChange = { raw ->
            val advance = snapDetents.advance(raw, pxPerValue, captureZonePx, escapeDistancePx)
            if (advance.didCapture) view.performDetentHaptic()
            val newValue = advance.value
            onValueChange(newValue)
        },
        valueRange = valueRange,
        steps = steps,
        enabled = enabled,
        interactionSource = interactionSource,
        modifier = Modifier
            .weight(1f)
            .onSizeChanged { sliderWidthPx = it.width }
            // TalkBack reads "50%" instead of a fraction, and the title with it.
            .semantics {
                contentDescription = title
                stateDescription = spokenText
            },
        track = { sliderPositions ->
            val activeColor = if (enabled) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            }
            val inactiveColor = MaterialTheme.colorScheme.surfaceVariant
            val activeTickColor = if (enabled) {
                MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.38f)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            }
            val inactiveTickColor = if (enabled) {
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            }
            val centerTickColor = MaterialTheme.colorScheme.outlineVariant
            Canvas(
                modifier = Modifier.fillMaxWidth().height(4.dp),
            ) {
                val centerY = size.height / 2
                val thumbFraction = sliderPositions.activeRange.endInclusive
                val fillStart = if (isCentered) min(0.5f, thumbFraction) else 0f
                val fillEnd = if (isCentered) max(0.5f, thumbFraction) else thumbFraction
                drawLine(
                    color = inactiveColor,
                    start = Offset(0f, centerY),
                    end = Offset(size.width, centerY),
                    strokeWidth = size.height,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = activeColor,
                    start = Offset(size.width * fillStart, centerY),
                    end = Offset(size.width * fillEnd, centerY),
                    strokeWidth = size.height,
                    cap = StrokeCap.Round,
                )
                if (isCentered) {
                    val tickHeight = 12.dp.toPx()
                    drawLine(
                        color = centerTickColor,
                        start = Offset(size.width / 2, centerY - tickHeight / 2),
                        end = Offset(size.width / 2, centerY + tickHeight / 2),
                        strokeWidth = 2.dp.toPx(),
                    )
                }
                if (steps <= MAX_TICK_STEPS) {
                    val tickSize = 2.dp.toPx()
                    sliderPositions.tickFractions.groupBy {
                        it > fillEnd || it < fillStart
                    }.forEach { (outsideFraction, fractions) ->
                        drawPoints(
                            points = fractions.map { Offset(size.width * it, centerY) },
                            pointMode = PointMode.Points,
                            color = if (outsideFraction) inactiveTickColor else activeTickColor,
                            strokeWidth = tickSize,
                            cap = StrokeCap.Round,
                        )
                    }
                }
            }
        },
        onValueChangeFinished = {
            snapDetents.reset()
            onValueChangeFinished()
        },
    )
    if (showsReadout) {
        SliderReadout(
            text = text,
            widestText = remember(min, max, effectiveStep, disableAutoPercentage) {
                PercentageSliderHelper.widestReadoutText(min, max, effectiveStep, disableAutoPercentage)
            },
            enabled = enabled,
        )
    }
}

/** Reserves the width of [widestText], so the track keeps its length and the thumb stays under the finger. */
@Composable
internal fun SliderReadout(
    text: String,
    widestText: String,
    enabled: Boolean,
) {
    val style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum")
    Box(
        modifier = Modifier
            .widthIn(min = 40.dp)
            .padding(start = 8.dp)
            // The slider's state description reads the value, so TalkBack reads neither text again.
            .clearAndSetSemantics {},
        contentAlignment = Alignment.CenterEnd,
    ) {
        Text(text = widestText, style = style, maxLines = 1, modifier = Modifier.alpha(0f))
        Text(
            text = text,
            style = style,
            maxLines = 1,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurfaceVariant
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
            textAlign = TextAlign.End,
        )
    }
}

/** One light tick on capture, never per step, otherwise a 100-step slider buzzes continuously. */
private fun android.view.View.performDetentHaptic() {
    performHapticFeedback(
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            HapticFeedbackConstants.SEGMENT_TICK
        } else {
            HapticFeedbackConstants.CLOCK_TICK
        },
    )
}

@Composable
fun PropertySlider(
    @StringRes title: Int,
    value: Float,
    onValueChange: (Float) -> Unit,
    onValueChangeFinished: () -> Unit,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    step: Float? = null,
    disableAutoPercentage: Boolean = false,
    enabled: Boolean = true,
    detents: List<Float> = emptyList(),
    showsReadout: Boolean = true,
) = PropertySlider(
    title = stringResource(title),
    value = value,
    onValueChange = onValueChange,
    onValueChangeFinished = onValueChangeFinished,
    valueRange = valueRange,
    step = step,
    disableAutoPercentage = disableAutoPercentage,
    enabled = enabled,
    detents = detents,
    showsReadout = showsReadout,
)
