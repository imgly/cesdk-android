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

public val IconPack.Warning: ImageVector
    get() {
        if (warning != null) {
            return warning!!
        }
        warning = Builder(
            name = "Warning",
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
                moveTo(2.725f, 21.0f)
                curveTo(2.542f, 21.0f, 2.375f, 20.954f, 2.225f, 20.863f)
                curveTo(2.075f, 20.772f, 1.959f, 20.651f, 1.875f, 20.5f)
                curveTo(1.792f, 20.349f, 1.746f, 20.187f, 1.738f, 20.012f)
                curveTo(1.73f, 19.837f, 1.776f, 19.667f, 1.875f, 19.5f)
                lineTo(11.125f, 3.5f)
                curveTo(11.225f, 3.333f, 11.354f, 3.208f, 11.513f, 3.125f)
                curveTo(11.672f, 3.042f, 11.834f, 3.0f, 12.0f, 3.0f)
                curveTo(12.166f, 3.0f, 12.329f, 3.042f, 12.488f, 3.125f)
                curveTo(12.648f, 3.208f, 12.776f, 3.333f, 12.875f, 3.5f)
                lineTo(22.125f, 19.5f)
                curveTo(22.225f, 19.667f, 22.271f, 19.838f, 22.263f, 20.013f)
                curveTo(22.255f, 20.188f, 22.209f, 20.351f, 22.125f, 20.5f)
                curveTo(22.041f, 20.649f, 21.924f, 20.77f, 21.775f, 20.863f)
                curveTo(21.626f, 20.956f, 21.459f, 21.001f, 21.275f, 21.0f)
                horizontalLineTo(2.725f)
                close()
                moveTo(12.713f, 17.713f)
                curveTo(12.905f, 17.521f, 13.0f, 17.283f, 13.0f, 17.0f)
                curveTo(13.0f, 16.717f, 12.904f, 16.479f, 12.712f, 16.288f)
                curveTo(12.52f, 16.097f, 12.283f, 16.001f, 12.0f, 16.0f)
                curveTo(11.717f, 15.999f, 11.48f, 16.095f, 11.288f, 16.288f)
                curveTo(11.096f, 16.481f, 11.0f, 16.718f, 11.0f, 17.0f)
                curveTo(11.0f, 17.282f, 11.096f, 17.52f, 11.288f, 17.713f)
                curveTo(11.48f, 17.906f, 11.717f, 18.002f, 12.0f, 18.0f)
                curveTo(12.283f, 17.998f, 12.521f, 17.903f, 12.713f, 17.713f)
                close()
                moveTo(12.713f, 14.712f)
                curveTo(12.905f, 14.521f, 13.0f, 14.283f, 13.0f, 14.0f)
                verticalLineTo(11.0f)
                curveTo(13.0f, 10.717f, 12.904f, 10.479f, 12.712f, 10.288f)
                curveTo(12.52f, 10.097f, 12.283f, 10.001f, 12.0f, 10.0f)
                curveTo(11.717f, 9.999f, 11.48f, 10.095f, 11.288f, 10.288f)
                curveTo(11.096f, 10.481f, 11.0f, 10.718f, 11.0f, 11.0f)
                verticalLineTo(14.0f)
                curveTo(11.0f, 14.283f, 11.096f, 14.521f, 11.288f, 14.713f)
                curveTo(11.48f, 14.905f, 11.717f, 15.001f, 12.0f, 15.0f)
                curveTo(12.283f, 14.999f, 12.521f, 14.903f, 12.713f, 14.712f)
                close()
            }
        }
            .build()
        return warning!!
    }

private var warning: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.Warning.IconPreview()
