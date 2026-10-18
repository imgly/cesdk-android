package ly.img.camera.record.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ly.img.camera.ActiveMixedSubMode
import ly.img.camera.components.Shadowed
import ly.img.camera.core.CameraConfiguration
import ly.img.camera.core.Capture
import ly.img.camera.core.CaptureCount
import ly.img.camera.core.CaptureType
import ly.img.camera.core.R
import ly.img.camera.preview.CameraState
import ly.img.camera.record.RecordingManager
import ly.img.editor.core.theme.LocalExtendedColorScheme
import ly.img.editor.core.ui.iconpack.Backspace
import ly.img.editor.core.ui.iconpack.IconPack

@Composable
internal fun CameraDock(
    modifier: Modifier = Modifier,
    cameraState: CameraState,
    recordingManager: RecordingManager,
    cameraConfiguration: CameraConfiguration,
    activeMixedSubMode: ActiveMixedSubMode,
    deletePreviousRecording: () -> Unit,
    capturePhoto: () -> Unit,
    setResult: () -> Unit,
) {
    val isSingleTake = cameraConfiguration.captureCount == CaptureCount.Single
    // Collapse the (CaptureType, ActiveMixedSubMode) pair into a single Photo/Video routing
    // decision so the shutter callbacks below stay branch-free. Mixed mode reads the toggle
    // state; pure Photo / Video ignore it.
    val behavesAsPhoto = when (cameraConfiguration.captureType) {
        CaptureType.Photo -> true
        CaptureType.Video -> false
        CaptureType.Mixed -> activeMixedSubMode == ActiveMixedSubMode.Photo
    }

    Box(modifier = modifier) {
        val state = recordingManager.state

        AnimatedVisibility(
            // Single-take auto-finishes, so neither the delete-last button nor the
            // next-button should appear regardless of stack state. Stay rendered through
            // TakingPhoto so rapid-fire multi captures don't slide the buttons out and back
            // in between each shot — chrome alpha already hides them visually during the flash.
            visible = !isSingleTake &&
                (
                    state.status is RecordingManager.Status.Idle ||
                        state.status is RecordingManager.Status.TakingPhoto
                ) &&
                state.captures.isNotEmpty(),
            modifier = Modifier
                .align(Alignment.Center)
                .offset(x = (-80).dp),
            enter = fadeIn() + slideInHorizontally(),
            exit = fadeOut() + slideOutHorizontally(),
        ) {
            var showDeleteLastRecordingDialog by remember { mutableStateOf(false) }

            IconButton(
                colors = IconButtonDefaults.iconButtonColors(
                    contentColor = LocalExtendedColorScheme.current.white,
                ),
                onClick = {
                    showDeleteLastRecordingDialog = true
                },
            ) {
                Shadowed {
                    Icon(
                        IconPack.Backspace,
                        contentDescription = stringResource(
                            R.string.ly_img_camera_button_delete_last_recording,
                        ),
                    )
                }
            }

            if (showDeleteLastRecordingDialog) {
                DeleteLastRecordingDialog(
                    onDismiss = {
                        showDeleteLastRecordingDialog = false
                    },
                    onConfirm = {
                        showDeleteLastRecordingDialog = false
                        deletePreviousRecording()
                    },
                )
            }
        }

        val recordedDurations =
            remember(state.captures, (state.status as? RecordingManager.Status.Recording)?.currentRecordingDuration) {
                val captureDurations = state.captures.map { capture ->
                    when (capture) {
                        is Capture.Photo -> capture.clipDuration
                        is Capture.Video -> capture.recording.duration
                    }
                }
                val currentRecordingDuration = (state.status as? RecordingManager.Status.Recording)?.currentRecordingDuration
                currentRecordingDuration?.let { captureDurations + it } ?: captureDurations
            }

        val context = LocalContext.current

        RecordingButton(
            modifier = Modifier
                .align(Alignment.Center),
            recordingColor = cameraConfiguration.recordingColor,
            maxDuration = recordingManager.state.maxDuration,
            enabled = state.status != RecordingManager.Status.Disabled && cameraState.isReady && !state.hasReachedMaxDuration,
            hasStartedRecording = recordingManager.hasStartedRecording,
            isRecording = recordingManager.isRecording,
            isTimerRunning = state.status is RecordingManager.Status.TimerRunning,
            behavesAsPhoto = behavesAsPhoto,
            recordedDurations = recordedDurations,
            onShortPress = {
                if (behavesAsPhoto) {
                    capturePhoto()
                } else {
                    recordingManager.toggleRecording(context)
                }
            },
            onLongPress = {
                if (behavesAsPhoto) return@RecordingButton
                // Hold-to-record bypasses the timer; tap still respects it.
                recordingManager.startRecordingImmediately(context)
            },
            onLongPressRelease = {
                if (behavesAsPhoto) return@RecordingButton
                recordingManager.toggleRecording(context)
            },
        )

        AnimatedVisibility(
            // Single-take auto-finishes via SingleEvent.FinishCapturing, so no Next button.
            // Stay rendered through TakingPhoto so rapid-fire multi captures don't wiggle.
            visible = !isSingleTake &&
                (
                    state.status is RecordingManager.Status.Idle ||
                        state.status is RecordingManager.Status.TakingPhoto
                ) &&
                state.captures.isNotEmpty(),
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 2 }),
        ) {
            NextButton(onClick = setResult)
        }
    }
}
