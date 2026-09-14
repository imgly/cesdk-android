package ly.img.editor.base.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ly.img.editor.core.theme.surface1
import ly.img.editor.core.ui.utils.ifTrue

@Composable
fun PropertySwitch(
    title: String,
    enabled: Boolean = true,
    isChecked: Boolean,
    onPropertyChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface1)
            .ifTrue(enabled) {
                clickable { onPropertyChange(isChecked.not()) }
            }
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(start = 16.dp),
        )
        Switch(
            modifier = Modifier.padding(end = 16.dp),
            checked = isChecked,
            onCheckedChange = onPropertyChange,
        )
    }
}

@Preview(showBackground = false)
@Composable
fun PreviewPropertySwitch() {
    PropertySwitch(
        title = "Title",
        isChecked = true,
        onPropertyChange = {},
    )
}
