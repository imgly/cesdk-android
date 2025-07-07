package ly.img.editor.core.ui.iconpack

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.EvenOdd
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

val IconPack.SquareCrop: ImageVector
    get() {
        if (squareCrop != null) {
            return squareCrop!!
        }
        squareCrop = Builder(
            name = "SquareCrop",
            defaultWidth = 24.0.dp,
            defaultHeight = 25.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 25.0f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFF46464F)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = EvenOdd,
            ) {
                moveTo(6.0f, 5.22f)
                horizontalLineTo(18.0f)
                curveTo(18.552f, 5.22f, 19.0f, 5.667f, 19.0f, 6.22f)
                verticalLineTo(18.22f)
                curveTo(19.0f, 18.772f, 18.552f, 19.22f, 18.0f, 19.22f)
                horizontalLineTo(6.0f)
                curveTo(5.448f, 19.22f, 5.0f, 18.772f, 5.0f, 18.22f)
                verticalLineTo(6.22f)
                curveTo(5.0f, 5.667f, 5.448f, 5.22f, 6.0f, 5.22f)
                close()
                moveTo(3.0f, 6.22f)
                curveTo(3.0f, 4.563f, 4.343f, 3.22f, 6.0f, 3.22f)
                horizontalLineTo(18.0f)
                curveTo(19.657f, 3.22f, 21.0f, 4.563f, 21.0f, 6.22f)
                verticalLineTo(18.22f)
                curveTo(21.0f, 19.877f, 19.657f, 21.22f, 18.0f, 21.22f)
                horizontalLineTo(6.0f)
                curveTo(4.343f, 21.22f, 3.0f, 19.877f, 3.0f, 18.22f)
                verticalLineTo(6.22f)
                close()
                moveTo(15.864f, 9.417f)
                lineTo(16.53f, 8.75f)
                lineTo(15.47f, 7.689f)
                lineTo(14.803f, 8.356f)
                lineTo(15.864f, 9.417f)
                close()
                moveTo(13.197f, 12.083f)
                lineTo(14.53f, 10.75f)
                lineTo(13.47f, 9.689f)
                lineTo(12.136f, 11.023f)
                lineTo(13.197f, 12.083f)
                close()
                moveTo(10.53f, 14.75f)
                lineTo(11.864f, 13.417f)
                lineTo(10.803f, 12.356f)
                lineTo(9.47f, 13.689f)
                lineTo(10.53f, 14.75f)
                close()
                moveTo(8.53f, 16.75f)
                lineTo(9.197f, 16.083f)
                lineTo(8.136f, 15.023f)
                lineTo(7.47f, 15.689f)
                lineTo(8.53f, 16.75f)
                close()
            }
        }
            .build()
        return squareCrop!!
    }

private var squareCrop: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.SquareCrop.IconPreview()
