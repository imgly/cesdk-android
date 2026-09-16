package ly.img.editor.core.ui.iconpack

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

val IconPack.ArrowUp: ImageVector
    get() {
        if (arrowUp != null) {
            return arrowUp!!
        }
        arrowUp = Builder(
            name = "ArrowUp",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(4.0f, 12.0f)
                lineToRelative(1.41f, 1.41f)
                lineTo(11.0f, 7.83f)
                verticalLineTo(20.0f)
                horizontalLineToRelative(2.0f)
                verticalLineTo(7.83f)
                lineToRelative(5.58f, 5.59f)
                lineTo(20.0f, 12.0f)
                lineToRelative(-8.0f, -8.0f)
                lineToRelative(-8.0f, 8.0f)
                close()
            }
        }
            .build()
        return arrowUp!!
    }

private var arrowUp: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.ArrowUp.IconPreview()
