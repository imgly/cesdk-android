package ly.img.editor.core.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import ly.img.editor.core.EditorScope
import ly.img.editor.core.LocalEditorScope
import ly.img.editor.core.ScopedDecoration
import ly.img.editor.core.ScopedProperty
import ly.img.editor.core.configuration.remember
import ly.img.engine.Engine
import java.util.LinkedList
import java.util.UUID

typealias HorizontalListBuilder<Item> = EditorComponent.ListBuilder<Item, Alignment.Horizontal, Arrangement.Horizontal>

typealias HorizontalListBuilderScope<Item> =
    EditorComponent.ListBuilder.New<Item, Alignment.Horizontal, Arrangement.Horizontal>

/**
 * A class that is used as an identifier for class [EditorComponent].
 * Note that it is highly recommended that every unique [EditorComponent] has a unique id.
 */
@JvmInline
@Immutable
value class EditorComponentId(
    val id: String,
)

/**
 * A class that represents a component that can be rendered in the editor.
 *
 * Use [EditorComponent.Companion.remember] function to create ambiguous components or use our inherited classes.
 * Check [EditorComponentBuilder] to see what each property does.
 */
@Stable
abstract class EditorComponent<Scope : EditorScope> {
    abstract val scope: Scope
    abstract val id: EditorComponentId
    abstract val modifier: Modifier
    abstract val visible: Boolean
    abstract val enterTransition: EnterTransition
    abstract val exitTransition: ExitTransition
    abstract val decoration: ScopedDecoration<Scope>

    /**
     * The main content of the component that should be provided.
     *
     * @param animatedVisibilityScope the animated visibility scope of this component. This scope can be used to
     * animate children of this component separately when [enterTransition] and [exitTransition]s are running.
     * Check the documentation of [AnimatedVisibilityScope.animateEnterExit] for more details.
     * Note that the value is null if [enterTransition] and [exitTransition] are [EnterTransition.None] and [ExitTransition.None]
     * respectively.
     */
    @Composable
    protected abstract fun Scope.Content(animatedVisibilityScope: AnimatedVisibilityScope?)

    @Composable
    @Suppress("NOTHING_TO_INLINE")
    internal inline fun Scope.ContentInternal(animatedVisibilityScope: AnimatedVisibilityScope?) {
        Content(animatedVisibilityScope)
    }

    @Stable
    companion object

    /**
     * A utility class for building list of [EditorComponent]s.
     */
    @Stable
    abstract class ListBuilder<Item : EditorComponent<*>, Alignment : Any, Arrangement : Any> {
        class AlignmentData<Item : EditorComponent<*>, Arrangement : Any>(
            val arrangement: Arrangement?,
            val items: List<Item>,
        )

        class AlignmentProviderData<Item : EditorComponent<*>, Arrangement : Any>(
            val arrangement: Arrangement?,
            val items: List<ScopedProperty<EditorScope, Item>>,
        )

        @Composable
        fun build(scope: EditorScope) = scope.buildLocal()

        @Composable
        abstract fun EditorScope.buildLocal(): Map<Alignment?, AlignmentData<Item, Arrangement>>

        /**
         * A scope that allows only appending new items.
         */
        class New<Item : EditorComponent<*>, Alignment : Any, Arrangement : Any> : ListBuilder<Item, Alignment, Arrangement>() {
            private val itemProviderMapping: MutableMap<Alignment?, AlignmentProviderData<Item, Arrangement>> = mutableMapOf()
            private val activeAlignmentItemProviderList: MutableList<ScopedProperty<EditorScope, Item>> = LinkedList()
            private var activeAlignment: Alignment? = null

            internal var builder: New<Item, Alignment, Arrangement>.() -> Unit = {}

            @Composable
            override fun EditorScope.buildLocal() = buildMap<Alignment?, AlignmentData<Item, Arrangement>> {
                builder()
                // Every recomposition calls the block, fills up the item list and then clears it
                if (activeAlignmentItemProviderList.isNotEmpty()) {
                    this[null] = AlignmentData(
                        arrangement = null,
                        items = activeAlignmentItemProviderList.map { it() },
                    )
                    activeAlignmentItemProviderList.clear()
                }
                itemProviderMapping.forEach { (alignment, data) ->
                    this[alignment] = AlignmentData(
                        arrangement = data.arrangement,
                        items = data.items.map { it() },
                    )
                }
                itemProviderMapping.clear()
            }

