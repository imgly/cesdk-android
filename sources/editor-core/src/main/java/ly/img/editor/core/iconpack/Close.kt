package ly.img.editor.core.iconpack

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

public val IconPack.Close: ImageVector
    get() {
        if (close != null) {
            return close!!
        }
        close = Builder(
            name = "Close",
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
                moveTo(12.0f, 13.4f)
                lineTo(7.1f, 18.3f)
                curveTo(6.916f, 18.483f, 6.683f, 18.575f, 6.4f, 18.575f)
                curveTo(6.116f, 18.575f, 5.883f, 18.483f, 5.7f, 18.3f)
                curveTo(5.516f, 18.117f, 5.425f, 17.883f, 5.425f, 17.6f)
                curveTo(5.425f, 17.316f, 5.516f, 17.083f, 5.7f, 16.9f)
                lineTo(10.6f, 12.0f)
                lineTo(5.7f, 7.1f)
                curveTo(5.516f, 6.916f, 5.425f, 6.683f, 5.425f, 6.4f)
                curveTo(5.425f, 6.116f, 5.516f, 5.883f, 5.7f, 5.7f)
                curveTo(5.883f, 5.516f, 6.116f, 5.425f, 6.4f, 5.425f)
                curveTo(6.683f, 5.425f, 6.916f, 5.516f, 7.1f, 5.7f)
                lineTo(12.0f, 10.6f)
                lineTo(16.9f, 5.7f)
                curveTo(17.083f, 5.516f, 17.316f, 5.425f, 17.6f, 5.425f)
                curveTo(17.883f, 5.425f, 18.117f, 5.516f, 18.3f, 5.7f)
                curveTo(18.483f, 5.883f, 18.575f, 6.116f, 18.575f, 6.4f)
                curveTo(18.575f, 6.683f, 18.483f, 6.916f, 18.3f, 7.1f)
                lineTo(13.4f, 12.0f)
                lineTo(18.3f, 16.9f)
                curveTo(18.483f, 17.083f, 18.575f, 17.316f, 18.575f, 17.6f)
                curveTo(18.575f, 17.883f, 18.483f, 18.117f, 18.3f, 18.3f)
                curveTo(18.117f, 18.483f, 17.883f, 18.575f, 17.6f, 18.575f)
                curveTo(17.316f, 18.575f, 17.083f, 18.483f, 16.9f, 18.3f)
                lineTo(12.0f, 13.4f)
                close()
            }
        }
            .build()
        return close!!
    }

private var close: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.Close.IconPreview()
