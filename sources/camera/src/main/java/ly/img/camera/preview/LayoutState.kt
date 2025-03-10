package ly.img.camera.preview

import android.graphics.RectF
import ly.img.camera.components.sidemenu.layout.LayoutMode
import ly.img.camera.core.CameraMode

internal class LayoutState(
    private val cameraMode: CameraMode,
) {
    var rect1 = Rect.fullScreen
    var rect2: RectF? = null

    val cameraRect: RectF
        get() = when (cameraMode) {
            is CameraMode.Reaction -> rect2!!
            is CameraMode.Standard -> rect1
        }

    var swapPositions = (cameraMode as? CameraMode.Reaction)?.positionsSwapped ?: false
        private set

    fun toggleSwapPositions() {
        swapPositions = !swapPositions
    }

    fun updateLayout(layoutMode: LayoutMode?) {
        when (layoutMode) {
            LayoutMode.Vertical -> {
                rect1 = if (swapPositions) Rect.bottomRect else Rect.topRect
                rect2 = if (swapPositions) Rect.topRect else Rect.bottomRect
            }
            LayoutMode.Horizontal -> {
                rect1 = if (swapPositions) Rect.rightRect else Rect.leftRect
                rect2 = if (swapPositions) Rect.leftRect else Rect.rightRect
            }
            null -> {
                rect1 = Rect.fullScreen
                rect2 = null
            }
        }
    }
}
