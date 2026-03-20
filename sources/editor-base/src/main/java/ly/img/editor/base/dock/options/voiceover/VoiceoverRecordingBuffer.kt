package ly.img.editor.base.dock.options.voiceover

import android.net.Uri
import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import java.nio.ByteBuffer
import java.util.ArrayDeque

internal class VoiceoverRecordingBuffer {
    private var currentBufferUri: Uri? = null
    private var currentBufferLengthBytes: Int = 0
    private var currentBufferCapacityBytes: Int = 0
    private val pendingChunks = ArrayDeque<ByteArray>()
    private val pendingChunksLock = Any()

    fun attach(bufferUri: Uri) {
        currentBufferUri = bufferUri
        currentBufferLengthBytes = 0
        currentBufferCapacityBytes = 0
        synchronized(pendingChunksLock) {
            pendingChunks.clear()
        }
    }

    fun enqueueChunk(chunk: ByteArray) {
        if (chunk.isEmpty()) return
        synchronized(pendingChunksLock) {
            pendingChunks.addLast(chunk)
        }
    }

    fun flush(
        engine: Engine,
        targetBlock: DesignBlock,
    ) {
        if (!VoiceoverEngineBlocks.isValidBlock(engine, targetBlock)) return
        val bufferUri = currentBufferUri ?: return
        val chunks = synchronized(pendingChunksLock) {
            if (pendingChunks.isEmpty()) {
                emptyList()
            } else {
                buildList(pendingChunks.size) {
                    while (pendingChunks.isNotEmpty()) {
                        add(pendingChunks.removeFirst())
                    }
                }
            }
        }
        if (chunks.isEmpty()) return

        val totalSize = chunks.sumOf { it.size }
        val writeEnd = currentBufferLengthBytes + totalSize
        ensureCapacity(
            engine = engine,
            bufferUri = bufferUri,
            requiredLengthBytes = writeEnd,
        )
        val data = ByteBuffer.allocateDirect(totalSize).apply {
            chunks.forEach { put(it) }
            flip()
        }
        runCatching {
            engine.editor.setBufferData(
                uri = bufferUri,
                offset = currentBufferLengthBytes,
                data = data,
            )
            currentBufferLengthBytes = writeEnd
        }
    }

    fun clear(engine: Engine) {
        synchronized(pendingChunksLock) {
            pendingChunks.clear()
        }
        currentBufferUri?.let { VoiceoverFiles.destroyBufferQuietly(engine, it) }
        currentBufferUri = null
        currentBufferLengthBytes = 0
        currentBufferCapacityBytes = 0
    }

    private fun ensureCapacity(
        engine: Engine,
        bufferUri: Uri,
        requiredLengthBytes: Int,
    ) {
        if (requiredLengthBytes <= currentBufferCapacityBytes) return
        val newCapacityBytes =
            ((requiredLengthBytes + ENGINE_BUFFER_CAPACITY_GROWTH_BYTES - 1) / ENGINE_BUFFER_CAPACITY_GROWTH_BYTES) *
                ENGINE_BUFFER_CAPACITY_GROWTH_BYTES
        runCatching {
            engine.editor.setBufferLength(
                uri = bufferUri,
                length = newCapacityBytes,
            )
            currentBufferCapacityBytes = newCapacityBytes
        }
    }
}