            /**
             * Appends a new [EditorComponent] item in the list.
             * Note that adding items to the list does not mean displaying. The items will be displayed if [EditorComponent.visible]
             * is true for them.
             * Also note that [EditorScope] in the [block] builder is the scope of the parent [EditorComponent].
             *
             * @param block a building block that returns an item that should be added to the list.
             */
            fun add(block: ScopedProperty<EditorScope, Item>) {
                if (activeAlignment == null && itemProviderMapping.isNotEmpty()) {
                    error("It is not allowed to add items both inside and outside align block at the same time.")
                }
                activeAlignmentItemProviderList.add(block)
            }

            /**
             * Starts a new aligned group in the component. All [add] invocations withing [block] will be grouped
             * together, be aligned via [alignment] and be arranged via [arrangement].
             * Note that it is not allowed to add items both inside and outside align block at the same time
             * meaning all items should either be part of aligned groups or there should not be aligned groups at all.
             *
             * @param alignment the alignment of the group. Most commonly it should be an instance of
             * [androidx.compose.ui.Alignment.Horizontal] or [androidx.compose.ui.Alignment.Vertical].
             * @param arrangement the arrangement of the items in this group.
             * @param block the builder block of this aligned group.
             */
            fun aligned(
                alignment: Alignment,
                arrangement: Arrangement? = null,
                block: () -> Unit,
            ) {
                if (activeAlignmentItemProviderList.isNotEmpty()) {
                    error("It is not allowed to add items both inside and outside align block at the same time.")
                }
                itemProviderMapping[alignment]?.let {
                    error("Aligned block with alignment = $alignment already exists.")
                }
                activeAlignment = alignment
                block()
                itemProviderMapping[alignment] = AlignmentProviderData(
                    arrangement = arrangement,
                    items = activeAlignmentItemProviderList.toList(),
                )
                activeAlignment = null
                activeAlignmentItemProviderList.clear()
            }
        }

