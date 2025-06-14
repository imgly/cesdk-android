package ly.img.editor.core.component

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import ly.img.editor.core.EditorContext
import ly.img.editor.core.EditorScope
import ly.img.editor.core.LocalEditorScope
import ly.img.editor.core.component.CanvasMenu.Companion.remember
import ly.img.editor.core.component.CanvasMenu.Item
import ly.img.editor.core.component.EditorComponent.ListBuilder.Companion.modify
import ly.img.editor.core.component.data.Nothing
import ly.img.editor.core.component.data.Selection
import ly.img.editor.core.component.data.nothing
import ly.img.editor.core.ui.IconTextButton
import ly.img.editor.core.ui.toPx
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import kotlin.math.cos
import kotlin.math.roundToInt

/**
 * A component for rendering the canvas menu next to a design block when it is selected.
 * Use [remember] from the companion object to create an instance of this class.
 *
 * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters below.
 * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
 * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas
 * for granular recompositions over updating the scope, since scope change triggers full recomposition of the canvas menu.
 * Also prefer updating individual [Item]s over updating the whole [CanvasMenu].
 * Ideally, scope should be updated when the parent scope (scope of the parent component) is updated and when you want to
 * observe changes from the [Engine].
 * @param visible whether the canvas menu should be visible.
 * @param enterTransition transition of the canvas menu when it enters the parent composable.
 * @param exitTransition transition of the canvas menu when it exits the parent composable.
 * @param decoration decoration of the canvas menu. Useful when you want to add custom background, foreground, shadow, paddings etc.
 * @param listBuilder the list builder of this canvas menu.
 * @param itemDecoration decoration of the items in the canvas menu. Useful when you want to add custom background, foreground, shadow,
 * paddings etc to the items. Prefer using this decoration when you want to apply the same decoration to all the items, otherwise
 * set decoration to individual items.
 */
