package ly.img.camera.components.sidemenu.layout

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import ly.img.camera.R
import ly.img.camera.components.sidemenu.SideMenuItem

@Composable
internal fun LayoutMenuItem(
    layoutMode: LayoutMode,
    enabled: Boolean,
    expanded: Boolean,
    onClick: () -> Unit,
    onDropdownMenuDismiss: () -> Unit,
    setLayoutMode: (LayoutMode) -> Unit,
) {
    Box {
        var isLayoutMenuItemSelected by remember { mutableStateOf(false) }

        SideMenuItem(
            imageVector = layoutMode.icon,
            contentDescription = R.string.ly_img_camera_reaction,
            label = R.string.ly_img_camera_reaction,
            checked = true,
            expanded = expanded,
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

        DropdownMenu(
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
