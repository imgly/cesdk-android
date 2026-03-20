package ly.img.editor.base.dock.options.voiceover

import ly.img.editor.base.ui.EditorUiViewModel
import ly.img.editor.core.EditorContext
import ly.img.engine.DesignBlock

private val EditorContext.voiceOverViewModel: EditorUiViewModel?
    get() = eventHandler as? EditorUiViewModel

internal val EditorContext.isVoiceOverRecordingInProgress: Boolean
    get() = voiceOverViewModel?.isVoiceOverRecordingInProgress == true

internal fun EditorContext.updateVoiceOverRecordingInProgress(value: Boolean) {
    voiceOverViewModel?.updateVoiceOverRecordingInProgress(value)
}

internal fun EditorContext.updateVoiceOverSheetTarget(targetBlock: DesignBlock?) {
    voiceOverViewModel?.updateVoiceOverSheetTarget(targetBlock)
}