@Stable
class CanvasMenu private constructor(
    override val scope: Scope,
    override val visible: @Composable Scope.() -> Boolean,
    override val enterTransition: @Composable Scope.() -> EnterTransition,
    override val exitTransition: @Composable Scope.() -> ExitTransition,
    override val decoration: @Composable Scope.(content: @Composable () -> Unit) -> Unit,
    val listBuilder: HorizontalListBuilder<Item<*>>,
    val itemDecoration: @Composable Scope.(content: @Composable () -> Unit) -> Unit,
    private val `_`: Nothing = nothing,
) : EditorComponent<CanvasMenu.Scope>() {
    override val id: EditorComponentId = EditorComponentId("ly.img.component.canvasMenu")

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
                add { Button.rememberSelectGroup() }
                add { Divider.remember(visible = { editorContext.isSelectionInGroup }) }
                add { Button.rememberBringForward() }
                add { Button.rememberSendBackward() }
                add { Divider.remember(visible = { editorContext.canSelectionMove }) }
                add { Button.rememberDuplicate() }
                add { Button.rememberDelete() }
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

    @Composable
    override fun Scope.Content(animatedVisibilityScope: AnimatedVisibilityScope?) {
        listBuilder.scope.result.forEach { (alignment, data) ->
            // Main list
            if (alignment == null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
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
     * The scope of the [CanvasMenu] component.
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

        private val _isSelectionInGroup by lazy {
            selection?.parentDesignBlock?.let {
                DesignBlockType.get(editorContext.engine.block.getType(it)) == DesignBlockType.Group
            } ?: false
        }

        private val _selectionSiblings by lazy {
            selection?.parentDesignBlock ?: return@lazy emptyList()
            val childIsAlwaysOnTop = editorContext.engine.block.isAlwaysOnTop(selection.designBlock)
            val childIsAlwaysOnBottom = editorContext.engine.block.isAlwaysOnBottom(selection.designBlock)
            val children = editorContext.engine.block.getChildren(selection.parentDesignBlock)
            // contains at least internalSelection.designBlock
            children.filter { childToCompare ->
                val matchingIsAlwaysOnTop = childIsAlwaysOnTop == editorContext.engine.block.isAlwaysOnTop(childToCompare)
                val matchingIsAlwaysOnBottom = childIsAlwaysOnBottom == editorContext.engine.block.isAlwaysOnBottom(
                    childToCompare,
                )
                matchingIsAlwaysOnTop && matchingIsAlwaysOnBottom
            }
        }

        private val _isScenePlaying by lazy {
            val page = editorContext.engine.scene.getCurrentPage() ?: editorContext.engine.scene.getPages()[0]
            editorContext.engine.block.isPlaying(page)
        }

        private val _canSelectionMove by lazy {
            val selection = selection ?: return@lazy false
            editorContext.engine.block.isAllowedByScope(selection.designBlock, "editor/add") &&
                run {
                    selection.parentDesignBlock?.let {
                        DesignBlockType.get(editorContext.engine.block.getType(it)) == DesignBlockType.Track &&
                            editorContext.engine.block.isAlwaysOnBottom(it)
                    } ?: false
                }.not() &&
                _selectionSiblings.size > 1
        }

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

        /**
         * Returns true if the design block in [selection] is in a [DesignBlockType.Group].
         */
        val EditorContext.isSelectionInGroup: Boolean
            get() = this@Scope._isSelectionInGroup

        /**
         * Returns true if the selection can be moved: forward or backward.
         */
        val EditorContext.canSelectionMove: Boolean
            get() = this@Scope._canSelectionMove

        /**
         * Returns the list of siblings of the design block in [selection] that can be used to reorder.
         * Note that the list contains [Selection.designBlock] as well.
         */
        val EditorContext.selectionSiblings: List<DesignBlock>
            get() = this@Scope._selectionSiblings

        /**
         * Returns true if the scene is currently playing. The value can be true only when in [ly.img.engine.SceneMode.VIDEO].
         */
        val EditorContext.isScenePlaying: Boolean
            get() = this@Scope._isScenePlaying
    }

    override fun toString(): String = "$`_`CanvasMenu(id=$id)"

    /**
     * The scope of the [Item] component.
     */
    @Stable
    open class ItemScope(
        private val parentScope: EditorScope,
    ) : EditorScope() {
        override val impl: EditorContext = parentScope.editorContext

        /**
         * Current selection of the editor.
         */
        val EditorContext.safeSelection: Selection?
            get() = (parentScope as Scope).run {
                editorContext.safeSelection
            }

        /**
         * Current selection of the editor.
         * Note that this is an unsafe call. Consider using [safeSelection] to get the nullable value.
         */
        val EditorContext.selection: Selection
            get() = requireNotNull(safeSelection)

        /**
         * Returns true if the design block in [selection] is in a [DesignBlockType.Group].
         */
        val EditorContext.isSelectionInGroup: Boolean
            get() = (parentScope as Scope).run {
                editorContext.isSelectionInGroup
            }

        /**
         * Returns true if the selection can be moved: forward or backward.
         */
        val EditorContext.canSelectionMove: Boolean
            get() = (parentScope as Scope).run {
                editorContext.canSelectionMove
            }

        /**
         * Returns the list of siblings of the design block in [selection] that can be used to reorder.
         * Note that the list contains [Selection.designBlock] as well.
         */
        val EditorContext.selectionSiblings: List<DesignBlock>
            get() = (parentScope as Scope).run {
                editorContext.selectionSiblings
            }
    }

    /**
     * A component that represents an item that can be rendered in the canvas menu.
     */
    abstract class Item<Scope : ItemScope> : EditorComponent<Scope>() {
        /**
         * The content of the item in the canvas menu.
         */
        @Composable
        protected abstract fun Scope.ItemContent()

        @Composable
        final override fun Scope.Content(animatedVisibilityScope: AnimatedVisibilityScope?) {
            ItemContent()
        }
    }

    /**
     * A component that represents a custom content in the [CanvasMenu].
     *
     * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
     * signature @Composable Scope.() -> {}.
     * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
     * Consider using [ItemScope] as the scope which will be updated when the parent component scope
     * ([CanvasMenu.scope], accessed via [LocalEditorScope]) is updated:
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

        override fun toString(): String = "CanvasMenu.Custom(id=$id)"

        companion object {
            /**
             * A composable function that creates and remembers a [CanvasMenu.Custom] instance.
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
             * @return a custom item that will be displayed in the canvas menu.
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
     * A component that represents a button in the [CanvasMenu].
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
                onClick = { onClick() },
                enabled = enabled(),
                icon = icon?.let { { it() } },
                text = text?.let { { it() } },
                contentPadding = PaddingValues(0.dp),
                tint = tint?.invoke(this) ?: MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        override fun toString(): String = "$`_`CanvasMenu.Button(id=$id)"

        class Id {
            companion object
        }

        companion object {
            /**
             * Predicate to be used when the [EditorComponent] is always enabled.
             */
            val alwaysEnabled: @Composable ButtonScope.() -> Boolean = { true }

            /**
             * A composable function that creates and remembers a [CanvasMenu.Button] instance.
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
             * @return a button that will be displayed in the canvas menu.
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
                    enabled = enabled,
                    tint = tint,
                    `_` = `_`,
                )
            }

            /**
             * A composable helper function that creates and remembers a [CanvasMenu.Button] instance where [icon] composable is
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
             * @return a button that will be displayed in the canvas menu.
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
                    "Content description must be provided when creating a CanvasMenu.Button with icon only."
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

    /**
     * The scope of the [Divider] component.
     *
     * @param parentScope the scope of the parent component.
     */
    @Stable
    open class DividerScope(
        parentScope: EditorScope,
    ) : ItemScope(parentScope)

    /**
     * A component that represents a divider in the [CanvasMenu].
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
     * @param modifier the modifier of the divider.
     * @param color the color of the divider.
     */
    @Stable
    class Divider private constructor(
        override val scope: DividerScope,
        override val visible: @Composable DividerScope.() -> Boolean,
        override val enterTransition: @Composable DividerScope.() -> EnterTransition,
        override val exitTransition: @Composable DividerScope.() -> ExitTransition,
        override val decoration: @Composable DividerScope.(content: @Composable () -> Unit) -> Unit,
        val modifier: @Composable DividerScope.() -> Modifier,
        val color: @Composable DividerScope.() -> Color,
        private val `_`: Nothing,
    ) : Item<DividerScope>() {
        override val id = EditorComponentId("ly.img.component.canvasMenu.divider")

        @Composable
        override fun DividerScope.ItemContent() {
            Divider(
                modifier = modifier(),
                color = color(),
            )
        }

        override fun toString(): String = "$`_`CanvasMenu.Divider(id=$id)"

        class Id {
            companion object
        }

        companion object {
            /**
             * A composable function that creates and remembers a [Divider] instance.
             *
             * @param scope the scope of this component. Every new value will trigger recomposition of all functions with
             * signature @Composable Scope.() -> {}.
             * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
             * By default it is updated only when the parent scope (accessed via [LocalEditorScope]) is updated.
             * @param visible whether the divider should be visible.
             * Default value is always true.
             * @param enterTransition transition of the divider when it enters the parent composable.
             * Default value is always no enter transition.
             * @param exitTransition transition of the divider when it exits the parent composable.
             * Default value is always no exit transition.
             * @param decoration decoration of the divider. Useful when you want to add custom background, foreground, shadow, paddings etc.
             * Default value is always no decoration.
             * @param modifier the modifier of the divider.
             * Default value is always a [Modifier] that sets size and paddings to the divider.
             * @param color the color of the divider.
             * Default value is always [DividerDefaults.color].
             * @return a divider that will be displayed in the canvas menu.
             */
            @Composable
            fun remember(
                scope: DividerScope = LocalEditorScope.current.run {
                    remember(this) { DividerScope(parentScope = this) }
                },
                visible: @Composable DividerScope.() -> Boolean = alwaysVisible,
                enterTransition: @Composable DividerScope.() -> EnterTransition = noneEnterTransition,
                exitTransition: @Composable DividerScope.() -> ExitTransition = noneExitTransition,
                decoration: @Composable DividerScope.(@Composable () -> Unit) -> Unit = { it() },
                modifier: @Composable DividerScope.() -> Modifier = {
                    remember(this) {
                        Modifier
                            .padding(horizontal = 8.dp)
                            .size(width = 1.dp, height = 24.dp)
                    }
                },
                color: @Composable DividerScope.() -> Color = { DividerDefaults.color },
                `_`: Nothing = nothing,
            ): Divider = remember(scope, visible, enterTransition, exitTransition, modifier, color) {
                Divider(
                    scope = scope,
                    visible = visible,
                    enterTransition = enterTransition,
                    exitTransition = exitTransition,
                    decoration = decoration,
                    modifier = modifier,
                    color = color,
                    `_` = `_`,
                )
            }
        }
    }

    companion object {
        /**
         * The default scope of the canvas menu. The value is updated when:
         * 1. Parent scope is updated.
         * 2. Selection is updated.
         * 3. Scene playing status is updated (applicable in video scenes only).
         */
        @OptIn(ExperimentalCoroutinesApi::class)
        val defaultScope: Scope
            @Composable
            get() = LocalEditorScope.current.run {
                fun getSelectedDesignBlock(): DesignBlock? = editorContext.engine.block.findAllSelected().firstOrNull()

                val initial = remember { getSelectedDesignBlock()?.let { Selection.getDefault(editorContext.engine, it) } }
                val camera = editorContext.engine.block.findByType(DesignBlockType.Camera).first()
                val selection by remember(this) {
                    editorContext.engine.block.onSelectionChanged()
                        .flatMapLatest {
                            val selectedDesignBlock = getSelectedDesignBlock() ?: return@flatMapLatest flowOf(null)
                            val parentDesignBlock = editorContext.engine.block.getParent(selectedDesignBlock)
                            val observableDesignBlocks = parentDesignBlock
                                ?.let { listOf(it, selectedDesignBlock) } ?: listOf(selectedDesignBlock)
                            merge(
                                editorContext.engine.event.subscribe(observableDesignBlocks),
                                editorContext.engine.event.subscribe(listOf(camera)),
                                editorContext.engine.editor.onStateChanged()
                                    .map { editorContext.engine.editor.getEditMode() }
                                    .distinctUntilChanged(),
                            )
                                .filter {
                                    // When the design block is unselected/deleted, this lambda is entered before onSelectionChanged is emitted.
                                    // We need to make sure that this flow does not emit previous selection in such scenario.
                                    selectedDesignBlock == getSelectedDesignBlock()
                                }
                                .map { Selection.getDefault(editorContext.engine, selectedDesignBlock) }
                                .onStart { emit(Selection.getDefault(editorContext.engine, selectedDesignBlock)) }
                        }
                }.collectAsState(initial = initial)

                var isScenePlayingTrigger by remember { mutableStateOf(false) }
                LaunchedEffect(this) {
                    val page = editorContext.engine.scene.getCurrentPage() ?: editorContext.engine.scene.getPages()[0]
                    editorContext.engine.event.subscribe(listOf(page))
                        .onEach { isScenePlayingTrigger = isScenePlayingTrigger.not() }
                        .collect()
                }

                remember(this, selection, isScenePlayingTrigger) {
                    Scope(parentScope = this, selection = selection)
                }
            }

        /**
         * The default decoration of the canvas menu.
         * Calculates the position and rotation of the selected design block and finds the coordinates where the canvas menu should be placed.
         * Finally, canvas menu is placed in a surface which parameters can be configured.
         *
         * @param shape the shape of the surface.
         * @param contentColor the content color of the surface.
         * @param shadowElevation the shadow elevation of the surface.
         * @param rotateHandleSize the reserved size of the rotate handle.
         * @param verticalPadding the vertical padding between the surface and selected design block.
         * @param horizontalPadding the horizontal padding between the surface and horizontal borders of the canvas.
         * @param content the content of the canvas menu.
         */
        @Composable
        inline fun Scope.DefaultDecoration(
            shape: Shape = MaterialTheme.shapes.extraLarge,
            contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
            shadowElevation: Dp = 1.dp,
            rotateHandleSize: Dp = 48.dp,
            verticalPadding: Dp = 24.dp,
            horizontalPadding: Dp = 16.dp,
            crossinline content: @Composable () -> Unit,
        ) {
            val editorState by editorContext.state.collectAsState()
            val selectedBlockRect = editorContext.selection.screenSpaceBoundingBoxRect
            if (selectedBlockRect.width().isNaN().not() && selectedBlockRect.height().isNaN().not()) {
                val rotateHandleSizePx = rotateHandleSize.toPx()
                val dy = remember(editorState, editorContext.selection, rotateHandleSize) {
                    val isGizmoPresent = editorContext.engine.editor.getSettingBoolean("controlGizmo/showRotateHandles") ||
                        editorContext.engine.editor.getSettingBoolean("controlGizmo/showMoveHandles")
                    if (isGizmoPresent) {
                        val rotation = editorContext.engine.block.getRotation(editorContext.selection.designBlock)
                        (cos(rotation) * rotateHandleSizePx).roundToInt()
                    } else {
                        0
                    }
                }
                Surface(
                    shape = shape,
                    contentColor = contentColor,
                    shadowElevation = shadowElevation,
                    modifier = Modifier.layout { measurable, constraints ->
                        val placeable = measurable.measure(constraints)
                        val width = placeable.width
                        val height = placeable.height
                        layout(width, height) nestedLayout@{
                            // In certain scenarios (eg. changing theme while the Canvas Menu is visible),
                            // it was observed that minWidth = maxWidth = 0. Not sure why this happens, for now, we just return here.
                            if (constraints.isZero) return@nestedLayout
                            val verticalPaddingPx = verticalPadding.roundToPx()
                            val horizontalPaddingPx = horizontalPadding.roundToPx()
                            val x = selectedBlockRect.centerX().dp.roundToPx() - width / 2
                            val minX = constraints.minWidth + horizontalPaddingPx
                            val maxX = constraints.maxWidth - width - horizontalPaddingPx
                            // minX > maxX if the allocated horizontal size of the canvas menu is larger than the screen size
                            val constrainedX = if (minX > maxX) horizontalPaddingPx else x.coerceIn(minX, maxX)

                            // Preference order -
                            // 1. Top
                            // 2. Bottom
                            // 3. Below top handle
                            val constrainedY = run {
                                val blockCenterY = selectedBlockRect.centerY()
                                val blockHeight = selectedBlockRect.height()
                                val canvasInsets = editorState.canvasInsets
                                val minY = constraints.minHeight + canvasInsets.top.dp.roundToPx()
                                val topY =
                                    (blockCenterY - blockHeight / 2).dp.roundToPx() - height - verticalPaddingPx + if (dy < 0) dy else 0
                                if (topY > minY) {
                                    return@run topY
                                }
                                val bottomY = (blockCenterY + blockHeight / 2).dp.roundToPx() + verticalPaddingPx + if (dy > 0) dy else 0
                                val bottomCutOff = constraints.maxHeight - canvasInsets.bottom.dp.roundToPx()
                                if (bottomY + height + horizontalPaddingPx <= bottomCutOff) {
                                    return@run bottomY
                                }
                                (blockCenterY - blockHeight / 2).dp.roundToPx() + horizontalPaddingPx + if (dy < 0) dy else 0
                            }
                            placeable.place(constrainedX, constrainedY)
                        }
                    },
                ) { content() }
            }
        }

        /**
         * A composable function that creates and remembers a [CanvasMenu] instance.
         * By default, the following items are added to the canvas menu. Each item also contains information for which design
         * block type, fill and kind the item is shown:
         *
         * - CanvasMenu.Button.rememberSelectGroup
         * - CanvasMenu.Button.rememberBringForward
         * - CanvasMenu.Button.rememberSendBackward
         * - CanvasMenu.Divider.remember
         * - CanvasMenu.Button.rememberDuplicate
         * - CanvasMenu.Button.rememberDelete
         *
         * For example, if you do not want to touch the default order, but rather add additional items and replace/hide default items, then
         * it is more convenient to call [EditorComponent.ListBuilder.modify] on the default builder [CanvasMenu.ListBuilder.remember]:
         *
         * canvasMenu = {
         *     CanvasMenu.remember(
         *         listBuilder = CanvasMenu.ListBuilder.remember().modify {
         *             addLast {
         *                 CanvasMenu.Button.remember(
         *                     id = EditorComponentId("my.package.canvasMenu.button.last")
         *                     vectorIcon = { IconPack.Music },
         *                     text = { "Last Button" },
         *                     onClick = {}
         *                 )
         *             }
         *             addFirst {
         *                 CanvasMenu.Button.remember(
         *                     id = EditorComponentId("my.package.canvasMenu.button.first")
         *                     vectorIcon = { IconPack.Music },
         *                     text = { "First Button" },
         *                     onClick = {}
         *                 )
         *             }
         *             addAfter(id = CanvasMenu.Button.Id.sendBackward) {
         *                 CanvasMenu.Button.remember(
         *                     id = EditorComponentId("my.package.canvasMenu.button.afterSendBackward")
         *                     vectorIcon = { IconPack.Music },
         *                     text = { "After System Gallery" },
         *                     onClick = {}
         *                 )
         *             }
         *             addBefore(id = CanvasMenu.Button.Id.duplicate) {
         *                 CanvasMenu.Button.remember(
         *                     id = EditorComponentId("my.package.canvasMenu.button.beforeDuplicate")
         *                     vectorIcon = { IconPack.Music },
         *                     text = { "Before System Camera" },
         *                     onClick = {}
         *                 )
         *             }
         *             replace(id = CanvasMenu.Button.Id.bringForward) {
         *                  CanvasMenu.Button.remember(
         *                      id = EditorComponentId("my.package.canvasMenu.button.replacedBringForward"),
         *                      vectorIcon = null,
         *                      text = { "Replaced Bring Forward" },
         *                      onClick = {},
         *                  )
         *             }
         *             remove(id = CanvasMenu.Button.Id.delete)
         *         }
         *     )
         * }
         *
         * However, if you want to make more complex customizations that includes touching the default order, it is more convenient to
         * go fully custom via [CanvasMenu.ListBuilder.remember] with [listBuilder] looking like this:
         *
         * For example, if you want to
         *  - 1. replace the icon of CanvasMenu.Button.rememberBringForward,
         *  - 2. drop CanvasMenu.Button.delete,
         *  - 3. swap CanvasMenu.Button.bringForward and CanvasMenu.Button.sendBackward,
         *  - 4. add one custom button to the front,
         *  - 5. update first custom button text when second custom button is clicked with an incremented value,
         *  - 6. show CanvasMenu.Button.rememberDuplicate when the counter is even,
         *  - 7. force update all items on any engine event (that will be obvious from first custom button random icon).
         * you should invoke [CanvasMenu.remember] with [listBuilder] looking like this:
         *
         * canvasMenu = {
         *     var counter by remember { mutableStateOf(0) }
         *     val canvasMenuScope by remember(this) {
         *          editorContext.engine.event.subscribe()
         *              .map { CanvasMenu.Scope(parentScope = this) }
         *     }.collectAsState(initial = remember { CanvasMenu.Scope(parentScope = this) })
         *     CanvasMenu.remember(
         *         scope = canvasMenuScope,
         *         listBuilder = CanvasMenu.ListBuilder.remember {
         *             add {
         *                 CanvasMenu.Button.remember(
         *                     id = EditorComponentId("my.package.canvasMenu.button.custom1"),
         *                     vectorIcon = { listOf(IconPack.Music, IconPack.PlayBox).random() },
         *                     text = { "Custom1 $counter" },
         *                     onClick = {}
         *                 )
         *             }
         *             add { CanvasMenu.Button.rememberSelectGroup() }
         *             add { CanvasMenu.Divider.remember(visible = { editorContext.isSelectionInGroup }) }
         *             add { CanvasMenu.Button.rememberSendBackward() }
         *             add {
         *                 CanvasMenu.Button.rememberBringForward(
         *                     vectorIcon = { IconPack.PlayBox },
         *                 )
         *             }
         *             add { CanvasMenu.Divider.remember(visible = { editorContext.canSelectionMove }) }
         *             add {
         *                 CanvasMenu.Button.rememberDuplicate(
         *                     visible = { counter % 2 == 0 }
         *                 )
         *             }
         *         }
         *     )
         * }
         *
         * @param scope the scope of this component. Every new value will trigger recomposition of all the lambda parameters below.
         * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
         * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas
         * for granular recompositions over updating the scope, since scope change triggers full recomposition of the canvas menu.
         * Also prefer updating individual [Item]s over updating the whole [CanvasMenu].
         * Ideally, scope should be updated when the parent scope (scope of the parent component) is updated and when you want to
         * observe changes from the [Engine].
         * By default [defaultScope] is used.
         * @param visible whether the canvas menu should be visible.
         * By default the value is true when touch is not active, no sheet is displayed currently, a design block is selected,
         * selected design block does not have a type [DesignBlockType.Audio] or [DesignBlockType.Page] and the keyboard is not visible.
         * In addition, selected design block should be visible at current playback time and containing scene should be on pause if design block is selected in a video scene.
         * @param enterTransition transition of the canvas menu when it enters the parent composable.
         * Default value is always no enter transition.
         * @param exitTransition transition of the canvas menu when it exits the parent composable.
         * Default value is always no exit transition.
         * @param decoration decoration of the canvas menu. Useful when you want to add custom background, foreground, shadow, paddings etc.
         * Default value is [DefaultDecoration].
         * @param listBuilder a builder that builds the list of [CanvasMenu.Item]s that should be part of the canvas menu.
         * Note that adding items to the list does not mean displaying. The items will be displayed if [CanvasMenu.Item.visible]
         * is true for them.
         * Also note that items will be rebuilt when [scope] is updated.
         * By default, the list mentioned above is added to the canvas menu.
         * @param itemDecoration decoration of the items in the canvas menu. Useful when you want to add custom background, foreground, shadow,
         * paddings etc to the items. Prefer using this decoration when you want to apply the same decoration to all the items, otherwise
         * set decoration to individual items.
         * Default value is always no decoration.
         * @return a canvas menu that will be displayed when a design block is selected.
         */
        @Composable
        fun remember(
            scope: Scope = defaultScope,
            visible: @Composable Scope.() -> Boolean = {
                val editorState by editorContext.state.collectAsState()
                remember(this, editorState) {
                    editorState.isTouchActive.not() &&
                        editorState.activeSheet == null &&
                        editorContext.safeSelection != null &&
                        editorContext.selection.type != DesignBlockType.Page &&
                        editorContext.selection.type != DesignBlockType.Audio &&
                        editorContext.engine.editor.getEditMode() != "Text" &&
                        editorContext.isScenePlaying.not() &&
                        editorContext.selection.isVisibleAtCurrentPlaybackTime
                }
            },
            enterTransition: @Composable Scope.() -> EnterTransition = noneEnterTransition,
            exitTransition: @Composable Scope.() -> ExitTransition = noneExitTransition,
            decoration: @Composable Scope.(content: @Composable () -> Unit) -> Unit = { DefaultDecoration { it() } },
            listBuilder: HorizontalListBuilder<Item<*>> = ListBuilder.remember(),
            itemDecoration: @Composable Scope.(content: @Composable () -> Unit) -> Unit = { it() },
            `_`: Nothing = nothing,
        ): CanvasMenu = remember(
            scope,
            visible,
            enterTransition,
            exitTransition,
            decoration,
            listBuilder,
            itemDecoration,
        ) {
            CanvasMenu(
                scope = scope,
                visible = visible,
                enterTransition = enterTransition,
                exitTransition = exitTransition,
                decoration = decoration,
                listBuilder = listBuilder,
                itemDecoration = itemDecoration,
                `_` = `_`,
            )
        }
    }
}
