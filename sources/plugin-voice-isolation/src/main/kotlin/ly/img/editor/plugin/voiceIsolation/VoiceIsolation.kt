package ly.img.editor.plugin.voiceIsolation

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ly.img.editor.core.EditorScope
import ly.img.editor.plugin.voiceIsolation.audio.decodeAudioTrack
import ly.img.editor.plugin.voiceIsolation.util.FileLoader
import ly.img.engine.AudioFromVideoOptions
import ly.img.engine.BlockState
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import ly.img.engine.EngineException
import ly.img.engine.FillType
import ly.img.engine.UnstableEngineApi
import okhttp3.OkHttpClient
import java.io.File
import java.util.UUID

/**
 * Whether [isolateVoice] can clean [block]: an audio clip, or a video clip whose audio the editor can separate. The
 * "fill/change" scope of [block] must allow it.
 *
 * @param block the block to check.
 * @param engine the engine of the scene that holds [block].
 * @return true if [isolateVoice] can clean [block].
 */
fun canIsolateVoice(
    block: DesignBlock,
    engine: Engine,
): Boolean = runCatching { clipAudio(engine, block) != null }.getOrDefault(false)

/**
 * Removes music and background noise from the audio of [block] and keeps the voice. The cleaned audio is a WAV file in
 * the app's files directory.
 *
 * - An audio clip gets the cleaned file.
 * - A video clip moves the audio track that the engine plays, its first one, into a new audio clip, as the editor's
 *   "Separate Audio" does, and this clip gets the cleaned file.
 *
 * Unlike the "Isolate Voice" button, the call leaves three things to the app. It does not wait for other calls, so the
 * isolator decides how many runs it takes at once. It shows no loading state: set the state of [block] to
 * `BlockState.Pending` before the call and back to `BlockState.Ready` when it returns or throws. It adds no undo step:
 * call `engine.editor.addUndoStep()` afterwards to let the user undo the change.
 *
 * Call it on the engine's thread, wherever the app has the engine, for example in the editor's `onCreate` after the
 * scene exists. It suspends while the audio is decoded and cleaned.
 *
 * Note that the cleaned files stay in the app's files directory. To keep a scene that refers to them, for example on
 * another device, upload each file when the editor exports or closes, and point the scene to the upload with
 * `engine.editor.relocateResource`.
 *
 * @param block the video or audio clip to clean.
 * @param engine the engine of the scene that holds [block].
 * @param config the configuration that selects the [VoiceIsolator].
 * @param context the context for the app's directories and the isolator.
 * @return the audio clip that got the cleaned file: [block] for an audio clip, the new audio clip for a video clip.
 * @throws VoiceIsolationException when [block] cannot be cleaned, the clip changes during the run or the isolator fails.
 */
suspend fun isolateVoice(
    block: DesignBlock,
    engine: Engine,
    config: VoiceIsolationConfig,
    context: Context,
): DesignBlock = isolateVoice(block = block, engine = engine, config = config, context = context, onCommit = {})

/**
 * [isolateVoice], which calls [onCommit] right after the change, before the engine's next update.
 */
private suspend fun isolateVoice(
    block: DesignBlock,
    engine: Engine,
    config: VoiceIsolationConfig,
    context: Context,
    onCommit: () -> Unit,
): DesignBlock {
    val clip = runCatching { clipAudio(engine, block) }.getOrNull() ?: throw VoiceIsolationException.UnsupportedBlock()
    val appContext = context.applicationContext
    return whileClipUnchanged(engine, clip) {
        wrapErrors {
            val file = clean(engine, appContext, clip, config)
            try {
                commit(engine, appContext, clip, file).also { onCommit() }
            } catch (exception: Throwable) {
                file.delete()
                throw exception
            }
        }
    }
}

/**
 * Cleans [block] for the "Isolate Voice" button: [block] shows its loading state meanwhile, the run waits for the
 * runs that started before it, so that the model cleans one clip at a time, and the change becomes one undo step.
 */
