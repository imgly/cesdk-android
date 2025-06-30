package ly.img.editor.base.dock.options.crop

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathBuilder
import androidx.compose.ui.unit.dp

fun roundedRectOutlineIcon(
    aspectRatio: Float,
    color: Color,
    viewport: Float = 24f,
    cornerRadius: Float = 2f,
    strokeWidth: Float = 2f,
    margin: Float = 2f,
): ImageVector {
    require(aspectRatio > 0f) { "aspectRatio must be positive" }

    val rectWidth: Float
    val rectHeight: Float

    if (aspectRatio >= 1f) {
        rectWidth = viewport - 2 * margin
        rectHeight = (viewport - 2 * margin) / aspectRatio
    } else {
        rectHeight = viewport - 2 * margin
        rectWidth = (viewport - 2 * margin) * aspectRatio
    }

    val offsetX = (viewport - rectWidth) / 2
    val offsetY = (viewport - rectHeight) / 2

    return ImageVector.Builder(
        name = "DynamicRoundedRectOutline",
        defaultWidth = viewport.dp,
        defaultHeight = viewport.dp,
        viewportWidth = viewport,
        viewportHeight = viewport,
    ).apply {
        addPath(
            pathData = PathBuilder().apply {
                moveTo(offsetX + cornerRadius, offsetY)
                lineTo(offsetX + rectWidth - cornerRadius, offsetY)
                arcTo(
                    cornerRadius,
                    cornerRadius,
                    0f,
                    false,
                    true,
                    offsetX + rectWidth,
                    offsetY + cornerRadius,
                )
                lineTo(offsetX + rectWidth, offsetY + rectHeight - cornerRadius)
                arcTo(
                    cornerRadius,
                    cornerRadius,
                    0f,
                    false,
                    true,
                    offsetX + rectWidth - cornerRadius,
                    offsetY + rectHeight,
                )
                lineTo(offsetX + cornerRadius, offsetY + rectHeight)
                arcTo(
                    cornerRadius,
                    cornerRadius,
                    0f,
                    false,
                    true,
                    offsetX,
                    offsetY + rectHeight - cornerRadius,
                )
                lineTo(offsetX, offsetY + cornerRadius)
                arcTo(
                    cornerRadius,
                    cornerRadius,
                    0f,
                    false,
                    true,
                    offsetX + cornerRadius,
                    offsetY,
                )
                close()
            }.getNodes(),
            stroke = SolidColor(color),
            fill = null,
            strokeLineWidth = strokeWidth,
            strokeLineCap = StrokeCap.Round,
            strokeLineJoin = StrokeJoin.Round,
            pathFillType = PathFillType.NonZero,
        )
    }.build()
}
