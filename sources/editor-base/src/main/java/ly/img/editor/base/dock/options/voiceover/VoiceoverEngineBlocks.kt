package ly.img.editor.base.dock.options.voiceover

import ly.img.editor.core.engine.getPlaybackControlBlock
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import java.util.ArrayDeque

internal object VoiceoverEngineBlocks {
    fun isValidBlock(
        engine: Engine,
        block: DesignBlock,
    ): Boolean = runCatching {
        engine.block.isValid(block)
    }.getOrDefault(false)

    fun getAudioResourceUri(
        engine: Engine,
        block: DesignBlock,
    ): String? = runCatching {
        engine.block.getString(block, "audio/fileURI")
    }.getOrNull()?.takeIf { it.isNotBlank() }

    fun hasCommittedAudioResource(
        engine: Engine,
        block: DesignBlock,
    ): Boolean = getAudioResourceUri(engine, block)
        ?.startsWith("buffer://")
        ?.not() == true

    fun findVoiceOverDraftOnPage(
        engine: Engine,
        page: DesignBlock,
    ): DesignBlock? = runCatching {
        engine.block.getChildren(page)
            .firstOrNull { child ->
                isVoiceOverBlock(engine, child) && !hasCommittedAudioResource(engine, child)
            }
    }.getOrNull()

    fun collectPlaybackAudioBlocks(
        engine: Engine,
        page: DesignBlock,
    ): List<DesignBlock> {
        val toVisit = ArrayDeque<DesignBlock>()
        val playbackBlocks = linkedSetOf<DesignBlock>()
        toVisit.add(page)
        while (toVisit.isNotEmpty()) {
            val current = toVisit.removeFirst()
            val playbackBlock = runCatching {
                engine.block.getPlaybackControlBlock(current)
            }.getOrNull()
            if (playbackBlock != null) {
                val supportsMute = runCatching { engine.block.isMuted(playbackBlock) }.isSuccess
                if (supportsMute) {
                    playbackBlocks.add(playbackBlock)
                }
            }
            runCatching { engine.block.getChildren(current) }
                .getOrDefault(emptyList())
                .forEach { child -> toVisit.add(child) }
        }
        return playbackBlocks.toList()
    }

    fun createDraftBlock(
        engine: Engine,
        page: DesignBlock,
        timeOffsetSeconds: Double,
    ): DesignBlock? = runCatching {
        val voiceOverBlock = engine.block.create(DesignBlockType.Audio)
        engine.block.appendChild(parent = page, child = voiceOverBlock)
        engine.block.setKind(voiceOverBlock, VOICEOVER_KIND)
        engine.block.setLooping(voiceOverBlock, false)
        engine.block.setAlwaysOnTop(voiceOverBlock, true)
        engine.block.setTimeOffset(voiceOverBlock, timeOffsetSeconds)
        runCatching { engine.block.setDuration(voiceOverBlock, 0.0) }
        voiceOverBlock
    }.getOrNull()

    private fun isVoiceOverBlock(
        engine: Engine,
        block: DesignBlock,
    ): Boolean = runCatching {
        DesignBlockType.get(engine.block.getType(block)) == DesignBlockType.Audio &&
            engine.block.getKind(block) == VOICEOVER_KIND
    }.getOrDefault(false)
}
