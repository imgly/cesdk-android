package ly.img.editor.base.components.numeric

import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.pow
import kotlin.math.roundToLong

/**
 * Arithmetic shared by every stepper and stepped slider. It has no Compose or engine dependency, so it stays
 * unit-testable.
 */
internal object NumberStepperMath {
    private const val MAX_DECIMALS = 4

    enum class Direction {
        /** Moves to the next grid point strictly above the current value. */
        Up,

        /** Moves to the next grid point strictly below the current value. */
        Down,
    }

    /** Fraction digits that [step] needs, at most 4: `0.01` needs 2, `0.25` needs 2, `1` needs 0. */
    fun decimals(step: Double): Int {
        val magnitude = abs(step)
        if (magnitude <= 0 || magnitude >= 1) return 0
        // The first power of ten that makes the step whole. The tolerance absorbs `0.01f` widening to `0.009999999776…`,
        // otherwise the noise adds digits. `ceil(-log10(step))` gives 1 for 0.25, which drops a needed digit.
        return (0 until MAX_DECIMALS).firstOrNull { digits ->
            val scaled = magnitude * 10.0.pow(digits)
            val whole = scaled.roundToLong()
            whole > 0 && abs(scaled - whole) < 1e-4 * whole
        } ?: MAX_DECIMALS
    }

    fun clamped(
        value: Double,
        bounds: ClosedFloatingPointRange<Double>,
    ): Double = value.coerceIn(bounds.start, bounds.endInclusive)

    /**
     * [bounds] with each finite end moved inward onto [decimals], so a clamped value matches the shown number.
     * Returns [bounds] unchanged when no shown value fits between the ends.
     */
    fun inwardRounded(
        bounds: ClosedFloatingPointRange<Double>,
        decimals: Int,
    ): ClosedFloatingPointRange<Double> {
        val scale = 10.0.pow(decimals)

        fun onShownGrid(
            end: Double,
            up: Boolean,
        ): Double {
            if (!end.isFinite()) return end
            val scaled = end * scale
            // A tolerance: `6.2f` widens to `6.1999998…` and `6.4f` to `6.4000001…`, so rounding either one can move
            // past a shown value.
            val tolerance = 1e-6 * max(1.0, abs(scaled))
            return (if (up) ceil(scaled - tolerance) else floor(scaled + tolerance)) / scale
        }
        val lower = onShownGrid(bounds.start, up = true)
        val upper = onShownGrid(bounds.endInclusive, up = false)
        return if (lower <= upper) lower..upper else bounds
    }

    /** Nearest multiple of [step] measured from [origin]. */
    fun snapped(
        value: Double,
        step: Double,
        origin: Double,
    ): Double = if (step <= 0) value else origin + ((value - origin) / step).roundToLong() * step

    /**
     * One button press, clamped to [bounds]. An off-grid [value] moves to the adjacent grid point in [direction], not
     * the nearest, so [Direction.Down] never raises it. Within `step / 100` of a grid point counts as on it.
     * The grid starts at the lower bound.
     */
    fun stepped(
        value: Double,
        direction: Direction,
        step: Double,
        bounds: ClosedFloatingPointRange<Double>,
    ): Double {
        // No grid, and dividing by `step` gives NaN. `abs` keeps a negative step directional.
        if (step <= 0) {
            val delta = if (direction == Direction.Up) abs(step) else -abs(step)
            return clamped(value + delta, bounds)
        }
        val origin = bounds.start
        val nearest = snapped(value, step, origin)
        val gridIndex = (value - origin) / step
        val moved = when {
            abs(nearest - value) < step / 100 -> if (direction == Direction.Up) nearest + step else nearest - step
            direction == Direction.Up -> origin + ceil(gridIndex) * step
            else -> origin + floor(gridIndex) * step
        }
        return clamped(moved, bounds)
    }
}
