package ly.img.editor.base.engine

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.view.PixelCopy
import android.view.TextureView
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.coroutines.CoroutineContext

class PageSnapshotTest {
    @Test
    fun cancellationBeforeCallbackKeepsTheBufferAliveUntilCopyCompletes() = onMain {
        val bitmap = bitmap()
        val dispatcher = QueuedDispatcher()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        lateinit var listener: PixelCopy.OnPixelCopyFinishedListener
        try {
            val job = scope.launch { awaitPixelCopy(bitmap) { listener = it } }
            dispatcher.runAll()
            job.cancel()
            dispatcher.runAll()
            assertFalse("PixelCopy may still write to the buffer", bitmap.isRecycled)
            listener.onPixelCopyFinished(PixelCopy.SUCCESS)
            assertTrue(bitmap.isRecycled)
        } finally {
            scope.cancel()
            bitmap.recycle()
        }
    }

    @Test
    fun cancellationAfterCallbackButBeforeDispatchRecyclesUndeliveredBitmap() = onMain {
        val bitmap = bitmap()
        val dispatcher = QueuedDispatcher()
        val scope = CoroutineScope(SupervisorJob() + dispatcher)
        lateinit var listener: PixelCopy.OnPixelCopyFinishedListener
        var delivered = false
        try {
            val job = scope.launch {
                awaitPixelCopy(bitmap) { listener = it }
                delivered = true
            }
            dispatcher.runAll()
            listener.onPixelCopyFinished(PixelCopy.SUCCESS)
            job.cancel()
            dispatcher.runAll()
            assertFalse(delivered)
            assertTrue(bitmap.isRecycled)
        } finally {
            scope.cancel()
            bitmap.recycle()
        }
    }

    @Test
    fun timeoutLeavesCleanupToTheLateCallback() = onMain {
        val bitmap = bitmap()
        lateinit var listener: PixelCopy.OnPixelCopyFinishedListener
        try {
            val result = async { awaitPixelCopy(bitmap) { listener = it } }.await()
            assertNull(result)
            assertFalse(bitmap.isRecycled)
            listener.onPixelCopyFinished(PixelCopy.SUCCESS)
            assertTrue(bitmap.isRecycled)
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun successTransfersAnUnrecycledBitmapToTheCaller() = onMain {
        val bitmap = bitmap()
        try {
            val result = awaitPixelCopy(bitmap) { it.onPixelCopyFinished(PixelCopy.SUCCESS) }
            assertSame(bitmap, result)
            assertFalse(bitmap.isRecycled)
        } finally {
            bitmap.recycle()
        }
    }

    @Test
    fun failedCopyAndRejectedRequestReleaseTheirBuffers() = onMain {
        val failed = bitmap()
        assertNull(awaitPixelCopy(failed) { it.onPixelCopyFinished(PixelCopy.ERROR_SOURCE_NO_DATA) })
        assertTrue(failed.isRecycled)
        val rejected = bitmap()
        assertNull(awaitPixelCopy(rejected) { throw IllegalArgumentException("Surface destroyed") })
        assertTrue(rejected.isRecycled)
    }

    @Test
    fun oversizedTextureIsDownsampledBeforeCroppingAndReleasesTheIntermediateBuffer() = onMain {
        val view = RecordingTexture(context)
        view.layout(0, 0, 4096, 2048)
        val result = copySurface(view, Rect(2048, 1024, 4096, 2048), 2048, 1024)
        try {
            assertEquals(2048, view.buffer.width)
            assertEquals(1024, view.buffer.height)
            assertEquals(1024, result?.width)
            assertEquals(512, result?.height)
            assertTrue(view.buffer.isRecycled)
            assertFalse(result!!.isRecycled)
        } finally {
            result?.recycle()
        }
    }

    @Test
    fun wholeTextureCropTransfersTheOriginalBufferWithoutRecyclingIt() = onMain {
        val view = RecordingTexture(context)
        view.layout(0, 0, 100, 50)
        val result = copySurface(view, Rect(0, 0, 100, 50), 100, 50)
        try {
            assertSame(view.buffer, result)
            assertFalse(view.buffer.isRecycled)
        } finally {
            result?.recycle()
        }
    }

    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private fun bitmap() = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888)

    private fun onMain(block: suspend CoroutineScope.() -> Unit) = runBlocking(Dispatchers.Main.immediate, block)

    private class RecordingTexture(
        context: Context,
    ) : TextureView(context) {
        lateinit var buffer: Bitmap

        override fun getBitmap(
            width: Int,
            height: Int,
        ): Bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888).also {
            buffer = it
        }
    }
}

internal class QueuedDispatcher : CoroutineDispatcher() {
    private val tasks = ArrayDeque<Runnable>()

    override fun dispatch(
        context: CoroutineContext,
        block: Runnable,
    ) {
        tasks.addLast(block)
    }

    fun runAll() {
        while (tasks.isNotEmpty()) {
            tasks.removeFirst().run()
        }
    }
}
