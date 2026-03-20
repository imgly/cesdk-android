package ly.img.editor.base.dock.options.voiceover

import android.net.Uri
import android.os.SystemClock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ly.img.editor.base.ui.Event
import ly.img.editor.core.EditorContext
import ly.img.editor.core.ui.engine.getBackgroundTrack
import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import java.io.File
import kotlin.math.max
import kotlin.math.min
import ly.img.editor.core.R as CoreR

@Composable
internal fun rememberVoiceoverRecordController(): VoiceoverRecordController = remember { VoiceoverRecordController() }

internal class VoiceoverRecordController {
    private val recorder = VoiceOverRecordSegmentRecorder()
    private var voiceOverBlock by mutableStateOf<DesignBlock?>(null)
    private var recordingStartCursorMs by mutableLongStateOf(0L)
    private var recordingStartRealtimeMs by mutableLongStateOf(0L)
    private var recordingPlaybackEndSeconds: Double = 0.0
    private var recordingExtendsPageDuration: Boolean = false
    private var recordingPage: DesignBlock? = null
    private var currentSegmentFile by mutableStateOf<File?>(null)
    private val recordingBuffer = VoiceoverRecordingBuffer()
    private var recordingPageWasLooping: Boolean? = null
    private var recordingMutedBlocks: Map<DesignBlock, Boolean> = emptyMap()
    private var recordingTargetWasMuted: Boolean? = null
    private var saveJob: Job? = null
    private var progressJob: Job? = null
    private var shouldAbortOnDispose: Boolean = true
    var onStopCompleted: (() -> Unit)? = null
    var muteOtherAudio by mutableStateOf(true)
        private set
    var elapsedRecordingMs by mutableLongStateOf(0L)
        private set
    var isSaving by mutableStateOf(false)
        private set
    var isRecording by mutableStateOf(false)
        private set

    fun bind(voiceOverBlock: DesignBlock) {
        this.voiceOverBlock = voiceOverBlock
        shouldAbortOnDispose = true
    }

    fun toggleMuteOtherAudio(editorContext: EditorContext) {
        muteOtherAudio = !muteOtherAudio
        val page = recordingPage ?: return
        updateMutedPlaybackAudio(
            engine = editorContext.engine,
            page = page,
        )
    }

