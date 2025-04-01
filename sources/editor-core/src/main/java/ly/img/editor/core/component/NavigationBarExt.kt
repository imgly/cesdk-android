package ly.img.editor.core.component

import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import ly.img.editor.core.EditorScope
import ly.img.editor.core.LocalEditorScope
import ly.img.editor.core.R
import ly.img.editor.core.UnstableEditorApi
import ly.img.editor.core.component.EditorComponent.Companion.alwaysVisible
import ly.img.editor.core.component.EditorComponent.Companion.noneEnterTransition
import ly.img.editor.core.component.EditorComponent.Companion.noneExitTransition
import ly.img.editor.core.component.EditorComponent.ListBuilder
import ly.img.editor.core.component.NavigationBar.Button
import ly.img.editor.core.component.NavigationBar.ButtonScope
import ly.img.editor.core.component.NavigationBar.Companion.DefaultDecoration
import ly.img.editor.core.component.NavigationBar.Item
import ly.img.editor.core.component.NavigationBar.Scope
import ly.img.editor.core.component.data.Nothing
import ly.img.editor.core.component.data.nothing
import ly.img.editor.core.component.data.unsafeLazy
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.iconpack.ArrowBack
import ly.img.editor.core.iconpack.ArrowForward
import ly.img.editor.core.iconpack.Export
import ly.img.editor.core.iconpack.IconPack
import ly.img.editor.core.iconpack.Pages
import ly.img.editor.core.iconpack.Preview
import ly.img.editor.core.iconpack.PreviewToggled
import ly.img.editor.core.iconpack.Redo
import ly.img.editor.core.iconpack.Undo
import ly.img.editor.core.state.EditorViewMode
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import ly.img.engine.SceneMode

/**
 * A composable helper function that creates and remembers a [NavigationBar] instance when launching [ly.img.editor.DesignEditor].
 * By default, the following items are added to the navigation bar:
 *
 * // Aligned at the start
 * - NavigationBar.Button.rememberCloseEditor
 *
 * // Aligned at the end
 * - NavigationBar.Button.rememberUndo
 * - NavigationBar.Button.rememberRedo
 * - NavigationBar.Button.rememberTogglePagesMode
 * - NavigationBar.Button.rememberExport
 *
 * For more information on how to customize [listBuilder], check [NavigationBar.remember].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas
 * for granular recompositions over updating the scope, since scope change triggers full recomposition of the navigation bar.
 * Also prefer updating individual [Item]s over updating the whole [NavigationBar].
 * Ideally, scope should be updated when the parent scope (scope of the parent component) is updated and when you want to
 * observe changes from the [Engine].
 * By default it is updated only when the parent scope (accessed via [LocalEditorScope]) is updated and when editor history is changed.
 * @param visible whether the navigation bar should be visible based on the [Engine]'s current state.
 * Default value is always true.
 * @param enterTransition transition of the navigation bar when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the navigation bar when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is [NavigationBar.DefaultDecoration].
 * @param listBuilder a builder that registers the list of [NavigationBar.Item]s that should be part of the navigation bar.
 * Note that registering does not mean displaying. The items will be displayed if [NavigationBar.Item.visible] is true for them.
 * Also note that items will be rebuilt when [scope] is updated.
 * By default, the list mentioned above is added to the navigation bar.
 * @param horizontalArrangement the horizontal arrangement that should be used to render the items in the navigation bar horizontally.
 * Note that the value will be ignored in case [listBuilder] contains aligned items. Check [EditorComponent.ListBuilder.Scope.New.aligned] for more
 * details on how to configure arrangement of aligned items.
 * Default value is [Arrangement.Start].
 * @param itemDecoration decoration of the items in the navigation bar. Useful when you want to add custom background, foreground, shadow,
 * paddings etc to the items. Prefer using this decoration when you want to apply the same decoration to all the items, otherwise
 * set decoration to individual items.
 * Default value is always no decoration.
 * @return a navigation bar that will be displayed when launching a [ly.img.editor.DesignEditor].
 */
