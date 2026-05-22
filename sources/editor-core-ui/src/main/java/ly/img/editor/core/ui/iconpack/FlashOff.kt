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

public val IconPack.FlashOff: ImageVector
    get() {
        if (flashOff != null) {
            return flashOff!!
        }
        flashOff = Builder(
            name = "FlashOff",
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
                moveTo(15.225f, 12.375f)
                lineTo(7.45f, 4.6f)
                curveTo(7.3f, 4.45f, 7.191f, 4.296f, 7.125f, 4.138f)
                curveTo(7.058f, 3.98f, 7.025f, 3.809f, 7.025f, 3.625f)
                curveTo(7.025f, 3.208f, 7.191f, 2.833f, 7.525f, 2.5f)
                curveTo(7.858f, 2.167f, 8.266f, 2.0f, 8.75f, 2.0f)
                horizontalLineTo(14.85f)
                curveTo(15.383f, 2.0f, 15.812f, 2.208f, 16.138f, 2.625f)
                curveTo(16.463f, 3.042f, 16.559f, 3.5f, 16.425f, 4.0f)
                lineTo(15.0f, 9.0f)
                horizontalLineTo(16.125f)
                curveTo(16.741f, 9.0f, 17.187f, 9.267f, 17.463f, 9.8f)
                curveTo(17.738f, 10.333f, 17.709f, 10.85f, 17.375f, 11.35f)
                lineTo(16.75f, 12.25f)
                curveTo(16.566f, 12.5f, 16.325f, 12.642f, 16.025f, 12.675f)
                curveTo(15.725f, 12.708f, 15.458f, 12.608f, 15.225f, 12.375f)
                close()
                moveTo(19.075f, 21.9f)
                lineTo(13.75f, 16.6f)
                lineTo(11.375f, 20.025f)
                curveTo(11.291f, 20.158f, 11.17f, 20.254f, 11.012f, 20.313f)
                curveTo(10.853f, 20.372f, 10.691f, 20.376f, 10.525f, 20.325f)
                curveTo(10.359f, 20.274f, 10.229f, 20.183f, 10.137f, 20.05f)
                curveTo(10.044f, 19.917f, 9.998f, 19.767f, 10.0f, 19.6f)
                verticalLineTo(14.0f)
                horizontalLineTo(9.0f)
                curveTo(8.45f, 14.0f, 7.979f, 13.804f, 7.588f, 13.413f)
                curveTo(7.196f, 13.022f, 7.0f, 12.551f, 7.0f, 12.0f)
                verticalLineTo(9.85f)
                lineTo(2.075f, 4.925f)
                curveTo(1.875f, 4.725f, 1.779f, 4.488f, 1.787f, 4.213f)
                curveTo(1.795f, 3.938f, 1.899f, 3.701f, 2.1f, 3.5f)
                curveTo(2.3f, 3.299f, 2.538f, 3.199f, 2.813f, 3.2f)
                curveTo(3.087f, 3.201f, 3.325f, 3.301f, 3.525f, 3.5f)
                lineTo(20.5f, 20.5f)
                curveTo(20.7f, 20.7f, 20.8f, 20.933f, 20.8f, 21.2f)
                curveTo(20.8f, 21.467f, 20.7f, 21.7f, 20.5f, 21.9f)
                curveTo(20.3f, 22.1f, 20.062f, 22.2f, 19.788f, 22.2f)
                curveTo(19.513f, 22.2f, 19.275f, 22.1f, 19.075f, 21.9f)
                close()
            }
        }
            .build()
        return flashOff!!
    }

private var flashOff: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.FlashOff.IconPreview()
