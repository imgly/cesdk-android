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

val IconPack.Tunevariant: ImageVector
    get() {
        if (tunevariant != null) {
            return tunevariant!!
        }
        tunevariant = Builder(
            name = "Tunevariant",
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
                moveTo(8.0f, 13.0f)
                curveTo(6.14f, 13.0f, 4.59f, 14.28f, 4.14f, 16.0f)
                horizontalLineTo(2.0f)
                verticalLineTo(18.0f)
                horizontalLineTo(4.14f)
                curveTo(4.59f, 19.72f, 6.14f, 21.0f, 8.0f, 21.0f)
                curveTo(9.86f, 21.0f, 11.41f, 19.72f, 11.86f, 18.0f)
                horizontalLineTo(22.0f)
                verticalLineTo(16.0f)
                horizontalLineTo(11.86f)
                curveTo(11.41f, 14.28f, 9.86f, 13.0f, 8.0f, 13.0f)
                close()
                moveTo(8.0f, 19.0f)
                curveTo(6.9f, 19.0f, 6.0f, 18.1f, 6.0f, 17.0f)
                curveTo(6.0f, 15.9f, 6.9f, 15.0f, 8.0f, 15.0f)
                curveTo(9.1f, 15.0f, 10.0f, 15.9f, 10.0f, 17.0f)
                curveTo(10.0f, 18.1f, 9.1f, 19.0f, 8.0f, 19.0f)
                close()
                moveTo(19.86f, 6.0f)
                curveTo(19.41f, 4.28f, 17.86f, 3.0f, 16.0f, 3.0f)
                curveTo(14.14f, 3.0f, 12.59f, 4.28f, 12.14f, 6.0f)
                horizontalLineTo(2.0f)
                verticalLineTo(8.0f)
                horizontalLineTo(12.14f)
                curveTo(12.59f, 9.72f, 14.14f, 11.0f, 16.0f, 11.0f)
                curveTo(17.86f, 11.0f, 19.41f, 9.72f, 19.86f, 8.0f)
                horizontalLineTo(22.0f)
                verticalLineTo(6.0f)
                horizontalLineTo(19.86f)
                close()
                moveTo(16.0f, 9.0f)
                curveTo(14.9f, 9.0f, 14.0f, 8.1f, 14.0f, 7.0f)
                curveTo(14.0f, 5.9f, 14.9f, 5.0f, 16.0f, 5.0f)
                curveTo(17.1f, 5.0f, 18.0f, 5.9f, 18.0f, 7.0f)
                curveTo(18.0f, 8.1f, 17.1f, 9.0f, 16.0f, 9.0f)
                close()
            }
        }.build()
        return tunevariant!!
    }

private var tunevariant: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.Tunevariant.IconPreview()
