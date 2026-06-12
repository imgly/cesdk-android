package ly.img.editor.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ly.img.editor.core.component.data.SolidFill

/**
 * A composable function that renders [Color].
 *
 * @param modifier the [Modifier] to be applied to this button.
 * @param color the color that should be rendered. Null will render no color.
 * @param selected if the button is selected.
 * @param punchHole if a hole should be punched in the center of the button.
 * @param onClick the click action of the button. Pass null to disable click.
 * @param buttonSize the size of the button.
 * @param selectionStrokeWidth the size of the stroke of the button when it is selected.
 */
@Composable
fun ColorButton(
    modifier: Modifier = Modifier,
    color: Color?,
    selected: Boolean = false,
    punchHole: Boolean = false,
    onClick: (() -> Unit)? = null,
    buttonSize: Dp = 40.dp,
    selectionStrokeWidth: Dp = 2.dp,
) = FillButton(
    modifier = modifier,
    fill = remember(color) {
        color?.let {
            SolidFill(color)
        }
    },
    selected = selected,
    punchHole = punchHole,
    onClick = onClick,
    buttonSize = buttonSize,
    selectionStrokeWidth = selectionStrokeWidth,
)

/**
 * A composable function that renders a list of [Color]s.
 *
 * A single colour renders as a plain filled swatch; multiple colours render as equal-width
 * vertical stripes, mirroring the web text-colour swatch when the selection spans mixed colours.
 *
 * @param modifier the [Modifier] to be applied to this button.
 * @param colors the ordered colours that should be rendered. Empty renders no color.
 * @param selected if the button is selected.
 * @param punchHole if a hole should be punched in the center of the button.
 * @param onClick the click action of the button. Pass null to disable click.
 * @param buttonSize the size of the button.
 * @param selectionStrokeWidth the size of the stroke of the button when it is selected.
 */
@Composable
fun ColorButton(
    colors: List<Color>,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    punchHole: Boolean = false,
    onClick: (() -> Unit)? = null,
    buttonSize: Dp = 40.dp,
    selectionStrokeWidth: Dp = 2.dp,
) = FillButton(
    modifier = modifier,
    fill = remember(colors) {
        colors.takeIf { it.isNotEmpty() }?.let { SolidFill(it) }
    },
    selected = selected,
    punchHole = punchHole,
    onClick = onClick,
    buttonSize = buttonSize,
    selectionStrokeWidth = selectionStrokeWidth,
)
