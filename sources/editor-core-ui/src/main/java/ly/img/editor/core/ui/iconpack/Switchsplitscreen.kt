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

val IconPack.Switchsplitscreen: ImageVector
    get() {
        if (switchsplitscreen != null) {
            return switchsplitscreen!!
        }
        switchsplitscreen = Builder(
            name = "Switchsplitscreen",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
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
                moveTo(14.0f, 5.0f)
                horizontalLineTo(16.0f)
                curveTo(17.1046f, 5.0f, 18.0f, 5.8954f, 18.0f, 7.0f)
                verticalLineTo(8.2f)
                lineTo(16.4f, 6.6f)
                lineTo(15.0f, 8.0f)
                lineTo(19.0f, 12.0f)
                lineTo(23.0f, 8.0f)
                lineTo(21.6f, 6.6f)
                lineTo(20.0f, 8.2f)
                verticalLineTo(7.0f)
                curveTo(20.0f, 4.7909f, 18.2091f, 3.0f, 16.0f, 3.0f)
                horizontalLineTo(14.0f)
                verticalLineTo(5.0f)
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
                moveTo(8.0f, 19.0f)
                curveTo(6.8954f, 19.0f, 6.0f, 18.1046f, 6.0f, 17.0f)
                verticalLineTo(15.8f)
                lineTo(7.6f, 17.4f)
                lineTo(9.0f, 16.0f)
                lineTo(5.0f, 12.0f)
                lineTo(1.0f, 16.0f)
                lineTo(2.4f, 17.4f)
                lineTo(4.0f, 15.8f)
                verticalLineTo(17.0f)
                curveTo(4.0f, 19.2091f, 5.7909f, 21.0f, 8.0f, 21.0f)
                horizontalLineTo(10.0f)
                verticalLineTo(19.0f)
                horizontalLineTo(8.0f)
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
                moveTo(5.0f, 3.0f)
                curveTo(3.8954f, 3.0f, 3.0f, 3.8954f, 3.0f, 5.0f)
                verticalLineTo(9.0f)
                curveTo(3.0f, 10.1046f, 3.8954f, 11.0f, 5.0f, 11.0f)
                horizontalLineTo(11.0f)
                curveTo(12.1046f, 11.0f, 13.0f, 10.1046f, 13.0f, 9.0f)
                verticalLineTo(5.0f)
                curveTo(13.0f, 3.8954f, 12.1046f, 3.0f, 11.0f, 3.0f)
                horizontalLineTo(5.0f)
                close()
                moveTo(11.0f, 5.0f)
                horizontalLineTo(5.0f)
                verticalLineTo(9.0f)
                horizontalLineTo(11.0f)
                verticalLineTo(5.0f)
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
                moveTo(11.0f, 15.0f)
                curveTo(11.0f, 13.8954f, 11.8954f, 13.0f, 13.0f, 13.0f)
                horizontalLineTo(19.0f)
                curveTo(20.1046f, 13.0f, 21.0f, 13.8954f, 21.0f, 15.0f)
                verticalLineTo(19.0f)
                curveTo(21.0f, 20.1046f, 20.1046f, 21.0f, 19.0f, 21.0f)
                horizontalLineTo(13.0f)
                curveTo(11.8954f, 21.0f, 11.0f, 20.1046f, 11.0f, 19.0f)
                verticalLineTo(15.0f)
                close()
            }
        }
            .build()
        return switchsplitscreen!!
    }

private var switchsplitscreen: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.Switchsplitscreen.IconPreview()
