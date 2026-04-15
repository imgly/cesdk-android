@file:Suppress("UnusedReceiverParameter")

package ly.img.editor.core.component

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import ly.img.editor.core.EditorContext
import ly.img.editor.core.EditorScope
import ly.img.editor.core.R
import ly.img.editor.core.ScopedProperty
import ly.img.editor.core.component.data.ConicalGradientFill
import ly.img.editor.core.component.data.EditorIcon
import ly.img.editor.core.component.data.Fill
import ly.img.editor.core.component.data.LinearGradientFill
import ly.img.editor.core.component.data.RadialGradientFill
import ly.img.editor.core.component.data.Selection
import ly.img.editor.core.component.data.SolidFill
import ly.img.editor.core.component.data.unsafeLazy
import ly.img.editor.core.compose.rememberLastValue
import ly.img.editor.core.configuration.remember
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.iconpack.Adjustments
import ly.img.editor.core.iconpack.Animation
import ly.img.editor.core.iconpack.AsClip
import ly.img.editor.core.iconpack.AsOverlay
import ly.img.editor.core.iconpack.Blur
import ly.img.editor.core.iconpack.CropRotate
import ly.img.editor.core.iconpack.Delete
import ly.img.editor.core.iconpack.Duplicate
import ly.img.editor.core.iconpack.Effect
import ly.img.editor.core.iconpack.Filter
import ly.img.editor.core.iconpack.GroupEnter
import ly.img.editor.core.iconpack.IconPack
import ly.img.editor.core.iconpack.Keyboard
import ly.img.editor.core.iconpack.LayersOutline
import ly.img.editor.core.iconpack.Rabbit
import ly.img.editor.core.iconpack.ReorderHorizontally
import ly.img.editor.core.iconpack.Replace
import ly.img.editor.core.iconpack.SelectGroup
import ly.img.editor.core.iconpack.ShapeIcon
import ly.img.editor.core.iconpack.Split
import ly.img.editor.core.iconpack.Typeface
import ly.img.editor.core.iconpack.VoiceoverAdd
import ly.img.editor.core.iconpack.VolumeHigh
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.ui.EditorIcon
import ly.img.engine.BlockApi
import ly.img.engine.ColorSpace
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import ly.img.engine.FillType
import ly.img.engine.RGBAColor
import ly.img.engine.ShapeType

private const val KIND_STICKER = "sticker"
private const val KIND_ANIMATED_STICKER = "animatedSticker"
private const val KIND_VOICEOVER = "voiceover"
private const val VOLUME_SPEED_CUTOFF = 3f

private fun Selection.isAnyKindOfSticker(): Boolean = this.kind == KIND_STICKER || this.kind == KIND_ANIMATED_STICKER

private fun Selection.isNotAnyKindOfSticker() = !this.isAnyKindOfSticker()

/**
 * An extension function for checking whether the [designBlock] is a background track.
 * A background track is identified as a [DesignBlockType.Track] type that is also set as the page duration source.
 *
 * @return true if the [designBlock] is a background track.
 */
private fun Engine.isBackgroundTrack(designBlock: DesignBlock): Boolean {
    if (DesignBlockType.get(block.getType(designBlock)) != DesignBlockType.Track) {
        return false
    }
    return block.isPageDurationSource(designBlock)
}

/**
 * An extension function for checking whether the [selection] can be moved up/down.
 *
 * @param selection the selection that is queried.
 * @return true if the [selection] can be moved, false otherwise.
 */
private fun Engine.isMoveAllowed(selection: Selection): Boolean = block.isAllowedByScope(selection.designBlock, "layer/move") &&
    selection.parentDesignBlock?.let { !isBackgroundTrack(it) } ?: true

/**
 * Returns the block that exposes playback control for the given [designBlock].
 */
private fun Engine.getPlaybackControlBlock(designBlock: DesignBlock): DesignBlock? = when {
    block.supportsPlaybackControl(designBlock) -> designBlock
    block.supportsFill(designBlock) -> {
        val fill = block.getFill(designBlock)
        if (block.supportsPlaybackControl(fill)) {
            fill
        } else {
            null
        }
    }
    else -> null
}

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.rememberReorder].
 */
val InspectorBar.Button.Id.reorder by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.reorder")
}

/**
 * A composable helper function that creates and remembers an [Button] that
 * opens reorder sheet via [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberReorder(builder: InspectorBar.ButtonBuilder.() -> Unit = {}): Button<InspectorBar.ItemScope> =
    InspectorBar.Button.remember {
        id = { InspectorBar.Button.Id.reorder }
        visible = {
            remember(this) {
                editorContext.selection.parentDesignBlock
                    ?.let { parent ->
                        editorContext.engine.isBackgroundTrack(parent) &&
                            editorContext.engine.block.getChildren(parent).size >= 2
                    } ?: false
            }
        }
        vectorIcon = { IconPack.ReorderHorizontally }
        textString = { stringResource(R.string.ly_img_editor_inspector_bar_button_reorder) }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Reorder()))
        }
        builder()
    }

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.rememberAnimations].
 */
