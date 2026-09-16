package ly.img.camera.core

/**
 * Determines what kind of media the camera can capture.
 *
 * Defaults to [Video] to preserve the existing camera behavior.
 *
 * Not all combinations of [CameraMode] and [CaptureType] are supported:
 * see [CameraMode.supports] for the allowed pairings.
 */
enum class CaptureType {
    /**
     * The camera captures still photos only. Tap-to-shoot. Microphone permission is not requested.
     */
    Photo,

    /**
     * The camera captures videos only. This is the legacy behavior.
     */
    Video,

    /**
     * The camera captures both photos and videos. The user picks the active sub-mode
     * via a photo↔video toggle rendered in the bottom-center of the camera UI.
     */
    Mixed,
}
