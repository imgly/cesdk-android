package ly.img.editor.base.engine

import ly.img.editor.core.ui.engine.getFillType
import ly.img.editor.core.ui.engine.isBackgroundTrack
import ly.img.engine.BlockApi
import ly.img.engine.BlurType
import ly.img.engine.Color
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.EffectType
import ly.img.engine.FillType
import ly.img.engine.GradientColorStop
import ly.img.engine.RGBAColor
import kotlin.math.min
import kotlin.math.tan

fun BlockApi.setFillType(
    designBlock: DesignBlock,
    fillType: FillType,
): DesignBlock {
    val oldFill = if (this.supportsFill(designBlock)) {
        this.getType(block = designBlock)
        this.getFill(designBlock).takeIf { isValid(it) }
    } else {
        null
    }

    return if (oldFill != null && this.getFillType(designBlock) == fillType) {
        oldFill
    } else {
        val newFill = this.createFill(fillType)
        if (oldFill != null) {
            this.destroy(oldFill)
        }
        this.setFill(designBlock, newFill)
        newFill
    }
}

fun BlockApi.setBlurType(
    designBlock: DesignBlock,
    type: BlurType?,
): DesignBlock? {
    val oldBlur = if (this.supportsBlur(designBlock)) {
        this.getBlur(designBlock).takeIf { isValid(it) }
    } else {
        null
    }

    if (type == null) {
        this.setBlurEnabled(designBlock, false)
        return null
    } else {
        return if (oldBlur != null && this.getType(oldBlur).endsWith(type.key)) {
            oldBlur
        } else {
            val newBlur = this.createBlur(type)
            this.setBlur(designBlock, newBlur)
            if (oldBlur != null) {
                this.destroy(oldBlur)
            }
            newBlur
        }.also {
            this.setBlurEnabled(designBlock, true)
        }
    }
}

fun BlockApi.setLinearGradientFill(
    designBlock: DesignBlock,
    startPointX: Float,
    startPointY: Float,
    endPointX: Float,
    endPointY: Float,
    colorStops: List<GradientColorStop>? = null,
) {
    setLinearGradient(setFillType(designBlock, FillType.LinearGradient), startPointX, startPointY, endPointX, endPointY, colorStops)
}

fun BlockApi.setRadialGradientFill(
    designBlock: DesignBlock,
    centerPointX: Float,
    centerPointY: Float,
    radius: Float,
    colorStops: List<GradientColorStop>? = null,
) {
    setRadialGradient(setFillType(designBlock, FillType.RadialGradient), centerPointX, centerPointY, radius, colorStops)
}

fun BlockApi.setConicalGradientFill(
    designBlock: DesignBlock,
    centerPointX: Float,
    centerPointY: Float,
    colorStops: List<GradientColorStop>? = null,
) {
    setConicalGradient(setFillType(designBlock, FillType.ConicalGradient), centerPointX, centerPointY, colorStops)
}

/**
 * Writes the points and the [colorStops] of the linear gradient fill block [fill].
 */
fun BlockApi.setLinearGradient(
    fill: DesignBlock,
    startPointX: Float,
    startPointY: Float,
    endPointX: Float,
    endPointY: Float,
    colorStops: List<GradientColorStop>? = null,
) {
    this.setFloat(fill, property = "fill/gradient/linear/startPointX", value = startPointX)
    this.setFloat(fill, property = "fill/gradient/linear/startPointY", value = startPointY)
    this.setFloat(fill, property = "fill/gradient/linear/endPointX", value = endPointX)
    this.setFloat(fill, property = "fill/gradient/linear/endPointY", value = endPointY)
    if (colorStops != null) {
        this.setGradientColorStops(fill, property = "fill/gradient/colors", colorStops = colorStops)
    }
}

/**
 * Writes the center, the radius and the [colorStops] of the radial gradient fill block [fill].
 */
fun BlockApi.setRadialGradient(
    fill: DesignBlock,
    centerPointX: Float,
    centerPointY: Float,
    radius: Float,
    colorStops: List<GradientColorStop>? = null,
) {
    this.setFloat(fill, "fill/gradient/radial/centerPointX", value = centerPointX)
    this.setFloat(fill, "fill/gradient/radial/centerPointY", value = centerPointY)
    this.setFloat(fill, "fill/gradient/radial/radius", value = radius)
    if (colorStops != null) {
        this.setGradientColorStops(fill, property = "fill/gradient/colors", colorStops = colorStops)
    }
}

/**
 * Writes the center and the [colorStops] of the conical gradient fill block [fill].
 */
fun BlockApi.setConicalGradient(
    fill: DesignBlock,
    centerPointX: Float,
    centerPointY: Float,
    colorStops: List<GradientColorStop>? = null,
) {
    this.setFloat(fill, "fill/gradient/conical/centerPointX", value = centerPointX)
    this.setFloat(fill, "fill/gradient/conical/centerPointY", value = centerPointY)
    if (colorStops != null) {
        this.setGradientColorStops(fill, property = "fill/gradient/colors", colorStops = colorStops)
    }
}

