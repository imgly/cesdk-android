package ly.img.camera.record

import android.content.Context
import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.core.content.ContextCompat
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import java.text.SimpleDateFormat
import java.util.Locale
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Wraps the CameraX [ImageCapture] use case. Each [capture] writes a JPEG to the app's
 * files directory and resumes with the resulting file [Uri].
 *
 * Threading: must be called from the main thread (CameraX requirement). The callback
 * runs on the main executor.
 */
internal class PhotoCapture(
    private val imageCaptureProvider: () -> ImageCapture?,
    private val filesDirProvider: suspend () -> File,
) {
    suspend fun capture(context: Context): Uri {
        val imageCapture = imageCaptureProvider()
            ?: throw IllegalStateException("ImageCapture is not bound — check captureType / device capability.")
        val photoFile = createFile()
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()
        return suspendCancellableCoroutine { continuation ->
            imageCapture.takePicture(
                outputOptions,
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        val uri = output.savedUri ?: Uri.fromFile(photoFile)
                        continuation.resume(uri)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resumeWithException(exception)
                    }
                },
            )
            // CameraX has no cancel API for takePicture, so on cancellation (e.g. activity destroy
            // mid-shutter) the JPEG still lands on disk. Sweep it so the temp dir doesn't grow.
            continuation.invokeOnCancellation { photoFile.takeIf { it.exists() }?.delete() }
        }
    }

    private suspend fun createFile(): File {
        // Include milliseconds so rapid-fire multi-take photo sessions (preview disabled) don't collide.
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss_SSS", Locale.US).format(System.currentTimeMillis())
        val filesDir = filesDirProvider()
        return File(filesDir, "PHOTO_$timeStamp.jpg")
    }
}
