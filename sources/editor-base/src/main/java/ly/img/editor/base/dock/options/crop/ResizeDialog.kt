package ly.img.editor.base.dock.options.crop

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import ly.img.editor.base.R
import ly.img.editor.base.components.PropertyItem
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.compose.foundation.focusable
import ly.img.editor.compose.material3.InputChip
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.ui.iconpack.IconPack
import ly.img.editor.core.ui.iconpack.LockClose
import ly.img.editor.core.ui.iconpack.LockOpen
import ly.img.engine.DesignUnit
import kotlin.math.roundToInt

private fun Float.cleanString(): String = if (this % 1f == 0f) this.toInt().toString() else this.toString()

@Composable
fun ResizeDialog(
    uiState: ResizeUiState,
    applyOnAllPages: Boolean = false,
    onEvent: (EditorEvent) -> Unit,
    onClose: () -> Unit,
) {
    var width by remember {
        mutableStateOf(TextFieldValue(uiState.width.toStringWithoutTailingZeros(uiState.unit.native)))
    }
    var height by remember {
        mutableStateOf(TextFieldValue(uiState.height.toStringWithoutTailingZeros(uiState.unit.native)))
    }
    var unit by remember { mutableStateOf(uiState.unit) }
    var unitValue by remember {
        mutableStateOf(
            when (unit.native) {
                DesignUnit.PIXEL -> uiState.pixelScaleFactor
                else -> uiState.dpi.toFloat()
            },
        )
    }
    var isAspectRatioLocked by remember { mutableStateOf(false) }
    var keepAspect by remember { mutableStateOf(uiState.width / uiState.height) }
    var isUnitDropdownOpen by remember { mutableStateOf(false) }
    var isUnitValueDropdownOpen by remember { mutableStateOf(false) }

    fun onApply() {
        val newUnitValue = unitValue
        onEvent(
            BlockEvent.OnChangePageSize(
                width = width.text.toFloatOrNull() ?: uiState.width,
                height = height.text.toFloatOrNull() ?: uiState.height,
                unit = unit.native,
                unitValue = newUnitValue,
                applyOnAllPages = applyOnAllPages,
            ),
        )
        onClose()
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 16.dp, bottom = 24.dp, start = 8.dp, end = 8.dp),
                text = stringResource(R.string.ly_img_editor_resize),
                // fontSize = 24.sp,
                style = MaterialTheme.typography.headlineSmall,
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        InputChip(
            selected = isAspectRatioLocked,
            label = {
                Text(
                    text = stringResource(R.string.ly_img_editor_aspect_lock),
                    style = MaterialTheme.typography.labelMedium,
                    color = if (isAspectRatioLocked) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            },
            leadingIcon = {
                Icon(
                    imageVector = if (isAspectRatioLocked) IconPack.LockClose else IconPack.LockOpen,
                    contentDescription = null,
                    tint = if (isAspectRatioLocked) {
                        MaterialTheme.colorScheme.onSecondaryContainer
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    modifier = Modifier.size(20.dp).padding(2.dp),
                )
            },
            onClick = {
                val widthValue = width.text.toFloatOrNull()
                val heightValue = height.text.toFloatOrNull()
                if (widthValue != null && heightValue != null) {
                    keepAspect = widthValue / heightValue
                    isAspectRatioLocked = !isAspectRatioLocked
                }
            },
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            OutlinedTextField(
                value = width,
                onValueChange = {
                    width = it
                    val newWidth = it.text.toFloatOrNull()
                    if (isAspectRatioLocked && newWidth != null) {
                        height = height.copy((newWidth / keepAspect).toStringWithoutTailingZeros(unit.native))
                    }
                },
                isError = width.text.isEmpty() || width.text.toFloatOrNull() == null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = {
                    Text(
                        stringResource(
                            R.string.ly_img_editor_unit_width,
                            unit.unitSuffix,
                        ),
                    )
                },
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = height,
                onValueChange = {
                    height = it
                    val newHeight = it.text.toFloatOrNull()
                    if (isAspectRatioLocked && newHeight != null) {
                        width = width.copy((newHeight * keepAspect).toStringWithoutTailingZeros(unit.native))
                    }
                },
                isError = height.text.isEmpty() || height.text.toFloatOrNull() == null,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                label = {
                    Text(
                        stringResource(
                            R.string.ly_img_editor_unit_height,
                            unit.unitSuffix,
                        ),
                    )
                },
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = stringResource(unit.titleRes),
                    onValueChange = {},
                    singleLine = true,
                    label = { Text(stringResource(R.string.ly_img_editor_unit_title)) },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusable(false),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { isUnitDropdownOpen = true },
                )
                DropdownMenu(
                    modifier = Modifier
                        .wrapContentWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    expanded = isUnitDropdownOpen,
                    onDismissRequest = { isUnitDropdownOpen = false },
                ) {
                    uiState.units.forEach { mode ->
                        PropertyItem(
                            checked = unit == mode,
                            textRes = mode.titleRes,
                            onClick = {
                                val oldUnit = unit
                                unit = mode
                                unitValue = when (mode.native) {
                                    DesignUnit.INCH, DesignUnit.MILLIMETER -> uiState.dpi.toFloat()
                                    DesignUnit.PIXEL -> uiState.pixelScaleFactor
                                }
                                width = convertUnit(
                                    from = oldUnit.native,
                                    to = mode.native,
                                    dpi = uiState.dpi.toFloat(),
                                    pixelScale = uiState.pixelScaleFactor,
                                    value = width,
                                )
                                height = convertUnit(
                                    from = oldUnit.native,
                                    to = mode.native,
                                    dpi = uiState.dpi.toFloat(),
                                    pixelScale = uiState.pixelScaleFactor,
                                    value = height,
                                )
                                isUnitDropdownOpen = false
                            },
                        )
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                OutlinedTextField(
                    value = "${unitValue.cleanString()}${unit.valueSuffix}",
                    onValueChange = { },
                    singleLine = true,
                    label = { Text(stringResource(unit.unitValueNameRes)) },
                    readOnly = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusable(false),
                )
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clickable { isUnitValueDropdownOpen = true },
                )
                DropdownMenu(
                    modifier = Modifier
                        .wrapContentWidth()
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surface),
                    expanded = isUnitValueDropdownOpen,
                    onDismissRequest = { isUnitValueDropdownOpen = false },
                ) {
                    unit.values.forEach { value ->
                        PropertyItem(
                            checked = unitValue == value,
                            text = "${value.cleanString()}${unit.valueSuffix}",
                            onClick = {
                                unitValue = value
                                isUnitValueDropdownOpen = false
                            },
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.weight(1f))
            TextButton(
                onClick = onClose,
                modifier = Modifier.weight(1f),
            ) {
                Text(stringResource(ly.img.editor.core.R.string.ly_img_editor_cancel))
            }

            fun hasChanges(): Boolean = (
                (width.text.isNotEmpty() && width.text.toFloatOrNull() != null) &&
                    (height.text.isNotEmpty() && height.text.toFloatOrNull() != null)
            ) &&
                (
                    width.text.toFloatOrNull() != uiState.width ||
                        height.text.toFloatOrNull() != uiState.height ||
                        unit != uiState.unit ||
                        when (uiState.unit.native) {
                            DesignUnit.INCH, DesignUnit.MILLIMETER -> unitValue != uiState.dpi.toFloat()
                            DesignUnit.PIXEL -> unitValue != uiState.pixelScaleFactor
                        }
                )

            TextButton(
                onClick = { onApply() },
                modifier = Modifier.weight(1f),
                enabled = hasChanges(),
            ) {
                Text(stringResource(ly.img.editor.core.R.string.ly_img_editor_apply))
            }
        }
    }
}

@Composable
@Preview(showBackground = true)
fun DefaultPreview() {
    Dialog(onDismissRequest = { }) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
        ) {
            ResizeDialog(
                uiState = ResizeUiState(
                    width = 800f,
                    height = 600f,
                    dpi = 1,
                    pixelScaleFactor = 1f,
                    unit = DesignUnitEntry(
                        titleRes = R.string.ly_img_editor_unit_inch,
                        native = DesignUnit.INCH,
                        values = listOf(72f, 150f, 300f, 600f, 1200f, 2400f),
                        defaultValue = 300f,
                        valueSuffix = " dpi",
                        unitValueNameRes = R.string.ly_img_editor_unit_dpi_value_name,
                        unitSuffix = "inch",
                    ),
                ),
                onEvent = {},
                onClose = {},
            )
        }
    }
}

private fun convertUnit(
    from: DesignUnit,
    to: DesignUnit,
    dpi: Float,
    pixelScale: Float,
    value: TextFieldValue,
): TextFieldValue {
    val newValue = value.text.toFloatOrNull()?.let {
        value.copy(
            convertUnit(from, to, dpi, pixelScale, value.text.toFloatOrNull() ?: 0f)
                .toStringWithoutTailingZeros(to),
        )
    }
    return newValue ?: value
}

private const val MILLIMETER_PER_INCH = 25.4

private fun convertUnit(
    from: DesignUnit,
    to: DesignUnit,
    dpi: Float,
    pixelScale: Float,
    value: Float,
): Float {
    val valueInInches: Float = when (from) {
        DesignUnit.INCH -> value
        DesignUnit.MILLIMETER -> (value / MILLIMETER_PER_INCH).toFloat()
        DesignUnit.PIXEL -> value / (dpi * pixelScale)
        else -> value
    }

    val convertedValue: Float = when (to) {
        DesignUnit.INCH -> valueInInches
        DesignUnit.MILLIMETER -> (valueInInches * MILLIMETER_PER_INCH).toFloat()
        DesignUnit.PIXEL -> valueInInches * dpi * pixelScale
        else -> value
    }

    return convertedValue
}

fun Float.toStringWithoutTailingZeros(unit: DesignUnit): String {
    if (unit == DesignUnit.PIXEL) {
        return this.roundToInt().toString()
    } else {
        val value = this.toString()
        return if (value.contains('.')) {
            value.replace(Regex("0+\$"), "").replace(Regex("\\.\$"), "")
        } else {
            value
        }
    }
}
