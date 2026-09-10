package ly.img.editor.base.dock.options.voiceover

import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import java.util.ArrayDeque
import kotlin.math.max

internal object VoiceoverTimelineSync {
    fun extendPageDurationIfNeeded(
        engine: Engine,
        page: DesignBlock,
        durationSeconds: Double,
    ) {
        if (durationSeconds <= PLAYBACK_END_EPSILON_SECONDS || !engine.block.isValid(page)) return
        val currentDuration = runCatching { engine.block.getDuration(page) }.getOrDefault(0.0)
        if (durationSeconds > currentDuration + PLAYBACK_END_EPSILON_SECONDS) {
            runCatching { engine.block.setDuration(page, durationSeconds) }
        }
    }

    fun syncPageDurationToContentEnd(
        engine: Engine,
        page: DesignBlock,
    ) {
        if (!engine.block.isValid(page)) return
        val contentEnd = calculateContentEndDuration(engine, page)
        if (contentEnd > PLAYBACK_END_EPSILON_SECONDS) {
            runCatching { engine.block.setDuration(page, contentEnd) }
        }
        // When we set the page duration manually, it becomes the duration source.
        // We need to unset it so the engine can always calculate the page duration correctly.
        runCatching {
            if (engine.block.isPageDurationSource(page)) {
                engine.block.removePageDurationSource(page)
            }
        }
    }

    private fun calculateContentEndDuration(
        engine: Engine,
        root: DesignBlock,
    ): Double {
        val toVisit = ArrayDeque<DesignBlock>()
        var maxEnd = 0.0
        toVisit.add(root)
        while (toVisit.isNotEmpty()) {
            val current = toVisit.removeFirst()
            runCatching { engine.block.getChildren(current) }
                .getOrDefault(emptyList())
                .forEach { child -> toVisit.add(child) }

            if (current == root || !engine.block.isValid(current)) continue

            val timeOffset = runCatching { engine.block.getTimeOffset(current) }.getOrDefault(0.0)
            val duration = runCatching { engine.block.getDuration(current) }.getOrDefault(0.0)
            maxEnd = max(maxEnd, timeOffset + duration)
        }
        return maxEnd
    }
}
