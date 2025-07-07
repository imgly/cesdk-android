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

val IconPack.Reset: ImageVector
    get() {
        if (reset != null) {
            return reset!!
        }
        reset = Builder(
            name = "Reset",
            defaultWidth = 18.0.dp,
            defaultHeight = 18.0.dp,
            viewportWidth = 18.0f,
            viewportHeight = 18.0f,
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
                moveTo(9.0f, 15.97f)
                curveTo(7.275f, 15.97f, 5.772f, 15.398f, 4.491f, 14.254f)
                curveTo(3.21f, 13.11f, 2.475f, 11.682f, 2.287f, 9.97f)
                horizontalLineTo(3.825f)
                curveTo(4.0f, 11.27f, 4.578f, 12.345f, 5.559f, 13.195f)
                curveTo(6.541f, 14.045f, 7.688f, 14.47f, 9.0f, 14.47f)
                curveTo(10.462f, 14.47f, 11.703f, 13.96f, 12.722f, 12.941f)
                curveTo(13.741f, 11.923f, 14.25f, 10.682f, 14.25f, 9.22f)
                curveTo(14.25f, 7.757f, 13.741f, 6.516f, 12.722f, 5.497f)
                curveTo(11.703f, 4.479f, 10.462f, 3.97f, 9.0f, 3.97f)
                curveTo(8.137f, 3.97f, 7.331f, 4.17f, 6.581f, 4.57f)
                curveTo(5.831f, 4.97f, 5.2f, 5.52f, 4.688f, 6.22f)
                horizontalLineTo(6.75f)
                verticalLineTo(7.72f)
                horizontalLineTo(2.25f)
                verticalLineTo(3.22f)
                horizontalLineTo(3.75f)
                verticalLineTo(4.982f)
                curveTo(4.387f, 4.182f, 5.166f, 3.563f, 6.085f, 3.126f)
                curveTo(7.003f, 2.688f, 7.975f, 2.47f, 9.0f, 2.47f)
                curveTo(9.938f, 2.47f, 10.816f, 2.648f, 11.635f, 3.004f)
                curveTo(12.453f, 3.36f, 13.166f, 3.841f, 13.772f, 4.447f)
                curveTo(14.378f, 5.054f, 14.859f, 5.766f, 15.216f, 6.585f)
                curveTo(15.572f, 7.404f, 15.75f, 8.282f, 15.75f, 9.22f)
                curveTo(15.75f, 10.157f, 15.572f, 11.035f, 15.216f, 11.854f)
                curveTo(14.859f, 12.673f, 14.378f, 13.385f, 13.772f, 13.991f)
                curveTo(13.166f, 14.598f, 12.453f, 15.079f, 11.635f, 15.436f)
                curveTo(10.816f, 15.792f, 9.938f, 15.97f, 9.0f, 15.97f)
                close()
            }
        }
            .build()
        return reset!!
    }

private var reset: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.Reset.IconPreview()
