package ly.img.camera.core

/**
 * Determines the layout of the camera in case of dual camera or reactions.
 */
enum class CameraLayoutMode {
    /**
     * Displays two video feeds, one on top of the other.
     */
    Vertical,

    /**
     * Displays two video feeds next to each other.
     */
    Horizontal,
}
