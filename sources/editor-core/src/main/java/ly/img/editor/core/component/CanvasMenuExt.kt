package ly.img.editor.core.component

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ly.img.editor.core.EditorScope
import ly.img.editor.core.LocalEditorScope
import ly.img.editor.core.R
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
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is always [R.string.ly_img_editor_bring_forward].
 * @return a button that will be displayed in the canvas menu.
 */
@Composable
fun Button.Companion.rememberBringForward(
    scope: ButtonScope = (LocalEditorScope.current as CanvasMenu.Scope).run {
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
    contentDescription: (@Composable ButtonScope.() -> String)? = {
        stringResource(R.string.ly_img_editor_bring_forward)
    },
    `_`: Nothing = nothing,
): Button = remember(
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
    contentDescription = contentDescription,
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
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is always [R.string.ly_img_editor_send_backward].
 * @return a button that will be displayed in the canvas menu.
 */
@Composable
fun Button.Companion.rememberSendBackward(
    scope: ButtonScope = (LocalEditorScope.current as CanvasMenu.Scope).run {
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
    contentDescription: (@Composable ButtonScope.() -> String)? = {
        stringResource(R.string.ly_img_editor_send_backward)
    },
    `_`: Nothing = nothing,
): Button = remember(
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
    contentDescription = contentDescription,
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
 * By default the value is true when the selected design block has an enabled engine scope "lifecycle/duplicate".
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
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is always [R.string.ly_img_editor_duplicate].
 * @return a button that will be displayed in the canvas menu.
 */
@Composable
fun Button.Companion.rememberDuplicate(
    scope: ButtonScope = (LocalEditorScope.current as CanvasMenu.Scope).run {
        rememberLastValue(this) {
            if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
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
    contentDescription: (@Composable ButtonScope.() -> String)? = {
        stringResource(R.string.ly_img_editor_duplicate)
    },
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
 * By default the value is true when the selected design block has an enabled engine scope "lifecycle/destroy".
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
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is always [R.string.ly_img_editor_delete].
 * @return a button that will be displayed in the canvas menu.
 */
@Composable
fun Button.Companion.rememberDelete(
    scope: ButtonScope = (LocalEditorScope.current as CanvasMenu.Scope).run {
        rememberLastValue(this) {
            if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
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
    contentDescription: (@Composable ButtonScope.() -> String)? = {
        stringResource(R.string.ly_img_editor_delete)
    },
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
 * The id of the canvas menu button returned by [CanvasMenu.Button.Companion.rememberSelectGroup].
 */
val Button.Id.Companion.selectGroup by unsafeLazy {
    EditorComponentId("ly.img.component.canvasMenu.button.selectGroup")
}

/**
 * A composable helper function that creates and remembers a [CanvasMenu.Button] that selects the group design block that
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
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_select_group].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Selection.SelectGroup] is invoked.
 * @return a button that will be displayed in the canvas menu.
 */
@Composable
fun Button.Companion.rememberSelectGroup(
    scope: ButtonScope = (LocalEditorScope.current as CanvasMenu.Scope).run {
        rememberLastValue(this) {
            if (editorContext.safeSelection == null) lastValue else ButtonScope(parentScope = this@run)
        }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            editorContext.isSelectionInGroup
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    text: @Composable ButtonScope.() -> String = { stringResource(R.string.ly_img_editor_select_group) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Selection.SelectGroup())
    },
    `_`: Nothing = nothing,
): CanvasMenu.Custom<ButtonScope> = CanvasMenu.Custom.remember(
    id = Button.Id.selectGroup,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
) {
    decoration(this) {
        val tintColor = tint?.invoke(this) ?: MaterialTheme.colorScheme.onSecondaryContainer
        Button(
            modifier = Modifier.padding(horizontal = 4.dp),
            colors =
                ButtonDefaults.textButtonColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = tintColor,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = tintColor.copy(alpha = tintColor.alpha * 0.5F),
                ),
            onClick = { onClick(this) },
            enabled = enabled(this),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp),
        ) {
            Text(
                text = text(this@remember),
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}