@UnstableEditorApi
@Composable
fun NavigationBar.Companion.rememberForDesign(
    scope: Scope = LocalEditorScope.current.run {
        var trigger by remember { mutableStateOf(false) }
        LaunchedEffect(this) {
            editorContext.engine.editor.onHistoryUpdated()
                .onEach { trigger = trigger.not() }
                .collect()
        }
        remember(this, trigger) { Scope(parentScope = this) }
    },
    visible: @Composable Scope.() -> Boolean = alwaysVisible,
    enterTransition: @Composable Scope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable Scope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable Scope.(@Composable () -> Unit) -> Unit = { DefaultDecoration { it() } },
    listBuilder: HorizontalListBuilder<Item<*>> = NavigationBar.ListBuilder.rememberForDesign(),
    horizontalArrangement: @Composable Scope.() -> Arrangement.Horizontal = { Arrangement.Start },
    itemDecoration: @Composable Scope.(content: @Composable () -> Unit) -> Unit = { it() },
    `_`: Nothing = nothing,
): NavigationBar = remember(
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
 * designed to use with [NavigationBar.Companion.rememberForDesign].
 *
 * It is convenient to use this helper function when you want to add additional items at the end of the list, or replace
 * the default items without touching the order in the navigation bar when launching a [ly.img.editor.DesignEditor].
 * For more complex adjustments consider using [EditorComponent.ListBuilder.remember].
 */
@UnstableEditorApi
@Composable
fun NavigationBar.ListBuilder.Companion.rememberForDesign(): HorizontalListBuilder<Item<*>> = ListBuilder.remember {
    aligned(alignment = Alignment.Start) {
        add { Button.rememberCloseEditor() }
    }
    aligned(alignment = Alignment.End) {
        add { Button.rememberUndo() }
        add { Button.rememberRedo() }
        add { Button.rememberTogglePagesMode() }
        add { Button.rememberExport() }
    }
}

/**
 * A composable helper function that creates and remembers a [NavigationBar] instance when launching [ly.img.editor.PhotoEditor].
 * By default, the following items are added to the navigation bar:
 *
 * // Aligned at the start
 * - NavigationBar.Button.rememberCloseEditor
 *
 * // Aligned at the end
 * - NavigationBar.Button.rememberUndo
 * - NavigationBar.Button.rememberRedo
 * - NavigationBar.Button.rememberTogglePreviewMode
 * - NavigationBar.Button.rememberExport
 *
 * For more information on how to customize [listBuilder], check [NavigationBar.remember].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas
 * for granular recompositions over updating the scope, since scope change triggers full recomposition of the navigation bar.
 * Also prefer updating individual [NavigationBar.Item]s over updating the whole [NavigationBar].
 * Ideally, scope should be updated when the parent scope (scope of the parent component) is updated and when you want to
 * observe changes from the [Engine].
 * By default it is updated only when the parent scope (accessed via [LocalEditorScope]) is updated and when editor history is changed.
 * @param visible whether the navigation bar should be visible based on the [Engine]'s current state.
 * Default value is always true.
 * @param enterTransition transition of the navigation bar when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the navigation bar when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is [NavigationBar.DefaultDecoration].
 * @param listBuilder a builder that registers the list of [NavigationBar.Item]s that should be part of the navigation bar.
 * Note that registering does not mean displaying. The items will be displayed if [NavigationBar.Item.visible] is true for them.
 * Also note that items will be rebuilt when [scope] is updated.
 * By default, the list mentioned above is added to the navigation bar.
 * @param horizontalArrangement the horizontal arrangement that should be used to render the items in the navigation bar horizontally.
 * Note that the value will be ignored in case [listBuilder] contains aligned items. Check [EditorComponent.ListBuilder.Scope.New.aligned] for more
 * details on how to configure arrangement of aligned items.
 * Default value is [Arrangement.Start].
 * @param itemDecoration decoration of the items in the navigation bar. Useful when you want to add custom background, foreground, shadow,
 * paddings etc to the items. Prefer using this decoration when you want to apply the same decoration to all the items, otherwise
 * set decoration to individual items.
 * Default value is always no decoration.
 * @return a navigation bar that will be displayed when launching a [ly.img.editor.PhotoEditor].
 */
@UnstableEditorApi
@Composable
fun NavigationBar.Companion.rememberForPhoto(
    scope: Scope = LocalEditorScope.current.run {
        var trigger by remember { mutableStateOf(false) }
        LaunchedEffect(this) {
            editorContext.engine.editor.onHistoryUpdated()
                .onEach { trigger = trigger.not() }
                .collect()
        }
        remember(this, trigger) { Scope(parentScope = this) }
    },
    visible: @Composable Scope.() -> Boolean = alwaysVisible,
    enterTransition: @Composable Scope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable Scope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable Scope.(@Composable () -> Unit) -> Unit = { DefaultDecoration { it() } },
    listBuilder: HorizontalListBuilder<Item<*>> = NavigationBar.ListBuilder.rememberForPhoto(),
    horizontalArrangement: @Composable Scope.() -> Arrangement.Horizontal = { Arrangement.Start },
    itemDecoration: @Composable Scope.(content: @Composable () -> Unit) -> Unit = { it() },
    `_`: Nothing = nothing,
): NavigationBar = remember(
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
 * designed to use with [NavigationBar.Companion.rememberForPhoto].
 *
 * It is convenient to use this helper function when you want to add additional items at the end of the list, or replace
 * the default items without touching the order in the navigation bar when launching a [ly.img.editor.PhotoEditor].
 * For more complex adjustments consider using [EditorComponent.ListBuilder.remember].
 */
@UnstableEditorApi
@Composable
fun NavigationBar.ListBuilder.Companion.rememberForPhoto(): HorizontalListBuilder<Item<*>> = ListBuilder.remember {
    aligned(alignment = Alignment.Start) {
        add { Button.rememberCloseEditor() }
    }
    aligned(alignment = Alignment.End) {
        add { Button.rememberUndo() }
        add { Button.rememberRedo() }
        add { Button.rememberTogglePreviewMode() }
        add { Button.rememberExport() }
    }
}

/**
 * A composable helper function that creates and remembers a [NavigationBar] instance when launching [ly.img.editor.VideoEditor].
 * By default, the following items are added to the navigation bar:
 *
 * // Aligned at the start
 * - NavigationBar.Button.rememberCloseEditor
 *
 * // Aligned at the end
 * - NavigationBar.Button.rememberUndo
 * - NavigationBar.Button.rememberRedo
 * - NavigationBar.Button.rememberExport
 *
 * For more information on how to customize [listBuilder], check [NavigationBar.remember].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas
 * for granular recompositions over updating the scope, since scope change triggers full recomposition of the navigation bar.
 * Also prefer updating individual [Item]s over updating the whole [NavigationBar].
 * Ideally, scope should be updated when the parent scope (scope of the parent component) is updated and when you want to
 * observe changes from the [Engine].
 * By default it is updated only when the parent scope (accessed via [LocalEditorScope]) is updated and when editor history is changed.
 * @param visible whether the navigation bar should be visible based on the [Engine]'s current state.
 * Default value is always true.
 * @param enterTransition transition of the navigation bar when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the navigation bar when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is [NavigationBar.DefaultDecoration].
 * @param listBuilder a builder that registers the list of [NavigationBar.Item]s that should be part of the navigation bar.
 * Note that registering does not mean displaying. The items will be displayed if [NavigationBar.Item.visible] is true for them.
 * Also note that items will be rebuilt when [scope] is updated.
 * By default, the list mentioned above is added to the navigation bar.
 * @param horizontalArrangement the horizontal arrangement that should be used to render the items in the navigation bar horizontally.
 * Note that the value will be ignored in case [listBuilder] contains aligned items. Check [EditorComponent.ListBuilder.Scope.New.aligned] for more
 * details on how to configure arrangement of aligned items.
 * Default value is [Arrangement.Start].
 * @param itemDecoration decoration of the items in the navigation bar. Useful when you want to add custom background, foreground, shadow,
 * paddings etc to the items. Prefer using this decoration when you want to apply the same decoration to all the items, otherwise
 * set decoration to individual items.
 * Default value is always no decoration.
 * @return a navigation bar that will be displayed when launching a [ly.img.editor.VideoEditor].
 */
@UnstableEditorApi
@Composable
fun NavigationBar.Companion.rememberForVideo(
    scope: Scope = LocalEditorScope.current.run {
        var trigger by remember { mutableStateOf(false) }
        LaunchedEffect(this) {
            editorContext.engine.editor.onHistoryUpdated()
                .onEach { trigger = trigger.not() }
                .collect()
        }
        remember(this, trigger) { Scope(parentScope = this) }
    },
    visible: @Composable Scope.() -> Boolean = alwaysVisible,
    enterTransition: @Composable Scope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable Scope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable Scope.(@Composable () -> Unit) -> Unit = { DefaultDecoration { it() } },
    listBuilder: HorizontalListBuilder<Item<*>> = NavigationBar.ListBuilder.rememberForVideo(),
    horizontalArrangement: @Composable Scope.() -> Arrangement.Horizontal = { Arrangement.Start },
    itemDecoration: @Composable Scope.(content: @Composable () -> Unit) -> Unit = { it() },
    `_`: Nothing = nothing,
): NavigationBar = remember(
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
 * designed to use with [NavigationBar.Companion.rememberForVideo].
 *
 * It is convenient to use this helper function when you want to add additional items at the end of the list, or replace
 * the default items without touching the order in the navigation bar when launching a [ly.img.editor.VideoEditor].
 * For more complex adjustments consider using [EditorComponent.ListBuilder.remember].
 */
@UnstableEditorApi
@Composable
fun NavigationBar.ListBuilder.Companion.rememberForVideo(): HorizontalListBuilder<Item<*>> = ListBuilder.remember {
    aligned(alignment = Alignment.Start) {
        add { Button.rememberCloseEditor() }
    }
    aligned(alignment = Alignment.End) {
        add { Button.rememberUndo() }
        add { Button.rememberRedo() }
        add { Button.rememberExport() }
    }
}

/**
 * A composable helper function that creates and remembers a [NavigationBar] instance when launching [ly.img.editor.ApparelEditor].
 * By default, the following items are added to the navigation bar:
 *
 * // Aligned at the start
 * - NavigationBar.Button.rememberCloseEditor
 *
 * // Aligned at the end
 * - NavigationBar.Button.rememberUndo
 * - NavigationBar.Button.rememberRedo
 * - NavigationBar.Button.rememberTogglePreviewMode
 * - NavigationBar.Button.rememberExport
 *
 * For more information on how to customize [listBuilder], check [NavigationBar.remember].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas
 * for granular recompositions over updating the scope, since scope change triggers full recomposition of the navigation bar.
 * Also prefer updating individual [Item]s over updating the whole [NavigationBar].
 * Ideally, scope should be updated when the parent scope (scope of the parent component) is updated and when you want to
 * observe changes from the [Engine].
 * By default it is updated only when the parent scope (accessed via [LocalEditorScope]) is updated and when editor history is changed.
 * @param visible whether the navigation bar should be visible based on the [Engine]'s current state.
 * Default value is always true.
 * @param enterTransition transition of the navigation bar when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the navigation bar when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is [NavigationBar.DefaultDecoration].
 * @param listBuilder a builder that registers the list of [NavigationBar.Item]s that should be part of the navigation bar.
 * Note that registering does not mean displaying. The items will be displayed if [NavigationBar.Item.visible] is true for them.
 * Also note that items will be rebuilt when [scope] is updated.
 * By default, the list mentioned above is added to the navigation bar.
 * @param horizontalArrangement the horizontal arrangement that should be used to render the items in the navigation bar horizontally.
 * Note that the value will be ignored in case [listBuilder] contains aligned items. Check [EditorComponent.ListBuilder.Scope.New.aligned] for more
 * details on how to configure arrangement of aligned items.
 * Default value is [Arrangement.Start].
 * @param itemDecoration decoration of the items in the navigation bar. Useful when you want to add custom background, foreground, shadow,
 * paddings etc to the items. Prefer using this decoration when you want to apply the same decoration to all the items, otherwise
 * set decoration to individual items.
 * Default value is always no decoration.
 * @return a navigation bar that will be displayed when launching a [ly.img.editor.ApparelEditor].
 */
@UnstableEditorApi
@Composable
fun NavigationBar.Companion.rememberForApparel(
    scope: Scope = LocalEditorScope.current.run {
        var trigger by remember { mutableStateOf(false) }
        LaunchedEffect(this) {
            editorContext.engine.editor.onHistoryUpdated()
                .onEach { trigger = trigger.not() }
                .collect()
        }
        remember(this, trigger) { Scope(parentScope = this) }
    },
    visible: @Composable Scope.() -> Boolean = alwaysVisible,
    enterTransition: @Composable Scope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable Scope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable Scope.(@Composable () -> Unit) -> Unit = { DefaultDecoration { it() } },
    listBuilder: HorizontalListBuilder<Item<*>> = NavigationBar.ListBuilder.rememberForApparel(),
    horizontalArrangement: @Composable Scope.() -> Arrangement.Horizontal = { Arrangement.Start },
    itemDecoration: @Composable Scope.(content: @Composable () -> Unit) -> Unit = { it() },
    `_`: Nothing = nothing,
): NavigationBar = remember(
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
 * designed to use with [NavigationBar.Companion.rememberForApparel].
 *
 * It is convenient to use this helper function when you want to add additional items at the end of the list, or replace
 * the default items without touching the order in the navigation bar when launching a [ly.img.editor.ApparelEditor].
 * For more complex adjustments consider using [EditorComponent.ListBuilder.remember].
 */
@UnstableEditorApi
@Composable
fun NavigationBar.ListBuilder.Companion.rememberForApparel(): HorizontalListBuilder<Item<*>> = ListBuilder.remember {
    aligned(alignment = Alignment.Start) {
        add { Button.rememberCloseEditor() }
    }
    aligned(alignment = Alignment.End) {
        add { Button.rememberUndo() }
        add { Button.rememberRedo() }
        add { Button.rememberTogglePreviewMode() }
        add { Button.rememberExport() }
    }
}

/**
 * A composable helper function that creates and remembers a [NavigationBar] instance when launching [ly.img.editor.PostcardEditor].
 * By default, the following items are added to the navigation bar:
 *
 * // Aligned at the start
 * - NavigationBar.Button.rememberCloseEditor
 * - NavigationBar.Button.rememberPreviousPage
 *
 * // Aligned at the center
 * - NavigationBar.Button.rememberUndo
 * - NavigationBar.Button.rememberRedo
 * - NavigationBar.Button.rememberTogglePreviewMode
 *
 * // Aligned at the end
 * - NavigationBar.Button.rememberNextPage
 * - NavigationBar.Button.rememberExport
 *
 * For more information on how to customize [listBuilder], check [NavigationBar.remember].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas
 * for granular recompositions over updating the scope, since scope change triggers full recomposition of the navigation bar.
 * Also prefer updating individual [Item]s over updating the whole [NavigationBar].
 * Ideally, scope should be updated when the parent scope (scope of the parent component) is updated and when you want to
 * observe changes from the [Engine].
 * By default it is updated only when the parent scope (accessed via [LocalEditorScope]) is updated, when editor history is changed and
 * when the current page is changed.
 * @param visible whether the navigation bar should be visible based on the [Engine]'s current state.
 * Default value is always true.
 * @param enterTransition transition of the navigation bar when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the navigation bar when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is [NavigationBar.DefaultDecoration].
 * @param listBuilder a builder that registers the list of [NavigationBar.Item]s that should be part of the navigation bar.
 * Note that registering does not mean displaying. The items will be displayed if [NavigationBar.Item.visible] is true for them.
 * Also note that items will be rebuilt when [scope] is updated.
 * By default, the list mentioned above is added to the navigation bar.
 * @param horizontalArrangement the horizontal arrangement that should be used to render the items in the navigation bar horizontally.
 * Note that the value will be ignored in case [listBuilder] contains aligned items. Check [EditorComponent.ListBuilder.Scope.New.aligned] for more
 * details on how to configure arrangement of aligned items.
 * Default value is [Arrangement.Start].
 * @param itemDecoration decoration of the items in the navigation bar. Useful when you want to add custom background, foreground, shadow,
 * paddings etc to the items. Prefer using this decoration when you want to apply the same decoration to all the items, otherwise
 * set decoration to individual items.
 * Default value is always no decoration.
 * @return a navigation bar that will be displayed when launching a [ly.img.editor.PostcardEditor].
 */
@UnstableEditorApi
@Composable
fun NavigationBar.Companion.rememberForPostcard(
    scope: Scope = LocalEditorScope.current.run {
        val pageIndex by remember(this) {
            val stack = editorContext.engine.block.findByType(DesignBlockType.Stack).first()
            editorContext.engine.event.subscribe(listOf(stack))
                .map {
                    val currentPage = editorContext.engine.scene.getCurrentPage()
                    editorContext.engine.scene.getPages().indexOf(currentPage)
                }
        }.collectAsState(initial = 0)
        var trigger by remember { mutableStateOf(false) }
        LaunchedEffect(this) {
            editorContext.engine.editor.onHistoryUpdated()
                .onEach { trigger = trigger.not() }
                .collect()
        }
        remember(this, pageIndex, trigger) {
            Scope(parentScope = this)
        }
    },
    visible: @Composable Scope.() -> Boolean = alwaysVisible,
    enterTransition: @Composable Scope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable Scope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable Scope.(@Composable () -> Unit) -> Unit = { DefaultDecoration { it() } },
    listBuilder: HorizontalListBuilder<Item<*>> = NavigationBar.ListBuilder.rememberForPostcard(),
    horizontalArrangement: @Composable Scope.() -> Arrangement.Horizontal = { Arrangement.Start },
    itemDecoration: @Composable Scope.(content: @Composable () -> Unit) -> Unit = { it() },
    `_`: Nothing = nothing,
): NavigationBar = remember(
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
 * designed to use with [NavigationBar.Companion.rememberForPostcard].
 *
 * It is convenient to use this helper function when you want to add additional items at the end of the list, or replace
 * the default items without touching the order in the navigation bar when launching a [ly.img.editor.PostcardEditor].
 * For more complex adjustments consider using [EditorComponent.ListBuilder.remember].
 */
@UnstableEditorApi
@Composable
fun NavigationBar.ListBuilder.Companion.rememberForPostcard(): HorizontalListBuilder<Item<*>> = ListBuilder.remember {
    aligned(alignment = Alignment.Start) {
        add { Button.rememberCloseEditor() }
        add {
            Button.rememberPreviousPage(
                text = { stringResource(R.string.ly_img_editor_design) },
            )
        }
    }

    aligned(alignment = Alignment.CenterHorizontally) {
        add { Button.rememberUndo() }
        add { Button.rememberRedo() }
        add { Button.rememberTogglePreviewMode() }
    }

    aligned(alignment = Alignment.End) {
        add {
            Button.rememberNextPage(
                text = { stringResource(R.string.ly_img_editor_write) },
            )
        }
        add { Button.rememberExport() }
    }
}

/**
 * The id of the navigation bar button returned by [NavigationBar.Button.Companion.rememberCloseEditor].
 */
val Button.Id.Companion.closeEditor by unsafeLazy {
    EditorComponentId("ly.img.component.navigationBar.button.closeEditor")
}

/**
 * A composable helper function that creates and remembers a [NavigationBar.Button] that triggers
 * [ly.img.editor.EngineConfiguration.onClose] callback via [EditorEvent.OnClose].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [NavigationBar] - [NavigationBar.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated only when the parent component scope ([NavigationBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true if the current view mode is [EditorViewMode.Edit] and either "features/pageCarouselEnabled" engine setting
 * is true or the current page is the first page of the scene, false otherwise.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.ArrowBack].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is null.
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.OnClose] event is invoked.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is always [R.string.ly_img_editor_back].
 * @return a button that will be displayed in the navigation bar.
 */
@Composable
fun Button.Companion.rememberCloseEditor(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        val state by editorContext.state.collectAsState()
        state.viewMode is EditorViewMode.Edit &&
            remember(this) {
                editorContext.engine.editor.getSettingBoolean("features/pageCarouselEnabled") ||
                    editorContext.engine.scene.run { getPages().firstOrNull() == getCurrentPage() }
            }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.ArrowBack },
    text: (@Composable ButtonScope.() -> String)? = null,
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.OnClose())
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = {
        stringResource(R.string.ly_img_editor_back)
    },
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.closeEditor,
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
 * The id of the navigation bar button returned by [NavigationBar.Button.Companion.rememberUndo].
 */
val Button.Id.Companion.undo by unsafeLazy {
    EditorComponentId("ly.img.component.navigationBar.button.undo")
}

/**
 * A composable helper function that creates and remembers a [NavigationBar.Button] that
 * does undo operation in the editor via [ly.img.engine.EditorApi.undo] engine API.
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [NavigationBar] - [NavigationBar.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated when the parent component scope ([NavigationBar.scope], accessed via [LocalEditorScope]) is updated
 * and whenever the editor history changes.
 * @param visible whether the button should be visible.
 * By default the value is true if current view mode is not [EditorViewMode.Preview].
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.Undo].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is null.
 * @param tint the tint color of the content. If null then no tint is applied.
 * By default the value is [Color.Transparent] when edit mode is [EditorViewMode.Preview],
 * [androidx.compose.material3.ColorScheme.onSurfaceVariant] otherwise.
 * @param enabled whether the button is enabled.
 * By default the value is true when current view mode is not [EditorViewMode.Preview], [ly.img.editor.core.state.EditorState.isHistoryEnabled]
 * is true and [ly.img.engine.EditorApi.canUndo] engine API returns true, false otherwise.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [ly.img.engine.EditorApi.undo] engine API is invoked.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is always [R.string.ly_img_editor_undo].
 * @return a button that will be displayed in the navigation bar.
 */
@Composable
fun Button.Companion.rememberUndo(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = alwaysVisible,
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.Undo },
    text: (@Composable ButtonScope.() -> String)? = null,
    tint: (@Composable ButtonScope.() -> Color)? = {
        val state by editorContext.state.collectAsState()
        val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
        remember(this, state.viewMode, onSurfaceVariant) {
            if (state.viewMode is EditorViewMode.Preview) Color.Transparent else onSurfaceVariant
        }
    },
    enabled: @Composable ButtonScope.() -> Boolean = {
        val state by editorContext.state.collectAsState()
        remember(this, state.viewMode, state.isHistoryEnabled) {
            state.viewMode !is EditorViewMode.Preview && state.isHistoryEnabled && editorContext.engine.editor.canUndo()
        }
    },
    onClick: ButtonScope.() -> Unit = { editorContext.engine.editor.undo() },
    contentDescription: (@Composable ButtonScope.() -> String)? = {
        stringResource(R.string.ly_img_editor_undo)
    },
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.undo,
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
 * The id of the navigation bar button returned by [NavigationBar.Button.Companion.rememberRedo].
 */
val Button.Id.Companion.redo by unsafeLazy {
    EditorComponentId("ly.img.component.navigationBar.button.redo")
}

/**
 * A composable helper function that creates and remembers a [NavigationBar.Button] that
 * does redo operation in the editor via [ly.img.engine.EditorApi.redo] engine API.
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [NavigationBar] - [NavigationBar.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated when the parent component scope ([NavigationBar.scope], accessed via [LocalEditorScope]) is updated
 * and whenever the editor history changes.
 * @param visible whether the button should be visible.
 * By default the value is true if current view mode is not [EditorViewMode.Preview].
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.Redo].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is null.
 * @param tint the tint color of the content. If null then no tint is applied.
 * By default the value is [Color.Transparent] when edit mode is [EditorViewMode.Preview],
 * [androidx.compose.material3.ColorScheme.onSurfaceVariant] otherwise.
 * @param enabled whether the button is enabled.
 * By default the value is true when current view mode is not [EditorViewMode.Preview], [ly.img.editor.core.state.EditorState.isHistoryEnabled]
 * is true and [ly.img.engine.EditorApi.canRedo] engine API returns true, false otherwise.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [ly.img.engine.EditorApi.redo] engine API is invoked.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is always [R.string.ly_img_editor_redo].
 * @return a button that will be displayed in the navigation bar.
 */
@Composable
fun Button.Companion.rememberRedo(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = alwaysVisible,
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.Redo },
    text: (@Composable ButtonScope.() -> String)? = null,
    tint: (@Composable ButtonScope.() -> Color)? = {
        val state by editorContext.state.collectAsState()
        val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
        remember(this, state.viewMode, onSurfaceVariant) {
            if (state.viewMode is EditorViewMode.Preview) Color.Transparent else onSurfaceVariant
        }
    },
    enabled: @Composable ButtonScope.() -> Boolean = {
        val state by editorContext.state.collectAsState()
        remember(this, state.viewMode, state.isHistoryEnabled) {
            state.viewMode !is EditorViewMode.Preview && state.isHistoryEnabled && editorContext.engine.editor.canRedo()
        }
    },
    onClick: ButtonScope.() -> Unit = { editorContext.engine.editor.redo() },
    contentDescription: (@Composable ButtonScope.() -> String)? = {
        stringResource(R.string.ly_img_editor_redo)
    },
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.redo,
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
 * The id of the navigation bar button returned by [NavigationBar.Button.Companion.rememberExport].
 */
val Button.Id.Companion.export by unsafeLazy {
    EditorComponentId("ly.img.component.navigationBar.button.export")
}

/**
 * A composable helper function that creates and remembers a [NavigationBar.Button] that
 * starts export via [EditorEvent.Export].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [NavigationBar] - [NavigationBar.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated only when the parent component scope ([NavigationBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true if the current view mode is not [EditorViewMode.Edit] or either "features/pageCarouselEnabled" engine setting
 * is true or the current page is the last page of the scene, false otherwise.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.Export].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is null.
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * By default the value is true when scene has mode [SceneMode.DESIGN] or when [ly.img.engine.BlockApi.getDuration] engine API
 * returns value greater than 0 for the scene design block, acquired via [ly.img.engine.SceneApi.get].
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Export] event is invoked.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is always [R.string.ly_img_editor_export].
 * @return a button that will be displayed in the navigation bar.
 */
@Composable
fun Button.Companion.rememberExport(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        val state by editorContext.state.collectAsState()
        state.viewMode !is EditorViewMode.Edit ||
            remember(this) {
                editorContext.engine.editor.getSettingBoolean("features/pageCarouselEnabled") ||
                    editorContext.engine.scene.run { getPages().lastOrNull() == getCurrentPage() }
            }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.Export },
    text: (@Composable ButtonScope.() -> String)? = null,
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = {
        remember(this) {
            editorContext.engine.scene.getMode() == SceneMode.DESIGN ||
                editorContext.engine.scene.get()?.let {
                    editorContext.engine.block.getDuration(it) > 0
                } ?: false
        }
    },
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Export.Start())
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = {
        stringResource(R.string.ly_img_editor_export)
    },
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.export,
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
 * The id of the navigation bar button returned by [NavigationBar.Button.Companion.rememberTogglePreviewMode].
 */
val Button.Id.Companion.togglePreviewMode by unsafeLazy {
    EditorComponentId("ly.img.component.navigationBar.button.togglePreviewMode")
}

/**
 * A composable helper function that creates and remembers a [NavigationBar.Button] that
 * updates editor view mode via [EditorEvent.SetViewMode]: when current view mode is [EditorViewMode.Edit], then [EditorViewMode.Preview]
 * is set and vice versa.
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [NavigationBar] - [NavigationBar.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated only when the parent component scope ([NavigationBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * Default value is always true.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * By default a circular background is applied when current view mode is [EditorViewMode.Preview].
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is [IconPack.PreviewToggled] when current view mode is [EditorViewMode.Pages], [IconPack.Preview] otherwise.
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is null.
 * @param tint the tint color of the content. If null then no tint is applied.
 * By default primary color tint is applied when current view mode is [EditorViewMode.Preview], no tint otherwise.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.SetViewMode] event is invoked.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is always [R.string.ly_img_editor_toggle_preview_mode].
 * @return a button that will be displayed in the navigation bar.
 */
@Composable
fun Button.Companion.rememberTogglePreviewMode(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = alwaysVisible,
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = {
        val state by editorContext.state.collectAsState()
        Box(
            modifier = Modifier
                .padding(horizontal = 4.dp)
                .size(40.dp)
                .background(
                    color = if (state.viewMode is EditorViewMode.Preview) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = CircleShape,
                ),
        ) { it() }
    },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = {
        val state by editorContext.state.collectAsState()
        when (state.viewMode) {
            is EditorViewMode.Preview -> IconPack.PreviewToggled
            else -> IconPack.Preview
        }
    },
    text: (@Composable ButtonScope.() -> String)? = null,
    tint: (@Composable ButtonScope.() -> Color)? = {
        val state by editorContext.state.collectAsState()
        when (state.viewMode) {
            is EditorViewMode.Preview -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    },
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        val viewMode = if (editorContext.state.value.viewMode is EditorViewMode.Preview) {
            EditorViewMode.Edit()
        } else {
            EditorViewMode.Preview()
        }
        editorContext.eventHandler.send(EditorEvent.SetViewMode(viewMode))
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = {
        stringResource(R.string.ly_img_editor_toggle_preview_mode)
    },
    `_`: Nothing = nothing,
): Button = remember(
    id = Button.Id.togglePreviewMode,
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
 * The id of the navigation bar button returned by [NavigationBar.Button.Companion.rememberTogglePagesMode].
 */
val Button.Id.Companion.togglePagesMode by unsafeLazy {
    EditorComponentId("ly.img.component.navigationBar.button.togglePagesMode")
}

/**
 * A composable helper function that creates and remembers a [NavigationBar.Custom] that
 * updates editor view mode via [EditorEvent.SetViewMode]: when current view mode is [EditorViewMode.Edit], then [EditorViewMode.Pages]
 * is set and vice versa.
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [NavigationBar] - [NavigationBar.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated only when the parent component scope ([NavigationBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * Default value is always true.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * By default a rounded corner background is applied when current view mode is [EditorViewMode.Pages].
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * By default the value is equivalent to the number of pages in the scene, acquire via [ly.img.engine.SceneApi.getPages] engine API.
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is null.
 * @param tint the tint color of the content. If null then no tint is applied.
 * By default primary color tint is applied when current view mode is [EditorViewMode.Preview], no tint otherwise.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.SetViewMode] event is invoked.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is always [R.string.ly_img_editor_toggle_pages_mode].
 * @return a button that will be displayed in the navigation bar.
 */
@Composable
fun Button.Companion.rememberTogglePagesMode(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = alwaysVisible,
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = {
        val state by editorContext.state.collectAsState()
        Box(
            modifier = Modifier
                .padding(4.dp)
                .height(40.dp)
                .background(
                    color = if (state.viewMode is EditorViewMode.Pages) MaterialTheme.colorScheme.primary else Color.Transparent,
                    shape = RoundedCornerShape(size = 100.dp),
                ),
        ) { it() }
    },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.Pages },
    text: (@Composable ButtonScope.() -> String)? = {
        remember(this) {
            editorContext.engine.scene.getPages().size.toString()
        }
    },
    tint: (@Composable ButtonScope.() -> Color)? = {
        val state by editorContext.state.collectAsState()
        when (state.viewMode) {
            is EditorViewMode.Pages -> MaterialTheme.colorScheme.onPrimary
            else -> MaterialTheme.colorScheme.onSurfaceVariant
        }
    },
    enabled: @Composable ButtonScope.() -> Boolean = { true },
    onClick: ButtonScope.() -> Unit = {
        val viewMode = if (editorContext.state.value.viewMode is EditorViewMode.Pages) {
            EditorViewMode.Edit()
        } else {
            EditorViewMode.Pages()
        }
        editorContext.eventHandler.send(EditorEvent.SetViewMode(viewMode))
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = {
        stringResource(R.string.ly_img_editor_toggle_pages_mode)
    },
    `_`: Nothing = nothing,
): NavigationBar.Custom<ButtonScope> {
    require(text != null || contentDescription != null) {
        "Content description must be provided when invoking NavigationBar.Button.rememberTogglePagesMode with icon only."
    }
    return NavigationBar.Custom.remember(
        id = Button.Id.togglePagesMode,
        scope = scope,
        visible = visible,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        content = {
            decoration(this) {
                val tintColor = tint?.invoke(this) ?: MaterialTheme.colorScheme.onSurfaceVariant
                Button(
                    modifier = Modifier.defaultMinSize(minWidth = 40.dp),
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = tintColor,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = tintColor.copy(alpha = tintColor.alpha * 0.5F),
                    ),
                    onClick = { onClick(this) },
                    enabled = enabled(this),
                    contentPadding = PaddingValues(horizontal = 8.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        vectorIcon?.let {
                            Icon(
                                imageVector = it(this@remember),
                                contentDescription = contentDescription?.invoke(this@remember),
                            )
                        }
                        text?.let {
                            Text(
                                modifier = Modifier.padding(start = 4.dp),
                                text = it(this@remember),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        },
    )
}

/**
 * The id of the navigation bar button returned by [NavigationBar.Button.Companion.rememberPreviousPage].
 */
val Button.Id.Companion.previousPage by unsafeLazy {
    EditorComponentId("ly.img.component.navigationBar.button.previousPage")
}

/**
 * A composable helper function that creates and remembers a [NavigationBar.Custom] that
 * navigates to the previous page via [EditorEvent.Navigation.ToPreviousPage] in [ly.img.editor.PostcardEditor].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [NavigationBar] - [NavigationBar.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated only when the parent component scope ([NavigationBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true if the current view mode is [EditorViewMode.Edit] and either "features/pageCarouselEnabled" engine setting
 * is true or the current page is not the first page of the scene, false otherwise.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.ArrowBack].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_previous].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Navigation.ToPreviousPage] event is invoked.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the navigation bar.
 */
@Composable
fun Button.Companion.rememberPreviousPage(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        val state by editorContext.state.collectAsState()
        state.viewMode is EditorViewMode.Edit &&
            remember(this) {
                editorContext.engine.editor.getSettingBoolean("features/pageCarouselEnabled") ||
                    editorContext.engine.scene.run { getPages().firstOrNull() != getCurrentPage() }
            }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.ArrowBack },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_previous) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Navigation.ToPreviousPage())
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): NavigationBar.Custom<ButtonScope> {
    require(text != null || contentDescription != null) {
        "Content description must be provided when invoking NavigationBar.Button.rememberPostcardNavigateToDesign with icon only."
    }
    return NavigationBar.Custom.remember(
        id = Button.Id.previousPage,
        scope = scope,
        visible = visible,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        content = {
            decoration(this) {
                val tintColor = tint?.invoke(this) ?: MaterialTheme.colorScheme.onSurfaceVariant
                Button(
                    colors =
                        ButtonDefaults.textButtonColors(
                            containerColor = Color.Transparent,
                            contentColor = tintColor,
                            disabledContainerColor = Color.Transparent,
                            disabledContentColor = tintColor.copy(alpha = tintColor.alpha * 0.5F),
                        ),
                    onClick = { onClick(this) },
                    enabled = enabled(this),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        vectorIcon?.let {
                            Icon(
                                imageVector = it(this@remember),
                                contentDescription = contentDescription?.invoke(this@remember),
                            )
                        }
                        text?.let {
                            Text(
                                modifier = Modifier.padding(start = 4.dp),
                                text = it(this@remember),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            }
        },
    )
}

/**
 * The id of the navigation bar button returned by [NavigationBar.Button.Companion.rememberNextPage].
 */
val Button.Id.Companion.nextPage by unsafeLazy {
    EditorComponentId("ly.img.component.navigationBar.button.nextPage")
}

/**
 * A composable helper function that creates and remembers a [NavigationBar.Custom] that
 * navigates to the next page via [EditorEvent.Navigation.ToNextPage] in [ly.img.editor.PostcardEditor].
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
 * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
 * Ideally, scope should be updated when the parent scope (scope of the parent component [NavigationBar] - [NavigationBar.Scope]) is updated
 * and when you want to observe changes from the [Engine].
 * By default the scope is updated only when the parent component scope ([NavigationBar.scope], accessed via [LocalEditorScope]) is updated.
 * @param visible whether the button should be visible.
 * By default the value is true if the current view mode is [EditorViewMode.Edit] and either "features/pageCarouselEnabled" engine setting
 * is true or the current page is not the last page of the scene, false otherwise.
 * @param enterTransition transition of the button when it enters the parent composable.
 * Default value is always no enter transition.
 * @param exitTransition transition of the button when it exits the parent composable.
 * Default value is always no exit transition.
 * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * Default value is always no decoration.
 * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
 * Default value is always [IconPack.ArrowForward].
 * @param text the text content of the button as a string. If null then text is not rendered.
 * Default value is always [R.string.ly_img_editor_next].
 * @param tint the tint color of the content. If null then no tint is applied.
 * Default value is null.
 * @param enabled whether the button is enabled.
 * Default value is always true.
 * @param onClick the callback that is invoked when the button is clicked.
 * By default [EditorEvent.Navigation.ToNextPage] event is invoked.
 * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
 * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
 * Default value is null.
 * @return a button that will be displayed in the navigation bar.
 */
@Composable
fun Button.Companion.rememberNextPage(
    scope: ButtonScope = LocalEditorScope.current.run {
        remember(this) { ButtonScope(parentScope = this) }
    },
    visible: @Composable ButtonScope.() -> Boolean = {
        val state by editorContext.state.collectAsState()
        state.viewMode is EditorViewMode.Edit &&
            remember(this) {
                editorContext.engine.editor.getSettingBoolean("features/pageCarouselEnabled") ||
                    editorContext.engine.scene.run { getPages().lastOrNull() != getCurrentPage() }
            }
    },
    enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
    exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
    decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
    vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = { IconPack.ArrowForward },
    text: (@Composable ButtonScope.() -> String)? = { stringResource(R.string.ly_img_editor_next) },
    tint: (@Composable ButtonScope.() -> Color)? = null,
    enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
    onClick: ButtonScope.() -> Unit = {
        editorContext.eventHandler.send(EditorEvent.Navigation.ToNextPage())
    },
    contentDescription: (@Composable ButtonScope.() -> String)? = null,
    `_`: Nothing = nothing,
): NavigationBar.Custom<ButtonScope> {
    require(text != null || contentDescription != null) {
        "Content description must be provided when calling NavigationBar.Button.rememberPostcardNavigateToWrite with icon only."
    }
    return NavigationBar.Custom.remember(
        id = Button.Id.nextPage,
        scope = scope,
        visible = visible,
        enterTransition = enterTransition,
        exitTransition = exitTransition,
        content = {
            decoration(this) {
                val tintColor = tint?.invoke(this) ?: MaterialTheme.colorScheme.onSurfaceVariant
                Button(
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = Color.Transparent,
                        contentColor = tintColor,
                        disabledContainerColor = Color.Transparent,
                        disabledContentColor = tintColor.copy(alpha = tintColor.alpha * 0.5F),
                    ),
                    onClick = { onClick(this) },
                    enabled = enabled(this),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        text?.let {
                            Text(
                                text = it(this@remember),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        vectorIcon?.let {
                            Icon(
                                modifier = Modifier.padding(start = 4.dp),
                                imageVector = it(this@remember),
                                contentDescription = contentDescription?.invoke(this@remember),
                            )
                        }
                    }
                }
            }
        },
    )
}
