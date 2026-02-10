package ly.img.editor.base.dock.options.speed

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import ly.img.editor.base.dock.BottomSheetContent
import ly.img.editor.base.timeline.state.TimelineConfiguration
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.base.ui.Event
import ly.img.editor.compose.material3.TextFieldDefaults
import ly.img.editor.core.R
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.iconpack.IconPack
import ly.img.editor.core.iconpack.Minus
import ly.img.editor.core.iconpack.Plus
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.theme.EditorTheme
import ly.img.editor.core.theme.surface2
import ly.img.editor.core.ui.SheetHeader
import ly.img.editor.core.ui.UiDefaults
import ly.img.editor.core.ui.sheetScrollableContentModifier
import ly.img.editor.core.ui.utils.ThemePreview
import java.text.NumberFormat
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

private const val SPEED_COMPARE_EPSILON = 0.001f
private const val SPEED_STEP = 0.25f
private const val SPEED_INPUT_DECIMALS = 2
private const val DURATION_INPUT_DECIMALS = 2
private const val MIN_SPEED = 0.25f
private val SPEED_INPUT_WIDTH = 84.dp
private val DURATION_INPUT_WIDTH = 96.dp

private data class DurationState(
    val isEnabled: Boolean,
    val normalizedDuration: Double?,
    val minDurationSeconds: Double?,
    val maxDurationSeconds: Double?,
    val fallbackDuration: String,
)

