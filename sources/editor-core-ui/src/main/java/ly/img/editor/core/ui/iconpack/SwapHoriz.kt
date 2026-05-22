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

public val IconPack.SwapHoriz: ImageVector
    get() {
        if (swapHoriz != null) {
            return swapHoriz!!
        }
        swapHoriz = Builder(
            name = "SwapHoriz",
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
                moveTo(5.825f, 16.0f)
                lineTo(7.7f, 17.875f)
                curveTo(7.883f, 18.058f, 7.975f, 18.288f, 7.975f, 18.563f)
                curveTo(7.975f, 18.838f, 7.883f, 19.076f, 7.7f, 19.275f)
                curveTo(7.5f, 19.475f, 7.263f, 19.575f, 6.988f, 19.575f)
                curveTo(6.713f, 19.575f, 6.476f, 19.475f, 6.275f, 19.275f)
                lineTo(2.7f, 15.7f)
                curveTo(2.6f, 15.6f, 2.529f, 15.491f, 2.487f, 15.375f)
                curveTo(2.445f, 15.258f, 2.425f, 15.133f, 2.426f, 15.0f)
                curveTo(2.427f, 14.866f, 2.448f, 14.741f, 2.489f, 14.625f)
                curveTo(2.53f, 14.508f, 2.6f, 14.4f, 2.701f, 14.3f)
                lineTo(6.301f, 10.7f)
                curveTo(6.501f, 10.5f, 6.734f, 10.404f, 7.001f, 10.413f)
                curveTo(7.268f, 10.422f, 7.501f, 10.526f, 7.701f, 10.725f)
                curveTo(7.884f, 10.925f, 7.98f, 11.158f, 7.989f, 11.425f)
                curveTo(7.998f, 11.691f, 7.902f, 11.925f, 7.701f, 12.125f)
                lineTo(5.825f, 14.0f)
                horizontalLineTo(12.0f)
                curveTo(12.283f, 14.0f, 12.521f, 14.096f, 12.713f, 14.288f)
                curveTo(12.905f, 14.48f, 13.0f, 14.717f, 13.0f, 15.0f)
                curveTo(12.999f, 15.283f, 12.903f, 15.52f, 12.712f, 15.713f)
                curveTo(12.521f, 15.906f, 12.283f, 16.001f, 12.0f, 16.0f)
                horizontalLineTo(5.825f)
                close()
                moveTo(18.175f, 10.0f)
                horizontalLineTo(12.0f)
                curveTo(11.717f, 10.0f, 11.479f, 9.904f, 11.288f, 9.712f)
                curveTo(11.097f, 9.52f, 11.0f, 9.282f, 11.0f, 9.0f)
                curveTo(10.999f, 8.717f, 11.095f, 8.48f, 11.288f, 8.288f)
                curveTo(11.481f, 8.096f, 11.718f, 8.0f, 12.0f, 8.0f)
                horizontalLineTo(18.175f)
                lineTo(16.3f, 6.125f)
                curveTo(16.117f, 5.941f, 16.025f, 5.712f, 16.025f, 5.438f)
                curveTo(16.025f, 5.163f, 16.117f, 4.925f, 16.3f, 4.725f)
                curveTo(16.5f, 4.525f, 16.737f, 4.425f, 17.013f, 4.425f)
                curveTo(17.288f, 4.425f, 17.525f, 4.525f, 17.725f, 4.725f)
                lineTo(21.3f, 8.3f)
                curveTo(21.4f, 8.4f, 21.471f, 8.508f, 21.512f, 8.625f)
                curveTo(21.553f, 8.741f, 21.574f, 8.866f, 21.575f, 9.0f)
                curveTo(21.576f, 9.133f, 21.555f, 9.258f, 21.512f, 9.375f)
                curveTo(21.469f, 9.491f, 21.399f, 9.6f, 21.3f, 9.7f)
                lineTo(17.7f, 13.3f)
                curveTo(17.5f, 13.5f, 17.267f, 13.596f, 17.0f, 13.588f)
                curveTo(16.733f, 13.58f, 16.5f, 13.476f, 16.3f, 13.275f)
                curveTo(16.117f, 13.075f, 16.021f, 12.842f, 16.012f, 12.575f)
                curveTo(16.003f, 12.308f, 16.099f, 12.075f, 16.3f, 11.875f)
                lineTo(18.175f, 10.0f)
                close()
            }
        }
            .build()
        return swapHoriz!!
    }

private var swapHoriz: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.SwapHoriz.IconPreview()
