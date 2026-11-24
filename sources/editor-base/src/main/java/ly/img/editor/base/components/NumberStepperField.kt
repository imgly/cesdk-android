package ly.img.editor.base.components

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ly.img.editor.base.components.numeric.NumberStepperMath
import ly.img.editor.base.components.numeric.formatNumber
import ly.img.editor.base.components.numeric.withUnit
import ly.img.editor.core.R
import ly.img.editor.core.iconpack.IconPack
import ly.img.editor.core.iconpack.Minus
import ly.img.editor.core.iconpack.Plus
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong

private val ControlHeight = 48.dp
private val CapsuleHeight = 36.dp
private val PillMinWidth = 83.dp
private val PillMaxWidth = 104.dp

// Two 48dp buttons, Material's minimum touch target, and the 1dp divider between them.
private val ButtonsWidth = 97.dp
private val DividerHeight = 18.dp

/**
 * `[ editable value · unit ] [−|+]`. Hold a button to repeat, faster the longer you hold. Tap the value to type one.
 *
 * @param valueRange can end at [Float.POSITIVE_INFINITY] for a value that has no maximum.
 * @param step the amount one press moves the value. The grid starts at the lower bound of [valueRange].
 * @param decimals the decimals shown and kept from typing. Null uses the decimals that [step] needs.
 * @param showsButtons false shows the editable pill alone.
 * @param onEditingChanged reports `true` and `false` once per press, hold or typed edit, never per repeated step, so a
 * caller adds one undo step on `false`. A TalkBack adjustment is one complete change and reports only `false`.
 * @param onTypedValueClamped called when a typed value was outside [valueRange] and was clamped.
 */
