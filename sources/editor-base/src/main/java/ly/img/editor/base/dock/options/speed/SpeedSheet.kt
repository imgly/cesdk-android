package ly.img.editor.base.dock.options.speed

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ly.img.editor.base.components.NumberStepperField
import ly.img.editor.base.dock.BottomSheetContent
import ly.img.editor.base.timeline.state.TimelineConfiguration
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.base.ui.Event
import ly.img.editor.core.R
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.theme.EditorTheme
import ly.img.editor.core.ui.SheetHeader
import ly.img.editor.core.ui.UiDefaults
import ly.img.editor.core.ui.sheetScrollableContentModifier
import ly.img.editor.core.ui.utils.ThemePreview

private const val SPEED_STEP = 0.25f
private const val MIN_SPEED = 0.25f
private const val INPUT_DECIMALS = 2

// The duration field has no buttons, so this step only sizes a TalkBack adjustment.
private const val DURATION_STEP = 0.1f

@Composable
fun SpeedSheet(
    uiState: SpeedUiState,
    onEvent: (EditorEvent) -> Unit,
) {
    val maxSpeed = uiState.maxSpeed
    // Each change during a hold or a typed edit is live, and the end of the gesture adds one undo step.
    val onEditingChanged = { isEditing: Boolean -> if (!isEditing) onEvent(BlockEvent.OnChangeFinish) }

    Column {
        SheetHeader(
            title = stringResource(id = R.string.ly_img_editor_sheet_clip_speed_title),
            onClose = { onEvent(EditorEvent.Sheet.Close(animate = true)) },
        )
        Card(
            Modifier.sheetScrollableContentModifier(),
            colors = UiDefaults.cardColors,
        ) {
            Column(Modifier.fillMaxWidth()) {
                val speedTitle = stringResource(id = R.string.ly_img_editor_sheet_clip_speed_speed_label)
                SpeedRow(title = speedTitle) {
                    NumberStepperField(
                        value = uiState.speed,
                        onValueChange = { onEvent(BlockEvent.OnPlaybackSpeedChange(it)) },
                        title = speedTitle,
                        valueRange = MIN_SPEED..maxSpeed,
                        step = SPEED_STEP,
                        unit = stringResource(id = R.string.ly_img_editor_sheet_clip_speed_speed_suffix),
                        decimals = INPUT_DECIMALS,
                        onEditingChanged = onEditingChanged,
                    )
                }

                val durationSeconds = uiState.durationSeconds
                if (durationSeconds != null) {
                    // The duration at 1×, so the bounds follow from the speed bounds.
                    val normalizedDuration = durationSeconds * uiState.speed
                    val minDuration = maxOf(
                        normalizedDuration / maxSpeed,
                        TimelineConfiguration.minClipDuration.inWholeMilliseconds / 1000.0,
                    )
                    val maxDuration = normalizedDuration / MIN_SPEED
                    Divider(Modifier.padding(horizontal = 16.dp))
                    val durationTitle = stringResource(id = R.string.ly_img_editor_sheet_clip_speed_duration_label)
                    SpeedRow(title = durationTitle) {
                        NumberStepperField(
                            value = durationSeconds.toFloat(),
                            onValueChange = { duration ->
                                val speed = (normalizedDuration / duration).toFloat().coerceIn(MIN_SPEED, maxSpeed)
                                onEvent(BlockEvent.OnPlaybackSpeedChange(speed))
                            },
                            title = durationTitle,
                            valueRange = minDuration.toFloat()..maxDuration.toFloat(),
                            step = DURATION_STEP,
                            unit = stringResource(id = R.string.ly_img_editor_sheet_clip_speed_duration_suffix),
                            decimals = INPUT_DECIMALS,
                            showsButtons = false,
                            onEditingChanged = onEditingChanged,
                            onTypedValueClamped = {
                                onEvent(Event.OnToast(R.string.ly_img_editor_notification_speed_clamped_duration))
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpeedRow(
    title: String,
    field: @Composable () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            maxLines = 2,
            modifier = Modifier.weight(1f),
        )
        field()
    }
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