        /**
         * A scope that allows only modifications on the original [ListBuilder].
         */
        class Modify<Item : EditorComponent<*>, Alignment : Any, Arrangement : Any> internal constructor(
            private val source: ListBuilder<Item, Alignment, Arrangement>,
        ) : ListBuilder<Item, Alignment, Arrangement>() {
            private val addFirstProviderMapping: MutableMap<Alignment?, LinkedList<ScopedProperty<EditorScope, Item>>> =
                mutableMapOf()
            private val addLastProviderMapping: MutableMap<Alignment?, LinkedList<ScopedProperty<EditorScope, Item>>> =
                mutableMapOf()
            private val addAfterProviderMapping: MutableMap<EditorComponentId, LinkedList<ScopedProperty<EditorScope, Item>>> =
                mutableMapOf()
            private val addBeforeProviderMapping: MutableMap<EditorComponentId, LinkedList<ScopedProperty<EditorScope, Item>>> =
                mutableMapOf()
            private val replaceItemProviderMapping: MutableMap<EditorComponentId, ScopedProperty<EditorScope, Item>> =
                mutableMapOf()
            private val removeList: LinkedList<EditorComponentId> = LinkedList()

            internal var builder: Modify<Item, Alignment, Arrangement>.() -> Unit = {}

            private fun getItemError(
                operation: String,
                id: String,
            ): String =
                "$operation was invoked with id=$id which does not exist in the source ListBuilder or is already removed via remove API."

            private fun getAlignmentError(
                operation: String,
                alignment: Alignment?,
                sourceAlignments: Set<Alignment?>,
            ): String = when {
                alignment == null -> {
                    "$operation was invoked without alignment, however source ListBuilder contains only aligned items."
                }
                sourceAlignments.contains(null) -> {
                    "$operation was invoked with alignment, however source ListBuilder contains only non-aligned items."
                }
                else -> {
                    "$operation was invoked with alignment=$alignment, however source ListBuilder does not contain such alignment."
                }
            }

            private inline fun <T> List<T>.ensureIsEmpty(message: (T) -> String) {
                if (isNotEmpty()) error(message(first()))
            }

            private inline fun <T> Map<T, *>.ensureIsEmpty(message: (T) -> String) {
                if (isNotEmpty()) error(message(keys.first()))
            }

            @Composable
            override fun EditorScope.buildLocal() = mutableMapOf<Alignment?, AlignmentData<Item, Arrangement>>().apply {
                // Every recomposition calls the block, fills up the item list and then clears it
                builder()
                val sourceResult = source.build(this@buildLocal)
                sourceResult.forEach { (alignment, data) ->
                    this[alignment] = AlignmentData(
                        arrangement = data.arrangement,
                        items = buildList {
                            addFirstProviderMapping.remove(alignment)?.map { it() }?.let(::addAll)
                            data.items.forEach forEachInner@{ item ->
                                // Try remove item first, then do other operations.
                                if (removeList.remove(item.id)) return@forEachInner
                                addBeforeProviderMapping.remove(item.id)?.map { it() }?.let(::addAll)
                                add(replaceItemProviderMapping.remove(item.id)?.invoke(this@buildLocal) ?: item)
                                addAfterProviderMapping.remove(
                                    item.id,
                                )?.map { it() }?.let(::addAll)
                            }
                            addLastProviderMapping.remove(alignment)?.map { it() }?.let(::addAll)
                        },
                    )
                }
                addFirstProviderMapping.ensureIsEmpty {
                    getAlignmentError(operation = "addFirst", alignment = it, sourceAlignments = sourceResult.keys)
                }
                addLastProviderMapping.ensureIsEmpty {
                    getAlignmentError(operation = "addLast", alignment = it, sourceAlignments = sourceResult.keys)
                }
                removeList.ensureIsEmpty {
                    getItemError(operation = "remove", id = it.id)
                }
                addBeforeProviderMapping.ensureIsEmpty {
                    getItemError(operation = "addBefore", id = it.id)
                }
                addAfterProviderMapping.ensureIsEmpty {
                    getItemError(operation = "addAfter", id = it.id)
                }
                replaceItemProviderMapping.ensureIsEmpty {
                    getItemError(operation = "replace", id = it.id)
                }
                addFirstProviderMapping.clear()
                addLastProviderMapping.clear()
                replaceItemProviderMapping.clear()
            }

            /**
             * Appends a new [EditorComponent] item in the list.
             * Note that adding items to the list does not mean displaying. The items will be displayed if [EditorComponent.visible]
             * is true for them.
             * Also note that [EditorScope] in the [block] builder is the scope of the parent [EditorComponent].
             *
             * @param block a building block that returns an item that should be added to the list at the back.
             */
            fun addLast(block: ScopedProperty<EditorScope, Item>) {
                val items = addLastProviderMapping.getAlignmentItems(null, operation = "addLast")
                items.add(block)
            }

            /**
             * Appends a new [EditorComponent] item in the list of items that are aligned by [alignment].
             * Note that adding items to the list does not mean displaying. The items will be displayed if [EditorComponent.visible]
             * is true for them.
             * Also note that [EditorScope] in the [block] builder is the scope of the parent [EditorComponent].
             *
             * @param block a building block that returns an item that should be added to the list at the back.
             */
            fun addLast(
                alignment: Alignment,
                block: ScopedProperty<EditorScope, Item>,
            ) {
                val items = addLastProviderMapping.getAlignmentItems(alignment, operation = "addLast")
                items.add(block)
            }

            /**
             * Prepends a new [EditorComponent] item in the list.
             * Note that adding items to the list does not mean displaying. The items will be displayed if [EditorComponent.visible]
             * is true for them.
             * Also note that [EditorScope] in the [block] builder is the scope of the parent [EditorComponent].
             *
             * @param block a building block that returns an item that should be added to the list at the front.
             */
            fun addFirst(block: ScopedProperty<EditorScope, Item>) {
                val items = addFirstProviderMapping.getAlignmentItems(null, operation = "addFirst")
                items.add(0, block)
            }

            /**
             * Prepends a new [EditorComponent] item in the list of items that are aligned by [alignment].
             * Note that adding items to the list does not mean displaying. The items will be displayed if [EditorComponent.visible]
             * is true for them.
             * Also note that [EditorScope] in the [block] builder is the scope of the parent [EditorComponent].
             *
             * @param block a building block that returns an item that should be added to the list at the back.
             */
            fun addFirst(
                alignment: Alignment,
                block: ScopedProperty<EditorScope, Item>,
            ) {
                val items = addFirstProviderMapping.getAlignmentItems(alignment, operation = "addFirst")
                items.add(block)
            }

            private fun MutableMap<Alignment?, LinkedList<ScopedProperty<EditorScope, Item>>>.getAlignmentItems(
                alignment: Alignment?,
                operation: String,
            ): LinkedList<ScopedProperty<EditorScope, Item>> {
                if (alignment == null && any { (otherAlignment) -> otherAlignment != null }) {
                    error(
                        "$operation is already invoked with alignment value. ListBuilder cannot have aligned and non-aligned" +
                            " items at the same time. Consider invoking the overloaded function with correct alignment value.",
                    )
                }
                if (alignment != null && any { (otherAlignment) -> otherAlignment == null }) {
                    error(
                        "$operation is already invoked without alignment value. ListBuilder cannot have aligned and non-aligned" +
                            " items at the same time. Consider invoking the overloaded function without any alignment value.",
                    )
                }
                return getOrPut(alignment) { LinkedList() }
            }

            /**
             * Adds a new [EditorComponent] item right after previously added [EditorComponent] with [id].
             * Note that adding items to the list does not mean displaying. The items will be displayed if [EditorComponent.visible]
             * is true for them.
             * Also note that [EditorScope] in the [block] builder is the scope of the parent [EditorComponent].
             *
             * @param block a building block that returns an item that should be added to the list.
             */
            fun addAfter(
                id: EditorComponentId,
                block: ScopedProperty<EditorScope, Item>,
            ) {
                val newList = addAfterProviderMapping.getOrPut(id) { LinkedList() }
                newList.add(0, block)
            }

            /**
             * Adds a new [EditorComponent] item right before previously added [EditorComponent] with [id].
             * Note that adding items to the list does not mean displaying. The items will be displayed if [EditorComponent.visible]
             * is true for them.
             * Also note that [EditorScope] in the [block] builder is the scope of the parent [EditorComponent].
             *
             * @param block a building block that returns an item that should be added to the list.
             */
            fun addBefore(
                id: EditorComponentId,
                block: ScopedProperty<EditorScope, Item>,
            ) {
                val newList = addBeforeProviderMapping.getOrPut(id) { LinkedList() }
                newList.add(block)
            }

            /**
             * Replaces the [EditorComponent] with id = [id] that was previously added in the [source] [ListBuilder].
             * Note that [EditorScope] in the [block] builder is the scope of the parent [EditorComponent].
             *
             * @param id the id of the component that should be replaced.
             * @param block a building block that returns an item that should replace.
             */
            fun replace(
                id: EditorComponentId,
                block: ScopedProperty<EditorScope, Item>,
            ) {
                replaceItemProviderMapping[id] = block
            }

            /**
             * Removes the [EditorComponent] with id = [id] that was previously added in the [source] [ListBuilder].
             *
             * @param id the id of the component that should be removed.
             */
            fun remove(id: EditorComponentId) {
                removeList.add(id)
            }
        }

