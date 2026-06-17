package ly.img.camera.util

/**
 * To communicate one-time events from the ViewModel to the UI. These are not part of the state.
 */
internal sealed interface SingleEvent {
    data object ErrorLoadingVideo : SingleEvent
}
