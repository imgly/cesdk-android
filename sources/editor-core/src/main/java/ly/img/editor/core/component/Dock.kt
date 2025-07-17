package ly.img.editor.core.component

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ly.img.editor.core.EditorContext
import ly.img.editor.core.EditorScope
import ly.img.editor.core.LocalEditorScope
import ly.img.editor.core.component.Dock.Companion.remember
import ly.img.editor.core.component.Dock.Item
import ly.img.editor.core.component.Dock.Scope
import ly.img.editor.core.component.EditorComponent.ListBuilder.Companion.modify
import ly.img.editor.core.component.data.Nothing
import ly.img.editor.core.component.data.nothing
import ly.img.editor.core.theme.surface1
import ly.img.editor.core.ui.IconTextButton
import ly.img.engine.Engine

/**
 * A component for rendering the dock at the bottom of the editor.
 * Use [remember] composable function in the companion object to create an instance of this class, or use
 * [Dock.Companion.rememberForDesign], [Dock.Companion.rememberForPhoto] and [Dock.Companion.rememberForVideo] helpers that
 * construct solution specific docks.
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas
 * for granular recompositions over updating the scope, since scope change triggers full recomposition of the dock.
 * Also prefer updating individual [Item]s over updating the whole [Dock].
 * Ideally, scope should be updated when the parent scope (scope of the parent component) is updated and when you want to
 * observe changes from the [Engine].
 * @param visible whether the dock should be visible based on the [Engine]'s current state.
 * @param enterTransition transition of the dock when it enters the parent composable.
 * @param exitTransition transition of the dock when it exits the parent composable.
 * @param decoration decoration of the dock. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * @param listBuilder a builder that builds the list of [Dock.Item]s that should be part of the dock.
 * Note that adding items to the list does not mean displaying. The items will be displayed if [Dock.Item.visible] is true for them.
 * @param horizontalArrangement the horizontal arrangement that should be used to render the items in the dock horizontally.
 * Note that the value will be ignored in case [listBuilder] contains aligned items. Check [EditorComponent.ListBuilder.Scope.New.aligned] for more
 * details on how to configure arrangement of aligned items.
 * Default value is [Arrangement.SpaceEvenly].
 * @param itemDecoration decoration of the items in the dock. Useful when you want to add custom background, foreground, shadow,
 * paddings etc to the items. Prefer using this decoration when you want to apply the same decoration to all the items, otherwise
 * set decoration to individual items.
 */
