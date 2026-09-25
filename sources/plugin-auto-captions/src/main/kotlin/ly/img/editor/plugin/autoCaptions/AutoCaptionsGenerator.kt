package ly.img.editor.plugin.autoCaptions

import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import ly.img.engine.AudioFromVideoOptions
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.util.UUID

/**
 * The generate flow: find the scene's audible blocks, read each one's audio, transcribe them in parallel, map the
 * cues onto the page timeline and merge them into a single SRT file for the editor to import.
 */
internal object AutoCaptionsGenerator {
    /** How an audio source's bytes have to be read — no single engine API covers every source an audio block can have. */
    internal enum class AudioReader {
        /** A video's muxed track, or a voiceover that is still being recorded. */
        Buffer,

        /** Anything the engine resolves locally, most importantly a committed voiceover behind a `file://` cache URL. */
        Resource,

        /** Library audio: `getResourceData` rejects `http(s)` sources outright. */
        Remote,

        ;

        companion object {
            fun of(scheme: String?): AudioReader = when (scheme?.lowercase()) {
                "buffer" -> Buffer
                "http", "https" -> Remote
                else -> Resource
            }
        }
    }

    /**
     * Which source a block's audio counts as. Two sources audible at the same instant cannot both caption it, so
     * the declaration order ranks them — see [timelineCues]. A voiceover is deliberate narration and outranks the
     * video it is layered over; music and sound effects are the least likely to carry the speech to caption.
     */
    internal enum class Source { Voiceover, Video, Audio }

    /** A half-open stretch of the page timeline. A degenerate span intersects nothing. */
    private data class TimeSpan(
        val start: Double,
        val end: Double,
    ) {
        fun intersects(other: TimeSpan) = start < other.end && other.start < end
    }

    /** A block's transcript plus its timeline mapping. Pure data, so [timelineCues] can be tested without an engine. */
    internal data class TranscribedBlock(
        val srt: String,
        val source: Source,
        /** Where the block sits on the page timeline. */
        val timeOffset: Double,
        /** The played (trimmed) window in timeline seconds. Both readers take the whole source track, so this is the trim range. */
        val windowStart: Double,
        val windowEnd: Double,
        val speed: Double,
    )

    /**
     * A block's audio staged on disk. A file rather than a byte array because a scene's audible content is
     * unbounded — several clips, or one long recording, would otherwise all be resident at once.
     */
    private data class BlockAudio(
        val file: File,
        val mimeType: String,
        val source: Source,
        val timeOffset: Double,
        val windowStart: Double,
        val windowEnd: Double,
        val speed: Double,
    )

    /**
     * Writes a temporary SRT file with cues for all audible content in the scene.
     *
     * Every block's audio is staged in a directory of its own, deleted however the run ends, cancellation
     * included.
     *
     * @return the SRT file, or null when nothing is audible or nothing was transcribed.
     */
    suspend fun generateCaptionsFile(
        engine: Engine,
        cacheDir: File,
        provider: TranscriptionProvider,
        options: TranscriptionOptions,
    ): File? {
        val staging = withContext(Dispatchers.IO) {
            File(cacheDir, "imgly-auto-captions-${UUID.randomUUID()}").apply { mkdirs() }
        }
        try {
            val audios = readAudibleBlocks(engine, staging)
            if (audios.isEmpty()) return null

            val cues = transcribe(audios, provider, options)
            if (cues.isEmpty()) return null

            // Serialised alongside the write rather than before it: both are off the main thread the editor scope runs on.
            return withContext(Dispatchers.IO) {
                val srt = Srt.serialize(cues) // already merged and sorted by `timelineCues`
                File(cacheDir, "imgly-auto-captions-${UUID.randomUUID()}.srt").apply { writeText(srt) }
            }
        } finally {
            withContext(NonCancellable + Dispatchers.IO) { staging.deleteRecursively() }
        }
    }

    // region Timeline mapping (pure)

