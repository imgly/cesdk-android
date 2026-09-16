package ly.img.editor.plugin.backgroundRemoval.remover

import ai.onnxruntime.NodeInfo
import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import ai.onnxruntime.TensorInfo
import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import androidx.core.graphics.scale
import androidx.core.net.toUri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ly.img.editor.core.EditorScope
import ly.img.editor.plugin.backgroundRemoval.BackgroundRemovalMask
import ly.img.editor.plugin.backgroundRemoval.IMGLYBackgroundRemovalConfig
import ly.img.editor.plugin.backgroundRemoval.util.BackgroundRemovalConstants
import ly.img.editor.plugin.backgroundRemoval.util.FileLoader
import ly.img.engine.internal.api.bitmap.BitmapNativeApi
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * ONNX Runtime based background remover that uses IMG.LY segmentation models.
 *
 * @param config IMG.LY background removal configuration.
 */
open class IMGLYBackgroundRemover(
    private val config: IMGLYBackgroundRemovalConfig,
) : BackgroundRemover<IMGLYBackgroundRemovalConfig> {
    private val environment: OrtEnvironment by lazy {
        OrtEnvironment.getEnvironment()
    }

    /**
     * Forces model file download outside editor scope.
     * This can be helpful if you want to have the model ready even before the editor is launched.
     */
    suspend fun forceDownloadModel(context: Context) {
        awaitModelFile(context = context)
    }

    /**
     * Starts loading the configured model when [IMGLYBackgroundRemovalConfig.loadMode] is eager.
     */
    override fun EditorScope.initialize() {
        editorContext.coroutineScope.launch {
            runCatching {
                if (config.loadMode == IMGLYBackgroundRemovalConfig.LoadMode.EAGER) {
                    awaitModelFile(context = editorContext.activity)
                }
            }.onFailure {
                Log.e(BackgroundRemovalConstants.TAG, "initialize failed", it)
            }
        }
    }

    /**
     * Runs the configured ONNX model for [bitmap] and returns its foreground mask.
     */
    override suspend fun EditorScope.processImage(bitmap: Bitmap): BackgroundRemovalMask = withContext(Dispatchers.Default) {
        awaitSession(context = editorContext.activity).use { session ->
            val inputName = session.inputNames.first()
            val inputInfo = session.inputInfo.getValue(inputName).tensorInfo
            val inputWidth = inputInfo.dimensionFromEnd(offset = 1) ?: FALLBACK_MASK_SIZE
            val inputHeight = inputInfo.dimensionFromEnd(offset = 2) ?: FALLBACK_MASK_SIZE
            val inputShape = longArrayOf(1L, 3L, inputHeight.toLong(), inputWidth.toLong())
            val inputBitmap = bitmap.scale(inputWidth, inputHeight)
            try {
                val inputTensor = OnnxTensor.createTensor(
                    environment,
                    BitmapNativeApi.createChannelsFirstBuffer(inputBitmap).asFloatBuffer(),
                    inputShape,
                )
                val outputName = session.outputNames.first()
                val outputShape = longArrayOf(1L, 1L, inputHeight.toLong(), inputWidth.toLong())
                val outputBuffer = ByteBuffer
                    .allocateDirect(inputWidth * inputHeight * Float.SIZE_BYTES)
                    .order(ByteOrder.nativeOrder())
                val outputTensor = OnnxTensor.createTensor(
                    environment,
                    outputBuffer.asFloatBuffer(),
                    outputShape,
                )
                try {
                    session.run(
                        mapOf(inputName to inputTensor),
                        mapOf(outputName to outputTensor),
                    ).use {
                        BackgroundRemovalMask(
                            buffer = outputBuffer,
                            width = inputWidth,
                            height = inputHeight,
                        )
                    }
                } finally {
                    inputTensor.close()
                    outputTensor.close()
                }
            } finally {
                if (inputBitmap !== bitmap) {
                    inputBitmap.recycle()
                }
            }
        }
    }

    private suspend fun awaitSession(context: Context): OrtSession {
        val file = awaitModelFile(context)
        return environment.createSession(
            file.absolutePath,
            OrtSession.SessionOptions().apply {
                setOptimizationLevel(OrtSession.SessionOptions.OptLevel.ALL_OPT)
                setExecutionMode(OrtSession.SessionOptions.ExecutionMode.SEQUENTIAL)
                setCPUArenaAllocator(false)
                setMemoryPatternOptimization(true)
            },
        )
    }

    private suspend fun awaitModelFile(context: Context): File = modelLoadMutex.withLock {
        val fileName = config.model.key
        withContext(Dispatchers.IO) {
            val modelFile = File(context.filesDir, "ly.img.editor/model/$fileName")
            val modelTempFile = File(modelFile.parentFile, modelFile.name + ".temp")
            modelFile.parentFile?.mkdirs()
            if (modelFile.exists()) return@withContext modelFile
            val modelUri = "${config.modelBaseUri}/$fileName".toUri()
            FileLoader.loadUri(
                context = context,
                uri = modelUri,
                httpClient = config.httpClient,
            ).use { inputStream ->
                modelTempFile.outputStream().use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            modelTempFile.renameTo(modelFile)
            modelFile
        }
    }

    private val NodeInfo.tensorInfo: TensorInfo
        get() = info as TensorInfo

    private fun TensorInfo.dimensionFromEnd(offset: Int): Int? {
        val shape = shape
        val value = shape.getOrNull(shape.size - offset) ?: return null
        return value.takeIf { it > 0 }?.toInt()
    }

    private companion object {
        const val FALLBACK_MASK_SIZE: Int = 1024
        private val modelLoadMutex: Mutex = Mutex()
    }
}
