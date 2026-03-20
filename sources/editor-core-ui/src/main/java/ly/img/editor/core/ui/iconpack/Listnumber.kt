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

val IconPack.Listnumber: ImageVector
    get() {
        if (listnumber != null) {
            return listnumber!!
        }
        listnumber = Builder(
            name = "Listnumber",
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
                // "3" digit at bottom-left
                moveTo(2.0f, 17.0f)
                horizontalLineToRelative(2.0f)
                verticalLineToRelative(0.5f)
                horizontalLineTo(3.0f)
                verticalLineToRelative(1.0f)
                horizontalLineToRelative(1.0f)
                verticalLineToRelative(0.5f)
                horizontalLineTo(2.0f)
                verticalLineToRelative(1.0f)
                horizontalLineToRelative(3.0f)
                verticalLineToRelative(-4.0f)
                horizontalLineTo(2.0f)
                verticalLineToRelative(1.0f)
                close()
                // "1" digit at top-left
                moveTo(3.0f, 8.0f)
                horizontalLineToRelative(1.0f)
                verticalLineTo(4.0f)
                horizontalLineTo(2.0f)
                verticalLineToRelative(1.0f)
                horizontalLineToRelative(1.0f)
                verticalLineToRelative(3.0f)
                close()
                // "2" digit at middle-left
                moveTo(2.0f, 11.0f)
                horizontalLineToRelative(1.8f)
                lineTo(2.0f, 13.1f)
                verticalLineToRelative(0.9f)
                horizontalLineToRelative(3.0f)
                verticalLineToRelative(-1.0f)
                horizontalLineTo(3.2f)
                lineTo(5.0f, 10.9f)
                verticalLineTo(10.0f)
                horizontalLineTo(2.0f)
                verticalLineToRelative(1.0f)
                close()
                // Bar at y=5–7
                moveTo(7.0f, 5.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(14.0f)
                verticalLineTo(5.0f)
                horizontalLineTo(7.0f)
                close()
                // Bar at y=17–19
                moveTo(7.0f, 19.0f)
                horizontalLineToRelative(14.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineTo(7.0f)
                verticalLineToRelative(2.0f)
                close()
                // Bar at y=11–13
                moveTo(7.0f, 13.0f)
                horizontalLineToRelative(14.0f)
                verticalLineToRelative(-2.0f)
                horizontalLineTo(7.0f)
                verticalLineToRelative(2.0f)
                close()
            }
        }.build()
        return listnumber!!
    }

private var listnumber: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.Listnumber.IconPreview()
