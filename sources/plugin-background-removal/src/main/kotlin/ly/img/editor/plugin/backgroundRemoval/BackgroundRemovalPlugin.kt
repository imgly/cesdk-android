package ly.img.editor.plugin.backgroundRemoval

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.Alignment
import ly.img.editor.core.EditorScope
import ly.img.editor.core.ScopedProperty
import ly.img.editor.core.component.Dock
import ly.img.editor.core.component.EditorComponent
import ly.img.editor.core.component.modify
import ly.img.editor.core.component.remember
import ly.img.editor.core.configuration.EditorConfigurationBuilder

/**
 * Plugin for background removal. This plugin adds a dedicated button to the [Dock] that removes
 * the background from the current page.
 */
class BackgroundRemovalPlugin : EditorConfigurationBuilder() {
    /**
     * The [Dock] modifier in order to place the [Dock.Button.rememberBackgroundRemoval] button.
     * By default, it is prepended to the dock.
     */
    var dockModifier: EditorComponent.ListBuilder.Modify<EditorComponent<*>, Alignment.Horizontal, Arrangement.Horizontal>.() -> Unit = {
        addFirst { Dock.Button.rememberBackgroundRemoval() }
    }

    override var dock: ScopedProperty<EditorScope, EditorComponent<*>?>? = {
        val sourceDock = parentConfiguration?.dock as? Dock ?: Dock.remember()
        val updatedListBuilder = sourceDock.listBuilder.modify {
            dockModifier()
        }
        androidx.compose.runtime.remember(sourceDock, updatedListBuilder) {
            sourceDock.copy(listBuilder = updatedListBuilder)
        }
    }
}