internal suspend fun EditorScope.isolateVoiceFromButton(
    block: DesignBlock,
    config: VoiceIsolationConfig,
): DesignBlock {
    val engine = editorContext.engine
    val editorJob = editorContext.coroutineScope.coroutineContext.job
    val processingBlocks = editorContext.mutableStateOf(key = PROCESSING_BLOCKS_KEY, initial = emptySet<DesignBlock>())
    processingBlocks.value += block
    engine.block.setState(block = block, state = BlockState.Pending(0f))
    var committed = false
    try {
        val queue = editorContext.mutableStateOf(key = QUEUE_KEY, initial = Mutex()).value
        return queue.withLock {
            // The button shows only for a clip that can be cleaned, so a clip that no longer can was changed meanwhile.
            if (!canIsolateVoice(block, engine)) throw VoiceIsolationException.BlockChanged()
            // The end of the loading state and the undo request follow the change with no suspension, so that another
            // undo request, which the engine's next update would take as its own step, cannot split the change.
            isolateVoice(block = block, engine = engine, config = config, context = editorContext.activity) {
                engine.block.setState(block = block, state = BlockState.Ready)
                engine.editor.addUndoStep()
                committed = true
            }
        }
    } finally {
        processingBlocks.value -= block
        // A closed editor stops its engine, and the next editor starts the same engine with other blocks.
        if (!committed && editorJob.isActive && engine.block.isValid(block)) {
            engine.block.setState(block = block, state = BlockState.Ready)
        }
    }
}

/**
 * Whether the "Isolate Voice" button cleans [block] now.
 */
internal fun EditorScope.isIsolatingVoice(block: DesignBlock): Boolean =
    block in editorContext.mutableStateOf(key = PROCESSING_BLOCKS_KEY, initial = emptySet<DesignBlock>()).value

/**
 * The audio of a clip: the file of an audio block, or the video file of a video clip's fill.
 */
private class ClipAudio(
    val block: DesignBlock,
    val videoFill: DesignBlock?,
    val uri: String,
    val isVideoMuted: Boolean = false,
) {
    /**
     * Whether [block] still shows this audio and may still change it. The number of audio tracks does not count: a
     * video that loads and turns out to have no audio ends the run with [VoiceIsolationException.NoAudio]. A video
     * whose audio the user separates during the run is muted then, so the run would separate it a second time.
     */
    fun isCurrent(engine: Engine): Boolean = runCatching {
        engine.block.isValid(block) &&
            engine.block.isAllowedByScope(block, "fill/change") &&
            if (videoFill == null) {
                engine.block.getString(block, AUDIO_FILE_URI) == uri
            } else {
                engine.block.getFill(block) == videoFill &&
                    engine.block.getString(videoFill, VIDEO_FILE_URI) == uri &&
                    engine.block.isMuted(videoFill) == isVideoMuted &&
                    !engine.block.isForceMuted(videoFill)
            }
    }.getOrDefault(false)
}

@OptIn(UnstableEngineApi::class)
private fun clipAudio(
    engine: Engine,
    block: DesignBlock,
): ClipAudio? {
    if (!engine.block.isValid(block) || !engine.block.isAllowedByScope(block, "fill/change")) return null
    return when (DesignBlockType.get(engine.block.getType(block))) {
        DesignBlockType.Audio -> {
            val uri = engine.block.getString(block, AUDIO_FILE_URI)
            // A voiceover that records holds raw samples in a buffer, which has no container to decode.
            val isRecording = engine.block.getKind(block) == KIND_VOICEOVER && Uri.parse(uri).scheme == BUFFER_SCHEME
            if (uri.isBlank() || isRecording) null else ClipAudio(block = block, videoFill = null, uri = uri)
        }
        // The editor's conditions for "Separate Audio".
        DesignBlockType.Graphic -> {
            if (engine.block.getKind(block) == KIND_ANIMATED_STICKER || !engine.block.supportsFill(block)) return null
            val fill = engine.block.getFill(block)
            if (engine.block.getType(fill) != FillType.Video.key || engine.block.isForceMuted(fill)) return null
            // A video's audio tracks are known only after it loads, so a video that has not loaded counts, also after a
            // failed download.
            if (engine.block.isAVResourceLoaded(fill)) {
                // The engine stops the app when it reads the tracks of a video that it could not open.
                if (engine.block.getState(fill) is BlockState.Error || engine.block.getAudioTrackCountFromVideo(fill) == 0) return null
            }
            val uri = engine.block.getString(fill, VIDEO_FILE_URI)
            if (uri.isBlank()) null else ClipAudio(block = block, videoFill = fill, uri = uri, isVideoMuted = engine.block.isMuted(fill))
        }
        else -> null
    }
}

