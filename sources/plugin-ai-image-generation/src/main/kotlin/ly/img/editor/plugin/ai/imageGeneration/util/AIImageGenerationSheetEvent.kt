package ly.img.editor.plugin.ai.imageGeneration.util

import android.util.Log
import android.widget.Toast
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ly.img.editor.core.EditorScope
import ly.img.editor.core.component.data.Height
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.sheet.SheetStyle
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.plugin.ai.core.gateway.AIGatewayConfig
import ly.img.editor.plugin.ai.imageGeneration.AIImageGenerationSheetContent
import ly.img.editor.plugin.ai.imageGeneration.Format
import ly.img.editor.plugin.ai.imageGeneration.R
import ly.img.engine.BlockState
import ly.img.engine.DesignBlock

internal fun createTextToImageSheetOpenEvent(
    aiGatewayConfig: AIGatewayConfig,
    initialImageUri: String? = null,
    targetBlock: DesignBlock? = null,
    coroutineScope: CoroutineScope,
): EditorEvent.Sheet.Open = EditorEvent.Sheet.Open(
    SheetType.Custom(
        style = SheetStyle(
            isFloating = true,
            maxHeight = Height.Fraction(.8f),
        ),
        content = {
            CreateAIImageGenerationSheetContent(
                aiGatewayConfig = aiGatewayConfig,
                initialImageUri = initialImageUri,
                targetBlock = targetBlock,
                coroutineScope = coroutineScope,
            )
        },
    ),
)

@Composable
private fun EditorScope.CreateAIImageGenerationSheetContent(
    aiGatewayConfig: AIGatewayConfig,
    initialImageUri: String?,
    targetBlock: DesignBlock?,
    coroutineScope: CoroutineScope,
) {
    AIImageGenerationSheetContent(
        aiGatewayConfig = aiGatewayConfig,
        initialImageUri = initialImageUri,
        handleImageGeneration = { state, generateImages ->
            coroutineScope.launch {
                val block = targetBlock?.apply {
                    editorContext.engine.block.setState(
                        block = this,
                        state = BlockState.Pending(0f),
                    )
                } ?: if (state.imageUri != null) {
                    val imageSize = AIImageGenerationUtils.getImageSize(
                        context = editorContext.activity,
                        imageUri = state.imageUri,
                    )
                    editorContext.engine.createPendingBlock(
                        format = Format.CUSTOM,
                        customWidth = imageSize?.width?.toString() ?: state.customWidth,
                        customHeight = imageSize?.height?.toString() ?: state.customHeight,
                    )
                } else {
                    editorContext.engine.createPendingBlock(
                        format = state.selectedFormat,
                        customWidth = state.customWidth,
                        customHeight = state.customHeight,
                    )
                }
                editorContext.eventHandler.send(
                    EditorEvent.Sheet.Close(animate = true),
                )
                try {
                    val images = generateImages()
                    val imageUri = images.first()
                    editorContext.engine.editor.addUndoStep()
                    editorContext.engine.addImageToBlock(block = block, imageUri = imageUri)
                } catch (e: Exception) {
                    if (e !is CancellationException && editorContext.engine.isEngineRunning()) {
                        Log.e(Constants.TAG, "generateImageError", e)
                        Toast.makeText(
                            editorContext.activity,
                            editorContext.activity.getString(
                                R.string.ly_img_plugin_ai_image_generation_text_error_failed_to_generate_image,
                            ),
                            Toast.LENGTH_SHORT,
                        ).show()
                        editorContext.engine.block.setState(
                            block = block,
                            state = BlockState.Error(BlockState.Error.Type.UNKNOWN),
                        )
                    }
                }
            }
        },
        onCloseSheet = {
            editorContext.eventHandler.send(
                EditorEvent.Sheet.Close(animate = true),
            )
        },
    )
}
