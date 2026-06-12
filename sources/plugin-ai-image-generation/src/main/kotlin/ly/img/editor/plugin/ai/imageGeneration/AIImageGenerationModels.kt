package ly.img.editor.plugin.ai.imageGeneration

import ly.img.editor.plugin.ai.core.gateway.AIGatewayPromptStyle

internal data class AIImageGenerationState(
    val prompt: String = "",
    val selectedStyle: AIGatewayPromptStyle = AIGatewayPromptStyle.curated.first(),
    val selectedFormat: Format = Format.SQUARE_HD,
    val customWidth: String = "1024",
    val customHeight: String = "1024",
    val imageUri: String? = null,
    val isImageSelected: Boolean = false,
)

internal enum class Format(
    val label: String,
    val ratio: String,
    val width: Int,
    val height: Int,
) {
    SQUARE_HD(
        label = "Square HD",
        ratio = "1:1",
        width = 1024,
        height = 1024,
    ),
    SQUARE(
        label = "Square",
        ratio = "1:1",
        width = 512,
        height = 512,
    ),
    PORTRAIT_4_3(
        label = "Portrait",
        ratio = "3:4",
        width = 1024,
        height = 1365,
    ),
    PORTRAIT_16_9(
        label = "Portrait",
        ratio = "9:16",
        width = 1024,
        height = 1820,
    ),
    LANDSCAPE_4_3(
        label = "Landscape",
        ratio = "4:3",
        width = 1365,
        height = 1024,
    ),
    LANDSCAPE_16_9(
        label = "Landscape",
        ratio = "16:9",
        width = 1820,
        height = 1024,
    ),
    CUSTOM(
        label = "Custom",
        ratio = "",
        width = 0,
        height = 0,
    ),
}
