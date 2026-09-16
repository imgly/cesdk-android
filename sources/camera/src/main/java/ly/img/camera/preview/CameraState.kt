package ly.img.camera.preview

import android.util.Log
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.core.UseCase
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ly.img.camera.core.CaptureType

internal class CameraState(
    private val previewBuilder: Preview.Builder,
    private val videoCaptureBuilder: VideoCapture.Builder<Recorder>,
    private val imageCaptureBuilder: ImageCapture.Builder,
    private val captureType: CaptureType,
    startWithFrontCamera: Boolean,
) {
    val isFlashEnabled: Boolean
        get() = !showFrontCamera

    var showFrontCamera by mutableStateOf(startWithFrontCamera)
        private set

    private var _cameraFlash by mutableStateOf(false)
    var cameraFlash: Boolean
        get() = _cameraFlash
        private set(value) {
            if (_cameraFlash == value) return
            _cameraFlash = value
        }

    var isReady by mutableStateOf(false)

    private var camera: Camera? = null
    private var cameraProvider: ProcessCameraProvider? = null
    private var lifecycleOwner: LifecycleOwner? = null

    lateinit var previewUseCase: Preview

    /**
     * Built only when [captureType] != [CaptureType.Photo]. Null in photo-only sessions where the
     * `Recorder` use case isn't needed.
     */
    var videoCaptureUseCase: VideoCapture<Recorder>? = null
        private set

    /**
     * Only populated when [captureType] != [CaptureType.Video] AND the device supports the
     * (Preview + VideoCapture + ImageCapture) combination. Otherwise `null` so callers
     * (e.g. `PhotoCapture`) can fail loud instead of triggering a runtime CameraX exception.
     */
    var imageCaptureUseCase: ImageCapture? = null
        private set

    fun toggleCamera() {
        showFrontCamera = !showFrontCamera
        if (showFrontCamera) {
            cameraFlash = false
        }
        rebind()
    }

    fun toggleFlash(behavesAsPhoto: Boolean) {
        cameraFlash = !cameraFlash
        applyFlashState(behavesAsPhoto)
    }

    fun applyFlashState(behavesAsPhoto: Boolean) {
        // Video mode keeps the continuous-LED torch behavior for preview lighting.
        // Photo / Mixed mode applies a true flash at takePicture time via flashMode.
        if (behavesAsPhoto) {
            camera?.cameraControl?.enableTorch(false)
            imageCaptureUseCase?.flashMode = if (cameraFlash) ImageCapture.FLASH_MODE_ON else ImageCapture.FLASH_MODE_OFF
        } else {
            imageCaptureUseCase?.flashMode = ImageCapture.FLASH_MODE_OFF
            camera?.cameraControl?.enableTorch(cameraFlash)
        }
    }

    fun bind(
        cameraProvider: ProcessCameraProvider,
        lifecycleOwner: LifecycleOwner,
        uiScope: CoroutineScope,
    ) {
        this.cameraProvider = cameraProvider
        this.lifecycleOwner = lifecycleOwner
        uiScope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                if (captureType != CaptureType.Mixed) {
                    applyFlashState(captureType == CaptureType.Photo)
                }
            }
        }
        initUseCases()
        rebind()
    }

    fun setZoomRatio(zoom: Float) {
        val zoomState = camera?.cameraInfo?.zoomState?.value ?: return
        val currentZoomRatio = zoomState.zoomRatio
        val newZoomRatio = currentZoomRatio * zoom
        val clampedZoomRatio = newZoomRatio.coerceIn(zoomState.minZoomRatio, zoomState.maxZoomRatio)
        camera?.cameraControl?.setZoomRatio(clampedZoomRatio)
    }

    private fun rebind() {
        val cameraProvider = cameraProvider ?: return
        val lifecycleOwner = lifecycleOwner ?: return
        cameraProvider.unbindAll()
        val cameraSelector = if (showFrontCamera) CameraSelector.DEFAULT_FRONT_CAMERA else CameraSelector.DEFAULT_BACK_CAMERA

        val allUseCases: Array<UseCase> = listOfNotNull(
            previewUseCase,
            videoCaptureUseCase,
            imageCaptureUseCase,
        ).toTypedArray()

        camera = try {
            cameraProvider.bindToLifecycle(lifecycleOwner, cameraSelector, *allUseCases)
        } catch (e: IllegalArgumentException) {
            if (imageCaptureUseCase == null) throw e
            // The device cannot host Preview + VideoCapture + ImageCapture at once. Drop photo and retry.
            Log.w(
                TAG,
                "Device does not support (Preview + VideoCapture + ImageCapture) — disabling photo capture.",
                e,
            )
            imageCaptureUseCase = null
            cameraProvider.unbindAll()
            cameraProvider.bindToLifecycle(
                lifecycleOwner,
                cameraSelector,
                *listOfNotNull(previewUseCase, videoCaptureUseCase).toTypedArray(),
            )
        }
    }

    private fun initUseCases() {
        val cameraProvider = cameraProvider ?: return
        val frontCameraInfo = cameraProvider.getCameraInfo(CameraSelector.DEFAULT_FRONT_CAMERA)
        val backCameraInfo = cameraProvider.getCameraInfo(CameraSelector.DEFAULT_BACK_CAMERA)

        val frontVideoCapabilities = Recorder.getVideoCapabilities(frontCameraInfo)
        val backVideoCapabilities = Recorder.getVideoCapabilities(backCameraInfo)
        val isVideoStabilisationSupported =
            frontVideoCapabilities.isStabilizationSupported && backVideoCapabilities.isStabilizationSupported

        val frontPreviewCapabilities = Preview.getPreviewCapabilities(frontCameraInfo)
        val backPreviewCapabilities = Preview.getPreviewCapabilities(backCameraInfo)
        val isPreviewStabilisationSupported =
            frontPreviewCapabilities.isStabilizationSupported && backPreviewCapabilities.isStabilizationSupported

        previewUseCase = previewBuilder.setPreviewStabilizationEnabled(isPreviewStabilisationSupported).build()
        videoCaptureUseCase = if (captureType == CaptureType.Photo) {
            null
        } else {
            videoCaptureBuilder.setVideoStabilizationEnabled(isVideoStabilisationSupported).build()
        }
        imageCaptureUseCase = if (captureType == CaptureType.Video) null else imageCaptureBuilder.build()
    }

    companion object {
        private const val TAG = "CameraState"
    }
}