@Composable
fun SpeedSheet(
    uiState: SpeedUiState,
    onEvent: (EditorEvent) -> Unit,
) {
    val speedNumberFormat =
        remember {
            NumberFormat.getNumberInstance().apply {
                minimumFractionDigits = SPEED_INPUT_DECIMALS
                maximumFractionDigits = SPEED_INPUT_DECIMALS
            }
        }
    val durationNumberFormat =
        remember {
            NumberFormat.getNumberInstance().apply {
                minimumFractionDigits = DURATION_INPUT_DECIMALS
                maximumFractionDigits = DURATION_INPUT_DECIMALS
            }
        }

    fun formatSpeedValue(speed: Float): String = speedNumberFormat.format(speed)

    fun formatDurationValue(durationSeconds: Double): String = durationNumberFormat.format(durationSeconds)
    val maxSpeed = uiState.maxSpeed
    val previousSpeedProvider = remember(uiState.speed) { { previousStepValue(uiState.speed) } }
    val nextSpeedProvider = remember(uiState.speed, maxSpeed) { { nextStepValue(uiState.speed, maxSpeed) } }
    var speedInput by remember { mutableStateOf(formatSpeedValue(uiState.speed)) }
    var isSpeedEditing by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.speed, isSpeedEditing) {
        if (!isSpeedEditing) {
            speedInput = formatSpeedValue(uiState.speed)
        }
    }
    val durationState =
        remember(
            uiState.durationSeconds,
            uiState.speed,
            maxSpeed,
        ) {
            val normalizedDuration = uiState.durationSeconds?.let { it * uiState.speed.toDouble() }
            val minDurationSeconds =
                normalizedDuration
                    ?.let { it / maxSpeed }
                    ?.let { maxOf(it, TimelineConfiguration.minClipDuration.inWholeMilliseconds / 1000.0) }
            val maxDurationSeconds = normalizedDuration?.let { it / MIN_SPEED }
            val fallbackDuration = uiState.durationSeconds?.let(::formatDurationValue) ?: ""
            DurationState(
                isEnabled = uiState.durationSeconds != null,
                normalizedDuration = normalizedDuration,
                minDurationSeconds = minDurationSeconds,
                maxDurationSeconds = maxDurationSeconds,
                fallbackDuration = fallbackDuration,
            )
        }
    var durationInput by remember { mutableStateOf(uiState.durationSeconds?.let(::formatDurationValue) ?: "") }
    var isDurationEditing by remember { mutableStateOf(false) }
    LaunchedEffect(uiState.durationSeconds, isDurationEditing) {
        if (!isDurationEditing) {
            durationInput = uiState.durationSeconds?.let(::formatDurationValue) ?: ""
        }
    }
    val speedSuffix = stringResource(id = R.string.ly_img_editor_sheet_speed_suffix)
    val durationSuffix = stringResource(id = R.string.ly_img_editor_sheet_duration_suffix)

    fun applySpeed(newSpeed: Float) {
        if (!isSpeedMatch(newSpeed, uiState.speed)) {
            onEvent(BlockEvent.OnPlaybackSpeedChange(newSpeed))
            onEvent(BlockEvent.OnChangeFinish)
        }
    }

    fun commitSpeedInput() {
        val parsedSpeed = parseDecimalInput(speedInput)
        if (parsedSpeed == null) {
            speedInput = formatSpeedValue(uiState.speed)
            return
        }
        val clampedSpeed = parsedSpeed.toFloat().coerceIn(MIN_SPEED, maxSpeed)
        applySpeed(clampedSpeed)
        speedInput = formatSpeedValue(clampedSpeed)
    }

    fun commitDurationInput() {
        val baseDuration = durationState.normalizedDuration
        val minDuration = durationState.minDurationSeconds
        val maxDuration = durationState.maxDurationSeconds
        if (baseDuration == null || minDuration == null || maxDuration == null) {
            durationInput = durationState.fallbackDuration
            return
        }
        val parsedDuration = parseDecimalInput(durationInput)
        if (parsedDuration == null || parsedDuration <= 0.0) {
            durationInput = durationState.fallbackDuration
            return
        }
        val wasClamped = parsedDuration < minDuration || parsedDuration > maxDuration
        val clampedDuration = parsedDuration.coerceIn(minDuration, maxDuration)
        val newSpeed = (baseDuration / clampedDuration).coerceIn(MIN_SPEED.toDouble(), maxSpeed.toDouble())
        applySpeed(newSpeed.toFloat())
        durationInput = formatDurationValue(clampedDuration)
        if (wasClamped) {
            onEvent(Event.OnToast(R.string.ly_img_editor_notification_speed_clamped_duration))
        }
    }

    Column {
        SheetHeader(
            title = stringResource(id = R.string.ly_img_editor_sheet_clip_speed_title),
            onClose = { onEvent(EditorEvent.Sheet.Close(animate = true)) },
        )
        Card(
            Modifier.sheetScrollableContentModifier(),
            colors = UiDefaults.cardColors,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth(),
            ) {
                SpeedStepperRow(
                    title = stringResource(id = R.string.ly_img_editor_sheet_speed_label),
                    value = speedInput,
                    previousValueProvider = previousSpeedProvider,
                    nextValueProvider = nextSpeedProvider,
                    onValueChange = { speedInput = sanitizeDecimalInput(it, SPEED_INPUT_DECIMALS) },
                    onCommit = { commitSpeedInput() },
                    onFocusChanged = { isSpeedEditing = it },
                    suffix = speedSuffix,
                ) { newSpeed ->
                    applySpeed(newSpeed)
                    speedInput = formatSpeedValue(newSpeed)
                }

                Divider(Modifier.padding(horizontal = 16.dp))

                DurationRow(
                    title = stringResource(id = R.string.ly_img_editor_sheet_duration_label),
                    value = durationInput,
                    isEnabled = durationState.isEnabled,
                    onValueChange = { durationInput = sanitizeDecimalInput(it, DURATION_INPUT_DECIMALS) },
                    onCommit = { commitDurationInput() },
                    onFocusChanged = { isDurationEditing = it },
                    suffix = durationSuffix,
                )
            }
        }
    }
}

@Composable
private fun SpeedStepperRow(
    title: String,
    value: String,
    previousValueProvider: () -> Float?,
    nextValueProvider: () -> Float?,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    suffix: String,
    onSpeedSelected: (Float) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        SpeedStepper(
            value = value,
            previousValueProvider = previousValueProvider,
            nextValueProvider = nextValueProvider,
            onValueChange = onValueChange,
            onCommit = onCommit,
            onFocusChanged = onFocusChanged,
            suffix = suffix,
            onSpeedSelected = onSpeedSelected,
        )
    }
}