/**
 * Runs [action], and cancels it with [VoiceIsolationException.BlockChanged] when the clip is deleted or gets another
 * file, so that a run does not go on for a clip that the result no longer fits. A cancellation from the isolator
 * itself, such as a timeout, is an error of the isolator.
 */
private suspend fun <T> whileClipUnchanged(
    engine: Engine,
    clip: ClipAudio,
    action: suspend () -> T,
): T = coroutineScope {
    val run = async { action() }
    val watcher = launch {
        engine.event.subscribe(listOfNotNull(clip.block, clip.videoFill)).first { !clip.isCurrent(engine) }
        run.cancel()
    }
    try {
        run.await()
    } catch (exception: CancellationException) {
        // Rethrows when the caller is cancelled, for example because the editor closed.
        currentCoroutineContext().ensureActive()
        throw if (clip.isCurrent(engine)) VoiceIsolationException.ProcessingFailed(exception) else VoiceIsolationException.BlockChanged()
    } finally {
        watcher.cancel()
    }
}

/**
 * Writes the cleaned audio of [clip]: of a video clip, the audio track that the engine plays, its first one. The engine
 * supplies the audio of a video clip, so the platform decoder opens a local file only: the extracted track, the local
 * file of an audio clip, or a copy of any other source.
 */
private suspend fun clean(
    engine: Engine,
    context: Context,
    clip: ClipAudio,
    config: VoiceIsolationConfig,
): File {
    // A separated audio track is complete only after its load, because the engine can still extract it into its buffer.
    engine.block.forceLoadAVResource(clip.videoFill ?: clip.block)
    val workFolder = File(context.cacheDir, RESULT_FOLDER)
    // The scene keeps referring to the result, so it goes where the system does not delete files to free space.
    val resultFolder = File(context.filesDir, RESULT_FOLDER)
    withContext(Dispatchers.IO) {
        workFolder.mkdirs()
        resultFolder.mkdirs()
    }
    val source = File(workFolder, UUID.randomUUID().toString())
    val input = File(workFolder, "${UUID.randomUUID()}.wav")
    val output = File(resultFolder, "${UUID.randomUUID()}.wav")
    try {
        val audio = if (clip.videoFill != null) {
            copyPlayedTrack(engine, clip.videoFill, source)
            Uri.fromFile(source)
        } else {
            localAudio(engine, context, Uri.parse(clip.uri), source, config.httpClient)
        }
        decodeAudioTrack(context = context, source = audio, audioTrack = 0, output = input)
        config.isolator.isolateVoice(context = context, input = input, output = output)
        return output
    } catch (exception: Throwable) {
        output.delete()
        throw exception
    } finally {
        input.delete()
        source.delete()
    }
}

/**
 * Copies the audio track that [videoFill] plays, its first one, into [file]. The engine extracts the track into a
 * buffer before `createAudioFromVideo` returns, and the temporary block and its buffer go in the same turn, so no
 * engine update and no undo step sees them.
 */
private suspend fun copyPlayedTrack(
    engine: Engine,
    videoFill: DesignBlock,
    file: File,
) {
    // The load can fail after the check in clipAudio, and a read of its tracks would stop the app.
    if (engine.block.getState(videoFill) is BlockState.Error) {
        throw VoiceIsolationException.ProcessingFailed(IllegalStateException("The engine cannot open the video."))
    }
    val audio = try {
        engine.block.createAudioFromVideo(
            videoFill = videoFill,
            trackIndex = 0,
            options = AudioFromVideoOptions(keepTrimSettings = false, muteOriginalVideo = false),
        )
    } catch (exception: EngineException) {
        throw if (exception.code == NO_AUDIO_TRACKS_IN_VIDEO) VoiceIsolationException.NoAudio() else exception
    }
    try {
        val buffer = Uri.parse(engine.block.getString(audio, AUDIO_FILE_URI))
        try {
            copyBuffer(engine, buffer, file)
        } finally {
            engine.editor.destroyBuffer(buffer)
        }
    } finally {
        engine.block.destroy(audio)
    }
    if (withContext(Dispatchers.IO) { file.length() } == 0L) {
        throw VoiceIsolationException.NoAudio()
    }
}

