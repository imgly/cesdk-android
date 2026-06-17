package ly.img.camera.core

/**
 * Determines whether the camera produces a single capture per session or a stack of captures.
 *
 * Defaults to [Multi] to preserve the existing camera behavior.
 */
enum class CaptureCount {
    /**
     * The camera produces exactly one capture and finishes. The recording-segments ring
     * and the delete-last-clip button are hidden.
     */
    Single,

    /**
     * The camera produces a list of captures stacked into the recording-segments ring.
     */
    Multi,
}
