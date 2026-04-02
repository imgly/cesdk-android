@file:Suppress("UnusedReceiverParameter")

package ly.img.editor.core.component

import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ly.img.camera.core.CameraResult
import ly.img.camera.core.CaptureVideo
import ly.img.camera.core.EngineConfiguration
import ly.img.editor.core.EditorScope
import ly.img.editor.core.R
import ly.img.editor.core.component.data.Height
import ly.img.editor.core.component.data.unsafeLazy
import ly.img.editor.core.configuration.remember
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.iconpack.AddAudio
import ly.img.editor.core.iconpack.AddCameraBackground
import ly.img.editor.core.iconpack.AddCameraForeground
import ly.img.editor.core.iconpack.AddGalleryForeground
import ly.img.editor.core.iconpack.AddImageForeground
import ly.img.editor.core.iconpack.AddOverlay
import ly.img.editor.core.iconpack.AddShape
import ly.img.editor.core.iconpack.AddSticker
import ly.img.editor.core.iconpack.AddText
import ly.img.editor.core.iconpack.Adjustments
import ly.img.editor.core.iconpack.Blur
import ly.img.editor.core.iconpack.CropRotate
import ly.img.editor.core.iconpack.Effect
import ly.img.editor.core.iconpack.Elements
import ly.img.editor.core.iconpack.Filter
import ly.img.editor.core.iconpack.IconPack
import ly.img.editor.core.iconpack.Plus
import ly.img.editor.core.iconpack.ReorderHorizontally
import ly.img.editor.core.iconpack.Resize
import ly.img.editor.core.iconpack.VoiceoverAdd
import ly.img.editor.core.library.data.AssetSourceType
import ly.img.editor.core.sheet.SheetStyle
import ly.img.editor.core.sheet.SheetType
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import java.io.File

/**
 * An extension function for getting the design block that has type [DesignBlockType.Track] and
 * [ly.img.engine.BlockApi.isPageDurationSource] is true.
 *
 * @return the background track design block if found.
 */
private fun Engine.getBackgroundTrack(): DesignBlock? {
    scene.get() ?: return null
    val page = scene.getCurrentPage() ?: return null
    val children = block.getChildren(page)
    return children.firstOrNull { child ->
        block.getType(child) == DesignBlockType.Track.key && block.isPageDurationSource(child)
    }
}

/**
 * The id of the dock button returned by [Dock.Button.rememberAssetLibrary].
 */
val Dock.Button.Id.assetLibrary by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.assetLibrary")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens a library sheet with tabs via [EditorEvent.Sheet.Open].
 * Every item in [ly.img.editor.core.library.AssetLibrary.tabs] is represented via a tab in the sheet.
 *
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Dock.Button.rememberAssetLibrary(
    builder: EditorComponentBuilder<EditorComponent<EditorScope>, EditorScope>.() -> Unit = {},
): EditorComponent<EditorScope> = EditorComponent.remember {
    id = { Dock.Button.Id.assetLibrary }
    scope = {
        remember(this) { Dock.ItemScope(parentScope = this) }
    }
    decoration = {
        Box(modifier = Modifier.padding(horizontal = 16.dp)) {
            FloatingActionButton(
                modifier = Modifier.testTag(tag = "LibraryTabsButton"),
                onClick = {
                    editorContext.eventHandler.send(EditorEvent.Sheet.Open(type = SheetType.LibraryAdd()))
                },
                elevation = FloatingActionButtonDefaults.elevation(defaultElevation = 4.dp),
            ) {
                Icon(
                    imageVector = IconPack.Plus,
                    contentDescription = stringResource(R.string.ly_img_editor_dock_button_library),
                )
            }
        }
    }
    builder()
}

/**
 * The id of the dock button returned by [Dock.Button.rememberElementsLibrary].
 */