/**
 * The file [fileUri] of an audio clip, as a URI that the platform decoder opens: a local file or a `content` URI in
 * place, any other source as a copy in [file]. The engine supplies a buffer or a file that it loaded. Only an http(s)
 * source downloads again, because `getResourceData` does not serve it.
 */
private suspend fun localAudio(
    engine: Engine,
    context: Context,
    fileUri: Uri,
    file: File,
    httpClient: OkHttpClient,
): Uri {
    // The engine plays what its URI resolver makes of the URI, such as a path relative to its base path.
    val uri = engine.editor.getAbsoluteUri(fileUri)
    val scheme = uri.scheme?.lowercase()
    // A file in the app's assets has no path that the decoder can open.
    val isLocalFile = scheme == "file" && withContext(Dispatchers.IO) { File(uri.path.orEmpty()).isFile }
    if (isLocalFile || scheme == "content") {
        return uri
    }
    when (scheme) {
        BUFFER_SCHEME -> copyBuffer(engine, uri, file)
        "http", "https" -> FileLoader.download(context = context, uri = uri, httpClient = httpClient, file = file)
        // The engine serves a loaded resource by the URI that its block holds, and hands over all chunks before the call
        // returns, so the copy needs no suspension.
        else -> file.outputStream().use { output ->
            engine.editor.getResourceData(uri = fileUri, chunkSize = BUFFER_SLICE) { data ->
                output.write(ByteArray(data.remaining()).also { data.get(it) })
                true
            }
        }
    }
    if (withContext(Dispatchers.IO) { file.length() } == 0L) throw VoiceIsolationException.NoAudio()
    return Uri.fromFile(file)
}

/**
 * Puts [file] into the scene, a video clip after it moves its audio into a new audio clip. The changes need no
 * suspension, so the engine's next update sees all of them.
 */
private fun commit(
    engine: Engine,
    context: Context,
    clip: ClipAudio,
    file: File,
): DesignBlock {
    if (!clip.isCurrent(engine)) throw VoiceIsolationException.BlockChanged()
    if (clip.videoFill == null) {
        engine.block.setString(clip.block, AUDIO_FILE_URI, Uri.fromFile(file).toString())
        return clip.block
    }
    val audioBlock = separateAudio(
        engine = engine,
        clip = clip.block,
        videoFill = clip.videoFill,
        // The name that the editor's Separate Audio gives a clip without one.
        fallbackName = context.getString(ly.img.editor.core.R.string.ly_img_editor_timeline_clip_audio_title),
    )
    val extracted = Uri.parse(engine.block.getString(audioBlock, AUDIO_FILE_URI))
    engine.block.setString(audioBlock, AUDIO_FILE_URI, Uri.fromFile(file).toString())
    // No undo step holds the extracted track yet, so its buffer goes at once.
    if (extracted.scheme == BUFFER_SCHEME) {
        engine.editor.destroyBuffer(extracted)
    }
    return audioBlock
}

/**
 * Moves the audio track that the engine plays for [clip], its first one, into a new audio block and mutes [clip], as
 * the editor's "Separate Audio" does for each track. The audio block starts with [clip], plays like it and is named
 * after it, or [fallbackName] when it has no name. The audio of a background clip goes into the audio row directly
 * above the background track, the audio of an overlay into the top audio row. [videoFill] must have loaded.
 */
