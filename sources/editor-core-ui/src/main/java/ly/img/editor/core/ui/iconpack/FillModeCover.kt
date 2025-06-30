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

val IconPack.FillModeCover: ImageVector
    get() {
        if (fillModeCover != null) {
            return fillModeCover!!
        }
        fillModeCover = Builder(
            name = "FillmodeCover",
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
                moveTo(4.1309f, 4.9844f)
                curveTo(4.1309f, 3.8798f, 5.0263f, 2.9844f, 6.1309f, 2.9844f)
                horizontalLineTo(18.1309f)
                curveTo(19.2354f, 2.9844f, 20.1309f, 3.8798f, 20.1309f, 4.9844f)
                verticalLineTo(5.9844f)
                horizontalLineTo(4.1309f)
                verticalLineTo(4.9844f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF46464F)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(20.1309f, 19.9844f)
                verticalLineTo(20.9844f)
                curveTo(20.1309f, 22.0889f, 19.2354f, 22.9844f, 18.1309f, 22.9844f)
                horizontalLineTo(6.1309f)
                curveTo(5.0263f, 22.9844f, 4.1309f, 22.0889f, 4.1309f, 20.9844f)
                verticalLineTo(19.9844f)
                horizontalLineTo(20.1309f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF46464F)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(6.1309f, 9.9844f)
                curveTo(5.5786f, 9.9844f, 5.1309f, 10.4321f, 5.1309f, 10.9844f)
                verticalLineTo(14.9844f)
                curveTo(5.1309f, 15.5367f, 5.5786f, 15.9844f, 6.1309f, 15.9844f)
                horizontalLineTo(18.1309f)
                curveTo(18.6831f, 15.9844f, 19.1309f, 15.5367f, 19.1309f, 14.9844f)
                verticalLineTo(10.9844f)
                curveTo(19.1309f, 10.4321f, 18.6831f, 9.9844f, 18.1309f, 9.9844f)
                horizontalLineTo(6.1309f)
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
                moveTo(6.1309f, 7.2344f)
                curveTo(4.0598f, 7.2344f, 2.3809f, 8.9133f, 2.3809f, 10.9844f)
                verticalLineTo(14.9844f)
                curveTo(2.3809f, 17.0554f, 4.0598f, 18.7344f, 6.1309f, 18.7344f)
                horizontalLineTo(18.1309f)
                curveTo(20.2019f, 18.7344f, 21.8809f, 17.0554f, 21.8809f, 14.9844f)
                verticalLineTo(10.9844f)
                curveTo(21.8809f, 8.9133f, 20.2019f, 7.2344f, 18.1309f, 7.2344f)
                horizontalLineTo(6.1309f)
                close()
                moveTo(3.8809f, 10.9844f)
                curveTo(3.8809f, 9.7417f, 4.8882f, 8.7344f, 6.1309f, 8.7344f)
                horizontalLineTo(18.1309f)
                curveTo(19.3735f, 8.7344f, 20.3809f, 9.7417f, 20.3809f, 10.9844f)
                verticalLineTo(14.9844f)
                curveTo(20.3809f, 16.227f, 19.3735f, 17.2344f, 18.1309f, 17.2344f)
                horizontalLineTo(6.1309f)
                curveTo(4.8882f, 17.2344f, 3.8809f, 16.227f, 3.8809f, 14.9844f)
                verticalLineTo(10.9844f)
                close()
            }
        }
            .build()
        return fillModeCover!!
    }

private var fillModeCover: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.FillModeCover.IconPreview()
