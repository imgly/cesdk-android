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

val IconPack.TextSubscript: ImageVector
    get() {
        if (textSubscript != null) {
            return textSubscript!!
        }
        textSubscript = Builder(
            name = "TextSubscript",
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
                moveTo(20.666f, 18.592f)
                verticalLineTo(20.0f)
                horizontalLineTo(15.163f)
                verticalLineTo(18.808f)
                lineTo(17.697f, 16.143f)
                curveTo(17.921f, 15.887f, 18.102f, 15.658f, 18.239f, 15.456f)
                curveTo(18.376f, 15.25f, 18.476f, 15.064f, 18.537f, 14.898f)
                curveTo(18.602f, 14.732f, 18.634f, 14.582f, 18.634f, 14.448f)
                curveTo(18.634f, 14.221f, 18.6f, 14.031f, 18.531f, 13.88f)
                curveTo(18.466f, 13.724f, 18.369f, 13.607f, 18.239f, 13.527f)
                curveTo(18.113f, 13.448f, 17.955f, 13.408f, 17.768f, 13.408f)
                curveTo(17.58f, 13.408f, 17.416f, 13.462f, 17.275f, 13.571f)
                curveTo(17.134f, 13.679f, 17.024f, 13.827f, 16.944f, 14.015f)
                curveTo(16.869f, 14.203f, 16.831f, 14.414f, 16.831f, 14.649f)
                horizontalLineTo(15.0f)
                curveTo(15.0f, 14.165f, 15.117f, 13.722f, 15.352f, 13.322f)
                curveTo(15.59f, 12.921f, 15.921f, 12.601f, 16.343f, 12.363f)
                curveTo(16.766f, 12.121f, 17.255f, 12.0f, 17.811f, 12.0f)
                curveTo(18.392f, 12.0f, 18.88f, 12.09f, 19.274f, 12.271f)
                curveTo(19.667f, 12.451f, 19.963f, 12.713f, 20.162f, 13.056f)
                curveTo(20.364f, 13.396f, 20.465f, 13.806f, 20.465f, 14.286f)
                curveTo(20.465f, 14.56f, 20.422f, 14.824f, 20.335f, 15.076f)
                curveTo(20.249f, 15.329f, 20.124f, 15.58f, 19.961f, 15.829f)
                curveTo(19.799f, 16.075f, 19.6f, 16.328f, 19.366f, 16.588f)
                curveTo(19.135f, 16.848f, 18.871f, 17.124f, 18.575f, 17.416f)
                lineTo(17.556f, 18.592f)
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
        return textSubscript!!
    }

private var textSubscript: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.TextSubscript.IconPreview()
