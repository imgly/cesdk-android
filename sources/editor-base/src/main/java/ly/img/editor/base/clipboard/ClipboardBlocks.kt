package ly.img.editor.base.clipboard

import android.util.Base64

/**
 * The CE.SDK block format of the system clipboard, which the web and iOS editors write and read too: an HTML flavor
 * whose empty spans carry the blocks as `block.saveToString` wrote them and the location of each resource that only
 * the copying engine can load. `bindings/wasm/js_web/src/clipboard/cesdkBlocks.ts` defines it.
 *
 * Each carried resource travels as a `data:` URI beside the blocks. A clip passes through Binder, which holds about
 * 1 MB, so a larger copy names itself in the HTML only and carries its blocks as a block archive behind a second clip
 * item, which only Android editors read.
 */
internal object ClipboardBlocks {
    /** Bump when a paste can no longer read what an older copy wrote. */
    const val VERSION = "1"

    /** Resources with these schemes keep their URL. Every other scheme only resolves in the engine that copied. */
    val portableSchemes = listOf("http", "https")

    /** The schemes that the copy for this engine keeps as they are: a paste in this session loads them again. */
    val sessionSchemes = listOf("buffer", "blob", "bundle", "file", "http", "https", "opfs", "data", "content")

    /**
     * The blocks name each carried resource by a URL with this prefix, unique to the copy, so the relocation after a
     * paste moves no URL that the scene already uses.
     */
    const val RESOURCE_URL_PREFIX = "buffer://ly.img.clipboard/"

    /** What a CE.SDK copy put on the clipboard. */
    data class Payload(
        /** The id of the copy, so the engine that copied can paste its session copy. */
        val id: String,
        /** The blocks as `block.saveToString` wrote them. Empty when the copy was too large to carry. */
        val blocks: String,
        /** The `data:` URI of each carried resource, by the URL that the blocks name it with. */
        val resources: Map<String, String>,
    )

    fun placeholder(
        copyId: String,
        index: Int,
    ) = "$RESOURCE_URL_PREFIX$copyId/$index"

    /**
     * The HTML flavor of a copy: the blocks and the `data:` URIs of their [resources] as attributes of empty spans, so
     * they add no text to a paste into another app, then [textHtml], the formatted text of the copied text blocks. With
     * empty [blocks] it only names the copy, which the web and iOS editors read as a copy too large to carry.
     */
    fun html(
        id: String,
        blocks: String,
        resources: List<Pair<String, String>>,
        textHtml: String,
    ): String = buildString {
        append("<span data-cesdk-blocks-version=\"$VERSION\" data-cesdk-blocks-id=\"${escape(id)}\" ")
        append("data-cesdk-blocks=\"${escape(blocks)}\"></span>")
        for ((url, dataUrl) in resources) {
            append("<span data-cesdk-resource=\"${escape(url)}\" data-cesdk-resource-data=\"${escape(dataUrl)}\"></span>")
        }
        append(textHtml)
    }

    /** The `data:` URI of [bytes]. */
    fun dataUrl(
        mimeType: String,
        bytes: ByteArray,
    ) = "data:$mimeType;base64,${Base64.encodeToString(bytes, Base64.NO_WRAP)}"

    /**
     * Finds a CE.SDK copy in the HTML flavor of the clipboard. Browsers and other apps wrap the HTML in a document and
     * may write the attributes in either quote style, so the scan reads each tag and does not expect the span at the
     * root.
     */
    fun parse(html: String): Payload? {
        // A cheap test first: most pastes carry no CE.SDK blocks.
        if (!html.contains("data-cesdk-blocks")) return null
        var id: String? = null
        var blocks = ""
        val resources = mutableMapOf<String, String>()
        val scanner = TagScanner(html)
        while (true) {
            val attributes = scanner.nextTag() ?: break
            val blocksAttribute = attributes["data-cesdk-blocks"]
            val url = attributes["data-cesdk-resource"]
            if (blocksAttribute != null && id == null) {
                if (attributes["data-cesdk-blocks-version"] != VERSION) return null
                id = attributes["data-cesdk-blocks-id"] ?: ""
                blocks = blocksAttribute
            } else if (url != null && url.startsWith(RESOURCE_URL_PREFIX)) {
                // Only the URLs of this format: a forged copy cannot move a URL of the scene.
                attributes["data-cesdk-resource-data"]?.let { resources[url] = it }
            }
        }
        return id?.let { Payload(it, blocks, resources) }
    }

    /** The bytes of a `data:` URI, or `null` when it is no `data:` URI or its base64 is invalid. */
    fun decodeDataUrl(url: String): ByteArray? {
        if (!url.startsWith("data:")) return null
        val comma = url.indexOf(',')
        if (comma < 0) return null
        val header = url.substring(5, comma)
        val payload = url.substring(comma + 1)
        if (header.endsWith(";base64")) {
            return try {
                Base64.decode(payload, Base64.DEFAULT)
            } catch (_: IllegalArgumentException) {
                null
            }
        }
        return percentDecoded(payload)
    }