val InspectorBar.Button.Id.animations by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.animations")
}

/**
 * A helper function that returns an [Button] that opens animation sheet via [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberAnimations(builder: InspectorBar.ButtonBuilder.() -> Unit = {}): Button<InspectorBar.ItemScope> =
    InspectorBar.Button.remember {
        id = { InspectorBar.Button.Id.animations }
        visible = {
            remember(this) {
                val selection = editorContext.selection
                selection.type != DesignBlockType.Page && selection.type != DesignBlockType.Audio
            }
        }
        vectorIcon = { IconPack.Animation }
        textString = { stringResource(R.string.ly_img_editor_inspector_bar_button_animations) }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Animation()))
        }
        builder()
    }

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.rememberAdjustments].
 */
val InspectorBar.Button.Id.adjustments by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.adjustments")
}

/**
 * A helper function that returns an [Button] that opens adjustments sheet via [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberAdjustments(builder: InspectorBar.ButtonBuilder.() -> Unit = {}): Button<InspectorBar.ItemScope> =
    InspectorBar.Button.remember {
        id = { InspectorBar.Button.Id.adjustments }
        visible = {
            remember(this) {
                editorContext.selection.isNotAnyKindOfSticker() &&
                    (editorContext.selection.fillType == FillType.Image || editorContext.selection.fillType == FillType.Video) &&
                    editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "appearance/adjustments")
            }
        }
        vectorIcon = { IconPack.Adjustments }
        textString = { stringResource(R.string.ly_img_editor_inspector_bar_button_adjustments) }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Adjustments()))
        }
        builder()
    }

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.rememberFilter].
 */
val InspectorBar.Button.Id.filter by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.filter")
}

/**
 * A composable helper function that creates and remembers an [Button] that opens filter sheet via
 * [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberFilter(builder: InspectorBar.ButtonBuilder.() -> Unit = {}): Button<InspectorBar.ItemScope> =
    InspectorBar.Button.remember {
        id = { InspectorBar.Button.Id.filter }
        visible = {
            remember(this) {
                editorContext.selection.isNotAnyKindOfSticker() &&
                    (editorContext.selection.fillType == FillType.Image || editorContext.selection.fillType == FillType.Video) &&
                    editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "appearance/filter")
            }
        }
        vectorIcon = { IconPack.Filter }
        textString = { stringResource(R.string.ly_img_editor_inspector_bar_button_filter) }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Filter()))
        }
        builder()
    }

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.rememberEffect].
 */
val InspectorBar.Button.Id.effect by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.effect")
}

/**
 * A composable helper function that creates and remembers an [Button] that opens effect sheet via
 * [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberEffect(builder: InspectorBar.ButtonBuilder.() -> Unit = {}): Button<InspectorBar.ItemScope> =
    InspectorBar.Button.remember {
        id = { InspectorBar.Button.Id.effect }
        visible = {
            remember(this) {
                editorContext.selection.isNotAnyKindOfSticker() &&
                    (editorContext.selection.fillType == FillType.Image || editorContext.selection.fillType == FillType.Video) &&
                    editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "appearance/effect")
            }
        }
        vectorIcon = { IconPack.Effect }
        textString = { stringResource(R.string.ly_img_editor_inspector_bar_button_effect) }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Effect()))
        }
        builder()
    }

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.rememberBlur].
 */
val InspectorBar.Button.Id.blur by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.blur")
}

/**
 * A composable helper function that creates and remembers an [Button] that opens blur sheet via
 * [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberBlur(builder: InspectorBar.ButtonBuilder.() -> Unit = {}): Button<InspectorBar.ItemScope> =
    InspectorBar.Button.remember {
        id = { InspectorBar.Button.Id.blur }
        visible = {
            remember(this) {
                editorContext.selection.isNotAnyKindOfSticker() &&
                    (editorContext.selection.fillType == FillType.Image || editorContext.selection.fillType == FillType.Video) &&
                    editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "appearance/blur")
            }
        }
        vectorIcon = { IconPack.Blur }
        textString = { stringResource(R.string.ly_img_editor_inspector_bar_button_blur) }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Blur()))
        }
        builder()
    }

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.rememberVolume].
 */
val InspectorBar.Button.Id.volume by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.volume")
}

