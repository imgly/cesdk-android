package ly.img.camera.preview

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size

/** Converts portrait preview coordinates to the upright capture canvas. */
internal fun Rect.toCaptureOrientation(
    orientation: Int,
    canvasSize: Size,
): Rect = when (orientation) {
    0 -> this
    90 -> Rect(top, canvasSize.width - right, bottom, canvasSize.width - left)
    180 -> Rect(canvasSize.width - right, canvasSize.height - bottom, canvasSize.width - left, canvasSize.height - top)
    270 -> Rect(canvasSize.height - bottom, left, canvasSize.height - top, right)
    else -> error("Camera orientation must be 0, 90, 180, or 270 degrees.")
}
