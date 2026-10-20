package ly.img.camera.components.sidemenu.timer

import androidx.compose.runtime.Composable
import ly.img.camera.components.sidemenu.CameraDropdownMenuItem
import ly.img.camera.record.Timer

@Composable
internal fun TimerDropdownMenuItem(
    timer: Timer,
    currentTimer: Timer,
    onClick: (Timer) -> Unit,
) {
    CameraDropdownMenuItem(
        checked = timer == currentTimer,
        label = timer.label,
        icon = timer.icon,
        onClick = { onClick(timer) },
    )
}