/**
 * A composable helper function that creates and remembers an [Button] that opens volume sheet via
 * [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberVolume(builder: InspectorBar.ButtonBuilder.() -> Unit = {}): Button<InspectorBar.ItemScope> =
    InspectorBar.Button.remember {
        id = { InspectorBar.Button.Id.volume }
        visible = {
            remember(this) {
                (editorContext.selection.type == DesignBlockType.Audio || editorContext.selection.fillType == FillType.Video) &&
                    editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "fill/change")
            }
        }
        vectorIcon = { IconPack.VolumeHigh }
        textString = { stringResource(R.string.ly_img_editor_inspector_bar_button_volume) }
        enabled = {
            val selection = editorContext.selection
            if (selection.fillType != FillType.Video) {
                true
            } else {
                val playbackBlock = remember(this) {
                    editorContext.engine.getPlaybackControlBlock(selection.designBlock)
                }
                if (playbackBlock == null) {
                    true
                } else {
                    val initialSpeed = remember(selection.designBlock) {
                        editorContext.engine.block.getPlaybackSpeed(playbackBlock)
                    }
                    val speed by remember(selection.designBlock, playbackBlock) {
                        editorContext.engine.event.subscribe(listOf(playbackBlock))
                            .filter {
                                // Ignore updates if selection changed while flow is still active.
                                selection.designBlock == editorContext.engine.block.findAllSelected().firstOrNull()
                            }
                            .map { editorContext.engine.block.getPlaybackSpeed(playbackBlock) }
                            .onStart { emit(initialSpeed) }
                    }.collectAsState(initial = initialSpeed)
                    speed <= VOLUME_SPEED_CUTOFF
                }
            }
        }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Volume()))
        }
        builder()
    }

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.rememberClipSpeed].
 */
val InspectorBar.Button.Id.clipSpeed by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.clipSpeed")
}

/**
 * A composable helper function that creates and remembers an [Button] that opens clip speed sheet via
 * [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberClipSpeed(builder: InspectorBar.ButtonBuilder.() -> Unit = {}): Button<InspectorBar.ItemScope> =
    InspectorBar.Button.remember {
        id = { InspectorBar.Button.Id.clipSpeed }
        visible = {
            remember(this) {
                val selection = editorContext.selection
                val designBlock = selection.designBlock
                val playbackBlock = editorContext.engine.getPlaybackControlBlock(designBlock)
                if (playbackBlock == null) {
                    false
                } else {
                    val isAudio = selection.type == DesignBlockType.Audio
                    val playbackFillType = editorContext.engine.block.getFillType(playbackBlock)
                    (isAudio || selection.fillType == FillType.Video || playbackFillType == FillType.Video) &&
                        editorContext.engine.block.isAllowedByScope(designBlock, "fill/change")
                }
            }
        }
        vectorIcon = { IconPack.Rabbit }
        textString = { stringResource(R.string.ly_img_editor_inspector_bar_button_clip_speed) }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Speed()))
        }
        builder()
    }

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.rememberCrop].
 */
val InspectorBar.Button.Id.crop by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.crop")
}

/**
 * A composable helper function that creates and remembers an [Button] that opens crop sheet via
 * [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberCrop(builder: InspectorBar.ButtonBuilder.() -> Unit = {}): Button<InspectorBar.ItemScope> =
    InspectorBar.Button.remember {
        id = { InspectorBar.Button.Id.crop }
        visible = {
            remember(this) {
                val designBlock = editorContext.selection.designBlock
                editorContext.selection.isNotAnyKindOfSticker() &&
                    (editorContext.selection.fillType == FillType.Image || editorContext.selection.fillType == FillType.Video) &&
                    editorContext.engine.block.supportsCrop(designBlock) &&
                    editorContext.engine.block.isAllowedByScope(designBlock, "layer/crop")
            }
        }
        vectorIcon = { IconPack.CropRotate }
        textString = { stringResource(R.string.ly_img_editor_inspector_bar_button_crop) }
        onClick = {
            val mode = if (editorContext.selection.type == DesignBlockType.Page) {
                SheetType.Crop.Mode.PageCrop
            } else {
                SheetType.Crop.Mode.Element
            }
            editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Crop(mode = mode)))
        }
        builder()
    }

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.rememberDuplicate].
 */
val InspectorBar.Button.Id.duplicate by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.duplicate")
}

/**
 * A composable helper function that creates and remembers an [Button] that duplicates currently selected
 * design block via [EditorEvent.Selection.Duplicate].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberDuplicate(builder: InspectorBar.ButtonBuilder.() -> Unit = {}): Button<InspectorBar.ItemScope> =
    InspectorBar.Button.remember {
        id = { InspectorBar.Button.Id.duplicate }
        visible = {
            remember(this) {
                editorContext.selection.type != DesignBlockType.Page &&
                    editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "lifecycle/duplicate")
            }
        }
        vectorIcon = { IconPack.Duplicate }
        textString = { stringResource(R.string.ly_img_editor_inspector_bar_button_duplicate) }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Selection.Duplicate())
        }
        builder()
    }

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.rememberLayer].
 */
val InspectorBar.Button.Id.layer by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.layer")
}

