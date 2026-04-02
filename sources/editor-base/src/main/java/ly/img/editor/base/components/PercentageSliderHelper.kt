package ly.img.editor.base.components

import kotlin.math.abs

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
    private fun isSymmetricalPercentageSlider(
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
     * Get the number of fractional digits (after the decimal separator) from a number, capped at [MAX_FRACTION_DIGITS].
     */
    private fun countFractionalDigits(value: Float): Int {
        val str = value.toString()
        val dotIndex = str.indexOf('.')
        if (dotIndex == -1) return 0
        val fraction = str.substring(dotIndex + 1)
        if (fraction == "0") return 0
        return fraction.length.coerceAtMost(MAX_FRACTION_DIGITS)
    }

    /**
     * Format a raw slider value for display, using the step to determine decimal places.
     */
    fun formatValue(
        value: Float,
        step: Float,
    ): String {
        val decimals = countFractionalDigits(step)
        return String.format("%.${decimals}f", value)
    }
}
