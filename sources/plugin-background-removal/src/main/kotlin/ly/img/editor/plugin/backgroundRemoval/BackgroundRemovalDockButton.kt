@file:Suppress("UnusedReceiverParameter")

package ly.img.editor.plugin.backgroundRemoval

import android.util.Log
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import ly.img.editor.core.component.Button
import ly.img.editor.core.component.Dock
import ly.img.editor.core.component.EditorComponentId
import ly.img.editor.core.component.remember
import ly.img.editor.plugin.backgroundRemoval.api.BackgroundRemovalApi.removeBackground
import ly.img.editor.plugin.backgroundRemoval.iconPack.BackgroundRemoval
import ly.img.editor.plugin.backgroundRemoval.iconPack.IconPack
import ly.img.editor.plugin.backgroundRemoval.util.Constants

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * removes background from the current page.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Dock.Button.rememberBackgroundRemoval(builder: Dock.ButtonBuilder.() -> Unit = {}): Button<Dock.ItemScope> {
    val scope = rememberCoroutineScope()
    var currentJob: Job? by androidx.compose.runtime.remember { mutableStateOf(null) }
    return Dock.Button.remember {
        id = { EditorComponentId("ly.img.component.dock.button.backgroundRemoval") }
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
            currentJob?.cancel()
            currentJob = scope.launch {
                try {
                    val currentPage = requireNotNull(editorContext.engine.scene.getCurrentPage())
                    removeBackground(currentPage)
                } catch (e: Exception) {
                    Log.e(Constants.TAG, "Failed to remove background: ${e.message}", e)
                } finally {
                    currentJob = null
                }
            }
        }
        builder()
    }
}
