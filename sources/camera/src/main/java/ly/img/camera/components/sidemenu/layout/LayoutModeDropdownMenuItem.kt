package ly.img.camera.components.sidemenu.layout

import androidx.compose.foundation.background
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import ly.img.editor.core.ui.iconpack.Check
import ly.img.editor.core.ui.iconpack.IconPack
import ly.img.editor.core.ui.utils.ifTrue

@Composable
internal fun LayoutModeDropdownMenuItem(
    layoutMode: LayoutMode,
    currentLayoutMode: LayoutMode,
    onClick: (LayoutMode) -> Unit,
) {
    val checked = layoutMode == currentLayoutMode
    DropdownMenuItem(
        modifier = Modifier.ifTrue(checked) {
            background(MaterialTheme.colorScheme.surfaceVariant)
        },
        text = { Text(stringResource(id = layoutMode.label)) },
        onClick = {
            onClick(layoutMode)
        },
        leadingIcon = {
            if (checked) {
                Icon(IconPack.Check, contentDescription = null)
            }
        },
        trailingIcon = { Icon(layoutMode.icon, contentDescription = null) },
    )
}