private fun separateAudio(
    engine: Engine,
    clip: DesignBlock,
    videoFill: DesignBlock,
    fallbackName: String,
): DesignBlock {
    val block = engine.block
    val page = generateSequence(block.getParent(clip), block::getParent).first { block.getType(it) == DesignBlockType.Page.key }
    // The timeline shows the audio rows between the overlays and the background track, and the audio block with the
    // lower page index in the higher row.
    val parent = block.getParent(clip)
    val isBackgroundClip = parent != null && block.getType(parent) == DesignBlockType.Track.key && block.isPageDurationSource(parent)
    val topAudioRowIndex = if (isBackgroundClip) {
        null
    } else {
        block.getChildren(page).indexOfFirst { isAudioRow(engine, it) }.takeIf { it >= 0 }
    }
    val audioBlock = block.createAudioFromVideo(
        videoFill = videoFill,
        trackIndex = 0,
        options = AudioFromVideoOptions(keepTrimSettings = true, muteOriginalVideo = true),
    )
    if (topAudioRowIndex != null) {
        block.insertChild(parent = page, child = audioBlock, index = topAudioRowIndex)
    } else {
        block.appendChild(parent = page, child = audioBlock)
    }
    // Keeps the audio out of the z-index reordering, like an audio block added from the library.
    block.setAlwaysOnTop(audioBlock, true)
    block.setTimeOffset(audioBlock, block.getTimeOffset(clip))
    // The engine names the block after its audio track, and the web timeline shows that name first.
    block.setName(audioBlock, "")
    val clipName = if (block.hasMetadata(clip, NAME_METADATA_KEY)) block.getMetadata(clip, NAME_METADATA_KEY) else ""
    // A video from the gallery has no name.
    block.setMetadata(audioBlock, NAME_METADATA_KEY, clipName.ifEmpty { fallbackName })
    return audioBlock
}

/**
 * Whether the timeline shows [designBlock] in the audio rows: an audio block, or a track that holds one.
 */
private fun isAudioRow(
    engine: Engine,
    designBlock: DesignBlock,
): Boolean = when (engine.block.getType(designBlock)) {
    DesignBlockType.Audio.key -> true
    DesignBlockType.Track.key -> engine.block.getChildren(designBlock).any { engine.block.getType(it) == DesignBlockType.Audio.key }
    else -> false
}

/**
 * Runs [action], and wraps any error that is not a [VoiceIsolationException] in
 * [VoiceIsolationException.ProcessingFailed].
 */
private suspend fun <T> wrapErrors(action: suspend () -> T): T = try {
    action()
} catch (exception: CancellationException) {
    throw exception
} catch (exception: VoiceIsolationException) {
    throw exception
} catch (exception: Exception) {
    throw VoiceIsolationException.ProcessingFailed(exception)
}

/**
 * Copies a `buffer://` resource, such as the audio of a separated video or a file of a loaded archive, a slice at a
 * time and in one turn, so that a temporary buffer goes before the engine's next update.
 */
private fun copyBuffer(
    engine: Engine,
    uri: Uri,
    file: File,
) {
    val length = engine.editor.getBufferLength(uri)
    file.outputStream().use { output ->
        var offset = 0
        while (offset < length) {
            val data = engine.editor.getBufferData(uri = uri, offset = offset, length = minOf(BUFFER_SLICE, length - offset))
            val bytes = ByteArray(data.remaining()).also { data.get(it) }
            output.write(bytes)
            offset += bytes.size
        }
    }
}

private const val PROCESSING_BLOCKS_KEY = "ly.img.editor.plugin.voiceIsolation.processingBlocks"
private const val QUEUE_KEY = "ly.img.editor.plugin.voiceIsolation.queue"
private const val RESULT_FOLDER = "ly.img.plugin.voiceIsolation"
private const val AUDIO_FILE_URI = "audio/fileURI"
private const val VIDEO_FILE_URI = "fill/video/fileURI"
private const val KIND_VOICEOVER = "voiceover"
private const val KIND_ANIMATED_STICKER = "animatedSticker"
private const val BUFFER_SCHEME = "buffer"
private const val BUFFER_SLICE = 1024 * 1024
private const val NO_AUDIO_TRACKS_IN_VIDEO = "BLOCK.NO_AUDIO_TRACKS_IN_VIDEO"
private const val NAME_METADATA_KEY = "name"