    fun startRecording(
        editorContext: EditorContext,
        coroutineScope: CoroutineScope,
    ) {
        if (isRecording || isSaving) return
        coroutineScope.launch {
            val engine = editorContext.engine
            val page = engine.scene.getCurrentPage() ?: return@launch
            val targetBlock = voiceOverBlock ?: return@launch
            if (!VoiceoverEngineBlocks.isValidBlock(engine, targetBlock) ||
                VoiceoverEngineBlocks.hasCommittedAudioResource(engine, targetBlock)
            ) {
                return@launch
            }

            val segmentFile = withContext(Dispatchers.IO) {
                File(
                    editorContext.activity.cacheDir,
                    "voiceover-segment-${System.currentTimeMillis()}.pcm",
                ).apply {
                    parentFile?.mkdirs()
                }
            }
            val bufferUri = runCatching { engine.editor.createBuffer() }
                .getOrElse {
                    withContext(Dispatchers.IO) {
                        segmentFile.delete()
                    }
                    editorContext.eventHandler.send(Event.OnError(it))
                    return@launch
                }
            val started = recorder.start(
                scope = coroutineScope,
                outputFile = segmentFile,
                onChunkRecorded = { chunk ->
                    recordingBuffer.enqueueChunk(
                        VoiceoverAudioCodec.toStereoFloatPcmChunk(chunk),
                    )
                },
                onRecorderError = { error ->
                    coroutineScope.launch {
                        handleRecorderFailure(
                            editorContext = editorContext,
                            coroutineScope = coroutineScope,
                            error = error,
                        )
                    }
                },
            )
            if (!started) {
                editorContext.eventHandler.send(
                    Event.OnToast(CoreR.string.ly_img_editor_dialog_permission_microphone_title),
                )
                withContext(Dispatchers.IO) {
                    segmentFile.delete()
                }
                VoiceoverFiles.destroyBufferQuietly(engine, bufferUri)
                return@launch
            }

            recordingStartCursorMs = (engine.block.getPlaybackTime(page) * 1000.0).toLong().coerceAtLeast(0L)
            recordingStartRealtimeMs = SystemClock.elapsedRealtime()
            elapsedRecordingMs = 0L
            currentSegmentFile = segmentFile
            recordingBuffer.attach(bufferUri)
            recordingPage = page
            val hasBackgroundTrack = runCatching { engine.getBackgroundTrack() != null }.getOrDefault(false)
            recordingExtendsPageDuration = !hasBackgroundTrack
            recordingPlaybackEndSeconds = if (hasBackgroundTrack) {
                runCatching { engine.block.getDuration(page) }.getOrDefault(0.0).coerceAtLeast(0.0)
            } else {
                0.0
            }
            runCatching { engine.block.setTimeOffset(targetBlock, recordingStartCursorMs / 1000.0) }
            runCatching { engine.block.setDuration(targetBlock, 0.0) }
            runCatching { engine.block.setString(targetBlock, "audio/fileURI", bufferUri.toString()) }
            recordingTargetWasMuted = runCatching { engine.block.isMuted(targetBlock) }.getOrNull()
            runCatching { engine.block.setMuted(targetBlock, true) }
            isRecording = true
            editorContext.updateVoiceOverRecordingInProgress(true)
            applyPlaybackRecordingConstraints(
                engine = engine,
                page = page,
            )
            runCatching { engine.block.setPlaybackTime(page, recordingStartCursorMs / 1000.0) }
            startVideoPlaybackDuringRecording(
                engine = engine,
                page = page,
            )

            progressJob?.cancel()
            progressJob = coroutineScope.launch progress@{
                var wasPagePlaying = runCatching { engine.block.isPlaying(page) }.getOrDefault(false)
                var lastEngineWaveformUpdateRealtimeMs = -ENGINE_WAVEFORM_UPDATE_INTERVAL_MS
                while (isActive && isRecording) {
                    val elapsedMs = max(0L, SystemClock.elapsedRealtime() - recordingStartRealtimeMs)
                    elapsedRecordingMs = elapsedMs
                    val shouldSyncEngineWaveform = elapsedMs - lastEngineWaveformUpdateRealtimeMs >= ENGINE_WAVEFORM_UPDATE_INTERVAL_MS
                    if (VoiceoverEngineBlocks.isValidBlock(engine, targetBlock) && shouldSyncEngineWaveform) {
                        recordingBuffer.flush(
                            engine = engine,
                            targetBlock = targetBlock,
                        )
                        val liveDurationSeconds = elapsedMs / 1000.0
                        runCatching { engine.block.setDuration(targetBlock, liveDurationSeconds) }
                        if (recordingExtendsPageDuration) {
                            VoiceoverTimelineSync.extendPageDurationIfNeeded(
                                engine = engine,
                                page = page,
                                durationSeconds = recordingStartCursorMs / 1000.0 + liveDurationSeconds + PLAYBACK_EXTENSION_BUFFER_SECONDS,
                            )
                        }
                        lastEngineWaveformUpdateRealtimeMs = elapsedMs
                    }
                    val playbackTime = runCatching { engine.block.getPlaybackTime(page) }.getOrDefault(0.0)
                    val isPagePlaying = runCatching { engine.block.isPlaying(page) }.getOrDefault(false)
                    val reachedPlaybackEnd = recordingPlaybackEndSeconds > PLAYBACK_END_EPSILON_SECONDS &&
                        playbackTime >= recordingPlaybackEndSeconds - PLAYBACK_END_EPSILON_SECONDS
                    val reachedDynamicPageEnd = recordingExtendsPageDuration &&
                        playbackTime >= runCatching { engine.block.getDuration(page) }
                            .getOrDefault(0.0) - PLAYBACK_END_EPSILON_SECONDS
                    val hasSettledAfterStart = elapsedMs >= (PLAYBACK_PAUSE_STOP_GRACE_SECONDS * 1000.0).toLong()
                    val pauseDetected = hasSettledAfterStart && wasPagePlaying && !isPagePlaying
                    if (pauseDetected && !reachedPlaybackEnd && !reachedDynamicPageEnd) {
                        stopAndPersist(
                            editorContext = editorContext,
                            coroutineScope = coroutineScope,
                        )
                        return@progress
                    }
                    wasPagePlaying = isPagePlaying
                    keepVideoPlaybackRunning(
                        engine = engine,
                        page = page,
                    )
                    delay(33)
                }
            }
        }
    }

