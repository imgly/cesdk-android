package ly.img.editor.core.component

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import ly.img.editor.core.EditorScope
import ly.img.editor.core.LocalEditorScope
import ly.img.editor.core.R
import ly.img.editor.core.component.EditorComponent.Companion.noneEnterTransition
import ly.img.editor.core.component.EditorComponent.Companion.noneExitTransition
import ly.img.editor.core.component.InspectorBar.Button
import ly.img.editor.core.component.InspectorBar.ButtonScope
import ly.img.editor.core.component.data.ConicalGradientFill
import ly.img.editor.core.component.data.EditorIcon
import ly.img.editor.core.component.data.Fill
import ly.img.editor.core.component.data.LinearGradientFill
import ly.img.editor.core.component.data.Nothing
import ly.img.editor.core.component.data.RadialGradientFill
import ly.img.editor.core.component.data.Selection
import ly.img.editor.core.component.data.SolidFill
import ly.img.editor.core.component.data.nothing
import ly.img.editor.core.component.data.unsafeLazy
import ly.img.editor.core.compose.rememberLastValue
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
import ly.img.editor.core.iconpack.ReorderHorizontally
import ly.img.editor.core.iconpack.Replace
import ly.img.editor.core.iconpack.SelectGroup
import ly.img.editor.core.iconpack.ShapeIcon
import ly.img.editor.core.iconpack.Split
import ly.img.editor.core.iconpack.Typeface
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
import ly.img.engine.SceneMode
import ly.img.engine.ShapeType

private const val KIND_STICKER = "sticker"
private const val KIND_ANIMATED_STICKER = "animatedSticker"

private fun Selection.isAnyKindOfSticker(): Boolean = this.kind == KIND_STICKER || this.kind == KIND_ANIMATED_STICKER

private fun Selection.isNotAnyKindOfSticker() = !this.isAnyKindOfSticker()

/**
 * An extension function for checking whether the [designBlock] is a background track.
 *
 * @return true if the [designBlock] is a background track.
 */
private fun Engine.isBackgroundTrack(designBlock: DesignBlock): Boolean =
    DesignBlockType.get(block.getType(designBlock)) == DesignBlockType.Track &&
        block.isAlwaysOnBottom(designBlock)

/**
 * An extension function for checking whether the [selection] can be moved up/down.
 *
 * @param selection the selection that is queried.
 * @return true if the [selection] can be moved, false otherwise.
 */
private fun Engine.isMoveAllowed(selection: Selection): Boolean = block.isAllowedByScope(selection.designBlock, "layer/move") &&
    selection.parentDesignBlock?.let { !isBackgroundTrack(it) } ?: true

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.Companion.rememberReorder].
 */
val Button.Id.Companion.reorder by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.reorder")
}

/**
 * A composable helper function that creates and remembers an [InspectorBar.Button] that
 * opens reorder sheet via [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated both when the parent component scope ([InspectorBar.scope], accessed via [LocalEditorScope]) is updated
 * and whenever the number of children in the background track becomes >= or < than 2.
 * @param visible whether the button should be visible.
 * By default the value is true when the editor has a scene with a background track where the number of tracks is >= 2, false otherwise.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.ReorderHorizontally].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_inspector_bar_button_reorder].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] is invoked with sheet type [SheetType.Reorder].
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun Button.Companion.rememberReorder(
    scope: ButtonScope = (LocalEditorScope.current as InspectorBar.Scope).run {
        rememberLastValue(this) {
            if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            editorContext.selection.parentDesignBlock
                ?.let { parent ->
                    editorContext.engine.isBackgroundTrack(parent) &&
                        editorContext.engine.block.getChildren(parent).size >= 2
                } ?: false
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.ReorderHorizontally },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_inspector_bar_button_reorder) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Reorder()))
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.reorder,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.Companion.rememberAnimations].
 */
val Button.Id.Companion.animations by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.animations")
}

