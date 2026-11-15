package ly.img.editor.base.dock.options.crop

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ScalePickerTest {
    @get:Rule
    val compose = createComposeRule()

    private var value by mutableFloatStateOf(0f)
    private val changes = mutableListOf<Float>()
    private val finished = mutableListOf<Float>()

    private fun setPicker(initial: Float) {
        value = initial
        compose.setContent {
            ScalePicker(
                value = value,
                valueRange = -45f..45f,
                onValueChange = {
                    changes += it
                    value = it
                },
                onValueChangeFinished = { finished += it },
                contentDescription = "Straighten",
            )
        }
    }

    private fun setProgress(target: Float): Boolean {
        var handled = false
        compose.onNodeWithContentDescription("Straighten").performSemanticsAction(SemanticsActions.SetProgress) {
            handled = it(target)
        }
        return handled
    }

    @Test
    fun aDistantTargetIsWrittenAsOneChange() {
        setPicker(0f)

        assertTrue(setProgress(20f))

        assertEquals(listOf(20f), changes)
        assertEquals(listOf(20f), finished)
    }

    @Test
    fun theCurrentTargetChangesNothing() {
        setPicker(10f)

        assertFalse(setProgress(10f))

        assertEquals(emptyList<Float>(), changes)
        assertEquals(emptyList<Float>(), finished)
    }

    @Test
    fun anOutOfRangeTargetStopsAtTheBound() {
        setPicker(0f)

        setProgress(100f)

        assertEquals(listOf(45f), changes)
    }

    @Test
    fun aTargetIsRoundedToWholeDegrees() {
        setPicker(0f)

        setProgress(12.6f)

        assertEquals(listOf(13f), changes)
    }
}
