package ly.img.editor.base.timeline.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.DropdownMenu
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ly.img.editor.base.timeline.state.AddAudioOption
import ly.img.editor.base.timeline.state.TimelineConfiguration
import ly.img.editor.core.LocalEditorScope
import ly.img.editor.core.R
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.iconpack.AddAudio
import ly.img.editor.core.iconpack.IconPack
import ly.img.editor.core.iconpack.Plus
import ly.img.editor.core.iconpack.Voiceoveradd
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.ui.library.components.ClipMenuItem

@Composable
fun AddAudioButton(
    modifier: Modifier,
    options: List<AddAudioOption> = TimelineConfiguration.addAudioOptions,
) {
    if (options.isEmpty()) return

    var showAudioMenu by remember { mutableStateOf(false) }
    val editorScope = LocalEditorScope.current
    val editorContext = with(editorScope) { editorContext }

    fun handleClickOf(option: AddAudioOption) = when (option) {
        AddAudioOption.Library -> {
            editorContext.eventHandler.send(
                EditorEvent.Sheet.Open(
                    SheetType.LibraryAdd(
                        libraryCategory = editorContext.assetLibrary.audios(
                            editorContext.engine.scene.getMode(),
                        ),
                    ),
                ),
            )
        }

        AddAudioOption.Voiceover -> {
            editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Voiceover()))
        }
    }

    Box(
        // zIndex of -1 ensures that the trim handles are drawn on top
        modifier = modifier.zIndex(-1f),
    ) {
        TimelineButton(
            id = R.string.ly_img_editor_timeline_button_add_audio,
            icon = IconPack.Plus,
        ) {
            if (options.size == 1) {
                handleClickOf(options.first())
            } else {
                showAudioMenu = true
            }
        }

        if (options.size > 1) {
            DropdownMenu(
                expanded = showAudioMenu,
                onDismissRequest = { showAudioMenu = false },
            ) {
                options.forEachIndexed { index, option ->
                    when (option) {
                        AddAudioOption.Library -> {
                            ClipMenuItem(
                                textResourceId = R.string.ly_img_editor_dock_button_audio,
                                icon = IconPack.AddAudio,
                            ) {
                                showAudioMenu = false
                                handleClickOf(option)
                            }
                        }

                        AddAudioOption.Voiceover -> {
                            ClipMenuItem(
                                textResourceId = R.string.ly_img_editor_timeline_add_audio_option_voiceover,
                                icon = IconPack.Voiceoveradd,
                            ) {
                                showAudioMenu = false
                                handleClickOf(option)
                            }
                        }
                    }
                    if (index < options.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                }
            }
        }
    }
}
