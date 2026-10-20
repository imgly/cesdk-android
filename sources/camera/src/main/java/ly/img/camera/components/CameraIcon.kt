package ly.img.camera.components

import android.view.OrientationEventListener
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
internal fun rememberCameraIconRotation(displayRotation: Int): State<Float> {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val targetRotation = remember { mutableFloatStateOf(0f) }
    var deviceOrientation by remember { mutableIntStateOf(OrientationEventListener.ORIENTATION_UNKNOWN) }

    LaunchedEffect(deviceOrientation, displayRotation) {
        if (deviceOrientation == OrientationEventListener.ORIENTATION_UNKNOWN) return@LaunchedEffect
        val relativeOrientation = (deviceOrientation + displayRotation * 90) % 360
        val delta = (-relativeOrientation - targetRotation.floatValue + 180).mod(360f) - 180
        // Switch 15 degrees past the midpoint to avoid jitter, and animate along the shortest arc.
        if (abs(delta) >= 60) {
            targetRotation.floatValue += (delta / 90).roundToInt() * 90f
        }
    }

    DisposableEffect(context, lifecycle) {
        // Phones can stay portrait-locked, so configuration changes alone cannot track device orientation.
        val listener = object : OrientationEventListener(context) {
            override fun onOrientationChanged(orientation: Int) {
                if (orientation == ORIENTATION_UNKNOWN) return
                deviceOrientation = orientation
            }
        }
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> if (listener.canDetectOrientation()) listener.enable()
                Lifecycle.Event.ON_PAUSE -> listener.disable()
                else -> Unit
            }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            listener.disable()
        }
    }

    return targetRotation
}

@Composable
internal fun CameraIcon(
    imageVector: ImageVector,
    rotation: () -> Float,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tint: Color = LocalContentColor.current,
) {
    Icon(
        imageVector = imageVector,
        contentDescription = contentDescription,
        modifier = modifier.graphicsLayer { rotationZ = rotation() },
        tint = tint,
    )
}
