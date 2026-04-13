package ly.img.editor.core.iconpack

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.EvenOdd
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

public val IconPack.VoiceoverAdd: ImageVector
    get() {
        if (voiceoverAdd != null) {
            return voiceoverAdd!!
        }
        voiceoverAdd = Builder(
            name = "Voiceoveradd",
            defaultWidth = 20.0.dp,
            defaultHeight = 21.0.dp,
            viewportWidth = 20.0f,
            viewportHeight = 21.0f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = EvenOdd,
            ) {
                moveTo(10.0f, 5.0f)
                curveTo(10.0f, 2.239f, 12.239f, 0.0f, 15.0f, 0.0f)
                curveTo(17.761f, 0.0f, 20.0f, 2.239f, 20.0f, 5.0f)
                curveTo(20.0f, 7.761f, 17.761f, 10.0f, 15.0f, 10.0f)
                curveTo(12.239f, 10.0f, 10.0f, 7.761f, 10.0f, 5.0f)
                close()
                moveTo(14.5f, 2.0f)
                verticalLineTo(4.5f)
                horizontalLineTo(12.0f)
                verticalLineTo(5.5f)
                horizontalLineTo(14.5f)
                verticalLineTo(8.0f)
                horizontalLineTo(15.5f)
                verticalLineTo(5.5f)
                horizontalLineTo(18.0f)
                verticalLineTo(4.5f)
                horizontalLineTo(15.5f)
                verticalLineTo(2.0f)
                horizontalLineTo(14.5f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF000000)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(8.0f, 5.0f)
                curveTo(8.0f, 4.078f, 8.178f, 3.197f, 8.503f, 2.39f)
                curveTo(8.058f, 2.13f, 7.557f, 2.0f, 7.0f, 2.0f)
                curveTo(6.167f, 2.0f, 5.458f, 2.292f, 4.875f, 2.875f)
                curveTo(4.292f, 3.458f, 4.0f, 4.167f, 4.0f, 5.0f)
                verticalLineTo(11.0f)
                curveTo(4.0f, 11.833f, 4.292f, 12.542f, 4.875f, 13.125f)
                curveTo(5.458f, 13.708f, 6.167f, 14.0f, 7.0f, 14.0f)
                curveTo(7.833f, 14.0f, 8.542f, 13.708f, 9.125f, 13.125f)
                curveTo(9.708f, 12.542f, 10.0f, 11.833f, 10.0f, 11.0f)
                verticalLineTo(9.899f)
                curveTo(8.763f, 8.636f, 8.0f, 6.907f, 8.0f, 5.0f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF000000)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(13.945f, 11.921f)
                curveTo(13.256f, 11.817f, 12.599f, 11.612f, 11.991f, 11.322f)
                curveTo(11.922f, 12.567f, 11.437f, 13.639f, 10.538f, 14.537f)
                curveTo(9.562f, 15.512f, 8.383f, 15.999f, 7.0f, 16.0f)
                curveTo(5.617f, 16.001f, 4.438f, 15.513f, 3.463f, 14.538f)
                curveTo(2.488f, 13.563f, 2.0f, 12.383f, 2.0f, 11.0f)
                horizontalLineTo(0.0f)
                curveTo(0.0f, 12.75f, 0.567f, 14.283f, 1.7f, 15.6f)
                curveTo(2.833f, 16.917f, 4.267f, 17.692f, 6.0f, 17.925f)
                verticalLineTo(21.0f)
                horizontalLineTo(8.0f)
                verticalLineTo(17.925f)
                curveTo(9.733f, 17.692f, 11.167f, 16.917f, 12.3f, 15.6f)
                curveTo(13.23f, 14.52f, 13.778f, 13.293f, 13.945f, 11.921f)
                close()
            }
        }.build()
        return voiceoverAdd!!
    }

private var voiceoverAdd: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.VoiceoverAdd.IconPreview()