    fun stopAndPersist(
        editorContext: EditorContext,
        coroutineScope: CoroutineScope,
    ) {
        if (!isRecording || isSaving) return
        isRecording = false
        editorContext.updateVoiceOverRecordingInProgress(false)
        recordingPlaybackEndSeconds = 0.0
        val segmentFile = currentSegmentFile
        currentSegmentFile = null
        val recordingPageBlock = recordingPage
        recordingPage = null
        val targetBlock = voiceOverBlock
        val extendsPageDuration = recordingExtendsPageDuration
        recordingPageBlock?.let { page ->
            runCatching {
                if (editorContext.engine.block.isValid(page)) {
                    editorContext.engine.block.setPlaying(page, false)
                }
            }
        }
        restorePlaybackRecordingConstraints(
            engine = editorContext.engine,
            page = recordingPageBlock,
        )
        recordingExtendsPageDuration = false

        if (segmentFile == null || targetBlock == null) {
            VoiceoverFiles.deleteFileAsync(coroutineScope, segmentFile)
            onStopCompleted?.invoke()
            return
        }

        isSaving = true
        saveJob = coroutineScope.launch {
            try {
                progressJob?.cancelAndJoin()
                progressJob = null
                val engine = editorContext.engine
                val durationMs = recorder.stop()
                recordingBuffer.flush(
                    engine = engine,
                    targetBlock = targetBlock,
                )
                elapsedRecordingMs = durationMs
                if (durationMs <= 0) {
                    recordingBuffer.clear(engine)
                    withContext(Dispatchers.IO) {
                        segmentFile.delete()
                    }
                    if (VoiceoverEngineBlocks.isValidBlock(engine, targetBlock) &&
                        !VoiceoverEngineBlocks.hasCommittedAudioResource(engine, targetBlock)
                    ) {
                        runCatching { editorContext.engine.block.destroy(targetBlock) }
                    }
                    return@launch
                }
                val segmentByteCount = withContext(Dispatchers.IO) { segmentFile.length().coerceAtLeast(0L) }
                val segmentDurationSec = VoiceoverAudioCodec.pcmBytesToDurationSeconds(segmentByteCount)
                if (segmentDurationSec <= 0.0) {
                    recordingBuffer.clear(engine)
                    withContext(Dispatchers.IO) {
                        segmentFile.delete()
                    }
                    if (VoiceoverEngineBlocks.isValidBlock(engine, targetBlock) &&
                        !VoiceoverEngineBlocks.hasCommittedAudioResource(engine, targetBlock)
                    ) {
                        runCatching { editorContext.engine.block.destroy(targetBlock) }
                    }
                    return@launch
                }

                val outputFile = try {
                    withContext(Dispatchers.IO) {
                        VoiceoverFiles.createOwnedVoiceOverFile(
                            filesDir = editorContext.activity.filesDir,
                            designBlock = targetBlock,
                        ).also { file ->
                            try {
                                VoiceoverAudioCodec.writeMonoPcm16FileAsStereoFloatWav(
                                    inputFile = segmentFile,
                                    outputFile = file,
                                    sampleRate = SAMPLE_RATE,
                                )
                            } catch (error: Throwable) {
                                file.delete()
                                throw error
                            }
                        }
                    }
                } finally {
                    withContext(Dispatchers.IO) {
                        segmentFile.delete()
                    }
                }
                if (!VoiceoverEngineBlocks.isValidBlock(engine, targetBlock)) {
                    withContext(Dispatchers.IO) {
                        outputFile.delete()
                    }
                    return@launch
                }
                runCatching {
                    engine.block.setString(targetBlock, "audio/fileURI", Uri.fromFile(outputFile).toString())
                    engine.block.setKind(targetBlock, VOICEOVER_KIND)
                    engine.block.setLooping(targetBlock, false)
                    engine.block.setMetadata(
                        targetBlock,
                        "name",
                        editorContext.activity.getString(CoreR.string.ly_img_editor_sheet_voiceover_title),
                    )
                    engine.block.setAlwaysOnTop(targetBlock, true)
                    engine.block.setTimeOffset(targetBlock, recordingStartCursorMs.coerceAtLeast(0L) / 1000.0)
                }.onFailure {
                    withContext(Dispatchers.IO) {
                        outputFile.delete()
                    }
                    throw it
                }
                recordingBuffer.clear(engine)
                restoreTargetMutedState(engine, targetBlock)

                val totalDurationSec = runCatching {
                    engine.block.forceLoadAVResource(targetBlock)
                    engine.block.getAVResourceTotalDuration(targetBlock)
                }.getOrDefault(segmentDurationSec).coerceAtLeast(0.0)

                val trimmedDurationSec = min(segmentDurationSec, totalDurationSec).coerceAtLeast(0.0)
                runCatching {
                    engine.block.setTrimOffset(targetBlock, 0.0)
                    engine.block.setTrimLength(targetBlock, trimmedDurationSec)
                    engine.block.setDuration(targetBlock, trimmedDurationSec)
                }.onFailure {
                    runCatching { engine.block.setDuration(targetBlock, trimmedDurationSec) }
                }
                if (extendsPageDuration && recordingPageBlock != null && VoiceoverEngineBlocks.isValidBlock(engine, recordingPageBlock)) {
                    VoiceoverTimelineSync.syncPageDurationToContentEnd(
                        engine = engine,
                        page = recordingPageBlock,
                    )
                }
                engine.block.findAllSelected()
                    .filter { selected -> engine.block.isValid(selected) }
                    .forEach { selected -> engine.block.setSelected(selected, false) }
                runCatching { engine.block.setSelected(targetBlock, true) }
                engine.editor.addUndoStep()
                shouldAbortOnDispose = false
            } finally {
                isSaving = false
                saveJob = null
                onStopCompleted?.invoke()
            }
        }
    }