/**
 * Writes [colorStops] and the default geometry of a gradient of [type] to the gradient fill block [fill]: a linear
 * gradient from top to bottom, or a radial or conical gradient around the center.
 */
fun BlockApi.setDefaultGradient(
    fill: DesignBlock,
    type: FillType,
    colorStops: List<GradientColorStop>,
) {
    when (type) {
        FillType.LinearGradient -> setLinearGradient(fill, 0.5f, 0f, 0.5f, 1.0f, colorStops)
        FillType.RadialGradient -> setRadialGradient(fill, 0.5f, 0.5f, 0.5f, colorStops)
        FillType.ConicalGradient -> setConicalGradient(fill, 0.5f, 0.5f, colorStops)
        else -> throw UnsupportedOperationException("Fill type $type is not a gradient")
    }
}

/**
 * Writes the points of the linear gradient fill block [fill] so that the gradient runs at [rotationInDegrees] through
 * the center of the block.
 */
fun BlockApi.setLinearGradientRotation(
    fill: DesignBlock,
    rotationInDegrees: Float,
) {
    val absRotationInDegrees = ((rotationInDegrees % 360.0) + 360.0) % 360.0

    val slope = tan(Math.toRadians(absRotationInDegrees))

    val startX: Double
    val startY: Double
    val endX: Double
    val endY: Double

    when (absRotationInDegrees) {
        in 0f..45f, in 315f..360f -> {
            startX = 0.0
            startY = 0.5 - 0.5 * slope
            endX = 1.0
            endY = 0.5 + 0.5 * slope
        }
        in 135.0f..225.0f -> {
            startX = 1.0
            startY = 0.5 + 0.5 * slope
            endX = 0.0
            endY = 0.5 - 0.5 * slope
        }
        in 45f..135.0f -> {
            startX = 0.5 - 0.5 / slope
            startY = 0.0
            endX = 0.5 + 0.5 / slope
            endY = 1.0
        }
        else -> { // 225.0f..315.0f
            startX = 0.5 + 0.5 / slope
            startY = 1.0
            endX = 0.5 - 0.5 / slope
            endY = 0.0
        }
    }
    setLinearGradient(fill, startX.toFloat(), startY.toFloat(), endX.toFloat(), endY.toFloat())
}

/**
 * Replaces the color of the stop at [index] of the gradient fill block [fill] with [color].
 */
fun BlockApi.setGradientColorAt(
    fill: DesignBlock,
    index: Int,
    color: Color,
) {
    val colors = getGradientColorStops(fill, "fill/gradient/colors")
    setGradientColorStops(
        fill,
        "fill/gradient/colors",
        colors.mapIndexed { colorIndex, gradientColorStop ->
            if (colorIndex == index) GradientColorStop(gradientColorStop.stop, color) else gradientColorStop
        },
    )
}

/**
 * The color stops of a gradient that replaces the solid [color]: [color], then a color of another lightness.
 */
fun gradientColorStopsFrom(color: RGBAColor): List<GradientColorStop> = listOf(
    GradientColorStop(0f, color),
    GradientColorStop(1f, color.changeLightnessBy(0.4f)),
)

fun BlockApi.findEffect(
    block: DesignBlock,
    type: EffectType,
) = this.getEffects(block).find { effect ->
    this.getType(effect).endsWith(type.key, ignoreCase = true)
}

fun BlockApi.getEffectOrCreateAndAppend(
    block: DesignBlock,
    type: EffectType,
) = this.findEffect(block, type)
    ?: this.createEffect(type).also { effect ->
        this.appendEffect(block, effect)
    }

fun BlockApi.removeEffectByType(
    block: DesignBlock,
    type: EffectType,
) {
    val filterId = this.getEffects(block).indexOfFirst { effect ->
        this.getType(effect).endsWith(type.key, ignoreCase = true)
    }
    if (filterId != -1) {
        this.removeEffect(block, filterId)
    }
}

/**
 * An extension function for checking whether the parent of the [designBlock] is a background track.
 * IMPORTANT! When modifying this function also modify similar function in InspectorBarExt.kt.
 *
 * @param designBlock the design block that is being queried.
 * @return true if the parent is a background track, false otherwise.
 */
fun BlockApi.isParentBackgroundTrack(designBlock: DesignBlock): Boolean {
    val parent = getParent(designBlock)
    return parent != null && isBackgroundTrack(parent)
}

fun BlockApi.getAspectRatio(designBlock: DesignBlock): Float {
    val width = getFrameWidth(designBlock)
    val height = getFrameHeight(designBlock)
    return width / height
}

fun BlockApi.getSmallerSide(block: DesignBlock): Float {
    val height = getHeight(block)
    val width = getWidth(block)
    return min(width, height)
}

/**
 * Returns true if [designBlock] is an audio block, or a Track containing at least one audio child.
 */
fun BlockApi.containsAudio(designBlock: DesignBlock): Boolean {
    val type = getType(designBlock)
    if (type == DesignBlockType.Audio.key) return true
    if (type != DesignBlockType.Track.key) return false
    return getChildren(designBlock).any { getType(it) == DesignBlockType.Audio.key }
}
