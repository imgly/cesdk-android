package ly.img.editor.plugin.backgroundRemoval.api

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.core.net.toUri
import ly.img.editor.core.EditorScope
import ly.img.editor.plugin.backgroundRemoval.BackgroundRemovalConfig
import ly.img.editor.plugin.backgroundRemoval.util.FileLoader
import ly.img.editor.plugin.backgroundRemoval.util.ImageProcessor
import ly.img.editor.plugin.backgroundRemoval.util.measureAndGet
import ly.img.engine.BlockState
import ly.img.engine.DesignBlock
import ly.img.engine.FillType

internal object BackgroundRemovalApi {
    /**
     * Removes the background from an image block using ONNX Runtime segmentation.
     */
    suspend fun EditorScope.removeBackground(
        targetBlock: DesignBlock,
        config: BackgroundRemovalConfig,
    ) {
        val engine = editorContext.engine
        val pageFill = engine.block.getFill(targetBlock)
        val imageUri = engine.block.getString(block = pageFill, property = "fill/image/imageFileURI")
        engine.block.setState(block = targetBlock, state = BlockState.Pending(0f))
        var srcBitmap: Bitmap? = null
        var maskedBitmap: Bitmap? = null
        try {
            srcBitmap = measureAndGet(step = "loadImageUri") {
                FileLoader.loadUri(
                    context = editorContext.activity,
                    uri = imageUri.toUri(),
                    httpClient = config.httpClient,
                ).use { BitmapFactory.decodeStream(it) }
            }
            requireNotNull(srcBitmap)

            val mask = measureAndGet(step = "processMask") {
                with(config.remover) {
                    processImage(bitmap = srcBitmap)
                }
            }
            maskedBitmap = measureAndGet(step = "applyMask") {
                ImageProcessor.applyMaskToBitmap(
                    srcBitmap = srcBitmap,
                    mask = mask,
                )
            }
            val newUri = measureAndGet(step = "saveBitmap") {
                ImageProcessor.saveBitmapAsTempFile(
                    bitmap = maskedBitmap,
                    context = editorContext.activity,
                )
            }

            if (newUri != null) {
                engine.editor.addUndoStep()
                val newFill = engine.block.createFill(FillType.Image)
                engine.block.setString(
                    block = newFill,
                    property = "fill/image/imageFileURI",
                    value = newUri.toString(),
                )
                engine.block.setFill(block = targetBlock, fill = newFill)
            }
        } finally {
            engine.block.setState(block = targetBlock, state = BlockState.Ready)
            srcBitmap?.recycle()
            maskedBitmap?.recycle()
        }
    }
}
