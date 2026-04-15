@file:Suppress("UnusedReceiverParameter")

package ly.img.editor.core.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ly.img.editor.core.EditorScope
import ly.img.editor.core.R
import ly.img.editor.core.ScopedProperty
import ly.img.editor.core.component.data.unsafeLazy
import ly.img.editor.core.configuration.remember
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.iconpack.ArrowBack
import ly.img.editor.core.iconpack.ArrowForward
import ly.img.editor.core.iconpack.Export
import ly.img.editor.core.iconpack.IconPack
import ly.img.editor.core.iconpack.Pages
import ly.img.editor.core.iconpack.Preview
import ly.img.editor.core.iconpack.PreviewToggled
import ly.img.editor.core.iconpack.Redo
import ly.img.editor.core.iconpack.Undo
import ly.img.editor.core.state.EditorViewMode
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine

/**
 * The id of the navigation bar button returned by [NavigationBar.Button.rememberCloseEditor].
 */
val NavigationBar.Button.Id.closeEditor by unsafeLazy {
    EditorComponentId("ly.img.component.navigationBar.button.closeEditor")
}

/**
 * A composable helper function that creates and remembers an [Button] that triggers
 * [ly.img.editor.core.configuration.EditorConfiguration.onClose] callback via [EditorEvent.OnClose].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the navigation bar.
 */
@Composable
fun NavigationBar.Button.rememberCloseEditor(builder: NavigationBar.ButtonBuilder.() -> Unit = {}): Button<NavigationBar.ItemScope> =
    NavigationBar.Button.remember {
        id = { NavigationBar.Button.Id.closeEditor }
        visible = {
            val state by editorContext.state.collectAsState()
            state.viewMode is EditorViewMode.Edit &&
                remember(this) {
                    editorContext.engine.editor.getSettingBoolean("features/pageCarouselEnabled") ||
                        editorContext.engine.scene.run { getPages().firstOrNull() == getCurrentPage() }
                }
        }
        vectorIcon = { IconPack.ArrowBack }
        contentDescription = { stringResource(R.string.ly_img_editor_navigation_bar_button_close_editor) }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.OnClose())
        }
        builder()
    }

/**
 * The id of the navigation bar button returned by [NavigationBar.Button.rememberUndo].
 */
val NavigationBar.Button.Id.undo by unsafeLazy {
    EditorComponentId("ly.img.component.navigationBar.button.undo")
}

/**
 * A composable helper function that creates and remembers an [Button] that
 * does undo operation in the editor via [ly.img.engine.EditorApi.undo] engine API.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the navigation bar.
 */
@Composable
fun NavigationBar.Button.rememberUndo(builder: NavigationBar.ButtonBuilder.() -> Unit = {}): Button<NavigationBar.ItemScope> =
    NavigationBar.Button.remember {
        id = { NavigationBar.Button.Id.undo }
        visible = alwaysVisible
        vectorIcon = { IconPack.Undo }
        tint = {
            val state by editorContext.state.collectAsState()
            val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
            remember(this, state.viewMode, onSurfaceVariant) {
                if (state.viewMode is EditorViewMode.Preview) Color.Transparent else onSurfaceVariant
            }
        }
        enabled = {
            val state by editorContext.state.collectAsState()
            remember(this, state.viewMode, state.isHistoryEnabled) {
                state.viewMode !is EditorViewMode.Preview && state.isHistoryEnabled && editorContext.engine.editor.canUndo()
            }
        }
        onClick = { editorContext.engine.editor.undo() }
        contentDescription = { stringResource(R.string.ly_img_editor_navigation_bar_button_undo) }
        builder()
    }

/**
 * The id of the navigation bar button returned by [NavigationBar.Button.rememberRedo].
 */
val NavigationBar.Button.Id.redo by unsafeLazy {
    EditorComponentId("ly.img.component.navigationBar.button.redo")
}

/**
 * A composable helper function that creates and remembers an [Button] that
 * does redo operation in the editor via [ly.img.engine.EditorApi.redo] engine API.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the navigation bar.
 */
