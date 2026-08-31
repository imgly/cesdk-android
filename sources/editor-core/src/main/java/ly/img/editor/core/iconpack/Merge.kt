package ly.img.editor.core.iconpack

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

val IconPack.Merge: ImageVector
    get() {
        if (merge != null) {
            return merge!!
        }
        merge = Builder(
            name = "Merge",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(6.41f, 21.0f)
                lineTo(5.0f, 19.59f)
                lineToRelative(4.83f, -4.83f)
                curveToRelative(0.75f, -0.75f, 1.17f, -1.77f, 1.17f, -2.83f)
                verticalLineToRelative(-5.1f)
                lineTo(9.41f, 8.6f)
                lineTo(8.0f, 7.19f)
                lineTo(12.0f, 3.2f)
                lineToRelative(4.0f, 3.99f)
                lineToRelative(-1.41f, 1.42f)
                lineTo(13.0f, 7.02f)
                verticalLineToRelative(5.1f)
                curveToRelative(0.0f, 1.06f, 0.42f, 2.07f, 1.17f, 2.82f)
                lineTo(19.0f, 19.75f)
                lineTo(17.59f, 21.0f)
                lineTo(12.0f, 15.41f)
                lineTo(6.41f, 21.0f)
                close()
            }
        }
            .build()
        return merge!!
    }

private var merge: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.Merge.IconPreview()
