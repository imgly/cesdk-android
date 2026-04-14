package ly.img.editor.base.dock.options.speed

import ly.img.editor.core.engine.getPlaybackControlBlock
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine

data class SpeedUiState(
    val speed: Float,
    val durationSeconds: Double?,
    val maxSpeed: Float,
) {
    companion object Factory {
        fun create(
            designBlock: DesignBlock,
            engine: Engine,
        ): SpeedUiState {
            val isAudio = DesignBlockType.get(engine.block.getType(designBlock)) == DesignBlockType.Audio
            val maxSpeed = if (isAudio) MAX_AUDIO_SPEED else MAX_VIDEO_SPEED
            val playbackBlock =
                requireNotNull(engine.block.getPlaybackControlBlock(designBlock)) {
                    "Playback control block missing for speed UI."
                }
            return SpeedUiState(
                speed = engine.block.getPlaybackSpeed(playbackBlock),
                durationSeconds =
                    designBlock
                        .takeIf { engine.block.supportsDuration(it) }
                        ?.let(engine.block::getDuration),
                maxSpeed = maxSpeed,
            )
        }
    }
}

private const val MAX_AUDIO_SPEED = 3f
private const val MAX_VIDEO_SPEED = 10f
