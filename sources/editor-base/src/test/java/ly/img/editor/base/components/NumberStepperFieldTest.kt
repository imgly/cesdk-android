package ly.img.editor.base.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NumberStepperFieldTest {
    @get:Rule
    val compose = createComposeRule()

    private var value by mutableFloatStateOf(0f)
    private val changes = mutableListOf<Float>()
    private val editing = mutableListOf<Boolean>()

    private fun setField(
        initial: Float,
        range: ClosedFloatingPointRange<Float> = 6f..90f,
    ) {
        value = initial
        compose.setContent {
            NumberStepperField(
                value = value,
                onValueChange = {
                    changes += it
                    value = it
                },
                title = TITLE,
                valueRange = range,
                step = 1f,
                unit = "pt",
                decimals = 1,
                onEditingChanged = { editing += it },
            )
        }
    }

    private fun field() = compose.onNodeWithContentDescription(TITLE)

    private fun pressAt(
        xFromEnd: Float,
        holdMs: Long,
    ) {
        compose.mainClock.autoAdvance = false
        field().performTouchInput {
            down(Offset(width - xFromEnd.dp.toPx(), centerY))
        }
        compose.mainClock.advanceTimeBy(holdMs)
        field().performTouchInput { up() }
        compose.mainClock.autoAdvance = true
        compose.waitForIdle()
    }

    @Test
    fun aTapOnPlusStepsOnceAndEndsTheEditOnce() {
        setField(22f)

        pressAt(xFromEnd = PLUS_FROM_END, holdMs = 50)

        assertEquals(listOf(23f), changes)
        assertEquals(listOf(true, false), editing)
    }

    @Test
    fun minusDoesNothingAtTheMinimum() {
        setField(6f)

        pressAt(xFromEnd = MINUS_FROM_END, holdMs = 50)

        assertEquals(emptyList<Float>(), changes)
        assertEquals(emptyList<Boolean>(), editing)
    }

    /** Each button is 48dp wide, Material's minimum touch target, so a tap near the outer edge of minus steps. */
    @Test
    fun aTapNearTheOuterEdgeOfMinusSteps() {
        setField(22f)

        pressAt(xFromEnd = 96f, holdMs = 50)

        assertEquals(listOf(21f), changes)
    }

    @Test
    fun aHoldRepeatsAndEndsTheEditOnce() {
        setField(22f)

        // The long-press timeout (400 ms on this SDK) to the first repeat, then 120 ms each: about 6 steps in 1 s.
        pressAt(xFromEnd = PLUS_FROM_END, holdMs = 1_000)

        assertTrue("expected several steps, got $changes", changes.size > 3)
        assertEquals((23..(22 + changes.size)).map { it.toFloat() }, changes)
        assertEquals(listOf(true, false), editing)
    }

    @Test
    fun aHoldStopsAtTheMaximumAndStillEndsTheEditOnce() {
        setField(88f)

        pressAt(xFromEnd = PLUS_FROM_END, holdMs = 2_000)

        assertEquals(listOf(89f, 90f), changes)
        assertEquals(listOf(true, false), editing)
    }

    @Test
    fun aTypedValueIsKeptOffTheStepGrid() {
        setField(22f)

        field().performSemanticsAction(SemanticsActions.OnClick)
        compose.onNode(hasSetTextAction()).performTextInput("24,5")
        compose.onNode(hasSetTextAction()).performImeAction()

        assertEquals(listOf(24.5f), changes)
        assertEquals(listOf(true, false), editing)
    }

    /** A maximum off the shown decimals clamps a typed value to the shown grid, so the next press moves the number. */
    @Test
    fun aTypedValueAboveAnOffGridMaximumStopsAtTheLastShownValue() {
        setField(5f, range = 0f..6.04f)

        field().performSemanticsAction(SemanticsActions.OnClick)
        compose.onNode(hasSetTextAction()).performTextInput("12")
        compose.onNode(hasSetTextAction()).performImeAction()
        // The commit ends edit mode on the next frame, and `pressAt` stops the clock, so wait for that frame first.
        compose.waitForIdle()
        pressAt(xFromEnd = MINUS_FROM_END, holdMs = 50)
        pressAt(xFromEnd = PLUS_FROM_END, holdMs = 50)
        pressAt(xFromEnd = PLUS_FROM_END, holdMs = 50)

        assertEquals(listOf(6f, 5f, 6f), changes)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aHardwareEnterCommitsOnKeyUpSoNoOtherControlReceivesTheKeyUp() {
        setField(22f)

        field().performSemanticsAction(SemanticsActions.OnClick)
        compose.onNode(hasSetTextAction()).performTextInput("30")
        compose.onNode(hasSetTextAction()).performKeyInput { keyDown(Key.Enter) }

        // Committing on key-down moves focus, and the next focused control, such as Back, clicks on the key-up.
        assertEquals(emptyList<Float>(), changes)

        compose.onNode(hasSetTextAction()).performKeyInput { keyUp(Key.Enter) }

        assertEquals(listOf(30f), changes)
        assertEquals(listOf(true, false), editing)
    }

    @Test
    fun aFieldRemovedMidEditDropsTheTypedValue() {
        var shown by mutableStateOf(true)
        value = 22f
        compose.setContent {
            if (shown) {
                NumberStepperField(
                    value = value,
                    onValueChange = { changes += it },
                    title = TITLE,
                    valueRange = 6f..90f,
                    step = 1f,
                    onEditingChanged = { editing += it },
                )
            }
        }

        field().performSemanticsAction(SemanticsActions.OnClick)
        compose.onNode(hasSetTextAction()).performTextInput("40")
        // The sheet closes because the selection changed, so a commit now would reach the newly selected block.
        shown = false
        compose.waitForIdle()

        assertEquals(emptyList<Float>(), changes)
        assertEquals(listOf(true, false), editing)
    }

    @Test
    fun unparseableTextKeepsTheValue() {
        setField(22f)

        field().performSemanticsAction(SemanticsActions.OnClick)
        compose.onNode(hasSetTextAction()).performTextInput("1-2")
        compose.onNode(hasSetTextAction()).performImeAction()

        assertEquals(emptyList<Float>(), changes)
        assertEquals(22f, value)
        assertEquals(listOf(true, false), editing)
    }

    @Test
    fun aTalkBackAdjustmentStepsOnceAndReportsOnlyTheEnd() {
        setField(22f)

        field().performSemanticsAction(SemanticsActions.SetProgress) { it(23f) }

        assertEquals(listOf(23f), changes)
        assertEquals(listOf(false), editing)
    }

    @Test
    fun aDistantProgressTargetIsWrittenAsOneChange() {
        setField(22f)

        var handled = false
        field().performSemanticsAction(SemanticsActions.SetProgress) { handled = it(30f) }

        assertTrue(handled)
        assertEquals(listOf(30f), changes)
        assertEquals(listOf(false), editing)
    }

    @Test
    fun theCurrentProgressTargetChangesNothing() {
        setField(22f)

        var handled = true
        field().performSemanticsAction(SemanticsActions.SetProgress) { handled = it(22f) }

        assertFalse(handled)
        assertEquals(emptyList<Float>(), changes)
        assertEquals(emptyList<Boolean>(), editing)
    }

    @Test
    fun anOutOfRangeProgressTargetStopsAtTheBound() {
        setField(22f)

        field().performSemanticsAction(SemanticsActions.SetProgress) { it(500f) }

        assertEquals(listOf(90f), changes)
    }

    @Test
    fun aRangeWithNoMaximumKeepsATypedValue() {
        setField(22f, range = 6f..Float.POSITIVE_INFINITY)

        field().performSemanticsAction(SemanticsActions.OnClick)
        compose.onNode(hasSetTextAction()).performTextInput("200")
        compose.onNode(hasSetTextAction()).performImeAction()

        assertEquals(listOf(200f), changes)
    }

    /** TalkBack moves a seek bar by its reported range divided by its steps, so that move must be one step. */
    @Test
    fun aTalkBackMoveOnARangeWithNoMaximumIsOneStep() {
        setField(22f, range = 6f..Float.POSITIVE_INFINITY)

        val info = field().fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo]
        val move = (info.range.endInclusive - info.range.start) / (info.steps + 1)
        field().performSemanticsAction(SemanticsActions.SetProgress) { it(info.current + move) }

        assertEquals(listOf(23f), changes)
    }

    /** A loaded value can lie below the minimum, and the reported range must still run upwards. */
    @Test
    fun aValueBelowTheMinimumOfARangeWithNoMaximumReportsARangeThatRunsUpwards() {
        setField(4f, range = 6f..Float.POSITIVE_INFINITY)

        val range = field().fetchSemanticsNode().config[SemanticsProperties.ProgressBarRangeInfo].range

        assertEquals(6f..7f, range)
    }

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun aKeyboardStepsOnceWithEnterOnPlus() {
        setField(22f)

        // Focus the text field, then Tab to minus and to plus. A test window starts with nothing focused, and Tab
        // moves focus only from a focused element.
        field().performSemanticsAction(SemanticsActions.OnClick)
        compose.onNode(hasSetTextAction()).performKeyInput { pressKey(Key.Tab) }
        compose.onRoot().performKeyInput { pressKey(Key.Tab) }
        compose.onRoot().performKeyInput { pressKey(Key.Enter) }

        assertEquals(listOf(23f), changes)
        // Tabbing through the text field opens and closes one edit, then the key press is one step.
        assertEquals(listOf(true, false, true, false), editing)
    }

    @Test
    fun aTapOnTheEmptyPartOfThePillOpensTyping() {
        setField(22f)

        // The pill ends 100dp from the end (buttons 94dp, gap 6dp) and is at least 83dp wide.
        field().performTouchInput { click(Offset(width - 175.dp.toPx(), centerY)) }

        assertEquals(listOf(true), editing)
    }

    private companion object {
        const val TITLE = "Font Size"

        // The buttons sit at the end: `[pill] 6dp [− | +]`, 94dp wide in total.
        const val PLUS_FROM_END = 23f
        const val MINUS_FROM_END = 70f
    }
}
