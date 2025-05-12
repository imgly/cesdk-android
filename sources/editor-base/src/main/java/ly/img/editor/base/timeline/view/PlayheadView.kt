package ly.img.editor.base.timeline.view

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.inset
import androidx.compose.ui.unit.dp
import ly.img.editor.core.ui.utils.toPx

@Composable
fun PlayheadView(
    modifier: Modifier = Modifier,
    outlineColor: Color = MaterialTheme.colorScheme.onPrimaryContainer,
    color: Color = MaterialTheme.colorScheme.primary,
    cornerRadius: CornerRadius = CornerRadius(4.dp.toPx()),
) {
    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .width(3.dp),
    ) {
        drawRoundRect(color = outlineColor, cornerRadius = cornerRadius, alpha = 0.24f)
        val inset = 1.dp.roundToPx().toFloat()
        // When sheets are open, timeline max height may become 0.
        // However, insets are not allowed to be more than the height. That is why this check is applied.
        if (size.height - 2 * inset >= 0) {
            inset(inset) {
                drawRoundRect(color = color, cornerRadius = cornerRadius)
            }
        }
    }
}
