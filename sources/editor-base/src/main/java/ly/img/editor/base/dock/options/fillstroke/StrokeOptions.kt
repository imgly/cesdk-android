package ly.img.editor.base.dock.options.fillstroke

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ly.img.editor.base.components.PropertyPicker
import ly.img.editor.base.components.PropertyStepperRow
import ly.img.editor.base.components.SectionHeader
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.base.ui.Event
import ly.img.editor.core.R
import ly.img.editor.core.ui.UiDefaults

@Composable
fun StrokeOptions(
    uiState: StrokeUiState,
    onEvent: (Event) -> Unit,
    openColorPicker: (stopIndex: Int?) -> Unit,
) {
    SectionHeader(stringResource(R.string.ly_img_editor_sheet_fill_stroke_label_stroke))
    Card(
        colors = UiDefaults.cardColors,
    ) {
        if (uiState.supportFillTypes) {
            PropertyPicker(
                title = stringResource(R.string.ly_img_editor_sheet_fill_stroke_label_type),
                propertyValue = uiState.fillType,
                properties = fillTypeProperties,
                onPropertyPicked = {
                    onEvent(if (it == null) BlockEvent.OnDisableStroke else BlockEvent.OnChangeStrokeFillStyle(it))
                },
            )
            Divider(Modifier.padding(horizontal = 16.dp))
        }
        val gradient = uiState.gradientFill.takeIf { uiState.isStrokeEnabled }
        if (gradient != null) {
            GradientOptions(
                gradient = gradient,
                colorPalette = uiState.colorPalette,
                onColorSelected = { index, color -> onEvent(BlockEvent.OnChangeStrokeGradientColors(index, color)) },
                onRotationChange = { onEvent(BlockEvent.OnChangeStrokeLinearGradientParams(rotationInDegrees = it)) },
                onChangeFinish = { onEvent(BlockEvent.OnChangeFinish) },
                openColorPicker = openColorPicker,
                punchHole = true,
            )
        } else {
            ColorOptions(
                enabled = uiState.isStrokeEnabled,
                selectedColors = listOf(uiState.strokeColor),
                onNoColorSelected = { onEvent(BlockEvent.OnDisableStroke) },
                onColorSelected = {
                    onEvent(BlockEvent.OnChangeStrokeColor(it))
                    if (it != uiState.strokeColor || !uiState.isStrokeEnabled) {
                        onEvent(BlockEvent.OnChangeFinish)
                    }
                },
                openColorPicker = { openColorPicker(null) },
                colors = uiState.colorPalette,
                punchHole = true,
            )
        }

        if (uiState.isStrokeEnabled) {
            Divider(Modifier.padding(horizontal = 16.dp))

            // A stepper, not a slider, so the same width can be typed again on another block.
            PropertyStepperRow(
                title = stringResource(R.string.ly_img_editor_sheet_fill_stroke_label_width),
                value = uiState.strokeWidth,
                onValueChange = { onEvent(BlockEvent.OnChangeStrokeWidth(it)) },
                onValueChangeFinished = { onEvent(BlockEvent.OnChangeFinish) },
                valueRange = 0F..uiState.strokeWidthMax,
                step = 0.1F,
                unit = uiState.strokeWidthUnit,
                decimals = uiState.strokeWidthDecimals,
            )
            Divider(Modifier.padding(horizontal = 16.dp))
            PropertyPicker(
                title = stringResource(R.string.ly_img_editor_sheet_fill_stroke_label_style),
                propertyValue = uiState.strokeStyle,
                properties = strokeStyleProperties,
                onPropertyPicked = { onEvent(BlockEvent.OnChangeStrokeStyle(it)) },
            )
            if (uiState.showPositionAndJoin) {
                Divider(Modifier.padding(horizontal = 16.dp))
                PropertyPicker(
                    title = stringResource(R.string.ly_img_editor_sheet_fill_stroke_label_position),
                    propertyValue = uiState.strokePosition,
                    enabled = uiState.isStrokePositionEnabled,
                    properties = strokePositionProperties,
                    onPropertyPicked = { onEvent(BlockEvent.OnChangeStrokePosition(it)) },
                )
                Divider(Modifier.padding(horizontal = 16.dp))
                PropertyPicker(
                    title = stringResource(R.string.ly_img_editor_sheet_fill_stroke_label_join),
                    propertyValue = uiState.strokeJoin,
                    properties = strokeJoinProperties,
                    enabled = uiState.isStrokeJointEnabled,
                    onPropertyPicked = { onEvent(BlockEvent.OnChangeStrokeCornerGeometry(it)) },
                )
            }
        }
    }
}
