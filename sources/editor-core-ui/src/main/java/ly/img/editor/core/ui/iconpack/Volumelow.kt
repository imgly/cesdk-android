package ly.img.editor.core.ui.iconpack

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

val IconPack.Volumelow: ImageVector
    get() {
        if (volumelow != null) {
            return volumelow!!
        }
        volumelow = Builder(
            name = "Volumelow",
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
                moveTo(7.0f, 9.0f)
                verticalLineTo(15.0f)
                horizontalLineTo(11.0f)
                lineTo(16.0f, 20.0f)
                verticalLineTo(4.0f)
                lineTo(11.0f, 9.0f)
                horizontalLineTo(7.0f)
                close()
            }
        }
            .build()
        return volumelow!!
    }

private var volumelow: ImageVector? = null
