package ly.img.editor.core.component

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.RowScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisallowComposableCalls
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Alignment
import ly.img.editor.core.EditorContext
import ly.img.editor.core.EditorScope
import ly.img.editor.core.LocalEditorScope
import java.util.LinkedList

internal typealias HorizontalListBuilder<Item> = EditorComponent.ListBuilder<Item, Alignment.Horizontal, Arrangement.Horizontal>

internal typealias HorizontalListBuilderScope<Item> =
    EditorComponent.ListBuilder.Scope.New<Item, Alignment.Horizontal, Arrangement.Horizontal>

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
 * In order to render the component, use [EditorComponent] functions below.
 */
@Stable
abstract class EditorComponent<Scope : EditorScope> {
    /**
     * The unique id of the component.
     */
    abstract val id: EditorComponentId

    /**
     * The scope of this component. Every new value will trigger recomposition of all functions with signature @Composable Scope.() -> {},
     * such as [visible], [enterTransition], [exitTransition] etc.
     * If you need to access [EditorScope] to construct the scope, use [LocalEditorScope].
     */
    abstract val scope: Scope

    /**
     * Whether the component should be visible.
     */
    abstract val visible: @Composable Scope.() -> Boolean

    /**
     * The transition of the component when it enters the parent composable.
     */
    abstract val enterTransition: @Composable Scope.() -> EnterTransition

    /**
     * The transition of the component when it exits the parent composable.
     */
    abstract val exitTransition: @Composable Scope.() -> ExitTransition

    /**
     * The decoration of this component. Useful when you want to add custom background, foreground, shadow, paddings etc.
     */
    abstract val decoration: @Composable Scope.(@Composable () -> Unit) -> Unit

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

