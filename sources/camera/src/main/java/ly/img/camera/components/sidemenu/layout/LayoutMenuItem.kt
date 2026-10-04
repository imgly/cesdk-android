package ly.img.camera.components.sidemenu.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ly.img.camera.components.sidemenu.CameraDropdownMenu
import ly.img.camera.components.sidemenu.SideMenuItem
import ly.img.camera.core.R

@Composable
internal fun LayoutMenuItem(
    layoutMode: LayoutMode,
    enabled: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    onDropdownMenuDismiss: () -> Unit,
    setLayoutMode: (LayoutMode) -> Unit,
    orientation: Int,
    rotation: () -> Float,
) {
    Box {
        var isLayoutMenuItemSelected by remember { mutableStateOf(false) }

        SideMenuItem(
            imageVector = layoutMode.icon,
            contentDescription = R.string.ly_img_camera_button_reaction,
            label = R.string.ly_img_camera_button_reaction,
            checked = true,
            expanded = expanded,
            alignToEnd = orientation == 270,
            enabled = enabled,
            onClick = {
                isLayoutMenuItemSelected = true
                onClick()
            },
        )

        fun onDropdownMenuItemClick(layoutMode: LayoutMode) {
            isLayoutMenuItemSelected = false
            setLayoutMode(layoutMode)
            onDropdownMenuDismiss()
        }

        CameraDropdownMenu(
            orientation = orientation,
            rotation = rotation,
            expanded = isLayoutMenuItemSelected,
            onDismissRequest = {
                isLayoutMenuItemSelected = false
                onDropdownMenuDismiss()
            },
        ) {
            val layoutModes = listOf(LayoutMode.Vertical, LayoutMode.Horizontal)
            layoutModes.forEach { item ->
                LayoutModeDropdownMenuItem(
                    layoutMode = item,
                    currentLayoutMode = layoutMode,
                    onClick = ::onDropdownMenuItemClick,
                )
            }
        }
    }
}
