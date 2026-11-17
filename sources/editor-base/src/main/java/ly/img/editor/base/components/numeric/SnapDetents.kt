package ly.img.editor.base.components.numeric

import kotlin.math.abs

/**
 * Magnetic-detent state machine for one slider drag with discrete snap targets. Distances are in px, so the zones stay
 * a fixed screen size whatever the range.
 */
internal class SnapDetents(
    private val targets: List<Float>,
) {
    sealed interface State {
        /** Arms once the value leaves every capture zone, so a drag that starts on a target does not latch. */
        data object Disarmed : State

        /** Outside every capture zone. Captures the moment the value enters one. */
        data object Armed : State

        /** Latched until the raw value is beyond the escape distance. */
        data class Snapped(
            val target: Float,
        ) : State
    }

    /** The result of one [advance] call. [didCapture] is `true` only on the call that captures a detent. */
    data class Advance(
        val value: Float,
        val didCapture: Boolean,
    )

    var state: State = State.Disarmed
        private set

    fun reset() {
        state = State.Disarmed
    }

    /**
     * Call once per drag update with the absolute value before snapping.
     *
     * @param pxPerValue screen px per unit of value. Zero or less disarms and disables snapping for this update.
     * @param captureZonePx half-width of the zone that captures a detent.
     * @param escapeDistancePx distance from the target that releases a detent. Wider than [captureZonePx], so a detent
     * does not flicker at its edge.
     */
    fun advance(
        rawValue: Float,
        pxPerValue: Float,
        captureZonePx: Float,
        escapeDistancePx: Float,
    ): Advance {
        val nearestTarget = targets.minByOrNull { abs(it - rawValue) }
        if (pxPerValue <= 0f || nearestTarget == null) {
            state = State.Disarmed
            return Advance(rawValue, didCapture = false)
        }

        fun px(from: Float) = abs(rawValue - from) * pxPerValue
        val isInCaptureZone = px(nearestTarget) <= captureZonePx
        return when (val current = state) {
            State.Disarmed -> {
                if (!isInCaptureZone) state = State.Armed
                Advance(rawValue, didCapture = false)
            }
            State.Armed -> if (isInCaptureZone) {
                state = State.Snapped(nearestTarget)
                Advance(nearestTarget, didCapture = true)
            } else {
                Advance(rawValue, didCapture = false)
            }
            is State.Snapped -> if (px(current.target) <= escapeDistancePx) {
                Advance(current.target, didCapture = false)
            } else {
                // Beyond the escape distance is outside this target's capture zone, so the next approach captures again.
                state = State.Armed
                Advance(rawValue, didCapture = false)
            }
        }
    }

    companion object {
        /**
         * [detents] on the step grid and inside [range], so a captured value matches the readout. Empty when one step is
         * at least [captureZonePx] wide: such a step lands on the target anyway, and the escape distance would make the
         * steps beside it unreachable.
         */
        fun targets(
            detents: List<Float>,
            step: Float,
            range: ClosedFloatingPointRange<Float>,
            pxPerValue: Float,
            captureZonePx: Float,
        ): List<Float> {
            if (step * pxPerValue >= captureZonePx) return emptyList()
            val bounds = range.start.toDouble()..range.endInclusive.toDouble()
            return detents.map {
                val aligned = NumberStepperMath.snapped(it.toDouble(), step.toDouble(), bounds.start)
                // A detent already on the grid stays exact: from an origin of −1, 0 aligns to 2e-16.
                val onGrid = if (abs(aligned - it) < step / 100) it.toDouble() else aligned
                NumberStepperMath.clamped(onGrid, bounds).toFloat()
            }
        }
    }
}
