package ly.img.editor.base.dock.options.crop

import android.provider.Settings
import android.view.HapticFeedbackConstants
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.paddingFromBaseline
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import ly.img.editor.core.ui.utils.toDp
import ly.img.editor.core.ui.utils.toPx
import kotlin.math.abs
import kotlin.math.exp
import kotlin.math.pow
import kotlin.math.roundToInt

@Composable
fun ScalePicker(
    value: Float,
    valueRange: ClosedFloatingPointRange<Float>,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    rangeInclusionType: RangeInclusionType = RangeInclusionType.RangeInclusiveInclusive,
    rangeExclusionDifferential: Float = 0.001f,
    onValueChangeFinished: ((Float) -> Unit)? = null,
    tickWidth: Dp = 1.dp,
    tickHeight: Dp = 16.dp,
    tickSpacing: Dp = 10.dp,
    tickStep: Int = 3,
    specialTickStep: Int = 15,
    scaleHeight: Dp = 20.dp,
    neutralNormalTickColor: Color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f),
    neutralSpecialTickColor: Color = MaterialTheme.colorScheme.outline,
    neutralScaleColor: Color = MaterialTheme.colorScheme.onSurface,
    highlightNormalTickColor: Color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
    highlightSpecialTickColor: Color = MaterialTheme.colorScheme.primary,
    highlightScaleColor: Color = MaterialTheme.colorScheme.primary,
    valuePrefix: String = "",
    contentDescription: String = valuePrefix,
) {
    val context = LocalContext.current
    // "Remove animations" sets the animator duration scale to 0, and the ruler then does not swell.
    val animationsEnabled = remember(context) {
        Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
    }
    var isDragging by remember { mutableStateOf(false) }
    val swellAmplitude by animateFloatAsState(
        targetValue = if (isDragging && animationsEnabled) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMediumLow),
        label = "ScalePickerSwell",
    )
    BoxWithConstraints(
        modifier = modifier.requiredHeight(48.dp),
    ) {
        var labelScale by remember { mutableFloatStateOf(1f) }
        val textStyle = TextStyle(
            fontFamily = FontFamily.Default,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.35.sp,
            lineHeight = 16.sp,
            fontSize = 11.sp * labelScale,
            fontFeatureSettings = "tnum",
        )

        val rangeStart = remember(valueRange) {
            if (rangeInclusionType.isStartIncluded()) valueRange.start else valueRange.start + rangeExclusionDifferential
        }

        val rangeEnd = remember(valueRange) {
            if (rangeInclusionType.isEndIncluded()) valueRange.endInclusive else valueRange.endInclusive - rangeExclusionDifferential
        }

        val tickRange = remember(valueRange, tickStep) {
            generateSequence(valueRange.start) { previous ->
                val next = previous + tickStep
                if (next > valueRange.endInclusive) null else next
            }.toList()
        }

        var currentValue by remember(value) {
            mutableStateOf(value)
        }

        val roundedCurrentValue = currentValue.roundToInt()
        val textToDraw = if (valuePrefix.isEmpty()) "$roundedCurrentValue°" else "$valuePrefix $roundedCurrentValue°"

        var snapState by remember {
            mutableStateOf(SnapState.Disarmed)
        }

        val totalTickWidthPx = tickWidth.toPx() + tickSpacing.toPx()
        val tickersWidthPx = totalTickWidthPx * tickRange.size
        val tickersWidthDp = tickersWidthPx.toDp()

        fun getTickColor(value: Float): Color {
            val roundedValue = value.roundToInt()
            return if ((currentValue < 0 && value <= 0 && value > currentValue) ||
                (currentValue > 0 && value >= 0 && value < currentValue)
            ) {
                if (roundedValue % specialTickStep == 0) highlightSpecialTickColor else highlightNormalTickColor
            } else {
                if (roundedValue % specialTickStep == 0) neutralSpecialTickColor else neutralNormalTickColor
            }
        }

        val localView = LocalView.current

        fun setAngle(target: Float): Boolean {
            val newValue = target.roundToInt().toFloat().coerceIn(rangeStart, rangeEnd)
            if (newValue == currentValue) return false
            currentValue = newValue
            onValueChange(newValue)
            onValueChangeFinished?.invoke(newValue)
            return true
        }

        Canvas(
            modifier = Modifier
                .fillMaxHeight()
                .width(maxWidth + tickersWidthDp)
                .pointerInput(value) {
                    // A restart of this block cancels the gesture without `onDragEnd` or `onDragCancel`, so the ticks
                    // would stay swollen.
                    try {
                        detectHorizontalDragGestures(
                            onDragStart = { isDragging = true },
                            onDragCancel = { isDragging = false },
                            onDragEnd = {
                                isDragging = false
                                onValueChangeFinished?.invoke(currentValue)
                            },
                            onHorizontalDrag = { _, dragAmount ->
                                val changeInValue = (-dragAmount / totalTickWidthPx) * tickStep
                                val changedValue = (currentValue + changeInValue).coerceIn(
                                    minimumValue = rangeStart,
                                    maximumValue = rangeEnd,
                                )
                                val canSnap = changedValue in (-0.5..0.5)
                                val newValue = if (snapState == SnapState.Disarmed) {
                                    if (!canSnap) snapState = SnapState.Armed
                                    changedValue
                                } else {
                                    if (canSnap) {
                                        if (abs(changedValue) > 0.2 && currentValue == 0f) {
                                            snapState = SnapState.Disarmed
                                        }
                                        0f
                                    } else {
                                        changedValue
                                    }
                                }
                                if (integerCrossedOrReached(currentValue / tickStep, newValue / tickStep) ||
                                    (newValue != currentValue && (newValue == rangeStart || newValue == rangeEnd))
                                ) {
                                    localView.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                                }
                                currentValue = newValue
                                onValueChange(currentValue)
                            },
                        )
                    } finally {
                        isDragging = false
                    }
                }
                // Workaround to enable alpha compositing
                // TODO: Use https://developer.android.com/jetpack/compose/graphics/draw/modifiers#compositing-strategy when available
                .graphicsLayer { alpha = 0.99F }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.horizontalGradient(
                            0f to Color.Transparent,
                            0.25f to Color.Black,
                            0.75f to Color.Black,
                            1f to Color.Transparent,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                },
        ) {
            val centerX = maxWidth.toPx() / 2
            val markerWidthPx = CenterMarkerWidth.toPx()
            val swellFalloffPx = 3 * tickSpacing.toPx()
            tickRange.forEach { tickValue ->
                val x = centerX + ((tickValue - currentValue) * (totalTickWidthPx / tickStep))
                val distance = abs(x - centerX)
                // The marker is opaque, so the tick under it is left out instead of showing above it.
                if (distance < markerWidthPx) return@forEach
                // Ticks near the centre grow while dragging, so the finger's position reads at a glance.
                val swell = swellAmplitude * exp(-(distance / swellFalloffPx).pow(2))
                val isLarge = tickValue.roundToInt() % specialTickStep == 0
                val growth = if (isLarge) LargeTickSwell else SmallTickSwell
                val startOffset = Offset(x = x, y = size.height)
                drawLine(
                    getTickColor(tickValue),
                    start = startOffset,
                    end = startOffset.minus(Offset(x = 0f, y = tickHeight.toPx() + growth.toPx() * swell)),
                    strokeWidth = tickWidth.toPx(),
                )
            }

            val centerTickStartOffset = Offset(center.x, size.height)
            val centerTickEndOffset = centerTickStartOffset.minus(
                Offset(x = 0f, y = scaleHeight.toPx()),
            )

            drawLine(
                if (roundedCurrentValue == 0) neutralScaleColor else highlightScaleColor,
                start = centerTickStartOffset,
                end = centerTickEndOffset,
                strokeWidth = markerWidthPx,
            )
        }

        Text(
            text = textToDraw,
            style = textStyle.copy(
                color = if (roundedCurrentValue == 0) neutralScaleColor else highlightScaleColor,
            ),
            maxLines = 1,
            softWrap = false,
            // Shrinks to 80% before it clips.
            onTextLayout = { if (it.hasVisualOverflow && labelScale > MIN_LABEL_SCALE) labelScale -= LABEL_SCALE_STEP },
            modifier = Modifier
                .align(Alignment.Center)
                // to make the number look centre-ish / account for the °
                .offset(x = 1.5.dp, y = (-4).dp)
                // Above the tallest swollen tick, so no tick runs through the digits.
                .paddingFromBaseline(bottom = tickHeight + LargeTickSwell + 4.dp)
                // The ruler below reads the value, so the label is not a second element.
                .clearAndSetSemantics {},
        )
        // One element for TalkBack, which moves 1° per swipe.
        Box(
            Modifier
                .matchParentSize()
                .clearAndSetSemantics {
                    this.contentDescription = contentDescription
                    stateDescription = "$roundedCurrentValue°"
                    progressBarRangeInfo = ProgressBarRangeInfo(
                        current = currentValue,
                        range = rangeStart..rangeEnd,
                        steps = ((rangeEnd - rangeStart).roundToInt() - 1).coerceAtLeast(0),
                    )
                    // SetProgress sets a value. TalkBack sends the next degree, other services can send any angle.
                    setProgress { target -> setAngle(target) }
                },
        )
    }
}