@Composable
fun NumberStepperField(
    value: Float,
    onValueChange: (Float) -> Unit,
    title: String,
    valueRange: ClosedFloatingPointRange<Float>,
    step: Float,
    modifier: Modifier = Modifier,
    unit: String? = null,
    decimals: Int? = null,
    enabled: Boolean = true,
    showsButtons: Boolean = true,
    onEditingChanged: (Boolean) -> Unit = {},
    onTypedValueClamped: () -> Unit = {},
) {
    val resolvedDecimals = decimals ?: NumberStepperMath.decimals(step.toDouble())
    val rawBounds = valueRange.start.toDouble()..valueRange.endInclusive.toDouble()
    // Moved inward onto the shown decimals, so a bound off that grid cannot store hidden decimals. A field without
    // buttons keeps the exact bounds, so a typed duration still reaches the exact speed limit.
    val bounds = if (showsButtons) NumberStepperMath.inwardRounded(rawBounds, resolvedDecimals) else rawBounds
    // The caller's value reaches here after an engine round trip, so a hold steps from the last value it sent.
    var current by remember { mutableFloatStateOf(value) }
    var lastValue by remember { mutableFloatStateOf(value) }
    if (lastValue != value) {
        lastValue = value
        current = value
    }
    val displayText = formatNumber(current.toDouble(), resolvedDecimals)
    val valueWithUnit = withUnit(displayText, unit)

    var editingText by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }
    val focusManager = LocalFocusManager.current
    val editScope = rememberCoroutineScope()
    val latestOnValueChange by rememberUpdatedState(onValueChange)
    val latestOnEditingChanged by rememberUpdatedState(onEditingChanged)
    val latestOnTypedValueClamped by rememberUpdatedState(onTypedValueClamped)

    fun setValue(newValue: Float) {
        current = newValue
        latestOnValueChange(newValue)
    }

    // Runs once per edit, whether Done, focus loss or leaving the screen comes first. The caller's `false` handler
    // reads the engine, so the commit comes first.
    fun finishEditing(commit: Boolean = true) {
        val text = editingText ?: return
        editingText = null
        val parsed = text.replace(',', '.').toDoubleOrNull()
        if (commit && parsed != null && parsed.isFinite()) {
            // Rounded to the shown decimals, not snapped to the step, otherwise 24.5 in a field whose buttons move by 1
            // becomes 25.
            val scale = 10.0.pow(resolvedDecimals)
            val rounded = (parsed * scale).roundToLong() / scale
            val clamped = NumberStepperMath.clamped(rounded, bounds)
            if (clamped != rounded) latestOnTypedValueClamped()
            setValue(clamped.toFloat())
        }
        latestOnEditingChanged(false)
    }
    // A sheet closes mid-edit when the selection changes, and the callers' events act on the selection, so a commit
    // here would reach the newly selected block, or crash with none. The edit ends without the typed value.
    val latestFinishEditing by rememberUpdatedState { finishEditing(commit = false) }
    DisposableEffect(Unit) {
        onDispose { latestFinishEditing() }
    }

    fun step(direction: NumberStepperMath.Direction): Boolean {
        val next = NumberStepperMath.stepped(current.toDouble(), direction, step.toDouble(), bounds).toFloat()
        if (next == current) return false
        setValue(next)
        return true
    }

    val typeValueLabel = stringResource(R.string.ly_img_editor_stepper_accessibility_action_type_value)
    val increaseLabel = stringResource(R.string.ly_img_editor_stepper_button_increase, title)
    val decreaseLabel = stringResource(R.string.ly_img_editor_stepper_button_decrease, title)
    // TalkBack moves by the reported range divided by its steps. A range with no maximum has no end to report, so it
    // ends one step above the value, otherwise that move is infinite. A loaded value can lie below the minimum.
    val reportedEnd =
        if (valueRange.endInclusive.isFinite()) bounds.endInclusive.toFloat() else maxOf(current, bounds.start.toFloat()) + step
    val stepCount = ((reportedEnd - bounds.start.toFloat()) / step).roundToInt() - 1
    val valueColor = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
    val capsuleColor = MaterialTheme.colorScheme.surfaceVariant
    val textStyle = MaterialTheme.typography.bodyLarge.copy(
        color = valueColor,
        textAlign = TextAlign.End,
        fontFeatureSettings = "tnum",
    )

    Row(
        modifier = modifier.then(
            // One element for TalkBack. While typing, the text field is its own element.
            if (editingText != null) {
                Modifier
            } else {
                Modifier.clearAndSetSemantics {
                    contentDescription = title
                    stateDescription = valueWithUnit
                    if (!enabled) disabled()
                    progressBarRangeInfo =
                        ProgressBarRangeInfo(current, bounds.start.toFloat()..reportedEnd, stepCount.coerceAtLeast(0))
                    if (enabled) {
                        // SetProgress sets a value. TalkBack sends the next step, other services can send any value.
                        setProgress { target ->
                            val snapped = NumberStepperMath.snapped(target.toDouble(), step.toDouble(), bounds.start)
                            val next = NumberStepperMath.clamped(snapped, bounds).toFloat()
                            if (next == current) return@setProgress false
                            setValue(next)
                            latestOnEditingChanged(false)
                            true
                        }
                        onClick(label = typeValueLabel) {
                            focusRequester.requestFocus()
                            true
                        }
                        if (showsButtons) {
                            customActions = listOf(
                                CustomAccessibilityAction(increaseLabel) {
                                    step(NumberStepperMath.Direction.Up).also { if (it) latestOnEditingChanged(false) }
                                },
                                CustomAccessibilityAction(decreaseLabel) {
                                    step(NumberStepperMath.Direction.Down).also { if (it) latestOnEditingChanged(false) }
                                },
                            )
                        }
                    }
                }
            },
        ),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .widthIn(min = PillMinWidth, max = PillMaxWidth)
                .height(ControlHeight)
                // A tap anywhere on the pill types a value. Not clickable, which would add a second keyboard stop before
                // the text field.
                .pointerInput(enabled) {
                    if (enabled) detectTapGestures { focusRequester.requestFocus() }
                },
            contentAlignment = Alignment.Center,
        ) {
            Capsule(color = capsuleColor)
            Row(
                modifier = Modifier.padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                BasicTextField(
                    // Focus empties the field and shows the value as the placeholder, so typing replaces the value.
                    value = editingText ?: displayText,
                    onValueChange = { text -> editingText = text.filter { it.isDigit() || it in ".,-" } },
                    enabled = enabled,
                    singleLine = true,
                    textStyle = textStyle,
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    keyboardOptions = KeyboardOptions(
                        // The decimal keyboard has no minus key on every IME, so a signed range gets the text keyboard.
                        keyboardType = if (valueRange.start < 0) KeyboardType.Text else KeyboardType.Decimal,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .focusRequester(focusRequester)
                        // A hardware Enter commits on key-up. Committing on key-down moves focus, and the key-up then
                        // clicks the next focused control, such as the navigation bar's Back button.
                        .onPreviewKeyEvent { event ->
                            if (event.key != Key.Enter && event.key != Key.NumPadEnter) return@onPreviewKeyEvent false
                            if (event.type == KeyEventType.KeyUp) focusManager.clearFocus()
                            true
                        }
                        .semantics { contentDescription = title }
                        .onFocusChanged { state ->
                            if (state.isFocused && editingText == null) {
                                editingText = ""
                                latestOnEditingChanged(true)
                            } else if (!state.isFocused && editingText != null) {
                                // A frame later, because removing the field also takes its focus: the scope is
                                // cancelled then, and `onDispose` ends the edit without the typed value.
                                editScope.launch { finishEditing() }
                            }
                        },
                    decorationBox = { innerTextField ->
                        Box(contentAlignment = Alignment.CenterEnd) {
                            if (editingText?.isEmpty() == true) {
                                Text(
                                    text = displayText,
                                    style = textStyle.copy(color = MaterialTheme.colorScheme.onSurfaceVariant),
                                    maxLines = 1,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                if (unit != null) {
                    Text(text = unit, style = textStyle, maxLines = 1, softWrap = false)
                }
            }
        }
        if (showsButtons) {
            val onPress = {
                // A press while typing ends the edit first, so the step starts from the typed value.
                if (editingText != null) {
                    finishEditing()
                    focusManager.clearFocus()
                }
            }
            Box(
                modifier = Modifier
                    .width(ButtonsWidth)
                    .height(ControlHeight),
                contentAlignment = Alignment.Center,
            ) {
                Capsule(color = capsuleColor)
                Row(Modifier.matchParentSize(), verticalAlignment = Alignment.CenterVertically) {
                    StepperButton(
                        icon = IconPack.Minus,
                        enabled = enabled && current > bounds.start.toFloat(),
                        onPress = onPress,
                        onTrigger = { step(NumberStepperMath.Direction.Down) },
                        onPressingChanged = { latestOnEditingChanged(it) },
                        modifier = Modifier.weight(1f),
                    )
                    Divider(
                        Modifier
                            .width(1.dp)
                            .height(DividerHeight),
                        color = MaterialTheme.colorScheme.outlineVariant,
                    )
                    StepperButton(
                        icon = IconPack.Plus,
                        enabled = enabled && current < bounds.endInclusive.toFloat(),
                        onPress = onPress,
                        onTrigger = { step(NumberStepperMath.Direction.Up) },
                        onPressingChanged = { latestOnEditingChanged(it) },
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}

/** The visible capsule, vertically centred in the 48dp touch height. */
@Composable
private fun BoxScope.Capsule(color: Color) {
    Box(
        Modifier
            .matchParentSize()
            .padding(vertical = (ControlHeight - CapsuleHeight) / 2)
            .background(color, RoundedCornerShape(50)),
    )
}

/**
 * A `−` or `+` that steps once on a tap, and repeats while held. The repeat stops on release, when the button leaves the
 * screen, at a bound and when the button is disabled mid-hold. Each repeat moves exactly one step: the grid starts at
 * the lower bound, so a ×10 jump from a value off the grid would leave the grid.
 */
@Composable
private fun StepperButton(
    icon: ImageVector,
    enabled: Boolean,
    onPress: () -> Unit,
    onTrigger: () -> Boolean,
    onPressingChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val latestEnabled by rememberUpdatedState(enabled)
    val latestOnPress by rememberUpdatedState(onPress)
    val latestOnTrigger by rememberUpdatedState(onTrigger)
    val latestOnPressingChanged by rememberUpdatedState(onPressingChanged)
    val interactionSource = remember { MutableInteractionSource() }
    val scope = rememberCoroutineScope()
    // The system's long-press timeout, which the user can change in the accessibility settings.
    val firstRepeatDelayMs = LocalViewConfiguration.current.longPressTimeoutMillis
    Box(
        modifier = modifier
            .fillMaxHeight()
            // 20dp, the radius of Material 3's IconButton, so the circle fits the 48dp row of the card that clips it.
            .indication(interactionSource, rememberRipple(bounded = false, radius = 20.dp))
            // A key press is one step and one edit, like a tap. Repeats of a held key are ignored, otherwise each adds
            // an undo step. The handler sits before focusable, so it receives the focused button's keys.
            .onKeyEvent { event ->
                if (!latestEnabled || event.key !in ActivationKeys) return@onKeyEvent false
                if (event.type == KeyEventType.KeyUp) {
                    latestOnPress()
                    latestOnPressingChanged(true)
                    latestOnTrigger()
                    latestOnPressingChanged(false)
                }
                true
            }
            .focusable(enabled = enabled, interactionSource = interactionSource)
            // Keyed on Unit, so a button that disables at a bound mid-hold keeps this gesture, and its release still
            // reports the end of the hold once.
            .pointerInput(Unit) {
                awaitEachGesture {
                    val down = awaitFirstDown()
                    if (!latestEnabled) return@awaitEachGesture
                    down.consume()
                    latestOnPress()
                    val press = PressInteraction.Press(down.position)
                    interactionSource.tryEmit(press)
                    latestOnPressingChanged(true)
                    var didRepeat = false
                    var repeat: Job? = null
                    try {
                        repeat = scope.launch {
                            delay(firstRepeatDelayMs)
                            didRepeat = true
                            var repeats = 0
                            while (latestEnabled && latestOnTrigger()) {
                                delay(repeatIntervalMs(repeats++))
                            }
                        }
                        val up = waitForUpOrCancellation()
                        repeat.cancel()
                        // A tap that follows a long press does not fire a second step.
                        if (up != null && !didRepeat && latestEnabled) latestOnTrigger()
                    } finally {
                        repeat?.cancel()
                        interactionSource.tryEmit(PressInteraction.Release(press))
                        latestOnPressingChanged(false)
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
            },
        )
    }
}

private val ActivationKeys = setOf(Key.Enter, Key.NumPadEnter, Key.Spacebar, Key.DirectionCenter)

private fun repeatIntervalMs(repeats: Int): Long = when {
    repeats < 10 -> 120L
    repeats < 40 -> 60L
    else -> 30L
}