/**
 * A helper function that returns an [InspectorBar.Button] that opens animation sheet via [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated only when the parent component scope ([InspectorBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the [SceneMode] is video and the selected design block type is not [DesignBlockType.Page]
 * or [DesignBlockType.Audio].
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.Animation].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_inspector_bar_button_animations].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] is invoked with sheet type [SheetType.Animation].
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun Button.Companion.rememberAnimations(
    scope: ButtonScope = (LocalEditorScope.current as InspectorBar.Scope).run {
        rememberLastValue(this) {
            if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            val selection = editorContext.selection
            editorContext.engine.scene.getMode() == SceneMode.VIDEO &&
                selection.type != DesignBlockType.Page &&
                selection.type != DesignBlockType.Audio
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.Animation },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_inspector_bar_button_animations) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Animation()))
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.animations,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.Companion.rememberAdjustments].
 */
val Button.Id.Companion.adjustments by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.adjustments")
}

/**
 * A helper function that returns an [InspectorBar.Button] that opens adjustments sheet via [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated only when the parent component scope ([InspectorBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the selected design block has a fill type [FillType.Image] or [FillType.Video] (where kind is not "sticker" or "animatedSticker")
 * and an enabled engine scope "appearance/adjustments".
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.Adjustments].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_inspector_bar_button_adjustments].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] is invoked with sheet type [SheetType.Adjustments].
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun Button.Companion.rememberAdjustments(
    scope: ButtonScope = (LocalEditorScope.current as InspectorBar.Scope).run {
        rememberLastValue(this) {
            if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            editorContext.selection.isNotAnyKindOfSticker() &&
                (editorContext.selection.fillType == FillType.Image || editorContext.selection.fillType == FillType.Video) &&
                editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "appearance/adjustments")
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.Adjustments },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_inspector_bar_button_adjustments) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Adjustments()))
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.adjustments,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.Companion.rememberFilter].
 */
val Button.Id.Companion.filter by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.filter")
}

/**
 * A helper function that returns an [InspectorBar.Button] that opens filter sheet via [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated only when the parent component scope ([InspectorBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the selected design block has a fill type [FillType.Image] or [FillType.Video] (where kind is not "sticker" or "animatedSticker")
 * and an enabled engine scope "appearance/filter".
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.Filter].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_inspector_bar_button_filter].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] is invoked with sheet type [SheetType.Filter].
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun Button.Companion.rememberFilter(
    scope: ButtonScope = (LocalEditorScope.current as InspectorBar.Scope).run {
        rememberLastValue(this) {
            if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            editorContext.selection.isNotAnyKindOfSticker() &&
                (editorContext.selection.fillType == FillType.Image || editorContext.selection.fillType == FillType.Video) &&
                editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "appearance/filter")
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.Filter },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_inspector_bar_button_filter) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Filter()))
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.filter,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.Companion.rememberEffect].
 */
val Button.Id.Companion.effect by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.effect")
}

/**
 * A helper function that returns an [InspectorBar.Button] that opens effect sheet via [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated only when the parent component scope ([InspectorBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * * By default the value is true when the selected design block has a fill type [FillType.Image] or [FillType.Video] (where kind is not "sticker" or "animatedSticker")
 * and an enabled engine scope "appearance/effect".
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.Effect].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_inspector_bar_button_effect].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] is invoked with sheet type [SheetType.Effect].
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun Button.Companion.rememberEffect(
    scope: ButtonScope = (LocalEditorScope.current as InspectorBar.Scope).run {
        rememberLastValue(this) {
            if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            editorContext.selection.isNotAnyKindOfSticker() &&
                (editorContext.selection.fillType == FillType.Image || editorContext.selection.fillType == FillType.Video) &&
                editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "appearance/effect")
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.Effect },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_inspector_bar_button_effect) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Effect()))
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.effect,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.Companion.rememberBlur].
 */
val Button.Id.Companion.blur by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.blur")
}