        companion object
    }
}

/**
 * Implementation of [EditorComponentBuilder] where content is empty.
 * Useful when component does not need to have any special structure and UI can be provided
 * via [decoration].
 */
abstract class NoContentEditorComponentBuilder<Scope : EditorScope> : EditorComponentBuilder<EditorComponent<Scope>, Scope>() {
    @Composable
    override fun build(
        scope: Scope,
        id: EditorComponentId,
        modifier: Modifier,
        visible: Boolean,
        enterTransition: EnterTransition,
        exitTransition: ExitTransition,
        decoration: ScopedDecoration<Scope>,
    ): EditorComponent<Scope> = remember(
        scope,
        id,
        modifier,
        visible,
        enterTransition,
        exitTransition,
        decoration,
    ) {
        object : EditorComponent<Scope>() {
            override val scope: Scope = scope
            override val id: EditorComponentId = id
            override val modifier: Modifier = modifier
            override val visible: Boolean = visible
            override val enterTransition: EnterTransition = enterTransition
            override val exitTransition: ExitTransition = exitTransition
            override val decoration: ScopedDecoration<Scope> = decoration

            @Composable
            override fun Scope.Content(animatedVisibilityScope: AnimatedVisibilityScope?) = Unit
        }
    }
}

/**
 * Base builder class of all [EditorComponent]s.
 */
