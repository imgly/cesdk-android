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

val IconPack.FreeCrop: ImageVector
    get() {
        if (freeCrop != null) {
            return freeCrop!!
        }
        freeCrop = Builder(
            name = "FreeCrop",
            defaultWidth = 24.0.dp,
            defaultHeight = 25.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 25.0f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFF171A2C)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(14.0f, 19.22f)
                horizontalLineTo(18.0f)
                curveTo(18.552f, 19.22f, 19.0f, 18.772f, 19.0f, 18.22f)
                verticalLineTo(14.22f)
                horizontalLineTo(21.0f)
                verticalLineTo(18.22f)
                curveTo(21.0f, 19.877f, 19.657f, 21.22f, 18.0f, 21.22f)
                horizontalLineTo(14.0f)
                verticalLineTo(19.22f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF171A2C)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(5.0f, 14.22f)
                verticalLineTo(18.22f)
                curveTo(5.0f, 18.772f, 5.448f, 19.22f, 6.0f, 19.22f)
                horizontalLineTo(10.0f)
                verticalLineTo(21.22f)
                horizontalLineTo(6.0f)
                curveTo(4.343f, 21.22f, 3.0f, 19.877f, 3.0f, 18.22f)
                verticalLineTo(14.22f)
                horizontalLineTo(5.0f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF171A2C)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(10.0f, 5.22f)
                horizontalLineTo(6.0f)
                curveTo(5.448f, 5.22f, 5.0f, 5.667f, 5.0f, 6.22f)
                verticalLineTo(10.22f)
                horizontalLineTo(3.0f)
                verticalLineTo(6.22f)
                curveTo(3.0f, 4.563f, 4.343f, 3.22f, 6.0f, 3.22f)
                horizontalLineTo(10.0f)
                verticalLineTo(5.22f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF171A2C)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(14.0f, 5.22f)
                horizontalLineTo(18.0f)
                curveTo(18.552f, 5.22f, 19.0f, 5.667f, 19.0f, 6.22f)
                verticalLineTo(10.22f)
                horizontalLineTo(21.0f)
                verticalLineTo(6.22f)
                curveTo(21.0f, 4.563f, 19.657f, 3.22f, 18.0f, 3.22f)
                horizontalLineTo(14.0f)
                verticalLineTo(5.22f)
                close()
            }
        }
            .build()
        return freeCrop!!
    }

private var freeCrop: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.FreeCrop.IconPreview()
