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

public val IconPack.Voiceoverstop: ImageVector
    get() {
        if (voiceoverstop != null) {
            return voiceoverstop!!
        }
        voiceoverstop = Builder(
            name = "Voiceoverstop",
            defaultWidth = 12.0.dp,
            defaultHeight = 12.0.dp,
            viewportWidth = 12.0f,
            viewportHeight = 12.0f,
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
                moveTo(12.0f, 12.0f)
                horizontalLineTo(0.0f)
                verticalLineTo(0.0f)
                horizontalLineTo(12.0f)
                verticalLineTo(12.0f)
                close()
            }
        }.build()
        return voiceoverstop!!
    }

private var voiceoverstop: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.Voiceoverstop.IconPreview()
