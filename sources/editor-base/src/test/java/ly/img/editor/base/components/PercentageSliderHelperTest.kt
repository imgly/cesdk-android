package ly.img.editor.base.components

import ly.img.editor.base.components.numeric.formatNumber
import ly.img.editor.base.components.numeric.withUnit
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Locale

class PercentageSliderHelperTest {
    @Test
    fun `formatValue drops trailing zeros`() {
        assertEquals("4", PercentageSliderHelper.formatValue(4f, step = 0.01f, locale = Locale.US))
        assertEquals("4.2", PercentageSliderHelper.formatValue(4.2f, step = 0.01f, locale = Locale.US))
        assertEquals("0.05", PercentageSliderHelper.formatValue(0.05f, step = 0.01f, locale = Locale.US))
        assertEquals("0.6", PercentageSliderHelper.formatValue(0.6f, step = 0.05f, locale = Locale.US))
    }

    @Test
    fun `formatValue shows at most 2 decimals`() {
        assertEquals("0.01", PercentageSliderHelper.formatValue(0.012f, step = 0.001f, locale = Locale.US))
    }

    @Test
    fun `formatValue is locale-aware and has no grouping`() {
        assertEquals("24,5", PercentageSliderHelper.formatValue(24.5f, step = 0.1f, locale = Locale.GERMANY))
        assertEquals("1200", PercentageSliderHelper.formatValue(1200f, step = 1f, locale = Locale.US))
    }

    @Test
    fun `a value that rounds to zero reads 0`() {
        assertEquals("0", formatNumber(-0.001, decimals = 2, locale = Locale.US))
    }

    @Test
    fun `a percentage readout shows the rounded number without a percent sign`() {
        assertEquals("50", readout(0.5f, min = 0f, max = 1f, step = 0.01f))
        assertEquals("-100", readout(-1f, min = -1f, max = 1f, step = 0.01f))
        assertEquals("100", readout(1f, min = -1f, max = 1f, step = 0.01f))
        assertEquals("0", readout(-0.004f, min = -1f, max = 1f, step = 0.01f))
    }

    @Test
    fun `talkback reads a percentage with a percent sign, and any other value as shown`() {
        assertEquals("50%", spoken(0.5f, min = 0f, max = 1f, step = 0.01f))
        assertEquals("-100%", spoken(-1f, min = -1f, max = 1f, step = 0.01f))
        assertEquals("4", spoken(4f, min = 0f, max = 10f, step = 0.1f))
    }

    @Test
    fun `a readout outside a percentage range shows the value`() {
        assertEquals("4", readout(4f, min = 0f, max = 10f, step = 0.1f))
    }

    @Test
    fun `disableAutoPercentage shows the plain value inside the percentage range`() {
        assertEquals("0.1", readout(0.1f, min = 0.1f, max = 1f, step = 0.009f, disableAutoPercentage = true))
        assertEquals("0", readout(0.1f, min = 0.1f, max = 1f, step = 0.009f))
    }

    @Test
    fun `the widest readout includes the value one step inside each end`() {
        // 0…100 in 0.5 steps: 99.5 is wider than both ends.
        assertEquals("99.5", PercentageSliderHelper.widestReadoutText(0f, 100f, 0.5f, false, Locale.US))
        // A percentage range keeps its widest end.
        assertEquals("-100", PercentageSliderHelper.widestReadoutText(-1f, 1f, 0.1f, false, Locale.US))
    }

    private fun readout(
        value: Float,
        min: Float,
        max: Float,
        step: Float,
        disableAutoPercentage: Boolean = false,
    ) = PercentageSliderHelper.readoutText(value, min, max, step, disableAutoPercentage, Locale.US)

    private fun spoken(
        value: Float,
        min: Float,
        max: Float,
        step: Float,
    ) = PercentageSliderHelper.spokenText(value, min, max, step, disableAutoPercentage = false, locale = Locale.US)

    @Test
    fun `the default step is a hundredth of the range, or 1 percent of a symmetric percentage`() {
        assertEquals(0.01f, PercentageSliderHelper.stepFromMinMax(0f, 1f), 1e-7f)
        assertEquals(0.01f, PercentageSliderHelper.stepFromMinMax(-1f, 1f), 1e-7f)
        assertEquals(0.84f, PercentageSliderHelper.stepFromMinMax(6f, 90f), 1e-6f)
    }

    @Test
    fun `withUnit puts no space before percent and one before any other unit`() {
        assertEquals("50%", withUnit("50", "%"))
        assertEquals("24 pt", withUnit("24", "pt"))
        assertEquals("24", withUnit("24", null))
    }
}
