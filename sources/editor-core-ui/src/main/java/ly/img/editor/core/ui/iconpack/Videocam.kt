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

val IconPack.Videocam: ImageVector
    get() {
        if (videocam != null) {
            return videocam!!
        }
        videocam = Builder(
            name = "Videocam",
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
                moveTo(4.0f, 20.0f)
                curveTo(3.45f, 20.0f, 2.979f, 19.804f, 2.587f, 19.412f)
                curveTo(2.195f, 19.02f, 1.999f, 18.549f, 2.0f, 18.0f)
                verticalLineTo(6.0f)
                curveTo(2.0f, 5.45f, 2.196f, 4.979f, 2.588f, 4.587f)
                curveTo(2.98f, 4.195f, 3.451f, 3.999f, 4.0f, 4.0f)
                horizontalLineTo(16.0f)
                curveTo(16.55f, 4.0f, 17.021f, 4.196f, 17.413f, 4.588f)
                curveTo(17.805f, 4.98f, 18.001f, 5.451f, 18.0f, 6.0f)
                verticalLineTo(10.5f)
                lineTo(22.0f, 6.5f)
                verticalLineTo(17.5f)
                lineTo(18.0f, 13.5f)
                verticalLineTo(18.0f)
                curveTo(18.0f, 18.55f, 17.804f, 19.021f, 17.412f, 19.413f)
                curveTo(17.02f, 19.805f, 16.549f, 20.001f, 16.0f, 20.0f)
                horizontalLineTo(4.0f)
                close()
            }
        }.build()
        return videocam!!
    }

private var videocam: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.Videocam.IconPreview()
