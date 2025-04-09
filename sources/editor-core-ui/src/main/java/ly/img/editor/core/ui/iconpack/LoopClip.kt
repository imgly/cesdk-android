package ly.img.editor.core.ui.iconpack

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.EvenOdd
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val IconPack.LoopClip: ImageVector
    get() {
        if (`_customLoop-clip` != null) {
            return `_customLoop-clip`!!
        }
        `_customLoop-clip` = Builder(
            name = "CustomLoop-clip",
            defaultWidth = 16.0.dp,
            defaultHeight =
                16.0.dp,
            viewportWidth = 16.0f,
            viewportHeight = 16.0f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFF46464F)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = EvenOdd,
            ) {
                moveTo(7.9999f, 1.3335f)
                curveTo(4.318f, 1.3335f, 1.3333f, 4.3183f, 1.3333f, 8.0002f)
                curveTo(1.3333f, 11.6821f, 4.318f, 14.6668f, 7.9999f, 14.6668f)
                curveTo(11.6818f, 14.6668f, 14.6666f, 11.6821f, 14.6666f, 8.0002f)
                curveTo(14.6666f, 4.3183f, 11.6818f, 1.3335f, 7.9999f, 1.3335f)
                close()
                moveTo(12.0833f, 8.0002f)
                curveTo(12.0833f, 9.2026f, 10.6295f, 9.8048f, 9.7792f, 8.9545f)
                lineTo(9.3333f, 8.5085f)
                lineTo(8.5083f, 9.3335f)
                lineTo(8.9543f, 9.7795f)
                curveTo(10.5395f, 11.3647f, 13.2499f, 10.242f, 13.2499f, 8.0002f)
                curveTo(13.2499f, 5.7583f, 10.5395f, 4.6357f, 8.9543f, 6.2208f)
                lineTo(6.2206f, 8.9545f)
                curveTo(5.3704f, 9.8048f, 3.9166f, 9.2026f, 3.9166f, 8.0002f)
                curveTo(3.9166f, 6.7977f, 5.3704f, 6.1956f, 6.2206f, 7.0458f)
                lineTo(6.6666f, 7.4918f)
                lineTo(7.4915f, 6.6668f)
                lineTo(7.0456f, 6.2208f)
                curveTo(5.4604f, 4.6357f, 2.7499f, 5.7583f, 2.7499f, 8.0002f)
                curveTo(2.7499f, 10.242f, 5.4604f, 11.3647f, 7.0456f, 9.7795f)
                lineTo(9.7792f, 7.0458f)
                curveTo(10.6295f, 6.1956f, 12.0833f, 6.7977f, 12.0833f, 8.0002f)
                close()
            }
        }
            .build()
        return `_customLoop-clip`!!
    }

private var `_customLoop-clip`: ImageVector? = null

@Composable
private fun Preview() = IconPack.LoopClip.IconPreview()
