package ly.img.editor.base.dock.options.fillstroke

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ly.img.editor.base.dock.options.crop.RangeInclusionType
import ly.img.editor.base.dock.options.crop.ScalePicker
import ly.img.editor.core.R
import ly.img.editor.core.component.data.GradientFill
import ly.img.editor.core.component.data.LinearGradientFill
import ly.img.editor.core.ui.engine.toComposeColor
import ly.img.engine.RGBAColor

/**
 * The color rows of the first two stops of [gradient], and the angle between them for a linear gradient. The fill
 * section edits the fill of a block with it, and the stroke section the fill that paints the stroke.
 *
 * @param onColorSelected called with the index of the stop and its new color.
 * @param onRotationChange called with the new angle of a linear gradient, in degrees.
 * @param onChangeFinish called when a change is done and needs an undo step.
 * @param openColorPicker called with the index of the stop whose color picker opens.
 */
@Composable
internal fun GradientOptions(
    gradient: GradientFill,
    colorPalette: List<Color>,
    onColorSelected: (index: Int, color: Color) -> Unit,
    onRotationChange: (rotationInDegrees: Float) -> Unit,
    onChangeFinish: () -> Unit,
    openColorPicker: (index: Int) -> Unit,
    punchHole: Boolean = false,
) {
    @Composable
    fun StopColorOptions(index: Int) {
        val stopColor = (gradient.colorStops[index].color as RGBAColor).toComposeColor()
        ColorOptions(
            enabled = true,
            selectedColors = listOf(stopColor),
            onNoColorSelected = {},
            allowDisableColor = false,
            onColorSelected = {
                onColorSelected(index, it)
                if (it != stopColor) {
                    onChangeFinish()
                }
            },
            openColorPicker = { openColorPicker(index) },
            colors = colorPalette,
            punchHole = punchHole,
        )
    }

    StopColorOptions(0)
    if (gradient is LinearGradientFill) {
        // `gradientRotation` is public and reads 0..360, so only the ruler shows −180..180, as iOS and the Web editor do.
        val angle = gradient.gradientRotation.let { if (it > 180f) it - 360f else it }
        Divider(Modifier.padding(horizontal = 16.dp))
        ScalePicker(
            contentDescription = stringResource(R.string.ly_img_editor_sheet_fill_stroke_accessibility_label_gradient_angle),
            value = angle,
            valueRange = GRADIENT_ROTATION_LOWER_BOUND..GRADIENT_ROTATION_UPPER_BOUND,
            // −180 and 180 are the same angle, and the angle reads 180.
            rangeInclusionType = RangeInclusionType.RangeExclusiveInclusive,
            onValueChange = onRotationChange,
            onValueChangeFinished = {
                if (angle != it) {
                    onChangeFinish()
                }
            },
            modifier = Modifier.padding(top = 8.dp, bottom = 12.dp),
        )
    }
    // A gradient from a scene or the API can have a single stop.
    if (gradient.colorStops.size > 1) {
        Divider(Modifier.padding(horizontal = 16.dp))
        StopColorOptions(1)
    }
}

private const val GRADIENT_ROTATION_LOWER_BOUND = -180f
private const val GRADIENT_ROTATION_UPPER_BOUND = 180f
