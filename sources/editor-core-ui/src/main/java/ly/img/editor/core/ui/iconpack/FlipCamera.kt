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

public val IconPack.FlipCamera: ImageVector
    get() {
        if (flipCamera != null) {
            return flipCamera!!
        }
        flipCamera = Builder(
            name = "FlipCamera",
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
                moveTo(12.0f, 22.0f)
                curveTo(9.867f, 22.0f, 7.946f, 21.392f, 6.238f, 20.175f)
                curveTo(4.53f, 18.958f, 3.317f, 17.367f, 2.6f, 15.4f)
                curveTo(2.517f, 15.15f, 2.542f, 14.917f, 2.675f, 14.7f)
                curveTo(2.808f, 14.483f, 3.008f, 14.333f, 3.275f, 14.25f)
                curveTo(3.542f, 14.167f, 3.796f, 14.196f, 4.038f, 14.338f)
                curveTo(4.28f, 14.48f, 4.451f, 14.676f, 4.55f, 14.925f)
                curveTo(5.15f, 16.442f, 6.125f, 17.667f, 7.475f, 18.6f)
                curveTo(8.825f, 19.533f, 10.333f, 20.0f, 12.0f, 20.0f)
                curveTo(13.433f, 20.0f, 14.767f, 19.646f, 16.0f, 18.938f)
                curveTo(17.233f, 18.23f, 18.2f, 17.251f, 18.9f, 16.0f)
                horizontalLineTo(17.0f)
                curveTo(16.717f, 16.0f, 16.479f, 15.904f, 16.288f, 15.712f)
                curveTo(16.097f, 15.52f, 16.001f, 15.283f, 16.0f, 15.0f)
                curveTo(15.999f, 14.717f, 16.095f, 14.48f, 16.288f, 14.288f)
                curveTo(16.481f, 14.096f, 16.718f, 14.0f, 17.0f, 14.0f)
                horizontalLineTo(21.0f)
                curveTo(21.283f, 14.0f, 21.521f, 14.096f, 21.713f, 14.288f)
                curveTo(21.905f, 14.48f, 22.001f, 14.717f, 22.0f, 15.0f)
                verticalLineTo(19.0f)
                curveTo(22.0f, 19.283f, 21.904f, 19.521f, 21.712f, 19.713f)
                curveTo(21.52f, 19.905f, 21.283f, 20.001f, 21.0f, 20.0f)
                curveTo(20.717f, 19.999f, 20.48f, 19.903f, 20.288f, 19.712f)
                curveTo(20.096f, 19.521f, 20.0f, 19.283f, 20.0f, 19.0f)
                verticalLineTo(18.0f)
                curveTo(19.05f, 19.267f, 17.875f, 20.25f, 16.475f, 20.95f)
                curveTo(15.075f, 21.65f, 13.583f, 22.0f, 12.0f, 22.0f)
                close()
                moveTo(12.0f, 4.0f)
                curveTo(10.567f, 4.0f, 9.233f, 4.354f, 8.0f, 5.063f)
                curveTo(6.767f, 5.772f, 5.8f, 6.751f, 5.1f, 8.0f)
                horizontalLineTo(7.0f)
                curveTo(7.283f, 8.0f, 7.521f, 8.096f, 7.713f, 8.288f)
                curveTo(7.905f, 8.48f, 8.001f, 8.717f, 8.0f, 9.0f)
                curveTo(7.999f, 9.283f, 7.903f, 9.52f, 7.712f, 9.713f)
                curveTo(7.521f, 9.906f, 7.283f, 10.001f, 7.0f, 10.0f)
                horizontalLineTo(3.0f)
                curveTo(2.717f, 10.0f, 2.479f, 9.904f, 2.288f, 9.712f)
                curveTo(2.097f, 9.52f, 2.001f, 9.283f, 2.0f, 9.0f)
                verticalLineTo(5.0f)
                curveTo(2.0f, 4.717f, 2.096f, 4.479f, 2.288f, 4.288f)
                curveTo(2.48f, 4.097f, 2.717f, 4.001f, 3.0f, 4.0f)
                curveTo(3.283f, 3.999f, 3.52f, 4.095f, 3.713f, 4.288f)
                curveTo(3.906f, 4.481f, 4.001f, 4.718f, 4.0f, 5.0f)
                verticalLineTo(6.0f)
                curveTo(4.95f, 4.733f, 6.125f, 3.75f, 7.525f, 3.05f)
                curveTo(8.925f, 2.35f, 10.417f, 2.0f, 12.0f, 2.0f)
                curveTo(14.133f, 2.0f, 16.054f, 2.608f, 17.763f, 3.825f)
                curveTo(19.472f, 5.042f, 20.684f, 6.633f, 21.4f, 8.6f)
                curveTo(21.483f, 8.85f, 21.458f, 9.083f, 21.325f, 9.3f)
                curveTo(21.192f, 9.517f, 20.992f, 9.667f, 20.725f, 9.75f)
                curveTo(20.458f, 9.833f, 20.204f, 9.804f, 19.962f, 9.662f)
                curveTo(19.72f, 9.52f, 19.549f, 9.324f, 19.45f, 9.075f)
                curveTo(18.85f, 7.558f, 17.875f, 6.333f, 16.525f, 5.4f)
                curveTo(15.175f, 4.467f, 13.667f, 4.0f, 12.0f, 4.0f)
                close()
                moveTo(9.875f, 14.125f)
                curveTo(9.292f, 13.542f, 9.0f, 12.833f, 9.0f, 12.0f)
                curveTo(9.0f, 11.167f, 9.292f, 10.458f, 9.875f, 9.875f)
                curveTo(10.458f, 9.292f, 11.167f, 9.0f, 12.0f, 9.0f)
                curveTo(12.833f, 9.0f, 13.542f, 9.292f, 14.125f, 9.875f)
                curveTo(14.708f, 10.458f, 15.0f, 11.167f, 15.0f, 12.0f)
                curveTo(15.0f, 12.833f, 14.708f, 13.542f, 14.125f, 14.125f)
                curveTo(13.542f, 14.708f, 12.833f, 15.0f, 12.0f, 15.0f)
                curveTo(11.167f, 15.0f, 10.458f, 14.708f, 9.875f, 14.125f)
                close()
            }
        }
            .build()
        return flipCamera!!
    }

private var flipCamera: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.FlipCamera.IconPreview()
