package ly.img.editor.core.component

import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.core.content.FileProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import ly.img.camera.core.CameraResult
import ly.img.camera.core.CaptureVideo
import ly.img.camera.core.EngineConfiguration
import ly.img.editor.core.EditorScope
import ly.img.editor.core.LocalEditorScope
import ly.img.editor.core.R
import ly.img.editor.core.UnstableEditorApi
import ly.img.editor.core.component.Dock.Button
import ly.img.editor.core.component.Dock.ButtonScope
import ly.img.editor.core.component.Dock.Companion.DefaultDecoration
import ly.img.editor.core.component.Dock.Item
import ly.img.editor.core.component.Dock.Scope
import ly.img.editor.core.component.EditorComponent.Companion.alwaysVisible
import ly.img.editor.core.component.EditorComponent.Companion.noneEnterTransition
import ly.img.editor.core.component.EditorComponent.Companion.noneExitTransition
import ly.img.editor.core.component.EditorComponent.ListBuilder
import ly.img.editor.core.component.data.Height
import ly.img.editor.core.component.data.Nothing
import ly.img.editor.core.component.data.nothing
import ly.img.editor.core.component.data.unsafeLazy
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.iconpack.AddAudio
import ly.img.editor.core.iconpack.AddCameraBackground
import ly.img.editor.core.iconpack.AddCameraForeground
import ly.img.editor.core.iconpack.AddGalleryBackground
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
import ly.img.editor.core.iconpack.ReorderHorizontally
import ly.img.editor.core.library.data.AssetSourceType
import ly.img.editor.core.sheet.SheetStyle
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.state.EditorViewMode
import ly.img.editor.featureFlag.flags.IMGLYCameraFeature
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import ly.img.engine.SceneMode
import java.io.File

/**
 * A composable helper function that creates and remembers a [Dock] instance when launching [ly.img.editor.DesignEditor].
 * By default, the following items are registered in the dock:
 *
 * - Dock.Button.rememberElementsLibrary
 * - Dock.Button.rememberSystemGallery
 * - Dock.Button.rememberSystemCamera
 * - Dock.Button.rememberImagesLibrary
 * - Dock.Button.rememberTextLibrary
 * - Dock.Button.rememberShapesLibrary
 * - Dock.Button.rememberStickersLibrary
 *
 * For more information on how to customize [listBuilder], check [Dock.remember].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas
 * for granular recompositions over updating the scope, since scope change triggers full recomposition of the dock.
 * Also prefer updating individual [Item]s over updating the whole [Dock].
 * Ideally, scope should be updated when the parent scope (scope of the parent component) is updated and when you want to
 * observe changes from the [Engine].
 * By default it is updated only when the parent scope (accessed via [LocalEditorScope]) is updated.
 * @param visible whether the dock should be visible based on the [Engine]'s current state.
 * Default value is always true.
 * @param enterTransition transition of the dock when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the dock when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is [Dock.DefaultDecoration].
 * @param listBuilder a builder that registers the list of [Dock.Item]s that should be part of the dock.
 * Note that registering does not mean displaying. The items will be displayed if [Dock.Item.visible] is true for them.
 * Also note that items will be rebuilt when [scope] is updated.
 * By default, the list mentioned above is added to the dock.
 * @param horizontalArrangement the horizontal arrangement that should be used to render the items in the dock horizontally.
 * Note that the value will be ignored in case [listBuilder] contains aligned items. Check [EditorComponent.ListBuilder.Scope.New.aligned] for more
 * details on how to configure arrangement of aligned items.
 * Default value is [Arrangement.SpaceEvenly].
 * @param itemDecoration decoration of the items in the dock. Useful when you want to add custom background, foreground, shadow,
 * paddings etc to the items. Prefer using this decoration when you want to apply the same decoration to all the items, otherwise
 * set decoration to individual items.
 * Default value is always no decoration.
 * @return a dock that will be displayed when launching a [ly.img.editor.DesignEditor].
 */
