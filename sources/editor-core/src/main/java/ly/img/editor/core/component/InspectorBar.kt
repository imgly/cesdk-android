package ly.img.editor.core.component

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import ly.img.editor.core.EditorContext
import ly.img.editor.core.EditorScope
import ly.img.editor.core.LocalEditorScope
import ly.img.editor.core.R
import ly.img.editor.core.component.EditorComponent.ListBuilder.Companion.modify
import ly.img.editor.core.component.InspectorBar.Companion.remember
import ly.img.editor.core.component.InspectorBar.Item
import ly.img.editor.core.component.data.EditorIcon
import ly.img.editor.core.component.data.Nothing
import ly.img.editor.core.component.data.Selection
import ly.img.editor.core.component.data.nothing
import ly.img.editor.core.iconpack.Close
import ly.img.editor.core.iconpack.IconPack
import ly.img.editor.core.ui.IconTextButton
import ly.img.engine.DesignBlock
import ly.img.engine.Engine

/**
 * A component for rendering the inspector bar at the bottom of the editor.
 * Use [remember] from the companion object to create an instance of this class.
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters below.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas
 * for granular recompositions over updating the scope, since scope change triggers full recomposition of the inspector bar.
 * Also prefer updating individual [Item]s over updating the whole [InspectorBar].
 * Ideally, scope should be updated when the parent scope (scope of the parent component) is updated and when you want to
 * observe changes from the [Engine].
 * @param visible whether the inspector bar should be visible based on the [Engine]'s current state.
 * @param enterTransition transition of the inspector bar when it enters the parent composable.
 * @param exitTransition transition of the inspector bar when it exits the parent composable.
 * @param decoration decoration of the inspector bar. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * @param listBuilder the list builder of this inspector bar.
 * @param horizontalArrangement the horizontal arrangement that should be used to render the items in the inspector bar horizontally.
 * Note that the value will be ignored in case [listBuilder] contains aligned items. Check [EditorComponent.ListBuilder.Scope.New.aligned] for more
 * details on how to configure arrangement of aligned items.
 * @param itemsRowEnterTransition separate transition of the [Item]s row only when [enterTransition] is running.
 * @param itemsRowExitTransition separate transition of the [Item]s row only when [exitTransition] is running.
 * @param itemDecoration decoration of the items in the inspector bar. Useful when you want to add custom background, foreground, shadow,
 * paddings etc to the items. Prefer using this decoration when you want to apply the same decoration to all the items, otherwise
 * set decoration to individual items.
 */