/**
 * A helper function that returns an [InspectorBar.Button] that opens blur sheet via [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated only when the parent component scope ([InspectorBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the selected design block has a fill type [FillType.Image] or [FillType.Video] (where kind is not "sticker" or "animatedSticker")
 * and an enabled engine scope "appearance/blur".
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.Blur].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_inspector_bar_button_blur].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] is invoked with sheet type [SheetType.Blur].
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun Button.Companion.rememberBlur(
    scope: ButtonScope = (LocalEditorScope.current as InspectorBar.Scope).run {
        rememberLastValue(this) {
            if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            editorContext.selection.isNotAnyKindOfSticker() &&
                (editorContext.selection.fillType == FillType.Image || editorContext.selection.fillType == FillType.Video) &&
                editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "appearance/blur")
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.Blur },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_inspector_bar_button_blur) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Blur()))
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.blur,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.Companion.rememberVolume].
 */
val Button.Id.Companion.volume by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.volume")
}

/**
 * A helper function that returns an [InspectorBar.Button] that opens volume sheet via [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated only when the parent component scope ([InspectorBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the selected design block is [DesignBlockType.Audio] or has a fill type [FillType.Video] and
 * an enabled engine scope "fill/change".
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.VolumeHigh].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_inspector_bar_button_volume].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] is invoked with sheet type [SheetType.Volume].
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun Button.Companion.rememberVolume(
    scope: ButtonScope = (LocalEditorScope.current as InspectorBar.Scope).run {
        rememberLastValue(this) {
            if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            (editorContext.selection.type == DesignBlockType.Audio || editorContext.selection.fillType == FillType.Video) &&
                editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "fill/change")
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.VolumeHigh },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_inspector_bar_button_volume) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Volume()))
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.volume,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.Companion.rememberCrop].
 */
val Button.Id.Companion.crop by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.crop")
}

/**
 * A helper function that returns an [InspectorBar.Button] that opens crop sheet via [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated only when the parent component scope ([InspectorBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the selected design block has a fill type [FillType.Image] or [FillType.Video] (where kind is not "sticker" or "animatedSticker"),
 * [ly.img.engine.BlockApi.supportsCrop] is true and an enabled engine scope "fill/change".
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.CropRotate].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_inspector_bar_button_crop].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] is invoked with sheet type [SheetType.Crop].
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun Button.Companion.rememberCrop(
    scope: ButtonScope = (LocalEditorScope.current as InspectorBar.Scope).run {
        rememberLastValue(this) {
            if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            val designBlock = editorContext.selection.designBlock
            editorContext.selection.isNotAnyKindOfSticker() &&
                (editorContext.selection.fillType == FillType.Image || editorContext.selection.fillType == FillType.Video) &&
                editorContext.engine.block.supportsCrop(designBlock) &&
                editorContext.engine.block.isAllowedByScope(designBlock, "layer/crop")
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.CropRotate },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_inspector_bar_button_crop) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        val mode = if (editorContext.selection.type == DesignBlockType.Page) {
            SheetType.Crop.Mode.PageCrop
        } else {
            SheetType.Crop.Mode.Element
        }
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Crop(mode = mode)))
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.crop,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.Companion.rememberDuplicate].
 */
val Button.Id.Companion.duplicate by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.duplicate")
}

/**
 * A helper function that returns an [InspectorBar.Button] that duplicates currently selected
 * design block via [EditorEvent.Selection.Duplicate].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated only when the parent component scope ([InspectorBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the selected design block type is not [DesignBlockType.Page]
 * and has an enabled engine scope "lifecycle/duplicate".
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.Duplicate].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_inspector_bar_button_duplicate].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Selection.Duplicate] is invoked.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun Button.Companion.rememberDuplicate(
    scope: ButtonScope = (LocalEditorScope.current as InspectorBar.Scope).run {
        rememberLastValue(this) {
            if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            editorContext.selection.type != DesignBlockType.Page &&
                editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "lifecycle/duplicate")
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.Duplicate },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_inspector_bar_button_duplicate) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Selection.Duplicate())
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.duplicate,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.Companion.rememberLayer].
 */
val Button.Id.Companion.layer by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.layer")
}

