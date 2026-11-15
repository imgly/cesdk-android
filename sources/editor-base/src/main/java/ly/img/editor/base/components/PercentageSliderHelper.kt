package ly.img.editor.base.components

import ly.img.editor.base.components.numeric.NumberStepperMath
import ly.img.editor.base.components.numeric.formatNumber
import ly.img.editor.base.components.numeric.withUnit
import java.util.Locale
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Helper to determine when to use a 'percentage slider',
 * with functions for mapping to/from percentage ranges.
 *
 * A 'percentage slider' is a Slider whose display range is [0..100] (or [-100..100] in certain cases).
 * This is a more intuitive user-facing slider range than the fractional values used behind the scenes to control shaders etc.
 */
internal object PercentageSliderHelper {
    /**
     * We show percentage sliders (range [0..100]) whenever
     * the supplied min and max parameters fall within the [-1..1] range.
     */
    fun isPercentageSlider(
        min: Float,
        max: Float,
    ): Boolean {
        val absBelow1 = abs(min) <= 1f && abs(max) <= 1f
        val different = min != max
        return absBelow1 && different
    }

    /**
     * A special case of percentage slider, when min and max values are mirrored at 0.
     * In that case, a range of [-100..100] is displayed.
     */
    fun isSymmetricalPercentageSlider(
        min: Float,
        max: Float,
    ): Boolean {
        val notZero = min != 0f && max != 0f
        val sameAbs = abs(min) == abs(max)
        return isPercentageSlider(min, max) && notZero && sameAbs
    }

    /**
     * Map a value in the range of min..max to the range of [0..100]
     * (or [-100..100] when it is a symmetrical slider).
     */
    fun valueToPercentage(
        value: Float,
        min: Float,
        max: Float,
    ): Float {
        fun valueToSteps(stepCount: Float): Float = (value - min) / ((max - min) / stepCount)
        return if (isSymmetricalPercentageSlider(min, max)) {
            valueToSteps(200f) - 100f
        } else {
            valueToSteps(100f)
        }
    }

    /**
     * Guess an appropriate step size from supplied min and max values.
     * Arranges step so that it covers the whole range with 100 or 200 steps.
     */
    fun stepFromMinMax(
        min: Float,
        max: Float,
    ): Float = if (isSymmetricalPercentageSlider(min, max)) {
        (max - min) / 200f
    } else {
        (max - min) / 100f
    }

    private const val MAX_FRACTION_DIGITS = 2

    /**
     * Format a raw slider value for display, with the decimals that [step] needs, at most [MAX_FRACTION_DIGITS].
     * Locale-aware, without grouping, and drops trailing zeros: `4`, `4.2`, `0.05`, `24,5` in German.
     */
    fun formatValue(
        value: Float,
        step: Float,
        locale: Locale = Locale.getDefault(),
    ): String = formatNumber(
        value = value.toDouble(),
        decimals = min(NumberStepperMath.decimals(step.toDouble()), MAX_FRACTION_DIGITS),
        locale = locale,
    )

    /** The longest readout the slider can show, so the readout reserves its width. */
    fun widestReadoutText(
        min: Float,
        max: Float,
        step: Float,
        disableAutoPercentage: Boolean,
        locale: Locale = Locale.getDefault(),
    ): String = listOf(min, max, (min + step).coerceAtMost(max), (max - step).coerceAtLeast(min))
        // One step inside each end, because 99.5 is wider than 100.
        .map { readoutText(it, min, max, step, disableAutoPercentage, locale) }
        .maxBy { it.length }

    /**
     * The number next to a slider: the rounded percentage on a percentage slider, otherwise the value with the decimals
     * of [step]. It has no unit, the same as the Web editor: `50`, `-100`, `4`.
     */
    fun readoutText(
        value: Float,
        min: Float,
        max: Float,
        step: Float,
        disableAutoPercentage: Boolean,
        locale: Locale = Locale.getDefault(),
    ): String = if (showsPercentage(min, max, disableAutoPercentage)) {
        valueToPercentage(value, min, max).roundToInt().toString()
    } else {
        formatValue(value, step, locale)
    }

    /** What TalkBack reads: the readout, with `%` on a percentage slider. */
    fun spokenText(
        value: Float,
        min: Float,
        max: Float,
        step: Float,
        disableAutoPercentage: Boolean,
        locale: Locale = Locale.getDefault(),
    ): String = withUnit(
        text = readoutText(value, min, max, step, disableAutoPercentage, locale),
        unit = "%".takeIf { showsPercentage(min, max, disableAutoPercentage) },
    )

    private fun showsPercentage(
        min: Float,
        max: Float,
        disableAutoPercentage: Boolean,
    ) = !disableAutoPercentage && isPercentageSlider(min, max)
}