@Stable
class InspectorBar private constructor(
    override val scope: Scope,
    override val visible: @Composable Scope.() -> Boolean,
    override val enterTransition: @Composable Scope.() -> EnterTransition,
    override val exitTransition: @Composable Scope.() -> ExitTransition,
    override val decoration: @Composable Scope.(content: @Composable () -> Unit) -> Unit,
    val listBuilder: HorizontalListBuilder<Item<*>>,
    val horizontalArrangement: @Composable Scope.() -> Arrangement.Horizontal,
    val itemsRowEnterTransition: @Composable Scope.() -> EnterTransition,
    val itemsRowExitTransition: @Composable Scope.() -> ExitTransition,
    val itemDecoration: @Composable Scope.(content: @Composable () -> Unit) -> Unit,
    private val `_`: Nothing = nothing,
) : EditorComponent<InspectorBar.Scope>() {
    override val id: EditorComponentId = EditorComponentId("ly.img.component.inspectorBar")

    @Stable
    class ListBuilder {
        companion object {
            /**
             * A composable function that creates and remembers a [ListBuilder] instance with default items.
             *
             * @return a new [ListBuilder] instance.
             */
            @Composable
            fun remember(): HorizontalListBuilder<Item<*>> = HorizontalListBuilder.remember {
                add { Button.rememberReplace() } // Video, Image, Sticker, Audio
                add { Button.rememberEditText() } // Text
                add { Button.rememberFormatText() } // Text
                add { Button.rememberFillStroke() } // Page, Video, Image, Shape, Text
                add { Button.rememberVolume() } // Video, Audio
                add { Button.rememberCrop() } // Video, Image
                add { Button.rememberAdjustments() } // Video, Image
                add { Button.rememberFilter() } // Video, Image
                add { Button.rememberEffect() } // Video, Image
                add { Button.rememberBlur() } // Video, Image
                add { Button.rememberShape() } // Video, Image, Shape
                add { Button.rememberSelectGroup() } // Video, Image, Sticker, Shape, Text
                add { Button.rememberEnterGroup() } // Group
                add { Button.rememberLayer() } // Video, Image, Sticker, Shape, Text
                add { Button.rememberSplit() } // Video, Image, Sticker, Shape, Text, Audio
                add { Button.rememberMoveAsClip() } // Video, Image, Sticker, Shape, Text
                add { Button.rememberMoveAsOverlay() } // Video, Image, Sticker, Shape, Text
                add { Button.rememberReorder() } // Video, Image, Sticker, Shape, Text
                add { Button.rememberDuplicate() } // Video, Image, Sticker, Shape, Text
                add { Button.rememberDelete() } // Video, Image, Sticker, Shape, Text
            }

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

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    override fun Scope.Content(animatedVisibilityScope: AnimatedVisibilityScope?) {
        val animationModifier = animatedVisibilityScope?.run {
            Modifier.animateEnterExit(
                enter = itemsRowEnterTransition(),
                exit = itemsRowExitTransition(),
            )
        } ?: Modifier
        listBuilder.scope.result.forEach { (alignment, data) ->
            // Main list
            if (alignment == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .then(animationModifier),
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
     * The scope of the [InspectorBar] component.
     *
     * @param parentScope the scope of the parent component.
     * @param selection current selection in the editor.
     */
    @Stable
    open class Scope(
        parentScope: EditorScope,
        private val selection: Selection?,
    ) : EditorScope() {
        override val impl: EditorContext = parentScope.editorContext

        /**
         * Current selection in the editor.
         */
        val EditorContext.safeSelection: Selection?
            get() = this@Scope.selection

        /**
         * Current selection in the editor.
         * Note that this is an unsafe call. Consider using [safeSelection] to get the nullable value.
         */
        val EditorContext.selection: Selection
            get() = requireNotNull(this@Scope.selection)
    }

    override fun toString(): String = "$`_`InspectorBar(id=$id)"

    /**
     * The scope of the [Item] component.
     */
    @Stable
    open class ItemScope(
        parentScope: EditorScope,
    ) : EditorScope() {
        override val impl: EditorContext = parentScope.editorContext

        private val _selection by lazy {
            (parentScope as Scope).run {
                editorContext.safeSelection
            }
        }

        /**
         * Current selection of the editor.
         */
        val EditorContext.safeSelection: Selection?
            get() = _selection

        /**
         * Current selection of the editor.
         * Note that this is an unsafe call. Consider using [safeSelection] to get the nullable value.
         */
        val EditorContext.selection: Selection
            get() = requireNotNull(_selection)
    }

    /**
     * A component that represents an item that can be rendered in the inspector bar.
     * The only limitation is that the component must have a maximum height of 64.dp.
     */
    abstract class Item<Scope : ItemScope> : EditorComponent<Scope>() {
        /**
         * The content of the item in the inspector bar.
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
     * A component that represents a custom content in the [InspectorBar].
     *
     * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
     * signature @Composable Scope.() -> {}.
     * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
     * Consider using [ItemScope] as the scope which will be updated when the parent component scope
     * ([InspectorBar.scope], accessed via [LocalEditorScope]) is updated:
     *     scope = LocalEditorScope.current.run {
     *         remember(this) { ItemScope(parentScope = this) }
     *     }
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

        override fun toString(): String = "InspectorBar.Custom(id=$id)"

        companion object {
            /**
             * A composable function that creates and remembers a [InspectorBar.Custom] instance.
             *
             * @param id the id of the custom view.
             * Note that it is highly recommended that every unique [EditorComponent] has a unique id.
             * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
             * signature @Composable Scope.() -> {}.
             * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
             * @param visible whether the custom item should be visible.
             * Default value is always true.
             * @param enterTransition transition of the custom item when it enters the parent composable.
             * Default value is always no enter transition.
             * @param exitTransition transition of the custom item when it exits the parent composable.
             * Default value is always no exit transition.
             * @param content the content of the component.
             * @return a custom item that will be displayed in the inspector bar.
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

    /**
     * The scope of the [InspectorBar.Button.Companion.rememberFillStroke] button in the inspector bar.
     *
     * @param parentScope the scope of the parent component.
     * @param fillStrokeIcon the icon state of the fill stroke button.
     */
    @Stable
    open class FillStrokeButtonScope(
        parentScope: EditorScope,
        private val fillStrokeIcon: EditorIcon.FillStroke?,
    ) : ButtonScope(parentScope) {
        /**
         * The icon state of the fill stroke button. Used in the [Button.Companion.rememberFillStroke]
         * button implementation.
         */
        val EditorContext.fillStrokeIcon: EditorIcon.FillStroke
            get() = requireNotNull(this@FillStrokeButtonScope.fillStrokeIcon)
    }

    /**
     * A component that represents a button in the [InspectorBar].
     *
     * @param id the id of the button.
     * Note that it is highly recommended that every unique [EditorComponent] has a unique id.
     * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
     * signature @Composable Scope.() -> {}.
     * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
     * @param visible whether the button should be visible.
     * @param enterTransition transition of the button when it enters the parent composable.
     * @param exitTransition transition of the button when it exits the parent composable.
     * @param decoration decoration of the button. Useful when you want to add custom background, foreground, shadow, paddings etc.
     * @param onClick the callback that is invoked when the button is clicked.
     * @param icon the icon content of the button. If null, it will not be rendered.
     * @param text the text content of the button. If null, it will not be rendered.
     * @param tint the tint color of the content. If null then no tint is applied.
     * @param enabled whether the button is enabled.
     */
    @Stable
    class Button private constructor(
        override val id: EditorComponentId,
        override val scope: ButtonScope,
        override val visible: @Composable ButtonScope.() -> Boolean,
        override val enterTransition: @Composable ButtonScope.() -> EnterTransition,
        override val exitTransition: @Composable ButtonScope.() -> ExitTransition,
        override val decoration: @Composable ButtonScope.(content: @Composable () -> Unit) -> Unit,
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

        override fun toString(): String = "$`_`InspectorBar.Button(id=$id)"

        class Id {
            companion object
        }

        companion object {
            /**
             * Predicate to be used when the [EditorComponent] is always enabled.
             */
            val alwaysEnabled: @Composable ButtonScope.() -> Boolean = { true }

            /**
             * A composable function that creates and remembers an [InspectorBar.Button] instance.
             *
             * @param id the id of the button.
             * Note that it is highly recommended that every unique [EditorComponent] has a unique id.
             * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
             * signature @Composable Scope.() -> {}.
             * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
             * By default it is updated only when the parent scope (accessed via [LocalEditorScope]) is updated.
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
             * @return a button that will be displayed in the inspector bar.
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
            ): Button = remember(scope, visible, enterTransition, exitTransition, onClick, icon, text, enabled) {
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
             * A composable helper function that creates and remembers an [InspectorBar.Button] instance where [icon] composable is
             * provided via [ImageVector] and [text] composable via [String].
             *
             * @param id the id of the button.
             * Note that it is highly recommended that every unique [EditorComponent] has a unique id.
             * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
             * signature @Composable Scope.() -> {}.
             * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
             * By default it is updated only when the parent scope (accessed via [LocalEditorScope]) is updated.
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
             * @return a button that will be displayed in the inspector bar.
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
                    "Content description must be provided when creating an InspectorBar.Button with icon only."
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
         * The default scope of the inspector bar. The value is updated when:
         * 1. Parent scope is updated.
         * 2. Selection is updated.
         */
        @OptIn(ExperimentalCoroutinesApi::class)
        val defaultScope: Scope
            @Composable
            get() = LocalEditorScope.current.run {
                fun getSelectedDesignBlock(): DesignBlock? = editorContext.engine.block.findAllSelected().firstOrNull()

                val initial = remember { getSelectedDesignBlock()?.let { Selection.getDefault(editorContext.engine, it) } }
                val selection by remember(this) {
                    editorContext.engine.block.onSelectionChanged()
                        .flatMapLatest {
                            val selectedDesignBlock = getSelectedDesignBlock() ?: return@flatMapLatest flowOf(null)
                            editorContext.engine.event.subscribe(listOf(selectedDesignBlock))
                                .filter {
                                    // When the design block is unselected/deleted, this lambda is entered before onSelectionChanged is emitted.
                                    // We need to make sure that this flow does not emit previous selection in such scenario.
                                    selectedDesignBlock == getSelectedDesignBlock()
                                }
                                .map { Selection.getDefault(editorContext.engine, selectedDesignBlock) }
                                .onStart { emit(Selection.getDefault(editorContext.engine, selectedDesignBlock)) }
                        }
                }.collectAsState(initial = initial)
                remember(this, selection) {
                    Scope(parentScope = this, selection = selection)
                }
            }

        /**
         * The default enter transition of the row of items.
         * Note that the items are animated separately from the inspector bar. They both run in parallel.
         */
        val defaultItemsRowEnterTransition: @Composable Scope.() -> EnterTransition = {
            remember {
                slideInHorizontally(
                    animationSpec = tween(400, easing = CubicBezierEasing(0.05F, 0.7F, 0.1F, 1F)),
                    initialOffsetX = { it / 3 },
                )
            }
        }

        /**
         * The default enter transition of the inspector bar.
         */
        val defaultEnterTransition: @Composable Scope.() -> EnterTransition = {
            remember {
                slideInVertically(
                    animationSpec = tween(
                        durationMillis = 400,
                        easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f),
                    ),
                    initialOffsetY = { it },
                )
            }
        }

        /**
         * The default exit transition of the inspector bar.
         */
        val defaultExitTransition: @Composable Scope.() -> ExitTransition = {
            remember {
                slideOutVertically(
                    animationSpec = tween(
                        durationMillis = 150,
                        easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f),
                    ),
                    targetOffsetY = { it },
                )
            }
        }

        /**
         * The default decoration of the inspector bar.
         * Sets a background color, applies paddings and adds a close button to the inspector bar.
         */
        @Deprecated("Use DefaultDecoration function instead.", ReplaceWith("{ DefaultDecoration() }"))
        val defaultDecoration: @Composable Scope.(@Composable () -> Unit) -> Unit = {
            DefaultDecoration { it() }
        }

        /**
         * The default decoration of the inspector bar.
         * Sets a background color, applies paddings and adds a close button to the inspector barby adding a containing box.
         *
         * @param background the background of the containing box.
         * @param paddingValues the padding values of the containing box.
         * @param content the content of the inspector bar.
         */
        @Composable
        inline fun Scope.DefaultDecoration(
            background: Color = MaterialTheme.colorScheme.surface,
            paddingValues: PaddingValues = PaddingValues(start = 16.dp, top = 10.dp, bottom = 10.dp),
            content: @Composable () -> Unit,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(background)
                    .padding(paddingValues),
            ) {
                Box {
                    val gradientHeight = 64.dp
                    val gradientWidth = 16.dp
                    val closeButtonWidth = 48.dp
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Start,
                    ) {
                        Surface(
                            onClick = {
                                editorContext.engine.block.setSelected(editorContext.selection.designBlock, false)
                            },
                            modifier = Modifier
                                .size(closeButtonWidth, gradientHeight)
                                .padding(start = 4.dp, end = 4.dp, top = 2.dp, bottom = 6.dp)
                                .semantics { role = Role.Button },
                            shape = IconButtonDefaults.filledShape,
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                            shadowElevation = 3.dp,
                        ) {
                            Box(
                                modifier = Modifier.size(40.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(IconPack.Close, contentDescription = stringResource(id = R.string.ly_img_editor_close))
                            }
                        }
                        content()
                    }

                    val gradientColor = MaterialTheme.colorScheme.surface
                    val gradient = remember(gradientColor) {
                        Brush.horizontalGradient(
                            listOf(
                                gradientColor,
                                gradientColor.copy(alpha = 0f),
                            ),
                        )
                    }
                    Box(
                        modifier = Modifier
                            .offset(x = closeButtonWidth)
                            .size(gradientWidth, gradientHeight)
                            .background(gradient),
                    )
                }
            }
        }

        /**
         * A composable function that creates and remembers an [InspectorBar] instance.
         * By default, the following items are added to the inspector bar. Each item also contains information for which design
         * block type, fill and kind the item is shown:
         *
         * - InspectorBar.Button.rememberReplace // Video, Image, Sticker, Audio
         *
         * - InspectorBar.Button.rememberEditText // Text
         * - InspectorBar.Button.rememberFormatText // Text
         * - InspectorBar.Button.rememberFillStroke // Page, Video, Image, Shape, Text
         * - InspectorBar.Button.rememberVolume // Video, Audio (video scenes only)
         * - InspectorBar.Button.rememberCrop // Video, Image
         *
         * - InspectorBar.Button.rememberAdjustments // Video, Image
         * - InspectorBar.Button.rememberFilter // Video, Image
         * - InspectorBar.Button.rememberEffect // Video, Image
         * - InspectorBar.Button.rememberBlur // Video, Image
         * - InspectorBar.Button.rememberShape // Video, Image, Shape
         *
         * - InspectorBar.Button.rememberSelectGroup // Video, Image, Sticker, Shape, Text
         * - InspectorBar.Button.rememberEnterGroup // Group
         *
         * - InspectorBar.Button.rememberLayer // Video, Image, Sticker, Shape, Text
         * - InspectorBar.Button.rememberSplit // Video, Image, Sticker, Shape, Text, Audio (video scenes only)
         * - InspectorBar.Button.rememberMoveAsClip // Video, Image, Sticker, Shape, Text (video scenes only)
         * - InspectorBar.Button.rememberMoveAsOverlay // Video, Image, Sticker, Shape, Text (video scenes only)
         * - InspectorBar.Button.rememberReorder // Video, Image, Sticker, Shape, Text (video scenes only)
         * - InspectorBar.Button.rememberDuplicate // Video, Image, Sticker, Shape, Text, Audio
         * - InspectorBar.Button.rememberDelete // Video, Image, Sticker, Shape, Text, Audio
         *
         * For example, if you do not want to touch the default order, but rather add additional items and replace/hide default items, then
         * it is more convenient to call [EditorComponent.ListBuilder.modify] on the default builder [InspectorBar.ListBuilder.remember]:
         *
         * inspectorBar = {
         *     InspectorBar.remember(
         *         listBuilder = InspectorBar.ListBuilder.remember().modify {
         *             addLast {
         *                 InspectorBar.Button.remember(
         *                     id = EditorComponentId("my.package.inspectorBar.button.last")
         *                     vectorIcon = { IconPack.Music },
         *                     text = { "Last Button" },
         *                     onClick = {}
         *                 )
         *             }
         *             addFirst {
         *                 InspectorBar.Button.remember(
         *                     id = EditorComponentId("my.package.inspectorBar.button.first")
         *                     vectorIcon = { IconPack.Music },
         *                     text = { "First Button" },
         *                     onClick = {}
         *                 )
         *             }
         *             addAfter(id = InspectorBar.Button.Id.adjustments) {
         *                 InspectorBar.Button.remember(
         *                     id = EditorComponentId("my.package.inspectorBar.button.afterAdjustments")
         *                     vectorIcon = { IconPack.Music },
         *                     text = { "After System Gallery" },
         *                     onClick = {}
         *                 )
         *             }
         *             addBefore(id = InspectorBar.Button.Id.shape) {
         *                 InspectorBar.Button.remember(
         *                     id = EditorComponentId("my.package.inspectorBar.button.beforeShape")
         *                     vectorIcon = { IconPack.Music },
         *                     text = { "Before System Camera" },
         *                     onClick = {}
         *                 )
         *             }
         *             replace(id = InspectorBar.Button.Id.volume) {
         *                  InspectorBar.Button.remember(
         *                      id = EditorComponentId("my.package.inspectorBar.button.replacedVolume"),
         *                      vectorIcon = null,
         *                      text = { "Replaced Volume" },
         *                      onClick = {},
         *                  )
         *             }
         *             remove(id = InspectorBar.Button.Id.shapesLibrary)
         *         }
         *     )
         * }
         *
         * However, if you want to make more complex customizations that includes touching the default order, it is more convenient to
         * go fully custom via [InspectorBar.ListBuilder.remember] with [listBuilder] looking like this:
         *
         * For example, if you want to
         *  - 1. replace the icon of InspectorBar.Button.rememberAdjustments,
         *  - 2. drop InspectorBar.Button.rememberShape, InspectorBar.Button.rememberDelete, InspectorBar.Button.rememberSelectGroup, InspectorBar.Button.rememberEnterGroup,
         *  - 3. swap InspectorBar.Button.rememberFilter and InspectorBar.Button.rememberEffect,
         *  - 4. add one custom button to the front and another in the middle,
         *  - 5. update first custom button text when second custom button is clicked with an incremented value,
         *  - 6. show InspectorBar.Button.rememberEffect when the counter is even,
         *  - 7. force update all items on any engine event (that will be obvious from first custom button random icon).
         * you should invoke [InspectorBar.remember] with [listBuilder] looking like this:
         *
         * inspectorBar = {
         *     var counter by remember { mutableStateOf(0) }
         *     val inspectorBarScope by remember(this) {
         *          editorContext.engine.event.subscribe()
         *              .map { InspectorBar.Scope(parentScope = this) }
         *     }.collectAsState(initial = remember { InspectorBar.Scope(parentScope = this) })
         *     InspectorBar.remember(
         *         scope = inspectorBarScope,
         *         listBuilder = InspectorBar.ListBuilder.remember {
         *             add {
         *                 InspectorBar.Button.remember(
         *                     id = EditorComponentId("my.package.inspectorBar.button.custom1"),
         *                     vectorIcon = { listOf(IconPack.Music, IconPack.PlayBox).random() },
         *                     text = { "Custom1 $counter" },
         *                     onClick = {}
         *                 )
         *             }
         *             add { InspectorBar.Button.rememberReplace() }
         *             add { InspectorBar.Button.rememberEditText() }
         *             add { InspectorBar.Button.rememberFormatText() }
         *             add { InspectorBar.Button.rememberFillStroke() }
         *             add { InspectorBar.Button.rememberVolume() }
         *             add { InspectorBar.Button.rememberCrop() }
         *             add {
         *                 InspectorBar.Button.rememberAdjustments(
         *                     vectorIcon = { IconPack.PlayBox },
         *                 )
         *             }
         *             add {
         *                 InspectorBar.Button.rememberEffect(
         *                     visible = { counter % 2 == 0 }
         *                 )
         *             }
         *             add { InspectorBar.Button.rememberFilter() }
         *             add { InspectorBar.Button.rememberBlur() }
         *             add {
         *                 InspectorBar.Button.remember(
         *                     id = EditorComponentId("my.package.inspectorBar.button.custom2"),
         *                     vectorIcon = { IconPack.PlayBox },
         *                     text = { "Custom2" },
         *                     onClick = { counter++ }
         *                 )
         *             }
         *             add { InspectorBar.Button.rememberLayer() }
         *             add { InspectorBar.Button.rememberSplit() }
         *             add { InspectorBar.Button.rememberMoveAsClip() }
         *             add { InspectorBar.Button.rememberMoveAsOverlay() }
         *             add { InspectorBar.Button.rememberReorder() }
         *             add { InspectorBar.Button.rememberDuplicate() }
         *         }
         *     )
         * }
         *
         * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters below.
         * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
         * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas
         * for granular recompositions over updating the scope, since scope change triggers full recomposition of the inspector bar.
         * Also prefer updating individual [Item]s over updating the whole [InspectorBar].
         * Ideally, scope should be updated when the parent scope (scope of the parent component) is updated and when you want to
         * observe changes from the [Engine].
         * By default [defaultScope] is used.
         * @param visible whether the inspector bar should be visible based on the [Engine]'s current state.
         * Default value is always true.
         * @param enterTransition transition of the inspector bar when it enters the parent composable.
         * Default value is [defaultEnterTransition].
         * @param exitTransition transition of the inspector bar when it exits the parent composable.
         * Default value is [defaultExitTransition].
         * @param decoration decoration of the inspector bar. Useful when you want to add custom background, foreground, shadow, paddings etc.
         * Default value is [DefaultDecoration].
         * @param listBuilder a builder that builds the list of [InspectorBar.Item]s that should be part of the inspector bar.
         * Note that adding items to the list does not mean displaying. The items will be displayed if [InspectorBar.Item.visible]
         * is true for them.
         * Also note that items will be rebuilt when [scope] is updated.
         * By default, the list mentioned above is added to the inspector bar.
         * @param horizontalArrangement the horizontal arrangement that should be used to render the items in the inspector bar horizontally.
         * Note that the value will be ignored in case [listBuilder] contains aligned items. Check [EditorComponent.ListBuilder.Scope.New.aligned] for more
         * details on how to configure arrangement of aligned items.
         * Default value is [Arrangement.Start].
         * @param itemsRowEnterTransition separate transition of the [Item]s row only when [enterTransition] is running.
         * Default value is [defaultItemsRowEnterTransition].
         * @param itemsRowExitTransition separate transition of the [Item]s row only when [exitTransition] is running.
         * Default value is always no exit transition.
         * @param itemDecoration decoration of the items in the inspector bar. Useful when you want to add custom background, foreground, shadow,
         * paddings etc to the items. Prefer using this decoration when you want to apply the same decoration to all the items, otherwise
         * set decoration to individual items.
         * Default value is always no decoration.
         * @return an inspector bar that will be displayed when a design block is selected.
         */
        @Composable
        fun remember(
            scope: Scope = defaultScope,
            visible: @Composable Scope.() -> Boolean = { editorContext.safeSelection != null },
            enterTransition: @Composable Scope.() -> EnterTransition = defaultEnterTransition,
            exitTransition: @Composable Scope.() -> ExitTransition = defaultExitTransition,
            decoration: @Composable Scope.(content: @Composable () -> Unit) -> Unit = { DefaultDecoration { it() } },
            listBuilder: HorizontalListBuilder<Item<*>> = ListBuilder.remember(),
            horizontalArrangement: @Composable Scope.() -> Arrangement.Horizontal = { Arrangement.Start },
            itemsRowEnterTransition: @Composable Scope.() -> EnterTransition = defaultItemsRowEnterTransition,
            itemsRowExitTransition: @Composable Scope.() -> ExitTransition = noneExitTransition,
            itemDecoration: @Composable Scope.(content: @Composable () -> Unit) -> Unit = { it() },
            `_`: Nothing = nothing,
        ): InspectorBar = remember(
            scope,
            visible,
            enterTransition,
            exitTransition,
            decoration,
            listBuilder,
            horizontalArrangement,
            itemsRowEnterTransition,
            itemsRowExitTransition,
            itemDecoration,
        ) {
            InspectorBar(
                scope = scope,
                visible = visible,
                enterTransition = enterTransition,
                exitTransition = exitTransition,
                decoration = decoration,
                listBuilder = listBuilder,
                horizontalArrangement = horizontalArrangement,
                itemsRowEnterTransition = itemsRowEnterTransition,
                itemsRowExitTransition = itemsRowExitTransition,
                itemDecoration = itemDecoration,
                `_` = `_`,
            )
        }
    }
}
