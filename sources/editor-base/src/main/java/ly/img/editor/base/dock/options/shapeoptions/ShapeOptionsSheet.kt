package ly.img.editor.base.dock.options.shapeoptions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ly.img.editor.base.components.PropertySlider
import ly.img.editor.base.components.PropertyStepper
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.core.R
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.ui.SheetHeader
import ly.img.editor.core.ui.sheetScrollableContentModifier

@Composable
fun ShapeOptionsSheet(
    uiState: ShapeOptionsUiState,
    onEvent: (EditorEvent) -> Unit,
) {
    Column {
        SheetHeader(
            title = stringResource(R.string.ly_img_editor_sheet_shape_title),
            onClose = { onEvent(EditorEvent.Sheet.Close(animate = true)) },
        )

        Column(
            Modifier.sheetScrollableContentModifier(),
        ) {
            when (uiState) {
                is PolygonShapeOptionsUiState -> {
                    PropertyStepper(
                        title = stringResource(R.string.ly_img_editor_sheet_shape_label_sides),
                        value = uiState.sides,
                        onValueChange = { onEvent(BlockEvent.OnChangePolygonSides(it)) },
                        onValueChangeFinished = { onEvent(BlockEvent.OnChangeFinish) },
                        valueRange = 3f..12f,
                        step = 1f,
                        decimals = 0,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PropertySlider(
                        title = stringResource(R.string.ly_img_editor_sheet_shape_label_round_corners),
                        value = uiState.cornerRadius,
                        onValueChange = { onEvent(BlockEvent.OnChangePolygonCornerRadius(it)) },
                        onValueChangeFinished = { onEvent(BlockEvent.OnChangeFinish) },
                        valueRange = 0f..1f,
                        // A ratio of the shape's size, so a number would not mean a length.
                        showsReadout = false,
                    )
                }

                is StarShapeOptionsUiState -> {
                    PropertyStepper(
                        title = stringResource(R.string.ly_img_editor_sheet_shape_label_points),
                        value = uiState.points,
                        onValueChange = { onEvent(BlockEvent.OnChangeStarPoints(it)) },
                        onValueChangeFinished = { onEvent(BlockEvent.OnChangeFinish) },
                        valueRange = 3f..12f,
                        step = 1f,
                        decimals = 0,
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    PropertySlider(
                        title = stringResource(R.string.ly_img_editor_sheet_shape_label_inner_diameter),
                        value = uiState.innerDiameter,
                        onValueChange = { onEvent(BlockEvent.OnChangeStarInnerDiameter(it)) },
                        onValueChangeFinished = { onEvent(BlockEvent.OnChangeFinish) },
                        valueRange = 0.1f..1f,
                        // A percentage would read a false 0% at the 0.1 minimum.
                        disableAutoPercentage = true,
                    )
                }

                is RectShapeUiState -> {
                    PropertySlider(
                        title = stringResource(R.string.ly_img_editor_sheet_shape_label_round_corners),
                        value = uiState.cornerRadiusTopLeft,
                        onValueChange = {
                            onEvent(
                                BlockEvent.OnChangeRectCornerRadius(
                                    topLeft = it,
                                    topRight = it,
                                    bottomLeft = it,
                                    bottomRight = it,
                                ),
                            )
                        },
                        onValueChangeFinished = { onEvent(BlockEvent.OnChangeFinish) },
                        valueRange = 0f..1f,
                        // A ratio of the shape's size, so a number would not mean a length.
                        showsReadout = false,
                    )
                }
            }
        }
    }
}