@Stable
abstract class EditorComponentBuilder<Target : EditorComponent<Scope>, Scope : EditorScope> {
    /**
     * Scope of this component. Every new value will trigger recomposition of all [ScopedProperty]s
     * such as [visible], [enterTransition], [exitTransition] etc.
     * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for granular
     * recompositions over updating the scope, since scope change triggers full recomposition of the component.
     * Ideally, scope should be updated when the parent scope (scope of the parent component) is updated and when you want to
     * observe changes from the [Engine].
     * Property is abstract.
     */
    abstract var scope: ScopedProperty<EditorScope, Scope>

    /**
     * Unique id of this component.
     * By default property is not initialized.
     */
    open var id: ScopedProperty<Scope, EditorComponentId> = {
        remember { EditorComponentId(UUID.randomUUID().toString()) }
    }

    /**
     * Modifier of this component.
     * By default empty Modifier is applied.
     */
    open var modifier: ScopedProperty<Scope, Modifier> = { Modifier }

    /**
     * Whether the component should be visible.
     * By default component is always visible.
     */
    open var visible: ScopedProperty<Scope, Boolean> = alwaysVisible

    /**
     * Transition of the component when it enters the parent composable.
     * By default no transition is applied.
     */
    open var enterTransition: ScopedProperty<Scope, EnterTransition> = noneEnterTransition

    /**
     * Transition of the component when it exits the parent composable.
     * By default no transition is applied.
     */
    open var exitTransition: ScopedProperty<Scope, ExitTransition> = noneExitTransition

    /**
     * Decoration of this component. Useful when you want to add custom background, foreground, shadow, paddings etc.
     * By default no decoration is applied.
     */
    open var decoration: ScopedDecoration<Scope> = emptyDecoration

    @Composable
    abstract fun build(
        scope: Scope,
        id: EditorComponentId,
        modifier: Modifier,
        visible: Boolean,
        enterTransition: EnterTransition,
        exitTransition: ExitTransition,
        decoration: ScopedDecoration<Scope>,
    ): Target

    @Composable
    fun build(): Target {
        val parentScope = LocalEditorScope.current
        val childScope = scope(parentScope)
        return build(
            scope = childScope,
            id = id(childScope),
            modifier = modifier(childScope),
            visible = visible(childScope),
            enterTransition = enterTransition(childScope),
            exitTransition = exitTransition(childScope),
            decoration = decoration,
        )
    }
}

/**
 * Predicate to be used when the [EditorComponent] is always visible.
 */
val alwaysVisible: ScopedProperty<EditorScope, Boolean> = { true }

/**
 * A helper lambda for getting [EnterTransition.None] in the [EditorScope].
 */
val noneEnterTransition: ScopedProperty<EditorScope, EnterTransition> = { EnterTransition.None }

/**
 * A helper lambda for getting [ExitTransition.None] in the [EditorScope].
 */
val noneExitTransition: ScopedProperty<EditorScope, ExitTransition> = { ExitTransition.None }

/**
 * Predicate to be used when the [EditorComponent] is always enabled.
 */
val alwaysEnabled: ScopedProperty<EditorScope, Boolean> = { true }

/**
 * A helper lambda that represents no decoration.
 */
val emptyDecoration: ScopedDecoration<EditorScope> = { it() }

/**
 * The content of the component.
 */
@Composable
fun <Scope : EditorScope> EditorComponent(
    component: EditorComponent<Scope>,
    modifier: Modifier = Modifier,
    onHide: () -> Unit = {},
) {
    EditorComponent(modifier, component, onHide) { visible, enter, exit, content ->
        AnimatedVisibility(
            visible = visible,
            enter = enter,
            exit = exit,
        ) {
            content()
        }
    }
}

/**
 * The content of the component when rendered in a [ColumnScope].
 * Prefer using this overload over without [ColumnScope] when the component is being rendered in a column.
 */
@Composable
fun <Scope : EditorScope> ColumnScope.EditorComponent(
    component: EditorComponent<Scope>,
    modifier: Modifier = Modifier,
    onHide: () -> Unit = {},
) {
    EditorComponent(modifier, component, onHide) { visible, enter, exit, content ->
        AnimatedVisibility(
            visible = visible,
            enter = enter,
            exit = exit,
        ) {
            content()
        }
    }
}

/**
 * The content of the component when rendered in a [RowScope].
 * Prefer using this overload over without [RowScope] when the component is being rendered in a row.
 */
