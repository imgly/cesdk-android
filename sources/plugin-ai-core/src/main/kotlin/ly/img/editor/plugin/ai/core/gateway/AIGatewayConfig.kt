package ly.img.editor.plugin.ai.core.gateway

import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * IMG.LY Gateway client config.
 *
 * Speaks to `https://gateway.img.ly` directly via OkHttp. Handles both
 * text-to-image and image-to-image by switching the model id based on
 * whether the request carries a source image.
 *
 * Authentication: the client sends the configured API key on every
 * request as `Authorization: Bearer <key>`.
 *
 * The chosen [AIGatewayImageModel] owns its `/v1/responses` body shape via
 * [AIGatewayImageModel.buildInput] — adding a new model is a localized,
 * additive change.
 */
class AIGatewayConfig(
    val apiKey: String,
    val model: AIGatewayImageModel = AIGatewayImageModel.FluxV2,
    val gatewayUrl: String = "https://gateway.img.ly",
    val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .build(),
)