@UnstableEditorApi
@Composable
fun Dock.Companion.rememberForDesign(
    scope: Scope = LocalEditorScope.current.run {
        remember(this) { Scope(parentScope = this) }
    },
    visible: @Composable Scope.() -> Boolean = alwaysVisible,
    enterTransition: @Composable Scope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable Scope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable Scope.(@Composable () -> Unit) -> Unit = { DefaultDecoration { it() } },
    listBuilder: HorizontalListBuilder<Item<*>> = Dock.ListBuilder.rememberForDesign(),
    horizontalArrangement: @Composable Scope.() -> Arrangement.Horizontal = { Arrangement.SpaceEvenly },
    itemDecoration: @Composable Scope.(content: @Composable () -> Unit) -> Unit = { it() },
    `_`: Nothing = nothing,
): Dock = remember(
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    listBuilder = listBuilder,
    horizontalArrangement = horizontalArrangement,
    itemDecoration = itemDecoration,
    `_` = `_`,
)

/**
 * A composable helper function that creates and remembers a [EditorComponent.ListBuilder],
 * designed to use with [Dock.Companion.rememberForDesign].
 *
 * It is convenient to use this helper function when you want to add additional items at the end of the list, or replace
 * the default items without touching the order in the dock when launching a [ly.img.editor.DesignEditor].
 * For more complex adjustments consider using [EditorComponent.ListBuilder.remember].
 */
@UnstableEditorApi
@Composable
fun Dock.ListBuilder.Companion.rememberForDesign(): HorizontalListBuilder<Item<*>> = HorizontalListBuilder.remember {
    add { Button.rememberElementsLibrary() }
    add { Button.rememberSystemGallery() }
    add { Button.rememberSystemCamera() }
    add { Button.rememberImagesLibrary() }
    add { Button.rememberTextLibrary() }
    add { Button.rememberShapesLibrary() }
    add { Button.rememberStickersLibrary() }
}

/**
 * A composable helper function that creates and remembers a [Dock] instance when launching [ly.img.editor.PhotoEditor].
 * By default, the following items are registered in the dock:
 *
 * - Dock.Button.rememberAdjustments
 * - Dock.Button.rememberFilter
 * - Dock.Button.rememberEffect
 * - Dock.Button.rememberBlur
 * - Dock.Button.rememberCrop
 * - Dock.Button.rememberTextLibrary
 * - Dock.Button.rememberShapesLibrary
 * - Dock.Button.rememberStickersLibrary
 *
 * For more information on how to customize [listBuilder], check [Dock.remember].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas
 * for granular recompositions over updating the scope, since scope change triggers full recomposition of the dock.
 * Also prefer updating individual [Dock.Item]s over updating the whole [Dock].
 * Ideally, scope should be updated when the parent scope (scope of the parent component) is updated and when you want to
 * observe changes from the [Engine].
 * By default it is updated only when the parent scope (accessed via [LocalEditorScope]) is updated.
 * @param visible whether the dock should be visible based on the [Engine]'s current state.
 * By default the value is true when the view mode of the editor is not [EditorViewMode.Preview].
 * @param enterTransition transition of the dock when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the dock when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is [Dock.DefaultDecoration].
 * @param listBuilder a builder that registers the list of [Dock.Item]s that should be part of the dock.
 * Note that registering does not mean displaying. The items will be displayed if [Dock.Item.visible] is true for them.
 * Also note that items will be rebuilt when [scope] is updated.
 * By default, the list mentioned above is added to the navigation bar.
 * @param horizontalArrangement the horizontal arrangement that should be used to render the items in the dock horizontally.
 * Note that the value will be ignored in case [listBuilder] contains aligned items. Check [EditorComponent.ListBuilder.Scope.New.aligned] for more
 * details on how to configure arrangement of aligned items.
 * Default value is [Arrangement.SpaceEvenly].
 * @param itemDecoration decoration of the items in the dock. Useful when you want to add custom background, foreground, shadow,
 * paddings etc to the items. Prefer using this decoration when you want to apply the same decoration to all the items, otherwise
 * set decoration to individual items.
 * Default value is always no decoration.
 * @return a dock that will be displayed when launching a [ly.img.editor.PhotoEditor].
 */
