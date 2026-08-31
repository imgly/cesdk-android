package ly.img.editor.base.dock.options.captions

import ly.img.engine.EngineErrorCode
import ly.img.engine.EngineException
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.IOException

/**
 * Pins how a picked file is classified and how a failed import is explained.
 *
 * Both matter more than they look. The engine identifies a caption file by its bytes and, failing that, by the
 * extension in its URI — so staging the picked file under the wrong extension turns a valid import into an
 * "unsupported format". And the error codes the engine raises here are shared with the export pipeline, so
 * falling back to its own copy would tell somebody importing a file that their export was stopped.
 */
class CaptionImportTest {
    // region Format from the file name

    @Test
    fun `an srt file name is subrip`() {
        assertEquals(CaptionFileFormat.SubRip, format(displayName = "captions.srt"))
    }

    @Test
    fun `a vtt file name is webvtt`() {
        assertEquals(CaptionFileFormat.WebVtt, format(displayName = "captions.vtt"))
    }

    @Test
    fun `the file name extension is matched case-insensitively`() {
        assertEquals(CaptionFileFormat.WebVtt, format(displayName = "CAPTIONS.VTT"))
    }

    @Test
    fun `only the last extension of a file name counts`() {
        assertEquals(CaptionFileFormat.WebVtt, format(displayName = "captions.srt.vtt"))
    }

    @Test
    fun `the file name wins over a contradicting mime type`() {
        assertEquals(
            CaptionFileFormat.WebVtt,
            format(displayName = "captions.vtt", mimeType = "application/x-subrip"),
        )
        // The other direction too, so neither format can pass by being the fallback.
        assertEquals(
            CaptionFileFormat.SubRip,
            format(displayName = "captions.srt", mimeType = "text/vtt"),
        )
    }

    // endregion
    // region Format from the mime type

    @Test
    fun `the subrip mime type is used when the file name has no extension`() {
        // Content that says WebVTT, so the assertion fails if the MIME type is ignored and the sniff decides.
        assertEquals(
            CaptionFileFormat.SubRip,
            format(displayName = "captions", mimeType = "application/x-subrip", content = "WEBVTT\n".toByteArray()),
        )
    }

    @Test
    fun `the vtt mime type is used when the file name has no extension`() {
        assertEquals(CaptionFileFormat.WebVtt, format(displayName = null, mimeType = "text/vtt"))
    }

    @Test
    fun `a mime type parameter does not stop it matching`() {
        assertEquals(CaptionFileFormat.WebVtt, format(displayName = null, mimeType = "text/vtt; charset=utf-8"))
    }

    @Test
    fun `the generic mime types providers report for caption files decide nothing`() {
        // Both are what stock Android and cloud providers hand out for .srt and .vtt, so they must not be trusted.
        assertEquals(CaptionFileFormat.SubRip, format(displayName = null, mimeType = "application/octet-stream"))
        assertEquals(CaptionFileFormat.SubRip, format(displayName = null, mimeType = "text/plain"))
    }

    // endregion
    // region Format from the content

    @Test
    fun `a webvtt signature is recognized when nothing else identifies the file`() {
        assertEquals(
            CaptionFileFormat.WebVtt,
            format(displayName = "download", mimeType = "text/plain", content = "WEBVTT\n\n".toByteArray()),
        )
    }

    @Test
    fun `a webvtt signature is recognized behind a utf-8 byte order mark`() {
        // The engine cannot: its byte sniffing has no case for the mark, which is the gap this closes.
        val content = byteArrayOf(0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte()) + "WEBVTT\n".toByteArray()
        assertEquals(CaptionFileFormat.WebVtt, format(displayName = null, content = content))
    }

    @Test
    fun `a webvtt signature is recognized behind a utf-16 little-endian byte order mark`() {
        val content = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + "WEBVTT".toByteArray(Charsets.UTF_16LE)
        assertEquals(CaptionFileFormat.WebVtt, format(displayName = null, content = content))
    }

