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

public val IconPack.Arrowrightbig: ImageVector
    get() {
        if (arrowrightbig != null) {
            return arrowrightbig!!
        }
        arrowrightbig = Builder(
            name = "Arrowrightbig",
            defaultWidth = 24.0.dp,
            defaultHeight =
                24.0.dp,
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
                moveTo(16.175f, 13.0f)
                horizontalLineTo(5.0f)
                curveTo(4.717f, 13.0f, 4.479f, 12.904f, 4.288f, 12.712f)
                curveTo(4.097f, 12.52f, 4.001f, 12.282f, 4.0f, 12.0f)
                curveTo(3.999f, 11.717f, 4.095f, 11.48f, 4.288f, 11.288f)
                curveTo(4.481f, 11.096f, 4.718f, 11.0f, 5.0f, 11.0f)
                horizontalLineTo(16.175f)
                lineTo(11.275f, 6.1f)
                curveTo(11.075f, 5.9f, 10.979f, 5.666f, 10.987f, 5.4f)
                curveTo(10.995f, 5.133f, 11.099f, 4.9f, 11.3f, 4.7f)
                curveTo(11.5f, 4.516f, 11.733f, 4.42f, 12.0f, 4.412f)
                curveTo(12.267f, 4.403f, 12.5f, 4.499f, 12.7f, 4.7f)
                lineTo(19.3f, 11.3f)
                curveTo(19.4f, 11.4f, 19.471f, 11.508f, 19.513f, 11.625f)
                curveTo(19.555f, 11.741f, 19.576f, 11.866f, 19.575f, 12.0f)
                curveTo(19.574f, 12.133f, 19.554f, 12.258f, 19.513f, 12.375f)
                curveTo(19.472f, 12.491f, 19.401f, 12.6f, 19.3f, 12.7f)
                lineTo(12.7f, 19.3f)
                curveTo(12.517f, 19.483f, 12.288f, 19.575f, 12.013f, 19.575f)
                curveTo(11.738f, 19.575f, 11.501f, 19.483f, 11.3f, 19.3f)
                curveTo(11.1f, 19.1f, 11.0f, 18.862f, 11.0f, 18.588f)
                curveTo(11.0f, 18.313f, 11.1f, 18.075f, 11.3f, 17.875f)
                lineTo(16.175f, 13.0f)
                close()
            }
        }
            .build()
        return arrowrightbig!!
    }

private var arrowrightbig: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.Arrowrightbig.IconPreview()
