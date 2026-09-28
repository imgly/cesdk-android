package ly.img.editor.base.timeline.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import ly.img.editor.base.timeline.state.TimelineConfiguration
import ly.img.editor.core.LocalEditorScope
import ly.img.editor.core.component.EditorComponent
import ly.img.editor.core.component.HorizontalListBuilder

@Composable
fun PlayerHeader(headerListBuilder: HorizontalListBuilder<EditorComponent<*>>) {
    val scope = LocalEditorScope.current
    val alignedData = headerListBuilder.build(scope)
    // Removing every header item removes the bar itself, otherwise an empty strip stays behind.
    if (alignedData.values.none { data -> data.items.any { it.visible } }) return

    Box(
        Modifier
            .fillMaxWidth()
            .height(TimelineConfiguration.headerHeight)
            .alpha(0.95f),
        contentAlignment = Alignment.Center,
    ) {
        alignedData.forEach { (alignment, data) ->
            if (alignment == null) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    data.items.forEach { EditorComponent(component = it) }
                }
            } else {
                // Wrapped in a Column because the list builder gives a horizontal alignment, which
                // only ColumnScope.align accepts. A Box would need a two-dimensional one.
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.align(alignment),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = data.arrangement ?: Arrangement.Start,
                    ) {
                        data.items.forEach { EditorComponent(component = it) }
                    }
                }
            }
        }
    }
}
