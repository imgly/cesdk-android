package ly.img.editor.base.dock.options.properties

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import ly.img.editor.base.components.NestedSheetHeader
import ly.img.editor.base.engine.DesignBlockWithProperties
import ly.img.editor.base.engine.PropertyAndValue
import ly.img.editor.core.event.EditorEvent

@Composable
fun PropertiesSheet(
    title: String,
    designBlockWithProperties: DesignBlockWithProperties,
    onBack: () -> Unit,
    onEvent: (EditorEvent) -> Unit,
    onOpenColorPicker: (PropertyAndValue) -> Unit,
) {
    BackHandler {
        onBack()
    }
    NestedSheetHeader(
        title = title,
        onBack = onBack,
        onClose = { onEvent(EditorEvent.Sheet.Close(animate = true)) },
    )
    PropertiesBlock(
        designBlockWithProperties = designBlockWithProperties,
        onEvent = onEvent,
        onOpenColorPicker = onOpenColorPicker,
    )
}
