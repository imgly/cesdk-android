package ly.img.editor.plugin.voiceIsolation.iconpack

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathFillType.Companion.NonZero
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap.Companion.Butt
import androidx.compose.ui.graphics.StrokeJoin.Companion.Miter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.ImageVector.Builder
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp

public val IconPack.VoiceIsolation: ImageVector
    get() {
        if (voiceIsolation != null) {
            return voiceIsolation!!
        }
        voiceIsolation = Builder(
            name = "VoiceIsolation",
            defaultWidth = 24.0.dp,
            defaultHeight =
                24.0.dp,
            viewportWidth = 960.0f,
            viewportHeight = 960.0f,
        ).apply {
            path(
                fill = SolidColor(Color(0xFF000000)),
                stroke = null,
                strokeLineWidth = 0.0f,
                strokeLineCap = Butt,
                strokeLineJoin = Miter,
                strokeLineMiter = 4.0f,
                pathFillType = NonZero,
            ) {
                moveToRelative(798.0f, 638.0f)
                lineToRelative(-62.0f, -62.0f)
                quadToRelative(44.0f, -41.0f, 69.0f, -97.0f)
                reflectiveQuadToRelative(25.0f, -119.0f)
                quadToRelative(0.0f, -63.0f, -25.0f, -118.0f)
                reflectiveQuadToRelative(-69.0f, -96.0f)
                lineToRelative(62.0f, -64.0f)
                quadToRelative(56.0f, 53.0f, 89.0f, 125.0f)
                reflectiveQuadToRelative(33.0f, 153.0f)
                quadToRelative(0.0f, 81.0f, -33.0f, 153.0f)
                reflectiveQuadToRelative(-89.0f, 125.0f)
                close()
                moveTo(670.0f, 510.0f)
                lineToRelative(-64.0f, -64.0f)
                quadToRelative(18.0f, -17.0f, 29.0f, -38.5f)
                reflectiveQuadToRelative(11.0f, -47.5f)
                quadToRelative(0.0f, -26.0f, -11.0f, -47.5f)
                reflectiveQuadTo(606.0f, 274.0f)
                lineToRelative(64.0f, -64.0f)
                quadToRelative(32.0f, 29.0f, 50.0f, 67.5f)
                reflectiveQuadToRelative(18.0f, 82.5f)
                quadToRelative(0.0f, 44.0f, -18.0f, 82.5f)
                reflectiveQuadTo(670.0f, 510.0f)
                close()
                moveTo(360.0f, 520.0f)
                quadToRelative(-66.0f, 0.0f, -113.0f, -47.0f)
                reflectiveQuadToRelative(-47.0f, -113.0f)
                quadToRelative(0.0f, -66.0f, 47.0f, -113.0f)
                reflectiveQuadToRelative(113.0f, -47.0f)
                quadToRelative(66.0f, 0.0f, 113.0f, 47.0f)
                reflectiveQuadToRelative(47.0f, 113.0f)
                quadToRelative(0.0f, 66.0f, -47.0f, 113.0f)
                reflectiveQuadToRelative(-113.0f, 47.0f)
                close()
                moveTo(40.0f, 840.0f)
                verticalLineToRelative(-112.0f)
                quadToRelative(0.0f, -33.0f, 17.0f, -62.0f)
                reflectiveQuadToRelative(47.0f, -44.0f)
                quadToRelative(51.0f, -26.0f, 115.0f, -44.0f)
                reflectiveQuadToRelative(141.0f, -18.0f)
                quadToRelative(77.0f, 0.0f, 141.0f, 18.0f)
                reflectiveQuadToRelative(115.0f, 44.0f)
                quadToRelative(30.0f, 15.0f, 47.0f, 44.0f)
                reflectiveQuadToRelative(17.0f, 62.0f)
                verticalLineToRelative(112.0f)
                lineTo(40.0f, 840.0f)
                close()
            }
        }
            .build()
        return voiceIsolation!!
    }

private var voiceIsolation: ImageVector? = null