    fun cancelAndRestoreSelection(
        editorContext: EditorContext,
        coroutineScope: CoroutineScope,
        selectionToRestore: List<DesignBlock>,
    ) {
        abort(
            editorContext = editorContext,
            coroutineScope = coroutineScope,
            selectionToRestore = selectionToRestore,
        )
        shouldAbortOnDispose = false
    }

    fun handleSheetDisposed(
        editorContext: EditorContext,
        coroutineScope: CoroutineScope,
        selectionToRestore: List<DesignBlock>,
    ) {
        if (!shouldAbortOnDispose) return
        abort(
            editorContext = editorContext,
            coroutineScope = coroutineScope,
            selectionToRestore = selectionToRestore,
        )
        shouldAbortOnDispose = false
    }

    private fun applyPlaybackRecordingConstraints(
        engine: Engine,
        page: DesignBlock,
    ) {
        recordingPageWasLooping = runCatching { engine.block.isLooping(page) }.getOrNull()
        runCatching { engine.block.setLooping(page, false) }
        updateMutedPlaybackAudio(
            engine = engine,
            page = page,
        )
    }

    private fun updateMutedPlaybackAudio(
        engine: Engine,
        page: DesignBlock,
    ) {
        if (!muteOtherAudio) {
            recordingMutedBlocks.forEach { (block, wasMuted) ->
                if (VoiceoverEngineBlocks.isValidBlock(engine, block)) {
                    runCatching { engine.block.setMuted(block, wasMuted) }
                }
            }
            recordingMutedBlocks = emptyMap()
            return
        }

        if (recordingMutedBlocks.isNotEmpty()) {
            recordingMutedBlocks.forEach { (block, wasMuted) ->
                if (VoiceoverEngineBlocks.isValidBlock(engine, block) && !wasMuted) {
                    runCatching { engine.block.setMuted(block, true) }
                }
            }
            return
        }

        val previousMutedStates = mutableMapOf<DesignBlock, Boolean>()
        VoiceoverEngineBlocks.collectPlaybackAudioBlocks(engine, page).forEach { playbackBlock ->
            val wasMuted = runCatching { engine.block.isMuted(playbackBlock) }.getOrNull() ?: return@forEach
            previousMutedStates[playbackBlock] = wasMuted
            if (!wasMuted) {
                runCatching { engine.block.setMuted(playbackBlock, true) }
            }
        }
        recordingMutedBlocks = previousMutedStates
    }

    private fun startVideoPlaybackDuringRecording(
        engine: Engine,
        page: DesignBlock,
    ) {
        val shouldPlay = runCatching {
            val playbackTime = engine.block.getPlaybackTime(page)
            val playbackEnd = recordingPlaybackEndSeconds
            playbackEnd <= PLAYBACK_END_EPSILON_SECONDS || playbackTime < playbackEnd - PLAYBACK_END_EPSILON_SECONDS
        }.getOrDefault(true)
        runCatching { engine.block.setPlaying(page, shouldPlay) }
    }