@Composable
fun NavigationBar.Button.rememberRedo(builder: NavigationBar.ButtonBuilder.() -> Unit = {}): Button<NavigationBar.ItemScope> =
    NavigationBar.Button.remember {
        id = { NavigationBar.Button.Id.redo }
        visible = alwaysVisible
        vectorIcon = { IconPack.Redo }
        tint = {
            val state by editorContext.state.collectAsState()
            val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
            remember(this, state.viewMode, onSurfaceVariant) {
                if (state.viewMode is EditorViewMode.Preview) Color.Transparent else onSurfaceVariant
            }
        }
        enabled = {
            val state by editorContext.state.collectAsState()
            remember(this, state.viewMode, state.isHistoryEnabled) {
                state.viewMode !is EditorViewMode.Preview && state.isHistoryEnabled && editorContext.engine.editor.canRedo()
            }
        }
        onClick = { editorContext.engine.editor.redo() }
        contentDescription = { stringResource(R.string.ly_img_editor_navigation_bar_button_redo) }
        builder()
    }

/**
 * The id of the navigation bar button returned by [NavigationBar.Button.rememberExport].
 */
val NavigationBar.Button.Id.export by unsafeLazy {
    EditorComponentId("ly.img.component.navigationBar.button.export")
}

/**
 * A composable helper function that creates and remembers an [Button] that
 * starts export via [EditorEvent.Export].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the navigation bar.
 */
@Composable
fun NavigationBar.Button.rememberExport(builder: NavigationBar.ButtonBuilder.() -> Unit = {}): Button<NavigationBar.ItemScope> =
    NavigationBar.Button.remember {
        id = { NavigationBar.Button.Id.export }
        visible = {
            val state by editorContext.state.collectAsState()
            state.viewMode !is EditorViewMode.Edit ||
                remember(this) {
                    editorContext.engine.editor.getSettingBoolean("features/pageCarouselEnabled") ||
                        editorContext.engine.scene.run { getPages().lastOrNull() == getCurrentPage() }
                }
        }
        vectorIcon = { IconPack.Export }
        onClick = {
            editorContext.eventHandler.send(EditorEvent.Export.Start())
        }
        contentDescription = { stringResource(R.string.ly_img_editor_navigation_bar_button_export) }
        builder()
    }

/**
 * The id of the navigation bar button returned by [NavigationBar.Button.rememberTogglePreviewMode].
 */
val NavigationBar.Button.Id.togglePreviewMode by unsafeLazy {
    EditorComponentId("ly.img.component.navigationBar.button.togglePreviewMode")
}

/**
 * A composable helper function that creates and remembers an [Button] that
 * updates editor view mode via [EditorEvent.SetViewMode]: when current view mode is [EditorViewMode.Edit], then
 * [EditorViewMode.Preview] is set and vice versa.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the navigation bar.
 */
