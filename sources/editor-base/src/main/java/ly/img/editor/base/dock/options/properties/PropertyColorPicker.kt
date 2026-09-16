package ly.img.editor.base.dock.options.properties

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import ly.img.editor.base.dock.options.fillstroke.ColorPickerSheet
import ly.img.editor.base.engine.PropertyAndValue
import ly.img.editor.base.engine.PropertyValue
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.core.event.EditorEvent
import ly.img.engine.DesignBlock

@Composable
fun PropertyColorPicker(
    designBlock: DesignBlock,
    propertyAndValue: PropertyAndValue,
    onBack: () -> Unit,
    onEvent: (EditorEvent) -> Unit,
    onColorChangeFinished: () -> Unit = {},
) {
    val (property, value) = propertyAndValue
    ColorPickerSheet(
        color = (value as PropertyValue.Color).value ?: Color.Black,
        title = property.title.value,
        showOpacity = false,
        onBack = onBack,
        onColorChange = { color ->
            BlockEvent.OnChangeProperty(
                designBlock = designBlock,
                property = property,
                newValue = PropertyValue.Color(color),
            ).let { onEvent(it) }
        },
        onEvent = onEvent,
        onColorChangeFinished = {
            onEvent(BlockEvent.OnChangeFinish)
            onColorChangeFinished()
        },
    )
}
