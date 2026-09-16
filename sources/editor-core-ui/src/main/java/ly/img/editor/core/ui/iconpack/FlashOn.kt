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

public val IconPack.FlashOn: ImageVector
    get() {
        if (flashOn != null) {
            return flashOn!!
        }
        flashOn = Builder(
            name = "FlashOn",
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
                moveTo(10.15f, 20.063f)
                curveTo(10.05f, 19.938f, 10.0f, 19.783f, 10.0f, 19.6f)
                verticalLineTo(14.0f)
                horizontalLineTo(9.0f)
                curveTo(8.45f, 14.0f, 7.979f, 13.804f, 7.588f, 13.413f)
                curveTo(7.197f, 13.022f, 7.001f, 12.551f, 7.0f, 12.0f)
                verticalLineTo(4.0f)
                curveTo(7.0f, 3.45f, 7.196f, 2.979f, 7.588f, 2.588f)
                curveTo(7.98f, 2.197f, 8.451f, 2.001f, 9.0f, 2.0f)
                horizontalLineTo(14.85f)
                curveTo(15.383f, 2.0f, 15.813f, 2.208f, 16.138f, 2.625f)
                curveTo(16.463f, 3.042f, 16.559f, 3.5f, 16.425f, 4.0f)
                lineTo(15.0f, 9.0f)
                horizontalLineTo(16.125f)
                curveTo(16.725f, 9.0f, 17.171f, 9.267f, 17.463f, 9.8f)
                curveTo(17.755f, 10.333f, 17.726f, 10.85f, 17.375f, 11.35f)
                lineTo(11.375f, 20.025f)
                curveTo(11.275f, 20.175f, 11.146f, 20.275f, 10.988f, 20.325f)
                curveTo(10.83f, 20.375f, 10.676f, 20.375f, 10.525f, 20.325f)
                curveTo(10.374f, 20.275f, 10.249f, 20.188f, 10.15f, 20.063f)
                close()
            }
        }
            .build()
        return flashOn!!
    }

private var flashOn: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.FlashOn.IconPreview()