/**
 * A composable helper function that creates and remembers an [Button] that opens layer sheet via
 * [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberLayer(builder: InspectorBar.ButtonBuilder.() -> Unit = {}): Button<InspectorBar.ItemScope> =
    InspectorBar.Button.remember {
        id = { InspectorBar.Button.Id.layer }
        visible = {
            remember(this) {
                val selection = editorContext.selection
                selection.type != DesignBlockType.Page &&
                    selection.type != DesignBlockType.Audio &&
                    (
                        editorContext.engine.block.isAllowedByScope(selection.designBlock, "layer/blendMode") ||
                            editorContext.engine.block.isAllowedByScope(selection.designBlock, "layer/opacity") ||
                            editorContext.engine.block.isAllowedByScope(selection.designBlock, "lifecycle/duplicate") ||
                            editorContext.engine.block.isAllowedByScope(selection.designBlock, "lifecycle/destroy") ||
                            editorContext.engine.isMoveAllowed(selection)
                    )
            }
        }
        vectorIcon = { IconPack.LayersOutline }
        textString = { stringResource(R.string.ly_img_editor_inspector_bar_button_layer) }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Layer()))
        }
        builder()
    }

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.rememberSplit].
 */
val InspectorBar.Button.Id.split by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.split")
}

/**
 * A composable helper function that creates and remembers an [Button] that splits currently selected
 * design block via [EditorEvent.Selection.Split] in a video scene.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberSplit(builder: InspectorBar.ButtonBuilder.() -> Unit = {}): Button<InspectorBar.ItemScope> =
    InspectorBar.Button.remember {
        id = { InspectorBar.Button.Id.split }
        visible = {
            remember(this) {
                editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "lifecycle/duplicate")
            }
        }
        vectorIcon = { IconPack.Split }
        textString = { stringResource(R.string.ly_img_editor_inspector_bar_button_split) }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Selection.Split())
        }
        builder()
    }

internal fun ly.img.engine.Color.toComposeColor(engine: Engine): Color {
    val rgbaEngineColor = this as? RGBAColor
        ?: engine.editor.convertColorToColorSpace(this, ColorSpace.SRGB) as RGBAColor
    return Color(
        red = rgbaEngineColor.r,
        green = rgbaEngineColor.g,
        blue = rgbaEngineColor.b,
        alpha = rgbaEngineColor.a,
    )
}

/**
 * An extension function that returns [FillType] of the design block.
 *
 * @param designBlock the design block that is queried.
 */
private fun BlockApi.getFillType(designBlock: DesignBlock): FillType? = if (!this.supportsFill(designBlock)) {
    null
} else {
    FillType.get(this.getType(this.getFill(designBlock)))
}

/**
 * An extension function that returns [Fill] of the design block.
 *
 * @param designBlock the design block that is queried.
 */