val Dock.Button.Id.elementsLibrary by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.elementsLibrary")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens a library sheet with elements via [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Dock.Button.rememberElementsLibrary(builder: Dock.ButtonBuilder.() -> Unit = {}): Button<Dock.ItemScope> = Dock.Button.remember {
    id = { Dock.Button.Id.elementsLibrary }
    onClick = {
        val assetLibrary = requireNotNull(editorContext.configuration.value?.assetLibrary)
        editorContext.eventHandler.send(
            EditorEvent.Sheet.Open(
                type = SheetType.LibraryAdd(
                    libraryCategory = assetLibrary.elements(),
                ),
            ),
        )
    }
    vectorIcon = { IconPack.Elements }
    textString = { stringResource(R.string.ly_img_editor_dock_button_elements) }
    builder()
}

/**
 * The id of the dock button returned by [Dock.Button.rememberOverlaysLibrary].
 */
val Dock.Button.Id.overlaysLibrary by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.overlaysLibrary")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens a library sheet with overlays via [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Dock.Button.rememberOverlaysLibrary(builder: Dock.ButtonBuilder.() -> Unit = {}): Button<Dock.ItemScope> = Dock.Button.remember {
    id = { Dock.Button.Id.overlaysLibrary }
    vectorIcon = { IconPack.AddOverlay }
    textString = { stringResource(R.string.ly_img_editor_dock_button_overlays) }
    onClick = {
        val assetLibrary = requireNotNull(editorContext.configuration.value?.assetLibrary)
        editorContext.eventHandler.send(
            EditorEvent.Sheet.Open(
                type = SheetType.LibraryAdd(
                    libraryCategory = assetLibrary.overlays(),
                ),
            ),
        )
    }
    builder()
}

/**
 * The id of the dock button returned by [Dock.Button.rememberImagesLibrary].
 */
val Dock.Button.Id.imagesLibrary by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.imagesLibrary")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens a library sheet with images via [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Dock.Button.rememberImagesLibrary(builder: Dock.ButtonBuilder.() -> Unit = {}): Button<Dock.ItemScope> = Dock.Button.remember {
    id = { Dock.Button.Id.imagesLibrary }
    vectorIcon = { IconPack.AddImageForeground }
    textString = { stringResource(R.string.ly_img_editor_dock_button_images) }
    onClick = {
        val assetLibrary = requireNotNull(editorContext.configuration.value?.assetLibrary)
        editorContext.eventHandler.send(
            EditorEvent.Sheet.Open(
                type = SheetType.LibraryAdd(
                    libraryCategory = assetLibrary.images(),
                ),
            ),
        )
    }
    builder()
}

/**
 * The id of the dock button returned by [Dock.Button.rememberTextLibrary].
 */
val Dock.Button.Id.textLibrary by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.textLibrary")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens a library sheet with text via [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Dock.Button.rememberTextLibrary(builder: Dock.ButtonBuilder.() -> Unit = {}): Button<Dock.ItemScope> = Dock.Button.remember {
    id = { Dock.Button.Id.textLibrary }
    vectorIcon = { IconPack.AddText }
    textString = { stringResource(R.string.ly_img_editor_dock_button_text) }
    onClick = {
        val assetLibrary = requireNotNull(editorContext.configuration.value?.assetLibrary)
        editorContext.eventHandler.send(
            EditorEvent.Sheet.Open(
                type = SheetType.LibraryAdd(
                    style = SheetStyle(
                        isFloating = true,
                        maxHeight = Height.Fraction(1F),
                        isHalfExpandingEnabled = true,
                        isHalfExpandedInitially = true,
                    ),
                    libraryCategory = assetLibrary.text(),
                ),
            ),
        )
    }
    builder()
}

/**
 * The id of the dock button returned by [Dock.Button.rememberShapesLibrary].
 */
