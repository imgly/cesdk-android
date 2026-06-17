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

public val IconPack.FlashlightOn: ImageVector
    get() {
        if (`flashlight-on` != null) {
            return `flashlight-on`!!
        }
        `flashlight-on` = Builder(
            name = "FlashlightOn",
            defaultWidth = 24.0.dp,
            defaultHeight =
                24.0.dp,
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
                moveTo(6.0f, 5.0f)
                verticalLineTo(4.0f)
                curveTo(6.0f, 3.45f, 6.196f, 2.979f, 6.588f, 2.588f)
                curveTo(6.98f, 2.197f, 7.451f, 2.001f, 8.0f, 2.0f)
                horizontalLineTo(16.0f)
                curveTo(16.55f, 2.0f, 17.021f, 2.196f, 17.413f, 2.588f)
                curveTo(17.805f, 2.98f, 18.001f, 3.451f, 18.0f, 4.0f)
                verticalLineTo(5.0f)
                horizontalLineTo(6.0f)
                close()
                moveTo(13.063f, 15.063f)
                curveTo(13.354f, 14.771f, 13.5f, 14.417f, 13.5f, 14.0f)
                curveTo(13.5f, 13.583f, 13.354f, 13.229f, 13.063f, 12.938f)
                curveTo(12.772f, 12.647f, 12.417f, 12.501f, 12.0f, 12.5f)
                curveTo(11.583f, 12.499f, 11.229f, 12.645f, 10.938f, 12.938f)
                curveTo(10.647f, 13.231f, 10.501f, 13.585f, 10.5f, 14.0f)
                curveTo(10.499f, 14.415f, 10.645f, 14.77f, 10.938f, 15.063f)
                curveTo(11.231f, 15.356f, 11.585f, 15.502f, 12.0f, 15.5f)
                curveTo(12.415f, 15.498f, 12.769f, 15.352f, 13.063f, 15.063f)
                close()
                moveTo(8.0f, 20.0f)
                verticalLineTo(11.0f)
                lineTo(6.325f, 8.5f)
                curveTo(6.208f, 8.333f, 6.125f, 8.158f, 6.075f, 7.975f)
                curveTo(6.025f, 7.792f, 6.0f, 7.6f, 6.0f, 7.4f)
                verticalLineTo(7.0f)
                horizontalLineTo(18.0f)
                verticalLineTo(7.4f)
                curveTo(18.0f, 7.6f, 17.975f, 7.792f, 17.925f, 7.975f)
                curveTo(17.875f, 8.158f, 17.792f, 8.333f, 17.675f, 8.5f)
                lineTo(16.0f, 11.0f)
                verticalLineTo(20.0f)
                curveTo(16.0f, 20.55f, 15.804f, 21.021f, 15.413f, 21.413f)
                curveTo(15.022f, 21.805f, 14.551f, 22.001f, 14.0f, 22.0f)
                horizontalLineTo(10.0f)
                curveTo(9.45f, 22.0f, 8.979f, 21.804f, 8.588f, 21.413f)
                curveTo(8.197f, 21.022f, 8.001f, 20.551f, 8.0f, 20.0f)
                close()
            }
        }
            .build()
        return `flashlight-on`!!
    }

private var `flashlight-on`: ImageVector? = null
