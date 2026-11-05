package ly.img.editor.plugin.backgroundRemoval.api

import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import ly.img.editor.core.EditorScope
import ly.img.editor.plugin.backgroundRemoval.BackgroundRemovalConfig
import ly.img.editor.plugin.backgroundRemoval.util.BackgroundRemovalConstants
import ly.img.editor.plugin.backgroundRemoval.util.FileLoader
import ly.img.editor.plugin.backgroundRemoval.util.ImageProcessor
import ly.img.editor.plugin.backgroundRemoval.util.measureAndGet
import ly.img.engine.BlockState
import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import ly.img.engine.FillType
import ly.img.engine.Source

internal object BackgroundRemovalApi {
    private const val IMAGE_FILE_URI = "fill/image/imageFileURI"
    private const val PREVIEW_FILE_URI = "fill/image/previewFileURI"
    private const val SOURCE_SET = "fill/image/sourceSet"
    private const val PROCESSING_BLOCKS_KEY = "ly.img.editor.plugin.backgroundRemoval.processingBlocks"
    private const val MODEL_LOCK_KEY = "ly.img.editor.plugin.backgroundRemoval.modelLock"

    // `loadBitmap` reads only these schemes. A relative URI has none.
    private val readableSchemes = setOf("http", "https", "file", "content", "android.resource", "buffer")

    /**
     * Whether the background of [block] can be removed: it has an image fill that is not SVG, and its scope allows a
     * fill change.
     */
    fun canProcess(
        engine: Engine,
        block: DesignBlock,
    ): Boolean = runCatching {
        engine.block.isAllowedByScope(block, "fill/change") && imageSource(engine, block) != null
    }.getOrDefault(false)

    /**
     * Removes the background of [block] in the editor's coroutine scope, so that a removal outlives the button that
     * started it. A tap on a block that is processing already is ignored. Several blocks are processed at once, and
     * the model runs on one image at a time, so the memory peak does not grow with each image the user taps.
     */
    fun EditorScope.startBackgroundRemoval(
        block: DesignBlock?,
        config: BackgroundRemovalConfig,
    ) {
        if (block == null) {
            Log.e(BackgroundRemovalConstants.TAG, "Failed to remove background: no block to remove the background from.")
            return
        }
        val processingBlocks = editorContext.mutableStateOf(key = PROCESSING_BLOCKS_KEY, initial = emptySet<DesignBlock>())
        if (block in processingBlocks.value) {
            return
        }
        processingBlocks.value += block
        editorContext.coroutineScope.launch {
            try {
                removeBackground(block = block, config = config)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                Log.e(BackgroundRemovalConstants.TAG, "Failed to remove background: ${exception.message}", exception)
            } finally {
                processingBlocks.value -= block
            }
        }
    }

    /**
     * Removes the background of the image of [block], and of each entry of its source set.
     */
    private suspend fun EditorScope.removeBackground(
        block: DesignBlock,
        config: BackgroundRemovalConfig,
    ) {
        val engine = editorContext.engine
        val editorJob = editorContext.coroutineScope.coroutineContext.job
        val source = requireNotNull(imageSource(engine, block)) { "Block $block has no image to process." }
        engine.block.setState(block = block, state = BlockState.Pending(0f))
        try {
            val modelLock = editorContext.mutableStateOf(key = MODEL_LOCK_KEY, initial = Mutex()).value
            lockUnlessReplaced(modelLock, block, source)
            val result = try {
                saveMaskedImage(block, source, config)
            } finally {
                modelLock.unlock()
            }
            val (current, files) = result ?: return
            if (currentImage(engine, block, source) == null) {
                ImageProcessor.delete(files)
                return
            }
            commit(engine, current, files)
            engine.editor.addUndoStep()
        } finally {
            // A closed editor stops its engine, and the next editor starts the same engine with other blocks.
            if (editorJob.isActive && engine.block.isValid(block)) {
                engine.block.setState(block = block, state = BlockState.Ready)
            }
        }
    }

    /**
     * Waits for [modelLock], and cancels the removal when [block] stops showing [source] before the lock is free, so a
     * waiting removal keeps no loading state on an image that the user replaced.
     */
    private suspend fun EditorScope.lockUnlessReplaced(
        modelLock: Mutex,
        block: DesignBlock,
        source: ImageSource,
    ) {
        val engine = editorContext.engine
        val removal = currentCoroutineContext().job
        val watcher = editorContext.coroutineScope.launch {
            engine.event.subscribe(listOf(block, source.fill)).first { currentImage(engine, block, source) == null }
            removal.cancel()
        }
        try {
            modelLock.lock()
        } finally {
            watcher.cancel()
        }
    }