/**
 * A helper function that returns an [InspectorBar.Button] that opens layer sheet via [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated only when the parent component scope ([InspectorBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the selected design block type is not [DesignBlockType.Page] or [DesignBlockType.Audio],
 * has enabled at least one of engine scopes "layer/blendMode", "layer/opacity", "lifecycle/duplicate", and "lifecycle/destroy", or
 * [isMoveAllowed] returns true.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.LayersOutline].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_inspector_bar_button_layer].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] is invoked with sheet type [SheetType.Layer].
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun Button.Companion.rememberLayer(
    scope: ButtonScope = (LocalEditorScope.current as InspectorBar.Scope).run {
        rememberLastValue(this) {
            if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
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
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.LayersOutline },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_inspector_bar_button_layer) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Layer()))
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.layer,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.Companion.rememberSplit].
 */
val Button.Id.Companion.split by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.split")
}

/**
 * A helper function that returns an [InspectorBar.Button] that splits currently selected
 * design block via [EditorEvent.Selection.Split] in a video scene.
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated only when the parent component scope ([InspectorBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the selected design block has an enabled engine scope "lifecycle/duplicate"
 * and the [SceneMode] is video.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.Split].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_inspector_bar_button_split].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Selection.Split] is invoked.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun Button.Companion.rememberSplit(
    scope: ButtonScope = (LocalEditorScope.current as InspectorBar.Scope).run {
        rememberLastValue(this) {
            if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "lifecycle/duplicate") &&
                editorContext.engine.scene.getMode() == SceneMode.VIDEO
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.Split },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_inspector_bar_button_split) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Selection.Split())
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.split,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

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
 * The id of the inspector bar button returned by [InspectorBar.Button.Companion.rememberFillStroke].
 */
val Button.Id.Companion.fillStroke by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.fillStroke")
}

/**
 * A helper function that returns an [InspectorBar.Button] that opens fill stroke sheet via [EditorEvent.Sheet.Open].
 * [InspectorBar.FillStrokeButtonScope] is constructed using [getFillStrokeButtonIcon] function.
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated only when the parent component scope ([InspectorBar.scope], accessed via [LocalEditorScope]) is updated and
 * when fill stroke value of the selected design block changes.
 * @param visible whether the button should be visible.
 * By default the value is true when the selected design block's kind is not "sticker" or "animatedSticker"
 * and [InspectorBar.FillStrokeButtonScope.fillStrokeIcon] has showFill == true or showStroke == true.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param icon the icon content of the button. If null then icon is not rendered.
 * Default value is an [EditorIcon], which is built based on [InspectorBar.FillStrokeButtonScope].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is a text, that is built based on [InspectorBar.FillStrokeButtonScope].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] is invoked with sheet type [SheetType.FillStroke].
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun Button.Companion.rememberFillStroke(
    scope: ButtonScope = (LocalEditorScope.current as InspectorBar.Scope).run {
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
            InspectorBar.FillStrokeButtonScope(parentScope = this, fillStrokeIcon = fillStrokeIcon)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            val fillStrokeIcon = (this as InspectorBar.FillStrokeButtonScope).editorContext.fillStrokeIcon
            editorContext.selection.isNotAnyKindOfSticker() && (fillStrokeIcon.showFill || fillStrokeIcon.showStroke)
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    icon: (@Composable ButtonScope.() -> Unit)? = {
        val fillStrokeIcon = (this as InspectorBar.FillStrokeButtonScope).editorContext.fillStrokeIcon
        EditorIcon(fillStrokeIcon)
    },
    text: (@Composable ButtonScope.() -> String)? = {
        val textResId = remember(this) {
            val fillStrokeIcon = (this as InspectorBar.FillStrokeButtonScope).editorContext.fillStrokeIcon
            if (fillStrokeIcon.showFill && fillStrokeIcon.showStroke) {
                R.string.ly_img_editor_inspector_bar_button_fill_and_stroke
            } else if (fillStrokeIcon.showFill) {
                R.string.ly_img_editor_inspector_bar_button_fill
            } else {
                R.string.ly_img_editor_inspector_bar_button_stroke
            }
        }
        stringResource(textResId)
    },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.FillStroke()))
    },
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.fillStroke,
    icon = icon,
    text = text?.let {
        {
            Text(
                text = text(this),
                style = MaterialTheme.typography.labelSmall,
                color = tint?.invoke(this) ?: Color.Unspecified,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    },
    enabled = enabled,
    onClick = onClick,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    `_` = `_`,
)

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.Companion.rememberMoveAsClip].
 */
