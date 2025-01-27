package ly.img.editor.core.component

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import ly.img.editor.core.EditorScope
import ly.img.editor.core.LocalEditorScope
import ly.img.editor.core.component.CanvasMenu.Button
import ly.img.editor.core.component.CanvasMenu.ButtonScope
import ly.img.editor.core.component.EditorComponent.Companion.noneEnterTransition
import ly.img.editor.core.component.EditorComponent.Companion.noneExitTransition
import ly.img.editor.core.component.data.Nothing
import ly.img.editor.core.component.data.nothing
import ly.img.editor.core.component.data.unsafeLazy
import ly.img.editor.core.compose.rememberLastValue
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.iconpack.BringForward
import ly.img.editor.core.iconpack.Delete
import ly.img.editor.core.iconpack.Duplicate
import ly.img.editor.core.iconpack.IconPack
import ly.img.editor.core.iconpack.SendBackward
import ly.img.engine.DesignBlockType

/**
 * The id of the canvas menu button returned by [CanvasMenu.Button.Companion.rememberBringForward].
 */
val Button.Id.Companion.bringForward by unsafeLazy {
    EditorComponentId("ly.img.component.canvasMenu.button.bringForward")
}

/**
 * A composable helper function that creates and remembers a [CanvasMenu.Button]
 * that brings forward currently selected design block via [EditorEvent.Selection.BringForward].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated when the parent component scope ([CanvasMenu.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value always matches with [ButtonScope.canSelectionMove].
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.BringForward].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is null.
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * By default the value is true when the selected design block is not the last reorderable child in the parent design block.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Selection.BringForward] is invoked.
 * @return a button that will be displayed in the canvas menu.
 */
@Composable
fun Button.Companion.rememberBringForward(
    scope: ButtonScope =
        (LocalEditorScope.current as CanvasMenu.Scope).run {
            rememberLastValue(this) {
                if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
            }
        },
    visible: @Composable ButtonScope.() -> Boolean = { editorContext.canSelectionMove },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.BringForward },
    text: (@Composable ButtonScope.() -> String)? = null,
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            editorContext.selectionSiblings.isNotEmpty() &&
                editorContext.selectionSiblings.last() != editorContext.selection.designBlock
        }
    },
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Selection.BringForward())
    },
    `_`: Nothing = nothing,
): Button =
    remember(
        id = Button.Id.bringForward,
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
        `_` = `_`,
    )

/**
 * The id of the canvas menu button returned by [CanvasMenu.Button.Companion. rememberSendBackward].
 */
val Button.Id.Companion.sendBackward by unsafeLazy {
    EditorComponentId("ly.img.component.canvasMenu.button.sendBackward")
}

/**
 * A composable helper function that creates and remembers a [CanvasMenu.Button] that
 * that sends backward currently selected design block via [EditorEvent.Selection.SendBackward].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated both when the parent component scope ([CanvasMenu.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value always matches with [ButtonScope.canSelectionMove].
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.SendBackward].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is null.
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * By default the value is true when the selected design block is not the first reorderable child in the parent design block.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Selection.SendBackward] is invoked.
 * @return a button that will be displayed in the canvas menu.
 */
@Composable
fun Button.Companion.rememberSendBackward(
    scope: ButtonScope =
        (LocalEditorScope.current as CanvasMenu.Scope).run {
            rememberLastValue(this) {
                if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
            }
        },
    visible: @Composable ButtonScope.() -> Boolean = { editorContext.canSelectionMove },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.SendBackward },
    text: (@Composable ButtonScope.() -> String)? = null,
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            editorContext.selectionSiblings.isNotEmpty() &&
                editorContext.selectionSiblings.first() != editorContext.selection.designBlock
        }
    },
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Selection.SendBackward())
    },
    `_`: Nothing = nothing,
): Button =
    remember(
        id = Button.Id.sendBackward,
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
        `_` = `_`,
    )

/**
 * The id of the canvas menu button returned by [CanvasMenu.Button.Companion.rememberDuplicate].
 */
val Button.Id.Companion.duplicate by unsafeLazy {
    EditorComponentId("ly.img.component.canvasMenu.button.duplicate")
}

/**
 * A composable helper function that creates and remembers a [CanvasMenu.Button]
 * that duplicates currently selected design block via [EditorEvent.Selection.Duplicate].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated both when the parent component scope ([CanvasMenu.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the selected design block is not in a [DesignBlockType.Group]
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
 * Default value is null.
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Selection.Duplicate] is invoked.
 * @return a button that will be displayed in the canvas menu.
 */
@Composable
fun Button.Companion.rememberDuplicate(
    scope: ButtonScope =
        (LocalEditorScope.current as CanvasMenu.Scope).run {
            rememberLastValue(this) {
                if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
            }
        },
    visible: @Composable ButtonScope.() -> Boolean = {
        editorContext.isSelectionInGroup.not() &&
            editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "lifecycle/duplicate")
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.Duplicate },
    text: (@Composable ButtonScope.() -> String)? = null,
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Selection.Duplicate())
    },
    `_`: Nothing = nothing,
): Button =
    remember(
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
        `_` = `_`,
    )

/**
 * The id of the canvas menu button returned by [CanvasMenu.Button.Companion.rememberDelete].
 */
val Button.Id.Companion.delete by unsafeLazy {
    EditorComponentId("ly.img.component.canvasMenu.button.delete")
}

/**
 * A composable helper function that creates and remembers a [CanvasMenu.Button] that
 * that deletes currently selected design block via [EditorEvent.Selection.Delete].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
 * signature @Composable Scope.() -> {}.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * By default it is updated both when the parent component scope ([CanvasMenu.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the selected design block is not in a [DesignBlockType.Group]
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
 * Default value is null.
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Selection.Delete] is invoked.
 * @return a button that will be displayed in the canvas menu.
 */
@Composable
fun Button.Companion.rememberDelete(
    scope: ButtonScope =
        (LocalEditorScope.current as CanvasMenu.Scope).run {
            rememberLastValue(this) {
                if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
            }
        },
    visible: @Composable ButtonScope.() -> Boolean = {
        editorContext.isSelectionInGroup.not() &&
            editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "lifecycle/destroy")
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.Delete },
    text: (@Composable ButtonScope.() -> String)? = null,
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Selection.Delete())
    },
    `_`: Nothing = nothing,
): Button =
    remember(
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
        `_` = `_`,
    )