    /**
     * Merges every block's transcript into one time-ordered cue list on the page timeline.
     *
     * The captions land in a single track, so sources audible at the same instant have to be resolved rather than
     * concatenated. Blocks are walked best-source-first (see [Source]), each claiming the stretches it is *speaking*
     * for (see [speechEnvelope]), and a lower-ranked block's cues inside an existing claim are dropped. A block that
     * transcribes to nothing claims nothing, so a silent voiceover never suppresses the video under it.
     *
     * A source claims what it says, not the clip it says it in: a 30-second voiceover holding 5 seconds of narration
     * leaves the other 25 to the video underneath, instead of silencing it throughout.
     *
     * Transcribing the *mixed* page audio instead is not reachable here: the engine's only mixer is the page-audio
     * encoder this plugin had to move off.
     */
    internal fun timelineCues(blocks: List<TranscribedBlock>): List<SubtitleCue> {
        val merged = mutableListOf<SubtitleCue>()
        val claimed = mutableListOf<TimeSpan>()
        blocks
            .sortedWith(compareBy({ it.source.ordinal }, { it.timeOffset }))
            .forEach { block ->
                val cues = timelineCues(block).filterNot { cue ->
                    claimed.any { it.intersects(TimeSpan(cue.start, cue.end)) }
                }
                if (cues.isEmpty()) return@forEach
                merged += cues
                claimed += speechEnvelope(cues)
            }
        return merged.sortedBy { it.start }
    }

    /**
     * The stretches a source is actually speaking for, with pauses up to [MAX_CLAIMED_PAUSE_SECONDS] absorbed.
     *
     * Both extremes are wrong. Claiming the block's whole length silences the video under a voiceover for the entire
     * clip, however little of it the narrator fills. Claiming the bare cues overcorrects: the beat between two
     * sentences would let a video caption flash in and straight back out. Merging across a short pause keeps a
     * continuous passage of narration whole while handing back a silence long enough to be worth captioning.
     */
    private fun speechEnvelope(cues: List<SubtitleCue>): List<TimeSpan> = cues
        .map { TimeSpan(it.start, it.end) }
        .sortedBy { it.start }
        .fold(mutableListOf()) { envelope, span ->
            val previous = envelope.lastOrNull()
            if (previous == null || span.start - previous.end > MAX_CLAIMED_PAUSE_SECONDS) {
                envelope.add(span)
            } else {
                // Cues can nest or straddle, so the run keeps whichever end reaches furthest.
                envelope[envelope.lastIndex] = TimeSpan(previous.start, maxOf(previous.end, span.end))
            }
            envelope
        }

    /**
     * Maps one block's transcript onto the page timeline. A cue straddling a window edge is clamped rather than
     * dropped: the trimmed-away half is silent, but the rest is still spoken.
     *
     * Cue timestamps are source seconds — the transcript comes from the untouched source bytes, which know nothing
     * of the playback speed — while the trim window is timeline seconds. So the window is scaled up to select and
     * clamp cues, and a survivor is scaled back down to be placed.
     */
    private fun timelineCues(block: TranscribedBlock): List<SubtitleCue> {
        val windowStart = block.windowStart * block.speed
        val windowEnd = block.windowEnd * block.speed

        fun onTimeline(sourceTime: Double) = block.timeOffset + (sourceTime - windowStart) / block.speed
        return Srt.parse(block.srt).mapNotNull { cue ->
            if (cue.end <= windowStart || cue.start >= windowEnd) return@mapNotNull null
            SubtitleCue(
                start = onTimeline(maxOf(cue.start, windowStart)),
                end = onTimeline(minOf(cue.end, windowEnd)),
                text = cue.text,
            )
        }
    }

    // endregion
    // region Audio reading (sequential, engine-bound)

    private suspend fun readAudibleBlocks(
        engine: Engine,
        staging: File,
    ): List<BlockAudio> {
        val audios = mutableListOf<BlockAudio>()
        var firstError: Throwable? = null
        run {
            for ((index, candidate) in audibleCandidates(engine).withIndex()) {
                val (block, source) = candidate
                currentCoroutineContext().ensureActive()
                try {
                    readAudio(block, source, engine, File(staging, "audio-$index"))?.let(audios::add)
                } catch (cancellation: CancellationException) {
                    throw cancellation
                } catch (throwable: Throwable) {
                    // One block failing must not sink the others — only an all-failed run surfaces an error.
                    Log.e(TAG, "Reading a block's audio failed", throwable)
                    if (firstError == null) firstError = throwable
                }
            }
        }
        if (audios.isEmpty()) firstError?.let { throw it }
        return audios
    }

    private suspend fun <T> withScratchHistory(
        engine: Engine,
        block: suspend () -> T,
    ): T {
        val previous = runCatching { engine.editor.getActiveHistory() }.getOrNull()
        val scratch = previous?.let { runCatching { engine.editor.createHistory() }.getOrNull() }
        scratch?.let { engine.editor.setActiveHistory(it) }
        try {
            return block()
        } finally {
            scratch?.let {
                runCatching {
                    engine.editor.setActiveHistory(previous)
                    engine.editor.destroyHistory(it)
                }
            }
        }
    }

