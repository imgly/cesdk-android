@file:Suppress("UnusedReceiverParameter")

package ly.img.editor.plugin.backgroundRemoval

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import ly.img.editor.core.EditorScope
import ly.img.editor.core.component.Button
import ly.img.editor.core.component.Dock
import ly.img.editor.core.component.EditorComponentId
import ly.img.editor.core.component.remember
import ly.img.editor.plugin.backgroundRemoval.api.BackgroundRemovalApi
import ly.img.editor.plugin.backgroundRemoval.api.BackgroundRemovalApi.startBackgroundRemoval
import ly.img.editor.plugin.backgroundRemoval.iconPack.BackgroundRemoval
import ly.img.editor.plugin.backgroundRemoval.iconPack.IconPack

/**
 * A composable helper function that creates and remembers a [Dock.Button] that removes the background of the image of
 * the current page. The button is only visible when the page has an image fill that is not SVG, and its scope allows a
 * fill change.
 *
 * @param config configuration that selects the background removal backend.
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Dock.Button.rememberBackgroundRemoval(
    config: BackgroundRemovalConfig,
    builder: Dock.ButtonBuilder.() -> Unit = {},
): Button<Dock.ItemScope> = Dock.Button.remember {
    id = { EditorComponentId("ly.img.component.dock.button.backgroundRemoval") }
    scope = {
        // The dock does not update when the current page or its image changes, so engine events update the button.
        val canRemoveBackground by remember(this) {
            editorContext.engine.event.subscribe()
                .map { canRemoveCurrentPageBackground() }
                .onStart { emit(canRemoveCurrentPageBackground()) }
                .distinctUntilChanged()
        }.collectAsState(initial = remember { canRemoveCurrentPageBackground() })
        remember(this, canRemoveBackground) {
            Dock.ItemScope(parentScope = this)
        }
    }
    visible = {
        remember(this) { canRemoveCurrentPageBackground() }
    }
    icon = {
        Icon(
            imageVector = IconPack.BackgroundRemoval,
            contentDescription = stringResource(R.string.ly_img_plugin_background_removal_dock_button),
            modifier = Modifier.size(24.dp),
        )
    }
    text = {
        Text(
            stringResource(R.string.ly_img_plugin_background_removal_dock_button),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
    onClick = {
        startBackgroundRemoval(block = runCatching { editorContext.engine.scene.getCurrentPage() }.getOrNull(), config = config)
    }
    builder()
}

private fun EditorScope.canRemoveCurrentPageBackground(): Boolean {
    val engine = editorContext.engine
    val page = runCatching { engine.scene.getCurrentPage() }.getOrNull() ?: return false
    return BackgroundRemovalApi.canProcess(engine, page)
}
