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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import ly.img.editor.core.EditorScope
import ly.img.editor.core.component.Button
import ly.img.editor.core.component.EditorComponentId
import ly.img.editor.core.component.InspectorBar
import ly.img.editor.core.component.data.Selection
import ly.img.editor.core.component.remember
import ly.img.editor.core.compose.rememberLastValue
import ly.img.editor.plugin.backgroundRemoval.api.BackgroundRemovalApi
import ly.img.editor.plugin.backgroundRemoval.api.BackgroundRemovalApi.startBackgroundRemoval
import ly.img.editor.plugin.backgroundRemoval.iconPack.BackgroundRemoval
import ly.img.editor.plugin.backgroundRemoval.iconPack.IconPack
import ly.img.engine.FillType

/**
 * A composable helper function that creates and remembers an [InspectorBar.Button] that removes the background of the
 * selected image or page. The button is hidden for stickers, and only visible when the block has an image fill that is
 * not SVG, and its scope allows a fill change.
 *
 * @param config configuration that selects the background removal backend.
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberBackgroundRemoval(
    config: BackgroundRemovalConfig,
    builder: InspectorBar.ButtonBuilder.() -> Unit = {},
): Button<InspectorBar.ItemScope> = InspectorBar.Button.remember {
    id = { EditorComponentId("ly.img.component.inspectorBar.button.backgroundRemoval") }
    scope = {
        (this as InspectorBar.Scope).run {
            // The selection stays the same when the image of the selected block changes, for example to an SVG, so the
            // events of the block and of its fill update the button.
            val isRemovable by remember(this) {
                val block = editorContext.safeSelection?.designBlock
                editorContext.engine.event.subscribe()
                    .filter { events ->
                        val fill = block?.let {
                            runCatching {
                                editorContext.engine.block.run { if (supportsFill(it)) getFill(it) else null }
                            }.getOrNull()
                        }
                        events.any { it.block == block || it.block == fill }
                    }
                    .map { canRemoveBackground(editorContext.safeSelection) }
                    .onStart { emit(canRemoveBackground(editorContext.safeSelection)) }
                    .distinctUntilChanged()
            }.collectAsState(initial = remember(this) { canRemoveBackground(editorContext.safeSelection) })
            rememberLastValue(this, isRemovable) {
                if (editorContext.safeSelection == null) lastValue else InspectorBar.ItemScope(parentScope = this@run)
            }
        }
    }
    visible = {
        remember(this) { canRemoveBackground(editorContext.safeSelection) }
    }
    icon = {
        Icon(
            imageVector = IconPack.BackgroundRemoval,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
    }
    text = {
        Text(
            stringResource(R.string.ly_img_plugin_background_removal_inspector_bar_button),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
    onClick = {
        startBackgroundRemoval(block = editorContext.safeSelection?.designBlock, config = config)
    }
    builder()
}

private fun EditorScope.canRemoveBackground(selection: Selection?): Boolean = selection != null &&
    selection.kind != KIND_STICKER &&
    selection.fillType == FillType.Image &&
    BackgroundRemovalApi.canProcess(editorContext.engine, selection.designBlock)

private const val KIND_STICKER = "sticker"