@UnstableEditorApi
@Composable
fun Dock.Companion.rememberForPhoto(
    scope: Scope = LocalEditorScope.current.run {
        remember(this) { Scope(parentScope = this) }
    },
    visible: @Composable Scope.() -> Boolean = {
        val state by editorContext.state.collectAsState()
        state.viewMode !is EditorViewMode.Preview
    },
    enterTransition: @Composable Scope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable Scope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable Scope.(@Composable () -> Unit) -> Unit = { DefaultDecoration { it() } },
    listBuilder: HorizontalListBuilder<Item<*>> = Dock.ListBuilder.rememberForPhoto(),
    horizontalArrangement: @Composable Scope.() -> Arrangement.Horizontal = { Arrangement.SpaceEvenly },
    itemDecoration: @Composable Scope.(content: @Composable () -> Unit) -> Unit = { it() },
    `_`: Nothing = nothing,
): Dock = remember(
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    listBuilder = listBuilder,
    horizontalArrangement = horizontalArrangement,
    itemDecoration = itemDecoration,
    `_` = `_`,
)

/**
 * A composable helper function that creates and remembers a [EditorComponent.ListBuilder],
 * designed to use with [Dock.Companion.rememberForPhoto].
 *
 * It is convenient to use this helper function when you want to add additional items at the end of the list, or replace
 * the default items without touching the order in the dock when launching a [ly.img.editor.PhotoEditor].
 * For more complex adjustments consider using [EditorComponent.ListBuilder.remember].
 */
@UnstableEditorApi
@Composable
fun Dock.ListBuilder.Companion.rememberForPhoto(): HorizontalListBuilder<Item<*>> = HorizontalListBuilder.remember {
    add { Button.rememberAdjustments() }
    add { Button.rememberFilter() }
    add { Button.rememberEffect() }
    add { Button.rememberBlur() }
    add { Button.rememberCrop() }
    add { Button.rememberTextLibrary() }
    add { Button.rememberShapesLibrary() }
    add { Button.rememberStickersLibrary() }
}

/**
 * A composable helper function that creates and remembers a [Dock] instance when launching [ly.img.editor.VideoEditor].
 * Make sure to add the gradle dependency of our camera library if you want to use the [rememberImglyCamera] button:
 * implementation "ly.img:camera:<same version as editor>".
 * If the dependency is missing, then [rememberSystemCamera] is used.
 * By default, the following items are registered in the dock:
 *
 * - Dock.Button.rememberSystemGallery
 * - Dock.Button.rememberImglyCamera or Dock.Button.rememberSystemCamera // depending on ly.img:camera dependency presence
 * - Dock.Button.rememberOverlaysLibrary
 * - Dock.Button.rememberTextLibrary
 * - Dock.Button.rememberStickersLibrary
 * - Dock.Button.rememberAudiosLibrary
 * - Dock.Button.rememberReorder
 *
 * For more information on how to customize [listBuilder], check [Dock.remember].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas
 * for granular recompositions over updating the scope, since scope change triggers full recomposition of the dock.
 * Also prefer updating individual [Item]s over updating the whole [Dock].
 * Ideally, scope should be updated when the parent scope (scope of the parent component) is updated and when you want to
 * observe changes from the [Engine].
 * By default it is updated only when the parent scope (accessed via [LocalEditorScope]) is updated and when the number of children
 * in the background track becomes more or less than two.
 * @param visible whether the dock should be visible based on the [Engine]'s current state.
 * Default value is always true.
 * @param enterTransition transition of the dock when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the dock when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is [Dock.DefaultDecoration].
 * @param listBuilder a builder that registers the list of [Dock.Item]s that should be part of the dock.
 * Note that registering does not mean displaying. The items will be displayed if [Dock.Item.visible] is true for them.
 * Also note that items will be rebuilt when [scope] is updated.
 * By default, the list mentioned above is added to the navigation bar.
 * @param horizontalArrangement the horizontal arrangement that should be used to render the items in the dock horizontally.
 * Note that the value will be ignored in case [listBuilder] contains aligned items. Check [EditorComponent.ListBuilder.Scope.New.aligned] for more
 * details on how to configure arrangement of aligned items.
 * Default value is [Arrangement.SpaceEvenly].
 * @param itemDecoration decoration of the items in the dock. Useful when you want to add custom background, foreground, shadow,
 * paddings etc to the items. Prefer using this decoration when you want to apply the same decoration to all the items, otherwise
 * set decoration to individual items.
 * Default value is always no decoration.
 * @return a dock that will be displayed when launching a [ly.img.editor.VideoEditor].
 */