    @Test
    fun `a webvtt signature is recognized behind a utf-16 big-endian byte order mark`() {
        val content = byteArrayOf(0xFE.toByte(), 0xFF.toByte()) + "WEBVTT".toByteArray(Charsets.UTF_16BE)
        assertEquals(CaptionFileFormat.WebVtt, format(displayName = null, content = content))
    }

    @Test
    fun `subrip content falls through to the default`() {
        assertEquals(
            CaptionFileFormat.SubRip,
            format(displayName = null, content = "1\n00:00:00,000 --> 00:00:02,500\nHello\n".toByteArray()),
        )
    }

    @Test
    fun `a file that identifies itself in no way at all is treated as subrip`() {
        // SubRip has no signature to look for, so it is the only format an unidentifiable file can be.
        assertEquals(CaptionFileFormat.SubRip, format(displayName = null, mimeType = null, content = ByteArray(0)))
    }

    @Test
    fun `a truncated signature is not mistaken for webvtt`() {
        assertEquals(CaptionFileFormat.SubRip, format(displayName = null, content = "WEB".toByteArray()))
    }

    @Test
    fun `a signature that does not start the file is not matched`() {
        assertEquals(CaptionFileFormat.SubRip, format(displayName = null, content = "\nWEBVTT".toByteArray()))
    }

    // endregion
    // region Failure copy

    @Test
    fun `an empty parse and empty resource data both read as no captions found`() {
        assertEquals(CaptionImportFailure.ParseEmpty, failure(EngineErrorCode.UTILS_CAPTION_PARSE_EMPTY))
        assertEquals(CaptionImportFailure.ParseEmpty, failure(EngineErrorCode.ENCODE_RESOURCE_DATA_EMPTY))
    }

    @Test
    fun `a file that parsed to nothing reads as no captions found`() {
        // The engine reports a resource that never loaded as a *successful* empty import.
        assertEquals(CaptionImportFailure.ParseEmpty, CaptionImport.failure(EmptyCaptionImportException()))
    }

    @Test
    fun `a rejected mime type reads as an unsupported format`() {
        assertEquals(CaptionImportFailure.UnsupportedFormat, failure(EngineErrorCode.ENCODE_MIME_TYPE_INVALID))
        assertEquals(CaptionImportFailure.UnsupportedFormat, failure(EngineErrorCode.UTILS_CAPTION_UNSUPPORTED_MIME))
    }

    @Test
    fun `an odd-sized utf-16 file reads as damaged`() {
        assertEquals(CaptionImportFailure.FileDamaged, failure(EngineErrorCode.UTILS_CAPTION_UTF16_INVALID_SIZE))
    }

    @Test
    fun `a resource that could not be loaded reads as unreadable`() {
        assertEquals(
            CaptionImportFailure.FileUnreadable,
            failure(EngineErrorCode.ENCODE_RESOURCE_LOAD_FAILED_WITH_REASON),
        )
        assertEquals(CaptionImportFailure.FileUnreadable, failure(EngineErrorCode.UTILS_CAPTION_DATA_UNAVAILABLE))
    }

    @Test
    fun `an engine failure with no import meaning keeps the engine's own message`() {
        // Captions can be disabled by an integrator setting, which fails before any file is read.
        assertEquals(CaptionImportFailure.Unknown, failure(EngineErrorCode.BLOCK_CAPTIONS_DISABLED))
    }

    @Test
    fun `a failure that never reached the engine reads as unreadable`() {
        // Staging the picked file is the step that throws these, and retrying is all the user can do.
        assertEquals(CaptionImportFailure.FileUnreadable, CaptionImport.failure(IOException("no such file")))
    }

    // endregion

    private fun format(
        displayName: String? = null,
        mimeType: String? = null,
        content: ByteArray = ByteArray(0),
    ) = CaptionImport.format(displayName = displayName, mimeType = mimeType, contentPrefix = content)

    private fun failure(code: String) = CaptionImport.failure(EngineException(message = "engine message", code = code))
}
