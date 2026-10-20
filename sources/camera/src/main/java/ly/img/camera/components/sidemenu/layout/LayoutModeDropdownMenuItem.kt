package ly.img.camera.components.sidemenu.layout

import androidx.compose.runtime.Composable
import ly.img.camera.components.sidemenu.CameraDropdownMenuItem

@Composable
internal fun LayoutModeDropdownMenuItem(
    layoutMode: LayoutMode,
    currentLayoutMode: LayoutMode,
    onClick: (LayoutMode) -> Unit,
) {
    CameraDropdownMenuItem(
        checked = layoutMode == currentLayoutMode,
        label = layoutMode.label,
        icon = layoutMode.icon,
        onClick = { onClick(layoutMode) },
    )
}
