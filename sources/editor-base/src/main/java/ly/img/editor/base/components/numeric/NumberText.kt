package ly.img.editor.base.components.numeric

import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs
import kotlin.math.pow

/**
 * Locale-aware, without grouping, and drops trailing zeros: `4`, `4.2`, `24,5` in German. A value that rounds to
 * zero reads `0`, never `-0`.
 */
internal fun formatNumber(
    value: Double,
    decimals: Int,
    locale: Locale = Locale.getDefault(),
): String {
    val shown = if (abs(value) < 0.5 / 10.0.pow(decimals)) 0.0 else value
    return NumberFormat.getInstance(locale).apply {
        minimumFractionDigits = 0
        maximumFractionDigits = decimals
        isGroupingUsed = false
    }.format(shown)
}

/** `50%` with no space before `%`, `24 pt` with a space before any other unit. */
internal fun withUnit(
    text: String,
    unit: String?,
): String = when (unit) {
    null -> text
    "%" -> "$text%"
    else -> "$text $unit"
}
