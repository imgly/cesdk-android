package ly.img.camera.components

import androidx.activity.compose.BackHandler
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOut
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import ly.img.camera.CameraViewModel
import ly.img.camera.components.sidemenu.SideMenu
import ly.img.camera.core.CaptureType
import ly.img.camera.core.R
import ly.img.camera.preview.CameraEnginePreview
import ly.img.camera.record.RecordingManager
import ly.img.camera.record.Timer
import ly.img.camera.record.components.CameraDock
import ly.img.camera.record.components.DeleteAllRecordingsDialog
import ly.img.editor.core.theme.LocalExtendedColorScheme
import ly.img.editor.core.ui.utils.activity
import ly.img.editor.core.ui.utils.formatForClip
import ly.img.editor.core.ui.utils.lifecycle.LifecycleEventEffect

@Composable
internal fun BoxScope.CameraView(
    modifier: Modifier,
    cameraProvider: ProcessCameraProvider,
    viewModel: CameraViewModel,
) {
    val cameraState = viewModel.cameraState
    val recordingManager = viewModel.recordingManager
    val context = LocalContext.current

    val previewing = viewModel.previewingPhotoUri != null
    val capturingPhoto = recordingManager.state.status is RecordingManager.Status.TakingPhoto
    val behavesAsPhoto = viewModel.activeCaptureBehavesAsPhoto

    // Hide the chrome instantly when capture starts so the white flash doesn't reveal it fading
    // away underneath; restore with a smooth animation once the capture/preview window ends.
    val chromeAlphaAnimatable = remember { Animatable(1f) }
    LaunchedEffect(capturingPhoto, previewing) {
        if (capturingPhoto || previewing) {
            chromeAlphaAnimatable.snapTo(0f)
        } else {
            chromeAlphaAnimatable.animateTo(1f, tween(durationMillis = 300))
        }
    }
    val chromeAlpha = chromeAlphaAnimatable.value

    Column(modifier = modifier) {
        CameraEnginePreview(
            engine = viewModel.engine,
            cameraProvider = cameraProvider,
            cameraState = cameraState,
            setupLayout = viewModel::setupLayout,
            setCameraPreview = viewModel::setCameraPreview,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
        )

        // Faded during preview so the bottom band looks empty.
        Box(modifier = Modifier.alpha(chromeAlpha)) {
            CameraControls(
                isCameraReady = cameraState.isReady,
                isFlashEnabled = cameraState.isFlashEnabled,
                isFlashOn = cameraState.cameraFlash,
                isSwappingAllowed = viewModel.cameraLayoutMode != null &&
                    !recordingManager.hasStartedRecording &&
                    recordingManager.hasNotRecordedYet,
                showPhotoVideoToggle = viewModel.cameraConfiguration.captureType == CaptureType.Mixed &&
                    recordingManager.state.status is RecordingManager.Status.Idle,
                activeMixedSubMode = viewModel.activeMixedSubMode,
                behavesAsPhoto = behavesAsPhoto,
                toggleFlash = viewModel::toggleFlash,
                toggleCamera = viewModel::toggleCamera,
                swapLayoutPositions = viewModel::swapLayoutPositions,
                onSubModeChange = viewModel::selectActiveMixedSubMode,
            )
        }
    }

    // Match the engine's bottom-aligned 9:16 letterbox (see `setupLayout`) so the preview
    // sits in the exact rectangle the live camera does. Bottom padding clears the dock band.
    viewModel.previewingPhotoUri?.let { uri ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 84.dp),
            contentAlignment = Alignment.BottomCenter,
        ) {
            val videoSize = viewModel.cameraConfiguration.videoSize
            PhotoPreview(
                uri = uri,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(videoSize.width / videoSize.height),
            )
        }
    }

    var showCloseDialog by remember { mutableStateOf(false) }
    val isCloseConfirmationRequired by remember {
        derivedStateOf {
            recordingManager.hasStartedRecording ||
                recordingManager.state.captures.isNotEmpty()
        }
    }

    fun close() {
        if (isCloseConfirmationRequired) {
            showCloseDialog = true
        } else {
            viewModel.discardPhotoPreview() // orphan-clean any unconfirmed photo
            checkNotNull(context.activity).finish()
        }
    }

    if (showCloseDialog) {
        DeleteAllRecordingsDialog(
            onDismiss = { showCloseDialog = false },
            onConfirm = {
                showCloseDialog = false
                viewModel.discardPhotoPreview()
                recordingManager.close()
                checkNotNull(context.activity).finish()
            },
        )
    }

    // System back: discard the preview if open, otherwise fall through to the close path.
    BackHandler(enabled = previewing, onBack = viewModel::discardPhotoPreview)
    BackHandler(enabled = !previewing && isCloseConfirmationRequired, onBack = ::close)

    LifecycleEventEffect(event = Lifecycle.Event.ON_STOP) {
        recordingManager.stop()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 84.dp)
            .alpha(chromeAlpha)
            .pointerInput(previewing) {
                if (previewing) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false).consume()
                    }
                }
            },
    ) {
        Toolbar(
            isRecording = recordingManager.hasStartedRecording,
            duration = recordingManager.state.totalRecordedDuration,
            maxDuration = recordingManager.state.maxDuration,
            recordingColor = viewModel.cameraConfiguration.recordingColor,
            // Pure-photo captures have no meaningful recording duration.
            showTimecode = viewModel.cameraConfiguration.captureType != CaptureType.Photo,
            onCloseClick = ::close,
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
        ) {
            // Stay rendered through the photo capture + preview window so the slide-in transition
            // doesn't re-fire every time we briefly leave Idle for a photo (relevant for rapid-fire
            // multi-take with `showsPhotoPreview = false`). Chrome alpha already hides it visually.
            this@Column.AnimatedVisibility(
                visible = cameraState.isReady &&
                    (
                        recordingManager.state.status is RecordingManager.Status.Idle ||
                            recordingManager.state.status is RecordingManager.Status.TakingPhoto ||
                            viewModel.previewingPhotoUri != null
                    ),
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(vertical = 32.dp, horizontal = 12.dp),
                enter = fadeIn() + slideInHorizontally(),
                exit = fadeOut() + slideOutHorizontally(),
            ) {
                SideMenu(
                    timer = recordingManager.state.timer,
                    setTimer = recordingManager::setTimer,
                    layoutMode = viewModel.cameraLayoutMode,
                    setLayoutMode = viewModel::updateLayoutMode,
                    layoutModeEnabled = recordingManager.hasNotRecordedYet,
                )
            }

            val state = recordingManager.state
            if (state.timer != Timer.Off) {
                CountdownTimerView(
                    modifier = Modifier.align(Alignment.Center),
                    recordingColor = viewModel.cameraConfiguration.recordingColor,
                    recordingStatus = state.status,
                )
            }

            this@Column.AnimatedVisibility(
                visible = state.hasReachedMaxDuration,
                modifier = Modifier
                    .align(Alignment.BottomCenter),
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it / 2 }),
            ) {
                Shadowed {
                    Text(
                        text = stringResource(
                            id = R.string.ly_img_camera_label_recording_limit,
                            recordingManager.state.maxDuration.formatForClip(),
                        ),
                        style = MaterialTheme.typography.titleMedium,
                        color = LocalExtendedColorScheme.current.white,
                    )
                }
            }
        }

        CameraDock(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 8.dp),
            cameraState = cameraState,
            recordingManager = recordingManager,
            cameraConfiguration = viewModel.cameraConfiguration,
            activeMixedSubMode = viewModel.activeMixedSubMode,
            deletePreviousRecording = viewModel::deletePreviousRecording,
            capturePhoto = viewModel::capturePhoto,
            setResult = viewModel::finishCapturing,
        )
    }

    // Replaces the bottom band (dock + controls) while the preview is up.
    if (previewing) {
        PhotoPreviewActions(
            onBack = viewModel::discardPhotoPreview,
            onDone = viewModel::confirmPhotoPreview,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
        )
    }

    // White flash on photo capture — instant in. On transition to preview the overlay fades
    // 350 ms ease-out; on direct return to ready (e.g. `showsPhotoPreview = false` multi-take)
    // it snaps off instantly so the rapid-fire flow doesn't drag a fade between shots.
    AnimatedVisibility(
        visible = viewModel.isFlashing,
        enter = EnterTransition.None,
        exit = if (previewing) {
            fadeOut(animationSpec = tween(durationMillis = 350, easing = EaseOut))
        } else {
            ExitTransition.None
        },
    ) {
        Box(modifier = Modifier.fillMaxSize().background(Color.White))
    }
}
