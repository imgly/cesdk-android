package ly.img.editor.core.ui.engine

import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.first
import ly.img.editor.core.component.data.ConicalGradientFill
import ly.img.editor.core.component.data.Fill
import ly.img.editor.core.component.data.LinearGradientFill
import ly.img.editor.core.component.data.RadialGradientFill
import ly.img.editor.core.component.data.SolidFill
import ly.img.engine.BlockApi
import ly.img.engine.ColorSpace
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import ly.img.engine.FillType
import ly.img.engine.RGBAColor
import ly.img.engine.SceneMode

suspend fun Engine.awaitEngineAndSceneLoad() {
    awaitStart()
    if (scene.get() == null) {
        scene.onActiveChanged().first()
    }
}

/**
 * An extension function that tells whether the engine scene mode is a [ly.img.engine.SceneMode.VIDEO].
 * Note that the function returns false if there is no active scene.
 *
 * @return true if there is an active scene and it has scene mode [ly.img.engine.SceneMode.VIDEO], false otherwise.
 */
val Engine.isSceneModeVideo: Boolean
    get() = try {
        scene.getMode() == SceneMode.VIDEO
    } catch (ex: IllegalArgumentException) {
        // In case we don't have any active scene
        false
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
 * IMPORTANT! When modifying this function also modify similar function in InspectorBarExt.kt.
 *
 * @return the converted color.
 */
fun ly.img.engine.Color.toRGBColor(engine: Engine): RGBAColor =
    this as? RGBAColor ?: engine.editor.convertColorToColorSpace(this, ColorSpace.SRGB) as RGBAColor

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
            val rgbaColor = if (DesignBlockType.getOrNull(block.getType(designBlock)) == DesignBlockType.Text) {
                block.getTextColors(designBlock).first().toRGBColor(this)
            } else {
                block.getColor(designBlock, "fill/solid/color").toRGBColor(this)
            }
            SolidFill(rgbaColor.toComposeColor())
        }

        FillType.LinearGradient -> {
            val fill = block.getFill(designBlock)
            LinearGradientFill(
                startPointX = block.getFloat(fill, "fill/gradient/linear/startPointX"),
                startPointY = block.getFloat(fill, "fill/gradient/linear/startPointY"),
                endPointX = block.getFloat(fill, "fill/gradient/linear/endPointX"),
                endPointY = block.getFloat(fill, "fill/gradient/linear/endPointY"),
                colorStops = block.getGradientColorStops(fill, "fill/gradient/colors"),
            )
        }

        FillType.RadialGradient -> {
            val fill = block.getFill(designBlock)
            RadialGradientFill(
                centerX = block.getFloat(fill, "fill/gradient/radial/centerPointX"),
                centerY = block.getFloat(fill, "fill/gradient/radial/centerPointY"),
                radius = block.getFloat(fill, "fill/gradient/radial/radius"),
                colorStops = block.getGradientColorStops(fill, "fill/gradient/colors"),
            )
        }

        FillType.ConicalGradient -> {
            val fill = block.getFill(designBlock)
            ConicalGradientFill(
                centerX = block.getFloat(fill, "fill/gradient/conical/centerPointX"),
                centerY = block.getFloat(fill, "fill/gradient/conical/centerPointY"),
                colorStops = block.getGradientColorStops(fill, "fill/gradient/colors"),
            )
        }

        // Image fill and Video fill are not supported yet
        else -> null
    }
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
