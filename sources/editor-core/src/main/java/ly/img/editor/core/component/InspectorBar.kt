@file:Suppress("UnusedReceiverParameter")

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import ly.img.editor.core.EditorContext
import ly.img.editor.core.EditorScope
import ly.img.editor.core.R
import ly.img.editor.core.ScopedDecoration
import ly.img.editor.core.ScopedProperty
import ly.img.editor.core.UnstableEditorApi
import ly.img.editor.core.component.data.Nothing
import ly.img.editor.core.component.data.Selection
import ly.img.editor.core.component.data.nothing
import ly.img.editor.core.compose.rememberLastValue
import ly.img.editor.core.configuration.remember
import ly.img.editor.core.iconpack.Close
import ly.img.editor.core.iconpack.IconPack
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.sheet.SheetValue
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine

/**
 * A component for rendering the inspector bar at the bottom of the editor.
 * Use [InspectorBar.Companion.remember] composable function to create an instance of this class.
 * Check [AbstractInspectorBarBuilder] and its superclasses to see what each property does.
 */
@Stable
data class InspectorBar<Scope : InspectorBar.Scope>(
    override val scope: Scope,
    override val id: EditorComponentId,
    override val modifier: Modifier,
    override val visible: Boolean,
    override val enterTransition: EnterTransition,
    override val exitTransition: ExitTransition,
    override val decoration: ScopedDecoration<Scope>,
    val listBuilder: HorizontalListBuilder<EditorComponent<*>>,
    val horizontalArrangement: Arrangement.Horizontal,
    val itemsRowEnterTransition: EnterTransition,
    val itemsRowExitTransition: ExitTransition,
    val itemDecoration: ScopedDecoration<Scope>,
) : EditorComponent<Scope>() {
    /**
     * Scope of the [InspectorBar] component.
     *
     * @param parentScope the scope of the parent component.
     * @param selection current selection in the editor.
     * @param editMode current edit mode in the editor.
     */
    @Stable
    open class Scope(
        parentScope: EditorScope,
        private val selection: Selection?,
        private val editMode: String,
    ) : EditorScope(parentScope) {
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
         * Current edit mode of the editor.
         */
        val EditorContext.editMode: String
            get() = this@Scope.editMode
    }

    /**
     * Scope of the items inside the [InspectorBar].
     *
     * @param parentScope the scope of the parent component.
     */
    @Stable
    open class ItemScope(
        parentScope: EditorScope,
    ) : EditorScope(parentScope) {
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
     * Builder class of [Button] component inside the [InspectorBar].
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
            val parentScope = this as Scope
            rememberLastValue(parentScope) {
                if (editorContext.safeSelection == null) lastValue else ItemScope(parentScope = parentScope)
            }
        }

        /**
         * Modifier of this component.
         * By default size modifiers are applied.
         */
        override var modifier: ScopedProperty<ItemScope, Modifier> = {
            Modifier
                .widthIn(min = 64.dp)
                .height(64.dp)
        }
    }

    // todo replace with nested typealias with kotlin 2.0 bump
    object Button {
        object Id
    }

    // todo replace with nested typealias with kotlin 2.0 bump
    object ListBuilder

    @OptIn(ExperimentalAnimationApi::class)
    @Composable
    override fun Scope.Content(animatedVisibilityScope: AnimatedVisibilityScope?) {
        val alignedData = listBuilder.build(this)
        Box(modifier = modifier) {
            val animationModifier = animatedVisibilityScope?.run {
                Modifier.animateEnterExit(
                    enter = itemsRowEnterTransition,
                    exit = itemsRowExitTransition,
                )
            } ?: Modifier
            alignedData.forEach { (alignment, data) ->
                // Main list
                if (alignment == null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState())
                            .then(animationModifier),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = horizontalArrangement,
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
 * Builder class for [InspectorBar] where the scope is [InspectorBar.Scope].
 */
@Stable
open class InspectorBarBuilder : AbstractInspectorBarBuilder<InspectorBar.Scope>() {
    /**
     * Scope of this component. Every new value will trigger recomposition of all [ScopedProperty]s
     * such as [visible], [enterTransition], [exitTransition] etc.
     * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas
     * for granular recompositions over updating the scope, since scope change triggers full recomposition of the inspector bar.
     * Also prefer updating individual [EditorComponent]s over updating the whole [InspectorBar].
     * Ideally, scope should be updated when the parent scope (scope of the parent component) is updated and when you want to
     * observe changes from the [Engine].
     * By default [InspectorBar.Companion.rememberDefaultScope] is used.
     */
    override var scope: ScopedProperty<EditorScope, InspectorBar.Scope> = {
        InspectorBar.rememberDefaultScope(parentScope = this)
    }
}

/**
 * Abstract builder class for [InspectorBar].
 */
@Stable
abstract class AbstractInspectorBarBuilder<Scope : InspectorBar.Scope> : EditorComponentBuilder<InspectorBar<Scope>, Scope>() {
    /**
     * Unique id of this component.
     * By default the value is "ly.img.component.inspectorBar".
     */
    override var id: ScopedProperty<Scope, EditorComponentId> = {
        EditorComponentId("ly.img.component.inspectorBar")
    }

    /**
     * Whether the component should be visible.
     * Default value is true if a block is selected, the edit mode is not "Crop" and the active sheet is not [SheetType.Voiceover].
     * The inspector bar is also visible when the voiceover sheet is closing.
     */
    @OptIn(UnstableEditorApi::class)
    override var visible: ScopedProperty<Scope, Boolean> = {
        val state by editorContext.state.collectAsState()
        val sheetTargetValue = state.activeSheetState?.targetValue
        remember(editorContext.safeSelection, editorContext.editMode, state.activeSheet, sheetTargetValue) {
            val isVoiceoverSheetClosing = state.activeSheet is SheetType.Voiceover && sheetTargetValue == SheetValue.Hidden
            val selection = editorContext.safeSelection
            selection != null &&
                editorContext.engine.block.isValid(selection.designBlock) &&
                editorContext.editMode != "Crop" &&
                (state.activeSheet !is SheetType.Voiceover || isVoiceoverSheetClosing)
        }
    }

    /**
     * Transition of the component when it enters the parent composable.
     * Default value is a vertical slide in transition.
     */
    override var enterTransition: ScopedProperty<Scope, EnterTransition> = {
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
     * Transition of the component when it exits the parent composable.
     * Default value is a vertical slide out transition.
     */
    override var exitTransition: ScopedProperty<Scope, ExitTransition> = {
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
     * Decoration of this component. Useful when you want to add custom background, foreground, shadow, paddings etc.
     * Default value is [InspectorBar.Companion.DefaultDecoration].
     */
    override var decoration: ScopedDecoration<Scope> = {
        InspectorBar.DefaultDecoration(scope = this, content = it)
    }

    /**
     * A list builder that builds a list of [EditorComponent]s that should be part of the inspector bar.
     * Note that adding items to the list does not mean displaying. The items will be displayed if [EditorComponent.visible] is true for them.
     * Also note that items will be rebuilt when [scope] is updated.
     * By default listBuilder does not add anything to the inspector bar.
     */
    var listBuilder: ScopedProperty<Scope, HorizontalListBuilder<EditorComponent<*>>> = {
        InspectorBar.ListBuilder.remember { }
    }

    /**
     * Horizontal arrangement that should be used to render the items in the inspector bar horizontally.
     * Note that the value will be ignored in case [listBuilder] contains aligned items. Check [EditorComponent.ListBuilder.New.aligned] for more
     * details on how to configure arrangement of aligned items.
     * Default value is [Arrangement.Start].
     */
    open var horizontalArrangement: ScopedProperty<Scope, Arrangement.Horizontal> = { Arrangement.Start }

    /**
     * Transition of the items row only (without close button) when [enterTransition] is running.
     * Default value is a horizontal slide in transition.
     */
    open var itemsRowEnterTransition: ScopedProperty<Scope, EnterTransition> = {
        remember {
            slideInHorizontally(
                animationSpec = tween(400, easing = CubicBezierEasing(0.05F, 0.7F, 0.1F, 1F)),
                initialOffsetX = { it / 3 },
            )
        }
    }

    /**
     * Transition of the items row only (without close button) when [exitTransition] is running.
     * Default value is always no exit transition.
     */
    open var itemsRowExitTransition: ScopedProperty<Scope, ExitTransition> = noneExitTransition

    /**
     * Decoration of the items in the inspector bar. Useful when you want to add custom background, foreground, shadow,
     * paddings etc to the items. Prefer using this decoration when you want to apply the same decoration to all the items, otherwise
     * set decoration to individual items.
     * Default value is an empty decoration.
     */
    open var itemDecoration: ScopedDecoration<Scope> = emptyDecoration

    @Composable
    override fun build(
        scope: Scope,
        id: EditorComponentId,
        modifier: Modifier,
        visible: Boolean,
        enterTransition: EnterTransition,
        exitTransition: ExitTransition,
        decoration: ScopedDecoration<Scope>,
    ): InspectorBar<Scope> {
        val listBuilder = listBuilder(scope)
        val horizontalArrangement = horizontalArrangement(scope)
        val itemsRowEnterTransition = itemsRowEnterTransition(scope)
        val itemsRowExitTransition = itemsRowExitTransition(scope)
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
            itemsRowEnterTransition,
            itemsRowExitTransition,
        ) {
            InspectorBar(
                scope = scope,
                id = id,
                modifier = modifier,
                visible = visible,
                enterTransition = enterTransition,
                exitTransition = exitTransition,
                decoration = decoration,
                listBuilder = listBuilder,
                horizontalArrangement = horizontalArrangement,
                itemsRowEnterTransition = itemsRowEnterTransition,
                itemsRowExitTransition = itemsRowExitTransition,
                itemDecoration = itemDecoration,
            )
        }
    }
}

/**
 * The default scope of the inspector bar. The value is updated when:
 * 1. Parent scope is updated.
 * 2. Selection is updated.
 *
 * @param parentScope the current scope that should be used to construct the new scope.
 * @param selectedDesignBlock the currently selected design block that should be used in the inspector bar.
 * This lambda can be used to control what is the currently selected design block in the scope of the inspector bar.
 * For instance, returning null will make sure inspector bar is not shown.
 * By default the lambda returns the first selected design block of the engine
 * unless it is of type [DesignBlockType.Page] and "page/selectWhenNoBlocksSelected" setting is enabled.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("UnusedFlow")
@Composable
fun InspectorBar.Companion.rememberDefaultScope(
    `_`: Nothing = nothing,
    parentScope: EditorScope,
    selectedDesignBlock: EditorScope.() -> DesignBlock? = {
        editorContext.engine.block.findAllSelected().firstOrNull()?.takeIf {
            editorContext.engine.block.getType(it) != DesignBlockType.Page.key ||
                editorContext.engine.editor.getSettingBoolean("page/selectWhenNoBlocksSelected").not()
        }
    },
    `__`: Nothing = nothing,
): InspectorBar.Scope = parentScope.run {
    val initialSelection = androidx.compose.runtime.remember {
        selectedDesignBlock()
            ?.takeIf { editorContext.engine.block.isValid(it) }
            ?.let { Selection.getDefault(editorContext, it) }
    }
    val selection by remember(this) {
        editorContext.engine.block.onSelectionChanged()
            .flatMapLatest {
                val selectedDesignBlock = selectedDesignBlock() ?: return@flatMapLatest flowOf(null)
                editorContext.engine.event.subscribe(listOf(selectedDesignBlock))
                    .filter {
                        // When the design block is unselected/deleted, this lambda is entered before onSelectionChanged is emitted.
                        // We need to make sure that this flow does not emit previous selection in such scenario.
                        selectedDesignBlock == selectedDesignBlock() && editorContext.engine.block.isValid(selectedDesignBlock)
                    }
                    .map { Selection.getDefault(editorContext, selectedDesignBlock) }
                    .onStart { emit(Selection.getDefault(editorContext, selectedDesignBlock)) }
            }
    }.collectAsState(initial = initialSelection)
    val initialEditMode = androidx.compose.runtime.remember {
        editorContext.engine.editor.getEditMode()
    }
    val editMode by remember(this) {
        editorContext.engine.editor.onStateChanged()
            .map { editorContext.engine.editor.getEditMode() }
            .distinctUntilChanged()
            .onStart { emit(editorContext.engine.editor.getEditMode()) }
    }.collectAsState(initial = initialEditMode)
    remember(this, selection, editMode) {
        InspectorBar.Scope(
            parentScope = this,
            selection = selection,
            editMode = editMode,
        )
    }
}

/**
 * Default decoration of the inspector bar.
 * Sets a background color, applies paddings and adds a close button to the inspector bar by adding a containing box.
 *
 * @param background the background of the containing box.
 * @param paddingValues the padding values of the containing box.
 * @param content the content of the inspector bar.
 */
@Composable
fun InspectorBar.Companion.DefaultDecoration(
    `_`: Nothing = nothing,
    scope: InspectorBar.Scope,
    background: Color = MaterialTheme.colorScheme.surface,
    paddingValues: PaddingValues = PaddingValues(start = 16.dp, top = 10.dp, bottom = 10.dp),
    `__`: Nothing = nothing,
    content: @Composable () -> Unit,
) = scope.run {
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
                        Icon(
                            imageVector = IconPack.Close,
                            contentDescription = stringResource(id = R.string.ly_img_editor_inspector_bar_button_close),
                        )
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
 * A composable overload for [InspectorBar.Companion.remember] that uses [AbstractInspectorBarBuilder] to create and remember
 * an [InspectorBar] instance. Check the documentation of overloaded [InspectorBar.Companion.remember] function below
 * for more details.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder block that configures the [InspectorBar].
 * @return an inspector bar that will be displayed when a design block is selected.
 */
@Composable
fun InspectorBar.Companion.remember(builder: InspectorBarBuilder.() -> Unit = {}): InspectorBar<InspectorBar.Scope> =
    remember(::InspectorBarBuilder, builder)

/**
 * A composable function that creates and remembers an [InspectorBar] instance.
 *
 * For example, if you want to have an inspector bar with the following functionality:
 *  - 1. Use InspectorBar.Button.rememberAdjustments button with a different icon.
 *  - 2. Add 2 custom buttons.
 *  - 3. Update first custom button text when second custom button is clicked with an incremented value.
 *  - 4. Show InspectorBar.Button.rememberEffect when the counter is even.
 *  - 5. Force update all items on any engine event (that will be obvious from first custom button random icon).
 * you should invoke [InspectorBar.Companion.remember] with [InspectorBar.listBuilder] looking like this:
 *
 * ```kotlin
 * inspectorBar = {
 *     var counter by remember { mutableStateOf(0) }
 *     InspectorBar.remember {
 *         scope = {
 *             val eventTrigger by EditorTrigger.remember {
 *                 editorContext.engine.event.subscribe()
 *             }
 *             remember(this, eventTrigger) {
 *                 InspectorBar.Scope(parentScope = this)
 *             }
 *         }
 *         listBuilder = {
 *             InspectorBar.ListBuilder.remember {
 *                 add {
 *                     InspectorBar.Button.remember {
 *                         id = { EditorComponentId("my.package.inspectorBar.button.custom1") }
 *                         vectorIcon = { listOf(IconPack.Music, IconPack.PlayBox).random() }
 *                         textString = { "Custom1 $counter" }
 *                         onClick = {}
 *                     }
 *                 }
 *                 add {
 *                     InspectorBar.Button.rememberAdjustments {
 *                         vectorIcon = { IconPack.Music }
 *                     }
 *                 }
 *                 add { InspectorBar.Button.rememberCrop() }
 *                 add {
 *                     InspectorBar.Button.remember {
 *                         id = { EditorComponentId("my.package.inspectorBar.button.custom2") }
 *                         vectorIcon = { IconPack.PlayBox }
 *                         textString = { "Custom2" }
 *                         onClick = { counter++ }
 *                     }
 *                 }
 *                 add { InspectorBar.Button.rememberFillStroke() }
 *                 add {
 *                     InspectorBar.Button.rememberEffect {
 *                         visible = { counter % 2 == 0 }
 *                     }
 *                 }
 *                 add { InspectorBar.Button.rememberDelete() }
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
 * inspectorBar = {
 *     InspectorBar.remember {
 *         listBuilder = {
 *             InspectorBar.ListBuilder.remember {
 *                 aligned(alignment = Alignment.Start) {
 *                     add { InspectorBar.Button.rememberAdjustments() }
 *                     add { InspectorBar.Button.rememberCrop() }
 *                 }
 *                 aligned(alignment = Alignment.End) {
 *                     add { InspectorBar.Button.rememberEffect() }
 *                     add { InspectorBar.Button.rememberDelete() }
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
 * @param builderFactory the factory that should be used to construct [InspectorBar].
 * @param builder the builder block that configures the [InspectorBar].
 * @return an inspector bar that will be displayed when a design block is selected.
 */
@Composable
fun <Scope : InspectorBar.Scope, Builder : AbstractInspectorBarBuilder<Scope>> InspectorBar.Companion.remember(
    builderFactory: () -> Builder,
    builder: Builder.() -> Unit = {},
): InspectorBar<Scope> = androidx.compose.runtime.remember { builderFactory().apply(builder) }.build()

/**
 * A composable function that creates and remembers an [InspectorBar.ListBuilder] instance.
 *
 * @param builder the building block of [InspectorBar.ListBuilder].
 * @return a new [InspectorBar.ListBuilder] instance.
 */
@Composable
fun InspectorBar.ListBuilder.remember(
    builder: HorizontalListBuilderScope<EditorComponent<*>>.() -> Unit,
): HorizontalListBuilder<EditorComponent<*>> = HorizontalListBuilder.remember(builder)

/**
 * A composable overload for [InspectorBar.Button.remember] that uses [AbstractButtonBuilder] to create and remember
 * an [Button] instance. Check the documentation of overloaded [InspectorBar.Button.remember] function below
 * for more details.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder block that configures the [Button].
 * @return a button that will be displayed in the inspector bar.
 */
@Composable
fun InspectorBar.Button.remember(builder: InspectorBar.ButtonBuilder.() -> Unit = {}): Button<InspectorBar.ItemScope> =
    Button.remember({ InspectorBar.ButtonBuilder() }, builder)
