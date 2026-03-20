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

val IconPack.Formatunderlined: ImageVector
    get() {
        if (formatunderlined != null) {
            return formatunderlined!!
        }
        formatunderlined = Builder(
            name = "Formatunderlined",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFF49454F)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(12.0f, 17.0f)
                curveTo(14.76f, 17.0f, 17.0f, 14.76f, 17.0f, 12.0f)
                verticalLineTo(3.0f)
                horizontalLineTo(14.5f)
                verticalLineTo(12.0f)
                curveTo(14.5f, 13.38f, 13.38f, 14.5f, 12.0f, 14.5f)
                curveTo(10.62f, 14.5f, 9.5f, 13.38f, 9.5f, 12.0f)
                verticalLineTo(3.0f)
                horizontalLineTo(7.0f)
                verticalLineTo(12.0f)
                curveTo(7.0f, 14.76f, 9.24f, 17.0f, 12.0f, 17.0f)
                close()
                moveTo(5.0f, 19.0f)
                verticalLineTo(21.0f)
                horizontalLineTo(19.0f)
                verticalLineTo(19.0f)
                horizontalLineTo(5.0f)
                close()
            }
        }.build()
        return formatunderlined!!
    }

private var formatunderlined: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.Formatunderlined.IconPreview()