    /** The MIME type of a `data:` URI. */
    fun dataUrlMimeType(url: String): String = url.removePrefix("data:").substringBefore(',').substringBefore(';')

    private fun escape(value: String): String = buildString(value.length) {
        for (character in value) {
            when (character) {
                '&' -> append("&amp;")
                '"' -> append("&quot;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                else -> append(character)
            }
        }
    }

    /** Reads the attributes of each start tag, with their character references decoded. */
    private class TagScanner(
        private val html: String,
    ) {
        private var index = 0

        fun nextTag(): Map<String, String>? {
            while (true) {
                val open = html.indexOf('<', index)
                if (open < 0) return null
                index = open + 1
                if (index >= html.length || !html[index].isAsciiLetter()) continue
                skip { !it.isHtmlSpace() && it != '>' }
                val attributes = mutableMapOf<String, String>()
                while (true) {
                    val (name, value) = nextAttribute() ?: break
                    attributes[name] = value
                }
                return attributes
            }
        }

        private fun nextAttribute(): Pair<String, String>? {
            skip { it.isHtmlSpace() || it == '/' }
            if (index >= html.length || html[index] == '>') return null
            val nameStart = index
            skip { !it.isHtmlSpace() && it != '=' && it != '>' }
            val name = html.substring(nameStart, index).lowercase()
            skip { it.isHtmlSpace() }
            if (index >= html.length || html[index] != '=') return name to ""
            index += 1
            skip { it.isHtmlSpace() }
            if (index >= html.length) return name to ""
            val quote = html[index]
            if (quote == '"' || quote == '\'') {
                val valueStart = index + 1
                val valueEnd = html.indexOf(quote, valueStart).let { if (it < 0) html.length else it }
                index = minOf(valueEnd + 1, html.length)
                return name to unescape(html.substring(valueStart, valueEnd))
            }
            val valueStart = index
            skip { !it.isHtmlSpace() && it != '>' }
            return name to unescape(html.substring(valueStart, index))
        }

        private inline fun skip(condition: (Char) -> Boolean) {
            while (index < html.length && condition(html[index])) index += 1
        }
    }

    private val namedReferences = mapOf(
        "amp" to "&",
        "quot" to "\"",
        "lt" to "<",
        "gt" to ">",
        "apos" to "'",
        "nbsp" to "\u00A0",
    )

    /** Decodes the named references that browsers write in attributes and every numeric reference. */
    private fun unescape(value: String): String {
        if (!value.contains('&')) return value
        val result = StringBuilder(value.length)
        var index = 0
        while (index < value.length) {
            val ampersand = value.indexOf('&', index)
            if (ampersand < 0) break
            result.append(value, index, ampersand)
            val semicolon = value.indexOf(';', ampersand + 1).takeIf { it in (ampersand + 1)..(ampersand + 10) }
            val decoded = semicolon?.let { decodeReference(value.substring(ampersand + 1, it)) }
            if (semicolon != null && decoded != null) {
                result.append(decoded)
                index = semicolon + 1
            } else {
                result.append('&')
                index = ampersand + 1
            }
        }
        return result.append(value, index, value.length).toString()
    }

    private fun decodeReference(reference: String): String? {
        namedReferences[reference]?.let { return it }
        if (!reference.startsWith("#")) return null
        val digits = reference.substring(1)
        val codePoint = if (digits.startsWith("x") || digits.startsWith("X")) {
            digits.substring(1).toIntOrNull(16)
        } else {
            digits.toIntOrNull()
        } ?: return null
        return if (Character.isValidCodePoint(codePoint)) String(Character.toChars(codePoint)) else null
    }

    /** Percent-decodes to bytes. A `%` without two hex digits stays as it is. */
    private fun percentDecoded(text: String): ByteArray {
        val bytes = text.toByteArray(Charsets.UTF_8)
        val result = java.io.ByteArrayOutputStream(bytes.size)
        var index = 0
        while (index < bytes.size) {
            val hex = if (bytes[index] == '%'.code.toByte() && index + 2 < bytes.size) {
                String(bytes, index + 1, 2, Charsets.US_ASCII)
            } else {
                null
            }
            // `toIntOrNull` takes a sign, which is no hex digit.
            val byte = hex?.takeIf { it.all { c -> c.isDigit() || c.lowercaseChar() in 'a'..'f' } }?.toInt(16)
            if (byte != null) {
                result.write(byte)
                index += 3
            } else {
                result.write(bytes[index].toInt())
                index += 1
            }
        }
        return result.toByteArray()
    }

    private fun Char.isHtmlSpace() = this == ' ' || this == '\t' || this == '\n' || this == '\u000C' || this == '\r'

    private fun Char.isAsciiLetter() = this in 'a'..'z' || this in 'A'..'Z'
}
