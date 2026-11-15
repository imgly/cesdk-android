package ly.img.editor.base.dock.options.fillstroke

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ly.img.editor.base.components.PropertyPicker
import ly.img.editor.base.components.SectionHeader
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.core.R
import ly.img.editor.core.component.data.GradientFill
import ly.img.editor.core.component.data.SolidFill
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.ui.SheetHeader
import ly.img.editor.core.ui.UiDefaults
import ly.img.editor.core.ui.engine.toComposeColor
import ly.img.editor.core.ui.sheetScrollableContentModifier
import ly.img.engine.RGBAColor

@Composable
fun FillStrokeOptionsSheet(
    uiState: FillStrokeUiState,
    onColorPickerActiveChanged: (active: Boolean) -> Unit,
    onEvent: (EditorEvent) -> Unit,
) {
    var screenState by remember { mutableStateOf<ScreenState>(ScreenState.Main) }

    BackHandler(enabled = screenState != ScreenState.Main) {
        screenState = ScreenState.Main
    }

    Column {
        when (val state = screenState) {
            ScreenState.Main -> {
                SheetHeader(
                    title = stringResource(id = uiState.titleRes),
                    onClose = { onEvent(EditorEvent.Sheet.Close(animate = true)) },
                )
                Column(
                    Modifier.sheetScrollableContentModifier(),
                ) {
                    if (uiState.fillUiState != null) {
                        SectionHeader(stringResource(R.string.ly_img_editor_sheet_fill_stroke_label_fill))
                        Card(
                            colors = UiDefaults.cardColors,
                        ) {
                            val fillState = uiState.fillUiState.fillState.takeIf { uiState.fillUiState.isFillEnabled }
                            if (uiState.fillUiState.supportFillTypes) {
                                PropertyPicker(
                                    title = stringResource(R.string.ly_img_editor_sheet_fill_stroke_label_type),
                                    propertyValue = uiState.fillUiState.fillType,
                                    properties = fillTypeProperties,
                                    onPropertyPicked = {
                                        if (it == null) {
                                            onEvent(BlockEvent.OnDisableFill)
                                        } else if (uiState.fillUiState.isFillEnabled) {
                                            onEvent(BlockEvent.OnChangeFillStyle(it))
                                        } else {
                                            onEvent(BlockEvent.OnEnableFill)
                                            onEvent(BlockEvent.OnChangeFillStyle(it))
                                        }
                                    },
                                )
                                Divider(Modifier.padding(horizontal = 16.dp))
                            }
                            when (fillState) {
                                null -> {
                                    ColorOptions(
                                        enabled = uiState.fillUiState.isFillEnabled,
                                        selectedColors = emptyList(),
                                        onNoColorSelected = {
                                            onEvent(
                                                BlockEvent.OnDisableFill,
                                            )
                                        },
                                        onColorSelected = {
                                            onEvent(BlockEvent.OnChangeFillColor(it))
                                            onEvent(BlockEvent.OnChangeFinish)
                                        },
                                        openColorPicker = {
                                            onColorPickerActiveChanged(true)
                                            screenState = ScreenState.ColorPicker(stroke = false, stopIndex = null)
                                        },
                                        colors = uiState.fillUiState.colorPalette,
                                    )
                                }

                                is SolidFill -> {
                                    ColorOptions(
                                        enabled = uiState.fillUiState.isFillEnabled,
                                        selectedColors = fillState.colors,
                                        onNoColorSelected = {
                                            onEvent(
                                                BlockEvent.OnDisableFill,
                                            )
                                        },
                                        onColorSelected = {
                                            onEvent(BlockEvent.OnChangeFillColor(it))
                                            if (it != fillState.mainColor) {
                                                onEvent(BlockEvent.OnChangeFinish)
                                            }
                                        },
                                        openColorPicker = {
                                            onColorPickerActiveChanged(true)
                                            screenState = ScreenState.ColorPicker(stroke = false, stopIndex = null)
                                        },
                                        colors = uiState.fillUiState.colorPalette,
                                    )
                                }

                                // A radial or conical gradient from a scene or the API shows its stop colors, without the
                                // angle that only a linear gradient has.
                                is GradientFill -> {
                                    GradientOptions(
                                        gradient = fillState,
                                        colorPalette = uiState.fillUiState.colorPalette,
                                        onColorSelected = { index, color ->
                                            onEvent(BlockEvent.OnChangeGradientFillColors(index, color))
                                        },
                                        onRotationChange = {
                                            onEvent(BlockEvent.OnChangeLinearGradientParams(rotationInDegrees = it))
                                        },
                                        onChangeFinish = { onEvent(BlockEvent.OnChangeFinish) },
                                        openColorPicker = { index ->
                                            onColorPickerActiveChanged(true)
                                            screenState = ScreenState.ColorPicker(stroke = false, stopIndex = index)
                                        },
                                    )
                                }

                                else -> throw UnsupportedOperationException(
                                    "Fill type not supported yet",
                                )
                            }
                        }
                    }
                    if (uiState.fillUiState != null && uiState.strokeUiState != null) {
                        Spacer(Modifier.height(16.dp))
                    }
                    if (uiState.strokeUiState != null) {
                        StrokeOptions(
                            uiState = uiState.strokeUiState,
                            onEvent = onEvent,
                            openColorPicker = { stopIndex ->
                                onColorPickerActiveChanged(true)
                                screenState = ScreenState.ColorPicker(stroke = true, stopIndex = stopIndex)
                            },
                        )
                    }
                }
            }

            is ScreenState.ColorPicker -> {
                val stopIndex = state.stopIndex
                val gradient = if (state.stroke) {
                    uiState.strokeUiState?.gradientFill
                } else {
                    uiState.fillUiState?.fillState as? GradientFill
                }
                val color = when {
                    stopIndex != null -> (gradient?.colorStops?.getOrNull(stopIndex)?.color as? RGBAColor)?.toComposeColor()
                    state.stroke -> uiState.strokeUiState?.strokeColor
                    else -> uiState.fillUiState?.fillState?.mainColor
                }
                // An undo can remove the color that the picker edits, so the sheet goes back to its main screen.
                if (color == null) {
                    LaunchedEffect(Unit) {
                        onColorPickerActiveChanged(false)
                        screenState = ScreenState.Main
                    }
                    return@Column
                }
                ColorPickerSheet(
                    color = color,
                    title = stringResource(
                        id = if (state.stroke) {
                            R.string.ly_img_editor_sheet_fill_stroke_color_picker_title_stroke
                        } else {
                            R.string.ly_img_editor_sheet_fill_stroke_color_picker_title_fill
                        },
                    ),
                    onBack = {
                        onColorPickerActiveChanged(false)
                        screenState = ScreenState.Main
                    },
                    onColorChange = {
                        onEvent(
                            when {
                                state.stroke && stopIndex != null -> BlockEvent.OnChangeStrokeGradientColors(stopIndex, it)
                                state.stroke -> BlockEvent.OnChangeStrokeColor(it)
                                stopIndex != null -> BlockEvent.OnChangeGradientFillColors(stopIndex, it)
                                else -> BlockEvent.OnChangeFillColor(it)
                            },
                        )
                    },
                    onEvent = onEvent,
                )
            }
        }
    }
}

private sealed interface ScreenState {
    data object Main : ScreenState

    // The picker of the stroke or of the fill, for the color of the gradient stop at [stopIndex], or for the solid
    // color when [stopIndex] is null.
    data class ColorPicker(
        val stroke: Boolean,
        val stopIndex: Int?,
    ) : ScreenState
}