@Composable
fun <Scope : EditorScope> RowScope.EditorComponent(
    component: EditorComponent<Scope>,
    modifier: Modifier = Modifier,
    onHide: () -> Unit = {},
) {
    EditorComponent(modifier, component, onHide) { visible, enter, exit, content ->
        AnimatedVisibility(
            visible = visible,
            enter = enter,
            exit = exit,
        ) {
            content()
        }
    }
}

@Composable
private fun <Scope : EditorScope> EditorComponent(
    modifier: Modifier,
    component: EditorComponent<Scope>,
    onHide: () -> Unit,
    animatedVisibility: @Composable (
        Boolean,
        EnterTransition,
        ExitTransition,
        @Composable AnimatedVisibilityScope.() -> Unit,
    ) -> Unit,
) = component.run {
    Box(modifier) {
        CompositionLocalProvider(LocalEditorScope provides scope) {
            // AnimatedVisibility is unstable and causes unexpected behaviors sometimes.
            // It is best to avoid it unless enterTransition and exitTransition are provided.
            if (enterTransition == EnterTransition.None && exitTransition == ExitTransition.None) {
                if (visible) {
                    component.decoration(scope) {
                        scope.ContentInternal(null)
                    }
                } else {
                    onHide()
                }
            } else {
                animatedVisibility(visible, enterTransition, exitTransition) {
                    component.decoration(scope) {
                        scope.ContentInternal(this)
                    }
                }
            }
        }
    }
}

/**
 * A composable overload for [EditorComponent.Companion.remember] where the component's scope is automatically [EditorScope] instance.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the building block of custom [EditorComponent].
 * @return fully custom [EditorComponent]. Use [EditorComponent.decoration] for rendering your custom content.
 */
@Composable
fun EditorComponent.Companion.remember(
    builder: EditorComponentBuilder<EditorComponent<EditorScope>, EditorScope>.() -> Unit = {},
): EditorComponent<EditorScope> = androidx.compose.runtime.remember {
    object : NoContentEditorComponentBuilder<EditorScope>() {
        override var scope: ScopedProperty<EditorScope, EditorScope> = {
            remember(this) { EditorScope(this) }
        }
    }.apply(builder)
}.build()

/**
 * A composable function that creates and remembers a fully custom editor component.
 * Use this function whenever you want to provide a fully custom UI for [EditorComponent].
 * For instance, you can replace the content of the inspector bar:
 *
 * ```kotlin
 * EditorConfiguration.remember {
 *    inspectorBar = EditorComponent.remember {
 *        id = { EditorComponentId("my.package.inspectorBar") }
 *        decoration = {
 *            // [this] references to [InspectorBar.Scope]
 *            // Custom UI here
 *        }
 *    )
 * )
 * ```
 * Note that both [builderFactory] and [builder] lambdas run only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builderFactory the factory that should be used to construct [EditorComponent].
 * @param builder the builder block that configures the [EditorComponent].
 * @return fully custom [EditorComponent]. Use [EditorComponent.decoration] for rendering your custom content.
 */
@Composable
fun <Builder : EditorComponentBuilder<EditorComponent<Scope>, Scope>, Scope : EditorScope> EditorComponent.Companion.remember(
    builderFactory: () -> Builder,
    builder: Builder.() -> Unit = {},
): EditorComponent<Scope> = androidx.compose.runtime.remember { builderFactory().apply(builder) }.build()

/**
 * A composable function that creates and remembers a [EditorComponent.ListBuilder] instance.
 *
 * @param builder the building block of [EditorComponent.ListBuilder].
 * @return a new [EditorComponent.ListBuilder] instance.
 */
@Composable
fun <Item : EditorComponent<*>, Alignment : Any, Arrangement : Any> EditorComponent.ListBuilder.Companion.remember(
    builder: EditorComponent.ListBuilder.New<Item, Alignment, Arrangement>.() -> Unit,
): EditorComponent.ListBuilder<Item, Alignment, Arrangement> = androidx.compose.runtime.remember {
    EditorComponent.ListBuilder.New<Item, Alignment, Arrangement>()
}.also { it.builder = builder }