private val CenterMarkerWidth = 2.dp
private val LargeTickSwell = 12.dp
private val SmallTickSwell = 8.dp
private const val MIN_LABEL_SCALE = 0.8f
private const val LABEL_SCALE_STEP = 0.05f

private fun integerCrossedOrReached(
    old: Float,
    new: Float,
): Boolean {
    if (old == new) return false
    val distanceMoreThanOne = abs(old - new) > 1
    val oldInt = old.toInt()
    val newInt = new.toInt()
    val exactOld = old == oldInt.toFloat()
    val exactNew = new == newInt.toFloat()
    val intChanged = oldInt != newInt
    return distanceMoreThanOne || exactNew || (!exactOld && intChanged)
}

enum class RangeInclusionType {
    RangeInclusiveInclusive,
    RangeInclusiveExclusive,
    RangeExclusiveInclusive,
    RangeExclusiveExclusive,
    ;

    fun isStartIncluded() = this == RangeInclusiveExclusive || this == RangeInclusiveInclusive

    fun isEndIncluded() = this == RangeExclusiveInclusive || this == RangeInclusiveInclusive
}

private enum class SnapState {
    Armed,
    Disarmed,
}

@Preview(showBackground = true)
@Composable
private fun ScalePickerPreview() {
    ScalePicker(
        value = 0f,
        valueRange = -30f..30f,
        onValueChange = {},
        modifier = Modifier.padding(16.dp),
        tickStep = 1,
        specialTickStep = 5,
        valuePrefix = "Angle",
    )
}