@Composable
fun NavigationBar.Button.rememberTogglePreviewMode(builder: NavigationBar.ButtonBuilder.() -> Unit = {}): Button<NavigationBar.ItemScope> =
    NavigationBar.Button.remember {
        id = { NavigationBar.Button.Id.togglePreviewMode }
        visible = alwaysVisible
        decoration = {
            val state by editorContext.state.collectAsState()
            Box(
                modifier = Modifier
                    .padding(horizontal = 4.dp)
                    .size(40.dp)
                    .background(
                        color = if (state.viewMode is EditorViewMode.Preview) MaterialTheme.colorScheme.primary else Color.Transparent,
                        shape = CircleShape,
                    ),
            ) { it() }
        }
        vectorIcon = {
            val state by editorContext.state.collectAsState()
            when (state.viewMode) {
                is EditorViewMode.Preview -> IconPack.PreviewToggled
                else -> IconPack.Preview
            }
        }
        tint = {
            val state by editorContext.state.collectAsState()
            when (state.viewMode) {
                is EditorViewMode.Preview -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
        }
        enabled = alwaysEnabled
        onClick = {
            val viewMode = if (editorContext.state.value.viewMode is EditorViewMode.Preview) {
                EditorViewMode.Edit()
            } else {
                EditorViewMode.Preview()
            }
            editorContext.eventHandler.send(EditorEvent.SetViewMode(viewMode))
        }
        contentDescription = { stringResource(R.string.ly_img_editor_navigation_bar_button_toggle_preview_mode) }
        builder()
    }

/**
 * The id of the navigation bar button returned by [NavigationBar.Button.rememberTogglePagesMode].
 */
val NavigationBar.Button.Id.togglePagesMode by unsafeLazy {
    EditorComponentId("ly.img.component.navigationBar.button.togglePagesMode")
}

/**
 * A composable helper function that creates and remembers a custom [EditorComponent] that
 * updates editor view mode via [EditorEvent.SetViewMode]: when current view mode is [EditorViewMode.Edit], then
 * [EditorViewMode.Pages] is set and vice versa.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the navigation bar.
 */
@Composable
fun NavigationBar.Button.rememberTogglePagesMode(
    builder: EditorComponentBuilder<EditorComponent<EditorScope>, EditorScope>.() -> Unit = {},
): EditorComponent<EditorScope> = EditorComponent.remember {
    id = { NavigationBar.Button.Id.togglePagesMode }
    visible = {
        remember(this) {
            editorContext.engine.block.findByType(DesignBlockType.Stack).isNotEmpty()
        }
    }
    decoration = {
        val state by editorContext.state.collectAsState()
        val textString = remember(this) {
            editorContext.engine.scene.getPages().size.toString()
        }
        Box(
            modifier = Modifier
                .padding(4.dp)
                .height(40.dp)
                .background(
                    color = if (state.viewMode is EditorViewMode.Pages) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(size = 100.dp),
                ),
        ) {
            val tintColor = when (state.viewMode) {
                is EditorViewMode.Pages -> MaterialTheme.colorScheme.onPrimary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            }
            Button(
                modifier = Modifier.defaultMinSize(minWidth = 40.dp),
                colors = ButtonDefaults.textButtonColors(
                    containerColor = Color.Transparent,
                    contentColor = tintColor,
                    disabledContainerColor = Color.Transparent,
                    disabledContentColor = tintColor.copy(alpha = tintColor.alpha * 0.5F),
                ),
                onClick = {
                    val viewMode = if (editorContext.state.value.viewMode is EditorViewMode.Pages) {
                        EditorViewMode.Edit()
                    } else {
                        EditorViewMode.Pages()
                    }
                    editorContext.eventHandler.send(EditorEvent.SetViewMode(viewMode))
                },
                enabled = true,
                contentPadding = PaddingValues(horizontal = 8.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = IconPack.Pages,
                        contentDescription = stringResource(R.string.ly_img_editor_navigation_bar_button_toggle_pages_mode),
                    )
                    Text(
                        modifier = Modifier.padding(start = 4.dp),
                        text = textString,
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
    builder()
}

/**
 * Builder class for [rememberPreviousPage] and [rememberNextPage] buttons.
 */
open class NavigationBarPageButtonBuilder : NoContentEditorComponentBuilder<NavigationBar.ItemScope>() {
    /**
     * Scope of this component. Every new value will trigger recomposition of all [ScopedProperty]s
     * such as [visible], [enterTransition], [exitTransition] etc.
     * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for granular
     * recompositions over updating the scope, since scope change triggers full recomposition of the component.
     * Ideally, scope should be updated when the parent scope (scope of the parent component) is updated and when you want to
     * observe changes from the [Engine].
     * By default it is updated only when the parent scope (accessed via `this`) is updated.
     */
    override var scope: ScopedProperty<EditorScope, NavigationBar.ItemScope> = {
        remember(this) { NavigationBar.ItemScope(this) }
    }

    /**
     * Text of the button as string.
     * By default no text string is applied.
     */
    open var textString: ScopedProperty<NavigationBar.ItemScope, String>? = null

    /**
     * Icon of the button as image vector.
     * By default no image vector is applied.
     */
    open var vectorIcon: ScopedProperty<NavigationBar.ItemScope, ImageVector>? = null
}

/**
 * The id of the navigation bar button returned by [NavigationBar.Button.rememberPreviousPage].
 */
val NavigationBar.Button.Id.previousPage by unsafeLazy {
    EditorComponentId("ly.img.component.navigationBar.button.previousPage")
}

/**
 * A composable helper function that creates and remembers a custom [EditorComponent] that
 * navigates to the previous page via [EditorEvent.Navigation.ToPreviousPage].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the navigation bar.
 */
@Composable
fun NavigationBar.Button.rememberPreviousPage(
    builder: NavigationBarPageButtonBuilder.() -> Unit = {},
): EditorComponent<NavigationBar.ItemScope> = EditorComponent.remember(::NavigationBarPageButtonBuilder) {
    id = { NavigationBar.Button.Id.previousPage }
    visible = {
        val state by editorContext.state.collectAsState()
        state.viewMode is EditorViewMode.Edit &&
            remember(this) {
                editorContext.engine.editor.getSettingBoolean("features/pageCarouselEnabled") ||
                    editorContext.engine.scene.run { getPages().firstOrNull() != getCurrentPage() }
            }
    }
    textString = { stringResource(R.string.ly_img_editor_navigation_bar_button_previous) }
    vectorIcon = { IconPack.ArrowBack }
    decoration = {
        val editorScope = this
        val tintColor = MaterialTheme.colorScheme.onSurfaceVariant
        Button(
            colors = ButtonDefaults.textButtonColors(
                containerColor = Color.Transparent,
                contentColor = tintColor,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = tintColor.copy(alpha = tintColor.alpha * 0.5F),
            ),
            onClick = { editorContext.eventHandler.send(EditorEvent.Navigation.ToPreviousPage()) },
            enabled = true,
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            vectorIcon?.invoke(editorScope)?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                )
            }
            textString?.invoke(editorScope)?.let {
                Text(
                    modifier = Modifier.padding(start = 4.dp),
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
    builder()
}

/**
 * The id of the navigation bar button returned by [NavigationBar.Button.rememberNextPage].
 */
val NavigationBar.Button.Id.nextPage by unsafeLazy {
    EditorComponentId("ly.img.component.navigationBar.button.nextPage")
}

/**
 * A composable helper function that creates and remembers a custom [EditorComponent] that
 * navigates to the next page via [EditorEvent.Navigation.ToNextPage].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the navigation bar.
 */
@Composable
fun NavigationBar.Button.rememberNextPage(
    builder: NavigationBarPageButtonBuilder.() -> Unit = {},
): EditorComponent<NavigationBar.ItemScope> = EditorComponent.remember(::NavigationBarPageButtonBuilder) {
    id = { NavigationBar.Button.Id.nextPage }
    visible = {
        val state by editorContext.state.collectAsState()
        state.viewMode is EditorViewMode.Edit &&
            remember(this) {
                editorContext.engine.editor.getSettingBoolean("features/pageCarouselEnabled") ||
                    editorContext.engine.scene.run { getPages().lastOrNull() != getCurrentPage() }
            }
    }
    textString = { stringResource(R.string.ly_img_editor_navigation_bar_button_next) }
    vectorIcon = { IconPack.ArrowForward }
    decoration = {
        val editorScope = this
        val tintColor = MaterialTheme.colorScheme.onSurfaceVariant
        Button(
            colors = ButtonDefaults.textButtonColors(
                containerColor = Color.Transparent,
                contentColor = tintColor,
                disabledContainerColor = Color.Transparent,
                disabledContentColor = tintColor.copy(alpha = tintColor.alpha * 0.5F),
            ),
            onClick = { editorContext.eventHandler.send(EditorEvent.Navigation.ToNextPage()) },
            contentPadding = PaddingValues(horizontal = 12.dp),
        ) {
            textString?.invoke(editorScope)?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
            vectorIcon?.invoke(editorScope)?.let {
                Icon(
                    modifier = Modifier.padding(start = 4.dp),
                    imageVector = it,
                    contentDescription = null,
                )
            }
        }
    }
    builder()
}
