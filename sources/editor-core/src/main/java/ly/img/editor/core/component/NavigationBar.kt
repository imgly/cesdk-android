@file:Suppress("UnusedReceiverParameter")

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ly.img.editor.core.EditorScope
import ly.img.editor.core.ScopedDecoration
import ly.img.editor.core.ScopedProperty
import ly.img.editor.core.component.HorizontalListBuilder
import ly.img.editor.core.component.data.Nothing
import ly.img.editor.core.component.data.nothing
import ly.img.editor.core.configuration.remember
import ly.img.engine.Engine

/**
 * A component for rendering the navigation bar at the top of the editor.
 * Use [NavigationBar.Companion.remember] composable function to create an instance of this class.
 * Check [AbstractNavigationBarBuilder] and its superclasses to see what each property does.
 */
@Stable
data class NavigationBar<Scope : NavigationBar.Scope>(
    override val scope: Scope,
    override val id: EditorComponentId,
    override val modifier: Modifier,
    override val visible: Boolean,
    override val enterTransition: EnterTransition,
    override val exitTransition: ExitTransition,
    override val decoration: ScopedDecoration<Scope>,
    val listBuilder: HorizontalListBuilder<EditorComponent<*>>,
    val horizontalArrangement: Arrangement.Horizontal,
    val itemDecoration: ScopedDecoration<Scope>,
) : EditorComponent<Scope>() {
    /**
     * Scope of the [NavigationBar] component.
     *
     * @param parentScope the scope of the parent component.
     */
    @Stable
    open class Scope(
        parentScope: EditorScope,
    ) : EditorScope(parentScope)

    /**
     * Scope of the [Button] component inside the [NavigationBar].
     *
     * @param parentScope the scope of the parent component.
     */
    @Stable
    open class ItemScope(
        parentScope: EditorScope,
    ) : EditorScope(parentScope)

    /**
     * Builder class of [Button] component inside the [NavigationBar].
     */
    @Stable
    open class ButtonBuilder : AbstractButtonBuilder<ItemScope>() {
        /**
         * Scope of this component. Every new value will trigger recomposition of all [ScopedProperty]s
         * such as [visible], [enterTransition], [exitTransition] etc.
         * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for granular
         * recompositions over updating the scope, since scope change triggers full recomposition of the component.
         * Ideally, scope should be updated when the parent scope (scope of the parent component) is updated and when you want to
         * observe changes from the [Engine].
         * By default it is updated only when the parent scope (accessed via `this`) is updated.
         */
        override var scope: ScopedProperty<EditorScope, ItemScope> = {
            remember(this) { ItemScope(parentScope = this) }
        }

        /**
         * Modifier of this component.
         * By default size modifiers are applied.
         */
        override var modifier: ScopedProperty<ItemScope, Modifier> = {
            Modifier.height(64.dp)
        }

        /**
         * Content padding of the button.
         * By default no paddings are applied.
         */
        override var contentPadding: ScopedProperty<ItemScope, PaddingValues> = {
            PaddingValues(0.dp)
        }
    }

    // todo replace with nested typealias with kotlin 2.0 bump
    object Button {
        object Id
    }

    // todo replace with nested typealias with kotlin 2.0 bump
    object ListBuilder

    @Composable
    override fun Scope.Content(animatedVisibilityScope: AnimatedVisibilityScope?) {
        val alignedData = listBuilder.build(this)
        Box(
            modifier = modifier,
            contentAlignment = Alignment.Center,
        ) {
            alignedData.forEach { (alignment, data) ->
                // Main list
                if (alignment == null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = horizontalArrangement,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Items(data.items)
                    }
                } else {
                    // List with alignments
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.align(alignment),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = data.arrangement ?: Arrangement.Start,
                        ) {
                            Items(data.items)
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun Scope.Items(items: List<EditorComponent<*>>) {
        items.forEach {
            itemDecoration {
                EditorComponent(component = it)
            }
        }
    }

    companion object
}

/**
 * Builder class for [NavigationBar] where the scope is [NavigationBar.Scope].
 */
@Stable
open class NavigationBarBuilder : AbstractNavigationBarBuilder<NavigationBar.Scope>() {
    /**
     * Scope of this component. Every new value will trigger recomposition of all [ScopedProperty]s
     * such as [visible], [enterTransition], [exitTransition] etc.
     * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for granular
     * recompositions over updating the scope, since scope change triggers full recomposition of the component.
     * Ideally, scope should be updated when the parent scope (scope of the parent component) is updated and when you want to
     * observe changes from the [Engine].
     * By default it is updated only when the parent scope (accessed via `this`) is updated.
     */
    override var scope: ScopedProperty<EditorScope, NavigationBar.Scope> = {
        remember(this) { NavigationBar.Scope(parentScope = this) }
    }
}

/**
 * Abstract builder class for [NavigationBar].
 */
@Stable
abstract class AbstractNavigationBarBuilder<Scope : NavigationBar.Scope> : EditorComponentBuilder<NavigationBar<Scope>, Scope>() {
    /**
     * Unique id of this component.
     * By default the value is "ly.img.component.navigationBar".
     */
    override var id: ScopedProperty<Scope, EditorComponentId> = {
        EditorComponentId("ly.img.component.navigationBar")
    }

    /**
     * Decoration of the navigation bar. Useful when you want to add custom background, foreground, shadow, paddings etc.
     * Default value is [NavigationBar.Companion.DefaultDecoration].
     */
    override var decoration: ScopedDecoration<Scope> = {
        NavigationBar.DefaultDecoration(content = it)
    }

    /**
     * A list builder that builds a list of [EditorComponent]s that should be part of the navigation bar.
     * Note that adding items to the list does not mean displaying. The items will be displayed if [EditorComponent.visible] is true for them.
     * Also note that items will be rebuilt when [scope] is updated.
     * By default listBuilder does not add anything to the navigation bar.
     */
    var listBuilder: ScopedProperty<Scope, HorizontalListBuilder<EditorComponent<*>>> = {
        NavigationBar.ListBuilder.remember { }
    }

    /**
     * Horizontal arrangement that should be used to render the items in the navigation bar horizontally.
     * Note that the value will be ignored in case [listBuilder] contains aligned items. Check [EditorComponent.ListBuilder.New.aligned] for more
     * details on how to configure arrangement of aligned items.
     * Default value is [Arrangement.Start].
     */
    var horizontalArrangement: ScopedProperty<Scope, Arrangement.Horizontal> = { Arrangement.Start }

    /**
     * Decoration of the items in the navigation bar. Useful when you want to add custom background, foreground, shadow,
     * paddings etc to the items. Prefer using this decoration when you want to apply the same decoration to all the items, otherwise
     * set decoration to individual items.
     * Default value is an empty decoration.
     */
    var itemDecoration: ScopedDecoration<Scope> = emptyDecoration

    @Composable
    override fun build(
        scope: Scope,
        id: EditorComponentId,
        modifier: Modifier,
        visible: Boolean,
        enterTransition: EnterTransition,
        exitTransition: ExitTransition,
        decoration: ScopedDecoration<Scope>,
    ): NavigationBar<Scope> {
        val listBuilder = listBuilder(scope)
        val horizontalArrangement = horizontalArrangement(scope)
        return remember(
            scope,
            id,
            modifier,
            visible,
            enterTransition,
            exitTransition,
            decoration,
            listBuilder,
            horizontalArrangement,
        ) {
            NavigationBar(
                id = id,
                scope = scope,
                modifier = modifier,
                visible = visible,
                enterTransition = enterTransition,
                exitTransition = exitTransition,
                decoration = decoration,
                listBuilder = listBuilder,
                horizontalArrangement = horizontalArrangement,
                itemDecoration = itemDecoration,
            )
        }
    }
}

/**
 * Default decoration of the navigation bar.
 * Sets a background color and applies paddings to the navigation bar by adding a containing box.
 *
 * @param background the background of the containing box.
 * @param paddingValues the padding values of the containing box.
 * @param content the content of the navigation bar.
 */
@Composable
fun NavigationBar.Companion.DefaultDecoration(
    `_`: Nothing = nothing,
    background: Color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95F),
    paddingValues: PaddingValues = PaddingValues(horizontal = 4.dp),
    `__`: Nothing = nothing,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .background(background)
            .padding(paddingValues),
    ) {
        content()
    }
}

/**
 * A composable overload for [NavigationBar.Companion.remember] that uses [NavigationBarBuilder] to create and remember a [NavigationBar] instance.
 * Check the documentation of overloaded [NavigationBar.Companion.remember] function below for more details.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder block that configures the [NavigationBar].
 * @return a navigation bar that will be displayed when launching an editor.
 */
@Composable
fun NavigationBar.Companion.remember(builder: NavigationBarBuilder.() -> Unit = {}): NavigationBar<NavigationBar.Scope> =
    remember(::NavigationBarBuilder, builder)

/**
 * A composable function that creates and remembers a [NavigationBar] instance.
 *
 * For example, if you want to have a navigation bar with the following functionality:
 *  - 1. Use NavigationBar.Button.rememberElementsLibrary button with a different icon.
 *  - 2. Add 2 custom buttons.
 *  - 3. Update first custom button text when second custom button is clicked with an incremented value.
 *  - 4. Show NavigationBar.Button.rememberStickersLibrary when the counter is even.
 *  - 5. Force update all items on any engine event (that will be obvious from first custom button random icon).
 * you should invoke [NavigationBar.Companion.remember] with [NavigationBar.listBuilder] looking like this:
 *
 * ```kotlin
 * navigationBar = {
 *     var counter by remember { mutableStateOf(0) }
 *     NavigationBar.remember {
 *         scope = {
 *             val eventTrigger by EditorTrigger.remember {
 *                 editorContext.engine.event.subscribe()
 *             }
 *             remember(this, eventTrigger) {
 *                 NavigationBar.Scope(parentScope = this)
 *             }
 *         }
 *         listBuilder = {
 *             NavigationBar.ListBuilder.remember {
 *                 add {
 *                     NavigationBar.Button.remember {
 *                         id = { EditorComponentId("my.package.navigationBar.button.custom1") }
 *                         vectorIcon = { listOf(IconPack.Music, IconPack.PlayBox).random() }
 *                         textString = { "Custom1 $counter" }
 *                         onClick = {}
 *                     }
 *                 }
 *                 add {
 *                     NavigationBar.Button.rememberElementsLibrary {
 *                         vectorIcon = { IconPack.Music }
 *                     }
 *                 }
 *                 add { NavigationBar.Button.rememberSystemGallery() }
 *                 add {
 *                     NavigationBar.Button.remember {
 *                         id = { EditorComponentId("my.package.navigationBar.button.custom2") }
 *                         vectorIcon = { IconPack.PlayBox }
 *                         textString = { "Custom2" }
 *                         onClick = { counter++ }
 *                     }
 *                 }
 *                 add { NavigationBar.Button.rememberImagesLibrary() }
 *                 add {
 *                     NavigationBar.Button.rememberStickersLibrary {
 *                         visible = { counter % 2 == 0 }
 *                     }
 *                 }
 *                 add { NavigationBar.Button.rememberTextLibrary() }
 *                 add { NavigationBar.Button.rememberResizeAll() }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * If you do not want to have the items as a single list, but rather as a set of separate aligned lists,
 * you can use the `aligned` function to specify the alignment of each sublist. For example:
 *
 * ```kotlin
 * navigationBar = {
 *     NavigationBar.remember {
 *         listBuilder = {
 *             NavigationBar.ListBuilder.remember {
 *                 aligned(alignment = Alignment.Start) {
 *                     add { NavigationBar.Button.rememberElementsLibrary() }
 *                     add { NavigationBar.Button.rememberSystemGallery() }
 *                 }
 *                 aligned(alignment = Alignment.End) {
 *                     add { NavigationBar.Button.rememberTextLibrary() }
 *                     add { NavigationBar.Button.rememberResizeAll() }
 *                 }
 *             }
 *         }
 *     }
 * }
 * ```
 *
 * In addition, if you already have an existing builder (either from starter kits or you created it yourself),
 * you can modify it by adding, removing, or replacing items using [EditorComponent.ListBuilder.modify] API. Check
 * [EditorComponent.ListBuilder.modify] documentation for more details.
 * Note that both [builderFactory] and [builder] lambdas run only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builderFactory the factory that should be used to construct [NavigationBar].
 * @param builder the builder block that configures the [NavigationBar].
 * @return a navigation bar that will be displayed when launching an editor.
 */
@Composable
fun <Scope : NavigationBar.Scope, Builder : AbstractNavigationBarBuilder<Scope>> NavigationBar.Companion.remember(
    builderFactory: () -> Builder,
    builder: Builder.() -> Unit = {},
): NavigationBar<Scope> = androidx.compose.runtime.remember { builderFactory().apply(builder) }.build()

/**
 * A composable function that creates and remembers a [NavigationBar.ListBuilder] instance.
 *
 * @param builder the building block of [NavigationBar.ListBuilder].
 * @return a new [NavigationBar.ListBuilder] instance.
 */
@Composable
fun NavigationBar.ListBuilder.remember(
    builder: HorizontalListBuilderScope<EditorComponent<*>>.() -> Unit,
): HorizontalListBuilder<EditorComponent<*>> = HorizontalListBuilder.remember(builder)

/**
 * A composable overload for [NavigationBar.Button.remember] that uses [AbstractButtonBuilder] to create and remember an [Button] instance.
 * Check the documentation of overloaded [NavigationBar.Button.remember] function below for more details.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder block that configures the [Button].
 * @return a button that will be displayed in the navigation bar.
 */
@Composable
fun NavigationBar.Button.remember(builder: NavigationBar.ButtonBuilder.() -> Unit = {}): Button<NavigationBar.ItemScope> =
    Button.remember({ NavigationBar.ButtonBuilder() }, builder)
