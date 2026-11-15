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

val IconPack.TextSuperscript: ImageVector
    get() {
        if (textSuperscript != null) {
            return textSuperscript!!
        }
        textSuperscript = Builder(
            name = "TextSuperscript",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFF49454F)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(20.666f, 10.592f)
                verticalLineTo(12.0f)
                horizontalLineTo(15.163f)
                verticalLineTo(10.808f)
                lineTo(17.697f, 8.144f)
                curveTo(17.921f, 7.887f, 18.102f, 7.658f, 18.239f, 7.456f)
                curveTo(18.376f, 7.25f, 18.476f, 7.064f, 18.537f, 6.898f)
                curveTo(18.602f, 6.732f, 18.634f, 6.582f, 18.634f, 6.448f)
                curveTo(18.634f, 6.221f, 18.6f, 6.031f, 18.531f, 5.879f)
                curveTo(18.466f, 5.724f, 18.369f, 5.607f, 18.239f, 5.527f)
                curveTo(18.113f, 5.448f, 17.955f, 5.408f, 17.768f, 5.408f)
                curveTo(17.58f, 5.408f, 17.416f, 5.462f, 17.275f, 5.571f)
                curveTo(17.134f, 5.679f, 17.024f, 5.827f, 16.944f, 6.015f)
                curveTo(16.869f, 6.203f, 16.831f, 6.414f, 16.831f, 6.649f)
                horizontalLineTo(15.0f)
                curveTo(15.0f, 6.165f, 15.117f, 5.722f, 15.352f, 5.322f)
                curveTo(15.59f, 4.921f, 15.921f, 4.601f, 16.343f, 4.363f)
                curveTo(16.766f, 4.121f, 17.255f, 4.0f, 17.811f, 4.0f)
                curveTo(18.392f, 4.0f, 18.88f, 4.09f, 19.274f, 4.271f)
                curveTo(19.667f, 4.451f, 19.963f, 4.713f, 20.162f, 5.056f)
                curveTo(20.364f, 5.396f, 20.465f, 5.805f, 20.465f, 6.286f)
                curveTo(20.465f, 6.56f, 20.422f, 6.824f, 20.335f, 7.077f)
                curveTo(20.249f, 7.329f, 20.124f, 7.58f, 19.961f, 7.829f)
                curveTo(19.799f, 8.075f, 19.6f, 8.328f, 19.366f, 8.588f)
                curveTo(19.135f, 8.848f, 18.871f, 9.124f, 18.575f, 9.416f)
                lineTo(17.556f, 10.592f)
                horizontalLineTo(20.666f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF49454F)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(6.304f, 6.0f)
                lineTo(8.946f, 10.394f)
                horizontalLineTo(9.04f)
                lineTo(11.694f, 6.0f)
                horizontalLineTo(14.173f)
                lineTo(10.476f, 12.0f)
                lineTo(14.231f, 18.0f)
                horizontalLineTo(11.712f)
                lineTo(9.04f, 13.635f)
                horizontalLineTo(8.946f)
                lineTo(6.274f, 18.0f)
                horizontalLineTo(3.767f)
                lineTo(7.558f, 12.0f)
                lineTo(3.813f, 6.0f)
                horizontalLineTo(6.304f)
                close()
            }
        }
            .build()
        return textSuperscript!!
    }

private var textSuperscript: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.TextSuperscript.IconPreview()
