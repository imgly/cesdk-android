@file:Suppress("UnusedReceiverParameter")

package ly.img.editor.core.component

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import ly.img.camera.core.CaptureMedia
import ly.img.editor.core.R
import ly.img.editor.core.component.data.unsafeLazy
import ly.img.editor.core.configuration.remember
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.iconpack.AddAudio
import ly.img.editor.core.iconpack.AddCameraBackground
import ly.img.editor.core.iconpack.AddGalleryBackground
import ly.img.editor.core.iconpack.ExpandMore
import ly.img.editor.core.iconpack.IconPack
import ly.img.editor.core.iconpack.LibraryElements
import ly.img.editor.core.iconpack.Pause
import ly.img.editor.core.iconpack.Play
import ly.img.editor.core.iconpack.Repeat
import ly.img.editor.core.iconpack.RepeatOff
import ly.img.editor.core.iconpack.VoiceoverAdd
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.featureFlag.flags.IMGLYCameraFeature
import java.util.Locale
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import ly.img.editor.core.iconpack.Timeline as TimelineIcon

private fun Duration.formatForPlayer(): String = toComponents { minutes, seconds, _ ->
    String.format(locale = Locale.getDefault(), "%d:%02d", minutes, seconds)
}

/**
 * The id of the timeline button returned by [Timeline.Button.rememberAddClip].
 */
val Timeline.Button.Id.addClip by unsafeLazy {
    EditorComponentId("ly.img.component.timeline.button.addClip")
}

/**
 * A composable helper function that creates and remembers the built-in "Add Clip" button of the [Timeline].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the timeline.
 */
@Composable
fun Timeline.Button.rememberAddClip(builder: Timeline.AddClipButtonBuilder.() -> Unit = {}): Timeline.AddClipButton =
    androidx.compose.runtime.remember {
        Timeline.AddClipButtonBuilder().apply(builder)
    }.build()

/**
 * The id of the timeline button returned by [Timeline.Button.rememberAddAudio].
 */
val Timeline.Button.Id.addAudio by unsafeLazy {
    EditorComponentId("ly.img.component.timeline.button.addAudio")
}

/**
 * A composable helper function that creates and remembers the built-in "Add Audio" button of the [Timeline].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the timeline.
 */
@Composable
fun Timeline.Button.rememberAddAudio(builder: Timeline.AddAudioButtonBuilder.() -> Unit = {}): Timeline.AddAudioButton =
    androidx.compose.runtime.remember {
        Timeline.AddAudioButtonBuilder().apply(builder)
    }.build()

/**
 * The id of the timeline button returned by [Timeline.Button.rememberPlayPause].
 */
val Timeline.Button.Id.playPause by unsafeLazy {
    EditorComponentId("ly.img.component.timeline.button.playPause")
}

/**
 * A composable helper function that creates and remembers a [Timeline] header button that starts and
 * pauses playback.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the timeline header.
 */
@Composable
fun Timeline.Button.rememberPlayPause(builder: Timeline.ButtonBuilder.() -> Unit = {}): Button<Timeline.ItemScope> =
    Button.remember(Timeline::ButtonBuilder) {
        id = { Timeline.Button.Id.playPause }
        scope = {
            val activeSceneTrigger by EditorTrigger.remember {
                editorContext.engine.scene.onActiveChanged()
            }
            val playbackTrigger by EditorTrigger.remember(activeSceneTrigger) playback@{
                val page = editorContext.engine.scene.getCurrentPage() ?: return@playback emptyFlow<Boolean>()
                editorContext.engine.event
                    .subscribe(listOf(page))
                    .filter { editorContext.engine.block.isValid(page) }
                    .map { editorContext.engine.block.isPlaying(page) }
                    .distinctUntilChanged()
            }
            remember(this, playbackTrigger) { Timeline.ItemScope(parentScope = this) }
        }
        contentDescription = {
            val isPlaying = remember(this) {
                editorContext.engine.run { scene.getCurrentPage()?.let { block.isPlaying(it) } == true }
            }
            stringResource(
                if (isPlaying) {
                    R.string.ly_img_editor_timeline_button_pause
                } else {
                    R.string.ly_img_editor_timeline_button_play
                },
            )
        }
        // Access through the declaring class: the Compose compiler emits an invalid setter call
        // when this composable lambda reads the inherited property through Timeline.ButtonBuilder.
        val buttonBuilder: AbstractButtonBuilder<Timeline.ItemScope> = this
        // vectorIcon has no size parameter; the play glyph is larger than the other header icons.
        icon = {
            Icon(
                modifier = Modifier.size(36.dp),
                imageVector = if (editorContext.engine.run { scene.getCurrentPage()?.let { block.isPlaying(it) } == true }) {
                    IconPack.Pause
                } else {
                    IconPack.Play
                },
                contentDescription = buttonBuilder.contentDescription?.invoke(this),
            )
        }
        onClick = {
            editorContext.engine.run {
                val page = scene.getCurrentPage() ?: return@run
                block.setPlaying(page, !block.isPlaying(page))
            }
        }
        builder()
    }

