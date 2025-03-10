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

val IconPack.TimerThreeOutline: ImageVector
    get() {
        if (`timer-three-outline` != null) {
            return `timer-three-outline`!!
        }
        `timer-three-outline` = Builder(
            name = "Timer-three-outline",
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
                moveTo(8.95f, 17.225f)
                horizontalLineTo(13.175f)
                curveTo(13.7583f, 17.225f, 14.2583f, 17.046f, 14.675f, 16.688f)
                curveTo(15.0917f, 16.33f, 15.3f, 15.9007f, 15.3f, 15.4f)
                verticalLineTo(14.1f)
                curveTo(15.3f, 13.7167f, 15.1457f, 13.3957f, 14.837f, 13.137f)
                curveTo(14.5283f, 12.8783f, 14.1577f, 12.7493f, 13.725f, 12.75f)
                curveTo(14.1583f, 12.75f, 14.5293f, 12.6167f, 14.838f, 12.35f)
                curveTo(15.1467f, 12.0833f, 15.3007f, 11.7583f, 15.3f, 11.375f)
                verticalLineTo(10.575f)
                curveTo(15.3f, 10.075f, 15.0917f, 9.65f, 14.675f, 9.3f)
                curveTo(14.2583f, 8.95f, 13.7583f, 8.775f, 13.175f, 8.775f)
                horizontalLineTo(8.95f)
                verticalLineTo(10.575f)
                horizontalLineTo(13.175f)
                verticalLineTo(11.85f)
                horizontalLineTo(11.075f)
                verticalLineTo(13.65f)
                horizontalLineTo(13.175f)
                verticalLineTo(15.4f)
                horizontalLineTo(8.95f)
                verticalLineTo(17.225f)
                close()
                moveTo(9.0f, 3.0f)
                verticalLineTo(1.0f)
                horizontalLineTo(15.0f)
                verticalLineTo(3.0f)
                horizontalLineTo(9.0f)
                close()
                moveTo(12.0f, 22.0f)
                curveTo(10.7667f, 22.0f, 9.6043f, 21.7623f, 8.513f, 21.287f)
                curveTo(7.4217f, 20.8117f, 6.4673f, 20.166f, 5.65f, 19.35f)
                curveTo(4.8333f, 18.5333f, 4.1873f, 17.579f, 3.712f, 16.487f)
                curveTo(3.2367f, 15.395f, 2.9993f, 14.2327f, 3.0f, 13.0f)
                curveTo(3.0f, 11.7667f, 3.2377f, 10.604f, 3.713f, 9.512f)
                curveTo(4.1883f, 8.42f, 4.834f, 7.466f, 5.65f, 6.65f)
                curveTo(6.4667f, 5.8333f, 7.421f, 5.1873f, 8.513f, 4.712f)
                curveTo(9.605f, 4.2367f, 10.7673f, 3.9993f, 12.0f, 4.0f)
                curveTo(13.0333f, 4.0f, 14.025f, 4.1667f, 14.975f, 4.5f)
                curveTo(15.925f, 4.8333f, 16.8167f, 5.3167f, 17.65f, 5.95f)
                lineTo(19.05f, 4.55f)
                lineTo(20.45f, 5.95f)
                lineTo(19.05f, 7.35f)
                curveTo(19.6833f, 8.1833f, 20.1667f, 9.075f, 20.5f, 10.025f)
                curveTo(20.8333f, 10.975f, 21.0f, 11.9667f, 21.0f, 13.0f)
                curveTo(21.0f, 14.2333f, 20.7623f, 15.396f, 20.287f, 16.488f)
                curveTo(19.8117f, 17.58f, 19.166f, 18.534f, 18.35f, 19.35f)
                curveTo(17.5333f, 20.1667f, 16.579f, 20.8127f, 15.487f, 21.288f)
                curveTo(14.395f, 21.7633f, 13.2327f, 22.0007f, 12.0f, 22.0f)
                close()
                moveTo(12.0f, 20.0f)
                curveTo(13.9333f, 20.0f, 15.5833f, 19.3167f, 16.95f, 17.95f)
                curveTo(18.3167f, 16.5833f, 19.0f, 14.9333f, 19.0f, 13.0f)
                curveTo(19.0f, 11.0667f, 18.3167f, 9.4167f, 16.95f, 8.05f)
                curveTo(15.5833f, 6.6833f, 13.9333f, 6.0f, 12.0f, 6.0f)
                curveTo(10.0667f, 6.0f, 8.4167f, 6.6833f, 7.05f, 8.05f)
                curveTo(5.6833f, 9.4167f, 5.0f, 11.0667f, 5.0f, 13.0f)
                curveTo(5.0f, 14.9333f, 5.6833f, 16.5833f, 7.05f, 17.95f)
                curveTo(8.4167f, 19.3167f, 10.0667f, 20.0f, 12.0f, 20.0f)
                close()
            }
        }
            .build()
        return `timer-three-outline`!!
    }

private var `timer-three-outline`: ImageVector? = null
