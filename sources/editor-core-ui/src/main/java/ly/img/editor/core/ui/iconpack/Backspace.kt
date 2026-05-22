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

public val IconPack.Backspace: ImageVector
    get() {
        if (backspace != null) {
            return backspace!!
        }
        backspace = Builder(
            name = "Backspace",
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
                pathFillType = EvenOdd,
            ) {
                moveTo(21.999f, 3.0f)
                curveTo(22.53f, 3.0f, 23.039f, 3.211f, 23.414f, 3.586f)
                curveTo(23.789f, 3.961f, 23.999f, 4.47f, 23.999f, 5.0f)
                verticalLineTo(19.0f)
                curveTo(23.999f, 19.53f, 23.789f, 20.039f, 23.414f, 20.414f)
                curveTo(23.039f, 20.789f, 22.53f, 21.0f, 21.999f, 21.0f)
                horizontalLineTo(6.999f)
                curveTo(6.31f, 21.0f, 5.769f, 20.64f, 5.41f, 20.11f)
                lineTo(0.74f, 13.109f)
                curveTo(0.292f, 12.438f, 0.291f, 11.563f, 0.739f, 10.891f)
                lineTo(5.41f, 3.88f)
                curveTo(5.769f, 3.35f, 6.31f, 3.0f, 6.999f, 3.0f)
                horizontalLineTo(21.999f)
                close()
                moveTo(18.294f, 7.705f)
                curveTo(17.905f, 7.316f, 17.274f, 7.316f, 16.885f, 7.705f)
                lineTo(13.999f, 10.59f)
                lineTo(11.115f, 7.705f)
                curveTo(10.725f, 7.316f, 10.094f, 7.316f, 9.704f, 7.705f)
                curveTo(9.315f, 8.094f, 9.315f, 8.726f, 9.704f, 9.115f)
                lineTo(12.59f, 12.0f)
                lineTo(9.704f, 14.885f)
                curveTo(9.315f, 15.274f, 9.315f, 15.906f, 9.704f, 16.295f)
                curveTo(10.094f, 16.684f, 10.725f, 16.684f, 11.115f, 16.295f)
                lineTo(13.999f, 13.41f)
                lineTo(16.885f, 16.295f)
                curveTo(17.274f, 16.684f, 17.905f, 16.684f, 18.294f, 16.295f)
                curveTo(18.684f, 15.906f, 18.684f, 15.274f, 18.294f, 14.885f)
                lineTo(15.41f, 12.0f)
                lineTo(18.294f, 9.115f)
                curveTo(18.684f, 8.726f, 18.684f, 8.094f, 18.294f, 7.705f)
                close()
            }
        }
            .build()
        return backspace!!
    }

private var backspace: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.Backspace.IconPreview()