/**
 * The id of the timeline button returned by [Timeline.Button.rememberLoop].
 */
val Timeline.Button.Id.loop by unsafeLazy {
    EditorComponentId("ly.img.component.timeline.button.loop")
}

/**
 * A composable helper function that creates and remembers a [Timeline] header button that toggles
 * looping playback.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the timeline header.
 */
@Composable
fun Timeline.Button.rememberLoop(builder: Timeline.ButtonBuilder.() -> Unit = {}): Button<Timeline.ItemScope> =
    Button.remember(Timeline::ButtonBuilder) {
        id = { Timeline.Button.Id.loop }
        scope = {
            val activeSceneTrigger by EditorTrigger.remember {
                editorContext.engine.scene.onActiveChanged()
            }
            val loopingTrigger by EditorTrigger.remember(activeSceneTrigger) looping@{
                val page = editorContext.engine.scene.getCurrentPage() ?: return@looping emptyFlow<Boolean>()
                editorContext.engine.event
                    .subscribe(listOf(page))
                    .filter { editorContext.engine.block.isValid(page) }
                    .map { editorContext.engine.block.isLooping(page) }
                    .distinctUntilChanged()
            }
            remember(this, loopingTrigger) { Timeline.ItemScope(parentScope = this) }
        }
        vectorIcon = {
            if (editorContext.engine.run { scene.getCurrentPage()?.let { block.isLooping(it) } == true }) {
                IconPack.Repeat
            } else {
                IconPack.RepeatOff
            }
        }
        contentDescription = { stringResource(R.string.ly_img_editor_timeline_button_loop) }
        onClick = {
            editorContext.engine.run {
                val page = scene.getCurrentPage() ?: return@run
                block.setLooping(page, !block.isLooping(page))
            }
        }
        builder()
    }

/**
 * The id of the timeline button returned by [Timeline.Button.rememberToggleExpanded].
 */
val Timeline.Button.Id.toggleExpanded by unsafeLazy {
    EditorComponentId("ly.img.component.timeline.button.toggleExpanded")
}

/**
 * A composable helper function that creates and remembers a [Timeline] header button that expands and
 * collapses the timeline.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the timeline header.
 */
