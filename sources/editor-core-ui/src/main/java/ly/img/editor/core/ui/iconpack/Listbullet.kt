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

val IconPack.Listbullet: ImageVector
    get() {
        if (listbullet != null) {
            return listbullet!!
        }
        listbullet = Builder(
            name = "Listbullet",
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
                // Bullet at center (4, 12)
                moveTo(4.0f, 10.5f)
                curveToRelative(-0.83f, 0.0f, -1.5f, 0.67f, -1.5f, 1.5f)
                reflectiveCurveToRelative(0.67f, 1.5f, 1.5f, 1.5f)
                reflectiveCurveToRelative(1.5f, -0.67f, 1.5f, -1.5f)
                reflectiveCurveToRelative(-0.67f, -1.5f, -1.5f, -1.5f)
                close()
                // Bullet at center (4, 6)
                moveTo(4.0f, 4.5f)
                curveToRelative(-0.83f, 0.0f, -1.5f, 0.67f, -1.5f, 1.5f)
                reflectiveCurveTo(3.17f, 7.5f, 4.0f, 7.5f)
                reflectiveCurveTo(5.5f, 6.83f, 5.5f, 6.0f)
                reflectiveCurveTo(4.83f, 4.5f, 4.0f, 4.5f)
                close()
                // Bullet at center (4, 18)
                moveTo(4.0f, 16.5f)
                curveToRelative(-0.83f, 0.0f, -1.5f, 0.68f, -1.5f, 1.5f)
                reflectiveCurveToRelative(0.68f, 1.5f, 1.5f, 1.5f)
                reflectiveCurveToRelative(1.5f, -0.68f, 1.5f, -1.5f)
                reflectiveCurveToRelative(-0.67f, -1.5f, -1.5f, -1.5f)
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
                // Bar at y=5–7
                moveTo(7.0f, 5.0f)
                verticalLineToRelative(2.0f)
                horizontalLineToRelative(14.0f)
                verticalLineTo(5.0f)
                horizontalLineTo(7.0f)
                close()
            }
        }.build()
        return listbullet!!
    }

private var listbullet: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.Listbullet.IconPreview()