val Button.Id.Companion.moveAsClip by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.moveAsClip")
}

/**
 * A helper function that returns an [InspectorBar.Button] that moves currently selected design block into
 * the background track as clip via [EditorEvent.Selection.MoveAsClip].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated only when the parent component scope ([InspectorBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the selected design block does not have a type [DesignBlockType.Audio],
 * the [SceneMode] is video and the parent is not a background track.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.AsClip].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_inspector_bar_button_move_as_clip].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Selection.MoveAsClip] is invoked.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun Button.Companion.rememberMoveAsClip(
    scope: ButtonScope = (LocalEditorScope.current as InspectorBar.Scope).run {
        rememberLastValue(this) {
            if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            editorContext.selection.type != DesignBlockType.Audio &&
                editorContext.engine.scene.getMode() == SceneMode.VIDEO &&
                editorContext.selection.parentDesignBlock.let {
                    it != null && editorContext.engine.isBackgroundTrack(it).not()
                }
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.AsClip },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_inspector_bar_button_move_as_clip) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Selection.MoveAsClip())
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.moveAsClip,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.Companion.rememberMoveAsOverlay].
 */
val Button.Id.Companion.moveAsOverlay by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.moveAsOverlay")
}

/**
 * A helper function that returns an [InspectorBar.Button] that moves currently selected design block from
 * the background track to an overlay via [EditorEvent.Selection.MoveAsOverlay]
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated only when the parent component scope ([InspectorBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the selected design block does not have a type [DesignBlockType.Audio],
 * the [SceneMode] is video and the parent is a background track.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.AsOverlay].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_inspector_bar_button_move_as_overlay].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Selection.MoveAsOverlay] is invoked.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun Button.Companion.rememberMoveAsOverlay(
    scope: ButtonScope = (LocalEditorScope.current as InspectorBar.Scope).run {
        rememberLastValue(this) {
            if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            editorContext.selection.type != DesignBlockType.Audio &&
                editorContext.engine.scene.getMode() == SceneMode.VIDEO &&
                editorContext.selection.parentDesignBlock.let {
                    it != null && editorContext.engine.isBackgroundTrack(it)
                }
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.AsOverlay },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_inspector_bar_button_move_as_overlay) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Selection.MoveAsOverlay())
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.moveAsOverlay,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.Companion.rememberReplace].
 */
val Button.Id.Companion.replace by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.replace")
}

/**
 * A helper function that returns an [InspectorBar.Button] that opens a library sheet via [EditorEvent.Sheet.Open].
 * Selected asset will replace the content of the currently selected design block.
 * By default [DesignBlockType], [FillType] and kind of the selected design block are used to find the library in
 * [ly.img.editor.core.library.AssetLibrary].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated only when the parent component scope ([InspectorBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the selected design block type is [DesignBlockType.Audio] or
 * [DesignBlockType.Graphic] with [FillType.Image] or [FillType.Video] fill and does not hand kind "sticker" or "animatedSticker" and
 * has an enabled engine scope "fill/change".
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.Replace].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_inspector_bar_button_replace].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] is invoked with sheet type [SheetType.LibraryReplace] where
 * the libraryCategory is picked from the [ly.img.editor.core.library.AssetLibrary] based on the [DesignBlockType], [FillType] and kind
 * of the selected block.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun Button.Companion.rememberReplace(
    scope: ButtonScope = (LocalEditorScope.current as InspectorBar.Scope).run {
        rememberLastValue(this) {
            if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            (
                editorContext.selection.type == DesignBlockType.Audio ||
                    (
                        editorContext.selection.type == DesignBlockType.Graphic &&
                            (editorContext.selection.fillType == FillType.Image || editorContext.selection.fillType == FillType.Video)
                    ) &&
                    editorContext.selection.isNotAnyKindOfSticker() &&
                    editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "fill/change")
            )
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.Replace },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_inspector_bar_button_replace) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        val libraryCategory = when (editorContext.selection.type) {
            DesignBlockType.Audio -> editorContext.assetLibrary.audios
            DesignBlockType.Graphic -> {
                when (editorContext.selection.kind) {
                    KIND_STICKER,
                    KIND_ANIMATED_STICKER,
                    -> editorContext.assetLibrary.stickers

                    else -> when (editorContext.selection.fillType) {
                        FillType.Image -> editorContext.assetLibrary.images
                        FillType.Video -> editorContext.assetLibrary.videos
                        else -> {
                            error(
                                "Unsupported fillType ${editorContext.selection.fillType} for replace inspector bar button.",
                            )
                        }
                    }
                }
            }
            else -> error("Unsupported type ${editorContext.selection.type} for replace inspector bar button.")
        }(editorContext.engine.scene.getMode())
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.LibraryReplace(libraryCategory = libraryCategory)))
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.replace,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.Companion.rememberEnterGroup].
 */
