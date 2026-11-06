package ly.img.editor.plugin.voiceIsolation

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import ly.img.editor.core.EditorScope
import ly.img.editor.core.ScopedProperty
import ly.img.editor.core.component.EditorComponent
import ly.img.editor.core.component.HorizontalListBuilderModify
import ly.img.editor.core.component.InspectorBar
import ly.img.editor.core.component.modify
import ly.img.editor.core.component.remember
import ly.img.editor.core.configuration.EditorConfiguration
import ly.img.editor.core.configuration.EditorConfigurationBuilder

/**
 * Plugin for voice isolation. This plugin adds an "Isolate Voice" button to the [InspectorBar] of a selected video or
 * audio clip, which removes music and background noise from the clip's audio and keeps the voice. Call [isolateVoice]
 * to clean a clip from your own code, for example in `onCreate` or `onExport`.
 */
open class VoiceIsolationPlugin : EditorConfigurationBuilder() {
    /**
     * Voice isolation configuration object. Check [VoiceIsolationPlugin] inheritors for available options.
     */
    var config by editorContext.mutableStateOf<VoiceIsolationConfig?>(
        key = "ly.img.editor.plugin.voiceIsolation.config",
        initial = null,
    )

    /**
     * The [InspectorBar] modifier in order to place the [InspectorBar.Button.rememberVoiceIsolation] button.
     * By default, it is prepended to the inspector bar.
     */
    var inspectorBarModifier: HorizontalListBuilderModify<EditorComponent<*>>.(VoiceIsolationConfig) -> Unit = {
        addFirst { InspectorBar.Button.rememberVoiceIsolation(config = it) }
    }

    /**
     * Calls [VoiceIsolator.prewarm] of the configured isolator after the parent configuration is created.
     */
    override var onCreate: (suspend EditorScope.() -> Unit)? = {
        val config = requireConfig()
        parentConfiguration?.onCreate?.invoke(this)
        editorContext.coroutineScope.launch {
            try {
                config.isolator.prewarm(editorContext.activity.applicationContext)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(TAG, "Failed to prewarm the voice isolator.", exception)
            }
        }
    }

    /**
     * Returns the editor inspector bar with the voice isolation button inserted by [inspectorBarModifier].
     */
    override var inspectorBar: ScopedProperty<EditorScope, EditorComponent<*>?>? = {
        val config = requireConfig()
        val sourceInspectorBar = parentConfiguration?.inspectorBar as? InspectorBar ?: InspectorBar.remember()
        val updatedListBuilder = sourceInspectorBar.listBuilder.modify {
            inspectorBarModifier(config)
        }
        androidx.compose.runtime.remember(sourceInspectorBar, updatedListBuilder) {
            sourceInspectorBar.copy(listBuilder = updatedListBuilder)
        }
    }

    /**
     * Ensures the plugin is configured before the editor configuration is decorated.
     */
    override var decorator: @Composable (EditorConfiguration.() -> EditorConfiguration) = {
        requireConfig()
        this
    }

    private fun requireConfig() = requireNotNull(config) {
        "Configure \"config\" property inside the VoiceIsolationPlugin configuration block or use one of the implementations of the plugin."
    }
}

internal const val TAG = "VoiceIsolation"
