package ly.img.editor.base.timeline.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import ly.img.editor.base.sheet.LibraryAddToBackgroundTrackSheetType
import ly.img.editor.base.timeline.state.AddClipOption
import ly.img.editor.base.timeline.state.TimelineConfiguration
import ly.img.editor.base.ui.Event
import ly.img.editor.core.R
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.iconpack.AddCameraBackground
import ly.img.editor.core.iconpack.AddGalleryBackground
import ly.img.editor.core.iconpack.IconPack
import ly.img.editor.core.iconpack.LibraryElements
import ly.img.editor.core.theme.surface3
import ly.img.editor.core.ui.library.LibraryViewModel
import ly.img.editor.core.ui.library.components.ClipMenuItem

@Composable
fun AddClipButton(
    modifier: Modifier,
    onEvent: (EditorEvent) -> Unit,
    options: List<AddClipOption> = TimelineConfiguration.addClipOptions,
) {
    if (options.isEmpty()) return

    var showClipMenu by remember { mutableStateOf(false) }
    val libraryViewModel = viewModel<LibraryViewModel>()
    var callback by remember { mutableStateOf<(@Composable () -> Unit)?>(null) }
    callback?.invoke()
    callback = null

    fun handleClickOf(option: AddClipOption) = when (option) {
        AddClipOption.Camera -> {
            onEvent(Event.OnVideoCameraClick { callback = it })
        }
        AddClipOption.Gallery -> {
            showClipMenu = false
            onEvent(
                EditorEvent.Sheet.Open(
                    LibraryAddToBackgroundTrackSheetType(
                        libraryCategory = libraryViewModel.assetLibrary.images(libraryViewModel.sceneMode),
                    ),
                ),
            )
        }
        AddClipOption.Library -> {
            onEvent(
                EditorEvent.Sheet.Open(
                    LibraryAddToBackgroundTrackSheetType(
                        libraryCategory = libraryViewModel.assetLibrary.clips(libraryViewModel.sceneMode),
                    ),
                ),
            )
        }
    }

    Box(
        // zIndex of -1 ensures that the trim handles are drawn on top
        modifier = modifier.zIndex(-1f),
    ) {
        TimelineButton(
            id = R.string.ly_img_editor_timeline_button_add_clip,
            containerColor = MaterialTheme.colorScheme.surface3,
        ) {
            if (options.size == 1) {
                handleClickOf(options.first())
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
                    when (option) {
                        AddClipOption.Camera -> {
                            ClipMenuItem(
                                textResourceId = R.string.ly_img_editor_timeline_add_clip_option_camera,
                                icon = IconPack.AddCameraBackground,
                            ) {
                                showClipMenu = false
                                handleClickOf(option)
                            }
                        }
                        AddClipOption.Gallery -> {
                            ClipMenuItem(
                                textResourceId = R.string.ly_img_editor_timeline_add_clip_option_gallery,
                                icon = IconPack.AddGalleryBackground,
                            ) {
                                showClipMenu = false
                                handleClickOf(option)
                            }
                        }
                        AddClipOption.Library -> {
                            ClipMenuItem(
                                textResourceId = R.string.ly_img_editor_timeline_add_clip_option_library,
                                icon = IconPack.LibraryElements,
                            ) {
                                showClipMenu = false
                                handleClickOf(option)
                            }
                        }
                    }
                    if (index < options.lastIndex) {
                        Spacer(modifier = Modifier.height(8.dp))
                        if (option == AddClipOption.Gallery && options[index + 1] == AddClipOption.Library) {
                            Divider()
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                    }
                }
            }
        }
    }
}
