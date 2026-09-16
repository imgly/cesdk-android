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

val IconPack.Photocamera: ImageVector
    get() {
        if (photocamera != null) {
            return photocamera!!
        }
        photocamera = Builder(
            name = "Photocamera",
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
                moveTo(12.0f, 17.5f)
                curveTo(13.25f, 17.5f, 14.313f, 17.062f, 15.188f, 16.187f)
                curveTo(16.063f, 15.312f, 16.501f, 14.249f, 16.5f, 13.0f)
                curveTo(16.5f, 11.75f, 16.062f, 10.687f, 15.187f, 9.812f)
                curveTo(14.312f, 8.937f, 13.249f, 8.499f, 12.0f, 8.5f)
                curveTo(10.75f, 8.5f, 9.687f, 8.938f, 8.812f, 9.813f)
                curveTo(7.937f, 10.688f, 7.499f, 11.751f, 7.5f, 13.0f)
                curveTo(7.5f, 14.25f, 7.938f, 15.313f, 8.813f, 16.188f)
                curveTo(9.688f, 17.063f, 10.751f, 17.501f, 12.0f, 17.5f)
                close()
                moveTo(12.0f, 15.5f)
                curveTo(11.3f, 15.5f, 10.708f, 15.258f, 10.225f, 14.775f)
                curveTo(9.742f, 14.292f, 9.5f, 13.7f, 9.5f, 13.0f)
                curveTo(9.5f, 12.3f, 9.742f, 11.708f, 10.225f, 11.225f)
                curveTo(10.708f, 10.742f, 11.3f, 10.5f, 12.0f, 10.5f)
                curveTo(12.7f, 10.5f, 13.292f, 10.742f, 13.775f, 11.225f)
                curveTo(14.258f, 11.708f, 14.5f, 12.3f, 14.5f, 13.0f)
                curveTo(14.5f, 13.7f, 14.258f, 14.292f, 13.775f, 14.775f)
                curveTo(13.292f, 15.258f, 12.7f, 15.5f, 12.0f, 15.5f)
                close()
                moveTo(4.0f, 21.0f)
                curveTo(3.45f, 21.0f, 2.979f, 20.804f, 2.587f, 20.412f)
                curveTo(2.195f, 20.02f, 1.999f, 19.549f, 2.0f, 19.0f)
                verticalLineTo(7.0f)
                curveTo(2.0f, 6.45f, 2.196f, 5.979f, 2.588f, 5.587f)
                curveTo(2.98f, 5.195f, 3.451f, 4.999f, 4.0f, 5.0f)
                horizontalLineTo(7.15f)
                lineTo(9.0f, 3.0f)
                horizontalLineTo(15.0f)
                lineTo(16.85f, 5.0f)
                horizontalLineTo(20.0f)
                curveTo(20.55f, 5.0f, 21.021f, 5.196f, 21.413f, 5.588f)
                curveTo(21.805f, 5.98f, 22.001f, 6.451f, 22.0f, 7.0f)
                verticalLineTo(19.0f)
                curveTo(22.0f, 19.55f, 21.804f, 20.021f, 21.412f, 20.413f)
                curveTo(21.02f, 20.805f, 20.549f, 21.001f, 20.0f, 21.0f)
                horizontalLineTo(4.0f)
                close()
            }
        }.build()
        return photocamera!!
    }

private var photocamera: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.Photocamera.IconPreview()
