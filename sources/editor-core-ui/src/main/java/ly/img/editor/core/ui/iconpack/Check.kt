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

public val IconPack.Check: ImageVector
    get() {
        if (check != null) {
            return check!!
        }
        check = Builder(
            name = "Check",
            defaultWidth = 24.0.dp,
            defaultHeight = 24.0.dp,
            viewportWidth = 24.0f,
            viewportHeight = 24.0f,
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
                moveTo(9.55f, 15.15f)
                lineTo(18.025f, 6.675f)
                curveTo(18.225f, 6.475f, 18.458f, 6.375f, 18.725f, 6.375f)
                curveTo(18.992f, 6.375f, 19.225f, 6.475f, 19.425f, 6.675f)
                curveTo(19.625f, 6.875f, 19.725f, 7.113f, 19.725f, 7.388f)
                curveTo(19.725f, 7.663f, 19.625f, 7.901f, 19.425f, 8.1f)
                lineTo(10.25f, 17.3f)
                curveTo(10.05f, 17.5f, 9.817f, 17.6f, 9.55f, 17.6f)
                curveTo(9.284f, 17.6f, 9.05f, 17.5f, 8.85f, 17.3f)
                lineTo(4.55f, 13.0f)
                curveTo(4.35f, 12.8f, 4.254f, 12.563f, 4.262f, 12.288f)
                curveTo(4.27f, 12.013f, 4.375f, 11.776f, 4.575f, 11.575f)
                curveTo(4.776f, 11.374f, 5.014f, 11.274f, 5.288f, 11.275f)
                curveTo(5.563f, 11.276f, 5.8f, 11.376f, 6.0f, 11.575f)
                lineTo(9.55f, 15.15f)
                close()
            }
        }
            .build()
        return check!!
    }

private var check: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.Check.IconPreview()
