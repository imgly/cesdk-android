package ly.img.editor.core.ui.iconpack

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.EvenOdd
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

val IconPack.FillModeFit: ImageVector
    get() {
        if (fillModeFit != null) {
            return fillModeFit!!
        }
        fillModeFit = Builder(
            name = "FillmodeFit",
            defaultWidth = 25.0.dp,
            defaultHeight =
                25.0.dp,
            viewportWidth = 25.0f,
            viewportHeight = 25.0f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFF46464F)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(9.1309f, 7.9844f)
                curveTo(8.0263f, 7.9844f, 7.1309f, 8.8798f, 7.1309f, 9.9844f)
                verticalLineTo(15.9844f)
                curveTo(7.1309f, 17.0889f, 8.0263f, 17.9844f, 9.1309f, 17.9844f)
                horizontalLineTo(15.1309f)
                curveTo(16.2354f, 17.9844f, 17.1309f, 17.0889f, 17.1309f, 15.9844f)
                verticalLineTo(9.9844f)
                curveTo(17.1309f, 8.8798f, 16.2354f, 7.9844f, 15.1309f, 7.9844f)
                horizontalLineTo(9.1309f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF46464F)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = EvenOdd,
            ) {
                moveTo(5.1309f, 5.2344f)
                curveTo(3.0598f, 5.2344f, 1.3809f, 6.9133f, 1.3809f, 8.9844f)
                verticalLineTo(16.9844f)
                curveTo(1.3809f, 19.0554f, 3.0598f, 20.7344f, 5.1309f, 20.7344f)
                horizontalLineTo(19.1309f)
                curveTo(21.2019f, 20.7344f, 22.8809f, 19.0554f, 22.8809f, 16.9844f)
                verticalLineTo(8.9844f)
                curveTo(22.8809f, 6.9133f, 21.2019f, 5.2344f, 19.1309f, 5.2344f)
                horizontalLineTo(5.1309f)
                close()
                moveTo(2.8809f, 8.9844f)
                curveTo(2.8809f, 7.7417f, 3.8882f, 6.7344f, 5.1309f, 6.7344f)
                horizontalLineTo(19.1309f)
                curveTo(20.3735f, 6.7344f, 21.3809f, 7.7417f, 21.3809f, 8.9844f)
                verticalLineTo(16.9844f)
                curveTo(21.3809f, 18.227f, 20.3735f, 19.2344f, 19.1309f, 19.2344f)
                horizontalLineTo(5.1309f)
                curveTo(3.8882f, 19.2344f, 2.8809f, 18.227f, 2.8809f, 16.9844f)
                verticalLineTo(8.9844f)
                close()
            }
        }
            .build()
        return fillModeFit!!
    }

private var fillModeFit: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.FillModeFit.IconPreview()