val Dock.Button.Id.shapesLibrary by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.shapesLibrary")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens a library sheet with shapes via [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Dock.Button.rememberShapesLibrary(builder: Dock.ButtonBuilder.() -> Unit = {}): Button<Dock.ItemScope> = Dock.Button.remember {
    id = { Dock.Button.Id.shapesLibrary }
    vectorIcon = { IconPack.AddShape }
    textString = { stringResource(R.string.ly_img_editor_dock_button_shapes) }
    onClick = {
        val assetLibrary = requireNotNull(editorContext.configuration.value?.assetLibrary)
        editorContext.eventHandler.send(
            EditorEvent.Sheet.Open(
                type = SheetType.LibraryAdd(
                    libraryCategory = assetLibrary.shapes(),
                ),
            ),
        )
    }
    builder()
}

/**
 * The id of the dock button returned by [Dock.Button.rememberStickersLibrary].
 */
val Dock.Button.Id.stickersLibrary by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.stickersLibrary")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens a library sheet with stickers via [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Dock.Button.rememberStickersLibrary(builder: Dock.ButtonBuilder.() -> Unit = {}): Button<Dock.ItemScope> = Dock.Button.remember {
    id = { Dock.Button.Id.stickersLibrary }
    vectorIcon = { IconPack.AddSticker }
    textString = { stringResource(R.string.ly_img_editor_dock_button_stickers) }
    onClick = {
        val assetLibrary = requireNotNull(editorContext.configuration.value?.assetLibrary)
        editorContext.eventHandler.send(
            EditorEvent.Sheet.Open(
                type = SheetType.LibraryAdd(
                    libraryCategory = assetLibrary.stickers(),
                ),
            ),
        )
    }
    builder()
}

/**
 * The id of the dock button returned by [Dock.Button.rememberStickersAndShapesLibrary].
 */
val Dock.Button.Id.stickersAndShapesLibrary by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.stickersAndShapesLibrary")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens a library sheet with stickers and shapes via [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Dock.Button.rememberStickersAndShapesLibrary(builder: Dock.ButtonBuilder.() -> Unit = {}): Button<Dock.ItemScope> =
    Dock.Button.remember {
        id = { Dock.Button.Id.stickersAndShapesLibrary }
        vectorIcon = { IconPack.AddSticker }
        textString = { stringResource(R.string.ly_img_editor_dock_button_stickers) }
        onClick = {
            val assetLibrary = requireNotNull(editorContext.configuration.value?.assetLibrary)
            editorContext.eventHandler.send(
                EditorEvent.Sheet.Open(
                    type = SheetType.LibraryAdd(
                        libraryCategory = assetLibrary.stickersAndShapes(),
                    ),
                ),
            )
        }
        builder()
    }

/**
 * The id of the dock button returned by [Dock.Button.rememberAudiosLibrary].
 */
val Dock.Button.Id.audiosLibrary by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.audiosLibrary")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens a library sheet with audios via [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Dock.Button.rememberAudiosLibrary(builder: Dock.ButtonBuilder.() -> Unit = {}): Button<Dock.ItemScope> = Dock.Button.remember {
    id = { Dock.Button.Id.audiosLibrary }
    vectorIcon = { IconPack.AddAudio }
    textString = { stringResource(R.string.ly_img_editor_dock_button_audio) }
    onClick = {
        val assetLibrary = requireNotNull(editorContext.configuration.value?.assetLibrary)
        editorContext.eventHandler.send(
            EditorEvent.Sheet.Open(
                type = SheetType.LibraryAdd(
                    libraryCategory = assetLibrary.audios(),
                ),
            ),
        )
    }
    builder()
}

/**
 * The id of the dock button returned by [Dock.Button.rememberVoiceoverRecord].
 */
val Dock.Button.Id.voiceoverRecord by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.voiceoverRecord")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens the voiceover recording sheet via [EditorEvent.Sheet.Open].
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Dock.Button.rememberVoiceoverRecord(builder: Dock.ButtonBuilder.() -> Unit = {}): Button<Dock.ItemScope> = Dock.Button.remember {
    id = { Dock.Button.Id.voiceoverRecord }
    vectorIcon = { IconPack.VoiceoverAdd }
    textString = { stringResource(R.string.ly_img_editor_dock_button_voiceover) }
    onClick = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Voiceover()))
    }
    builder()
}

