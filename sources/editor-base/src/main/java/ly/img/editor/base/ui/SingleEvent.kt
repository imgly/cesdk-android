package ly.img.editor.base.ui

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarDuration
import ly.img.editor.core.sheet.SheetValue

/**
 * To communicate one-time events from the ViewModel to the UI. These are not part of the state.
 */
sealed interface SingleEvent {
    data class Exit(
        val throwable: Throwable?,
    ) : SingleEvent

    data class ChangeSheetState(
        val state: SheetValue,
        val animate: Boolean,
    ) : SingleEvent

    data object HideScrimSheet : SingleEvent

    data class Snackbar(
        @StringRes val text: Int,
        val duration: SnackbarDuration = SnackbarDuration.Short,
    ) : SingleEvent

    /**
     * A non-fatal notice carrying a [Throwable] whose customer-facing copy is resolved at the UI
     * layer via `EngineException.getDisplayMessage`. Used for recoverable failures (e.g. an asset that
     * can't be applied) that should not surface in the terminal error dialog.
     */
    data class SnackbarError(
        val throwable: Throwable,
        val duration: SnackbarDuration = SnackbarDuration.Short,
    ) : SingleEvent
}
