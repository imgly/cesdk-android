package ly.img.editor.core.iconpack

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

val IconPack.TextPresets: ImageVector
    get() {
        if (textPresets != null) {
            return textPresets!!
        }
        textPresets = Builder(
            name = "TextPresets",
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
                moveTo(7.947f, 13.199f)
                lineTo(6.28f, 5.949f)
                curveTo(6.033f, 4.872f, 6.705f, 3.799f, 7.781f, 3.551f)
                lineTo(11.083f, 2.792f)
                curveTo(10.748f, 3.211f, 10.491f, 3.705f, 10.344f, 4.256f)
                lineTo(7.947f, 13.199f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF49454F)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(4.33f, 6.396f)
                curveTo(4.148f, 5.603f, 4.215f, 4.811f, 4.481f, 4.101f)
                lineTo(1.955f, 5.945f)
                curveTo(1.063f, 6.597f, 0.867f, 7.848f, 1.519f, 8.74f)
                lineTo(6.409f, 15.439f)
                lineTo(4.33f, 6.396f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF49454F)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = EvenOdd,
            ) {
                moveTo(17.098f, 21.139f)
                curveTo(18.165f, 21.425f, 19.261f, 20.792f, 19.547f, 19.725f)
                lineTo(22.798f, 7.593f)
                curveTo(23.084f, 6.526f, 22.451f, 5.429f, 21.384f, 5.143f)
                lineTo(14.725f, 3.359f)
                curveTo(13.658f, 3.073f, 12.561f, 3.706f, 12.276f, 4.773f)
                lineTo(9.025f, 16.905f)
                curveTo(8.739f, 17.972f, 9.372f, 19.069f, 10.439f, 19.355f)
                lineTo(17.098f, 21.139f)
                close()
                moveTo(12.595f, 15.0f)
                curveTo(13.424f, 15.0f, 14.095f, 15.671f, 14.095f, 16.5f)
                curveTo(14.095f, 17.328f, 13.424f, 18.0f, 12.595f, 18.0f)
                curveTo(11.767f, 18.0f, 11.095f, 17.328f, 11.095f, 16.5f)
                curveTo(11.095f, 15.671f, 11.767f, 15.0f, 12.595f, 15.0f)
                close()
            }
        }.build()
        return textPresets!!
    }

private var textPresets: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.TextPresets.IconPreview()
