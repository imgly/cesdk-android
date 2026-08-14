package ly.img.editor.base.timeline.state

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ly.img.editor.core.ui.engine.getCurrentPage
import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

/** Plays a single animation or transition after the engine has applied the edit that triggered it. */
class AnimationPreview(
    private val engine: Engine,
    private val coroutineScope: CoroutineScope,
) {
    private var job: Job? = null
    private var isPreviewPlaying = false
    private var previewId = 0L
    private val page = engine.getCurrentPage()

    fun playAnimation(
        clip: DesignBlock,
        mode: Mode,
    ) {
        val animation = when (mode) {
            Mode.In -> engine.block.getInAnimation(clip)
            Mode.Out -> engine.block.getOutAnimation(clip)
            Mode.Loop -> engine.block.getLoopAnimation(clip)
        }
        if (!engine.block.isValid(animation) || !engine.block.supportsDuration(animation)) return
        play(clip, engine.block.getDuration(animation).seconds, mode == Mode.Out)
    }

    fun playTransition(outgoingBlock: DesignBlock) {
        val transition = engine.block.getTransition(outgoingBlock)
        if (!engine.block.isValid(transition) || !engine.block.supportsDuration(transition)) return

        val incomingClip = engine.transitionIncomingClip(outgoingBlock) ?: return
        if (!engine.block.supportsDuration(outgoingBlock) ||
            !engine.block.supportsDuration(incomingClip)
        ) {
            return
        }

        val duration = minOf(
            engine.block.getDuration(transition),
            engine.block.getDuration(outgoingBlock) / 2,
            engine.block.getDuration(incomingClip) / 2,
        ).seconds
        play(outgoingBlock, duration, endAligned = true)
    }

    /** Stops both a pending preview and the preview playback it started. */
    fun stop() {
        previewId++
        val hasPreview = job != null || isPreviewPlaying
        job?.cancel()
        job = null
        if (hasPreview && engine.block.isPlaying(page)) {
            engine.block.setPlaying(page, false)
        }
        isPreviewPlaying = false
    }

    private fun play(
        clip: DesignBlock,
        duration: Duration,
        endAligned: Boolean,
    ) {
        stop()
        if (duration <= ZERO || !engine.block.supportsDuration(clip)) return

        val clipOffset = engine.block.getTimeOffset(clip).seconds
        val clipDuration = engine.block.getDuration(clip).seconds
        val previewDuration = duration.coerceAtMost(clipDuration)
        val start = if (endAligned) clipOffset + clipDuration - previewDuration else clipOffset

        engine.block.setPlaying(page, false)
        engine.block.setPlaybackTime(page, start.toDouble(DurationUnit.SECONDS))

        val id = ++previewId
        job = coroutineScope.launch {
            // Let the renderer and media decoder present the sought frame before playback starts.
            // This also ensures asset application has settled before the preview samples it.
            delay(PLAYBACK_START_DELAY_MILLIS)
            if (id != previewId) return@launch
            engine.block.setPlaying(page, true)
            isPreviewPlaying = true
            delay(previewDuration.inWholeMilliseconds)
            if (id == previewId && isPreviewPlaying) {
                engine.block.setPlaying(page, false)
                isPreviewPlaying = false
                job = null
            }
        }
    }

    enum class Mode {
        In,
        Out,
        Loop,
    }

    private companion object {
        const val PLAYBACK_START_DELAY_MILLIS = 100L
    }
}
