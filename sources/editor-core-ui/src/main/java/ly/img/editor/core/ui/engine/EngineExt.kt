package ly.img.editor.core.ui.engine

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.first
import ly.img.editor.core.component.data.ConicalGradientFill
import ly.img.editor.core.component.data.Fill
import ly.img.editor.core.component.data.GradientFill
import ly.img.editor.core.component.data.LinearGradientFill
import ly.img.editor.core.component.data.RadialGradientFill
import ly.img.editor.core.component.data.SolidFill
import ly.img.engine.BlockApi
import ly.img.engine.CMYKColor
import ly.img.engine.ColorSpace
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import ly.img.engine.EngineErrorCode
import ly.img.engine.EngineException
import ly.img.engine.FillType
import ly.img.engine.GradientColorStop
import ly.img.engine.RGBAColor
import ly.img.engine.SpotColor

suspend fun Engine.awaitEngineAndSceneLoad() {
    awaitStart()
    if (scene.get() == null) {
        scene.onActiveChanged().first()
    }
}

fun Engine.deselectAllBlocks() {
    block.findAllSelected().forEach {
        block.setSelected(it, false)
    }
}

/**
 * An extension function for getting the page design block based on the index.
 *
 * @return the page at [index].
 */
fun Engine.getPage(index: Int): DesignBlock = scene.getPages()[index]

/**
 * An extension function for getting the current page of the scene. The definition of current is defined
 * in the documentation of [ly.img.engine.SceneApi.getCurrentPage]. In case [ly.img.engine.SceneApi.getCurrentPage]
 * returns null, the first page is returned.
 *
 * @return the current page in the scene.
 */
fun Engine.getCurrentPage(): DesignBlock = scene.getCurrentPage() ?: getPage(0)

fun Engine.getScene(): DesignBlock = block.findByType(DesignBlockType.Scene).first()

fun Engine.getStackOrNull(): DesignBlock? = block.findByType(DesignBlockType.Stack).firstOrNull()

fun Engine.getCamera(): DesignBlock = block.findByType(DesignBlockType.Camera).first()

fun Engine.dpToCanvasUnit(dp: Float): Float {
    val sceneUnit = block.getEnum(getScene(), "scene/designUnit")
    val sceneDpi = block.getFloat(getScene(), "scene/dpi")
    val densityFactor = when (sceneUnit) {
        "Millimeter" -> sceneDpi / 25.4f
        "Inch" -> sceneDpi
        else -> 1f
    }
    val zoomLevel = scene.getZoomLevel()
    return dp * (block.getFloat(getCamera(), "camera/pixelRatio")) / (densityFactor * zoomLevel)
}

fun Engine.overrideAndRestore(
    designBlock: DesignBlock,
    vararg scopes: String,
    action: (DesignBlock) -> Unit,
) {
    val disabledScopes = getDisabledScopes(designBlock, *scopes)
    action(designBlock)
    restoreScopes(designBlock, disabledScopes)
}

suspend fun Engine.overrideAndRestoreAsync(
    designBlock: DesignBlock,
    vararg scopes: String,
    action: suspend (DesignBlock) -> Unit,
) {
    val disabledScopes = getDisabledScopes(designBlock, *scopes)
    action(designBlock)
    restoreScopes(designBlock, disabledScopes)
}

private fun Engine.getDisabledScopes(
    designBlock: DesignBlock,
    vararg scopes: String,
): Set<String> {
    val disabledScopes = hashSetOf<String>()
    scopes.forEach {
        val wasEnabled = block.isScopeEnabled(designBlock, it)
        if (!wasEnabled) {
            block.setScopeEnabled(designBlock, it, true)
            disabledScopes.add(it)
        }
    }
    return disabledScopes
}

private fun Engine.restoreScopes(
    designBlock: DesignBlock,
    scopes: Set<String>,
) {
    scopes.forEach {
        block.setScopeEnabled(designBlock, it, false)
    }
}

/**
 * An extension function for converting an [Engine] RGBA color into Jetpack Compose Color.
 *
 * @return the converted color.
 */
fun RGBAColor.toComposeColor(): Color = Color(
    red = this.r,
    green = this.g,
    blue = this.b,
    alpha = this.a,
)

/**
 * An extension function for converting any [Engine] color into an Engine RGBA color.
 * While the CMYK profile loads, or when it cannot load, a CMYK color converts by the formula that the canvas draws with.
 * IMPORTANT! When modifying this function also modify similar function in InspectorBarExt.kt.
 *
 * @return the converted color.
 */
