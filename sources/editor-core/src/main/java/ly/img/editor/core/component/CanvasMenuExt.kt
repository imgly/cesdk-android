@file:Suppress("UnusedReceiverParameter")

package ly.img.editor.core.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import ly.img.editor.core.R
import ly.img.editor.core.component.data.unsafeLazy
import ly.img.editor.core.configuration.remember
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.iconpack.BringForward
import ly.img.editor.core.iconpack.Delete
import ly.img.editor.core.iconpack.Duplicate
import ly.img.editor.core.iconpack.IconPack
import ly.img.editor.core.iconpack.SendBackward

/**
 * The id of the canvas menu button returned by [CanvasMenu.Button.rememberBringForward].
 */
val CanvasMenu.Button.Id.bringForward by unsafeLazy {
    EditorComponentId("ly.img.component.canvasMenu.button.bringForward")
}

/**
 * A composable helper function that creates and remembers an [Button] that brings forward currently
 * selected design block via [EditorEvent.Selection.BringForward].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the canvas menu.
 */
@Composable
fun CanvasMenu.Button.rememberBringForward(builder: CanvasMenu.ButtonBuilder.() -> Unit = {}): Button<CanvasMenu.ItemScope> =
    CanvasMenu.Button.remember {
        id = { CanvasMenu.Button.Id.bringForward }
        visible = { editorContext.canSelectionMove }
        vectorIcon = { IconPack.BringForward }
        contentDescription = { stringResource(R.string.ly_img_editor_canvas_menu_button_bring_forward) }
        enabled = {
            remember(this) {
                editorContext.selectionSiblings.isNotEmpty() &&
                    editorContext.selectionSiblings.last() != editorContext.selection.designBlock
            }
        }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Selection.BringForward())
        }
        builder()
    }

/**
 * The id of the canvas menu button returned by [CanvasMenu.Button.rememberSendBackward].
 */
val CanvasMenu.Button.Id.sendBackward by unsafeLazy {
    EditorComponentId("ly.img.component.canvasMenu.button.sendBackward")
}

/**
 * A composable helper function that creates and remembers an [Button] that sends backward currently
 * selected design block via [EditorEvent.Selection.SendBackward].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the canvas menu.
 */
@Composable
fun CanvasMenu.Button.rememberSendBackward(builder: CanvasMenu.ButtonBuilder.() -> Unit = {}): Button<CanvasMenu.ItemScope> =
    CanvasMenu.Button.remember {
        id = { CanvasMenu.Button.Id.sendBackward }
        visible = { editorContext.canSelectionMove }
        vectorIcon = { IconPack.SendBackward }
        contentDescription = { stringResource(R.string.ly_img_editor_canvas_menu_button_send_backward) }
        enabled = {
            remember(this) {
                editorContext.selectionSiblings.isNotEmpty() &&
                    editorContext.selectionSiblings.first() != editorContext.selection.designBlock
            }
        }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Selection.SendBackward())
        }
        builder()
    }

/**
 * The id of the canvas menu button returned by [CanvasMenu.Button.rememberDuplicate].
 */
val CanvasMenu.Button.Id.duplicate by unsafeLazy {
    EditorComponentId("ly.img.component.canvasMenu.button.duplicate")
}

/**
 * A composable helper function that creates and remembers an [Button] that duplicates currently
 * selected design block via [EditorEvent.Selection.Duplicate].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the canvas menu.
 */
@Composable
fun CanvasMenu.Button.rememberDuplicate(builder: CanvasMenu.ButtonBuilder.() -> Unit = {}): Button<CanvasMenu.ItemScope> =
    CanvasMenu.Button.remember {
        id = { CanvasMenu.Button.Id.duplicate }
        visible = {
            editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "lifecycle/duplicate")
        }
        vectorIcon = { IconPack.Duplicate }
        contentDescription = { stringResource(R.string.ly_img_editor_canvas_menu_button_duplicate) }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Selection.Duplicate())
        }
        builder()
    }

/**
 * The id of the canvas menu button returned by [CanvasMenu.Button.rememberDelete].
 */
val CanvasMenu.Button.Id.delete by unsafeLazy {
    EditorComponentId("ly.img.component.canvasMenu.button.delete")
}

/**
 * A composable helper function that creates and remembers an [Button] that deletes currently
 * selected design block via [EditorEvent.Selection.Delete].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the canvas menu.
 */
@Composable
fun CanvasMenu.Button.rememberDelete(builder: CanvasMenu.ButtonBuilder.() -> Unit = {}): Button<CanvasMenu.ItemScope> =
    CanvasMenu.Button.remember {
        id = { CanvasMenu.Button.Id.delete }
        visible = {
            editorContext.engine.block.isAllowedByScope(editorContext.selection.designBlock, "lifecycle/destroy")
        }
        vectorIcon = { IconPack.Delete }
        contentDescription = { stringResource(R.string.ly_img_editor_canvas_menu_button_delete) }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Selection.Delete())
        }
        builder()
    }

/**
 * The id of the canvas menu button returned by [CanvasMenu.Button.rememberSelectGroup].
 */
val CanvasMenu.Button.Id.selectGroup by unsafeLazy {
    EditorComponentId("ly.img.component.canvasMenu.button.selectGroup")
}

/**
 * A composable helper function that creates and remembers an [Button] that selects the group design block that
 * contains the currently selected design block via [EditorEvent.Selection.SelectGroup].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the canvas menu.
 */
@Composable
fun CanvasMenu.Button.rememberSelectGroup(builder: CanvasMenu.ButtonBuilder.() -> Unit = {}): Button<CanvasMenu.ItemScope> =
    CanvasMenu.Button.remember {
        id = { CanvasMenu.Button.Id.selectGroup }
        visible = {
            remember(this) { editorContext.isSelectionInGroup }
        }
        textString = { stringResource(R.string.ly_img_editor_canvas_menu_button_select_group) }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Selection.SelectGroup())
        }
        builder()
    }
