package ly.img.editor.core.component.data

import androidx.annotation.FloatRange
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A class that describes the height of a component.
 */
@Stable
sealed interface Height {
    /**
     * Height as an exact [size].
     */
    @Stable
    data class Exactly(
        val size: Dp,
    ) : Height

    /**
     * Height as a [fraction] of another height.
     */
    @Stable
    data class Fraction(
        @FloatRange(from = 0.0, to = 1.0) val fraction: Float,
    ) : Height
}

/**
 * Insets of a component.
 *
 * @param left left inset of a component.
 * @param top top inset of a component.
 * @param right right inset of a component.
 * @param bottom bottom inset of a component.
 */
data class Insets(
    val left: Dp,
    val top: Dp,
    val right: Dp,
    val bottom: Dp,
) {
    /**
     * Convenience constructor for equal insets on each direction.
     *
     * @param horizontal horizontal inset of a component.
     * @param vertical vertical inset of a component.
     */
    constructor(
        horizontal: Dp,
        vertical: Dp,
    ) : this(
        left = horizontal,
        top = vertical,
        right = horizontal,
        bottom = vertical,
    )

    /**
     * Convenience constructor for equal insets on all sides.
     *
     * @param value inset of a component.
     */
    constructor(
        value: Dp,
    ) : this(
        horizontal = value,
        vertical = value,
    )

    /**
     * Plus operator implementation that adds [other]'s values on each side.
     *
     * @param other the insets to add.
     */
    operator fun plus(other: Insets) = Insets(
        left = left + other.left,
        top = top + other.top,
        right = right + other.right,
        bottom = bottom + other.bottom,
    )

    /**
     * Minus operator implementation that subtracts [other]'s values on each side.
     *
     * @param other the insets to subtract.
     */
    operator fun minus(other: Insets) = Insets(
        left = left - other.left,
        top = top - other.top,
        right = right - other.right,
        bottom = bottom - other.bottom,
    )

    /**
     * Times operator implementation that multiplies the values of each side by [factor].
     *
     * @param factor the factor to multiply with.
     */
    operator fun times(factor: Float) = Insets(
        left = left * factor,
        top = top * factor,
        right = right * factor,
        bottom = bottom * factor,
    )

    /**
     * Div operator implementation that divides the values of each side by [factor].
     *
     * @param factor the factor to divide by.
     */
    operator fun div(factor: Float) = Insets(
        left = left / factor,
        top = top / factor,
        right = right / factor,
        bottom = bottom / factor,
    )

    companion object {
        /**
         * Zero value for [Insets].
         */
        val Zero = Insets(value = 0.dp)
    }
}

/**
 * Size of a component.
 *
 * @param width width of a component.
 * @param height height of a component.
 */
data class Size(
    val width: Dp,
    val height: Dp,
) {
    /**
     * Convenience constructor for equal width and height.
     *
     * @param value size of a component.
     */
    constructor(
        value: Dp,
    ) : this(
        width = value,
        height = value,
    )

    /**
     * Plus operator implementation that adds [other]'s width and height.
     *
     * @param other the size to add.
     */
    operator fun plus(other: Size) = Size(
        width = width + other.width,
        height = height + other.height,
    )

    /**
     * Minus operator implementation that subtracts [other]'s width and height.
     *
     * @param other the size to subtract.
     */
    operator fun minus(other: Size) = Size(
        width = width - other.width,
        height = height - other.height,
    )

    /**
     * Times operator implementation that multiplies the values of width and height by [factor].
     *
     * @param factor the factor to multiply with.
     */
    operator fun times(factor: Float) = Size(
        width = width * factor,
        height = height * factor,
    )

    /**
     * Div operator implementation that divides the values of width and height by [factor].
     *
     * @param factor the factor to divide by.
     */
    operator fun div(factor: Float) = Size(
        width = width / factor,
        height = height / factor,
    )

    companion object {
        /**
         * Zero value for [Size].
         */
        val Zero = Size(width = 0.dp, height = 0.dp)
    }
}