/**
 * The id of the dock button returned by [Dock.Button.rememberSystemGallery].
 */
val Dock.Button.Id.systemGallery by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.systemGallery")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens a library sheet with system gallery content via [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Dock.Button.rememberSystemGallery(builder: Dock.ButtonBuilder.() -> Unit = {}): Button<Dock.ItemScope> = Dock.Button.remember {
    id = { Dock.Button.Id.systemGallery }
    vectorIcon = { IconPack.AddGalleryForeground }
    textString = { stringResource(R.string.ly_img_editor_dock_button_gallery) }
    onClick = {
        val assetLibrary = requireNotNull(editorContext.configuration.value?.assetLibrary)
        val category = assetLibrary.gallery()
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.LibraryAdd(libraryCategory = category)))
    }
    builder()
}

/**
 * The id of the dock button returned by [Dock.Button.rememberSystemCamera].
 */
val Dock.Button.Id.systemCamera by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.systemCamera")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens the system camera via [EditorEvent.LaunchContract].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Dock.Button.rememberSystemCamera(
    captureVideo: Dock.ItemScope.() -> Boolean = { false },
    builder: Dock.ButtonBuilder.() -> Unit = {},
): Button<Dock.ItemScope> = Dock.Button.remember {
    id = { Dock.Button.Id.systemCamera }
    vectorIcon = { IconPack.AddCameraForeground }
    textString = { stringResource(R.string.ly_img_editor_dock_button_camera) }
    onClick = {
        editorContext.coroutineScope.launch {
            val isCaptureVideo = captureVideo()
            val launchContract = if (isCaptureVideo) {
                ActivityResultContracts.CaptureVideo()
            } else {
                ActivityResultContracts.TakePicture()
            }
            val uri = withContext(Dispatchers.Default) {
                val context = editorContext.activity
                File.createTempFile("imgly_", null, context.filesDir).let {
                    FileProvider.getUriForFile(context, "${context.packageName}.ly.img.editor.fileprovider", it)
                }
            }
            val event = EditorEvent.LaunchContract(launchContract, uri) { success ->
                if (success) {
                    val uploadSource = if (isCaptureVideo) {
                        AssetSourceType.VideoUploads
                    } else {
                        AssetSourceType.ImageUploads
                    }
                    editorContext.eventHandler.send(EditorEvent.AddUriToScene(uploadSource, uri))

                    // Persist selection and rescan so it appears in gallery for follow-up edits
                    val context = editorContext.activity
                    runCatching { ly.img.editor.core.library.data.SystemGalleryPermission.addSelected(uri, context) }
                    runCatching { android.media.MediaScannerConnection.scanFile(context, arrayOf(uri.toString()), null, null) }

                    // Open the uploads category so users can re-use the capture
                    val assetLibrary = requireNotNull(editorContext.configuration.value?.assetLibrary)
                    val category = if (isCaptureVideo) {
                        assetLibrary.videos()
                    } else {
                        assetLibrary.images()
                    }
                    editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.LibraryAdd(libraryCategory = category)))
                }
            }
            editorContext.eventHandler.send(event)
        }
    }
    builder()
}

/**
 * The id of the dock button returned by [Dock.Button.rememberImglyCamera].
 */
