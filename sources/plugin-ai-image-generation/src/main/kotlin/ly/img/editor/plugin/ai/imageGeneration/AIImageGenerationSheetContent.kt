package ly.img.editor.plugin.ai.imageGeneration

import android.content.res.Configuration
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import ly.img.editor.plugin.ai.core.gateway.AIGatewayApi
import ly.img.editor.plugin.ai.core.gateway.AIGatewayConfig
import ly.img.editor.plugin.ai.imageGeneration.component.AIImageGenerationContent
import ly.img.editor.plugin.ai.imageGeneration.component.FormatPickerDialog
import ly.img.editor.plugin.ai.imageGeneration.component.FullScreenStyleDialog
import ly.img.editor.plugin.ai.imageGeneration.preview.PreviewTheme
import ly.img.editor.plugin.ai.imageGeneration.util.Constants

@Composable
internal fun AIImageGenerationSheetContent(
    aiGatewayConfig: AIGatewayConfig,
    initialImageUri: String? = null,
    handleImageGeneration: (AIImageGenerationState, suspend () -> List<String>) -> Unit = { _, _ -> },
    onCloseSheet: () -> Unit = {},
) {
    var state by remember { mutableStateOf(AIImageGenerationState(imageUri = initialImageUri)) }
    var showFormatDialog by remember { mutableStateOf(false) }
    var showStyleDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val aiGatewayApi = remember(aiGatewayConfig) {
        AIGatewayApi(aiGatewayConfig)
    }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { mediaUri ->
        mediaUri?.let { uri ->
            state = state.copy(
                imageUri = uri.toString(),
                isImageSelected = true,
            )
        }
    }

    BackHandler {
        onCloseSheet()
    }

    AIImageGenerationContent(
        state = state,
        onPromptChange = { state = state.copy(prompt = it) },
        onAddImageClick = {
            imageLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        },
        onRemoveImageClick = {
            state = state.copy(
                imageUri = null,
                isImageSelected = false,
            )
        },
        onMakeItClick = {
            if (state.prompt.isNotBlank()) {
                handleImageGeneration(state) {
                    aiGatewayApi.generateImage(
                        prompt = state.prompt,
                        style = state.selectedStyle,
                        imageSize = state.toImageSize(),
                        inputImageUri = state.imageUri.orEmpty(),
                        context = context,
                    )
                }
                true
            } else {
                Log.w(Constants.TAG, "Prompt is empty, cannot generate image")
                false
            }
        },
        onStyleClick = { showStyleDialog = true },
        onAspectRatioClick = { showFormatDialog = true },
        onCloseSheet = onCloseSheet,
    )

    if (showFormatDialog) {
        FormatPickerDialog(
            selectedFormat = state.selectedFormat,
            customWidth = state.customWidth,
            customHeight = state.customHeight,
            onFormatSelected = { format ->
                state = state.copy(selectedFormat = format)
                if (format != Format.CUSTOM) {
                    showFormatDialog = false
                }
            },
            onCustomWidthChange = { width ->
                state = state.copy(customWidth = width)
            },
            onCustomHeightChange = { height ->
                state = state.copy(customHeight = height)
            },
            onDismiss = { showFormatDialog = false },
        )
    }

    if (showStyleDialog) {
        FullScreenStyleDialog(
            selectedStyle = state.selectedStyle,
            onStyleSelected = { style ->
                state = state.copy(selectedStyle = style)
                showStyleDialog = false
            },
            onDismiss = { showStyleDialog = false },
        )
    }
}

/**
 * Maps the current [Format] (or the custom width/height pair) to the
 * `{width, height}` map the gateway expects for the `format` field.
 * For image-to-image we send `null` — the gateway picks dimensions
 * from the input image.
 */
private fun AIImageGenerationState.toImageSize(): Map<String, Int>? {
    if (imageUri != null) return null
    if (selectedFormat == Format.CUSTOM) {
        return mapOf(
            "width" to (customWidth.toIntOrNull() ?: Format.SQUARE_HD.width),
            "height" to (customHeight.toIntOrNull() ?: Format.SQUARE_HD.height),
        )
    }
    return mapOf(
        "width" to selectedFormat.width,
        "height" to selectedFormat.height,
    )
}

@Composable
@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
internal fun AIImageGenerationSheetContentPreview() {
    PreviewTheme {
        Surface {
            AIImageGenerationSheetContent(
                aiGatewayConfig = remember { AIGatewayConfig(apiKey = "") },
                onCloseSheet = {},
            )
        }
    }
}