@Composable
private fun SpeedStepper(
    value: String,
    previousValueProvider: () -> Float?,
    nextValueProvider: () -> Float?,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    suffix: String,
    onSpeedSelected: (Float) -> Unit,
) {
    val previousValue = previousValueProvider()
    val nextValue = nextValueProvider()
    Row(
        modifier = Modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StepperIconButton(
            icon = IconPack.Minus,
            enabled = previousValue != null,
            valueProvider = previousValueProvider,
            onValueSelected = onSpeedSelected,
        )
        NumericInputPill(
            value = value,
            placeholder = "--",
            suffix = suffix,
            enabled = true,
            onValueChange = onValueChange,
            onCommit = onCommit,
            onFocusChanged = onFocusChanged,
            modifier = Modifier.width(SPEED_INPUT_WIDTH),
        )
        StepperIconButton(
            icon = IconPack.Plus,
            enabled = nextValue != null,
            valueProvider = nextValueProvider,
            onValueSelected = onSpeedSelected,
        )
    }
}

@Composable
private fun DurationRow(
    title: String,
    value: String,
    isEnabled: Boolean,
    suffix: String,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 20.dp, top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
        )
        NumericInputPill(
            value = value,
            placeholder = "--",
            suffix = suffix,
            enabled = isEnabled,
            onValueChange = onValueChange,
            onCommit = onCommit,
            onFocusChanged = onFocusChanged,
            modifier = Modifier.width(DURATION_INPUT_WIDTH),
        )
    }
}

@Composable
private fun StepperIconButton(
    icon: ImageVector,
    enabled: Boolean,
    valueProvider: () -> Float?,
    onValueSelected: (Float) -> Unit,
) {
    val currentValueProvider by rememberUpdatedState(valueProvider)
    val currentOnValueSelected by rememberUpdatedState(onValueSelected)
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    var suppressClick by remember { mutableStateOf(false) }

    LaunchedEffect(isPressed, enabled) {
        if (!enabled) return@LaunchedEffect
        if (isPressed) {
            delay(500)
            if (!isPressed) return@LaunchedEffect
            suppressClick = true
            while (isPressed && isActive) {
                val nextValue = currentValueProvider()
                if (nextValue == null) {
                    break
                }
                currentOnValueSelected(nextValue)
                delay(250)
            }
        } else if (suppressClick) {
            delay(100)
            suppressClick = false
        }
    }

    IconButton(
        onClick = {
            if (!suppressClick) {
                currentValueProvider()?.let { currentOnValueSelected(it) }
            }
            suppressClick = false
        },
        enabled = enabled,
        colors =
            IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                disabledContainerColor = MaterialTheme.colorScheme.surface2,
                disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f),
            ),
        interactionSource = interactionSource,
    ) {
        Icon(icon, contentDescription = null)
    }
}

private fun isSpeedMatch(
    optionSpeed: Float,
    currentSpeed: Float,
): Boolean = abs(optionSpeed - currentSpeed) <= SPEED_COMPARE_EPSILON

