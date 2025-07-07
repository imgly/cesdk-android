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

val IconPack.Resize: ImageVector
    get() {
        if (resize != null) {
            return resize!!
        }
        resize = Builder(
            name = "Resize",
            defaultWidth = 24.0.dp,
            defaultHeight = 25.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 25.0f,
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
                moveTo(14.0f, 11.6197f)
                lineTo(12.6f, 10.2197f)
                lineTo(17.6f, 5.2197f)
                horizontalLineTo(11.0f)
                verticalLineTo(3.2197f)
                horizontalLineTo(21.0f)
                verticalLineTo(13.2197f)
                horizontalLineTo(19.0f)
                verticalLineTo(6.6197f)
                lineTo(14.0f, 11.6197f)
                close()
            }
            path(
                fill = SolidColor(Color(0xFF46464F)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(10.0f, 12.8197f)
                lineTo(11.4f, 14.2197f)
                lineTo(6.4f, 19.2197f)
                horizontalLineTo(13.0f)
                verticalLineTo(21.2197f)
                horizontalLineTo(3.0f)
                verticalLineTo(11.2197f)
                horizontalLineTo(5.0f)
                verticalLineTo(17.8197f)
                lineTo(10.0f, 12.8197f)
                close()
            }
        }
            .build()
        return resize!!
    }

private var resize: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.Resize.IconPreview()