val Dock.Button.Id.imglyCamera by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.imglyCamera")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens the imgly camera via [EditorEvent.LaunchContract].
 *
 * IMPORTANT: Make sure your app has the dependency of ly.img:camera:<version> next
 * to the ly.img:editor:<version> dependency.
 * Also make sure that their versions match. Failing to provide the dependency will result to a crash.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Dock.Button.rememberImglyCamera(builder: Dock.ButtonBuilder.() -> Unit = {}): Button<Dock.ItemScope> = Dock.Button.remember {
    id = { Dock.Button.Id.imglyCamera }
    vectorIcon = { IconPack.AddCameraBackground }
    textString = {
        stringResource(R.string.ly_img_editor_dock_button_camera)
    }
    onClick = {
        EditorEvent.LaunchContract(
            contract = CaptureVideo(),
            input = CaptureVideo.Input(
                engineConfiguration = EngineConfiguration(
                    license = editorContext.engine.editor.getActiveLicense(),
                    userId = editorContext.userId,
                ),
            ),
            onOutput = {
                (it as? CameraResult.Record)
                    ?.recordings
                    ?.map { recording ->
                        Pair(recording.videos.first().uri, recording.duration)
                    }?.let { recordings ->
                        // For camera recordings, still upload into timeline via existing flow to keep behavior; optional: route via gallery if needed
                        editorContext.eventHandler.send(
                            EditorEvent.AddCameraRecordingsToScene(
                                uploadAssetSourceType = AssetSourceType.VideoUploads,
                                recordings = recordings,
                            ),
                        )
                    }
            },
        ).let { editorContext.eventHandler.send(it) }
    }
    builder()
}

/**
 * The id of the dock button returned by [Dock.Button.rememberReorder].
 */
val Dock.Button.Id.reorder by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.reorder")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens reorder sheet via [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the dock.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun Dock.Button.rememberReorder(builder: Dock.ButtonBuilder.() -> Unit = {}): Button<Dock.ItemScope> = Dock.Button.remember {
    id = { Dock.Button.Id.reorder }
    scope = {
        val engine = editorContext.engine
        val reorderButtonVisible by remember(this) {
            engine.event.subscribe(engine.scene.getPages())
                .map { editorContext.engine.getBackgroundTrack() }
                .onStart { emit(editorContext.engine.getBackgroundTrack()) }
                .distinctUntilChanged()
                .flatMapLatest { backgroundTrack ->
                    if (backgroundTrack == null) {
                        flowOf(false)
                    } else {
                        engine.event.subscribe(listOf(backgroundTrack))
                            .filter { engine.block.isValid(backgroundTrack) }
                            .map { engine.block.getChildren(backgroundTrack).size >= 2 }
                            .onStart { emit(engine.block.getChildren(backgroundTrack).size >= 2) }
                            .distinctUntilChanged()
                    }
                }
        }.collectAsState(
            initial = androidx.compose.runtime.remember {
                val result = editorContext.engine.getBackgroundTrack()?.let {
                    engine.block.getChildren(it).size >= 2
                } ?: false
                result
            },
        )

        // Update button whenever the number of videos becomes gt or lse than 2.
        remember(this, reorderButtonVisible) {
            Dock.ItemScope(parentScope = this)
        }
    }
    visible = {
        remember(this) {
            editorContext.engine.getBackgroundTrack()?.let {
                editorContext.engine.block.getChildren(it).size >= 2
            } ?: false
        }
    }
    vectorIcon = { IconPack.ReorderHorizontally }
    textString = { stringResource(R.string.ly_img_editor_dock_button_reorder) }
    onClick = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Reorder()))
    }
    builder()
}

/**
 * The id of the dock button returned by [Dock.Button.rememberAdjustments].
 */
val Dock.Button.Id.adjustments by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.adjustments")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens adjustments sheet for the current page via [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Dock.Button.rememberAdjustments(builder: Dock.ButtonBuilder.() -> Unit = {}): Button<Dock.ItemScope> = Dock.Button.remember {
    id = { Dock.Button.Id.adjustments }
    visible = {
        remember(this) {
            val designBlock = editorContext.engine.scene.getCurrentPage() ?: editorContext.engine.scene.getPages()[0]
            editorContext.engine.block.isAllowedByScope(designBlock, "appearance/adjustments")
        }
    }
    vectorIcon = { IconPack.Adjustments }
    textString = { stringResource(R.string.ly_img_editor_dock_button_adjustments) }
    onClick = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Adjustments()))
    }
    builder()
}

/**
 * The id of the dock button returned by [Dock.Button.rememberFilter].
 */
