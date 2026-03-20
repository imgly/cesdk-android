package ly.img.editor.base.dock.options.voiceover

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import ly.img.editor.base.ui.Event
import ly.img.editor.core.EditorContext
import ly.img.editor.core.LocalEditorScope
import ly.img.editor.core.R
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.iconpack.Close
import ly.img.editor.core.iconpack.IconPack
import ly.img.editor.core.ui.iconpack.Muteotheraudio
import ly.img.editor.core.ui.iconpack.Muteotheraudiooff
import ly.img.editor.core.ui.permissions.PermissionManager.Companion.hasMicPermission
import ly.img.engine.DesignBlock
import ly.img.editor.core.ui.iconpack.IconPack as CoreUiIconPack

@Composable
internal fun VoiceoverOptionsSheet(
    uiState: VoiceoverUiState,
    onEvent: (EditorEvent) -> Unit,
) {
    val editorContext = with(LocalEditorScope.current) { editorContext }
    val controller = rememberVoiceoverRecordController()
    val coroutineScope = rememberCoroutineScope()
    val draftVoiceOverBlock = uiState.draftVoiceOverBlock
    var startAfterPermission by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted && startAfterPermission) {
            controller.startRecording(
                editorContext = editorContext,
                coroutineScope = coroutineScope,
            )
        } else if (!granted) {
            editorContext.eventHandler.send(
                Event.OnToast(R.string.ly_img_editor_dialog_permission_microphone_title),
            )
        }
        startAfterPermission = false
    }

    DisposableEffect(editorContext, draftVoiceOverBlock) {
        controller.bind(draftVoiceOverBlock)
        controller.onStopCompleted = {
            closeVoiceoverOptionsSheet(onEvent)
        }
        onDispose {
            controller.onStopCompleted = null
            editorContext.updateVoiceOverSheetTarget(null)
            controller.handleSheetDisposed(
                editorContext = editorContext,
                coroutineScope = coroutineScope,
                selectionToRestore = uiState.previousSelectedBlocks,
            )
        }
    }

    if (!VoiceoverEngineBlocks.isValidBlock(editorContext.engine, draftVoiceOverBlock)) {
        LaunchedEffect(editorContext) {
            closeVoiceoverOptionsSheet(onEvent)
        }
        return
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxWidth()
            .height(voiceOverRecordSheetHeight),
    ) {
        val recordButtonWidth = if (controller.isRecording) {
            voiceOverRecordButtonRecordingWidth
        } else {
            voiceOverRecordButtonIdleWidth
        }
        val horizontalPadding = if (maxWidth >= voiceOverRecordSheetFigmaWidth) {
            voiceOverRecordSheetHorizontalPadding
        } else {
            voiceOverRecordSheetCompactHorizontalPadding
        }
        val availableActionWidth = ((maxWidth - horizontalPadding * 2 - recordButtonWidth) / 2)
            .coerceAtLeast(voiceOverSheetActionMinWidth)
        val actionWidth = if (maxWidth >= voiceOverRecordSheetFigmaWidth) {
            availableActionWidth.coerceIn(voiceOverSheetActionWidth, voiceOverSheetActionExpandedWidth)
        } else {
            availableActionWidth
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(voiceOverRecordSheetControlsHeight)
                .padding(
                    start = horizontalPadding,
                    top = voiceOverRecordSheetTopInset,
                    end = horizontalPadding,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            VoiceOverSheetBarButton(
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight(),
                icon = IconPack.Close,
                label = stringResource(R.string.ly_img_editor_dialog_close_confirm_button_dismiss),
                enabled = !controller.isSaving,
                onClick = {
                    cancelVoiceoverOptionsSheet(
                        editorContext = editorContext,
                        controller = controller,
                        previousSelectedBlocks = uiState.previousSelectedBlocks,
                        coroutineScope = coroutineScope,
                        onEvent = onEvent,
                    )
                },
            )

            VoiceOverRecordActionButton(
                modifier = Modifier
                    .fillMaxHeight(),
                isRecording = controller.isRecording,
                isSaving = controller.isSaving,
                recordedDurationMs = controller.elapsedRecordingMs,
                onClick = {
                    if (controller.isRecording) {
                        controller.stopAndPersist(
                            editorContext = editorContext,
                            coroutineScope = coroutineScope,
                        )
                    } else if (editorContext.activity.hasMicPermission()) {
                        controller.startRecording(
                            editorContext = editorContext,
                            coroutineScope = coroutineScope,
                        )
                    } else {
                        startAfterPermission = true
                        permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                    }
                },
            )

            VoiceOverSheetBarButton(
                modifier = Modifier
                    .width(actionWidth)
                    .fillMaxHeight(),
                icon = if (controller.muteOtherAudio) CoreUiIconPack.Muteotheraudiooff else CoreUiIconPack.Muteotheraudio,
                label = if (controller.muteOtherAudio) {
                    stringResource(R.string.ly_img_editor_sheet_voiceover_button_unmute)
                } else {
                    stringResource(R.string.ly_img_editor_sheet_voiceover_button_mute)
                },
                iconSize = 18.dp,
                enabled = !controller.isSaving,
                onClick = {
                    controller.toggleMuteOtherAudio(editorContext)
                },
            )
        }
    }
}

private fun closeVoiceoverOptionsSheet(onEvent: (EditorEvent) -> Unit) {
    onEvent(EditorEvent.Sheet.Close(animate = true))
}

private fun cancelVoiceoverOptionsSheet(
    editorContext: EditorContext,
    controller: VoiceoverRecordController,
    previousSelectedBlocks: List<DesignBlock>,
    coroutineScope: CoroutineScope,
    onEvent: (EditorEvent) -> Unit,
) {
    controller.cancelAndRestoreSelection(
        editorContext = editorContext,
        coroutineScope = coroutineScope,
        selectionToRestore = previousSelectedBlocks,
    )
    closeVoiceoverOptionsSheet(onEvent)
}