internal fun Engine.getFill(designBlock: DesignBlock): Fill? = if (!block.supportsFill(designBlock)) {
    null
} else {
    when (block.getFillType(designBlock)) {
        FillType.Color -> {
            val rgbaColor = if (DesignBlockType.getOrNull(block.getType(designBlock)) == DesignBlockType.Text) {
                block.getTextColors(designBlock).first()
            } else {
                block.getColor(designBlock, "fill/solid/color")
            }
            SolidFill(rgbaColor.toComposeColor(this))
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
 * An extension function that returns stroke color of the design block.
 *
 * @param designBlock the design block that is queried.
 */
internal fun Engine.getStrokeColor(designBlock: DesignBlock): Color? {
    if (!block.supportsStroke(designBlock)) return null
    return block.getColor(designBlock, "stroke/color").toComposeColor(this)
}

/**
 * An extension function that constructs [EditorIcon.FillStroke] icon from [designBlock] based on
 * the engine state.
 *
 * @param designBlock the design block to construct the icon from.
 */
private fun Engine.getFillStrokeButtonIcon(designBlock: DesignBlock): EditorIcon.FillStroke {
    fun BlockApi.hasColorOrGradientFill(designBlock: DesignBlock): Boolean {
        val fillType = getFillType(designBlock)
        return fillType == FillType.Color ||
            fillType == FillType.LinearGradient ||
            fillType == FillType.RadialGradient ||
            fillType == FillType.ConicalGradient
    }

    val showFill = block.supportsFill(designBlock) &&
        block.hasColorOrGradientFill(designBlock) &&
        block.isAllowedByScope(designBlock, "fill/change")
    val showStroke = block.supportsStroke(designBlock) && block.isAllowedByScope(designBlock, "stroke/change")
    return EditorIcon.FillStroke(
        showFill = showFill,
        showStroke = showStroke,
        fill = if (showFill && block.isFillEnabled(designBlock)) {
            getFill(designBlock)
        } else {
            null
        },
        stroke = if (showStroke && block.isStrokeEnabled(designBlock)) {
            getStrokeColor(designBlock)
        } else {
            null
        },
    )
}

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.rememberFillStroke].
 */
val InspectorBar.Button.Id.fillStroke by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.fillStroke")
}

/**
 * Scope of the [InspectorBar.Button.rememberFillStroke] button in the inspector bar.
 *
 * @param parentScope the scope of the parent component.
 * @param fillStrokeIcon the icon state of the fill stroke button.
 */
@Stable
open class FillStrokeItemScope(
    parentScope: EditorScope,
    private val fillStrokeIcon: EditorIcon.FillStroke?,
) : InspectorBar.ItemScope(parentScope) {
    /**
     * The icon state of the fill stroke button. Used in the [InspectorBar.Button.rememberFillStroke]
     * button implementation.
     */
    val EditorContext.fillStrokeIcon: EditorIcon.FillStroke
        get() = requireNotNull(this@FillStrokeItemScope.fillStrokeIcon)
}

/**
 * Builder class for the [rememberFillStroke] button.
 */
@Stable
open class FillStrokeButtonBuilder : AbstractButtonBuilder<FillStrokeItemScope>() {
    /**
     * Scope of this component. Every new value will trigger recomposition of all [ScopedProperty]s
     * such as [visible], [enterTransition], [exitTransition] etc.
     * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for granular
     * recompositions over updating the scope, since scope change triggers full recomposition of the component.
     * Ideally, scope should be updated when the parent scope (scope of the parent component) is updated and when you want to
     * observe changes from the [Engine].
     * By default the value is updated whenever [ly.img.editor.core.component.data.EditorIcon.FillStroke]
     * visually changes for any selected design block.
     */
    override var scope: ScopedProperty<EditorScope, FillStrokeItemScope> = {
        (this as InspectorBar.Scope).run {
            val parentScope = rememberLastValue(this) {
                if (editorContext.safeSelection == null) lastValue else this@run
            }
            val initial = remember(parentScope) {
                editorContext.engine.getFillStrokeButtonIcon(editorContext.selection.designBlock)
            }
            val fillStrokeIcon by remember(parentScope) {
                val selection = parentScope.editorContext.selection
                editorContext.engine.event.subscribe(listOf(selection.designBlock))
                    .filter {
                        // When the design block is unselected/deleted, this lambda is entered before parent scope is updated.
                        // We need to make sure that current component does not update if engine selection has changed.
                        selection.designBlock == editorContext.engine.block.findAllSelected().firstOrNull()
                    }
                    .map { editorContext.engine.getFillStrokeButtonIcon(selection.designBlock) }
                    .onStart { emit(initial) }
            }.collectAsState(initial = initial)
            remember(parentScope, fillStrokeIcon) {
                FillStrokeItemScope(parentScope = parentScope, fillStrokeIcon = fillStrokeIcon)
            }
        }
    }
}

/**
 * A composable helper function that creates and remembers an [Button] that opens fill stroke sheet via
 * [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberFillStroke(
    builder: AbstractButtonBuilder<FillStrokeItemScope>.() -> Unit = {},
): Button<FillStrokeItemScope> = Button.remember(::FillStrokeButtonBuilder) {
    id = { InspectorBar.Button.Id.fillStroke }
    visible = {
        remember(this) {
            val fillStrokeIcon = editorContext.fillStrokeIcon
            editorContext.selection.isNotAnyKindOfSticker() && (fillStrokeIcon.showFill || fillStrokeIcon.showStroke)
        }
    }
    icon = {
        val fillStrokeIcon = editorContext.fillStrokeIcon
        EditorIcon(fillStrokeIcon)
    }
    textString = {
        val fillStrokeIcon = editorContext.fillStrokeIcon
        val textResId = when {
            fillStrokeIcon.showFill && fillStrokeIcon.showStroke -> {
                R.string.ly_img_editor_inspector_bar_button_fill_and_stroke
            }
            fillStrokeIcon.showFill -> R.string.ly_img_editor_inspector_bar_button_fill
            else -> R.string.ly_img_editor_inspector_bar_button_stroke
        }
        stringResource(textResId)
    }
    onClick = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.FillStroke()))
    }
    builder()
}

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.rememberMoveAsClip].
 */
val InspectorBar.Button.Id.moveAsClip by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.moveAsClip")
}

/**
 * A composable helper function that creates and remembers an [Button] that moves currently selected design block into
 * the background track as clip via [EditorEvent.Selection.MoveAsClip].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberMoveAsClip(builder: InspectorBar.ButtonBuilder.() -> Unit = {}): Button<InspectorBar.ItemScope> =
    InspectorBar.Button.remember {
        id = { InspectorBar.Button.Id.moveAsClip }
        visible = {
            remember(this) {
                editorContext.selection.type != DesignBlockType.Audio &&
                    editorContext.selection.parentDesignBlock.let {
                        it != null && editorContext.engine.isBackgroundTrack(it).not()
                    }
            }
        }
        vectorIcon = { IconPack.AsClip }
        textString = { stringResource(R.string.ly_img_editor_inspector_bar_button_move_as_clip) }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Selection.MoveAsClip())
        }
        builder()
    }

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.rememberMoveAsOverlay].
 */