    /**
     * Runs the model on the image of [block] and writes the result as PNG files. Returns `null` when the block shows
     * another image than [source]: the user can delete the block or replace its image during each suspension, so the
     * image is read again before the model and before the encoding.
     */
    private suspend fun EditorScope.saveMaskedImage(
        block: DesignBlock,
        source: ImageSource,
        config: BackgroundRemovalConfig,
    ): Pair<ImageSource, ImageProcessor.ImageFiles>? {
        val engine = editorContext.engine
        val image = currentImage(engine, block, source) ?: return null
        val sourceBitmap = measureAndGet(step = "loadImage") { loadBitmap(image.uri, config) }
        val maskedBitmap = try {
            val mask = measureAndGet(step = "processMask") {
                with(config.remover) { processImage(bitmap = sourceBitmap) }
            }
            measureAndGet(step = "applyMask") {
                ImageProcessor.applyMaskToBitmap(srcBitmap = sourceBitmap, mask = mask)
            }
        } finally {
            sourceBitmap.recycle()
        }
        try {
            val current = currentImage(engine, block, source) ?: return null
            val files = measureAndGet(step = "savePNGs") {
                ImageProcessor.savePngs(
                    bitmap = maskedBitmap,
                    sourceSet = current.sourceSet,
                    context = editorContext.activity,
                )
            }
            return current to files
        } finally {
            maskedBitmap.recycle()
        }
    }

    private class ImageSource(
        val fill: DesignBlock,
        val uri: Uri,
        val imageFileUri: String,
        val sourceSet: List<Source>,
    )

    private fun imageSource(
        engine: Engine,
        block: DesignBlock,
    ): ImageSource? {
        if (!engine.block.supportsFill(block)) {
            return null
        }
        val fill = engine.block.getFill(block)
        if (engine.block.getType(fill) != FillType.Image.key) {
            return null
        }
        val sourceSet = engine.block.getSourceSet(fill, SOURCE_SET)
        val imageFileUri = engine.block.getString(fill, IMAGE_FILE_URI)
        // The engine draws the source set when the fill has one, and the image file otherwise.
        val uri = sourceSet.maxByOrNull { it.width.toLong() * it.height }?.uri ?: Uri.parse(imageFileUri)
        if (uri.scheme?.lowercase() !in readableSchemes) {
            return null
        }
        // `BitmapFactory` cannot decode SVG.
        if (isSvg(engine, uri)) {
            return null
        }
        return ImageSource(fill = fill, uri = uri, imageFileUri = imageFileUri, sourceSet = sourceSet)
    }

    /**
     * The image of [block] with its current source set, or `null` when the block shows another image than [source]. The
     * drawn URI decides, so an entry that the engine adds to the source set still counts as the same image.
     */
    private fun currentImage(
        engine: Engine,
        block: DesignBlock,
        source: ImageSource,
    ): ImageSource? {
        if (!engine.block.isValid(block)) {
            return null
        }
        val current = runCatching { imageSource(engine, block) }.getOrNull() ?: return null
        return current.takeIf {
            it.fill == source.fill && it.imageFileUri == source.imageFileUri && it.uri == source.uri
        }
    }

    /**
     * An archive keeps each image in a buffer whose URI has no file extension, so the first bytes decide. A raster
     * format starts with a binary signature, and SVG starts with `<`, after an optional BOM and whitespace.
     */
    private fun isSvg(
        engine: Engine,
        uri: Uri,
    ): Boolean {
        if (uri.scheme?.lowercase() != "buffer") {
            return uri.path.orEmpty().endsWith(".svg", ignoreCase = true)
        }
        val head = engine.editor.getBufferData(uri = uri, offset = 0, length = minOf(64, engine.editor.getBufferLength(uri)))
        val skipped = setOf<Byte>(0x20, 0x09, 0x0A, 0x0D, 0xEF.toByte(), 0xBB.toByte(), 0xBF.toByte())
        while (head.hasRemaining()) {
            val byte = head.get()
            if (byte !in skipped) {
                return byte == '<'.code.toByte()
            }
        }
        return false
    }

    /**
     * Reads a `buffer://` image, such as one of a loaded archive, from the engine, and any other image through
     * [FileLoader].
     */
    private suspend fun EditorScope.loadBitmap(
        uri: Uri,
        config: BackgroundRemovalConfig,
    ): Bitmap {
        val bytes = if (uri.scheme?.lowercase() == "buffer") {
            val engine = editorContext.engine
            val data = engine.editor.getBufferData(uri = uri, offset = 0, length = engine.editor.getBufferLength(uri))
            ByteArray(data.remaining()).also { data.get(it) }
        } else {
            withContext(Dispatchers.IO) {
                FileLoader.loadUri(
                    context = editorContext.activity,
                    uri = uri,
                    httpClient = config.httpClient,
                ).use { it.readBytes() }
            }
        }
        val bitmap = withContext(Dispatchers.Default) { ImageProcessor.decodeUpright(bytes) }
        return requireNotNull(bitmap) { "Could not decode the image at $uri." }
    }

    /**
     * Writes [files] into [image]'s fill. `setSourceSet` takes the sizes as given, while `addImageFileURIToSourceSet`
     * reads them from the file and replaces by width. `setSourceSet` resets the crop and the
     * content fill mode of the block, as in the web plugin.
     */
    private fun commit(
        engine: Engine,
        image: ImageSource,
        files: ImageProcessor.ImageFiles,
    ) {
        engine.block.setString(image.fill, IMAGE_FILE_URI, files.image.toString())
        if (image.sourceSet.isNotEmpty()) {
            val sourceSet = image.sourceSet.zip(files.sourceSet) { original, uri ->
                Source(uri = uri, width = original.width, height = original.height)
            }
            engine.block.setSourceSet(image.fill, SOURCE_SET, sourceSet)
        }
        if (engine.block.getString(image.fill, PREVIEW_FILE_URI).isNotEmpty()) {
            engine.block.setString(image.fill, PREVIEW_FILE_URI, "")
        }
    }
}
