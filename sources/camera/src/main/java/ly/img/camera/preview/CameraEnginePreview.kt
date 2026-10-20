package ly.img.camera.preview

import android.annotation.SuppressLint
import android.view.SurfaceView
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import ly.img.camera.components.rememberCameraDisplayRotation
import ly.img.editor.core.ui.utils.detectZoomGestures
import ly.img.engine.Engine

@SuppressLint("ClickableViewAccessibility")
@Composable
internal fun CameraEnginePreview(
    engine: Engine,
    cameraProvider: ProcessCameraProvider,
    cameraState: CameraState,
    setupLayout: suspend (Float, Float) -> Unit,
    setCameraPreview: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val displayRotation by rememberCameraDisplayRotation()

    LaunchedEffect(displayRotation) { cameraState.updatePreviewRotation(displayRotation) }

    val previewView = remember { SurfaceView(context) }

    fun bind() {
        if (engine.isBound().not()) {
            engine.bindSurfaceView(previewView)
        }
    }

    BoxWithConstraints(
        modifier = modifier.pointerInput(Unit) {
            detectZoomGestures(onZoom = { zoom -> cameraState.setZoomRatio(zoom) }, onZoomEnd = {})
        },
    ) {
        AndroidView(
            factory = { previewView },
        )

        LaunchedEffect(maxWidth, maxHeight) {
            setupLayout(maxWidth.value, maxHeight.value)
        }

        LaunchedEffect(cameraProvider, lifecycleOwner) {
            bind()
            cameraState.bind(cameraProvider, lifecycleOwner, scope)
            setCameraPreview()
        }

        DisposableEffect(engine) {
            val observer = LifecycleEventObserver { _, event ->
                if (engine.isEngineRunning().not()) return@LifecycleEventObserver
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        engine.editor.setAppIsPaused(true)
                        engine.pause()
                    }
                    Lifecycle.Event.ON_RESUME -> {
                        engine.unpause()
                        bind()
                        engine.editor.setAppIsPaused(false)
                    }
                    else -> {
                    }
                }
            }

            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                if (engine.isEngineRunning()) {
                    engine.unbind()
                }
            }
        }
    }
}