val InspectorBar.Button.Id.moveAsOverlay by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.moveAsOverlay")
}

/**
 * A composable helper function that creates and remembers an [Button] that moves currently selected design block from
 * the background track to an overlay via [EditorEvent.Selection.MoveAsOverlay].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberMoveAsOverlay(builder: InspectorBar.ButtonBuilder.() -> Unit = {}): Button<InspectorBar.ItemScope> =
    InspectorBar.Button.remember {
        id = { InspectorBar.Button.Id.moveAsOverlay }
        visible = {
            remember(this) {
                editorContext.selection.type != DesignBlockType.Audio &&
                    editorContext.selection.parentDesignBlock.let {
                        it != null && editorContext.engine.isBackgroundTrack(it)
                    }
            }
        }
        vectorIcon = { IconPack.AsOverlay }
        textString = { stringResource(R.string.ly_img_editor_inspector_bar_button_move_as_overlay) }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Selection.MoveAsOverlay())
        }
        builder()
    }

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.rememberVoiceover].
 */
val Button.Id.Companion.voiceover by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.voiceover")
}

/**
 * A helper function that returns an [InspectorBar.Button] that opens the voiceover recording sheet via
 * [EditorEvent.Sheet.Open].
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberVoiceover(builder: InspectorBar.ButtonBuilder.() -> Unit = {}): Button<InspectorBar.ItemScope> =
    InspectorBar.Button.remember {
        id = { InspectorBar.Button.Id.moveAsClip }
        visible = {
            val selection = editorContext.selection

            fun Selection.isVoiceoverSelection(): Boolean = type == DesignBlockType.Audio && kind == KIND_VOICEOVER

            fun Selection.isReadyVoiceoverSelection(engine: Engine): Boolean = !isVoiceoverSelection() ||
                runCatching {
                    engine.block.getString(designBlock, "audio/fileURI")
                }.getOrDefault("").isNotBlank()

            val initialReadyState = remember(selection.designBlock) {
                selection.isReadyVoiceoverSelection(editorContext.engine)
            }
            val isReady by remember(selection.designBlock) {
                editorContext.engine.event.subscribe(listOf(selection.designBlock))
                    .filter { selection.designBlock == editorContext.engine.block.findAllSelected().firstOrNull() }
                    .map { selection.isReadyVoiceoverSelection(editorContext.engine) }
                    .onStart { emit(initialReadyState) }
                    .distinctUntilChanged()
            }.collectAsState(initial = initialReadyState)
            remember(selection, isReady) {
                selection.isVoiceoverSelection() && isReady
            }
        }
        vectorIcon = { IconPack.VoiceoverAdd }
        textString = { stringResource(R.string.ly_img_editor_sheet_voiceover_button_add_recording) }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Voiceover()))
        }
        builder()
    }

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.rememberReplace].
 */
val InspectorBar.Button.Id.replace by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.replace")
}

/**
 * A composable helper function that creates and remembers an [Button] that opens a library sheet via
 * [EditorEvent.Sheet.Open]. Selected asset will replace the content of the currently selected design block.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberReplace(builder: InspectorBar.ButtonBuilder.() -> Unit = {}): Button<InspectorBar.ItemScope> =
    InspectorBar.Button.remember {
        id = { InspectorBar.Button.Id.replace }
        visible = {
            remember(this) {
                (
                    (
                        editorContext.selection.type == DesignBlockType.Audio &&
                            editorContext.selection.kind != KIND_VOICEOVER
                    ) ||
                        (
                            editorContext.selection.type == DesignBlockType.Graphic &&
                                (editorContext.selection.fillType == FillType.Image || editorContext.selection.fillType == FillType.Video)
                        ) &&
                        editorContext.selection.isNotAnyKindOfSticker() &&
                        editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "fill/change")
                )
            }
        }
        vectorIcon = { IconPack.Replace }
        textString = { stringResource(R.string.ly_img_editor_inspector_bar_button_replace) }
        onClick = {
            val assetLibrary = requireNotNull(editorContext.configuration.value?.assetLibrary)
            val libraryCategory = when (editorContext.selection.type) {
                DesignBlockType.Audio -> assetLibrary.audios
                DesignBlockType.Graphic -> {
                    when (editorContext.selection.kind) {
                        KIND_STICKER,
                        KIND_ANIMATED_STICKER,
                        -> assetLibrary.stickers

                        else -> when (editorContext.selection.fillType) {
                            FillType.Image -> assetLibrary.images
                            FillType.Video -> assetLibrary.videos
                            else -> {
                                error(
                                    "Unsupported fillType ${editorContext.selection.fillType} for replace inspector bar button.",
                                )
                            }
                        }
                    }
                }
                else -> error("Unsupported type ${editorContext.selection.type} for replace inspector bar button.")
            }()
            editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.LibraryReplace(libraryCategory = libraryCategory)))
        }
        builder()
    }

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.rememberEnterGroup].
 */
