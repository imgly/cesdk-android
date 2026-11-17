package ly.img.editor.base.components.numeric

import ly.img.editor.base.components.numeric.NumberStepperMath.Direction.Down
import ly.img.editor.base.components.numeric.NumberStepperMath.Direction.Up
import org.junit.Assert.assertEquals
import org.junit.Test

class NumberStepperMathTest {
    @Test
    fun `decimals follow the step`() {
        assertEquals(0, NumberStepperMath.decimals(1.0))
        assertEquals(0, NumberStepperMath.decimals(5.0))
        assertEquals(1, NumberStepperMath.decimals(0.1))
        assertEquals(2, NumberStepperMath.decimals(0.01))
        assertEquals(2, NumberStepperMath.decimals(0.25))
        assertEquals(2, NumberStepperMath.decimals(0.05))
        assertEquals(3, NumberStepperMath.decimals(0.001))
        assertEquals(4, NumberStepperMath.decimals(0.00001))
        assertEquals(0, NumberStepperMath.decimals(0.0))
    }

    @Test
    fun `a Float step of 0_01 needs 2 decimals, not 3`() {
        assertEquals(2, NumberStepperMath.decimals(0.01f.toDouble()))
        assertEquals(1, NumberStepperMath.decimals(0.1f.toDouble()))
    }

    @Test
    fun `snapped rounds to the nearest grid point from the origin`() {
        assertEquals(16.0, NumberStepperMath.snapped(15.6, step = 10.0, origin = 6.0), EPS)
        assertEquals(0.25, NumberStepperMath.snapped(0.3, step = 0.25, origin = 0.0), EPS)
        assertEquals(0.3, NumberStepperMath.snapped(0.3, step = 0.0, origin = 0.0), EPS)
    }

    @Test
    fun `stepped moves one step on the grid`() {
        assertEquals(23.0, NumberStepperMath.stepped(22.0, Up, step = 1.0, bounds = 6.0..90.0), EPS)
        assertEquals(21.0, NumberStepperMath.stepped(22.0, Down, step = 1.0, bounds = 6.0..90.0), EPS)
    }

    @Test
    fun `stepped moves an off-grid value to the adjacent grid point in the direction`() {
        assertEquals(25.0, NumberStepperMath.stepped(24.5, Up, step = 1.0, bounds = 6.0..90.0), EPS)
        assertEquals(24.0, NumberStepperMath.stepped(24.5, Down, step = 1.0, bounds = 6.0..90.0), EPS)
        // 24.9 is nearest to 25, but Down never raises.
        assertEquals(24.0, NumberStepperMath.stepped(24.9, Down, step = 1.0, bounds = 6.0..90.0), EPS)
    }

    @Test
    fun `stepped treats a value within a hundredth of a step as on the grid`() {
        assertEquals(1.25, NumberStepperMath.stepped(1.0001, Up, step = 0.25, bounds = 0.25..10.0), EPS)
        assertEquals(0.75, NumberStepperMath.stepped(0.9999, Down, step = 0.25, bounds = 0.25..10.0), EPS)
    }

    @Test
    fun `stepped stays at a bound`() {
        assertEquals(90.0, NumberStepperMath.stepped(90.0, Up, step = 1.0, bounds = 6.0..90.0), EPS)
        assertEquals(6.0, NumberStepperMath.stepped(6.0, Down, step = 1.0, bounds = 6.0..90.0), EPS)
        assertEquals(1.4, NumberStepperMath.stepped(1.395, Up, step = 0.01, bounds = -0.15..1.4), EPS)
    }

    @Test
    fun `the grid starts at the lower bound`() {
        assertEquals(7.0, NumberStepperMath.stepped(6.0, Up, step = 1.0, bounds = 6.0..90.0), EPS)
        assertEquals(0.75, NumberStepperMath.stepped(0.5, Up, step = 0.25, bounds = 0.25..10.0), EPS)
    }

    @Test
    fun `a step of zero or less adds its magnitude without a grid`() {
        assertEquals(0.35, NumberStepperMath.stepped(0.3, Up, step = -0.05, bounds = 0.0..1.0), EPS)
        assertEquals(0.25, NumberStepperMath.stepped(0.3, Down, step = -0.05, bounds = 0.0..1.0), EPS)
        assertEquals(0.3, NumberStepperMath.stepped(0.3, Up, step = 0.0, bounds = 0.0..1.0), EPS)
    }

    private companion object {
        const val EPS = 1e-9
    }
}
