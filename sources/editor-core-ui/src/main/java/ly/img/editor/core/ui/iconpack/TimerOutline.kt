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

public val IconPack.TimerOutline: ImageVector
    get() {
        if (timerOutline != null) {
            return timerOutline!!
        }
        timerOutline = Builder(
            name = "TimerOutline",
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
                moveTo(10.0f, 3.0f)
                curveTo(9.717f, 3.0f, 9.479f, 2.904f, 9.288f, 2.712f)
                curveTo(9.097f, 2.52f, 9.001f, 2.283f, 9.0f, 2.0f)
                curveTo(8.999f, 1.717f, 9.095f, 1.48f, 9.288f, 1.288f)
                curveTo(9.481f, 1.096f, 9.718f, 1.0f, 10.0f, 1.0f)
                horizontalLineTo(14.0f)
                curveTo(14.283f, 1.0f, 14.521f, 1.096f, 14.713f, 1.288f)
                curveTo(14.905f, 1.48f, 15.001f, 1.717f, 15.0f, 2.0f)
                curveTo(14.999f, 2.283f, 14.903f, 2.52f, 14.712f, 2.713f)
                curveTo(14.521f, 2.906f, 14.283f, 3.001f, 14.0f, 3.0f)
                horizontalLineTo(10.0f)
                close()
                moveTo(12.713f, 13.713f)
                curveTo(12.904f, 13.521f, 13.0f, 13.283f, 13.0f, 13.0f)
                verticalLineTo(9.0f)
                curveTo(13.0f, 8.717f, 12.904f, 8.479f, 12.712f, 8.288f)
                curveTo(12.52f, 8.097f, 12.283f, 8.001f, 12.0f, 8.0f)
                curveTo(11.717f, 7.999f, 11.48f, 8.095f, 11.288f, 8.288f)
                curveTo(11.096f, 8.481f, 11.0f, 8.718f, 11.0f, 9.0f)
                verticalLineTo(13.0f)
                curveTo(11.0f, 13.283f, 11.096f, 13.521f, 11.288f, 13.713f)
                curveTo(11.48f, 13.905f, 11.717f, 14.001f, 12.0f, 14.0f)
                curveTo(12.283f, 13.999f, 12.52f, 13.903f, 12.713f, 13.712f)
                moveTo(8.513f, 21.288f)
                curveTo(7.421f, 20.813f, 6.467f, 20.167f, 5.65f, 19.35f)
                curveTo(4.833f, 18.533f, 4.188f, 17.579f, 3.713f, 16.487f)
                curveTo(3.238f, 15.395f, 3.001f, 14.233f, 3.0f, 13.0f)
                curveTo(2.999f, 11.767f, 3.237f, 10.605f, 3.713f, 9.512f)
                curveTo(4.189f, 8.419f, 4.835f, 7.465f, 5.65f, 6.65f)
                curveTo(6.465f, 5.835f, 7.42f, 5.189f, 8.513f, 4.713f)
                curveTo(9.606f, 4.237f, 10.769f, 3.999f, 12.0f, 4.0f)
                curveTo(13.033f, 4.0f, 14.025f, 4.167f, 14.975f, 4.5f)
                curveTo(15.925f, 4.833f, 16.817f, 5.317f, 17.65f, 5.95f)
                lineTo(18.35f, 5.25f)
                curveTo(18.533f, 5.067f, 18.767f, 4.975f, 19.05f, 4.975f)
                curveTo(19.333f, 4.975f, 19.567f, 5.067f, 19.75f, 5.25f)
                curveTo(19.933f, 5.433f, 20.025f, 5.667f, 20.025f, 5.95f)
                curveTo(20.025f, 6.233f, 19.933f, 6.467f, 19.75f, 6.65f)
                lineTo(19.05f, 7.35f)
                curveTo(19.683f, 8.183f, 20.167f, 9.075f, 20.5f, 10.025f)
                curveTo(20.833f, 10.975f, 21.0f, 11.967f, 21.0f, 13.0f)
                curveTo(21.0f, 14.233f, 20.762f, 15.396f, 20.287f, 16.488f)
                curveTo(19.812f, 17.58f, 19.166f, 18.534f, 18.35f, 19.35f)
                curveTo(17.534f, 20.166f, 16.58f, 20.812f, 15.487f, 21.288f)
                curveTo(14.394f, 21.764f, 13.232f, 22.001f, 12.0f, 22.0f)
                curveTo(10.768f, 21.999f, 9.606f, 21.761f, 8.513f, 21.288f)
                close()
                moveTo(16.95f, 17.95f)
                curveTo(18.317f, 16.583f, 19.0f, 14.933f, 19.0f, 13.0f)
                curveTo(19.0f, 11.067f, 18.317f, 9.417f, 16.95f, 8.05f)
                curveTo(15.583f, 6.683f, 13.933f, 6.0f, 12.0f, 6.0f)
                curveTo(10.067f, 6.0f, 8.417f, 6.683f, 7.05f, 8.05f)
                curveTo(5.683f, 9.417f, 5.0f, 11.067f, 5.0f, 13.0f)
                curveTo(5.0f, 14.933f, 5.683f, 16.583f, 7.05f, 17.95f)
                curveTo(8.417f, 19.317f, 10.067f, 20.0f, 12.0f, 20.0f)
                curveTo(13.933f, 20.0f, 15.583f, 19.317f, 16.95f, 17.95f)
                close()
            }
        }
            .build()
        return timerOutline!!
    }

private var timerOutline: ImageVector? = null

@Preview
@Composable
private fun Preview() = IconPack.TimerOutline.IconPreview()