    /**
     * Every block on the current page that could carry audible content, tagged with the source it counts as. Audio
     * is found by type because its kind varies (the editor stamps voiceovers `voiceover`); video only by kind,
     * since it shares the `graphic` type with images.
     *
     * Both queries search the whole scene, so other pages' blocks are filtered out: the SRT is imported into the
     * current page, and their cues would land there at another page's local time offsets.
     */
    private fun audibleCandidates(engine: Engine): List<Pair<DesignBlock, Source>> {
        val page = runCatching { engine.scene.getCurrentPage() }.getOrNull() ?: return emptyList()
        val audio = runCatching { engine.block.findByType(DesignBlockType.Audio) }.getOrDefault(emptyList())
            .map { it to sourceOf(it, engine) }
        val video = runCatching { engine.block.findByKind("video") }.getOrDefault(emptyList())
            .map { it to Source.Video }
        return (audio + video).filter { (block, _) -> isDescendant(block, page, engine) }
    }

    /** Ranks an audio block: the editor stamps recorded voiceovers, integrators use their own kinds. */
    private fun sourceOf(
        block: DesignBlock,
        engine: Engine,
    ): Source = if (runCatching { engine.block.getKind(block) }.getOrNull() == VOICEOVER_KIND) {
        Source.Voiceover
    } else {
        Source.Audio
    }

    /** Whether a block sits anywhere below [page] — directly, or nested in one of its tracks. */
    private fun isDescendant(
        block: DesignBlock,
        page: DesignBlock,
        engine: Engine,
    ): Boolean {
        var current = block
        while (true) {
            val parent = runCatching { engine.block.getParent(current) }.getOrNull() ?: return false
            if (parent == page) return true
            current = parent
        }
    }

    /**
     * A block's audio, or `null` when it has nothing audible — muted, silenced, or a video without an audio track.
     *
     * Audibility needs no loaded resource, so silent blocks are dropped before the expensive `forceLoadAVResource`.
     * The audibility id matches the timeline's trimmable id: the fill for a video, the block itself for audio.
     */
    private suspend fun readAudio(
        block: DesignBlock,
        source: Source,
        engine: Engine,
        destination: File,
    ): BlockAudio? {
        if (source == Source.Video) return readVideoAudio(block, engine, destination)

        // Read the source directly: `exportAudio` routes through the page-audio encoder, which strips the page
        // mid-export and crashes the live editor. Same reason the video branch never exports the video block.
        if (!isAudible(block, engine)) return null
        engine.block.forceLoadAVResource(block)
        val uri = audioFileUri(block, engine) ?: return null
        if (!writeAudio(uri, engine, destination)) return null
        val mimeType = runCatching { engine.editor.getMimeType(uri) }.getOrNull().canonicalAudioMimeType()
        return blockAudio(destination, mimeType, source, of = block, trimmedBy = block, engine = engine)
    }

    /**
     * Extracts a video's audio track into a detached audio block and reads its buffer. A video's audio-track count
     * can only be probed once the resource is loaded, so that check follows the load.
     *
     * Destroying the block does *not* release the buffer the extraction minted — only `destroyBuffer` does — so
     * both are freed on every path out, throws and cancellation included.
     */
    private suspend fun readVideoAudio(
        block: DesignBlock,
        engine: Engine,
        destination: File,
    ): BlockAudio? {
        if (!engine.block.supportsFill(block)) return null
        val fill = engine.block.getFill(block)
        if (!isAudible(fill, engine)) return null
        engine.block.forceLoadAVResource(fill)
        if (engine.block.getAudioTrackCountFromVideo(fill) <= 0) return null

        // `createAudioFromVideo` pushes an undo step of its own, so the extraction runs against a scratch history
        // the user never sees. Scoped to one video: the active history is swapped for as long as it is held.
        return withScratchHistory(engine) {
            val audioBlock = engine.block.createAudioFromVideo(
                videoFill = fill,
                trackIndex = 0,
                // The whole track, untrimmed: cue timestamps are source seconds and `timelineCues` applies the window.
                options = AudioFromVideoOptions(keepTrimSettings = false, muteOriginalVideo = false),
            )
            val bufferUri = runCatching { audioFileUri(audioBlock, engine) }.getOrNull()
            try {
                if (bufferUri == null || !writeAudio(bufferUri, engine, destination)) {
                    return@withScratchHistory null
                }
                blockAudio(destination, "audio/mp4", Source.Video, of = block, trimmedBy = fill, engine = engine)
            } finally {
                runCatching { engine.block.destroy(audioBlock) }
                // Scheme check: only a minted buffer may be destroyed, never a URI resolving to a user's own file.
                if (bufferUri != null && AudioReader.of(bufferUri.scheme) == AudioReader.Buffer) {
                    runCatching { engine.editor.destroyBuffer(bufferUri) }
                }
            }
        }
    }

