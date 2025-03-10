package ly.img.editor.base.components.colorpicker

import androidx.annotation.FloatRange
import androidx.compose.ui.graphics.Color
import android.graphics.Color as AndroidColor

internal data class HsvColor(
    @FloatRange(from = 0.0, to = 360.0) val hue: Float,
    @FloatRange(from = 0.0, to = 1.0) val saturation: Float,
    @FloatRange(from = 0.0, to = 1.0) val value: Float,
    @FloatRange(from = 0.0, to = 1.0) val alpha: Float,
) {
    companion object {
        fun from(color: Color): HsvColor {
            val hsv = FloatArray(3)
            AndroidColor.RGBToHSV(
                (color.red * 255).toInt(),
                (color.green * 255).toInt(),
                (color.blue * 255).toInt(),
                hsv,
            )

            return HsvColor(
                hue = hsv[0],
                saturation = hsv[1],
                value = hsv[2],
                alpha = color.alpha,
            )
        }
    }
}

internal fun HsvColor.toComposeColor(): Color {
    val hsv = floatArrayOf(hue, saturation, value)
    val argb = AndroidColor.HSVToColor((alpha * 255).toInt(), hsv)
    return Color(argb)
}
