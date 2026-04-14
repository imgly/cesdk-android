package ly.img.editor.base.ui.handler

import ly.img.editor.base.engine.isParentBackgroundTrack
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.core.R
import ly.img.editor.core.engine.getPlaybackControlBlock
import ly.img.editor.core.ui.EventsHandler
import ly.img.editor.core.ui.engine.getCurrentPage
import ly.img.editor.core.ui.inject
import ly.img.editor.core.ui.library.engine.isVideoBlock
import ly.img.editor.core.ui.register
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine

/**
 * Register events related to clip playback speed.
 * @param engine Lambda returning the engine instance
 * @param block Lambda returning the block instance
 * @param showToast Lambda for surfacing short user messages
 */
@Suppress("NAME_SHADOWING")
fun EventsHandler.speedEvents(
    engine: () -> Engine,
    block: () -> DesignBlock,
    showToast: (Int) -> Unit,
) {
    val engine by inject(engine)
    val block by inject(block)
    var wasOverAudioCutoff = false
    var lastAudioCutoffBlock: DesignBlock? = null

    register<BlockEvent.OnPlaybackSpeedChange> {
        if (lastAudioCutoffBlock != block) {
            lastAudioCutoffBlock = block
            wasOverAudioCutoff = false
        }
        val playbackBlock = engine.block.getPlaybackControlBlock(block)
        if (playbackBlock != null) {
            val blockType = DesignBlockType.get(engine.block.getType(block))
            val newSpeed =
                if (blockType == DesignBlockType.Audio) {
                    it.speed.coerceAtMost(SPEED_AUDIO_MAX)
                } else {
                    it.speed
                }
            if (engine.block.supportsDuration(block) && newSpeed > 0f) {
                val currentDuration = engine.block.getDuration(block)
                val currentSpeed = engine.block.getPlaybackSpeed(playbackBlock)
                val newDuration = (currentDuration * currentSpeed) / newSpeed
                val shouldCheckCollision = !engine.block.isParentBackgroundTrack(block)
                if (shouldCheckCollision && detectClipCollision(engine, block, newDuration)) {
                    moveClipToNewTrack(engine, block)
                }
            }
            engine.block.setPlaybackSpeed(playbackBlock, newSpeed)
            val isOverAudioCutoff = newSpeed > SPEED_AUDIO_CUTOFF && engine.block.isVideoBlock(block)
            if (isOverAudioCutoff && !wasOverAudioCutoff) {
                showToast(R.string.ly_img_editor_notification_speed_no_audio_at_speed)
            }
            wasOverAudioCutoff = isOverAudioCutoff
        }
    }
}

private const val SPEED_AUDIO_CUTOFF = 3f
private const val SPEED_AUDIO_MAX = 3f
private const val TRACK_AUTO_OFFSET_KEY = "track/automaticallyManageBlockOffsets"

private fun detectClipCollision(
    engine: Engine,
    block: DesignBlock,
    newDuration: Double,
): Boolean {
    val track = getParentTrack(engine, block) ?: return false
    val trackChildren = engine.block.getChildren(track)
    if (trackChildren.size < 2) return false

    val currentStartTime = engine.block.getTimeOffset(block)
    val newEndTime = currentStartTime + newDuration
    val nextClipStartTime =
        trackChildren
            .asSequence()
            .filter { it != block }
            .map { engine.block.getTimeOffset(it) }
            .filter { it > currentStartTime }
            .minOrNull()
            ?: return false

    return newEndTime > nextClipStartTime
}

private fun moveClipToNewTrack(
    engine: Engine,
    block: DesignBlock,
): DesignBlock? {
    val page = engine.getCurrentPage()
    val currentTrack = getParentTrack(engine, block) ?: return null
    val pageChildren = engine.block.getChildren(page)
    val trackIndex = pageChildren.indexOf(currentTrack)
    if (trackIndex == -1) return null

    val currentTimeOffset = engine.block.getTimeOffset(block)
    val newTrack = engine.block.create(DesignBlockType.Track)
    engine.block.insertChild(parent = page, child = newTrack, index = trackIndex + 1)
    engine.block.setBoolean(newTrack, TRACK_AUTO_OFFSET_KEY, false)
    engine.block.appendChild(parent = newTrack, child = block)
    engine.block.setTimeOffset(block, currentTimeOffset)
    return newTrack
}

private fun getParentTrack(
    engine: Engine,
    block: DesignBlock,
): DesignBlock? {
    var parent = engine.block.getParent(block) ?: return null
    while (engine.block.isValid(parent)) {
        if (engine.block.getType(parent) == DesignBlockType.Track.key) {
            return parent
        }
        parent = engine.block.getParent(parent) ?: return null
    }
    return null
}