    /** The source an audio block reads from; `null` when `audio/fileURI` is unset. */
    private fun audioFileUri(
        audioBlock: DesignBlock,
        engine: Engine,
    ): Uri? = engine.block.getString(audioBlock, "audio/fileURI").takeIf { it.isNotBlank() }?.let(Uri::parse)

    /**
     * Copies an audio source into [destination] a chunk at a time; `false` when the source is empty.
     *
     * Never materialised as one array: a scene's audio is unbounded, and a single long recording is enough to
     * exhaust the heap on its own.
     */
    private suspend fun writeAudio(
        uri: Uri,
        engine: Engine,
        destination: File,
    ): Boolean {
        val written = when (AudioReader.of(uri.scheme)) {
            AudioReader.Buffer -> writeBuffer(uri, engine, destination)
            AudioReader.Resource -> writeResource(uri, engine, destination)
            AudioReader.Remote -> writeRemote(uri, destination)
        }
        if (written <= 0L) withContext(Dispatchers.IO) { destination.delete() }
        return written > 0L
    }

    /**
     * Sliced rather than read whole: `getBufferData` allocates whatever length it is asked for, so the request
     * itself is what has to stay bounded.
     *
     * The read stays on the calling thread — the engine is single-threaded, so reading its buffer from a worker
     * would be reaching across that boundary — while each slice is written off it.
     */
    private suspend fun writeBuffer(
        uri: Uri,
        engine: Engine,
        destination: File,
    ): Long {
        val length = engine.editor.getBufferLength(uri)
        if (length <= 0) return 0L
        var written = 0
        while (written < length) {
            val chunk = engine.editor.getBufferData(
                uri = uri,
                offset = written,
                length = minOf(RESOURCE_CHUNK_BYTES, length - written),
            )
            val bytes = chunk.remaining()
            // A short read would otherwise spin: the offset never advances past it.
            if (bytes <= 0) break
            val slice = chunk.toByteArray()
            withContext(Dispatchers.IO) { destination.appendBytes(slice) }
            written += bytes
        }
        return written.toLong()
    }

    /**
     * Only resources the engine already holds are readable, which the preceding `forceLoadAVResource` guarantees.
     *
     * Each chunk is copied inside the callback: the engine hands out a view it may reuse once the callback
     * returns. The copy crosses to an I/O worker, since writing here would trip StrictMode on the main thread.
     *
     * A failed hand-off stops the walk by returning `false` rather than throwing, so the exception is raised here
     * instead of unwinding through the engine's callback.
     */
    private suspend fun writeResource(
        uri: Uri,
        engine: Engine,
        destination: File,
    ): Long = coroutineScope {
        // One slot rather than the default rendezvous, which would make the blocking send below wait for the
        // writer on every chunk. One is enough for a write to overlap the next read.
        val chunks = Channel<ByteArray>(capacity = 1)
        val writer = async(Dispatchers.IO) {
            var written = 0L
            destination.outputStream().buffered().use { output ->
                for (chunk in chunks) {
                    output.write(chunk)
                    written += chunk.size
                }
            }
            written
        }
        // A writer that died leaves nobody draining, and the send below runs on its own job, so cancelling the
        // channel is what stops it parking forever.
        writer.invokeOnCompletion { if (it != null) chunks.cancel() }
        var failure: Throwable? = null
        try {
            engine.editor.getResourceData(uri, chunkSize = RESOURCE_CHUNK_BYTES) { chunk ->
                val bytes = chunk.toByteArray()
                try {
                    // The engine's callback cannot suspend, so backpressure blocks here — a wait, not I/O.
                    runBlocking { chunks.send(bytes) }
                    true
                } catch (throwable: Throwable) {
                    failure = throwable
                    false
                }
            }
        } finally {
            chunks.close()
        }
        // Awaited before the send's own failure is raised: a cancelled channel throws CancellationException,
        // which reads as a user cancel and would be swallowed instead of the write error that caused it.
        val written = writer.await()
        failure?.let { throw it }
        written
    }

    /** OkHttp rather than a plain connection so cancelling the generation aborts a download in progress. */
    private suspend fun writeRemote(
        uri: Uri,
        destination: File,
    ): Long = httpClient.newCall(Request.Builder().url(uri.toString()).build()).useCancellable { response ->
        if (!response.isSuccessful) {
            throw IOException("Downloading audio from $uri returned HTTP ${response.code}.")
        }
        val body = response.body ?: return@useCancellable 0L
        // Streamed into the file instead of `body.bytes()`, which would hold the whole download in memory.
        withContext(Dispatchers.IO) {
            body.byteStream().use { input -> destination.outputStream().buffered().use(input::copyTo) }
        }
    }

