package ly.img.camera.components.sidemenu

import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.unit.constrainHeight
import kotlinx.coroutines.delay
import ly.img.camera.components.sidemenu.layout.LayoutMenuItem
import ly.img.camera.components.sidemenu.layout.LayoutMode
import ly.img.camera.components.sidemenu.timer.TimerMenuItem
import ly.img.camera.record.Timer

@Composable
internal fun SideMenu(
    modifier: Modifier = Modifier,
    timer: Timer,
    setTimer: (Timer) -> Unit,
    layoutMode: LayoutMode?,
    setLayoutMode: (LayoutMode) -> Unit,
    layoutModeEnabled: Boolean,
    orientation: Int,
    rotation: () -> Float,
) {
    Layout(
        modifier = modifier.width(IntrinsicSize.Max),
        content = {
            var expanded by remember { mutableStateOf(true) }
            var itemInteractedToggle by remember { mutableStateOf(false) }

            LaunchedEffect(itemInteractedToggle) {
                if (itemInteractedToggle) return@LaunchedEffect
                delay(2000)
                expanded = false
            }

            fun onItemClick() {
                itemInteractedToggle = true
                expanded = true
            }

            fun onDropdownMenuDismiss() {
                itemInteractedToggle = false
            }

            TimerMenuItem(
                orientation = orientation,
                timer = timer,
                expanded = expanded,
                onClick = ::onItemClick,
                onDropdownMenuDismiss = ::onDropdownMenuDismiss,
                setTimer = setTimer,
                rotation = rotation,
            )

            if (layoutMode != null) {
                LayoutMenuItem(
                    orientation = orientation,
                    layoutMode = layoutMode,
                    enabled = layoutModeEnabled,
                    expanded = expanded,
                    onClick = ::onItemClick,
                    onDropdownMenuDismiss = ::onDropdownMenuDismiss,
                    setLayoutMode = setLayoutMode,
                    rotation = rotation,
                )
            }
        },
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
        val timerPlaceable = placeables.first()
        val remainingHeight = placeables.drop(1).sumOf { it.height }
        // Balance the other rows with space above the timer so its icon stays at the center.
        val height = constraints.constrainHeight(timerPlaceable.height + 2 * remainingHeight)
        layout(placeables.maxOf { it.width }, height) {
            var y = (height - timerPlaceable.height) / 2
            placeables.forEach { placeable ->
                placeable.placeRelative(0, y)
                y += placeable.height
            }
        }
    }
}
