package ly.img.editor.base.dock.options.captions

import ly.img.engine.EngineErrorCode
import ly.img.engine.EngineException

/** The caption file formats the engine can parse. */
internal enum class CaptionFileFormat(
    val extension: String,
) {
    SubRip("srt"),
    WebVtt("vtt"),
}

/**
 * Why an SRT/VTT import failed, in the terms the sheet reports to the user.
 *
 * The engine raises its failures at two layers — the resource gate (`ENCODE.*`) and the caption parser
 * (`UTILS.CAPTION_*`) — and several of them mean the same thing to somebody who just picked a file, so they
 * collapse onto one reason each. Kept out of [CaptionsEngine] so the mapping can be tested without a scene.
 */
internal enum class CaptionImportFailure {
    /** The file parsed, but held no cues.  */
    ParseEmpty,

    /** The file is not an SRT or a VTT. */
    UnsupportedFormat,

    /** The file is an SRT or VTT, but its bytes are truncated. */
    FileDamaged,

    /** The file could not be read at all. */
    FileUnreadable,

    /** An engine failure with no import-specific meaning; the engine's own message is shown instead. */
    Unknown,
}

/**
 * Raised when a caption file yielded no cues.
 *
 * The engine's own failures should already cover this — `UTILS.CAPTION_PARSE_EMPTY` for a parsed-but-cueless
 * file, `ENCODE.RESOURCE_DATA_EMPTY` for a resource that never reached `Ready` — so `createCaptionsFromURI`
 * is not expected to return an empty list. This guards that invariant directly rather than trusting it, so a
 * slip in the engine can't silently replace an existing track with nothing. [failure] maps it to the same
 * copy as the two codes above, so keeping the check costs nothing.
 */
internal class EmptyCaptionImportException : RuntimeException("The caption file contained no captions")

/** The parsing and validation around an SRT/VTT import that needs no engine, so it can be tested directly. */
internal object CaptionImport {
    /**
     * How the picked file is classified, in the order the answers get less reliable.
     *
     * Storage Access Framework providers describe caption files inconsistently: stock Android maps `.srt` to
     * `application/x-subrip` but has no entry for `.vtt` at all, and cloud providers routinely report both as
     * `text/plain` or `application/octet-stream`. So the picker accepts a deliberately wide set of MIME types
     * and the format is settled here instead — the engine then validates the bytes and rejects anything that
     * is neither.
     */
    fun format(
        displayName: String?,
        mimeType: String?,
        contentPrefix: ByteArray,
    ): CaptionFileFormat = formatOfExtension(displayName?.substringAfterLast('.', missingDelimiterValue = ""))
        ?: formatOfMimeType(mimeType)
        ?: formatOfContent(contentPrefix)
        // SubRip is the safer default: it has no header to recognize, whereas a VTT always announces itself.
        ?: CaptionFileFormat.SubRip

    /** The format a file extension names, or `null` when it names neither. */
    private fun formatOfExtension(extension: String?): CaptionFileFormat? {
        val normalized = extension?.lowercase() ?: return null
        return CaptionFileFormat.values().firstOrNull { it.extension == normalized }
    }

    /** The format a MIME type names, or `null` for the generic types SAF hands out for unregistered formats. */
    private fun formatOfMimeType(mimeType: String?): CaptionFileFormat? = when (mimeType?.lowercase()?.substringBefore(';')?.trim()) {
        "application/x-subrip", "text/srt" -> CaptionFileFormat.SubRip
        "text/vtt" -> CaptionFileFormat.WebVtt
        else -> null
    }

    /**
     * The format the first bytes of the file announce.
     *
     * Only WebVTT can be recognized this way — it opens with a `WEBVTT` signature, which is matched here in
     * UTF-8 and in both UTF-16 byte orders, past an optional byte-order mark. SubRip has no signature.
     */
    private fun formatOfContent(contentPrefix: ByteArray): CaptionFileFormat? {
        val signature = "WEBVTT"
        val body = contentPrefix.dropByteOrderMark()
        val startsWithSignature = ENCODINGS.any { encoding ->
            val expected = signature.toByteArray(encoding)
            body.size >= expected.size && expected.indices.all { body[it] == expected[it] }
        }
        return CaptionFileFormat.WebVtt.takeIf { startsWithSignature }
    }

    /** The bytes past a UTF-8 or UTF-16 byte-order mark, or all of them when there is none. */
    private fun ByteArray.dropByteOrderMark(): ByteArray = BYTE_ORDER_MARKS
        .firstOrNull { mark -> size >= mark.size && mark.indices.all { this[it] == mark[it] } }
        ?.let { copyOfRange(it.size, size) }
        ?: this

    /**
     * What to tell the user about a failed import.
     *
     * Falling back to the engine's own copy is not enough for these codes: they are shared with the export
     * pipeline, so `ENCODE.RESOURCE_LOAD_FAILED_WITH_REASON` would tell somebody importing a caption file that
     * their export was stopped. Anything outside this set keeps the engine's message, which is why
     * [CaptionImportFailure.Unknown] exists.
     */
    fun failure(throwable: Throwable): CaptionImportFailure = if (throwable is EmptyCaptionImportException) {
        CaptionImportFailure.ParseEmpty
    } else {
        engineFailure(throwable)
    }

    private fun engineFailure(throwable: Throwable): CaptionImportFailure = when ((throwable as? EngineException)?.code) {
        EngineErrorCode.UTILS_CAPTION_PARSE_EMPTY,
        EngineErrorCode.ENCODE_RESOURCE_DATA_EMPTY,
        -> CaptionImportFailure.ParseEmpty

        EngineErrorCode.ENCODE_MIME_TYPE_INVALID,
        EngineErrorCode.UTILS_CAPTION_UNSUPPORTED_MIME,
        -> CaptionImportFailure.UnsupportedFormat

        EngineErrorCode.UTILS_CAPTION_UTF16_INVALID_SIZE,
        -> CaptionImportFailure.FileDamaged

        EngineErrorCode.ENCODE_RESOURCE_LOAD_FAILED_WITH_REASON,
        EngineErrorCode.UTILS_CAPTION_DATA_UNAVAILABLE,
        -> CaptionImportFailure.FileUnreadable

        // An engine failure the import has no better words for keeps the engine's own message. Everything that
        // is not an EngineException at all comes from reading the picked file, which the user can only retry.
        else -> if (throwable is EngineException) CaptionImportFailure.Unknown else CaptionImportFailure.FileUnreadable
    }

    /** How many leading bytes [format] needs to recognize a WebVTT signature behind a byte-order mark. */
    const val CONTENT_PREFIX_BYTES = 16

    private val ENCODINGS = listOf(Charsets.UTF_8, Charsets.UTF_16LE, Charsets.UTF_16BE)

    private val BYTE_ORDER_MARKS = listOf(
        byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()),
        byteArrayOf(0xFF.toByte(), 0xFE.toByte()),
        byteArrayOf(0xFE.toByte(), 0xFF.toByte()),
    )
}
