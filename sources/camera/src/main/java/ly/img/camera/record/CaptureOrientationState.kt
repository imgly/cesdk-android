package ly.img.camera.record

internal class CaptureOrientationState {
    private var previewOrientation = 0
    private var displayRotation = 0
    private var lockedTargetRotation: Int? = null
    private var lockedOutputOrientation: Int? = null

    // Output rectangles share the first take's canvas even when the preview display rotates.
    val outputOrientation: Int
        get() = lockedOutputOrientation ?: previewOrientation

    val orientation: Int
        get() = lockedTargetRotation?.let { (it - displayRotation).mod(4) * 90 } ?: previewOrientation

    // Preview orientation is relative to the display; CameraX uses the device's natural orientation.
    val targetRotation: Int
        get() = lockedTargetRotation ?: (displayRotation + previewOrientation / 90) % 4

    fun update(
        orientation: Int,
        displayRotation: Int,
    ) {
        previewOrientation = orientation
        this.displayRotation = displayRotation
    }

    fun lock() {
        if (lockedTargetRotation == null) {
            lockedOutputOrientation = outputOrientation
            lockedTargetRotation = targetRotation
        }
    }

    fun unlock() {
        lockedTargetRotation = null
        lockedOutputOrientation = null
    }
}
