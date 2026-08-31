package ly.img.editor.plugin.autoCaptions.gateway

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import ly.img.editor.plugin.autoCaptions.Srt
import ly.img.editor.plugin.autoCaptions.TimedWord
import ly.img.editor.plugin.autoCaptions.TranscriptionOptions
import ly.img.editor.plugin.autoCaptions.TranscriptionProvider
import ly.img.editor.plugin.autoCaptions.cuesFrom
import okhttp3.OkHttpClient
import org.json.JSONObject
import java.io.File

/**
 * The default [TranscriptionProvider]: ElevenLabs Scribe v2 through the IMG.LY AI Gateway. The gateway handles
 * provider routing, billing and asset storage, so integrators only supply an IMG.LY API key.
 *
 * @param apiKey an IMG.LY Gateway API key (`sk_…`), from the IMG.LY Dashboard.
 * @param httpClient an OkHttp client to route requests through, e.g. to add an interceptor or a proxy. Defaults to
 * one with timeouts generous enough for long recordings.
 */
class GatewayTranscriptionProvider(
    apiKey: String,
    gatewayUrl: String = DEFAULT_GATEWAY_URL,
    httpClient: OkHttpClient? = null,
) : TranscriptionProvider {
    override val name = "IMG.LY Gateway — ElevenLabs Scribe v2"

    private val client = GatewayClient(
        apiKey = apiKey,
        gatewayUrl = gatewayUrl.trimEnd('/'),
        httpClient = httpClient ?: GatewayClient.defaultHttpClient(),
    )

    override suspend fun transcribe(
        audio: File,
        mimeType: String,
        options: TranscriptionOptions,
    ): String {
        val audioUrl = client.upload(file = audio, contentType = mimeType)
        currentCoroutineContext().ensureActive()

        val input = JSONObject()
            .put("model", MODEL)
            .put("audio_url", audioUrl)
        options.language?.let { input.put("language_code", it) }

        val completed = client.run(input)
        // Off the caller's thread: the editor scope this runs in is the main one, and parsing a transcript,
        // grouping it and serialising the result is real work on a long recording.
        return withContext(Dispatchers.Default) {
            val words = parseWords(completed)
            val cues = cuesFrom(words, maxLineLength = options.maxLineLength, maxLines = options.maxLines)
            Srt.serialize(cues)
        }
    }

    companion object {
        const val DEFAULT_GATEWAY_URL = "https://gateway.img.ly"

        private const val MODEL = "elevenlabs/scribe-v2"

        /** The only entry type that carries speech; `spacing` and `audio_event` are not transcribed text. */
        private const val WORD_TYPE = "word"

        /**
         * Decodes `output[0].data.words`. Internal, not private, so a unit test can pin the response shape.
         *
         * Scribe tags each entry `word`, `spacing` or `audio_event`, and a `spacing` entry's text is a literal
         * space. [cuesFrom] joins words with its own space, so letting those through would double the spacing in
         * every caption. Only an entry that says it is *not* a word is dropped: a payload without the field at all
         * — a gateway that already normalises the shape — must still yield its words.
         */
        internal fun parseWords(payload: String): List<TimedWord> {
            val output = JSONObject(payload).optJSONArray("output") ?: return emptyList()
            val words = output.optJSONObject(0)?.optJSONObject("data")?.optJSONArray("words") ?: return emptyList()
            return (0 until words.length()).mapNotNull { index ->
                val word = words.optJSONObject(index) ?: return@mapNotNull null
                val type = word.optString("type")
                if (type.isNotEmpty() && type != WORD_TYPE) return@mapNotNull null
                TimedWord(
                    text = word.optString("text"),
                    start = word.optDouble("start", 0.0),
                    end = word.optDouble("end", 0.0),
                )
            }
        }
    }
}