val InspectorBar.Button.Id.enterGroup by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.enterGroup")
}

/**
 * A composable helper function that creates and remembers an [Button] that changes selection from the selected group
 * design block to a design block within that group via [EditorEvent.Selection.EnterGroup].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberEnterGroup(builder: InspectorBar.ButtonBuilder.() -> Unit = {}): Button<InspectorBar.ItemScope> =
    InspectorBar.Button.remember {
        id = { InspectorBar.Button.Id.enterGroup }
        visible = {
            remember(this) {
                editorContext.selection.type == DesignBlockType.Group
            }
        }
        vectorIcon = { IconPack.GroupEnter }
        textString = { stringResource(R.string.ly_img_editor_inspector_bar_button_enter_group) }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Selection.EnterGroup())
        }
        builder()
    }

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.rememberSelectGroup].
 */
val InspectorBar.Button.Id.selectGroup by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.selectGroup")
}

/**
 * A composable helper function that creates and remembers an [Button] that selects the group design block that
 * contains the currently selected design block via [EditorEvent.Selection.SelectGroup].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberSelectGroup(builder: InspectorBar.ButtonBuilder.() -> Unit = {}): Button<InspectorBar.ItemScope> =
    InspectorBar.Button.remember {
        id = { InspectorBar.Button.Id.selectGroup }
        visible = {
            remember(this) {
                val parentDesignBlock = editorContext.selection.parentDesignBlock ?: return@remember false
                DesignBlockType.get(editorContext.engine.block.getType(parentDesignBlock)) == DesignBlockType.Group
            }
        }
        vectorIcon = { IconPack.SelectGroup }
        textString = { stringResource(R.string.ly_img_editor_inspector_bar_button_select_group) }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Selection.SelectGroup())
        }
        builder()
    }

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.rememberDelete].
 */
val InspectorBar.Button.Id.delete by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.delete")
}

/**
 * A composable helper function that creates and remembers an [Button] that deletes currently selected
 * design block via [EditorEvent.Selection.Delete].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberDelete(builder: InspectorBar.ButtonBuilder.() -> Unit = {}): Button<InspectorBar.ItemScope> =
    InspectorBar.Button.remember {
        id = { InspectorBar.Button.Id.delete }
        visible = {
            remember(this) {
                editorContext.selection.type != DesignBlockType.Page &&
                    editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "lifecycle/destroy")
            }
        }
        vectorIcon = { IconPack.Delete }
        textString = { stringResource(R.string.ly_img_editor_inspector_bar_button_delete) }
        tint = { MaterialTheme.colorScheme.error }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Selection.Delete())
        }
        builder()
    }

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.rememberEditText].
 */
val InspectorBar.Button.Id.editText by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.editText")
}

/**
 * A composable helper function that creates and remembers an [Button] that enters text editing mode for the
 * selected design block via [EditorEvent.Selection.EnterTextEditMode].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberEditText(builder: InspectorBar.ButtonBuilder.() -> Unit = {}): Button<InspectorBar.ItemScope> =
    InspectorBar.Button.remember {
        id = { InspectorBar.Button.Id.editText }
        visible = {
            remember(this) {
                editorContext.selection.type == DesignBlockType.Text &&
                    editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "text/edit")
            }
        }
        vectorIcon = { IconPack.Keyboard }
        textString = { stringResource(R.string.ly_img_editor_inspector_bar_button_edit_text) }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Selection.EnterTextEditMode())
        }
        builder()
    }

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.rememberFormatText].
 */
val InspectorBar.Button.Id.formatText by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.formatText")
}

/**
 * A composable helper function that creates and remembers an [Button] that opens text formatting sheet via
 * [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberFormatText(builder: InspectorBar.ButtonBuilder.() -> Unit = {}): Button<InspectorBar.ItemScope> =
    InspectorBar.Button.remember {
        id = { InspectorBar.Button.Id.formatText }
        visible = {
            remember(this) {
                editorContext.selection.type == DesignBlockType.Text &&
                    editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "text/character")
            }
        }
        vectorIcon = { IconPack.Typeface }
        textString = { stringResource(R.string.ly_img_editor_inspector_bar_button_format_text) }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.FormatText()))
        }
        builder()
    }

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.rememberShape].
 */
val InspectorBar.Button.Id.shape by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.shape")
}