fun ly.img.engine.Color.toRGBColor(engine: Engine): RGBAColor = this as? RGBAColor ?: try {
    engine.editor.convertColorToColorSpace(this, ColorSpace.SRGB) as RGBAColor
} catch (exception: EngineException) {
    if (exception.code !in CMYK_PROFILE_ERRORS) throw exception
    toUnmanagedRGBColor(engine)
}

/**
 * Converts a CMYK color, or a spot color with only a CMYK approximation, to sRGB by the formula that the canvas draws
 * with while the CMYK profile loads or after it failed to load.
 * IMPORTANT! When modifying this function also modify similar function in InspectorBarExt.kt.
 */
private fun ly.img.engine.Color.toUnmanagedRGBColor(engine: Engine): RGBAColor {
    val (cmyk, tint) = when (this) {
        is CMYKColor -> this to tint
        is SpotColor -> engine.editor.getSpotColorCMYK(name) to tint
        else -> error("Only a CMYK or a spot color needs the CMYK profile, got $this")
    }
    val ink = { component: Float -> component.coerceIn(0F, 1F) * tint.coerceIn(0F, 1F) }
    val key = 1F - ink(cmyk.k)
    return ly.img.engine.Color.fromRGBA(r = (1F - ink(cmyk.c)) * key, g = (1F - ink(cmyk.m)) * key, b = (1F - ink(cmyk.y)) * key)
}

// The codes of a conversion that fails only because the CMYK profile is loading or failed to load.
private val CMYK_PROFILE_ERRORS = setOf(EngineErrorCode.COLOR_PROFILE_NOT_LOADED, EngineErrorCode.COLOR_PROFILE_MISSING)

/**
 * An extension function for getting the [FillType] of the [designBlock]. Check the documentation of [FillType] for more information.
 * IMPORTANT! When modifying this function also modify similar function in InspectorBarExt.kt.
 *
 * @return the [FillType] of the [designBlock].
 */
fun BlockApi.getFillType(designBlock: DesignBlock): FillType? = if (!this.supportsFill(designBlock)) {
    null
} else {
    FillType.get(this.getType(this.getFill(designBlock)))
}

/**
 * An extension function for getting the isLooping of the [Fill] of the [designBlock].
 * If the block has no fill or supportsPlaybackControl is not given it returns false.
 *
 * @return if the block is looping.
 */
fun BlockApi.isFillLooping(designBlock: DesignBlock): Boolean = if (this.supportsFill(designBlock)) {
    this.getFill(designBlock).let { this.supportsPlaybackControl(it) && this.isLooping(it) }
} else {
    false
}

/**
 * An extension function for getting the [Fill] of the [designBlock]. Check the documentation of [Fill] for more information.
 * Note that [Fill] (defined in the editor) does not support all the types defined in [FillType] (defined in the [Engine]).
 * More types will be added over time.
 * IMPORTANT! When modifying this function also modify similar function in InspectorBarExt.kt.
 *
 * @return the [Fill] of the [designBlock] based on its [FillType] and engine's current state.
 */
fun Engine.getFill(designBlock: DesignBlock): Fill? = if (!block.supportsFill(designBlock)) {
    null
} else {
    when (block.getFillType(designBlock)) {
        FillType.Color -> {
            val type = DesignBlockType.getOrNull(block.getType(designBlock))
            val rgbaColor = if (type == DesignBlockType.Text || type == DesignBlockType.Caption) {
                block.getTextColors(designBlock).first().toRGBColor(this)
            } else {
                block.getColor(designBlock, "fill/solid/color").toRGBColor(this)
            }
            SolidFill(rgbaColor.toComposeColor())
        }

        else -> getGradientFill(block.getFill(designBlock))
    }
}

/**
 * An extension function for getting the gradient that the fill block [fill] paints.
 *
 * @return the [GradientFill] of [fill], or null when [fill] is not a gradient fill.
 */
private fun Engine.getGradientFill(fill: DesignBlock): GradientFill? {
    val type = FillType.getOrNull(block.getType(fill))
    // Image fill and Video fill are not supported yet
    if (type != FillType.LinearGradient && type != FillType.RadialGradient && type != FillType.ConicalGradient) return null
    // The editor shows the stops in sRGB, so a CMYK or spot color stop is converted.
    val colorStops = block.getGradientColorStops(fill, "fill/gradient/colors")
        .map { GradientColorStop(it.stop, it.color.toRGBColor(this)) }
    return when (type) {
        FillType.LinearGradient -> LinearGradientFill(
            startPointX = block.getFloat(fill, "fill/gradient/linear/startPointX"),
            startPointY = block.getFloat(fill, "fill/gradient/linear/startPointY"),
            endPointX = block.getFloat(fill, "fill/gradient/linear/endPointX"),
            endPointY = block.getFloat(fill, "fill/gradient/linear/endPointY"),
            colorStops = colorStops,
        )

        FillType.RadialGradient -> RadialGradientFill(
            centerX = block.getFloat(fill, "fill/gradient/radial/centerPointX"),
            centerY = block.getFloat(fill, "fill/gradient/radial/centerPointY"),
            radius = block.getFloat(fill, "fill/gradient/radial/radius"),
            colorStops = colorStops,
        )

        else -> ConicalGradientFill(
            centerX = block.getFloat(fill, "fill/gradient/conical/centerPointX"),
            centerY = block.getFloat(fill, "fill/gradient/conical/centerPointY"),
            colorStops = colorStops,
        )
    }
}