@OptIn(ExperimentalCoroutinesApi::class)
@UnstableEditorApi
@Composable
fun Dock.Companion.rememberForVideo(
    scope: Scope = LocalEditorScope.current.run {
        val engine = editorContext.engine

        fun getBackgroundTrack(): DesignBlock? = engine.block.findByType(DesignBlockType.Track).firstOrNull {
            DesignBlockType.get(engine.block.getType(it)) == DesignBlockType.Track &&
                engine.block.isAlwaysOnBottom(it)
        }

        val reorderButtonVisible by remember(this) {
            engine.event.subscribe(engine.scene.getPages())
                .map { getBackgroundTrack() }
                .onStart { emit(getBackgroundTrack()) }
                .distinctUntilChanged()
                .flatMapLatest { backgroundTrack ->
                    if (backgroundTrack == null) {
                        flowOf(false)
                    } else {
                        engine.event.subscribe(listOf(backgroundTrack))
                            .map { engine.block.getChildren(backgroundTrack).size >= 2 }
                            .onStart { emit(engine.block.getChildren(backgroundTrack).size >= 2) }
                    }
                }
        }.collectAsState(
            initial = remember { getBackgroundTrack()?.let { engine.block.getChildren(it).size >= 2 } ?: false },
        )
        remember(this, reorderButtonVisible) { Scope(parentScope = this) }
    },
    visible: @Composable Scope.() -> Boolean = alwaysVisible,
    enterTransition: @Composable Scope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable Scope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable Scope.(@Composable () -> Unit) -> Unit = { DefaultDecoration { it() } },
    listBuilder: HorizontalListBuilder<Item<*>> = Dock.ListBuilder.rememberForVideo(),
    horizontalArrangement: @Composable Scope.() -> Arrangement.Horizontal = { Arrangement.SpaceEvenly },
    itemDecoration: @Composable Scope.(content: @Composable () -> Unit) -> Unit = { it() },
    `_`: Nothing = nothing,
): Dock = remember(
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    listBuilder = listBuilder,
    horizontalArrangement = horizontalArrangement,
    itemDecoration = itemDecoration,
    `_` = `_`,
)

/**
 * A composable helper function that creates and remembers a [EditorComponent.ListBuilder],
 * designed to use with [Dock.Companion.rememberForVideo].
 *
 * It is convenient to use this helper function when you want to add additional items at the end of the list, or replace
 * the default items without touching the order in the dock when launching a [ly.img.editor.VideoEditor].
 * For more complex adjustments consider using [EditorComponent.ListBuilder.remember].
 */
@UnstableEditorApi
@Composable
fun Dock.ListBuilder.Companion.rememberForVideo(): HorizontalListBuilder<Item<*>> = ListBuilder.remember {
    add { Button.rememberSystemGallery() }
    add {
        /*
        Make sure to add the gradle dependency of our camera library if you want to use the [rememberImglyCamera] button:
        implementation "ly.img:camera:<same version as editor>".
        If the dependency is missing, then [rememberSystemCamera] is used.
         */
        val isImglyCameraAvailable = androidx.compose.runtime.remember {
            runCatching { CaptureVideo() }.isSuccess
        } &&
            IMGLYCameraFeature.enabled
        if (isImglyCameraAvailable) {
            Button.rememberImglyCamera()
        } else {
            Button.rememberSystemCamera()
        }
    }
    add { Button.rememberOverlaysLibrary() }
    add { Button.rememberTextLibrary() }
    add { Button.rememberStickersAndShapesLibrary() }
    add { Button.rememberAudiosLibrary() }
    add { Button.rememberReorder() }
}

/**
 * The id of the dock button returned by [Dock.Button.Companion.rememberElementsLibrary].
 */
val Button.Id.Companion.elementsLibrary by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.elementsLibrary")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens a library sheet with elements via [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [Dock] - [Dock.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated only when the parent component scope ([Dock.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * Default value is always true.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.Elements].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_elements].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] event is invoked with sheet type [SheetType.LibraryAdd] and
 * [ly.img.editor.core.library.AssetLibrary.elements] content is displayed on the sheet.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Button.Companion.rememberElementsLibrary(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = alwaysVisible,
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.Elements },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_elements) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(
            EditorEvent.Sheet.Open(
                type = SheetType.LibraryAdd(
                    libraryCategory = editorContext.assetLibrary.elements(editorContext.engine.scene.getMode()),
                ),
            ),
        )
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.elementsLibrary,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the dock button returned by [Dock.Button.Companion.rememberOverlaysLibrary].
 */
