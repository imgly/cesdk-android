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
import ly.img.camera.components.Shadowed
import ly.img.camera.core.CameraConfiguration
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
    deletePreviousRecording: () -> Unit,
    setResult: () -> Unit,
) {
    Box(modifier = modifier) {
        val state = recordingManager.state

        AnimatedVisibility(
            visible = state.status is RecordingManager.Status.Idle && state.recordings.isNotEmpty(),
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
            remember(state.recordings.count(), (state.status as? RecordingManager.Status.Recording)?.currentRecordingDuration) {
                val recordings = state.recordings
                val currentRecordingDuration = (state.status as? RecordingManager.Status.Recording)?.currentRecordingDuration
                recordings.map { it.duration }.let { durations ->
                    currentRecordingDuration?.let { currentDuration ->
                        durations + currentDuration
                    } ?: durations
                }
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
            recordedDurations = recordedDurations,
            onShortPress = {
                recordingManager.toggleRecording(context)
            },
            onLongPress = {
                if (!recordingManager.hasStartedRecording) {
                    recordingManager.toggleRecording(context)
                }
            },
            onLongPressRelease = {
                recordingManager.toggleRecording(context)
            },
        )

        AnimatedVisibility(
            visible = state.status is RecordingManager.Status.Idle && state.recordings.isNotEmpty(),
            modifier = Modifier.align(Alignment.CenterEnd),
            enter = fadeIn() + slideInHorizontally(initialOffsetX = { it / 2 }),
            exit = fadeOut() + slideOutHorizontally(targetOffsetX = { it / 2 }),
        ) {
            NextButton(onClick = setResult)
        }
    }
}