/**
 * A composable function that modifies existing [EditorComponent.ListBuilder].
 * Useful if you want to apply modifications to the original builder, without touching the original builder.
 *
 * The example below is based on the [Dock] component but it is exactly the same for all the other components that
 * contain [EditorComponent.ListBuilder].
 *
 * ```kotlin
 * dock = {
 *     Dock.remember {
 *         listBuilder = {
 *             // Makes sense to use only with builders that are already available and cannot be modified by you directly.
 *             val existingListBuilder = Dock.ListBuilder.remember {
 *                 add { Dock.Button.rememberSystemGallery() }
 *                 add { Dock.Button.rememberSystemCamera() }
 *                 add { Dock.Button.rememberTextLibrary() }
 *                 add { Dock.Button.rememberShapesLibrary() }
 *             }
 *             existingListBuilder.modify {
 *                 addLast {
 *                     Dock.Button.remember {
 *                         id = { EditorComponentId("my.package.dock.button.last") }
 *                         vectorIcon = { IconPack.Music }
 *                         text = { "Last Button" }
 *                         onClick = {}
 *                     }
 *                 }
 *                 addFirst {
 *                     Dock.Button.remember {
 *                         id = { EditorComponentId("my.package.dock.button.first") }
 *                         vectorIcon = { IconPack.Music }
 *                         text = { "First Button" }
 *                         onClick = {}
 *                     }
 *                 }
 *                 addAfter(id = Dock.Button.Id.systemGallery) {
 *                     Dock.Button.remember {
 *                         id = { EditorComponentId("my.package.dock.button.afterSystemGallery") }
 *                         vectorIcon = { IconPack.Music }
 *                         text = { "After System Gallery" }
 *                         onClick = {}
 *                     }
 *                 }
 *                 addBefore(id = Dock.Button.Id.systemCamera) {
 *                     Dock.Button.remember {
 *                         id = { EditorComponentId("my.package.dock.button.beforeSystemCamera") }
 *                         vectorIcon = { IconPack.Music }
 *                         text = { "Before System Camera" }
 *                         onClick = {}
 *                     }
 *                 }
 *                 replace(id = Dock.Button.Id.textLibrary) {
 *                      Dock.Button.remember {
 *                          id = { EditorComponentId("my.package.dock.button.replacedTextLibrary") }
 *                          vectorIcon = null
 *                          text = { "Replaced Text Library" }
 *                          onClick = {}
 *                      }
 *                 }
 *                 remove(id = Dock.Button.Id.shapesLibrary)
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * In case the existingListBuilder contains aligned blocks, overrides of [EditorComponent.ListBuilder.Modify.addFirst]
 * and [EditorComponent.ListBuilder.Modify.addLast] should be used that accept alignment parameter:
 *
 * ```kotlin
 * dock = {
 *     Dock.remember {
 *         listBuilder = {
 *             Dock.ListBuilder.remember {
 *                 aligned(alignment = Alignment.Start) {
 *                     add { Dock.Button.rememberSystemGallery() }
 *                     add { Dock.Button.rememberSystemCamera() }
 *                 }
 *                 aligned(alignment = Alignment.End) {
 *                     add { Dock.Button.rememberTextLibrary() }
 *                     add { Dock.Button.rememberShapesLibrary() }
 *                 }
 *             }.modify {
 *                 addLast(alignment = Alignment.Start) {
 *                     Dock.Button.remember {
 *                         id = { EditorComponentId("my.package.dock.button.last") }
 *                         vectorIcon = { IconPack.Music }
 *                         textString = { "Last Button" }
 *                         onClick = {}
 *                     }
 *                 }
 *                 addFirst(alignment = Alignment.End) {
 *                     Dock.Button.remember {
 *                         id = { EditorComponentId("my.package.dock.button.first") }
 *                         vectorIcon = { IconPack.Music }
 *                         textString = { "First Button" }
 *                         onClick = {}
 *                     }
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * @param builder the building block of [EditorComponent.ListBuilder].
 * @return a new [EditorComponent.ListBuilder] instance.
 */
@Composable
fun <Item : EditorComponent<*>, Alignment : Any, Arrangement : Any> EditorComponent.ListBuilder<Item, Alignment, Arrangement>.modify(
    builder: EditorComponent.ListBuilder.Modify<Item, Alignment, Arrangement>.() -> Unit,
): EditorComponent.ListBuilder<Item, Alignment, Arrangement> = androidx.compose.runtime.remember {
    EditorComponent.ListBuilder.Modify(source = this)
}.also { it.builder = builder }
