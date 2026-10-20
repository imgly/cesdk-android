package ly.img.camera

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import ly.img.camera.components.CameraView
import ly.img.camera.components.VideoErrorDialog
import ly.img.camera.components.rememberCameraDisplayRotation
import ly.img.camera.components.rememberCameraIconRotation
import ly.img.camera.core.CaptureMedia
import ly.img.camera.core.CaptureType
import ly.img.camera.setup.CameraEngineInitializer
import ly.img.camera.setup.SetupView
import ly.img.camera.util.SingleEvent
import ly.img.editor.core.navbar.SystemNavBar
import ly.img.editor.core.theme.EditorTheme
import ly.img.editor.core.theme.LocalExtendedColorScheme
import ly.img.editor.core.ui.permissions.PermissionManager.Companion.hasCameraPermission
import ly.img.editor.core.ui.permissions.PermissionManager.Companion.hasMicPermission
import ly.img.engine.Engine

open class CameraActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Engine.init(application)
        setContent {
            val displayRotation by rememberCameraDisplayRotation()
            val targetRotation by rememberCameraIconRotation(displayRotation)
            val orientation = remember(targetRotation) { targetRotation.mod(360f).toInt() }
            val rotation = animateFloatAsState(
                targetValue = targetRotation,
                animationSpec = tween(durationMillis = 200),
                label = "Camera rotation",
            )
            EditorTheme(useDarkTheme = true) {
                val viewModel = viewModel<CameraViewModel>()
                LaunchedEffect(orientation, displayRotation) {
                    viewModel.updateCaptureOrientation(orientation, displayRotation)
                }

                // fail early
                val engineConfig = viewModel.engineConfiguration ?: run {
                    Log.i("CameraActivity", "EngineConfiguration not found in intent, finishing...")
                    finish()
                    return@EditorTheme
                }

                SystemNavBar(LocalExtendedColorScheme.current.black)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(LocalExtendedColorScheme.current.black)
                        .systemBarsPadding()
                        .background(MaterialTheme.colorScheme.surface),
                ) {
                    val context = LocalContext.current

                    val captureType = viewModel.cameraConfiguration.captureType
                    val micPermissionRequired = captureType != CaptureType.Photo

                    var hasRequiredPermissions by remember {
                        mutableStateOf(
                            (!micPermissionRequired || context.hasMicPermission()) && context.hasCameraPermission(),
                        )
                    }

                    var initResult by remember { mutableStateOf<Result<ProcessCameraProvider>?>(null) }
                    val cameraProvider = initResult?.getOrNull()

                    if (hasRequiredPermissions && cameraProvider != null) {
                        CameraView(
                            orientation = orientation,
                            rotation = { rotation.value },
                            modifier = Modifier.fillMaxSize(),
                            cameraProvider = cameraProvider,
                            viewModel = viewModel,
                        )
                    } else {
                        SetupView(
                            orientation = orientation,
                            rotation = { rotation.value },
                            modifier = Modifier.fillMaxSize(),
                            hasRequiredPermissions = hasRequiredPermissions,
                            isLoading = initResult == null,
                            error = initResult?.exceptionOrNull(),
                            requestOnlyCameraPermission = !micPermissionRequired,
                            onClose = ::finish,
                            onAllPermissionsGranted = {
                                hasRequiredPermissions = true
                            },
                        )
                    }

                    // While user is granting permissions,
                    // we use the time to get the camera and the engine ready in the background
                    CameraEngineInitializer(
                        engine = viewModel.engine,
                        engineConfiguration = engineConfig,
                        loadScene = viewModel::loadScene,
                        onResult = {
                            initResult = it
                        },
                    )
                }

                var showVideoErrorDialog by remember { mutableStateOf(false) }

                LaunchedEffect(Unit) {
                    viewModel.uiEvent.collect {
                        when (it) {
                            is SingleEvent.ErrorLoadingVideo -> showVideoErrorDialog = true
                            is SingleEvent.FinishCapturing -> {
                                val result = viewModel.getResult()
                                val intent = Intent().apply {
                                    putExtra(CaptureMedia.INTENT_KEY_CAMERA_RESULT, result)
                                }
                                setResult(Activity.RESULT_OK, intent)
                                finish()
                            }
                        }
                    }
                }

                if (showVideoErrorDialog) {
                    VideoErrorDialog(
                        orientation = orientation,
                        rotation = { rotation.value },
                        onClose = {
                            showVideoErrorDialog = false
                            finish()
                        },
                    )
                }
            }
        }
    }
}
