package ly.img.editor.plugin.ai.core.gateway

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.job
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import kotlin.math.roundToInt

/**
 * IMG.LY AI Gateway client.
 *
 * Speaks to `https://gateway.img.ly` directly via OkHttp. Handles both
 * text-to-image and image-to-image by switching the model id based on
 * whether the request carries a source image.
 *
 * Authentication: the client sends the configured API key on every
 * request as `Authorization: Bearer <key>`.
 *
 * Style support is **prompt-engineered**: if a [AIGatewayPromptStyle] is provided,
 * the client appends its `promptSnippet` to the user's prompt before
 * calling the model. No gateway model exposes a native `style` parameter,
 * so this is the cross-model way to steer aesthetics.
 *
 * The chosen [AIGatewayImageModel] owns its `/v1/responses` body shape via
 * [AIGatewayImageModel.buildInput] — adding a new model is a localized,
 * additive change.
 */
class AIGatewayApi(
    val config: AIGatewayConfig,
) {
    /**
     * Generate an image. Uses the text-to-image model when [inputImageUri]
     * is empty; otherwise uses the image-to-image model and uploads the
     * source image first if it's a local URI.
     *
     * Returns a list of generated image URLs (currently always size 1,
     * matching the gateway's single-output response shape).
     */
    suspend fun generateImage(
        prompt: String,
        style: AIGatewayPromptStyle? = null,
        imageSize: Map<String, Int>? = null,
        inputImageUri: String = "",
        context: Context,
    ): List<String> = withContext(Dispatchers.IO) {
        val isImageToImage = inputImageUri.isNotEmpty()
        val modelId =
            if (isImageToImage) config.model.imageToImageId else config.model.textToImageId

        // Append the selected style's snippet to the user's prompt. The
        // gateway sees only one combined `prompt` field — model-agnostic
        // style steering. Semicolon (not comma) keeps the snippet from
        // being interpreted as a continuation of the user's prompt.
        val finalPrompt = if (style != null && style.promptSnippet.isNotEmpty()) {
            "$prompt; ${style.promptSnippet}"
        } else {
            prompt
        }

        // Resolve image inputs: upload local bytes via /v1/uploads,
        // otherwise pass a remote URL through unchanged.
        val imageUrls: List<String>? = if (isImageToImage) {
            if (inputImageUri.startsWith("http")) {
                listOf(inputImageUri)
            } else {
                val (data, mime) = transcodeImage(
                    uri = Uri.parse(inputImageUri),
                    context = context,
                )
                    ?: throw IOException("Failed to read image at $inputImageUri")
                listOf(uploadImage(data, mime))
            }
        } else {
            null
        }

        val body =
            config.model.buildInput(prompt = finalPrompt, size = imageSize, imageUrls = imageUrls)
                .apply {
                    put("model", modelId)
                }.toString()

        val url = postAndStreamSse(path = "/v1/responses", bodyJson = body)
        listOf(url)
    }

    // ----- HTTP / SSE -----

    /**
     * Posts the request body and streams the SSE response, returning the
     * generated image URL.
     *
     * The gateway streams 3 event types: `generation.status` and
     * `generation.delta` (progress, ignored here) and the terminals
     * `generation.completed` / `generation.failed`. We pump lines until
     * we see a terminal, then return or throw.
     */
    private suspend fun postAndStreamSse(
        path: String,
        bodyJson: String,
    ): String {
        val request = Request.Builder()
            .url("${config.gatewayUrl}$path")
            .header("Authorization", "Bearer ${config.apiKey}")
            .header("Accept", "text/event-stream")
            .post(bodyJson.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        return executeWithCancellation(request).use { resp ->
            if (!resp.isSuccessful) {
                val errorBody = resp.body?.string().orEmpty().take(MAX_ERROR_BODY_BYTES)
                throw IOException("Gateway returned HTTP ${resp.code}: $errorBody")
            }
            val source = resp.body?.source()
                ?: throw IOException("Gateway response had no body")

            var currentEvent = ""
            while (true) {
                currentCoroutineContext().ensureActive()
                val line = source.readUtf8Line() ?: break
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed.startsWith(":")) continue

                if (trimmed.startsWith("event:")) {
                    currentEvent = trimmed.substringAfter("event:").trim()
                    continue
                }
                if (!trimmed.startsWith("data:")) continue
                val payload = trimmed.substringAfter("data:").trim()

                when (currentEvent) {
                    "generation.completed" -> return@use parseCompletedEvent(payload)
                    "generation.failed" -> throw IOException(parseFailedMessage(payload))
                    else -> { // generation.status / generation.delta / unknown — ignore
                    }
                }
                currentEvent = ""
            }
            throw IOException("Stream ended without a completion event")
        }
    }

    /**
     * Executes a [Request], cancelling the in-flight OkHttp call if the
     * caller's coroutine is cancelled. Converts IOExceptions thrown while
     * the coroutine is no longer active into [kotlinx.coroutines.CancellationException], so
     * the caller sees a clean cancellation instead of an opaque IO error.
     */
    private suspend fun executeWithCancellation(request: Request): Response {
        val call = config.httpClient.newCall(request)
        currentCoroutineContext().job.invokeOnCompletion {
            if (it != null) call.cancel()
        }
        return try {
            call.execute()
        } catch (e: IOException) {
            if (!currentCoroutineContext().isActive) {
                throw CancellationException("Cancelled")
            }
            throw e
        }
    }

    // ----- Upload (two-step presigned PUT) -----

    /**
     * Two-step upload: POST `/v1/uploads` to mint a presigned URL, then
     * PUT the bytes directly to that URL (no auth — the URL itself is
     * the credential). Returns the resulting `asset_url`, which is passed
     * to `image_urls` on the generation request.
     */
    private suspend fun uploadImage(
        data: ByteArray,
        mimeType: String,
    ): String {
        // Step 1 — request a presigned upload URL.
        val metadataUrl = "${config.gatewayUrl}/v1/uploads"
        val metadataBody = JSONObject().put("content_type", mimeType).toString()

        val metadataRequest = Request.Builder()
            .url(metadataUrl)
            .header("Authorization", "Bearer ${config.apiKey}")
            .post(metadataBody.toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val (uploadUrl, assetUrl) = executeWithCancellation(metadataRequest).use { resp ->
            if (!resp.isSuccessful) {
                val errorBody = resp.body?.string().orEmpty().take(MAX_ERROR_BODY_BYTES)
                throw IOException("Gateway upload metadata HTTP ${resp.code}: $errorBody")
            }
            val json = JSONObject(resp.body?.string().orEmpty())
            json.getString("upload_url") to json.getString("asset_url")
        }

        // Step 2 — PUT the bytes to the presigned URL (no auth header).
        currentCoroutineContext().ensureActive()

        val putRequest = try {
            Request.Builder()
                .url(uploadUrl)
                .header("Content-Type", mimeType)
                .put(data.toRequestBody(mimeType.toMediaType()))
                .build()
        } catch (e: IllegalArgumentException) {
            throw IOException("Gateway returned an invalid upload_url", e)
        }
        executeWithCancellation(putRequest).use { resp ->
            if (!resp.isSuccessful) {
                val errorBody = resp.body?.string().orEmpty().take(MAX_ERROR_BODY_BYTES)
                throw IOException("Presigned PUT failed with HTTP ${resp.code}: $errorBody")
            }
        }

        return assetUrl
    }

    // ----- SSE event decoding -----

    private fun parseCompletedEvent(json: String): String {
        val event = JSONObject(json)
        val output = event.getJSONArray("output")
        val first = if (output.length() > 0) output.optJSONObject(0) else null
        val urlString = first?.optString("url", "").orEmpty()
        if (urlString.isEmpty()) {
            throw IOException("Completion event contained no usable image URL")
        }
        return urlString
    }

    private fun parseFailedMessage(json: String): String = try {
        val obj = JSONObject(json)
        when (val err = obj.opt("error")) {
            is JSONObject -> err.optString("message", "Generation failed")
            else -> "Generation failed"
        }
    } catch (_: Exception) {
        "Generation failed"
    }

    // ----- Image preprocessing (HEIC/PNG/JPEG -> JPEG, resize to fit max dim) -----

    /**
     * Reads the URI as bytes, then attempts to decode + resize + re-encode
     * as JPEG. Returns the processed bytes when the round-trip succeeds, or
     * falls back to the original bytes (still claiming `image/jpeg`) when
     * decode fails — matching the iOS service's permissive posture. Returns
     * `null` only when the bytes can't be read at all.
     */
    private fun transcodeImage(
        uri: Uri,
        context: Context,
    ): Pair<ByteArray, String>? {
        val rawBytes = try {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            return null
        } ?: return null

        val bitmap = try {
            BitmapFactory.decodeByteArray(rawBytes, 0, rawBytes.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            null
        } ?: return Pair(rawBytes, "image/jpeg")

        return try {
            val width = bitmap.width
            val height = bitmap.height
            val maxDimension = MAX_SOURCE_IMAGE_DIMENSION
            val needsResize = width > maxDimension || height > maxDimension

            val processed = if (needsResize) {
                val scale = minOf(
                    maxDimension.toFloat() / width,
                    maxDimension.toFloat() / height,
                )
                Bitmap.createScaledBitmap(
                    bitmap,
                    (width * scale).roundToInt(),
                    (height * scale).roundToInt(),
                    true,
                )
            } else {
                bitmap
            }

            val output = ByteArrayOutputStream()
            processed.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, output)
            if (needsResize) bitmap.recycle()
            Pair(output.toByteArray(), "image/jpeg")
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Re-encode failed but we have the raw bytes — best effort.
            Pair(rawBytes, "image/jpeg")
        }
    }

    companion object {
        private const val MAX_SOURCE_IMAGE_DIMENSION = 1820
        private const val MAX_ERROR_BODY_BYTES = 4096
        private const val JPEG_QUALITY = 85
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()
    }
}