    /**
     * Takes the timeline placement from [of] and clips cues to [trimmedBy]'s trim window and speed. A speed the
     * engine cannot report — or reports as non-positive or non-finite — falls back to real time.
     */
    private fun blockAudio(
        file: File,
        mimeType: String,
        source: Source,
        of: DesignBlock,
        trimmedBy: DesignBlock,
        engine: Engine,
    ): BlockAudio {
        val trimOffset = runCatching { engine.block.getTrimOffset(trimmedBy) }.getOrDefault(0.0)
        val trimLength = runCatching { engine.block.getTrimLength(trimmedBy) }.getOrDefault(0.0)
        val speed = runCatching { engine.block.getPlaybackSpeed(trimmedBy).toDouble() }.getOrDefault(1.0)
        return BlockAudio(
            file = file,
            mimeType = mimeType,
            source = source,
            timeOffset = runCatching { engine.block.getTimeOffset(of) }.getOrDefault(0.0),
            windowStart = trimOffset,
            windowEnd = if (trimLength > 0) trimOffset + trimLength else Double.POSITIVE_INFINITY,
            speed = if (speed.isFinite() && speed > 0) speed else 1.0,
        )
    }

    private fun isAudible(
        block: DesignBlock,
        engine: Engine,
    ): Boolean = !engine.block.isMuted(block) && engine.block.getVolume(block) > 0

    /**
     * The registered IANA type for an audio MIME the engine reports under a common alias — it answers `audio/mp3`
     * for an MP3, which services reject in favour of `audio/mpeg`.
     */
    internal fun String?.canonicalAudioMimeType(): String {
        val type = this?.substringBefore(';')?.trim()?.lowercase()
        return when (type) {
            null, "" -> "audio/mpeg"
            "audio/mp3", "audio/x-mp3", "audio/mpeg3", "audio/x-mpeg3" -> "audio/mpeg"
            "audio/m4a", "audio/x-m4a" -> "audio/mp4"
            else -> type
        }
    }

    // endregion
    // region Transcription (parallel, network-bound)

    private suspend fun transcribe(
        audios: List<BlockAudio>,
        provider: TranscriptionProvider,
        options: TranscriptionOptions,
    ): List<SubtitleCue> {
        // Each result is captured rather than thrown so one block's failure does not cancel its siblings — but a
        // cancellation still has to propagate, which `runCatching` alone would swallow.
        val results = coroutineScope {
            audios
                .map { audio ->
                    async {
                        try {
                            Result.success(
                                TranscribedBlock(
                                    srt = provider.transcribe(audio.file, audio.mimeType, options),
                                    source = audio.source,
                                    timeOffset = audio.timeOffset,
                                    windowStart = audio.windowStart,
                                    windowEnd = audio.windowEnd,
                                    speed = audio.speed,
                                ),
                            )
                        } catch (cancellation: CancellationException) {
                            throw cancellation
                        } catch (throwable: Throwable) {
                            Result.failure(throwable)
                        }
                    }
                }
                .awaitAll()
        }

        val blocks = results.mapNotNull { it.getOrNull() }
        val firstError = results
            .mapNotNull { it.exceptionOrNull()?.also { error -> Log.e(TAG, "Transcription failed via ${provider.name}", error) } }
            .firstOrNull()
        // A cancelled provider call surfaces as its transport's error; report the cancellation instead.
        currentCoroutineContext().ensureActive()

        val cues = timelineCues(blocks)
        // A silent (empty-success) block must not mask a sibling's transport error as "no speech".
        if (cues.isEmpty() && firstError != null) throw firstError
        return cues
    }

    // endregion

    /** Shared by every remote audio fetch; the gateway provider owns its own client with longer timeouts. */
    private val httpClient by lazy { OkHttpClient() }

    private const val TAG = "AutoCaptions"

    private const val RESOURCE_CHUNK_BYTES = 1 shl 20

    /**
     * A pause this short inside one source's speech stays claimed by that source. Long enough to cover the beat
     * between two sentences, short enough that a real silence is handed back to whatever else is audible.
     */
    private const val MAX_CLAIMED_PAUSE_SECONDS = 1.5

    /** A heuristic, not a contract: the kind the editor stamps on a recorded voiceover. Any other kind is still transcribed, it just doesn't get the narration ranking. */
    private const val VOICEOVER_KIND = "voiceover"
}

private fun java.nio.ByteBuffer.toByteArray() = ByteArray(remaining()).also(::get)