val Button.Id.Companion.overlaysLibrary by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.overlaysLibrary")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens a library sheet with overlays via [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [Dock] - [Dock.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated only when the parent component scope ([Dock.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * Default value is always true.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.AddOverlay].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_overlays].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] event is invoked with sheet type [SheetType.LibraryAdd] and
 * [ly.img.editor.core.library.AssetLibrary.overlays] content is displayed on the sheet.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Button.Companion.rememberOverlaysLibrary(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = alwaysVisible,
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.AddOverlay },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_overlay) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(
            EditorEvent.Sheet.Open(
                type = SheetType.LibraryAdd(
                    libraryCategory = editorContext.assetLibrary.overlays(editorContext.engine.scene.getMode()),
                ),
            ),
        )
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.overlaysLibrary,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the dock button returned by [Dock.Button.Companion.rememberImagesLibrary].
 */
val Button.Id.Companion.imagesLibrary by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.imagesLibrary")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens a library sheet with images via [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [Dock] - [Dock.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated only when the parent component scope ([Dock.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * Default value is always true.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.AddImageForeground].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_image].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] event is invoked with sheet type [SheetType.LibraryAdd] and
 * [ly.img.editor.core.library.AssetLibrary.images] content is displayed on the sheet.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Button.Companion.rememberImagesLibrary(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = alwaysVisible,
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.AddImageForeground },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_image) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(
            EditorEvent.Sheet.Open(
                type = SheetType.LibraryAdd(
                    libraryCategory = editorContext.assetLibrary.images(editorContext.engine.scene.getMode()),
                ),
            ),
        )
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.imagesLibrary,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the dock button returned by [Dock.Button.Companion.rememberTextLibrary].
 */
val Button.Id.Companion.textLibrary by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.textLibrary")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens a library sheet with text via [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [Dock] - [Dock.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated only when the parent component scope ([Dock.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * Default value is always true.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.AddText].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_text].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] event is invoked with sheet type [SheetType.LibraryAdd] and
 * [ly.img.editor.core.library.AssetLibrary.text] content is displayed on the sheet.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Button.Companion.rememberTextLibrary(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = alwaysVisible,
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.AddText },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_text) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(
            EditorEvent.Sheet.Open(
                type = SheetType.LibraryAdd(
                    style = SheetStyle(
                        isFloating = true,
                        maxHeight = Height.Fraction(1F),
                        isHalfExpandingEnabled = true,
                        isHalfExpandedInitially = true,
                    ),
                    libraryCategory = editorContext.assetLibrary.text(editorContext.engine.scene.getMode()),
                ),
            ),
        )
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.textLibrary,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the dock button returned by [Dock.Button.Companion.rememberShapesLibrary].
 */
val Button.Id.Companion.shapesLibrary by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.shapesLibrary")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens a library sheet with shapes via [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [Dock] - [Dock.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated only when the parent component scope ([Dock.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * Default value is always true.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.AddShape].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_shape].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] event is invoked with sheet type [SheetType.LibraryAdd] and
 * [ly.img.editor.core.library.AssetLibrary.shapes] content is displayed on the sheet.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Button.Companion.rememberShapesLibrary(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = alwaysVisible,
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.AddShape },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_shape) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(
            EditorEvent.Sheet.Open(
                type = SheetType.LibraryAdd(
                    libraryCategory = editorContext.assetLibrary.shapes(editorContext.engine.scene.getMode()),
                ),
            ),
        )
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.shapesLibrary,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the dock button returned by [Dock.Button.Companion.rememberStickersLibrary].
 */
