package ly.img.camera.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

internal class CameraPositionTransition<T>(
    initialPosition: T,
    initialOrientation: Int,
) {
    var position by mutableStateOf(initialPosition)
    var orientation by mutableStateOf(initialOrientation)
    var isMoving by mutableStateOf(false)
    val alpha = Animatable(1f)

    fun rotationFor(
        targetPosition: T,
        animatedRotation: () -> Float,
    ): Float = if (position != targetPosition || isMoving) orientation.toFloat() else animatedRotation()
}

@Composable
internal fun <T> rememberCameraPositionTransition(
    targetPosition: T,
    orientation: Int,
): CameraPositionTransition<T> {
    val transition = remember { CameraPositionTransition(targetPosition, orientation) }
    LaunchedEffect(targetPosition, orientation) {
        if (transition.position != targetPosition || (transition.isMoving && transition.orientation != orientation)) {
            transition.isMoving = true
            transition.alpha.animateTo(0f, tween(durationMillis = 300))
            // Keep the same composable instance and its state when moving it.
            transition.position = targetPosition
        }
        // Moving controls change orientation only while hidden; stationary controls use the shared rotation animation.
        transition.orientation = orientation
        transition.alpha.animateTo(1f, tween(durationMillis = 300))
        transition.isMoving = false
    }
    return transition
}
