package ly.img.camera.components

import android.hardware.display.DisplayManager
import android.os.Handler
import android.os.Looper
import android.view.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView

@Composable
internal fun rememberCameraDisplayRotation(): State<Int> {
    val context = LocalContext.current
    val view = LocalView.current
    val rotation = remember(view) { mutableIntStateOf(view.display?.rotation ?: Surface.ROTATION_0) }

    DisposableEffect(context, view) {
        val displayManager = context.getSystemService(DisplayManager::class.java)
        val listener = object : DisplayManager.DisplayListener {
            override fun onDisplayAdded(displayId: Int) = Unit

            override fun onDisplayRemoved(displayId: Int) = Unit

            override fun onDisplayChanged(displayId: Int) {
                if (view.display?.displayId == displayId) {
                    rotation.intValue = view.display?.rotation ?: Surface.ROTATION_0
                }
            }
        }
        displayManager.registerDisplayListener(listener, Handler(Looper.getMainLooper()))
        rotation.intValue = view.display?.rotation ?: Surface.ROTATION_0
        onDispose { displayManager.unregisterDisplayListener(listener) }
    }

    return rotation
}
