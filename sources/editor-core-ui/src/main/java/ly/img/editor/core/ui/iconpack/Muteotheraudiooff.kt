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

public val IconPack.Muteotheraudiooff: ImageVector
    get() {
        if (muteotheraudiooff != null) {
            return muteotheraudiooff!!
        }
        muteotheraudiooff = Builder(
            name = "Muteotheraudiooff",
            defaultWidth = 18.14.dp,
            defaultHeight = 18.13.dp,
            viewportWidth = 18.14f,
            viewportHeight = 18.13f,
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
                moveTo(1.41f, 0.0f)
                lineTo(0.0f, 1.41f)
                lineTo(4.36f, 5.77f)
                lineTo(4.07f, 6.07f)
                horizontalLineTo(0.07f)
                verticalLineTo(12.07f)
                horizontalLineTo(4.07f)
                lineTo(9.07f, 17.07f)
                verticalLineTo(10.48f)
                lineTo(13.25f, 14.66f)
                curveTo(12.6f, 15.15f, 11.87f, 15.54f, 11.07f, 15.77f)
                verticalLineTo(17.83f)
                curveTo(12.41f, 17.53f, 13.64f, 16.91f, 14.68f, 16.08f)
                lineTo(16.73f, 18.13f)
                lineTo(18.14f, 16.72f)
                lineTo(1.41f, 0.0f)
                close()
                moveTo(7.07f, 12.24f)
                lineTo(4.9f, 10.07f)
                horizontalLineTo(2.07f)
                verticalLineTo(8.07f)
                horizontalLineTo(4.9f)
                lineTo(5.78f, 7.19f)
                lineTo(7.07f, 8.48f)
                verticalLineTo(12.24f)
                close()
                moveTo(16.07f, 9.07f)
                curveTo(16.07f, 9.89f, 15.92f, 10.68f, 15.66f, 11.41f)
                lineTo(17.19f, 12.94f)
                curveTo(17.75f, 11.77f, 18.07f, 10.46f, 18.07f, 9.07f)
                curveTo(18.07f, 4.79f, 15.08f, 1.21f, 11.07f, 0.3f)
                verticalLineTo(2.36f)
                curveTo(13.96f, 3.22f, 16.07f, 5.9f, 16.07f, 9.07f)
                close()
                moveTo(9.07f, 1.07f)
                lineTo(7.19f, 2.95f)
                lineTo(9.07f, 4.83f)
                verticalLineTo(1.07f)
                close()
                moveTo(13.57f, 9.07f)
                curveTo(13.57f, 7.3f, 12.55f, 5.78f, 11.07f, 5.04f)
                verticalLineTo(6.83f)
                lineTo(13.55f, 9.31f)
                curveTo(13.56f, 9.23f, 13.57f, 9.15f, 13.57f, 9.07f)
                close()
            }
        }.build()
        return muteotheraudiooff!!
    }

private var muteotheraudiooff: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.Muteotheraudiooff.IconPreview()