@Stable
class Dock private constructor(
    override val scope: Scope,
    override val visible: @Composable Scope.() -> Boolean,
    override val enterTransition: @Composable Scope.() -> EnterTransition,
    override val exitTransition: @Composable Scope.() -> ExitTransition,
    override val decoration: @Composable Scope.(content: @Composable () -> Unit) -> Unit,
    val listBuilder: HorizontalListBuilder<Item<*>>,
    val horizontalArrangement: @Composable Scope.() -> Arrangement.Horizontal,
    val itemDecoration: @Composable Scope.(content: @Composable () -> Unit) -> Unit,
    private val `_`: Nothing,
) : EditorComponent<Scope>() {
    override val id: EditorComponentId = EditorComponentId("ly.img.component.dock")

    @Stable
    class ListBuilder {
        companion object {
            /**
             * A composable function that creates and remembers a [ListBuilder] instance.
             *
             * @param block the building block of [ListBuilder].
             * @return a new [ListBuilder] instance.
             */
            @Composable
            fun remember(block: @DisallowComposableCalls HorizontalListBuilderScope<Item<*>>.() -> Unit): HorizontalListBuilder<Item<*>> =
                HorizontalListBuilder.remember(block)
        }
    }

    @Composable
    override fun Scope.Content(animatedVisibilityScope: AnimatedVisibilityScope?) {
        listBuilder.scope.result.forEach { (alignment, data) ->
            // Main list
            if (alignment == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = horizontalArrangement(),
                ) {
                    Items(data)
                }
            } else {
                // List with alignments
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.align(alignment),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = data.arrangement ?: Arrangement.Start,
                    ) {
                        Items(data)
                    }
                }
            }
        }
    }

    @Composable
    private fun Scope.Items(data: EditorComponent.ListBuilder.AlignmentData<Item<*>, *>) {
        data.items.forEach {
            itemDecoration {
                EditorComponent(component = it)
            }
        }
    }

    /**
     * The scope of the [Dock] component.
     *
     * @param parentScope the scope of the parent component.
     */
    @Stable
    open class Scope(
        parentScope: EditorScope,
    ) : EditorScope() {
        override val impl: EditorContext = parentScope.editorContext
    }

    override fun toString(): String = "$`_`Dock(id=$id)"

    /**
     * The scope of the [Item] component.
     *
     * @param parentScope the scope of the parent component.
     */
    @Stable
    open class ItemScope(
        parentScope: EditorScope,
    ) : EditorScope() {
        override val impl: EditorContext = parentScope.editorContext
    }

    /**
     * A component that represents an item that can be rendered in the dock.
     * The only limitation is that the component must have a maximum height of 64.dp.
     */
    abstract class Item<Scope : ItemScope> : EditorComponent<Scope>() {
        /**
         * The content of the item in the dock.
         */
        @Composable
        protected abstract fun Scope.ItemContent()

        @Composable
        final override fun Scope.Content(animatedVisibilityScope: AnimatedVisibilityScope?) {
            Box(modifier = Modifier.sizeIn(maxHeight = 64.dp)) {
                ItemContent()
            }
        }
    }

    /**
     * A component that represents a custom content in the [Dock].
     *
     * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
     * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
     * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
     * granular recompositions over updating the scope, since scope change triggers full recomposition of the custom item
     * Ideally, scope should be updated when the parent scope (scope of the parent component [Dock] - [Dock.Scope]) is updated
     * and when you want to observe changes from the [Engine].
     * @param visible whether the custom item should be visible.
     * @param enterTransition transition of the custom item when it enters the parent composable.
     * @param exitTransition transition of the custom item when it exits the parent composable.
     * @param content the content of the component.
     */
    @Stable
    class Custom<Scope : ItemScope> private constructor(
        override val id: EditorComponentId,
        override val scope: Scope,
        override val visible: @Composable Scope.() -> Boolean,
        override val enterTransition: @Composable Scope.() -> EnterTransition,
        override val exitTransition: @Composable Scope.() -> ExitTransition,
        val content: @Composable Scope.() -> Unit,
    ) : Item<Scope>() {
        override val decoration: @Composable Scope.(@Composable () -> Unit) -> Unit = { it() }

        @Composable
        override fun Scope.ItemContent() {
            content()
        }

        override fun toString(): String = "Dock.Custom(id=$id)"

        companion object {
            /**
             * A composable function that creates and remembers a [Dock.Custom] instance.
             *
             * @param id the id of the custom view.
             * Note that it is highly recommended that every unique [EditorComponent] has a unique id.
             * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
             * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
             * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
             * granular recompositions over updating the scope, since scope change triggers full recomposition of the custom item.
             * Ideally, scope should be updated when the parent scope (scope of the parent component [Dock] - [Dock.Scope]) is updated
             * and when you want to observe changes from the [Engine].
             * @param visible whether the custom item should be visible.
             * Default value is always true.
             * @param enterTransition transition of the custom item when it enters the parent composable.
             * Default value is always no enter transition.
             * @param exitTransition transition of the custom item when it exits the parent composable.
             * Default value is always no exit transition.
             * @param content the content of the component.
             * @return a custom item that will be displayed in the dock.
             */
            @Composable
            fun <Scope : ItemScope> remember(
                id: EditorComponentId,
                scope: Scope,
                visible: @Composable Scope.() -> Boolean = alwaysVisible,
                enterTransition: @Composable Scope.() -> EnterTransition = noneEnterTransition,
                exitTransition: @Composable Scope.() -> ExitTransition = noneExitTransition,
                content: @Composable Scope.() -> Unit,
            ) = remember(scope, visible, enterTransition, exitTransition, content) {
                Custom(
                    id = id,
                    scope = scope,
                    visible = visible,
                    enterTransition = enterTransition,
                    exitTransition = exitTransition,
                    content = content,
                )
            }
        }
    }

    /**
     * The scope of the [Button] component.
     *
     * @param parentScope the scope of the parent component.
     */
    @Stable
    open class ButtonScope(
        parentScope: EditorScope,
    ) : ItemScope(parentScope)

    @Stable
    @Deprecated("Class is deprecated and unused.")
    open class ReorderButtonScope(
        parentScope: EditorScope,
        private val visible: Boolean,
    ) : ButtonScope(parentScope) {
        val EditorContext.visible: Boolean
            get() = this@ReorderButtonScope.visible
    }

    /**
     * A component that represents a button in the [Dock].
     *
     * @param id the id of the button.
     * Note that it is highly recommended that every unique [EditorComponent] has a unique id.
     * @param onClick the callback that is invoked when the button is clicked.
     * @param icon the icon content of the button. If null, it will not be rendered.
     * @param text the text content of the button. If null, it will not be rendered.
     * @param tint the tint color of the content. If null then no tint is applied.
     * @param enabled whether the button is enabled.
     * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters.
     * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
     * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for
     * granular recompositions over updating the scope, since scope change triggers full recomposition of the button.
     * Ideally, scope should be updated when the parent scope (scope of the parent component [Dock] - [Dock.Scope]) is updated
     * and when you want to observe changes from the [Engine].
     * @param visible whether the button should be visible.
     * @param enterTransition transition of the button when it enters the parent composable.
     * @param exitTransition transition of the button when it exits the parent composable.
     * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
     */
    @Stable
    class Button private constructor(
        override val id: EditorComponentId,
        override val scope: ButtonScope,
        override val visible: @Composable ButtonScope.() -> Boolean,
        override val enterTransition: @Composable ButtonScope.() -> EnterTransition,
        override val exitTransition: @Composable ButtonScope.() -> ExitTransition,
        override val decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit,
        val onClick: ButtonScope.() -> Unit,
        val icon: (@Composable ButtonScope.() -> Unit)?,
        val text: (@Composable ButtonScope.() -> Unit)?,
        val tint: (@Composable ButtonScope.() -> Color)?,
        val enabled: @Composable ButtonScope.() -> Boolean,
        private val `_`: Nothing,
    ) : Item<ButtonScope>() {
        @Composable
        override fun ButtonScope.ItemContent() {
            IconTextButton(
                modifier = Modifier
                    .widthIn(min = 64.dp)
                    .height(64.dp),
                onClick = { onClick() },
                enabled = enabled(),
                icon = icon?.let { { it() } },
                text = text?.let { { it() } },
                tint = tint?.invoke(this) ?: MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        override fun toString(): String = "$`_`Dock.Button(id=$id)"

        class Id {
            companion object
        }

        companion object {
            /**
             * Predicate to be used when the [EditorComponent] is always enabled.
             */
            val alwaysEnabled: @Composable ButtonScope.() -> Boolean = { true }

            /**
             * A composable function that creates and remembers a [Dock.Button] instance.
             *
             * @param id the id of the button.
             * Note that it is highly recommended that every unique [EditorComponent] has a unique id.
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
             * @param onClick the callback that is invoked when the button is clicked.
             * @param icon the icon content of the button. If null, it will not be rendered.
             * Default value is null.
             * @param text the text content of the button. If null, it will not be rendered.
             * Default value is null.
             * @param tint the tint color of the content. If null then no tint is applied.
             * Default value is null.
             * @param enabled whether the button is enabled.
             * Default value is always true.
             * @return a button that will be displayed in the dock.
             */
            @Composable
            fun remember(
                id: EditorComponentId,
                scope: ButtonScope = LocalEditorScope.current.run {
                    remember(this) { ButtonScope(parentScope = this) }
                },
                visible: @Composable ButtonScope.() -> Boolean = alwaysVisible,
                enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
                exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
                decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
                onClick: ButtonScope.() -> Unit,
                icon: (@Composable ButtonScope.() -> Unit)? = null,
                text: (@Composable ButtonScope.() -> Unit)? = null,
                tint: (@Composable ButtonScope.() -> Color)? = null,
                enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
                `_`: Nothing = nothing,
            ): Button = remember(scope, visible, enterTransition, exitTransition, decoration, onClick, icon, text, enabled) {
                Button(
                    id = id,
                    scope = scope,
                    visible = visible,
                    enterTransition = enterTransition,
                    exitTransition = exitTransition,
                    decoration = decoration,
                    onClick = onClick,
                    icon = icon,
                    text = text,
                    tint = tint,
                    enabled = enabled,
                    `_` = `_`,
                )
            }

            /**
             * A composable helper function that creates and remembers a [Dock.Button] instance where [icon] composable is
             * provided via [ImageVector] and [text] composable via [String].
             *
             * @param id the id of the button.
             * Note that it is highly recommended that every unique [EditorComponent] has a unique id.
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
             * @param onClick the callback that is invoked when the button is clicked.
             * @param vectorIcon the icon content of the button as a vector. If null then icon is not rendered.
             * Default value is null.
             * @param text the text content of the button as a string. If null then text is not rendered.
             * Default value is null.
             * @param tint the tint color of the content. If null then no tint is applied.
             * Default value is null.
             * @param enabled whether the button is enabled.
             * Default value is always true.
             * @param contentDescription the content description of the [vectorIcon] that is used by accessibility services to describe what
             * this icon represents. Having both [text] and [contentDescription] as null will cause a crash.
             * Default value is null.
             * @return a button that will be displayed in the dock.
             */
            @Composable
            fun remember(
                id: EditorComponentId,
                scope: ButtonScope = LocalEditorScope.current.run {
                    remember(this) { ButtonScope(parentScope = this) }
                },
                visible: @Composable ButtonScope.() -> Boolean = alwaysVisible,
                enterTransition: @Composable ButtonScope.() -> EnterTransition = noneEnterTransition,
                exitTransition: @Composable ButtonScope.() -> ExitTransition = noneExitTransition,
                decoration: @Composable ButtonScope.(@Composable () -> Unit) -> Unit = { it() },
                onClick: ButtonScope.() -> Unit,
                vectorIcon: (@Composable ButtonScope.() -> ImageVector)? = null,
                text: (@Composable ButtonScope.() -> String)? = null,
                tint: (@Composable ButtonScope.() -> Color)? = null,
                enabled: @Composable ButtonScope.() -> Boolean = alwaysEnabled,
                contentDescription: (@Composable ButtonScope.() -> String)? = null,
                `_`: Nothing = nothing,
            ): Button {
                require(text != null || contentDescription != null) {
                    "Content description must be provided when creating a Dock.Button with icon only."
                }
                return remember(
                    id = id,
                    scope = scope,
                    visible = visible,
                    enterTransition = enterTransition,
                    exitTransition = exitTransition,
                    decoration = decoration,
                    onClick = onClick,
                    icon = vectorIcon?.let {
                        {
                            Icon(
                                imageVector = vectorIcon(this),
                                contentDescription = contentDescription?.invoke(this),
                            )
                        }
                    },
                    text = text?.let {
                        {
                            Text(
                                text = text(this),
                                style = MaterialTheme.typography.labelSmall,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    },
                    tint = tint,
                    enabled = enabled,
                    `_` = `_`,
                )
            }
        }
    }

    companion object {
        /**
         * The default decoration of the dock.
         * Sets a background color and applies paddings to the dock.
         */
        @Deprecated("Use DefaultDecoration function instead.", ReplaceWith("{ DefaultDecoration() }"))
        val defaultDecoration: @Composable Scope.(@Composable () -> Unit) -> Unit = {
            DefaultDecoration { it() }
        }

        /**
         * The default decoration of the dock.
         * Sets a background color and applies paddings to the dock by adding a containing box.
         *
         * @param background the background of the containing box.
         * @param paddingValues the padding values of the containing box.
         * @param content the content of the dock.
         */
        @Composable
        inline fun Scope.DefaultDecoration(
            background: Color = MaterialTheme.colorScheme.surface1.copy(alpha = 0.95f),
            paddingValues: PaddingValues = PaddingValues(vertical = 10.dp),
            content: @Composable () -> Unit,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(background)
                    .padding(paddingValues),
            ) {
                content()
            }
        }

        /**
         * A composable function that creates and remembers a [Dock] instance.
         * Consider using [Dock.Companion.rememberForDesign], [Dock.Companion.rememberForPhoto] and [Dock.Companion.rememberForVideo]
         * helpers that construct solution specific docks instead.
         *
         * For demonstration purposes, let's assume the following items are registered in the dock:
         *
         * - Dock.Button.rememberElementsLibrary
         * - Dock.Button.rememberSystemGallery
         * - Dock.Button.rememberSystemCamera
         * - Dock.Button.rememberImagesLibrary
         * - Dock.Button.rememberTextLibrary
         * - Dock.Button.rememberShapesLibrary
         * - Dock.Button.rememberStickersLibrary
         *
         * For example, if you do not want to touch the default order, but rather add additional items and replace/hide default items, then
         * it is more convenient to call [EditorComponent.ListBuilder.modify] on an existing builder, i.e. [Dock.ListBuilder.Companion.rememberForDesign]:
         *
         * dock = {
         *     Dock.rememberForDesign(
         *         listBuilder = Dock.ListBuilder.rememberForDesign().modify {
         *             addLast {
         *                 Dock.Button.remember(
         *                     id = EditorComponentId("my.package.dock.button.last"),
         *                     vectorIcon = { IconPack.Music },
         *                     text = { "Last Button" },
         *                     onClick = {}
         *                 )
         *             }
         *             addFirst {
         *                 Dock.Button.remember(
         *                     id = EditorComponentId("my.package.dock.button.first"),
         *                     vectorIcon = { IconPack.Music },
         *                     text = { "First Button" },
         *                     onClick = {}
         *                 )
         *             }
         *             addAfter(id = Dock.Button.Id.systemGallery) {
         *                 Dock.Button.remember(
         *                     id = EditorComponentId("my.package.dock.button.afterSystemGallery"),
         *                     vectorIcon = { IconPack.Music },
         *                     text = { "After System Gallery" },
         *                     onClick = {}
         *                 )
         *             }
         *             addBefore(id = Dock.Button.Id.systemCamera) {
         *                 Dock.Button.remember(
         *                     id = EditorComponentId("my.package.dock.button.beforeSystemCamera"),
         *                     vectorIcon = { IconPack.Music },
         *                     text = { "Before System Camera" },
         *                     onClick = {}
         *                 )
         *             }
         *             replace(id = Dock.Button.Id.textLibrary) {
         *                  Dock.Button.remember(
         *                      id = EditorComponentId("my.package.dock.button.replacedTextLibrary"),
         *                      vectorIcon = null,
         *                      text = { "Replaced Text Library" },
         *                      onClick = {},
         *                  )
         *             }
         *             remove(id = Dock.Button.Id.shapesLibrary)
         *         }
         *     )
         * }
         *
         * However, if you want to make more complex customizations that includes touching the default order, it is more convenient to
         * go fully custom via [Dock.ListBuilder.remember] with [listBuilder] looking like this:
         *
         * For example, if you want to
         *  - 1. replace the icon of Dock.Button.rememberElementsLibrary,
         *  - 2. drop Dock.Button.rememberSystemCamera and Dock.Button.rememberShapesLibrary,
         *  - 3. swap Dock.Button.rememberTextLibrary and Dock.Button.rememberStickersLibrary,
         *  - 4. add one custom button to the front and another in the middle,
         *  - 5. update first custom button text when second custom button is clicked with an incremented value,
         *  - 6. show Dock.Button.rememberStickersLibrary when the counter is even,
         *  - 7. force update all items on any engine event (that will be obvious from first custom button random icon).
         * you should invoke [ly.img.editor.core.component.Dock.Companion.rememberForDesign] with [listBuilder] looking like this:
         *
         * dock = {
         *     var counter by remember { mutableStateOf(0) }
         *     val dockScope by remember(this) {
         *          editorContext.engine.event.subscribe()
         *              .map { Dock.Scope(parentScope = this) }
         *     }.collectAsState(initial = remember { Dock.Scope(parentScope = this) })
         *     Dock.rememberForDesign(
         *         scope = dockScope,
         *         listBuilder = Dock.ListBuilder.remember {
         *             add {
         *                 Dock.Button.remember(
         *                     id = EditorComponentId("my.package.dock.button.custom1"),
         *                     vectorIcon = { listOf(IconPack.Music, IconPack.PlayBox).random() },
         *                     text = { "Custom1 $counter" },
         *                     onClick = {}
         *                 )
         *             }
         *             add {
         *                 Dock.Button.rememberElementsLibrary(
         *                     vectorIcon = { IconPack.Music }
         *                 )
         *             }
         *             add { Dock.Button.rememberSystemGallery() }
         *             add {
         *                 Dock.Button.remember(
         *                     id = EditorComponentId("my.package.dock.button.custom2"),
         *                     vectorIcon = { IconPack.PlayBox },
         *                     text = { "Custom2" },
         *                     onClick = { counter++ }
         *                 )
         *             }
         *             add { Dock.Button.rememberImagesLibrary() }
         *             add {
         *                 Dock.Button.rememberStickersLibrary(
         *                     visible = { counter % 2 == 0 }
         *                 )
         *             }
         *             add { Dock.Button.rememberTextLibrary() }
         *         }
         *     )
         * }
         *
         *
         * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters below.
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
         * @param decoration decoration of the dock. Useful when you want to add custom background, foreground, shadow, paddings etc.
         * Default value is [DefaultDecoration].
         * @param listBuilder a builder that builds the list of [Dock.Item]s that should be part of the dock.
         * Note that adding items to the list does not mean displaying. The items will be displayed if [Dock.Item.visible] is true for them.
         * By default listBuilder does not add anything to the dock.
         * @param horizontalArrangement the horizontal arrangement that should be used to render the items in the dock horizontally.
         * Note that the value will be ignored in case [listBuilder] contains aligned items. Check [EditorComponent.ListBuilder.Scope.New.aligned] for more
         * details on how to configure arrangement of aligned items.
         * Default value is [Arrangement.SpaceEvenly].
         * @param itemDecoration decoration of the items in the dock. Useful when you want to add custom background, foreground, shadow,
         * paddings etc to the items. Prefer using this decoration when you want to apply the same decoration to all the items, otherwise
         * set decoration to individual items.
         * Default value is always no decoration.
         * @return a dock that will be displayed when launching an editor.
         */
        @Composable
        fun remember(
            scope: Scope = LocalEditorScope.current.run {
                remember(this) { Scope(parentScope = this) }
            },
            visible: @Composable Scope.() -> Boolean = alwaysVisible,
            enterTransition: @Composable Scope.() -> EnterTransition = noneEnterTransition,
            exitTransition: @Composable Scope.() -> ExitTransition = noneExitTransition,
            decoration: @Composable Scope.(@Composable () -> Unit) -> Unit = { DefaultDecoration { it() } },
            listBuilder: HorizontalListBuilder<Item<*>> = ListBuilder.remember {},
            horizontalArrangement: @Composable Scope.() -> Arrangement.Horizontal = { Arrangement.SpaceEvenly },
            itemDecoration: @Composable Scope.(content: @Composable () -> Unit) -> Unit = { it() },
            `_`: Nothing = nothing,
        ): Dock = remember(
            scope,
            visible,
            enterTransition,
            exitTransition,
            listBuilder,
            horizontalArrangement,
            decoration,
            itemDecoration,
        ) {
            Dock(
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
        }
    }
}