/**
 * A composable helper function that creates and remembers an [Button] that opens shape options sheet via
 * [EditorEvent.Sheet.Open]. The button is applicable for the following shape types:
 * [ShapeType.Star], [ShapeType.Polygon], [ShapeType.Line], [ShapeType.Rect].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberShape(builder: InspectorBar.ButtonBuilder.() -> Unit = {}): Button<InspectorBar.ItemScope> =
    InspectorBar.Button.remember {
        id = { InspectorBar.Button.Id.shape }
        visible = {
            remember(this) {
                val selection = editorContext.selection
                val designBlock = selection.designBlock
                (selection.fillType != FillType.Image || selection.kind != KIND_STICKER) &&
                    (selection.fillType != FillType.Video || selection.kind != KIND_ANIMATED_STICKER) &&
                    editorContext.engine.block.isAllowedByScope(designBlock, "shape/change") &&
                    run {
                        val shapeType = if (editorContext.engine.block.supportsShape(designBlock)) {
                            ShapeType.get(editorContext.engine.block.getType(editorContext.engine.block.getShape(designBlock)))
                        } else {
                            null
                        }
                        shapeType in arrayOf(ShapeType.Star, ShapeType.Polygon, ShapeType.Line, ShapeType.Rect)
                    }
            }
        }
        vectorIcon = { IconPack.ShapeIcon }
        textString = { stringResource(R.string.ly_img_editor_inspector_bar_button_shape) }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Shape()))
        }
        builder()
    }

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.rememberTextBackground].
 */
val InspectorBar.Button.Id.textBackground by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.textBackground")
}

/**
 * Scope of the [rememberTextBackground] button in the inspector bar.
 *
 * @param parentScope the scope of the parent component.
 * @param icon the icon state of the text background button.
 */
@Stable
open class TextBackgroundItemScope(
    parentScope: EditorScope,
    private val icon: EditorIcon,
) : InspectorBar.ItemScope(parentScope) {
    /**
     * The icon state of the text background button. Used in the [InspectorBar.Button.rememberTextBackground]
     * button implementation.
     */
    val EditorContext.icon: EditorIcon
        get() = this@TextBackgroundItemScope.icon
}

/**
 * Builder class for the [rememberTextBackground] button.
 */
@Stable
open class TextBackgroundButtonBuilder : AbstractButtonBuilder<TextBackgroundItemScope>() {
    /**
     * Scope of this component. Every new value will trigger recomposition of all [ScopedProperty]s
     * such as [visible], [enterTransition], [exitTransition] etc.
     * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for granular
     * recompositions over updating the scope, since scope change triggers full recomposition of the component.
     * Ideally, scope should be updated when the parent scope (scope of the parent component) is updated and when you want to
     * observe changes from the [Engine].
     * By default the value is updated whenever [ly.img.editor.core.component.data.EditorIcon]
     * visually changes for any selected design block.
     */
    override var scope: ScopedProperty<EditorScope, TextBackgroundItemScope> = {
        (this as InspectorBar.Scope).run {
            val parentScope = rememberLastValue(this) {
                if (editorContext.safeSelection == null) lastValue else this@run
            }

            fun getIcon(designBlock: DesignBlock): EditorIcon = runCatching {
                editorContext.engine.block.getBoolean(designBlock, "backgroundColor/enabled")
            }
                .getOrNull()
                ?.takeIf { it }
                ?.let { editorContext.engine.block.getColor(designBlock, "backgroundColor/color") }
                ?.toComposeColor(editorContext.engine)
                .let { EditorIcon.Colors(color = it) }

            val initial = remember(parentScope) {
                getIcon(designBlock = editorContext.selection.designBlock)
            }
            val editorIcon by remember(parentScope) {
                val selection = parentScope.editorContext.selection
                editorContext.engine.event.subscribe(listOf(selection.designBlock))
                    .filter {
                        // When the design block is unselected/deleted, this lambda is entered before parent scope is updated.
                        // We need to make sure that current component does not update if engine selection has changed.
                        selection.designBlock == editorContext.engine.block.findAllSelected().firstOrNull()
                    }
                    .map { getIcon(designBlock = selection.designBlock) }
                    .onStart { emit(initial) }
            }.collectAsState(initial = initial)
            remember(parentScope, editorIcon) {
                TextBackgroundItemScope(parentScope = parentScope, icon = editorIcon)
            }
        }
    }
}

/**
 * A composable helper function that creates and remembers an [Button] that opens text background options sheet via
 * [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberTextBackground(
    builder: AbstractButtonBuilder<TextBackgroundItemScope>.() -> Unit = {},
): Button<TextBackgroundItemScope> = Button.remember(::TextBackgroundButtonBuilder) {
    id = { InspectorBar.Button.Id.textBackground }
    visible = {
        remember(this) {
            editorContext.selection.type == DesignBlockType.Text &&
                editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "text/character")
        }
    }
    icon = {
        val editorIcon = editorContext.icon
        EditorIcon(icon = editorIcon)
    }
    textString = { stringResource(R.string.ly_img_editor_inspector_bar_button_text_background) }
    onClick = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.TextBackground()))
    }
    builder()
}