@Composable
private fun NumericInputPill(
    value: String,
    placeholder: String,
    suffix: String,
    enabled: Boolean,
    onValueChange: (String) -> Unit,
    onCommit: () -> Unit,
    onFocusChanged: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    minHeight: Dp = 40.dp,
) {
    val focusManager = LocalFocusManager.current
    val textColor =
        if (enabled) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        }
    val interactionSource = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(8.dp)
    val visualTransformation = suffixVisualTransformation(suffix)
    val colors =
        TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface2,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface2,
            disabledContainerColor = MaterialTheme.colorScheme.surface2,
            errorContainerColor = MaterialTheme.colorScheme.surface2,
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent,
            disabledIndicatorColor = Color.Transparent,
            errorIndicatorColor = Color.Transparent,
        )
    var wasFocused by remember { mutableStateOf(false) }
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        enabled = enabled,
        singleLine = true,
        textStyle = MaterialTheme.typography.labelLarge.copy(color = textColor, textAlign = TextAlign.End),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal, imeAction = ImeAction.Done),
        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
        interactionSource = interactionSource,
        cursorBrush = SolidColor(textColor),
        visualTransformation = visualTransformation,
        modifier =
            modifier
                .heightIn(min = minHeight)
                .onFocusChanged { state ->
                    when {
                        state.isFocused && !wasFocused -> {
                            wasFocused = true
                            onFocusChanged(true)
                        }
                        !state.isFocused && wasFocused -> {
                            wasFocused = false
                            onFocusChanged(false)
                            onCommit()
                        }
                    }
                },
    ) { innerTextField ->
        TextFieldDefaults.DecorationBox(
            value = value,
            innerTextField = innerTextField,
            enabled = enabled,
            singleLine = true,
            visualTransformation = visualTransformation,
            interactionSource = interactionSource,
            placeholder = {
                Text(
                    placeholder,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.End,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            },
            shape = shape,
            colors = colors,
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 10.dp),
        )
    }
}

private fun suffixVisualTransformation(suffix: String): VisualTransformation {
    if (suffix.isEmpty()) return VisualTransformation.None
    return VisualTransformation { text ->
        if (text.text.isEmpty()) {
            return@VisualTransformation TransformedText(text, OffsetMapping.Identity)
        }
        val transformed = text.text + " " + suffix
        val offsetMapping =
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int): Int = offset

                override fun transformedToOriginal(offset: Int): Int = min(offset, text.text.length)
            }
        TransformedText(AnnotatedString(transformed), offsetMapping)
    }
}

private fun sanitizeDecimalInput(
    input: String,
    maxDecimals: Int,
): String {
    val filtered = input.filter { it.isDigit() || it == '.' || it == ',' }
    val separatorIndex = filtered.indexOfFirst { it == '.' || it == ',' }
    if (separatorIndex == -1) {
        return filtered
    }
    val separator = filtered[separatorIndex]
    val prefix = filtered.substring(0, separatorIndex)
    val suffix = filtered.substring(separatorIndex + 1).filter { it.isDigit() }.take(maxDecimals)
    return if (suffix.isEmpty()) {
        "$prefix$separator"
    } else {
        "$prefix$separator$suffix"
    }
}

private fun parseDecimalInput(input: String): Double? = input.replace(',', '.').toDoubleOrNull()

private fun previousStepValue(current: Float): Float? {
    if (current <= MIN_SPEED + SPEED_COMPARE_EPSILON) return null
    val stepIndex = current / SPEED_STEP
    val targetStep =
        if (isOnStep(current)) {
            (stepIndex - 1f).coerceAtLeast(0f)
        } else {
            floor(stepIndex.toDouble()).toFloat()
        }
    val previous = targetStep * SPEED_STEP
    return previous.coerceAtLeast(MIN_SPEED)
}

private fun nextStepValue(
    current: Float,
    maxSpeed: Float,
): Float? {
    if (current >= maxSpeed - SPEED_COMPARE_EPSILON) return null
    val stepIndex = current / SPEED_STEP
    val targetStep =
        if (isOnStep(current)) {
            stepIndex + 1f
        } else {
            ceil(stepIndex.toDouble()).toFloat()
        }
    val next = targetStep * SPEED_STEP
    return next.coerceAtMost(maxSpeed)
}

private fun isOnStep(value: Float): Boolean {
    val stepIndex = value / SPEED_STEP
    return abs(stepIndex - stepIndex.roundToInt()) <= SPEED_COMPARE_EPSILON
}

class SpeedBottomSheetContent(
    override val type: SheetType,
    val uiState: SpeedUiState,
) : BottomSheetContent

@ThemePreview
@Composable
private fun SpeedSheetPreview() {
    EditorTheme {
        SpeedSheet(
            uiState =
                SpeedUiState(
                    speed = 1f,
                    durationSeconds = 15.58,
                    maxSpeed = 10f,
                ),
            onEvent = {},
        )
    }
}