val Button.Id.Companion.stickersLibrary by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.stickersLibrary")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens a library sheet with stickers via [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [Dock] - [Dock.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated only when the parent component scope ([Dock.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * Default value is always true.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.AddSticker].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_sticker].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] event is invoked with sheet type [SheetType.LibraryAdd] and
 * [ly.img.editor.core.library.AssetLibrary.stickers] content is displayed on the sheet.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Button.Companion.rememberStickersLibrary(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = alwaysVisible,
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.AddSticker },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_sticker) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(
            EditorEvent.Sheet.Open(
                type = SheetType.LibraryAdd(
                    libraryCategory = editorContext.assetLibrary.stickers(editorContext.engine.scene.getMode()),
                ),
            ),
        )
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.stickersLibrary,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the dock button returned by [Dock.Button.Companion.rememberStickersAndShapesLibrary].
 */
val Button.Id.Companion.stickersAndShapesLibrary by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.stickersAndShapesLibrary")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens a library sheet with stickers and shapes via [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [Dock] - [Dock.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated only when the parent component scope ([Dock.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * Default value is always true.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.AddSticker].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_sticker].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] event is invoked with sheet type [SheetType.LibraryAdd] and
 * [ly.img.editor.core.library.AssetLibrary.stickersAndShapes] content is displayed on the sheet.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Button.Companion.rememberStickersAndShapesLibrary(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = alwaysVisible,
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.AddSticker },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_sticker) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(
            EditorEvent.Sheet.Open(
                type = SheetType.LibraryAdd(
                    libraryCategory = editorContext.assetLibrary.stickersAndShapes(editorContext.engine.scene.getMode()),
                ),
            ),
        )
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.stickersAndShapesLibrary,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the dock button returned by [Dock.Button.Companion.rememberAudiosLibrary].
 */
val Button.Id.Companion.audiosLibrary by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.audiosLibrary")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens a library sheet with audios via [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [Dock] - [Dock.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated only when the parent component scope ([Dock.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * Default value is always true.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.AddAudio].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_audio].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] event is invoked with sheet type [SheetType.LibraryAdd] and
 * [ly.img.editor.core.library.AssetLibrary.audios] content is displayed on the sheet.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Button.Companion.rememberAudiosLibrary(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = alwaysVisible,
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.AddAudio },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_audio) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(
            EditorEvent.Sheet.Open(
                type = SheetType.LibraryAdd(
                    libraryCategory = editorContext.assetLibrary.audios(editorContext.engine.scene.getMode()),
                ),
            ),
        )
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.audiosLibrary,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the dock button returned by [Dock.Button.Companion.rememberSystemGallery].
 */
val Button.Id.Companion.systemGallery by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.systemGallery")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that opens the system gallery via [EditorEvent.LaunchContract].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [Dock] - [Dock.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated only when the parent component scope ([Dock.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * Default value is always true.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.AddGalleryForeground].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_gallery].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.LaunchContract] event is invoked with [ActivityResultContracts.PickVisualMedia] contract.
 * If the editor has a video scene, then both images and videos are allowed to be picked, if not then only images.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Button.Companion.rememberSystemGallery(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = alwaysVisible,
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = {
        if (editorContext.engine.scene.getMode() == SceneMode.VIDEO) {
            IconPack.AddGalleryBackground
        } else {
            IconPack.AddGalleryForeground
        }
    },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_gallery) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        val mediaType = if (editorContext.engine.scene.getMode() == SceneMode.VIDEO) {
            ActivityResultContracts.PickVisualMedia.ImageAndVideo
        } else {
            ActivityResultContracts.PickVisualMedia.ImageOnly
        }
        val request = PickVisualMediaRequest(mediaType)
        val event = EditorEvent.LaunchContract(ActivityResultContracts.PickVisualMedia(), request) {
            it?.let {
                val uploadAssetSourceType = if (editorContext.activity.contentResolver.getType(it)?.startsWith("video") == true) {
                    AssetSourceType.VideoUploads
                } else {
                    AssetSourceType.ImageUploads
                }
                editorContext.eventHandler.send(
                    EditorEvent.AddUriToScene(uploadAssetSourceType, it),
                )
            }
        }
        editorContext.eventHandler.send(event)
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.systemGallery,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the dock button returned by [Dock.Button.Companion.rememberSystemCamera].
 */
