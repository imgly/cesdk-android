package ly.img.editor.base.dock.options.captions

import ly.img.editor.core.EditorScope
import ly.img.engine.DesignBlock

/** What the captions sheet needs to know at the moment it opens. */
internal data class CaptionsUiState(
    /** Caption selected when the sheet opened, so the list can open on that row. `null` when opened from the dock. */
    val deepLinkTarget: DesignBlock?,
)

/** Builds the sheet's initial state, or `null` when there is no page to host captions. */
internal object CaptionsUiStateFactory {
    fun create(editorScope: EditorScope): CaptionsUiState? {
        val editorContext = with(editorScope) { editorContext }
        val engine = editorContext.engine
        // A caption track hangs off a page, so without one the sheet has nothing to do.
        engine.scene.getCurrentPage() ?: return null
        return CaptionsUiState(deepLinkTarget = CaptionsEngine(engine).selectedCaption())
    }
}