val Dock.Button.Id.filter by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.filter")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens filter sheet for the current page via [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Dock.Button.rememberFilter(builder: Dock.ButtonBuilder.() -> Unit = {}): Button<Dock.ItemScope> = Dock.Button.remember {
    id = { Dock.Button.Id.filter }
    visible = {
        remember(this) {
            val designBlock = editorContext.engine.scene.getCurrentPage() ?: editorContext.engine.scene.getPages()[0]
            editorContext.engine.block.isAllowedByScope(designBlock, "appearance/filter")
        }
    }
    vectorIcon = { IconPack.Filter }
    textString = { stringResource(R.string.ly_img_editor_dock_button_filter) }
    onClick = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Filter()))
    }
    builder()
}

/**
 * The id of the dock button returned by [Dock.Button.rememberEffect].
 */
val Dock.Button.Id.effect by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.effect")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens effect sheet for the current page via [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Dock.Button.rememberEffect(builder: Dock.ButtonBuilder.() -> Unit = {}): Button<Dock.ItemScope> = Dock.Button.remember {
    id = { Dock.Button.Id.effect }
    visible = {
        remember(this) {
            val designBlock = editorContext.engine.scene.getCurrentPage() ?: editorContext.engine.scene.getPages()[0]
            editorContext.engine.block.isAllowedByScope(designBlock, "appearance/effect")
        }
    }
    vectorIcon = { IconPack.Effect }
    textString = { stringResource(R.string.ly_img_editor_dock_button_effect) }
    onClick = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Effect()))
    }
    builder()
}

/**
 * The id of the dock button returned by [Dock.Button.rememberBlur].
 */
val Dock.Button.Id.blur by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.blur")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens blur sheet for the current page via [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Dock.Button.rememberBlur(builder: Dock.ButtonBuilder.() -> Unit = {}): Button<Dock.ItemScope> = Dock.Button.remember {
    id = { Dock.Button.Id.blur }
    visible = {
        remember(this) {
            val designBlock = editorContext.engine.scene.getCurrentPage() ?: editorContext.engine.scene.getPages()[0]
            editorContext.engine.block.isAllowedByScope(designBlock, "appearance/blur")
        }
    }
    vectorIcon = { IconPack.Blur }
    textString = { stringResource(R.string.ly_img_editor_dock_button_blur) }
    onClick = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Blur()))
    }
    builder()
}

/**
 * The id of the dock button returned by [Dock.Button.rememberCrop].
 */
val Dock.Button.Id.crop by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.crop")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens crop sheet for the current page via [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param mode the crop mode specifying which crop operation is opened.
 * Default value is [SheetType.Crop.Mode.ImageCrop].
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Dock.Button.rememberCrop(
    mode: SheetType.Crop.Mode = SheetType.Crop.Mode.ImageCrop,
    builder: Dock.ButtonBuilder.() -> Unit = {},
): Button<Dock.ItemScope> = Dock.Button.remember {
    id = { Dock.Button.Id.crop }
    visible = {
        remember(this) {
            val designBlock = editorContext.engine.scene.getCurrentPage() ?: editorContext.engine.scene.getPages()[0]
            editorContext.engine.block.supportsCrop(designBlock) &&
                editorContext.engine.block.isAllowedByScope(designBlock, "layer/crop")
        }
    }
    vectorIcon = { IconPack.CropRotate }
    textString = { stringResource(R.string.ly_img_editor_dock_button_crop) }
    onClick = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Crop(mode = mode)))
    }
    builder()
}

/**
 * The id of the dock button returned by [Dock.Button.rememberResizeAll].
 */
val Dock.Button.Id.resizeAll by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.resizeAll")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens resize sheet for the current page via [EditorEvent.Sheet.Open].
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder lambda to override the default builder.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Dock.Button.rememberResizeAll(builder: Dock.ButtonBuilder.() -> Unit = {}): Button<Dock.ItemScope> = Dock.Button.remember {
    id = { Dock.Button.Id.resizeAll }
    vectorIcon = { IconPack.Resize }
    textString = { stringResource(R.string.ly_img_editor_dock_button_resize) }
    onClick = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.ResizeAll()))
    }
    builder()
}