val Button.Id.Companion.systemCamera by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.systemCamera")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that opens the system camera via [EditorEvent.LaunchContract].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [Dock] - [Dock.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated only when the parent component scope ([Dock.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * Default value is always true.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.AddCameraForeground].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_camera].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.LaunchContract] event is invoked with [ActivityResultContracts.CaptureVideo] contract when the editor
 * is a video scene and [ActivityResultContracts.TakePicture] when it is not.
 * The image/video is stored in the local files dir. After returning to the editor, the uri is added to the scene via
 * [EditorEvent.AddUriToScene] event and is converted to an asset and stored in [AssetSourceType.ImageUploads] or
 * [AssetSourceType.VideoUploads] depending on the type of the content.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Button.Companion.rememberSystemCamera(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = alwaysVisible,
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = {
        if (editorContext.engine.scene.getMode() == SceneMode.VIDEO) {
            IconPack.AddCameraBackground
        } else {
            IconPack.AddCameraForeground
        }
    },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_camera) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        val context = editorContext.activity
        val uri = File.createTempFile("imgly_", null, context.filesDir).let {
            FileProvider.getUriForFile(context, "${context.packageName}.ly.img.editor.fileprovider", it)
        }
        val isVideoScene = editorContext.engine.scene.getMode() == SceneMode.VIDEO
        val launchContract = if (isVideoScene) {
            ActivityResultContracts.CaptureVideo()
        } else {
            ActivityResultContracts.TakePicture()
        }
        val event = EditorEvent.LaunchContract(launchContract, uri) {
            if (it) {
                val assetSourceType = if (isVideoScene) {
                    AssetSourceType.VideoUploads
                } else {
                    AssetSourceType.ImageUploads
                }
                editorContext.eventHandler.send(EditorEvent.AddUriToScene(assetSourceType, uri))
            }
        }
        editorContext.eventHandler.send(event)
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.systemCamera,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the dock button returned by [Dock.Button.Companion.rememberImglyCamera].
 */
val Button.Id.Companion.imglyCamera by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.imglyCamera")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that opens the imgly camera via [EditorEvent.LaunchContract].
 * IMPORTANT: Make sure your app has the dependency of ly.img:camera:<version> next to the ly.img:editor:<version> dependency.
 * Also make sure that their versions match. Failing to provide the dependency will result to a crash.
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [Dock] - [Dock.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated only when the parent component scope ([Dock.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * Default value is always true.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.AddCameraForeground].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_camera].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.LaunchContract] event is invoked with [CaptureVideo] contract.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Button.Companion.rememberImglyCamera(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = alwaysVisible,
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.AddCameraBackground },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_camera) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
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
                        editorContext.eventHandler.send(
                            EditorEvent.AddCameraRecordingsToScene(
                                uploadAssetSourceType = AssetSourceType.VideoUploads,
                                recordings = recordings,
                            ),
                        )
                    }
            },
        ).let { editorContext.eventHandler.send(it) }
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.imglyCamera,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the dock button returned by [Dock.Button.Companion.rememberReorder].
 */
val Button.Id.Companion.reorder by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.reorder")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that
 * opens reorder sheet via [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [Dock] - [Dock.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated only when the parent component scope ([Dock.scope], accessed via [LocalEditorScope]) is updated and
 * whenever the number of children in the background track becomes >= or < than 2.
 * @param visible whether the button should be visible.
 * By default the value is true when the editor has a scene with a background track where the number of tracks is >= 2, false otherwise.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.ReorderHorizontally].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_reorder].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] is invoked with sheet type [SheetType.Reorder].
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Button.Companion.rememberReorder(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            val backgroundTrack = editorContext.engine.block.findByType(DesignBlockType.Track).firstOrNull {
                DesignBlockType.get(editorContext.engine.block.getType(it)) == DesignBlockType.Track &&
                    editorContext.engine.block.isAlwaysOnBottom(it)
            }
            backgroundTrack?.let { editorContext.engine.block.getChildren(it).size >= 2 } ?: false
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.ReorderHorizontally },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_reorder) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Reorder()))
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.reorder,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the dock button returned by [Dock.Button.Companion.rememberAdjustments].
 */
