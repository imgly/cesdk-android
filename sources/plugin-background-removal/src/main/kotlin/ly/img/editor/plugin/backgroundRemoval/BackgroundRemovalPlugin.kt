package ly.img.editor.plugin.backgroundRemoval

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import ly.img.editor.core.EditorScope
import ly.img.editor.core.ScopedProperty
import ly.img.editor.core.component.Dock
import ly.img.editor.core.component.EditorComponent
import ly.img.editor.core.component.HorizontalListBuilderModify
import ly.img.editor.core.component.modify
import ly.img.editor.core.component.remember
import ly.img.editor.core.configuration.EditorConfiguration
import ly.img.editor.core.configuration.EditorConfigurationBuilder

/**
 * Plugin for background removal. This plugin adds a dedicated button to the [Dock] that removes
 * the background from the current page.
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
