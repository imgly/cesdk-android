package ly.img.editor.base.timeline.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ly.img.editor.core.UnstableEditorApi
import ly.img.editor.core.component.EditorComponent
import ly.img.editor.core.component.Timeline

@OptIn(UnstableEditorApi::class)
@Composable
fun AddClipButton(
    button: Timeline.AddClipButton,
    cameraContent: (@Composable () -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val scope = button.scope
    val options = requireNotNull(button.optionsBuilder.build(scope)[null]?.items) {
        "optionsBuilder should call add { ... } without any VerticalArrangement parameter."
    }
    if (options.isEmpty()) return

    // The camera launches through a composable contract, so the timeline hands its content back
    // here to be rendered next to the button.
    cameraContent?.invoke()

    var showClipMenu by remember { mutableStateOf(false) }
    // A lone entry has no menu to open, so clicking the button triggers it directly.
    val singleOption = options.singleOrNull()

    Box(modifier = modifier) {
        TimelineButton(
            text = { button.text?.invoke(scope) },
            icon = button.icon?.let { { it(scope) } },
            containerColor = button.containerColor,
            tint = button.tint,
            contentPadding = button.contentPadding,
            enabled = button.enabled && (singleOption?.enabled ?: true),
        ) {
            if (singleOption != null) {
                singleOption.onClick(scope)
            } else {
                showClipMenu = true
            }
        }
        if (options.size > 1) {
            DropdownMenu(
                expanded = showClipMenu,
                onDismissRequest = { showClipMenu = false },
            ) {
                options.forEachIndexed { index, option ->
                    DropdownMenuItem(
                        text = { EditorComponent(component = option) },
                        enabled = option.enabled,
                        onClick = {
                            showClipMenu = false
                            option.onClick(scope)
                        },
                    )
                    if (index < options.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
