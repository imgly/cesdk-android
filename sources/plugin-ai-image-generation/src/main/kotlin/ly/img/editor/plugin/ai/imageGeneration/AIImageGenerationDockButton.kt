@file:Suppress("UnusedReceiverParameter")

package ly.img.editor.plugin.ai.imageGeneration

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ly.img.editor.core.component.Button
import ly.img.editor.core.component.Dock
import ly.img.editor.core.component.EditorComponentId
import ly.img.editor.core.component.remember
import ly.img.editor.plugin.ai.core.gateway.AIGatewayConfig
import ly.img.editor.plugin.ai.imageGeneration.iconPack.CreateWithAI
import ly.img.editor.plugin.ai.imageGeneration.iconPack.IconPack
import ly.img.editor.plugin.ai.imageGeneration.util.createTextToImageSheetOpenEvent

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens a sheet with prompt input to generate an image.
 *
 * @param aiGatewayConfig the gateway config to IMG.LY AI features.
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Dock.Button.rememberAIImageGeneration(
    aiGatewayConfig: AIGatewayConfig,
    builder: Dock.ButtonBuilder.() -> Unit = {},
): Button<Dock.ItemScope> {
    val scope = rememberCoroutineScope()
    return Dock.Button.remember {
        id = { EditorComponentId("ly.img.component.dock.button.aiImageGeneration") }
        icon = {
            Icon(
                imageVector = IconPack.CreateWithAI,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
        text = {
            Text(
                stringResource(R.string.ly_img_plugin_ai_image_generation_button_generate),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        onClick = {
            editorContext.eventHandler.send(
                event = createTextToImageSheetOpenEvent(
                    aiGatewayConfig = aiGatewayConfig,
                    coroutineScope = scope,
                ),
            )
        }
        builder()
    }
}