val Button.Id.Companion.adjustments by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.adjustments")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that opens adjustments sheet for the current page via
 * [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [Dock] - [Dock.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated only when the parent component scope ([Dock.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the current page of the scene has an enabled engine scope "appearance/adjustments".
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.Adjustments].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is is always [R.string.ly_img_editor_adjustments].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] is invoked with sheet type [SheetType.Adjustments].
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Button.Companion.rememberAdjustments(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            val designBlock = editorContext.engine.scene.getCurrentPage() ?: editorContext.engine.scene.getPages()[0]
            editorContext.engine.block.isAllowedByScope(designBlock, "appearance/adjustments")
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.Adjustments },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_adjustments) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Adjustments()))
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.adjustments,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the dock button returned by [Dock.Button.Companion.rememberFilter].
 */
val Button.Id.Companion.filter by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.filter")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that opens filter sheet for the current page via
 * [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [Dock] - [Dock.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated only when the parent component scope ([Dock.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the current page of the scene has an enabled engine scope "appearance/filter".
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.Filter].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is is always [R.string.ly_img_editor_filter].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] is invoked with sheet type [SheetType.Filter].
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Button.Companion.rememberFilter(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            val designBlock = editorContext.engine.scene.getCurrentPage() ?: editorContext.engine.scene.getPages()[0]
            editorContext.engine.block.isAllowedByScope(designBlock, "appearance/filter")
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.Filter },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_filter) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Filter()))
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.filter,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the dock button returned by [Dock.Button.Companion.rememberEffect].
 */
val Button.Id.Companion.effect by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.effect")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that opens effect sheet for the current page via
 * [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [Dock] - [Dock.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated only when the parent component scope ([Dock.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the current page of the scene has an enabled engine scope "appearance/effect".
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.Effect].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is is always [R.string.ly_img_editor_effect].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] is invoked with sheet type [SheetType.Effect].
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Button.Companion.rememberEffect(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            val designBlock = editorContext.engine.scene.getCurrentPage() ?: editorContext.engine.scene.getPages()[0]
            editorContext.engine.block.isAllowedByScope(designBlock, "appearance/effect")
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.Effect },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_effect) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Effect()))
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.effect,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the dock button returned by [Dock.Button.Companion.rememberBlur].
 */
val Button.Id.Companion.blur by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.blur")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that opens blur sheet for the current page via
 * [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [Dock] - [Dock.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated only when the parent component scope ([Dock.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the current page of the scene has an enabled engine scope "appearance/blur".
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.Blur].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is is always [R.string.ly_img_editor_blur].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] is invoked with sheet type [SheetType.Blur].
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Button.Companion.rememberBlur(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            val designBlock = editorContext.engine.scene.getCurrentPage() ?: editorContext.engine.scene.getPages()[0]
            editorContext.engine.block.isAllowedByScope(designBlock, "appearance/blur")
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.Blur },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_blur) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Blur()))
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.blur,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)

/**
 * The id of the dock button returned by [Dock.Button.Companion.rememberCrop].
 */
val Button.Id.Companion.crop by unsafeLazy {
    EditorComponentId("ly.img.component.dock.button.crop")
}

/**
 * A composable helper function that creates and remembers a [Dock.Button] that opens crop sheet for the current page via
 * [EditorEvent.Sheet.Open].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [Dock] - [Dock.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated only when the parent component scope ([Dock.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true when the current page of the scene has an enabled engine scope "layer/crop"
 * and when [ly.img.engine.BlockApi.supportsCrop] is true for that page.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.CropRotate].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is is always [R.string.ly_img_editor_crop].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Sheet.Open] is invoked with sheet type [SheetType.Crop].
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the dock.
 */
@Composable
fun Button.Companion.rememberCrop(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            val designBlock = editorContext.engine.scene.getCurrentPage() ?: editorContext.engine.scene.getPages()[0]
            editorContext.engine.block.supportsCrop(designBlock) &&
                editorContext.engine.block.isAllowedByScope(designBlock, "layer/crop")
        }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.CropRotate },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_crop) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Sheet.Open(SheetType.Crop()))
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.crop,
    scope = scope,
    visible = visible,
    enterTransition = enterTransition,
    exitTransition = exitTransition,
    decoration = decoration,
    vectorIcon = vectorIcon,
    text = text,
    tint = tint,
    enabled = enabled,
    onClick = onClick,
    contentDescription = contentDescription,
    `_` = `_`,
)
