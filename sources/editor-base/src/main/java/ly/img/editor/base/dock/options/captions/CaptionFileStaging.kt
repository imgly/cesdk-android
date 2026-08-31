package ly.img.editor.base.dock.options.captions

import android.content.ContentResolver
import android.net.Uri
import android.provider.OpenableColumns
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ly.img.editor.core.ui.Environment
import java.io.File
import java.io.InputStream

/**
 * The MIME types the SRT/VTT picker offers.
 *
 * Deliberately wide: stock Android maps `.srt` to `application/x-subrip` but has no entry for `.vtt` at all,
 * and cloud providers report caption files as `text/plain` or `application/octet-stream`. Filtering to the two
 * caption types alone would hide every VTT file on the device. Picking the wrong file is caught downstream
 * instead: the parser finds no cues in it and the sheet reports that the file held no captions. The same
 * reasoning is behind the filters the archive and scene pickers use.
 */
internal val CAPTION_IMPORT_MIME_TYPES = arrayOf(
    "application/x-subrip",
    "text/srt",
    "text/vtt",
    "text/plain",
    "application/octet-stream",
)

/**
 * Copies a picked caption file into the editor's cache, under a name that ends in `.srt` or `.vtt`.
 *
 * The copy is not just about the read grant expiring. The engine classifies a resource by its magic bytes
 * first and by the URI's file extension second, and a `content://` URI carries no usable extension — so a file
 * the bytes cannot identify (a VTT behind a byte-order mark, a very short SRT) would be rejected as an
 * unsupported format even though it parses fine. Staging it under the right extension restores that fallback.
 *
 * The caller is responsible for deleting the returned file once the engine has read it.
 */
internal suspend fun stageCaptionFileForImport(
    contentResolver: ContentResolver,
    uri: Uri,
): File = withContext(Dispatchers.IO) {
    val directory = Environment.getEditorCacheDir().apply { mkdirs() }
    val stream = contentResolver.openInputStream(uri) ?: error("Cannot read the picked caption file at $uri")
    stream.buffered().use { input ->
        // The format is settled before anything is written, so the copy can be created under its final name —
        // a file that had to be renamed afterwards could outlive a cancellation with nobody left to delete it.
        input.mark(CaptionImport.CONTENT_PREFIX_BYTES)
        val prefix = input.readPrefix(CaptionImport.CONTENT_PREFIX_BYTES)
        input.reset()

        val format = CaptionImport.format(
            displayName = contentResolver.displayName(uri),
            mimeType = contentResolver.getType(uri),
            contentPrefix = prefix,
        )
        val staged = File.createTempFile("imgly-captions-", ".${format.extension}", directory)
        try {
            staged.outputStream().use { output -> input.copyTo(output) }
        } catch (throwable: Throwable) {
            staged.delete()
            throw throwable
        }
        staged
    }
}

/** The file name a content provider reports for a URI, which is where its extension survives. */
private fun ContentResolver.displayName(uri: Uri): String? = runCatching {
    query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
        val column = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
        if (column >= 0 && cursor.moveToFirst()) cursor.getString(column) else null
    }
}.getOrNull() ?: uri.lastPathSegment

/** At most [count] leading bytes, for recognizing a format signature. Leaves the stream positioned past them. */
private fun InputStream.readPrefix(count: Int): ByteArray {
    val buffer = ByteArray(count)
    var filled = 0
    while (filled < count) {
        val read = read(buffer, filled, count - filled)
        if (read < 0) break
        filled += read
    }
    return buffer.copyOf(filled)
}
