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

public val IconPack.Photocameraoutline: ImageVector
    get() {
        if (photocameraoutline != null) {
            return photocameraoutline!!
        }
        photocameraoutline = Builder(
            name = "Photocameraoutline",
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
                moveTo(12.0f, 17.5f)
                curveTo(13.25f, 17.5f, 14.313f, 17.063f, 15.188f, 16.188f)
                curveTo(16.063f, 15.313f, 16.501f, 14.251f, 16.5f, 13.0f)
                curveTo(16.499f, 11.749f, 16.062f, 10.687f, 15.188f, 9.813f)
                curveTo(14.314f, 8.939f, 13.251f, 8.501f, 12.0f, 8.5f)
                curveTo(10.749f, 8.499f, 9.686f, 8.936f, 8.813f, 9.813f)
                curveTo(7.94f, 10.69f, 7.502f, 11.752f, 7.5f, 13.0f)
                curveTo(7.498f, 14.248f, 7.936f, 15.311f, 8.813f, 16.188f)
                curveTo(9.69f, 17.065f, 10.753f, 17.503f, 12.0f, 17.5f)
                close()
                moveTo(12.0f, 15.5f)
                curveTo(11.3f, 15.5f, 10.708f, 15.258f, 10.225f, 14.775f)
                curveTo(9.742f, 14.292f, 9.5f, 13.7f, 9.5f, 13.0f)
                curveTo(9.5f, 12.3f, 9.742f, 11.708f, 10.225f, 11.225f)
                curveTo(10.708f, 10.742f, 11.3f, 10.5f, 12.0f, 10.5f)
                curveTo(12.7f, 10.5f, 13.292f, 10.742f, 13.775f, 11.225f)
                curveTo(14.258f, 11.708f, 14.5f, 12.3f, 14.5f, 13.0f)
                curveTo(14.5f, 13.7f, 14.258f, 14.292f, 13.775f, 14.775f)
                curveTo(13.292f, 15.258f, 12.7f, 15.5f, 12.0f, 15.5f)
                close()
                moveTo(4.0f, 21.0f)
                curveTo(3.45f, 21.0f, 2.979f, 20.804f, 2.588f, 20.413f)
                curveTo(2.197f, 20.022f, 2.001f, 19.551f, 2.0f, 19.0f)
                verticalLineTo(7.0f)
                curveTo(2.0f, 6.45f, 2.196f, 5.979f, 2.588f, 5.588f)
                curveTo(2.98f, 5.197f, 3.451f, 5.001f, 4.0f, 5.0f)
                horizontalLineTo(7.15f)
                lineTo(8.4f, 3.65f)
                curveTo(8.583f, 3.45f, 8.804f, 3.292f, 9.063f, 3.175f)
                curveTo(9.322f, 3.058f, 9.592f, 3.0f, 9.875f, 3.0f)
                horizontalLineTo(14.125f)
                curveTo(14.408f, 3.0f, 14.679f, 3.058f, 14.938f, 3.175f)
                curveTo(15.197f, 3.292f, 15.417f, 3.45f, 15.6f, 3.65f)
                lineTo(16.85f, 5.0f)
                horizontalLineTo(20.0f)
                curveTo(20.55f, 5.0f, 21.021f, 5.196f, 21.413f, 5.588f)
                curveTo(21.805f, 5.98f, 22.001f, 6.451f, 22.0f, 7.0f)
                verticalLineTo(19.0f)
                curveTo(22.0f, 19.55f, 21.804f, 20.021f, 21.413f, 20.413f)
                curveTo(21.022f, 20.805f, 20.551f, 21.001f, 20.0f, 21.0f)
                horizontalLineTo(4.0f)
                close()
                moveTo(4.0f, 19.0f)
                horizontalLineTo(20.0f)
                verticalLineTo(7.0f)
                horizontalLineTo(15.95f)
                lineTo(14.125f, 5.0f)
                horizontalLineTo(9.875f)
                lineTo(8.05f, 7.0f)
                horizontalLineTo(4.0f)
                verticalLineTo(19.0f)
                close()
            }
        }
            .build()
        return photocameraoutline!!
    }

private var photocameraoutline: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.Photocameraoutline.IconPreview()
