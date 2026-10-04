package ly.img.camera.components.sidemenu

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import ly.img.editor.core.ui.iconpack.Check
import ly.img.editor.core.ui.iconpack.IconPack
import ly.img.editor.core.ui.utils.ifTrue

@Composable
internal fun CameraDropdownMenuItem(
    checked: Boolean,
    @StringRes label: Int,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        modifier = Modifier.ifTrue(checked) {
            background(MaterialTheme.colorScheme.surfaceVariant)
        },
        text = { Text(stringResource(id = label)) },
        onClick = onClick,
        leadingIcon = {
            if (checked) {
                Icon(IconPack.Check, contentDescription = null)
            }
        },
        trailingIcon = { Icon(icon, contentDescription = null) },
    )
}
