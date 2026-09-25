package ly.img.editor.core.ui.library.components

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun ClipMenuItem(
    @StringRes textResourceId: Int,
    icon: ImageVector,
    onClick: () -> Unit,
) {
    DropdownMenuItem(
        text = {
            Text(
                text = stringResource(id = textResourceId),
                modifier = Modifier.widthIn(min = 72.dp),
            )
        },
        trailingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
            )
        },
        onClick = onClick,
    )
}
