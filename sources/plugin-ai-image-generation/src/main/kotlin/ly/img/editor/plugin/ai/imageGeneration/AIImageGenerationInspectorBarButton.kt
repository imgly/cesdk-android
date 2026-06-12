@file:Suppress("UnusedReceiverParameter")

package ly.img.editor.plugin.ai.imageGeneration

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ly.img.editor.core.component.Button
import ly.img.editor.core.component.EditorComponentId
import ly.img.editor.core.component.InspectorBar
import ly.img.editor.core.component.data.Selection
import ly.img.editor.core.component.remember
import ly.img.editor.core.compose.rememberLastValue
import ly.img.editor.plugin.ai.core.gateway.AIGatewayConfig
import ly.img.editor.plugin.ai.imageGeneration.iconPack.CreateWithAI
import ly.img.editor.plugin.ai.imageGeneration.iconPack.IconPack
import ly.img.editor.plugin.ai.imageGeneration.util.createTextToImageSheetOpenEvent
import ly.img.engine.FillType

/**
 * A composable helper function that creates and remembers an [InspectorBar.Button] that
 * opens a sheet with prompt input to generate an image and replace currently selected image block.
 *
 * @param aiGatewayConfig the gateway config to IMG.LY AI features.
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.rememberAIImageGeneration(
    aiGatewayConfig: AIGatewayConfig,
    builder: InspectorBar.ButtonBuilder.() -> Unit = {},
): Button<InspectorBar.ItemScope> {
    val coroutineScope = rememberCoroutineScope()
    return InspectorBar.Button.remember {
        id = { EditorComponentId("ly.img.component.inspectorBar.button.aiImageGeneration") }
        scope = {
            (this as InspectorBar.Scope).run {
                rememberLastValue(this) {
                    if (editorContext.safeSelection == null) lastValue else InspectorBar.ItemScope(parentScope = this@run)
                }
            }
        }
        visible = {
            remember(this) {
                editorContext.selection.isNotAnyKindOfSticker() &&
                    editorContext.selection.fillType == FillType.Image
            }
        }
        icon = {
            Icon(
                imageVector = IconPack.CreateWithAI,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
            )
        }
        text = {
            Text(
                stringResource(R.string.ly_img_plugin_ai_image_generation_button_edit),
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
        onClick = {
            editorContext.safeSelection?.designBlock?.let { block ->
                val initialImageUri = editorContext.engine.block.getString(
                    editorContext.engine.block.getFill(block),
                    "fill/image/imageFileURI",
                )

                editorContext.eventHandler.send(
                    event = createTextToImageSheetOpenEvent(
                        aiGatewayConfig = aiGatewayConfig,
                        initialImageUri = initialImageUri,
                        targetBlock = block,
                        coroutineScope = coroutineScope,
                    ),
                )
            }
        }
        builder()
    }
}

private fun Selection.isAnyKindOfSticker(): Boolean = this.kind == KIND_STICKER || this.kind == KIND_ANIMATED_STICKER

private fun Selection.isNotAnyKindOfSticker() = !this.isAnyKindOfSticker()

private const val KIND_STICKER = "sticker"
private const val KIND_ANIMATED_STICKER = "animatedSticker"
