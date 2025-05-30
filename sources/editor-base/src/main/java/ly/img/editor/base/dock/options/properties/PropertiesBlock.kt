package ly.img.editor.base.dock.options.properties

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ly.img.editor.base.components.PropertyPicker
import ly.img.editor.base.components.PropertySlider
import ly.img.editor.base.components.PropertySwitch
import ly.img.editor.base.components.SectionHeader
import ly.img.editor.base.components.colorpicker.ColorPickerButton
import ly.img.editor.base.dock.options.fillstroke.ColorOptions
import ly.img.editor.base.engine.DesignBlockWithProperties
import ly.img.editor.base.engine.PropertyAndValue
import ly.img.editor.base.engine.PropertyValue
import ly.img.editor.base.engine.PropertyValueType
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.compose.material3.Card
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.ui.UiDefaults
import ly.img.editor.core.ui.sheetScrollableContentModifier
import ly.img.engine.DesignBlock
import kotlin.math.roundToInt

@Composable
fun PropertiesBlock(
    designBlockWithProperties: DesignBlockWithProperties,
    onEvent: (EditorEvent) -> Unit,
    onOpenColorPicker: (PropertyAndValue) -> Unit,
) {
    Column(
        Modifier.sheetScrollableContentModifier(),
    ) {
        designBlockWithProperties.properties.forEach {
            when (it.property.valueType) {
                is PropertyValueType.Int -> IntProperty(
                    designBlock = designBlockWithProperties.designBlock,
                    propertyAndValue = it,
                    onEvent = onEvent,
                )

                is PropertyValueType.Float -> FloatProperty(
                    designBlock = designBlockWithProperties.designBlock,
                    propertyAndValue = it,
                    onEvent = onEvent,
                )

                is PropertyValueType.Double -> DoubleProperty(
                    designBlock = designBlockWithProperties.designBlock,
                    propertyAndValue = it,
                    onEvent = onEvent,
                )

                PropertyValueType.Boolean -> BooleanProperty(
                    designBlock = designBlockWithProperties.designBlock,
                    propertyAndValue = it,
                    onEvent = onEvent,
                )

                is PropertyValueType.Color -> ColorProperty(
                    designBlock = designBlockWithProperties.designBlock,
                    propertyAndValue = it,
                    onOpenColorPicker = onOpenColorPicker,
                    onEvent = onEvent,
                )

                is PropertyValueType.String -> { /* no impl for now */ }

                is PropertyValueType.StringEnum -> StringEnumProperty(
                    designBlock = designBlockWithProperties.designBlock,
                    propertyAndValue = it,
                    onEvent = onEvent,
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun IntProperty(
    designBlock: DesignBlock,
    propertyAndValue: PropertyAndValue,
    onEvent: (EditorEvent) -> Unit,
) {
    val (property, value) = propertyAndValue
    PropertySlider(
        title = property.titleRes,
        value = (value as PropertyValue.Int).value.toFloat(),
        valueRange = remember((property.valueType as PropertyValueType.Int).range) {
            property.valueType.range.let {
                it.first.toFloat()..it.last.toFloat()
            }
        },
        onValueChange = { newValue ->
            BlockEvent.OnChangeProperty(
                designBlock = designBlock,
                property = property,
                newValue = PropertyValue.Int(newValue.roundToInt()),
            ).let { onEvent(it) }
        },
        onValueChangeFinished = { onEvent(BlockEvent.OnChangeFinish) },
    )
}

@Composable
private fun FloatProperty(
    designBlock: DesignBlock,
    propertyAndValue: PropertyAndValue,
    onEvent: (EditorEvent) -> Unit,
) {
    val (property, value) = propertyAndValue
    PropertySlider(
        title = property.titleRes,
        value = (value as PropertyValue.Float).value,
        valueRange = (property.valueType as PropertyValueType.Float).range,
        onValueChange = { newValue ->
            BlockEvent.OnChangeProperty(
                designBlock = designBlock,
                property = property,
                newValue = PropertyValue.Float(newValue),
            ).let { onEvent(it) }
        },
        onValueChangeFinished = { onEvent(BlockEvent.OnChangeFinish) },
    )
}

@Composable
private fun DoubleProperty(
    designBlock: DesignBlock,
    propertyAndValue: PropertyAndValue,
    onEvent: (EditorEvent) -> Unit,
) {
    val (property, value) = propertyAndValue
    PropertySlider(
        title = property.titleRes,
        value = (value as PropertyValue.Double).value.toFloat(),
        valueRange = remember((property.valueType as PropertyValueType.Double).range) {
            property.valueType.range.let {
                it.start.toFloat()..it.endInclusive.toFloat()
            }
        },
        onValueChange = { newValue ->
            BlockEvent.OnChangeProperty(
                designBlock = designBlock,
                property = property,
                newValue = PropertyValue.Double(newValue.toDouble()),
            ).let { onEvent(it) }
        },
        onValueChangeFinished = { onEvent(BlockEvent.OnChangeFinish) },
    )
}

@Composable
private fun ColorProperty(
    designBlock: DesignBlock,
    propertyAndValue: PropertyAndValue,
    onOpenColorPicker: (PropertyAndValue) -> Unit,
    onEvent: (EditorEvent) -> Unit,
) {
    val valueType = propertyAndValue.property.valueType as PropertyValueType.Color
    if (valueType.colorPalette == null) {
        ColorPropertyWithPickerOnly(
            propertyAndValue = propertyAndValue,
            onOpenColorPicker = onOpenColorPicker,
        )
    } else {
        ColorPropertyWithListAndPicker(
            designBlock = designBlock,
            colors = valueType.colorPalette,
            propertyAndValue = propertyAndValue,
            onOpenColorPicker = onOpenColorPicker,
            onEvent = onEvent,
        )
    }
}

@Composable
private fun ColorPropertyWithListAndPicker(
    designBlock: DesignBlock,
    colors: List<Color>,
    propertyAndValue: PropertyAndValue,
    onOpenColorPicker: (PropertyAndValue) -> Unit,
    onEvent: (EditorEvent) -> Unit,
) {
    val (property, value) = propertyAndValue
    val currentColor = (value as PropertyValue.Color).value ?: Color.Black
    SectionHeader(text = property.titleRes)
    androidx.compose.material3.Card(
        colors = UiDefaults.cardColors,
    ) {
        ColorOptions(
            enabled = value.value != null,
            selectedColor = currentColor,
            onNoColorSelected = {
                BlockEvent.OnChangeProperty(
                    designBlock = designBlock,
                    property = property,
                    newValue = PropertyValue.Color(null),
                ).let { onEvent(it) }
                onEvent(BlockEvent.OnChangeFinish)
            },
            onColorSelected = { newValue ->
                BlockEvent.OnChangeProperty(
                    designBlock = designBlock,
                    property = property,
                    newValue = PropertyValue.Color(newValue),
                ).let { onEvent(it) }
                onEvent(BlockEvent.OnChangeFinish)
            },
            openColorPicker = { onOpenColorPicker(propertyAndValue) },
            colors = colors,
        )
    }
}

@Composable
private fun ColorPropertyWithPickerOnly(
    propertyAndValue: PropertyAndValue,
    onOpenColorPicker: (PropertyAndValue) -> Unit,
) {
    val (property, value) = propertyAndValue
    val currentColor = (value as PropertyValue.Color).value ?: Color.Black
    Card(
        colors = UiDefaults.cardColorsExperimental,
        onClick = { onOpenColorPicker(propertyAndValue) },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(id = property.titleRes),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
            ColorPickerButton(
                color = currentColor,
                onClick = null,
            )
        }
    }
}

@Composable
private fun BooleanProperty(
    designBlock: DesignBlock,
    propertyAndValue: PropertyAndValue,
    onEvent: (EditorEvent) -> Unit,
) {
    val (property, value) = propertyAndValue
    androidx.compose.material3.Card(
        colors = UiDefaults.cardColors,
    ) {
        PropertySwitch(
            title = stringResource(property.titleRes),
            isChecked = (value as PropertyValue.Boolean).value,
            onPropertyChange = { newValue ->
                BlockEvent.OnChangeProperty(
                    designBlock = designBlock,
                    property = property,
                    newValue = PropertyValue.Boolean(newValue),
                ).let { onEvent(it) }
                onEvent(BlockEvent.OnChangeFinish)
            },
        )
    }
}

@Composable
private fun StringEnumProperty(
    designBlock: DesignBlock,
    propertyAndValue: PropertyAndValue,
    onEvent: (EditorEvent) -> Unit,
) {
    val (property, value) = propertyAndValue
    val options = (property.valueType as PropertyValueType.StringEnum).options
    val propertyTextRes = remember(options, value) {
        options.first { it.value == (value as PropertyValue.Enum).value }.textRes
    }
    androidx.compose.material3.Card(
        colors = UiDefaults.cardColors,
    ) {
        PropertyPicker(
            title = stringResource(property.titleRes),
            propertyTextRes = propertyTextRes,
            properties = options,
            onPropertyPicked = { newValue ->
                BlockEvent.OnChangeProperty(
                    designBlock = designBlock,
                    property = property,
                    newValue = PropertyValue.Enum(newValue),
                ).let { onEvent(it) }
                onEvent(BlockEvent.OnChangeFinish)
            },
        )
    }
}
