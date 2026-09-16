package ly.img.editor.plugin.ai.core.gateway

import org.json.JSONArray
import org.json.JSONObject

/**
 * IMG.LY Gateway image model family.
 *
 * The showcase ships with [FluxV2] and [GptImage2] as worked examples —
 * the same structure extends to any other model the gateway supports.
 * Each case owns:
 * 1. The pair of model IDs (text2image + image2image).
 * 2. The per-model request-body builder ([buildInput]) — request shapes
 *    differ across models (see `GET /v1/models/schema?model=…`).
 * 3. (Implicitly) any model-specific extras the builder sends.
 *
 * To add a new family:
 *   1. Confirm the model IDs are in your API key's scopes
 *      (manage keys in the IMG.LY Dashboard: https://img.ly/dashboard).
 *   2. Add a case below.
 *   3. Extend [textToImageId]/[imageToImageId] and override [buildInput]
 *      to match the model's `/v1/models/schema` shape.
 *   4. Mind the schema: some models (e.g. `google/nano-banana-pro`) only
 *      accept ratio strings for `format`, others (`bfl/flux-2`,
 *      `openai/gpt-image-2`) accept `{width, height}`. If your model only
 *      takes ratio strings, you also need to pick the closest ratio to
 *      the user's dimensions when building the input.
 *
 * Discover what models a key has access to:
 *
 * ```
 * curl -H "Authorization: Bearer sk_live_…" \
 *   "https://gateway.img.ly/v1/models?groupBy=capability"
 * ```
 */
sealed class AIGatewayImageModel {
    abstract val textToImageId: String
    abstract val imageToImageId: String

    /**
     * Build the `input` payload for `POST /v1/responses` for this model.
     *
     * - [prompt]: the final prompt to send (already augmented with any
     *   selected [AIGatewayPromptStyle.promptSnippet] by the service).
     * - [size]: optional `{ "width": Int, "height": Int }` map. Models
     *   whose schema accepts CustomSize will receive this verbatim; for
     *   ratio-only models, override this to convert.
     * - [imageUrls]: non-null/non-empty when an input image is present
     *   (image-to-image). The service has already uploaded local bytes
     *   via `/v1/uploads` and passes the resulting `asset_url` here.
     */
    abstract fun buildInput(
        prompt: String,
        size: Map<String, Int>?,
        imageUrls: List<String>?,
    ): JSONObject

    /**
     * FLUX.2 by Black Forest Labs.
     * Schema: `prompt`, `image_urls`, `format` (`anyOf<enum|CustomSize>`).
     * - text-to-image: `bfl/flux-2`
     * - image-to-image: `bfl/flux-2-edit`
     */
    data object FluxV2 : AIGatewayImageModel() {
        override val textToImageId: String = "bfl/flux-2"
        override val imageToImageId: String = "bfl/flux-2-edit"

        override fun buildInput(
            prompt: String,
            size: Map<String, Int>?,
            imageUrls: List<String>?,
        ): JSONObject = JSONObject().apply {
            put("prompt", prompt)
            if (!imageUrls.isNullOrEmpty()) {
                put("image_urls", JSONArray(imageUrls))
            }
            // flux-2 accepts `format` as either a ratio enum or
            // `{width, height}`. We send dimensions so any custom size
            // the user picked is honoured.
            if (size != null) {
                put("format", dimensionFormat(size))
            }
        }
    }

    /**
     * GPT Image 2 by OpenAI.
     * Schema: `prompt`, `image_urls`, `format` (`anyOf<enum|CustomSize>`),
     * `quality` (`low`/`medium`/`high`, gateway picks the default). To
     * expose `quality` in the UI, add it to the body here.
     * - text-to-image: `openai/gpt-image-2`
     * - image-to-image: `openai/gpt-image-2-edit`
     */
    data object GptImage2 : AIGatewayImageModel() {
        override val textToImageId: String = "openai/gpt-image-2"
        override val imageToImageId: String = "openai/gpt-image-2-edit"

        override fun buildInput(
            prompt: String,
            size: Map<String, Int>?,
            imageUrls: List<String>?,
        ): JSONObject = JSONObject().apply {
            put("prompt", prompt)
            if (!imageUrls.isNullOrEmpty()) {
                put("image_urls", JSONArray(imageUrls))
            }
            // Same `format` shape as flux-2 — either a ratio enum or
            // `{width, height}`. Custom dimensions are honoured.
            if (size != null) {
                put("format", dimensionFormat(size))
            }
        }
    }

    companion object {
        /**
         * Clamp the `{width, height}` to the gateway's 1–2048 per-dimension
         * limit and return as a `JSONObject` suitable for use as the
         * `format` field on models that accept CustomSize.
         */
        private fun dimensionFormat(size: Map<String, Int>): JSONObject = JSONObject().apply {
            put("width", (size["width"] ?: 1).coerceIn(MIN_DIMENSION, MAX_DIMENSION))
            put("height", (size["height"] ?: 1).coerceIn(MIN_DIMENSION, MAX_DIMENSION))
        }

        private const val MIN_DIMENSION = 1
        private const val MAX_DIMENSION = 2048
    }
}