@Composable
fun Timeline.Button.rememberToggleExpanded(builder: Timeline.ItemBuilder.() -> Unit = {}): EditorComponent<Timeline.ItemScope> =
    EditorComponent.remember(Timeline::ItemBuilder) {
        id = { Timeline.Button.Id.toggleExpanded }
        decoration = {
            var expanded by editorContext.expandedState
            val onClick = { expanded = expanded.not() }
            if (expanded) {
                IconButton(onClick = onClick, modifier = Modifier.padding(horizontal = 4.dp)) {
                    Icon(
                        imageVector = IconPack.ExpandMore,
                        contentDescription = stringResource(R.string.ly_img_editor_timeline_button_hide_timeline),
                    )
                }
            } else {
                TextButton(onClick = onClick, modifier = Modifier.padding(horizontal = 4.dp)) {
                    Icon(
                        imageVector = IconPack.TimelineIcon,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Text(
                        text = stringResource(R.string.ly_img_editor_timeline_button_show_timeline),
                        modifier = Modifier.padding(start = 8.dp),
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
        builder()
    }

/**
 * The id of the timeline label returned by [Timeline.Label.rememberTimecode].
 */
val Timeline.Label.Id.timecode by unsafeLazy {
    EditorComponentId("ly.img.component.timeline.label.timecode")
}

/**
 * A composable helper function that creates and remembers the [Timeline] header timecode, displaying
 * the current playhead position and the total duration.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a component that will be displayed in the timeline header.
 */
@Composable
fun Timeline.Label.rememberTimecode(builder: Timeline.ItemBuilder.() -> Unit = {}): EditorComponent<Timeline.ItemScope> =
    EditorComponent.remember(Timeline::ItemBuilder) {
        id = { Timeline.Label.Id.timecode }
        scope = {
            val activeSceneTrigger by EditorTrigger.remember {
                editorContext.engine.scene.onActiveChanged()
            }
            val timecodeTrigger by EditorTrigger.remember(activeSceneTrigger) timecode@{
                val page = editorContext.engine.scene.getCurrentPage() ?: return@timecode emptyFlow<Pair<Long, Long>>()
                editorContext.engine.event
                    .subscribe(listOf(page))
                    .filter { editorContext.engine.block.isValid(page) }
                    .map {
                        editorContext.engine.block.run {
                            getPlaybackTime(page).seconds.inWholeSeconds to getDuration(page).seconds.inWholeSeconds
                        }
                    }
                    .distinctUntilChanged()
            }
            remember(this, timecodeTrigger) { Timeline.ItemScope(parentScope = this) }
        }
        decoration = {
            val (playheadPosition, totalDuration) = remember(this) {
                editorContext.engine.run {
                    val page = scene.getCurrentPage()
                    (page?.let { block.getPlaybackTime(it) } ?: 0.0).seconds.formatForPlayer() to
                        (page?.let { block.getDuration(it) } ?: 0.0).seconds.formatForPlayer()
                }
            }
            Row(modifier = Modifier.padding(horizontal = 20.dp)) {
                Text(
                    text = "$playheadPosition / ",
                    style = MaterialTheme.typography.labelMedium,
                )
                Text(
                    text = totalDuration,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.alpha(0.75f),
                )
            }
        }
        builder()
    }

/**
 * A composable function that creates and remembers a [Timeline.HeaderListBuilder] instance.
 *
 * An alignment group is centered as a whole, so a centered group holds one item at most, otherwise
 * that item would not sit in the middle of the header.
 *
 * @param builder the building block of [Timeline.HeaderListBuilder].
 * @return a new [Timeline.HeaderListBuilder] instance.
 */
@Composable
fun Timeline.HeaderListBuilder.remember(
    builder: HorizontalListBuilderScope<EditorComponent<*>>.() -> Unit,
): HorizontalListBuilder<EditorComponent<*>> = HorizontalListBuilder.remember(builder)

/**
 * The id of the option returned by [Timeline.AddClipOption.Companion.rememberCamera].
 */
val Timeline.AddClipOption.Id.camera by unsafeLazy {
    EditorComponentId("ly.img.component.timeline.addClip.camera")
}

/**
 * A composable helper function that creates and remembers the "Add Clip" entry that records a clip
 * with the camera and appends it to the background track.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 *
 * @param builder the builder lambda to override the default builder.
 * @return an entry for the "Add Clip" menu.
 */
@Composable
fun Timeline.AddClipOption.Companion.rememberCamera(builder: Timeline.AddClipOptionBuilder.() -> Unit = {}): Timeline.AddClipOption {
    val isImglyCameraAvailable = androidx.compose.runtime.remember {
        runCatching { CaptureMedia() }.isSuccess
    } &&
        IMGLYCameraFeature.enabled
    val onClickDelegate = if (isImglyCameraAvailable) {
        Dock.Button.rememberImglyCamera(acceptsVideoCapture = { true })
    } else {
        Dock.Button.rememberSystemCamera()
    }.onClick
    return Timeline.AddClipOption.remember(Timeline::AddClipOptionBuilder) {
        id = { Timeline.AddClipOption.Id.camera }
        vectorIcon = { IconPack.AddCameraBackground }
        textString = { stringResource(R.string.ly_img_editor_timeline_add_clip_option_camera) }
        onClick = {
            onClickDelegate(Dock.ItemScope(this))
        }
        builder()
    }
}

/**
 * The id of the option returned by [Timeline.AddClipOption.Companion.rememberGallery].
 */
val Timeline.AddClipOption.Id.gallery by unsafeLazy {
    EditorComponentId("ly.img.component.timeline.addClip.gallery")
}

/**
 * A composable helper function that creates and remembers the "Add Clip" entry that opens the system
 * gallery and appends the selection to the background track.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 *
 * @param builder the builder lambda to override the default builder.
 * @return an entry for the "Add Clip" menu.
 */
@Composable
fun Timeline.AddClipOption.Companion.rememberGallery(builder: Timeline.AddClipOptionBuilder.() -> Unit = {}): Timeline.AddClipOption =
    Timeline.AddClipOption.remember(Timeline::AddClipOptionBuilder) {
        id = { Timeline.AddClipOption.Id.gallery }
        vectorIcon = { IconPack.AddGalleryBackground }
        textString = { stringResource(R.string.ly_img_editor_timeline_add_clip_option_gallery) }
        onClick = {
            val assetLibrary = requireNotNull(editorContext.configuration.value?.assetLibrary)
            editorContext.eventHandler.send(
                EditorEvent.Sheet.Open(SheetType.LibraryAdd(libraryCategory = assetLibrary.gallery(), addToBackgroundTrack = true)),
            )
        }
        builder()
    }

/**
 * The id of the option returned by [Timeline.AddClipOption.Companion.rememberLibrary].
 */
val Timeline.AddClipOption.Id.library by unsafeLazy {
    EditorComponentId("ly.img.component.timeline.addClip.library")
}

/**
 * A composable helper function that creates and remembers the "Add Clip" entry that opens the asset
 * library and appends the selection to the background track.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 *
 * @param builder the builder lambda to override the default builder.
 * @return an entry for the "Add Clip" menu.
 */
@Composable
fun Timeline.AddClipOption.Companion.rememberLibrary(builder: Timeline.AddClipOptionBuilder.() -> Unit = {}): Timeline.AddClipOption =
    Timeline.AddClipOption.remember(Timeline::AddClipOptionBuilder) {
        id = { Timeline.AddClipOption.Id.library }
        vectorIcon = { IconPack.LibraryElements }
        textString = { stringResource(R.string.ly_img_editor_timeline_add_clip_option_library) }
        onClick = {
            val assetLibrary = requireNotNull(editorContext.configuration.value?.assetLibrary)
            editorContext.eventHandler.send(
                EditorEvent.Sheet.Open(SheetType.LibraryAdd(libraryCategory = assetLibrary.clips(), addToBackgroundTrack = true)),
            )
        }
        builder()
    }

/**
 * The id of the option returned by [Timeline.AddAudioOption.Companion.rememberMusic].
 */
val Timeline.AddAudioOption.Id.music by unsafeLazy {
    EditorComponentId("ly.img.component.timeline.addAudio.music")
}

/**
 * A composable helper function that creates and remembers the "Add Audio" entry that opens the asset
 * library's audio category.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 *
 * @param builder the builder lambda to override the default builder.
 * @return an entry for the "Add Audio" menu.
 */
@Composable
fun Timeline.AddAudioOption.Companion.rememberMusic(builder: Timeline.AddAudioOptionBuilder.() -> Unit = {}): Timeline.AddAudioOption =
    Timeline.AddAudioOption.remember(Timeline::AddAudioOptionBuilder) {
        id = { Timeline.AddAudioOption.Id.music }
        vectorIcon = { IconPack.AddAudio }
        textString = { stringResource(R.string.ly_img_editor_timeline_add_audio_option_music) }
        onClick = {
            val assetLibrary = requireNotNull(editorContext.configuration.value?.assetLibrary)
            editorContext.eventHandler.send(
                EditorEvent.Sheet.Open(SheetType.LibraryAdd(libraryCategory = assetLibrary.audios())),
            )
        }
        builder()
    }

/**
 * The id of the option returned by [Timeline.AddAudioOption.Companion.rememberVoiceover].
 */
val Timeline.AddAudioOption.Id.voiceover by unsafeLazy {
    EditorComponentId("ly.img.component.timeline.addAudio.voiceover")
}

/**
 * A composable helper function that creates and remembers the "Add Audio" entry that records a
 * voiceover.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 *
 * @param builder the builder lambda to override the default builder.
 * @return an entry for the "Add Audio" menu.
 */
@Composable
fun Timeline.AddAudioOption.Companion.rememberVoiceover(builder: Timeline.AddAudioOptionBuilder.() -> Unit = {}): Timeline.AddAudioOption =
    Timeline.AddAudioOption.remember(Timeline::AddAudioOptionBuilder) {
        id = { Timeline.AddAudioOption.Id.voiceover }
        vectorIcon = { IconPack.VoiceoverAdd }
        textString = { stringResource(R.string.ly_img_editor_timeline_add_audio_option_voiceover) }
        onClick = { editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Voiceover())) }
        builder()
    }
