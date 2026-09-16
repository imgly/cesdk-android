package ly.img.editor.base.components

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ly.img.editor.base.engine.PropertyText
import ly.img.editor.core.theme.surface1
import ly.img.editor.core.ui.iconpack.Arrowdropdown
import ly.img.editor.core.ui.iconpack.Check
import ly.img.editor.core.ui.iconpack.IconPack
import ly.img.editor.core.ui.utils.ifTrue

@Composable
fun <T> PropertyPicker(
    title: String,
    propertyValue: T,
    enabled: Boolean = true,
    properties: List<PropertyOption<T>>,
    onPropertyPicked: (T) -> Unit,
) {
    var showMenu by remember { mutableStateOf(false) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface1)
            .ifTrue(enabled) {
                clickable { showMenu = true }
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
        CompositionLocalProvider(
            LocalContentColor provides
                if (enabled) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                },
        ) {
            Row(
                modifier = Modifier.padding(ButtonDefaults.TextButtonContentPadding),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                val text = remember(properties, propertyValue) {
                    properties.firstOrNull { it.value == propertyValue }?.text
                }?.value ?: ""
                Text(
                    text = text,
                    modifier = Modifier.padding(horizontal = 10.dp),
                    style = MaterialTheme.typography.labelLarge,
                )
                Box {
                    Icon(IconPack.Arrowdropdown, contentDescription = null)
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                    ) {
                        properties.forEach { property ->
                            if (property.selectable) {
                                PropertyItem(
                                    checked = property.value == propertyValue,
                                    text = property.text.value,
                                    icon = property.icon,
                                    onClick = {
                                        onPropertyPicked(property.value)
                                        showMenu = false
                                    },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PropertyItem(
    checked: Boolean,
    @StringRes textRes: Int,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) = PropertyItem(
    checked = checked,
    text = stringResource(textRes),
    icon = icon,
    onClick = onClick,
)

@Composable
fun PropertyItem(
    checked: Boolean,
    text: String,
    icon: ImageVector? = null,
    onClick: () -> Unit,
) {
    val contentColor = if (checked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    DropdownMenuItem(
        modifier = Modifier.ifTrue(checked) {
            background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
        },
        colors = MenuDefaults.itemColors(
            textColor = contentColor,
            leadingIconColor = contentColor,
            trailingIconColor = contentColor,
        ),
        text = { Text(text) },
        onClick = onClick,
        leadingIcon = {
            if (checked) {
                Icon(
                    imageVector = IconPack.Check,
                    contentDescription = null,
                )
            }
        },
        trailingIcon = {
            icon?.let {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                )
            }
        },
    )
}

data class PropertyOption<T>(
    val text: PropertyText,
    val value: T,
    val icon: ImageVector? = null,
    /** This should be false only in case our UI does not support the engine value, i.e.
     * FillType.ConicalGradient or FillType.RadialGradient.
     */
    val selectable: Boolean = true,
)
