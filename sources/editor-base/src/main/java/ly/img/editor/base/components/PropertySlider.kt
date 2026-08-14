package ly.img.editor.base.components

import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ly.img.editor.compose.material3.Slider
import ly.img.editor.core.ui.UiDefaults
import kotlin.math.roundToInt

private const val MAX_TICK_STEPS = 20

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
) {
    val min = valueRange.start
    val max = valueRange.endInclusive
    val showPercentage = remember(disableAutoPercentage, min, max) {
        !disableAutoPercentage && PercentageSliderHelper.isPercentageSlider(min, max)
    }
    val effectiveStep = remember(step, min, max) {
        step ?: PercentageSliderHelper.stepFromMinMax(min, max)
    }
    val steps = remember(effectiveStep, min, max) {
        (((max - min) / effectiveStep).roundToInt() - 1).coerceAtLeast(0)
    }

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
                Slider(
                    value = sliderValue,
                    onValueChange = {
                        sliderValue = it
                        onValueChange(it)
                    },
                    valueRange = valueRange,
                    steps = steps,
                    enabled = enabled,
                    modifier = Modifier.weight(1f),
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
                        Canvas(
                            modifier = Modifier.fillMaxWidth().height(4.dp),
                        ) {
                            val centerY = size.height / 2
                            drawLine(
                                color = inactiveColor,
                                start = Offset(0f, centerY),
                                end = Offset(size.width, centerY),
                                strokeWidth = size.height,
                                cap = StrokeCap.Round,
                            )
                            drawLine(
                                color = activeColor,
                                start = Offset(0f, centerY),
                                end = Offset(size.width * sliderPositions.activeRange.endInclusive, centerY),
                                strokeWidth = size.height,
                                cap = StrokeCap.Round,
                            )
                            if (steps <= MAX_TICK_STEPS) {
                                val tickSize = 2.dp.toPx()
                                sliderPositions.tickFractions.groupBy {
                                    it > sliderPositions.activeRange.endInclusive ||
                                        it < sliderPositions.activeRange.start
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
                        if (sliderValue != value) {
                            onValueChangeFinished()
                        }
                    },
                )
                val formattedText = remember(showPercentage, sliderValue, min, max, effectiveStep) {
                    if (showPercentage) {
                        "${PercentageSliderHelper.valueToPercentage(sliderValue, min, max).toInt()}"
                    } else {
                        PercentageSliderHelper.formatValue(sliderValue, effectiveStep)
                    }
                }
                Text(
                    text = formattedText,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (enabled) {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                    textAlign = TextAlign.End,
                    modifier = Modifier.widthIn(min = 40.dp).padding(start = 8.dp),
                )
            }
        }
    }
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
) = PropertySlider(
    title = stringResource(title),
    value = value,
    onValueChange = onValueChange,
    onValueChangeFinished = onValueChangeFinished,
    valueRange = valueRange,
    step = step,
    disableAutoPercentage = disableAutoPercentage,
    enabled = enabled,
)
