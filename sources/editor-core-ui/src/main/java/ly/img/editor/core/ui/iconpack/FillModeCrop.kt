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

public val IconPack.FillModeCrop: ImageVector
    get() {
        if (fillModeCrop != null) {
            return fillModeCrop!!
        }
        fillModeCrop = Builder(
            name = "FillmodeCrop",
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
                moveTo(2.1309f, 6.9844f)
                curveTo(2.1309f, 5.8798f, 3.0263f, 4.9844f, 4.1309f, 4.9844f)
                horizontalLineTo(19.1309f)
                curveTo(20.2354f, 4.9844f, 21.1309f, 5.8798f, 21.1309f, 6.9844f)
                verticalLineTo(8.9844f)
                horizontalLineTo(13.1309f)
                curveTo(10.3694f, 8.9844f, 8.1309f, 11.223f, 8.1309f, 13.9844f)
                verticalLineTo(18.9844f)
                horizontalLineTo(4.1309f)
                curveTo(3.0263f, 18.9844f, 2.1309f, 18.0889f, 2.1309f, 16.9844f)
                verticalLineTo(6.9844f)
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
                moveTo(13.1309f, 12.9844f)
                curveTo(12.5786f, 12.9844f, 12.1309f, 13.4321f, 12.1309f, 13.9844f)
                verticalLineTo(16.9844f)
                curveTo(12.1309f, 17.5367f, 12.5786f, 17.9844f, 13.1309f, 17.9844f)
                horizontalLineTo(19.1309f)
                curveTo(19.6831f, 17.9844f, 20.1309f, 17.5367f, 20.1309f, 16.9844f)
                verticalLineTo(13.9844f)
                curveTo(20.1309f, 13.4321f, 19.6831f, 12.9844f, 19.1309f, 12.9844f)
                horizontalLineTo(13.1309f)
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
                moveTo(13.1309f, 10.2344f)
                curveTo(11.0598f, 10.2344f, 9.3809f, 11.9133f, 9.3809f, 13.9844f)
                verticalLineTo(16.9844f)
                curveTo(9.3809f, 19.0554f, 11.0598f, 20.7344f, 13.1309f, 20.7344f)
                horizontalLineTo(19.1309f)
                curveTo(21.2019f, 20.7344f, 22.8809f, 19.0554f, 22.8809f, 16.9844f)
                verticalLineTo(13.9844f)
                curveTo(22.8809f, 11.9133f, 21.2019f, 10.2344f, 19.1309f, 10.2344f)
                horizontalLineTo(13.1309f)
                close()
                moveTo(10.8809f, 13.9844f)
                curveTo(10.8809f, 12.7417f, 11.8882f, 11.7344f, 13.1309f, 11.7344f)
                horizontalLineTo(19.1309f)
                curveTo(20.3735f, 11.7344f, 21.3809f, 12.7417f, 21.3809f, 13.9844f)
                verticalLineTo(16.9844f)
                curveTo(21.3809f, 18.227f, 20.3735f, 19.2344f, 19.1309f, 19.2344f)
                horizontalLineTo(13.1309f)
                curveTo(11.8882f, 19.2344f, 10.8809f, 18.227f, 10.8809f, 16.9844f)
                verticalLineTo(13.9844f)
                close()
            }
        }
            .build()
        return fillModeCrop!!
    }

private var fillModeCrop: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.FillModeCrop.IconPreview()