val Button.Id.Companion.enterGroup by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.enterGroup")
}

/**
 * A helper function that changes selection from the selected group design block to a design block
 * within that group via [EditorEvent.Selection.EnterGroup].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated only when the parent component scope ([InspectorBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the selected design block type is [DesignBlockType.Group].
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.GroupEnter].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_inspector_bar_button_enter_group].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Selection.EnterGroup] is invoked.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun Button.Companion.rememberEnterGroup(
    scope: ButtonScope = (LocalEditorScope.current as InspectorBar.Scope).run {
        rememberLastValue(this) {
            if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            editorContext.selection.type == DesignBlockType.Group
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.GroupEnter },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_inspector_bar_button_enter_group) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Selection.EnterGroup())
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.enterGroup,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.Companion.rememberSelectGroup].
 */
val Button.Id.Companion.selectGroup by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.selectGroup")
}

/**
 * A helper function that returns an [InspectorBar.Button] that selects the group design block that
 * contains the currently selected design block via [EditorEvent.Selection.SelectGroup].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated only when the parent component scope ([InspectorBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the selected design block is part of a [DesignBlockType.Group].
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.SelectGroup].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_inspector_bar_button_select_group].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Selection.EnterGroup] is invoked.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun Button.Companion.rememberSelectGroup(
    scope: ButtonScope = (LocalEditorScope.current as InspectorBar.Scope).run {
        rememberLastValue(this) {
            if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            val parentDesignBlock = editorContext.selection.parentDesignBlock ?: return@remember false
            DesignBlockType.get(editorContext.engine.block.getType(parentDesignBlock)) == DesignBlockType.Group
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.SelectGroup },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_inspector_bar_button_select_group) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Selection.SelectGroup())
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.selectGroup,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.Companion.rememberDelete].
 */
val Button.Id.Companion.delete by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.delete")
}

/**
 * A helper function that returns an [InspectorBar.Button] that deletes currently selected
 * design block via [EditorEvent.Selection.Delete].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated only when the parent component scope ([InspectorBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the selected design block type is not [DesignBlockType.Page]
 * and has an enabled engine scope "lifecycle/destroy".
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.Delete].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_inspector_bar_button_delete].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is [androidx.compose.material3.ColorScheme.error].
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Selection.Delete] is invoked.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun Button.Companion.rememberDelete(
    scope: ButtonScope = (LocalEditorScope.current as InspectorBar.Scope).run {
        rememberLastValue(this) {
            if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            editorContext.selection.type != DesignBlockType.Page &&
                editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "lifecycle/destroy")
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.Delete },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_inspector_bar_button_delete) },
    tint: (@Composable ButtonScope.() -> Color)? = { MaterialTheme.colorScheme.error },
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Selection.Delete())
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.delete,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.Companion.rememberEditText].
 */
val Button.Id.Companion.editText by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.editText")
}

/**
 * A helper function that returns an [InspectorBar.Button] that enters text editing mode for the
 * selected design block via [EditorEvent.Selection.EnterTextEditMode].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated only when the parent component scope ([InspectorBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the selected design block type is [DesignBlockType.Text]
 * and has an enabled engine scope "text/edit".
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.Keyboard].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_inspector_bar_button_edit_text].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Selection.EnterTextEditMode] is invoked.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun Button.Companion.rememberEditText(
    scope: ButtonScope = (LocalEditorScope.current as InspectorBar.Scope).run {
        rememberLastValue(this) {
            if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            editorContext.selection.type == DesignBlockType.Text &&
                editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "text/edit")
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.Keyboard },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_inspector_bar_button_edit_text) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Selection.EnterTextEditMode())
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.editText,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.Companion.rememberFormatText].
 */
