package ly.img.editor.base.ui

import androidx.annotation.StringRes
import androidx.compose.material3.SnackbarDuration
import ly.img.editor.compose.bottomsheet.ModalBottomSheetValue

/**
 * To communicate one-time events from the ViewModel to the UI. These are not part of the state.
 */
sealed interface SingleEvent {
    data class Exit(
        val throwable: Throwable?,
    ) : SingleEvent

    data class ChangeSheetState(
        val state: ModalBottomSheetValue,
        val animate: Boolean,
    ) : SingleEvent

    data object HideScrimSheet : SingleEvent

    data class Snackbar(
        @StringRes val text: Int,
        val duration: SnackbarDuration = SnackbarDuration.Short,
    ) : SingleEvent
}