/**
 * An extension function that tells whether a fill block can paint the stroke of [designBlock], through
 * [BlockApi.setStrokeFill]. Only graphics and pages qualify: the engine refuses a stroke fill on text and captions, and
 * a line paints its own fill on its stroke.
 * IMPORTANT! When modifying this function also modify similar function in InspectorBarExt.kt.
 *
 * @param designBlock the design block that is being queried.
 * @return true if a fill block can paint the stroke of [designBlock].
 */
fun BlockApi.canPaintStrokeWithFill(designBlock: DesignBlock): Boolean {
    if (!supportsStroke(designBlock) || isLineOrigin(designBlock)) return false
    val type = DesignBlockType.getOrNull(getType(designBlock))
    return type == DesignBlockType.Graphic || type == DesignBlockType.Page
}

/**
 * An extension function for getting the gradient that paints the stroke of [designBlock].
 *
 * @param designBlock the design block that is being queried.
 * @return the [GradientFill] that paints the stroke, or null when the stroke color paints it.
 */
fun Engine.getStrokeGradientFill(designBlock: DesignBlock): GradientFill? {
    if (!block.canPaintStrokeWithFill(designBlock)) return null
    return block.getStrokeFill(designBlock)?.let { getGradientFill(it) }
}

/**
 * An extension function that returns the stroke color of the [designBlock] if it is present.
 * IMPORTANT! When modifying this function also modify similar function in InspectorBarExt.kt.
 *
 * @param designBlock the design block that is being queried.
 * @return the stroke color of the [designBlock] if it is present, null otherwise.
 */
fun Engine.getStrokeColor(designBlock: DesignBlock): Color? {
    if (!block.supportsStroke(designBlock)) return null
    return block.getColor(designBlock, "stroke/color")
        .toRGBColor(this)
        .toComposeColor()
}

/**
 * An extension function that tells whether the [designBlock] is a background track.
 * A background track is identified as a [DesignBlockType.Track] type that is also set as the page duration source.
 *
 * @param designBlock the design block that is being queried.
 * @return true if the design block is a background track, false otherwise.
 */
fun BlockApi.isBackgroundTrack(designBlock: DesignBlock): Boolean {
    if (DesignBlockType.get(getType(designBlock)) != DesignBlockType.Track) {
        return false
    }
    return isPageDurationSource(designBlock)
}

/**
 * An extension function that tells whether the [designBlock] is a caption track.
 *
 * @param designBlock the design block that is being queried.
 * @return true if the design block is a caption track, false otherwise.
 */
fun BlockApi.isCaptionTrack(designBlock: DesignBlock): Boolean = runCatching {
    DesignBlockType.get(getType(designBlock)) == DesignBlockType.CaptionTrack
}.getOrDefault(false)

/**
 * An extension function for getting the background track from the scene.
 * A scene should have a maximum of one background track.
 * A background track is identified as a [DesignBlockType.Track] type that is set as the page duration source.
 *
 * @return the design block of the background track.
 */
fun Engine.getBackgroundTrack(): DesignBlock? {
    val page = getCurrentPage()
    val children = block.getChildren(page)
    return children.firstOrNull { child ->
        block.getType(child) == DesignBlockType.Track.key &&
            block.isPageDurationSource(child)
    }
}

fun Engine.getSafeBackgroundTrack(): DesignBlock = getBackgroundTrack() ?: createBackgroundTrack()

private fun Engine.createBackgroundTrack(): DesignBlock {
    val page = getCurrentPage()
    val backgroundTrack = block.create(DesignBlockType.Track)
    block.appendChild(parent = page, child = backgroundTrack)
    block.setAlwaysOnBottom(backgroundTrack, true)
    block.fillParent(backgroundTrack)
    block.setScopeEnabled(backgroundTrack, Scope.EditorSelect, false)
    if (block.supportsPageDurationSource(page, backgroundTrack)) {
        block.setPageDurationSource(page, backgroundTrack)
    }
    return backgroundTrack
}
