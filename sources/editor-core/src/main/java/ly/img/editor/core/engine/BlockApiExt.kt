package ly.img.editor.core.engine

import ly.img.engine.BlockApi
import ly.img.engine.DesignBlock

/**
 * Returns the block that exposes playback control for the given [designBlock].
 */
fun BlockApi.getPlaybackControlBlock(designBlock: DesignBlock): DesignBlock? = when {
    supportsPlaybackControl(designBlock) -> designBlock
    supportsFill(designBlock) -> {
        val fill = getFill(designBlock)
        if (supportsPlaybackControl(fill)) {
            fill
        } else {
            null
        }
    }
    else -> null
}
