@file:Suppress("UnusedReceiverParameter")

package ly.img.editor.plugin.voiceIsolation

import android.util.Log
import android.widget.Toast
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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import ly.img.editor.core.EditorScope
import ly.img.editor.core.component.Button
import ly.img.editor.core.component.EditorComponentId
import ly.img.editor.core.component.InspectorBar
import ly.img.editor.core.component.data.Selection
import ly.img.editor.core.component.remember
import ly.img.editor.core.compose.rememberLastValue
import ly.img.editor.plugin.voiceIsolation.iconpack.IconPack
import ly.img.editor.plugin.voiceIsolation.iconpack.VoiceIsolation
import ly.img.engine.DesignBlock

/**
 * A composable helper function that creates and remembers an [InspectorBar.Button] that isolates the voice of the
 * selected video or audio clip with [isolateVoice]. The button is visible when [canIsolateVoice] is true for the
 * selected block, and stays visible after the clip is cleaned. A tap on a clip that is being cleaned does nothing.
 *
 * Unlike a call of [isolateVoice], the button shows the loading state of the clip, cleans one clip at a time, so taps on
 * other clips wait, and makes the change one undo step.
 *
 * @param config configuration that selects the [VoiceIsolator].
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberVoiceIsolation(
    config: VoiceIsolationConfig,
    builder: InspectorBar.ButtonBuilder.() -> Unit = {},
): Button<InspectorBar.ItemScope> = InspectorBar.Button.remember {
    id = { EditorComponentId("ly.img.component.inspectorBar.button.voiceIsolation") }
    scope = {
        (this as InspectorBar.Scope).run {
            // The selection stays the same when the clip gets another file or its video loads, so the events of the
            // block and of its fill update the button.
            val canIsolate by remember(this) {
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
                    .map { canIsolateVoice(editorContext.safeSelection) }
                    .onStart { emit(canIsolateVoice(editorContext.safeSelection)) }
                    .distinctUntilChanged()
            }.collectAsState(initial = remember(this) { canIsolateVoice(editorContext.safeSelection) })
            rememberLastValue(this, canIsolate) {
                if (editorContext.safeSelection == null) lastValue else InspectorBar.ItemScope(parentScope = this@run)
            }
        }
    }
    visible = {
        remember(this) { canIsolateVoice(editorContext.safeSelection) }
    }
    icon = {
        Icon(
            imageVector = IconPack.VoiceIsolation,
            contentDescription = null,
            modifier = Modifier.size(24.dp),
        )
    }
    text = {
        Text(
            stringResource(R.string.ly_img_plugin_voice_isolation_inspector_bar_button),
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(top = 4.dp),
        )
    }
    onClick = {
        editorContext.safeSelection?.designBlock?.let { startVoiceIsolation(block = it, config = config) }
    }
    builder()
}

private fun EditorScope.canIsolateVoice(selection: Selection?): Boolean =
    selection != null && canIsolateVoice(block = selection.designBlock, engine = editorContext.engine)

/**
 * Cleans [block] in the editor's coroutine scope, so that a run outlives the button that started it, and adds one undo
 * step. A tap on a block that is being cleaned does nothing. An error shows a toast, except a change of the clip, which
 * the user made.
 */
private fun EditorScope.startVoiceIsolation(
    block: DesignBlock,
    config: VoiceIsolationConfig,
) {
    if (isIsolatingVoice(block)) return
    // Undispatched, so that the block counts as being cleaned before the next tap.
    editorContext.coroutineScope.launch(start = CoroutineStart.UNDISPATCHED) {
        try {
            isolateVoiceFromButton(block = block, config = config)
        } catch (exception: CancellationException) {
            throw exception
        } catch (exception: VoiceIsolationException.BlockChanged) {
            Log.i(TAG, "The clip changed while its voice was being isolated.")
        } catch (exception: Exception) {
            Log.e(TAG, "Failed to isolate the voice.", exception)
            val message = when (exception) {
                is VoiceIsolationException.NoAudio -> R.string.ly_img_plugin_voice_isolation_error_no_audio
                is VoiceIsolationException.ModelDownloadFailed -> R.string.ly_img_plugin_voice_isolation_error_model_download
                else -> R.string.ly_img_plugin_voice_isolation_error_failed
            }
            Toast.makeText(editorContext.activity, message, Toast.LENGTH_LONG).show()
        }
    }
}