    /**
     * A utility class for building list of [EditorComponent]s.
     */
    @Stable
    class ListBuilder<Item : EditorComponent<*>, Alignment : Any, Arrangement : Any>
        @PublishedApi
        internal constructor(
            internal val scope: Scope<Item, Alignment, Arrangement>,
        ) {
            class AlignmentData<Item : EditorComponent<*>, Arrangement : Any>(
                val arrangement: Arrangement?,
                val items: List<Item>,
            )

            private class AlignmentProviderData<Item : EditorComponent<*>, Arrangement : Any>(
                val arrangement: Arrangement?,
                val items: List<@Composable EditorScope.() -> Item>,
            )

            /**
             * A scope class of the builder.
             */
            @Stable
            sealed class Scope<Item : EditorComponent<*>, Alignment : Any, Arrangement : Any> : EditorScope() {
                /**
                 * Final result of the builder.
                 */
                @get:Composable
                abstract val result: Map<Alignment?, AlignmentData<Item, Arrangement>>

                protected var editorContext: EditorContext? = null
                override val impl: EditorContext
                    get() = requireNotNull(editorContext)

                /**
                 * A scope that allows only appending new items.
                 */
                class New<Item : EditorComponent<*>, Alignment : Any, Arrangement : Any>
                    @PublishedApi
                    internal constructor(
                        private val block: @DisallowComposableCalls New<Item, Alignment, Arrangement>.() -> Unit,
                    ) : Scope<Item, Alignment, Arrangement>() {
                        private val itemProviderMapping: MutableMap<Alignment?, AlignmentProviderData<Item, Arrangement>> = mutableMapOf()
                        private val activeAlignmentItemProviderList: MutableList<@Composable EditorScope.() -> Item> = LinkedList()
                        private var activeAlignment: Alignment? = null

                        override val result: Map<Alignment?, AlignmentData<Item, Arrangement>>
                            @Composable
                            get() = mutableMapOf<Alignment?, AlignmentData<Item, Arrangement>>().apply {
                                val editorScope = LocalEditorScope.current.apply {
                                    this@New.editorContext = editorContext
                                }
                                // Every recomposition calls the block, fills up the item list and then clears it
                                block()
                                if (activeAlignmentItemProviderList.isNotEmpty()) {
                                    this[null] = AlignmentData(
                                        arrangement = null,
                                        items = activeAlignmentItemProviderList.map { it(editorScope) },
                                    )
                                    activeAlignmentItemProviderList.clear()
                                }
                                itemProviderMapping.forEach { (alignment, data) ->
                                    AlignmentData(
                                        arrangement = data.arrangement,
                                        items = data.items.map { it(editorScope) },
                                    ).let { this[alignment] = it }
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
                        fun add(block: @Composable EditorScope.() -> Item) {
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
                    private val block: @DisallowComposableCalls Modify<Item, Alignment, Arrangement>.() -> Unit,
                ) : Scope<Item, Alignment, Arrangement>() {
                    private val addFirstProviderMapping: MutableMap<Alignment?, LinkedList<@Composable EditorScope.() -> Item>> =
                        mutableMapOf()
                    private val addLastProviderMapping: MutableMap<Alignment?, LinkedList<@Composable EditorScope.() -> Item>> =
                        mutableMapOf()
                    private val addAfterProviderMapping: MutableMap<EditorComponentId, LinkedList<@Composable EditorScope.() -> Item>> =
                        mutableMapOf()
                    private val addBeforeProviderMapping: MutableMap<EditorComponentId, LinkedList<@Composable EditorScope.() -> Item>> =
                        mutableMapOf()
                    private val replaceItemProviderMapping: MutableMap<EditorComponentId, @Composable EditorScope.() -> Item> =
                        mutableMapOf()
                    private val removeList: LinkedList<EditorComponentId> = LinkedList()

                    private fun error(
                        operation: String,
                        key: String,
                        value: Any,
                    ) {
                        error(
                            "$operation was invoked with $key=$value which does not exist in the source ListBuilder or is already removed via remove API.",
                        )
                    }

                    override val result: Map<Alignment?, AlignmentData<Item, Arrangement>>
                        @Composable
                        get() = mutableMapOf<Alignment?, AlignmentData<Item, Arrangement>>().apply {
                            val editorScope = LocalEditorScope.current.apply {
                                this@Modify.editorContext = editorContext
                            }
                            // Every recomposition calls the block, fills up the item list and then clears it
                            block()
                            source.scope.result.mapValues { (alignment, data) ->
                                AlignmentData(
                                    arrangement = data.arrangement,
                                    items = buildList {
                                        addFirstProviderMapping.remove(alignment)?.map { it(editorScope) }?.let(::addAll)
                                        data.items.forEach { item ->
                                            // Try remove item first, then do other operations.
                                            if (removeList.remove(item.id)) return@forEach
                                            addBeforeProviderMapping.remove(item.id)?.map { it(editorScope) }?.let(::addAll)
                                            add(replaceItemProviderMapping.remove(item.id)?.invoke(editorScope) ?: item)
                                            addAfterProviderMapping.remove(
                                                item.id,
                                            )?.map { it(editorScope) }?.let(::addAll)
                                        }
                                        addLastProviderMapping.remove(alignment)?.map { it(editorScope) }?.let(::addAll)
                                    },
                                )
                            }
                            if (addFirstProviderMapping.isNotEmpty()) {
                                val alignment = addFirstProviderMapping.keys.first()
                                error(operation = "addFirst", key = "alignment", value = alignment ?: "")
                            }
                            if (addLastProviderMapping.isNotEmpty()) {
                                val alignment = addLastProviderMapping.keys.first()
                                error(operation = "addLast", key = "alignment", value = alignment ?: "")
                            }
                            if (removeList.isNotEmpty()) {
                                val id = removeList.first()
                                error(operation = "remove", key = "id", value = id.id)
                            }
                            if (addBeforeProviderMapping.isNotEmpty()) {
                                val id = addBeforeProviderMapping.keys.first()
                                error(operation = "addBefore", key = "id", value = id.id)
                            }
                            if (addAfterProviderMapping.isNotEmpty()) {
                                val id = addAfterProviderMapping.keys.first()
                                error(operation = "addAfter", key = "id", value = id.id)
                            }
                            if (replaceItemProviderMapping.isNotEmpty()) {
                                val id = replaceItemProviderMapping.keys.first()
                                error(operation = "replace", key = "id", value = id.id)
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
                    fun addLast(block: @Composable EditorScope.() -> Item) {
                        if (addLastProviderMapping.size > 1 || addLastProviderMapping.keys.first() != null) {
                            error(
                                "addLast is already invoked with alignment value. ListBuilder cannot have aligned and non-aligned groups" +
                                    " at the same time. Consider invoking the overloaded function with correct alignment value.",
                            )
                        }
                        val newList = addLastProviderMapping[null] ?: LinkedList()
                        newList.add(block)
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
                        block: @Composable EditorScope.() -> Item,
                    ) {
                        if (addLastProviderMapping[null] != null) {
                            error(
                                "addLast is already invoked without alignment value. ListBuilder cannot have aligned and non-aligned" +
                                    "groups at the same time. Consider invoking the overloaded function without any alignment value.",
                            )
                        }
                        val newList = addLastProviderMapping[alignment] ?: LinkedList()
                        newList.add(block)
                    }

                    /**
                     * Prepends a new [EditorComponent] item in the list.
                     * Note that adding items to the list does not mean displaying. The items will be displayed if [EditorComponent.visible]
                     * is true for them.
                     * Also note that [EditorScope] in the [block] builder is the scope of the parent [EditorComponent].
                     *
                     * @param block a building block that returns an item that should be added to the list at the front.
                     */
                    fun addFirst(block: @Composable EditorScope.() -> Item) {
                        if (addFirstProviderMapping.size > 1 || addFirstProviderMapping.keys.first() != null) {
                            error(
                                "addFirst is already invoked with alignment value. ListBuilder cannot have aligned and non-aligned items" +
                                    "at the same time. Consider invoking the overloaded function with correct alignment value.",
                            )
                        }
                        val newList = addFirstProviderMapping[null] ?: LinkedList()
                        newList.add(0, block)
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
                        block: @Composable EditorScope.() -> Item,
                    ) {
                        if (addFirstProviderMapping[null] != null) {
                            error(
                                "addFirst is already invoked without alignment value. ListBuilder cannot have aligned and non-aligned" +
                                    "items at the same time. Consider invoking the overloaded function without any alignment value.",
                            )
                        }
                        val newList = addFirstProviderMapping[alignment] ?: LinkedList()
                        newList.add(block)
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
                        block: @Composable EditorScope.() -> Item,
                    ) {
                        val newList = addAfterProviderMapping[id] ?: LinkedList()
                        newList.add(0, block)
                        addAfterProviderMapping[id] = newList
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
                        block: @Composable EditorScope.() -> Item,
                    ) {
                        val newList = addBeforeProviderMapping[id] ?: LinkedList()
                        newList.add(block)
                        addBeforeProviderMapping[id] = newList
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
                        block: @Composable EditorScope.() -> Item,
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
            }

            companion object {
                /**
                 * A composable function that creates and remembers a [ListBuilder] instance.
                 *
                 * @param block the building block of [ListBuilder].
                 * @return a new [ListBuilder] instance.
                 */
                @Composable
                @PublishedApi
                internal fun <Item : EditorComponent<*>, Alignment : Any, Arrangement : Any> remember(
                    block: @DisallowComposableCalls Scope.New<Item, Alignment, Arrangement>.() -> Unit,
                ): ListBuilder<Item, Alignment, Arrangement> = androidx.compose.runtime.remember {
                    ListBuilder(scope = Scope.New(block))
                }

                /**
                 * A composable function that modifies existing [ListBuilder].
                 * Useful if you want to apply modifications to the original builder, without touching the original builder.
                 *
                 * @param block the building block of [ListBuilder].
                 * @return a new [ListBuilder] instance.
                 */
                @Composable
                fun <Item : EditorComponent<*>, Alignment : Any, Arrangement : Any> ListBuilder<Item, Alignment, Arrangement>.modify(
                    block: @DisallowComposableCalls Scope.Modify<Item, Alignment, Arrangement>.() -> Unit,
                ): ListBuilder<Item, Alignment, Arrangement> = androidx.compose.runtime.remember {
                    ListBuilder(scope = Scope.Modify(this, block))
                }
            }
        }

    companion object {
        /**
         * Predicate to be used when the [EditorComponent] is always visible.
         */
        val alwaysVisible: @Composable EditorScope.() -> Boolean = { true }

        /**
         * A helper lambda for getting [EnterTransition.None] in the [EditorScope].
         */
        val noneEnterTransition: @Composable EditorScope.() -> EnterTransition = { EnterTransition.None }

        /**
         * A helper lambda for getting [EnterTransition.None] in the [EditorScope].
         */
        val noneExitTransition: @Composable EditorScope.() -> ExitTransition = { ExitTransition.None }
    }
}

/**
 * The content of the component.
 */
@Composable
fun <Scope : EditorScope> EditorComponent(component: EditorComponent<Scope>) {
    EditorComponent(component) { visible, enter, exit, content ->
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
fun <Scope : EditorScope> ColumnScope.EditorComponent(component: EditorComponent<Scope>) {
    EditorComponent(component) { visible, enter, exit, content ->
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
fun <Scope : EditorScope> RowScope.EditorComponent(component: EditorComponent<Scope>) {
    EditorComponent(component) { visible, enter, exit, content ->
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
    component: EditorComponent<Scope>,
    animatedVisibility: @Composable (
        Boolean,
        EnterTransition,
        ExitTransition,
        @Composable AnimatedVisibilityScope.() -> Unit,
    ) -> Unit,
) = component.run {
    val scope = component.scope
    CompositionLocalProvider(LocalEditorScope provides scope) {
        val visible = component.visible(scope)
        val enterTransition = enterTransition(scope)
        val exitTransition = exitTransition(scope)
        // AnimatedVisibility is unstable and causes unexpected behaviors sometimes.
        // It is best to avoid it unless enterTransition and exitTransition are provided.
        if (enterTransition == EnterTransition.None && exitTransition == ExitTransition.None) {
            if (visible) {
                component.decoration(scope) {
                    scope.ContentInternal(null)
                }
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
