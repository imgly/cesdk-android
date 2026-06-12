package ly.img.editor.plugin.ai.imageGeneration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import ly.img.editor.plugin.ai.core.gateway.AIGatewayConfig

/**
 * Plugin for image generation using AI.
 * This plugin adds dedicated buttons both to the [Dock] and the [InspectorBar].
 */
class AIImageGenerationPlugin : EditorConfigurationBuilder() {
    /**
     * The gateway config to IMG.LY AI features.
     */
    var aiGatewayConfig by editorContext.mutableStateOf<AIGatewayConfig?>(
        key = "ly.img.editor.plugin.ai.aiGatewayConfig",
        initial = null,
    )

    /**
     * The [Dock] modifier in order to place the [Dock.Button.rememberAIImageGeneration] button.
     * By default, it is prepended to the dock.
     */
    var dockModifier: HorizontalListBuilderModify<EditorComponent<*>>.(AIGatewayConfig) -> Unit = {
        addFirst { Dock.Button.rememberAIImageGeneration(aiGatewayConfig = it) }
    }

    /**
     * The [InspectorBar] modifier in order to place the [InspectorBar.Button.rememberAIImageGeneration] button.
     * By default, it is prepended to the inspector bar.
     */
    var inspectorBarModifier: HorizontalListBuilderModify<EditorComponent<*>>.(AIGatewayConfig) -> Unit = {
        addFirst { InspectorBar.Button.rememberAIImageGeneration(aiGatewayConfig = it) }
    }

    override var dock: ScopedProperty<EditorScope, EditorComponent<*>?>? = impl@{
        val aiGatewayConfig = requireAIGatewayConfig()
        val sourceDock = parentConfiguration?.dock as? Dock ?: Dock.remember()
        val updatedListBuilder = sourceDock.listBuilder.modify { dockModifier(aiGatewayConfig) }
        remember(sourceDock, updatedListBuilder) {
            sourceDock.copy(listBuilder = updatedListBuilder)
        }
    }

    override var inspectorBar: ScopedProperty<EditorScope, EditorComponent<*>?>? = impl@{
        val aiGatewayConfig = requireAIGatewayConfig()
        val sourceInspectorBar = parentConfiguration?.inspectorBar as? InspectorBar ?: InspectorBar.remember()
        val updatedListBuilder = sourceInspectorBar.listBuilder.modify { inspectorBarModifier(aiGatewayConfig) }
        remember(sourceInspectorBar, updatedListBuilder) {
            sourceInspectorBar.copy(listBuilder = updatedListBuilder)
        }
    }

    override var decorator: @Composable (EditorConfiguration.() -> EditorConfiguration) = {
        requireAIGatewayConfig()
        this
    }

    private fun requireAIGatewayConfig() = requireNotNull(aiGatewayConfig) {
        "Configure \"aiGatewayConfig\" property inside the AIImageGenerationPlugin configuration block."
    }
}