val Button.Id.Companion.formatText by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.formatText")
}

/**
 * A helper function that returns an [InspectorBar.Button] that opens text formatting sheet via [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated only when the parent component scope ([InspectorBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the selected design block type is [DesignBlockType.Text]
 * and has an enabled engine scope "text/character".
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.Typeface].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_inspector_bar_button_format_text].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] is invoked with sheet type [SheetType.FormatText].
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun Button.Companion.rememberFormatText(
    scope: ButtonScope = (LocalEditorScope.current as InspectorBar.Scope).run {
        rememberLastValue(this) {
            if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            editorContext.selection.type == DesignBlockType.Text &&
                editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "text/character")
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.Typeface },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_inspector_bar_button_format_text) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.FormatText()))
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.formatText,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.Companion.rememberShape].
 */
val Button.Id.Companion.shape by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.shape")
}

/**
 * A helper function that returns an [InspectorBar.Button] that opens shape options sheet via [EditorEvent.Sheet.Open].
 * The button is applicable for the following shape types:
 * [ShapeType.Star], [ShapeType.Polygon], [ShapeType.Line], [ShapeType.Rect].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated only when the parent component scope ([InspectorBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the selected design block does not have a fill type [FillType.Image], or has but the kind
 * is not "sticker", does not have a fill type [FillType.Video], or has but the kind is not "animatedSticker",
 * and has an enabled engine scope "shape/change", [BlockApi.supportsShape] is true and the [ShapeType] is
 * one of the following: [ShapeType.Star], [ShapeType.Polygon], [ShapeType.Line], [ShapeType.Rect].
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.ShapeIcon].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_inspector_bar_button_shape].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] is invoked with sheet type [SheetType.Shape].
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun Button.Companion.rememberShape(
    scope: ButtonScope = (LocalEditorScope.current as InspectorBar.Scope).run {
        rememberLastValue(this) {
            if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
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
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.ShapeIcon },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_inspector_bar_button_shape) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Shape()))
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.shape,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the inspector bar button returned by [InspectorBar.Button.Companion. rememberTextBackground].
 */
val Button.Id.Companion.textBackground by unsafeLazy {
    EditorComponentId("ly.img.component.inspectorBar.button.textBackground")
}

/**
 * A helper function that returns an [InspectorBar.Button] that opens text background options sheet via [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated only when the parent component scope ([InspectorBar.scope], accessed via [LocalEditorScope]) is updated and
 * when the background color of the selected design block changes.
 * @param visible whether the button should be visible.
 * By default the value is true when the selected design block type is [DesignBlockType.Text]
 * and has an enabled engine scope "text/character".
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param icon the icon content of the button. If null then icon is not rendered.
 * Default value is an [EditorIcon], which is built based on [InspectorBar.TextBackgroundButtonScope].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_inspector_bar_button_text_background].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] is invoked with sheet type [SheetType.TextBackground].
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun Button.Companion.rememberTextBackground(
    scope: ButtonScope = (LocalEditorScope.current as InspectorBar.Scope).run {
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
            InspectorBar.TextBackgroundButtonScope(parentScope = this, icon = editorIcon)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            editorContext.selection.type == DesignBlockType.Text &&
                editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "text/character")
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    icon: (@Composable ButtonScope.() -> Unit)? = {
        val editorIcon = (this as InspectorBar.TextBackgroundButtonScope).editorContext.icon
        EditorIcon(icon = editorIcon)
    },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_inspector_bar_button_text_background) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.TextBackground()))
    },
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.textBackground,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    icon = icon,
    text = text?.let {
        {
            Text(
                text = text(this),
                style = MaterialTheme.typography.labelSmall,
                color = tint?.invoke(this) ?: Color.Unspecified,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    },
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    `_` = `_`,
)