    private fun keepVideoPlaybackRunning(
        engine: Engine,
        page: DesignBlock,
    ) {
        val shouldKeepPlaying = runCatching {
            val playbackTime = engine.block.getPlaybackTime(page)
            val playbackEnd = recordingPlaybackEndSeconds
            playbackEnd <= PLAYBACK_END_EPSILON_SECONDS || playbackTime < playbackEnd - PLAYBACK_END_EPSILON_SECONDS
        }.getOrDefault(false)
        if (!shouldKeepPlaying) return
        runCatching {
            if (!engine.block.isPlaying(page)) {
                engine.block.setPlaying(page, true)
            }
        }
    }

    private fun restorePlaybackRecordingConstraints(
        engine: Engine,
        page: DesignBlock?,
    ) {
        recordingMutedBlocks.forEach { (block, wasMuted) ->
            if (VoiceoverEngineBlocks.isValidBlock(engine, block)) {
                runCatching { engine.block.setMuted(block, wasMuted) }
            }
        }
        recordingMutedBlocks = emptyMap()

        val wasLooping = recordingPageWasLooping
        recordingPageWasLooping = null
        if (page != null && wasLooping != null && VoiceoverEngineBlocks.isValidBlock(engine, page)) {
            runCatching { engine.block.setLooping(page, wasLooping) }
        }
    }

    private fun abort(
        editorContext: EditorContext,
        coroutineScope: CoroutineScope,
        selectionToRestore: List<DesignBlock> = emptyList(),
    ) {
        recorder.abort()
        saveJob?.cancel()
        saveJob = null
        isSaving = false
        progressJob?.cancel()
        progressJob = null
        VoiceoverFiles.deleteFileAsync(coroutineScope, currentSegmentFile)
        currentSegmentFile = null
        val engine = editorContext.engine
        val page = recordingPage ?: engine.scene.getCurrentPage()
        if (page != null && VoiceoverEngineBlocks.isValidBlock(engine, page)) {
            runCatching { engine.block.setPlaying(page, false) }
        }
        recordingBuffer.clear(engine)
        clearSelection(engine)
        restorePlaybackRecordingConstraints(
            engine = engine,
            page = page,
        )
        recordingExtendsPageDuration = false
        val targetBlock = voiceOverBlock
        if (targetBlock != null) {
            restoreTargetMutedState(engine, targetBlock)
        }
        if (targetBlock != null &&
            VoiceoverEngineBlocks.isValidBlock(engine, targetBlock) &&
            !VoiceoverEngineBlocks.hasCommittedAudioResource(engine, targetBlock)
        ) {
            runCatching { engine.block.destroy(targetBlock) }
        }
        restoreSelection(engine, selectionToRestore)
        recordingPage = null
        recordingPlaybackEndSeconds = 0.0
        elapsedRecordingMs = 0L
        isRecording = false
        editorContext.updateVoiceOverRecordingInProgress(false)
    }

    private fun clearSelection(engine: Engine) {
        runCatching {
            engine.block.findAllSelected()
                .filter { selected -> engine.block.isValid(selected) }
                .forEach { selected -> engine.block.setSelected(selected, false) }
        }
    }

    private fun restoreSelection(
        engine: Engine,
        selectionToRestore: List<DesignBlock>,
    ) {
        if (selectionToRestore.isEmpty()) return
        selectionToRestore
            .filter { selected -> engine.block.isValid(selected) }
            .forEach { selected ->
                runCatching { engine.block.setSelected(selected, true) }
            }
    }

    private fun restoreTargetMutedState(
        engine: Engine,
        targetBlock: DesignBlock,
    ) {
        val wasMuted = recordingTargetWasMuted
        recordingTargetWasMuted = null
        if (wasMuted != null && VoiceoverEngineBlocks.isValidBlock(engine, targetBlock)) {
            runCatching { engine.block.setMuted(targetBlock, wasMuted) }
        }
    }

    private fun handleRecorderFailure(
        editorContext: EditorContext,
        coroutineScope: CoroutineScope,
        error: Throwable,
    ) {
        if (!isRecording || isSaving) return
        abort(
            editorContext = editorContext,
            coroutineScope = coroutineScope,
        )
        editorContext.eventHandler.send(Event.OnError(error))
    }
}
