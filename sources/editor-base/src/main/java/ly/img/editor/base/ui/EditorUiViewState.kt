package ly.img.editor.base.ui

import android.content.Context
import android.content.Intent
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Stable
import ly.img.editor.base.timeline.state.TimelineState
import ly.img.editor.core.event.EditorEvent

@Stable
data class EditorUiViewState(
    val isInPreviewMode: Boolean = false,
    val allowEditorInteraction: Boolean = false,
    val selectedBlock: Block? = null,
    val isEditingText: Boolean = false,
    val timelineMaxHeightInDp: Float = Float.MAX_VALUE,
    val timelineState: TimelineState? = null,
    val pageCount: Int = 0,
    val isSceneLoaded: Boolean = false,
    val timelineTrigger: Boolean = false,
    val pagesState: EditorPagesState? = null,
    val openContract: EditorEvent.LaunchContract<*, *> = EditorEvent.LaunchContract(
        contract = DummyContract,
        input = Unit,
        onOutput = {},
    ),
)

object DummyContract : ActivityResultContract<Unit, Unit>() {
    override fun createIntent(
        context: Context,
        input: Unit,
    ): Intent {
        error("Should never enter")
    }

    override fun parseResult(
        resultCode: Int,
        intent: Intent?,
    ) {
        // FIXME: We should never enter here, but in the case of process restoration, we actually do reach here because we don't have a way
        //  to restore the originally registered contract correctly at the moment.
        // No-op. The result will simply be ignored.
    }
}
