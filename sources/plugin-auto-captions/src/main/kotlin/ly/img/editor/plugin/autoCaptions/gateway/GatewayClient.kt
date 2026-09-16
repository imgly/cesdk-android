package ly.img.editor.plugin.autoCaptions.gateway

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import ly.img.editor.plugin.autoCaptions.useCancellable
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/** A minimal OkHttp client for the IMG.LY AI Gateway: upload an asset, run a model, read the SSE result. */
internal class GatewayClient(
    private val apiKey: String,
    private val gatewayUrl: String,
    private val httpClient: OkHttpClient,
) {
    /**
     * Mints a presigned URL, PUTs the file to it (the URL is the credential, so no auth), returns the asset URL.
     *
     * The file is handed to OkHttp rather than read here, so a long recording streams out in segments instead of
     * being resident while it uploads.
     */
    suspend fun upload(
        file: File,
        contentType: String,
    ): String = withContext(Dispatchers.IO) {
        val metadataRequest = authorizedRequest("v1/uploads")
            .post(JSONObject().put("content_type", contentType).toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        val (uploadUrl, assetUrl) = httpClient.newCall(metadataRequest).useCancellable { response ->
            validate(response, "upload metadata")
            val json = JSONObject(response.body?.string().orEmpty())
            json.getString("upload_url") to json.getString("asset_url")
        }

        currentCoroutineContext().ensureActive()

        val putRequest = try {
            Request.Builder()
                .url(uploadUrl)
                // The header carries the type; the body stays untyped so a MIME type OkHttp cannot parse fails at
                // the gateway, which knows what it accepts, rather than here.
                .header("Content-Type", contentType)
                .put(file.asRequestBody())
                .build()
        } catch (throwable: IllegalArgumentException) {
            throw IOException("The gateway returned an invalid upload_url", throwable)
        }
        httpClient.newCall(putRequest).useCancellable { validate(it, "upload") }

        assetUrl
    }

    /** Runs a model and returns the payload of the `generation.completed` SSE event. */
    suspend fun run(body: JSONObject): String = withContext(Dispatchers.IO) {
        val request = authorizedRequest("v1/responses")
            .header("Accept", "text/event-stream")
            .post(body.toString().toRequestBody(JSON_MEDIA_TYPE))
            .build()

        httpClient.newCall(request).useCancellable { response ->
            validate(response, "transcription")
            val source = response.body?.source() ?: throw IOException("The gateway response had no body")

            var event = ""
            while (true) {
                currentCoroutineContext().ensureActive()
                val trimmed = (source.readUtf8Line() ?: break).trim()
                // A blank line ends an SSE event and a leading colon marks a comment; neither carries data.
                if (trimmed.isEmpty() || trimmed.startsWith(":")) continue
                if (trimmed.startsWith("event:")) {
                    event = trimmed.substringAfter("event:").trim()
                    continue
                }
                if (!trimmed.startsWith("data:")) continue
                val payload = trimmed.substringAfter("data:").trim()
                when (event) {
                    "generation.completed" -> return@useCancellable payload
                    "generation.failed" -> throw IOException(failureMessage(payload))
                }
                // Reset after each consumed data line, so a later `data:` without its own `event:` cannot inherit
                // this event type.
                event = ""
            }
            throw IOException("The gateway stream ended without a result")
        }
    }

    private fun authorizedRequest(path: String) = Request.Builder()
        .url("$gatewayUrl/$path")
        .header("Authorization", "Bearer $apiKey")
        .header("Content-Type", "application/json")

    /** The body is peeked, not read whole: it only makes the message actionable and must not balloon it. */
    private fun validate(
        response: Response,
        step: String,
    ) {
        if (response.isSuccessful) return
        val body = runCatching { response.peekBody(MAX_ERROR_BODY_BYTES).string() }.getOrDefault("")
        throw IOException("The gateway returned HTTP ${response.code} for the $step request: $body")
    }

    private fun failureMessage(payload: String): String = runCatching {
        JSONObject(payload).getJSONObject("error").getString("message")
    }.getOrNull() ?: "Transcription failed"

    companion object {
        /** Speech-to-text can take a while for long recordings, so allow a generous idle timeout. */
        private const val REQUEST_TIMEOUT_SECONDS = 600L
        private const val CONNECT_TIMEOUT_SECONDS = 15L
        private const val MAX_ERROR_BODY_BYTES = 4096L
        private val JSON_MEDIA_TYPE = "application/json".toMediaType()

        fun defaultHttpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(REQUEST_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .build()
    }
}
