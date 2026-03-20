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

public val IconPack.Muteotheraudio: ImageVector
    get() {
        if (muteotheraudio != null) {
            return muteotheraudio!!
        }
        muteotheraudio = Builder(
            name = "Muteotheraudio",
            defaultWidth = 18.0.dp,
            defaultHeight = 17.54.dp,
            viewportWidth = 18.0f,
            viewportHeight = 17.54f,
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
                moveTo(0.0f, 5.77f)
                verticalLineTo(11.77f)
                horizontalLineTo(4.0f)
                lineTo(9.0f, 16.77f)
                verticalLineTo(0.77f)
                lineTo(4.0f, 5.77f)
                horizontalLineTo(0.0f)
                close()
                moveTo(7.0f, 5.6f)
                verticalLineTo(11.94f)
                lineTo(4.83f, 9.77f)
                horizontalLineTo(2.0f)
                verticalLineTo(7.77f)
                horizontalLineTo(4.83f)
                lineTo(7.0f, 5.6f)
                close()
                moveTo(13.5f, 8.77f)
                curveTo(13.5f, 7.0f, 12.48f, 5.48f, 11.0f, 4.74f)
                verticalLineTo(12.79f)
                curveTo(12.48f, 12.06f, 13.5f, 10.54f, 13.5f, 8.77f)
                close()
                moveTo(11.0f, 0.0f)
                verticalLineTo(2.06f)
                curveTo(13.89f, 2.92f, 16.0f, 5.6f, 16.0f, 8.77f)
                curveTo(16.0f, 11.94f, 13.89f, 14.62f, 11.0f, 15.48f)
                verticalLineTo(17.54f)
                curveTo(15.01f, 16.63f, 18.0f, 13.05f, 18.0f, 8.77f)
                curveTo(18.0f, 4.49f, 15.01f, 0.91f, 11.0f, 0.0f)
                close()
            }
        }.build()
        return muteotheraudio!!
    }

private var muteotheraudio: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.Muteotheraudio.IconPreview()
