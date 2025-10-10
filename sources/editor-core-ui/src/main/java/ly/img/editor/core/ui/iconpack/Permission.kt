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

public val IconPack.Permission: ImageVector
    get() {
        if (permisson != null) {
            return permisson!!
        }
        permisson = Builder(
            name =
                "Material-symbolsShield-toggle-outline",
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
                moveTo(10.0f, 11.0f)
                horizontalLineTo(14.0f)
                curveTo(14.55f, 11.0f, 15.021f, 10.804f, 15.413f, 10.413f)
                curveTo(15.805f, 10.022f, 16.001f, 9.551f, 16.0f, 9.0f)
                curveTo(15.999f, 8.449f, 15.804f, 7.979f, 15.413f, 7.588f)
                curveTo(15.022f, 7.197f, 14.551f, 7.001f, 14.0f, 7.0f)
                horizontalLineTo(10.0f)
                curveTo(9.45f, 7.0f, 8.979f, 7.196f, 8.588f, 7.588f)
                curveTo(8.197f, 7.98f, 8.001f, 8.451f, 8.0f, 9.0f)
                curveTo(7.999f, 9.549f, 8.195f, 10.02f, 8.588f, 10.413f)
                curveTo(8.981f, 10.806f, 9.451f, 11.001f, 10.0f, 11.0f)
                close()
                moveTo(14.0f, 10.0f)
                curveTo(13.717f, 10.0f, 13.479f, 9.904f, 13.288f, 9.712f)
                curveTo(13.097f, 9.52f, 13.001f, 9.283f, 13.0f, 9.0f)
                curveTo(12.999f, 8.717f, 13.095f, 8.48f, 13.288f, 8.288f)
                curveTo(13.481f, 8.096f, 13.718f, 8.0f, 14.0f, 8.0f)
                curveTo(14.282f, 8.0f, 14.52f, 8.096f, 14.713f, 8.288f)
                curveTo(14.906f, 8.48f, 15.002f, 8.717f, 15.0f, 9.0f)
                curveTo(14.998f, 9.283f, 14.902f, 9.52f, 14.712f, 9.713f)
                curveTo(14.522f, 9.906f, 14.285f, 10.001f, 14.0f, 10.0f)
                close()
                moveTo(10.0f, 16.0f)
                horizontalLineTo(14.0f)
                curveTo(14.55f, 16.0f, 15.021f, 15.804f, 15.413f, 15.413f)
                curveTo(15.805f, 15.022f, 16.001f, 14.551f, 16.0f, 14.0f)
                curveTo(15.999f, 13.449f, 15.804f, 12.979f, 15.413f, 12.588f)
                curveTo(15.022f, 12.197f, 14.551f, 12.001f, 14.0f, 12.0f)
                horizontalLineTo(10.0f)
                curveTo(9.45f, 12.0f, 8.979f, 12.196f, 8.588f, 12.588f)
                curveTo(8.197f, 12.98f, 8.001f, 13.451f, 8.0f, 14.0f)
                curveTo(7.999f, 14.549f, 8.195f, 15.02f, 8.588f, 15.413f)
                curveTo(8.981f, 15.806f, 9.451f, 16.001f, 10.0f, 16.0f)
                close()
                moveTo(10.0f, 15.0f)
                curveTo(9.717f, 15.0f, 9.479f, 14.904f, 9.288f, 14.712f)
                curveTo(9.097f, 14.52f, 9.001f, 14.283f, 9.0f, 14.0f)
                curveTo(8.999f, 13.717f, 9.095f, 13.48f, 9.288f, 13.288f)
                curveTo(9.481f, 13.096f, 9.718f, 13.0f, 10.0f, 13.0f)
                curveTo(10.282f, 13.0f, 10.52f, 13.096f, 10.713f, 13.288f)
                curveTo(10.906f, 13.48f, 11.002f, 13.717f, 11.0f, 14.0f)
                curveTo(10.998f, 14.283f, 10.902f, 14.52f, 10.712f, 14.713f)
                curveTo(10.522f, 14.906f, 10.285f, 15.001f, 10.0f, 15.0f)
                close()
                moveTo(12.0f, 22.0f)
                curveTo(9.683f, 21.417f, 7.771f, 20.087f, 6.262f, 18.012f)
                curveTo(4.753f, 15.937f, 3.999f, 13.633f, 4.0f, 11.1f)
                verticalLineTo(5.0f)
                lineTo(12.0f, 2.0f)
                lineTo(20.0f, 5.0f)
                verticalLineTo(11.1f)
                curveTo(20.0f, 13.633f, 19.246f, 15.938f, 17.738f, 18.013f)
                curveTo(16.23f, 20.088f, 14.317f, 21.417f, 12.0f, 22.0f)
                close()
                moveTo(12.0f, 19.9f)
                curveTo(13.733f, 19.35f, 15.167f, 18.25f, 16.3f, 16.6f)
                curveTo(17.433f, 14.95f, 18.0f, 13.117f, 18.0f, 11.1f)
                verticalLineTo(6.375f)
                lineTo(12.0f, 4.125f)
                lineTo(6.0f, 6.375f)
                verticalLineTo(11.1f)
                curveTo(6.0f, 13.117f, 6.567f, 14.95f, 7.7f, 16.6f)
                curveTo(8.833f, 18.25f, 10.267f, 19.35f, 12.0f, 19.9f)
                close()
            }
        }
            .build()
        return permisson!!
    }

private var permisson: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.Permission.IconPreview()
