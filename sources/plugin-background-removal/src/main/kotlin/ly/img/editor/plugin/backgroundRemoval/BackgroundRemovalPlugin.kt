package ly.img.editor.plugin.backgroundRemoval

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import ly.img.editor.core.EditorScope
import ly.img.editor.core.ScopedProperty
import ly.img.editor.core.component.Dock
import ly.img.editor.core.component.EditorComponent
import ly.img.editor.core.component.HorizontalListBuilderModify
import ly.img.editor.core.component.InspectorBar
import ly.img.editor.core.component.modify
import ly.img.editor.core.component.remember
import ly.img.editor.core.configuration.EditorConfiguration
import ly.img.editor.core.configuration.EditorConfigurationBuilder

/**
 * Plugin for background removal. This plugin adds a button to the [Dock] that removes the background of the current
 * page's image. Add [InspectorBar.Button.rememberBackgroundRemoval] with [inspectorBarModifier] to remove the
 * background of the selected image or page. The results are PNG files in the app's cache directory, which Android can
 * clear. To keep them, upload the `file://` URIs that `engine.editor.findAllMediaURIs()` returns, and point the scene
 * to the uploads with `engine.editor.relocateResource`.
 */
open class BackgroundRemovalPlugin : EditorConfigurationBuilder() {
    /**
     * Background removal configuration object. Check [BackgroundRemovalPlugin] inheritors for
     * available options.
     */
    var config by editorContext.mutableStateOf<BackgroundRemovalConfig?>(
        key = "ly.img.editor.plugin.backgroundRemoval.config",
        initial = null,
    )

    /**
     * The [Dock] modifier in order to place the [Dock.Button.rememberBackgroundRemoval] button.
     * By default, it is prepended to the dock.
     */
    var dockModifier: HorizontalListBuilderModify<EditorComponent<*>>.(BackgroundRemovalConfig) -> Unit = {
        addFirst { Dock.Button.rememberBackgroundRemoval(config = it) }
    }

    /**
     * The [InspectorBar] modifier in order to place the [InspectorBar.Button.rememberBackgroundRemoval] button.
     * By default, it is `null`, and the plugin leaves the inspector bar unchanged. To prepend the button, set
     * `inspectorBarModifier = { addFirst { InspectorBar.Button.rememberBackgroundRemoval(config = it) } }`.
     */
    var inspectorBarModifier: (HorizontalListBuilderModify<EditorComponent<*>>.(BackgroundRemovalConfig) -> Unit)? = null

    /**
     * Initializes the configured background remover after the parent configuration is created.
     */
    override var onCreate: (suspend EditorScope.() -> Unit)? = {
        val config = requireConfig()
        parentConfiguration?.onCreate?.invoke(this)
        with(config.remover) { initialize() }
    }

    /**
     * Returns the editor dock with the background removal button inserted by [dockModifier].
     */
    override var dock: ScopedProperty<EditorScope, EditorComponent<*>?>? = {
        val config = requireConfig()
        val sourceDock = parentConfiguration?.dock as? Dock ?: Dock.remember()
        val updatedListBuilder = sourceDock.listBuilder.modify {
            dockModifier(config)
        }
        androidx.compose.runtime.remember(sourceDock, updatedListBuilder) {
            sourceDock.copy(listBuilder = updatedListBuilder)
        }
    }

    /**
     * Returns the editor inspector bar with the background removal button inserted by [inspectorBarModifier], or the
     * parent's inspector bar when [inspectorBarModifier] is `null`.
     */
    override var inspectorBar: ScopedProperty<EditorScope, EditorComponent<*>?>? = impl@{
        val modifier = inspectorBarModifier ?: return@impl parentConfiguration?.inspectorBar
        val config = requireConfig()
        val sourceInspectorBar = parentConfiguration?.inspectorBar as? InspectorBar ?: InspectorBar.remember()
        val updatedListBuilder = sourceInspectorBar.listBuilder.modify {
            modifier(config)
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
        "Configure \"config\" property inside the BackgroundRemovalPlugin configuration block or use one of the implementations of the plugin."
    }
}
