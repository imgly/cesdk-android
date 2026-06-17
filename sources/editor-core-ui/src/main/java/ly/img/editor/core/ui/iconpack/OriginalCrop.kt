package ly.img.editor.core.ui.iconpack

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeCap.Companion.Round
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

val IconPack.OriginalCrop: ImageVector
    get() {
        if (originalCrop != null) {
            return originalCrop!!
        }
        originalCrop = Builder(
            name = "OriginalCrop",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
        ).apply {
            path(
                fill = SolidColor(Color(0x00000000)),
                stroke = SolidColor(Color(0xFF46464F)),
                strokeLineWidth = 1.8f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(7.0f, 3.0f)
                horizontalLineTo(17.0f)
                arcTo(4.0f, 4.0f, 0.0f, false, true, 21.0f, 7.0f)
                verticalLineTo(17.0f)
                arcTo(4.0f, 4.0f, 0.0f, false, true, 17.0f, 21.0f)
                horizontalLineTo(7.0f)
                arcTo(4.0f, 4.0f, 0.0f, false, true, 3.0f, 17.0f)
                verticalLineTo(7.0f)
                arcTo(4.0f, 4.0f, 0.0f, false, true, 7.0f, 3.0f)
                close()
            }
            path(
                fill = SolidColor(Color(0x00000000)),
                stroke = SolidColor(Color(0xFF46464F)),
                strokeLineWidth = 1.8f,
                strokeLineCap = Round,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(7.5f, 12.0f)
                horizontalLineTo(16.5f)
            }
            path(
                fill = SolidColor(Color(0x00000000)),
                stroke = SolidColor(Color(0xFF46464F)),
                strokeLineWidth = 1.8f,
                strokeLineCap = Round,
                strokeLineJoin = StrokeJoin.Companion.Round,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(10.0f, 9.5f)
                lineTo(7.5f, 12.0f)
                lineTo(10.0f, 14.5f)
            }
            path(
                fill = SolidColor(Color(0x00000000)),
                stroke = SolidColor(Color(0xFF46464F)),
                strokeLineWidth = 1.8f,
                strokeLineCap = Round,
                strokeLineJoin = StrokeJoin.Companion.Round,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveTo(14.0f, 9.5f)
                lineTo(16.5f, 12.0f)
                lineTo(14.0f, 14.5f)
            }
        }
            .build()
        return originalCrop!!
    }

private var originalCrop: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.OriginalCrop.IconPreview()
