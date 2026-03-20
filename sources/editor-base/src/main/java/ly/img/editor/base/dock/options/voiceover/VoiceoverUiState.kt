package ly.img.editor.base.dock.options.voiceover

import ly.img.editor.core.EditorScope
import ly.img.engine.DesignBlock

internal data class VoiceoverUiState(
    val draftVoiceOverBlock: DesignBlock,
    val previousSelectedBlocks: List<DesignBlock>,
)

internal object VoiceoverUiStateFactory {
    fun create(editorScope: EditorScope): VoiceoverUiState? {
        val editorContext = with(editorScope) { editorContext }
        val engine = editorContext.engine
        val page = engine.scene.getCurrentPage() ?: return null
        val currentPlaybackTime = runCatching { engine.block.getPlaybackTime(page) }.getOrDefault(0.0).coerceAtLeast(0.0)
        val draft = VoiceoverEngineBlocks.findVoiceOverDraftOnPage(engine, page) ?: VoiceoverEngineBlocks.createDraftBlock(
            engine = engine,
            page = page,
            timeOffsetSeconds = currentPlaybackTime,
        ) ?: return null
        val previousSelectedBlocks = engine.block.findAllSelected()
            .filter { selected: DesignBlock -> engine.block.isValid(selected) }
        editorContext.updateVoiceOverSheetTarget(draft)
        runCatching { engine.block.setTimeOffset(draft, currentPlaybackTime) }
        runCatching { engine.block.setDuration(draft, 0.0) }
        previousSelectedBlocks
            .forEach { selected: DesignBlock -> runCatching { engine.block.setSelected(selected, false) } }
        runCatching { engine.block.setSelected(draft, true) }
        return VoiceoverUiState(
            draftVoiceOverBlock = draft,
            previousSelectedBlocks = previousSelectedBlocks,
        )
    }
}
