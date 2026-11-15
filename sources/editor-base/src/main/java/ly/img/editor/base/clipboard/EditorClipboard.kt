package ly.img.editor.base.clipboard

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.os.Build
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import ly.img.engine.EngineErrorCode
import ly.img.engine.EngineException
import ly.img.engine.PositionMode
import ly.img.engine.SizeMode
import java.io.File
import java.util.UUID

/**
 * Copy and paste through the system clipboard, in the block format of the web and iOS editors, so a copy pastes in
 * another editor and another app. While a text block is in edit mode, the engine's text input handles both.
 */
internal class EditorClipboard(
    private val engine: Engine,
    private val context: Context,
    private val clipboard: ClipboardManager = context.getSystemService(ClipboardManager::class.java),
) {
    /**
     * The last copy of this editor, which holds every resource of the blocks. A paste of the same copy uses it, so it
     * needs no archive.
     */
    private data class Copy(
        val id: String,
        val blocks: String,
        /** How often this editor pasted the copy, so each paste lands a step further from the copied blocks. */
        var pasteCount: Int = 0,
    )

    private var lastCopy: Copy? = null

    /** A file or media item of the clipboard that the editor's upload path takes. */
    data class Media(
        /** A file of the editor's file provider that holds the item. */
        val uri: Uri,
        val mimeType: String,
    )

    /** What a paste did, or what it leaves to the upload path of the editor. */
    sealed interface Result {
        /** The paste placed blocks. */
        data object Placed : Result

        /** The clipboard holds media for the upload path. [unsupported] tells whether it held a file of another type. */
        data class Upload(
            val media: List<Media>,
            val unsupported: Boolean,
        ) : Result

        /** The clipboard holds nothing that the editor can place. */
        data object Nothing : Result
    }

    /** Whether the clipboard holds something. Reads only its description, so Android shows no paste notice. */
    val hasContent: Boolean
        get() = clipboard.hasPrimaryClip()

    /**
     * Copies [blocks] to the system clipboard and keeps a session copy. The clipboard gets the blocks in the web format
     * when they fit under [MAX_HTML_LENGTH], and a block archive behind a second clip item otherwise.
     */
    suspend fun copy(blocks: List<DesignBlock>) {
        val id = UUID.randomUUID().toString()
        val textBlocks = blocks.filter { engine.block.getType(it) == DesignBlockType.Text.key }
        val text = textBlocks.joinToString("\n") { engine.block.getString(it, "text/text") }
        // Other apps and other engines read the formatting of the text from its HTML.
        val textHtml = textBlocks.joinToString("") { engine.block.getTextAsHTML(it) }
        val session = Copy(id, engine.block.saveToString(blocks, ClipboardBlocks.sessionSchemes))

        val portable = portableHtml(id, blocks, text, textHtml)
        // The text of a copy above the limit stays out too, so the clip still fits through Binder.
        val fits = text.length + textHtml.length <= MAX_HTML_LENGTH
        val clip = ClipData.newHtmlText(
            CLIP_LABEL,
            if (fits) text else "",
            portable ?: ClipboardBlocks.html(id, "", emptyList(), if (fits) textHtml else ""),
        )
        if (portable == null) {
            // The URI item lets the system grant read access to the app that pastes.
            val item = ClipData.Item(writeArchive(id, blocks))
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                clip.addItem(context.contentResolver, item)
            } else {
                clip.addItem(item)
            }
        }
        clipboard.setPrimaryClip(clip)
        lastCopy = session
    }

    /** The web format of the copy, or `null` when its HTML would not fit through Binder. */
    private suspend fun portableHtml(
        id: String,
        blocks: List<DesignBlock>,
        text: String,
        textHtml: String,
    ): String? {
        // The engine asks for every resource before it gets an answer, so each placeholder takes its index before the
        // first suspension.
        val resources = sortedMapOf<Int, Pair<String, String>>()
        var nextIndex = 0
        var budget = MAX_HTML_LENGTH - text.length - textHtml.length
        var readFailed = false
        val saved = try {
            engine.block.saveToString(blocks, ClipboardBlocks.portableSchemes) { uri, _ ->
                val index = nextIndex++
                val placeholder = ClipboardBlocks.placeholder(id, index)
                val bytes = java.io.ByteArrayOutputStream()
                // Base64 makes 4 characters of 3 bytes, so the read stops once the copy cannot fit.
                try {
                    engine.editor.getResourceData(uri, READ_CHUNK_SIZE) { chunk ->
                        budget -= (chunk.remaining() + 2) / 3 * 4
                        if (budget < 0) return@getResourceData false
                        bytes.write(ByteArray(chunk.remaining()).also { chunk.get(it) })
                        true
                    }
                } catch (_: EngineException) {
                    readFailed = true
                }
                val mimeType = try {
                    engine.editor.getMimeType(uri)
                } catch (_: EngineException) {
                    ""
                }
                resources[index] = placeholder to ClipboardBlocks.dataUrl(mimeType, bytes.toByteArray())
                Uri.parse(placeholder)
            }
        } catch (_: EngineException) {
            return null
        }
        // A copy with a resource that could not be read carries the archive, as the web carries no such copy.
        if (readFailed || budget < 0) return null
        return ClipboardBlocks.html(id, saved, resources.values.toList(), textHtml).takeIf { it.length <= MAX_HTML_LENGTH }
    }

    private suspend fun writeArchive(
        id: String,
        blocks: List<DesignBlock>,
    ): Uri {
        val archive = engine.block.saveToArchive(blocks)
        return withContext(Dispatchers.IO) {
            val directory = File(context.cacheDir, ARCHIVE_DIRECTORY).apply { mkdirs() }
            val file = File(directory, archiveName(id))
            file.outputStream().channel.use { it.write(archive) }
            // Only the latest copy is on the clipboard, so the archives of earlier copies go.
            directory.listFiles()?.filter { it != file }?.forEach { it.delete() }
            FileProvider.getUriForFile(context, "${context.packageName}$FILE_PROVIDER_SUFFIX", file)
        }
    }

    /**
     * Pastes onto [page] what the clipboard holds, in this order, as the web editor does: CE.SDK blocks, then media
     * files, then SVG markup, then formatted text and plain text as a new text block. Media goes back to the caller,
     * which owns the upload path.
     */
    suspend fun paste(page: DesignBlock): Result {
        val clip = clipboard.primaryClip ?: return Result.Nothing
        val items = (0 until clip.itemCount).map(clip::getItemAt)
        val html = items.firstNotNullOfOrNull { it.htmlText } ?: ""
        val plain = items.firstNotNullOfOrNull { it.text?.toString() } ?: ""
        var uris = items.mapNotNull { it.uri }

        ClipboardBlocks.parse(html)?.let { payload ->
            if (pasteBlocks(payload, uris, page)) return Result.Placed
            // The archive of a copy is no file to upload: a paste that cannot place the copy pastes its text.
            uris = uris.filterNot { it.lastPathSegment == archiveName(payload.id) }
        }
        val mayAdd = engine.block.isAllowedByScope(page, "editor/add")
        if (uris.isNotEmpty()) return if (mayAdd) readMedia(uris) else Result.Nothing
        if (!mayAdd) return Result.Nothing
        if (ClipboardSvg.isMarkup(plain)) {
            // Adobe Illustrator copies its selection as SVG markup in plain text.
            runCatching { engine.block.createFromSVG(plain) }.getOrNull()?.let {
                placePastedBlocks(listOf(it), page)
                return Result.Placed
            }
        }
        return if (pasteText(html, plain, page)) Result.Placed else Result.Nothing
    }

    /** Pastes the SVG file at [uri] as editable shapes. */
    suspend fun pasteSvg(
        uri: Uri,
        page: DesignBlock,
    ) {
        val svg = withContext(Dispatchers.IO) {
            context.contentResolver.openInputStream(uri)?.use { it.readBytes().decodeToString() }
        } ?: throw IllegalStateException("Could not read $uri.")
        placePastedBlocks(listOf(engine.block.createFromSVG(svg)), page)
    }

    private suspend fun pasteBlocks(
        payload: ClipboardBlocks.Payload,
        uris: List<Uri>,
        page: DesignBlock,
    ): Boolean {
        val session = lastCopy?.takeIf { it.id == payload.id }
        if (session == null && !engine.block.isAllowedByScope(page, "editor/add")) return false
        val loaded = when {
            session != null -> engine.block.loadFromString(session.blocks)
            payload.blocks.isNotEmpty() -> {
                val loaded = engine.block.loadFromString(payload.blocks)
                relocate(payload.resources)
                loaded
            }
            else -> {
                // The archive of a copy from another Android editor, which names it by the id of the copy.
                val archive = uris.firstOrNull { it.lastPathSegment == archiveName(payload.id) } ?: return false
                engine.block.loadFromArchive(Uri.fromFile(copyToCache(archive, "zip")))
            }
        }
        engine.block.findAllSelected().forEach { engine.block.setSelected(it, false) }
        val managesPages = engine.editor.getRole() == CREATOR_ROLE
        val pasteCount = session?.let {
            it.pasteCount += 1
            it.pasteCount
        } ?: 0
        for (block in loaded) {
            val isPage = engine.block.getType(block) == DesignBlockType.Page.key
            // A copy from another editor passed no check of this editor's role.
            if (isPage && !managesPages) {
                engine.block.destroy(block)
                continue
            }
            // A copied page cannot nest inside a page, so it goes into the parent of the current one.
            val parent = (if (isPage) engine.block.getParent(page) else page) ?: continue
            engine.block.appendChild(parent, block)
            if (session != null && !isPage && !engine.block.isTransformLocked(block)) {
                offset(block, PASTE_OFFSET * pasteCount)
            }
            engine.block.setSelected(block, true)
        }
        engine.editor.addUndoStep()
        return true
    }

    private fun offset(
        block: DesignBlock,
        distance: Float,
    ) {
        engine.block.setPositionXMode(block, PositionMode.ABSOLUTE)
        engine.block.setPositionX(block, engine.block.getPositionX(block) + distance)
        engine.block.setPositionYMode(block, PositionMode.ABSOLUTE)
        engine.block.setPositionY(block, engine.block.getPositionY(block) - distance)
    }

    /** Points the pasted blocks at files with the bytes that a web or iOS copy carried. */
    private suspend fun relocate(resources: Map<String, String>) {
        for ((placeholder, dataUrl) in resources) {
            // A resource that fails to decode keeps its placeholder, so its block shows an error instead of failing the
            // paste.
            val bytes = ClipboardBlocks.decodeDataUrl(dataUrl) ?: continue
            val extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(ClipboardBlocks.dataUrlMimeType(dataUrl))
            val file = withContext(Dispatchers.IO) {
                File(context.cacheDir, "ly.img.clipboard.pasted").apply { mkdirs() }
                    .let { File.createTempFile("resource", extension?.let { ".$it" }, it) }
                    .apply { writeBytes(bytes) }
            }
            engine.editor.relocateResource(Uri.parse(placeholder), Uri.fromFile(file))
        }
    }

    /** Reads files that a paste or a drop hands over, for the upload path. Files of other types set `unsupported`. */
    suspend fun readMedia(uris: List<Uri>): Result.Upload {
        val media = mutableListOf<Media>()
        var unsupported = false
        for (uri in uris) {
            val mimeType = context.contentResolver.getType(uri) ?: ""
            if (MEDIA_TYPES.none { mimeType.startsWith(it) }) {
                unsupported = true
                continue
            }
            // A clipboard grant ends with the clip, so the upload keeps a copy, as the camera's photos are.
            val copy = copyToCache(uri, MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType) ?: "")
            media += Media(FileProvider.getUriForFile(context, "${context.packageName}$FILE_PROVIDER_SUFFIX", copy), mimeType)
        }
        return Result.Upload(media, unsupported)
    }

    private fun archiveName(copyId: String) = "$copyId.zip"

    private suspend fun copyToCache(
        uri: Uri,
        extension: String,
    ): File = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "ly.img.clipboard.pasted").apply { mkdirs() }
        val file = File.createTempFile("pasted", if (extension.isEmpty()) null else ".$extension", directory)
        val input = context.contentResolver.openInputStream(uri) ?: throw IllegalStateException("Could not read $uri.")
        input.use { source -> file.outputStream().use { source.copyTo(it) } }
        file
    }

    private suspend fun pasteText(
        html: String,
        plain: String,
        page: DesignBlock,
    ): Boolean {
        if (html.isEmpty() && plain.isBlank()) return false
        val typeface = if (html.isEmpty()) null else PastedTextTypeface.find(engine, html)
        val text = engine.block.create(DesignBlockType.Text)
        try {
            if (html.isNotEmpty()) {
                typeface?.let { engine.block.setTypeface(text, it) }
                try {
                    engine.block.replaceTextWithHTML(text, html)
                } catch (exception: EngineException) {
                    if (exception.code != EngineErrorCode.BLOCK_TEXT_HTML_HAS_NO_TEXT) throw exception
                    // A CE.SDK copy too large to carry holds no text, and so pastes nothing here.
                    if (plain.isEmpty()) {
                        engine.block.destroy(text)
                        return false
                    }
                    engine.block.replaceText(text, plain)
                }
            } else {
                engine.block.replaceText(text, plain)
            }
        } catch (exception: Exception) {
            engine.block.destroy(text)
            throw exception
        }
        engine.block.setWidthMode(text, SizeMode.AUTO)
        engine.block.setHeightMode(text, SizeMode.AUTO)
        engine.block.appendChild(page, text)
        // A long line wraps instead of running past the page.
        val maxWidth = engine.block.getFrameWidth(page) * MAX_WIDTH_OF_PAGE
        if (engine.block.getFrameWidth(text) > maxWidth) {
            engine.block.setWidthMode(text, SizeMode.ABSOLUTE)
            engine.block.setWidth(text, maxWidth)
        }
        // A shrunk font would be unreadable, so a long text runs past the page.
        placePastedBlocks(listOf(text), page, shrinkToFit = false)
        return true
    }

    /**
     * Places content from outside CE.SDK on a page: appends it, shrinks it to fit unless [shrinkToFit] is `false`,
     * centers it, selects it and adds one undo step. The blocks keep their layout relative to each other.
     */
    private fun placePastedBlocks(
        ids: List<DesignBlock>,
        parent: DesignBlock,
        shrinkToFit: Boolean = true,
    ) {
        if (ids.isEmpty()) return
        val block = engine.block
        ids.forEach { block.appendChild(parent, it) }
        val frames = ids.map {
            Frame(it, block.getPositionX(it), block.getPositionY(it), block.getFrameWidth(it), block.getFrameHeight(it))
        }
        val minX = frames.minOf { it.x }
        val minY = frames.minOf { it.y }
        val width = frames.maxOf { it.x + it.width } - minX
        val height = frames.maxOf { it.y + it.height } - minY
        val parentWidth = block.getFrameWidth(parent)
        val parentHeight = block.getFrameHeight(parent)
        val factor = if (shrinkToFit) minOf(1F, parentWidth / width, parentHeight / height) else 1F
        val left = (parentWidth - width * factor) / 2
        val top = (parentHeight - height * factor) / 2
        block.findAllSelected().forEach { block.setSelected(it, false) }
        for (frame in frames) {
            if (factor < 1F) block.scale(frame.id, factor, anchorX = 0F, anchorY = 0F)
            block.setPositionX(frame.id, left + (frame.x - minX) * factor)
            block.setPositionY(frame.id, top + (frame.y - minY) * factor)
            block.setSelected(frame.id, true)
        }
        engine.editor.addUndoStep()
    }

    private data class Frame(
        val id: DesignBlock,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
    )

    companion object {
        private const val CLIP_LABEL = "CE.SDK"
        private const val ARCHIVE_DIRECTORY = "ly.img.clipboard"
        private const val FILE_PROVIDER_SUFFIX = ".ly.img.editor.fileprovider"
        private const val MAX_WIDTH_OF_PAGE = 0.8F
        private const val CREATOR_ROLE = "Creator"

        /** The step between the pastes of a copy, as Duplicate moves its copy. */
        private const val PASTE_OFFSET = 5F
        private const val READ_CHUNK_SIZE = 1024 * 1024

        /**
         * A clip travels through one Binder transaction of at most 1 MB, which also holds the plain text, and a string
         * takes 2 bytes a character in it. A copy with more HTML carries a block archive instead.
         */
        const val MAX_HTML_LENGTH = 256 * 1024
        private val MEDIA_TYPES = listOf("image/", "video/", "audio/")
    }
}
