package ly.img.editor.base.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import ly.img.editor.core.ui.iconpack.Check
import ly.img.editor.core.ui.iconpack.IconPack
import ly.img.editor.core.ui.library.data.font.FontData
import ly.img.editor.core.ui.utils.fontFamily
import ly.img.editor.core.ui.utils.ifTrue

@Composable
fun CheckedTextRow(
    isChecked: Boolean,
    text: String,
    icon: ImageVector? = null,
    fontData: FontData? = null,
    onClick: (() -> Unit)? = null,
) {
    Row(
        Modifier
            .ifTrue(isChecked) { background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)) }
            // Selectable, not clickable, so accessibility services announce which row is the checked one.
            .then(
                if (onClick != null) {
                    Modifier.selectable(selected = isChecked, role = Role.RadioButton, onClick = onClick)
                } else {
                    Modifier.semantics { selected = isChecked }
                },
            )
            .heightIn(min = 56.dp) // to ensure the UI doesn't jump around when the check is toggled
            .padding(vertical = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CompositionLocalProvider(
            LocalContentColor provides if (isChecked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        ) {
            if (isChecked) {
                Icon(
                    IconPack.Check,
                    contentDescription = null,
                    Modifier.padding(horizontal = 16.dp),
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = fontData?.fontFamily,
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(
                        start = if (isChecked) 0.dp else 56.dp,
                        end = if (icon != null) 0.dp else 24.dp,
                    ),
            )
            if (icon != null) {
                Icon(icon, contentDescription = null, Modifier.padding(start = 16.dp, end = 24.dp))
            }
        }
    }
}
