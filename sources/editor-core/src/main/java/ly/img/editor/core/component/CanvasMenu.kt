@file:Suppress("UnusedReceiverParameter")

package ly.img.editor.core.component

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import ly.img.editor.core.EditorContext
import ly.img.editor.core.EditorScope
import ly.img.editor.core.ScopedDecoration
import ly.img.editor.core.ScopedProperty
import ly.img.editor.core.component.data.Nothing
import ly.img.editor.core.component.data.Selection
import ly.img.editor.core.component.data.nothing
import ly.img.editor.core.compose.rememberLastValue
import ly.img.editor.core.configuration.remember
import ly.img.editor.core.ui.toPx
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import kotlin.math.cos
import kotlin.math.roundToInt

/**
 * A component for rendering the canvas menu next to a design block when it is selected.
 * Use [CanvasMenu.Companion.remember] composable function to create an instance of this class.
 * Check [AbstractCanvasMenuBuilder] and its superclasses to see what each property does.
 */
@Stable
data class CanvasMenu<Scope : CanvasMenu.Scope>(
    override val scope: Scope,
    override val id: EditorComponentId,
    override val modifier: Modifier,
    override val visible: Boolean,
    override val enterTransition: EnterTransition,
    override val exitTransition: ExitTransition,
    override val decoration: ScopedDecoration<Scope>,
    val listBuilder: HorizontalListBuilder<EditorComponent<*>>,
    val itemDecoration: ScopedDecoration<Scope>,
) : EditorComponent<Scope>() {
    /**
     * Scope of the [CanvasMenu] component.
     *
     * @param parentScope the scope of the parent component.
     * @param selection current selection in the editor.
     */
    @Stable
    open class Scope(
        parentScope: EditorScope,
        private val selection: Selection?,
    ) : EditorScope(parentScope) {
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
            editorContext.engine.block.supportsPlaybackTime(page) && editorContext.engine.block.isPlaying(page)
        }

        private val _canSelectionMove by lazy {
            val selection = selection ?: return@lazy false
            editorContext.engine.block.isAllowedByScope(selection.designBlock, "layer/move") &&
                run {
                    selection.parentDesignBlock?.let {
                        DesignBlockType.get(editorContext.engine.block.getType(it)) == DesignBlockType.Track &&
                            editorContext.engine.block.isPageDurationSource(it)
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
         * Returns true if the scene is currently playing.
         */
        val EditorContext.isScenePlaying: Boolean
            get() = this@Scope._isScenePlaying
    }

    /**
     * Scope of the items inside the [CanvasMenu].
     *
     * @param parentScope the scope of the parent component.
     */
    @Stable
    open class ItemScope(
        private val parentScope: EditorScope,
    ) : EditorScope(parentScope) {
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
     * Builder class of [Button] component inside the [CanvasMenu].
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

    /**
     * Builder class of [Divider] component inside the [CanvasMenu].
     */
    @Stable
    open class DividerBuilder : AbstractDividerBuilder<ItemScope>() {
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
         * By default padding, size and background modifiers are applied.
         */
        override var modifier: ScopedProperty<ItemScope, Modifier> = {
            remember(this) {
                Modifier
                    .padding(horizontal = 8.dp)
                    .size(width = DividerDefaults.Thickness, height = 24.dp)
            }
        }
    }

    // todo replace with nested typealias with kotlin 2.0 bump
    object Divider

    // todo replace with nested typealias with kotlin 2.0 bump
    object ListBuilder

    @Composable
    override fun Scope.Content(animatedVisibilityScope: AnimatedVisibilityScope?) {
        val alignedData = listBuilder.build(this)
        Box(modifier = modifier) {
            alignedData.forEach { (alignment, data) ->
                // Main list
                if (alignment == null) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
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
 * Builder class for [CanvasMenu] where the scope is [CanvasMenu.Scope].
 */
@Stable
open class CanvasMenuBuilder : AbstractCanvasMenuBuilder<CanvasMenu.Scope>() {
    /**
     * Scope of this component. Every new value will trigger recomposition of all [ScopedProperty]s
     * such as [visible], [enterTransition], [exitTransition] etc.
     * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas
     * for granular recompositions over updating the scope, since scope change triggers full recomposition of the canvas menu.
     * Also prefer updating individual [EditorComponent]s over updating the whole [CanvasMenu].
     * Ideally, scope should be updated when the parent scope (scope of the parent component) is updated and when you want to
     * observe changes from the [Engine].
     * By default [CanvasMenu.Companion.rememberDefaultScope] is used.
     */
    override var scope: ScopedProperty<EditorScope, CanvasMenu.Scope> = {
        CanvasMenu.rememberDefaultScope(parentScope = this)
    }
}

/**
 * Abstract builder class for [CanvasMenu].
 */
@Stable
abstract class AbstractCanvasMenuBuilder<Scope : CanvasMenu.Scope> : EditorComponentBuilder<CanvasMenu<Scope>, Scope>() {
    /**
     * Unique id of this component.
     * By default the value is "ly.img.component.canvasMenu".
     */
    override var id: ScopedProperty<Scope, EditorComponentId> = {
        EditorComponentId("ly.img.component.canvasMenu")
    }

    /**
     * Whether the component should be visible.
     * Default value is true when touch is not active, no sheet is displayed currently, a design block is selected,
     * selected design block does not have a type [DesignBlockType.Audio] or [DesignBlockType.Page] and the keyboard is not visible.
     * In addition, selected design block should be visible at current playback time and containing scene should be on pause if design
     * block is selected in a video scene.
     */
    override var visible: ScopedProperty<Scope, Boolean> = {
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
    }

    /**
     * Transition of the component when it enters the parent composable.
     * Default value is always no enter transition.
     */
    override var enterTransition: ScopedProperty<Scope, EnterTransition> = noneEnterTransition

    /**
     * Transition of the component when it exits the parent composable.
     * Default value is always no exit transition.
     */
    override var exitTransition: ScopedProperty<Scope, ExitTransition> = noneExitTransition

    /**
     * Decoration of this component. Useful when you want to add custom background, foreground, shadow, paddings etc.
     * Default value is [CanvasMenu.Companion.DefaultDecoration].
     */
    override var decoration: ScopedDecoration<Scope> = {
        CanvasMenu.DefaultDecoration(scope = this, content = it)
    }

    /**
     * A builder that builds the list of [EditorComponent]s that should be part of the canvas menu.
     * Note that adding items to the list does not mean displaying. The items will be displayed if [EditorComponent.visible] is true for them.
     * Also note that items will be rebuilt when [scope] is updated.
     * By default listBuilder does not add anything to the canvas menu.
     */
    open var listBuilder: ScopedProperty<Scope, HorizontalListBuilder<EditorComponent<*>>> = {
        CanvasMenu.ListBuilder.remember { }
    }

    /**
     * Decoration of the items in the canvas menu. Useful when you want to add custom background, foreground, shadow,
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
    ): CanvasMenu<Scope> {
        val listBuilder = listBuilder(scope)
        return remember(
            scope,
            id,
            modifier,
            visible,
            enterTransition,
            exitTransition,
            decoration,
            listBuilder,
        ) {
            CanvasMenu(
                scope = scope,
                id = id,
                modifier = modifier,
                visible = visible,
                enterTransition = enterTransition,
                exitTransition = exitTransition,
                decoration = decoration,
                listBuilder = listBuilder,
                itemDecoration = itemDecoration,
            )
        }
    }
}

/**
 * The default scope of the canvas menu. The value is updated when:
 * 1. Parent scope is updated.
 * 2. Selection is updated.
 * 3. Scene playing status is updated (applicable in video scenes only).
 *
 * @param parentScope the current scope that should be used to construct the new scope.
 * @param selectedDesignBlock the currently selected design block that should be used in the canvas menu.
 * This lambda can be used to control what is the currently selected design block in the scope of the canvas menu.
 * For instance, returning null will make sure canvas menu is not shown.
 * By default the lambda returns the first selected design block of the engine.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("UnusedFlow")
@Composable
fun CanvasMenu.Companion.rememberDefaultScope(
    `_`: Nothing = nothing,
    parentScope: EditorScope,
    selectedDesignBlock: EditorScope.() -> DesignBlock? = { editorContext.engine.block.findAllSelected().firstOrNull() },
    `__`: Nothing = nothing,
): CanvasMenu.Scope = parentScope.run {
    val initial = androidx.compose.runtime.remember {
        selectedDesignBlock()
            ?.takeIf { editorContext.engine.block.isValid(it) }
            ?.let { Selection.getDefault(editorContext, it) }
    }
    val selection by remember(this) {
        editorContext.engine.block.onSelectionChanged()
            .flatMapLatest {
                val camera = editorContext.engine.block.findByType(DesignBlockType.Camera).first()
                val selectedDesignBlock = selectedDesignBlock() ?: return@flatMapLatest flowOf(null)
                val parentDesignBlock = editorContext.engine.block.getParent(selectedDesignBlock)
                val observableDesignBlocks = parentDesignBlock
                    ?.let { listOf(it, selectedDesignBlock) } ?: listOf(selectedDesignBlock)
                merge(
                    editorContext.engine.event.subscribe(observableDesignBlocks),
                    editorContext.engine.event.subscribe(listOf(camera)),
                    editorContext.engine.editor.onStateChanged()
                        .map { editorContext.engine.editor.getEditMode() }
                        .distinctUntilChanged(),
                    editorContext.state
                        .map { it.isTouchActive to (it.activeSheet != null) }
                        .distinctUntilChanged(),
                )
                    .filter {
                        // When the design block is unselected/deleted, this lambda is entered before onSelectionChanged is emitted.
                        // We need to make sure that this flow does not emit previous selection in such scenario.
                        selectedDesignBlock == selectedDesignBlock() && editorContext.engine.block.isValid(selectedDesignBlock)
                    }
                    .map { Selection.getDefault(editorContext, selectedDesignBlock) }
                    .onStart { emit(Selection.getDefault(editorContext, selectedDesignBlock)) }
            }
    }.collectAsState(initial = initial)

    val activeSceneTrigger by EditorTrigger.remember {
        editorContext.engine.scene.onActiveChanged()
    }

    val isPageChangedTrigger by EditorTrigger.remember(activeSceneTrigger) {
        editorContext.engine.event
            .subscribe()
            .map { editorContext.engine.scene.getCurrentPage() }
            .distinctUntilChanged()
    }

    val isScenePlayingTrigger by EditorTrigger.remember(isPageChangedTrigger) {
        editorContext.engine.scene.get() ?: return@remember emptyFlow<Boolean>()
        val page = editorContext.engine.scene.getCurrentPage() ?: return@remember emptyFlow<Boolean>()
        editorContext.engine.event
            .subscribe(listOf(page))
            .filter { editorContext.engine.block.isValid(page) }
            .map {
                if (editorContext.engine.block.supportsPlaybackTime(page)) {
                    editorContext.engine.block.isPlaying(page)
                } else {
                    false
                }
            }
            .distinctUntilChanged()
    }

    remember(this, selection, isScenePlayingTrigger) {
        // isScenePlayingTrigger (driven by page events) can change before the selection Flow
        // emits null after a block deletion, causing a new Scope with a stale selection.
        val validSelection = selection?.takeIf {
            editorContext.engine.block.isValid(it.designBlock)
        }
        CanvasMenu.Scope(parentScope = this, selection = validSelection)
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
fun CanvasMenu.Companion.DefaultDecoration(
    `_`: Nothing = nothing,
    scope: CanvasMenu.Scope,
    shape: Shape = MaterialTheme.shapes.extraLarge,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    shadowElevation: Dp = 1.dp,
    rotateHandleSize: Dp = 48.dp,
    verticalPadding: Dp = 24.dp,
    horizontalPadding: Dp = 16.dp,
    `__`: Nothing = nothing,
    content: @Composable () -> Unit,
) = scope.run {
    val editorState by editorContext.state.collectAsState()
    val blockSelection = editorContext.selection
    val selectedBlockRect = blockSelection.screenSpaceBoundingBoxRect
    if (selectedBlockRect != null && (selectedBlockRect.width().isNaN().not() && selectedBlockRect.height().isNaN().not())) {
        val rotateHandleSizePx = rotateHandleSize.toPx()
        val dy = remember(blockSelection, rotateHandleSize) {
            val isGizmoPresent = editorContext.engine.editor.getSettingBoolean("controlGizmo/showRotateHandles") ||
                editorContext.engine.editor.getSettingBoolean("controlGizmo/showMoveHandles")
            if (isGizmoPresent) {
                val rotation = editorContext.engine.block.getRotation(blockSelection.designBlock)
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
                        val canvasInsets = editorState.insets
                        val minY = constraints.minHeight + canvasInsets.top.roundToPx()
                        val topY =
                            (blockCenterY - blockHeight / 2).dp.roundToPx() - height - verticalPaddingPx + if (dy < 0) dy else 0
                        if (topY > minY) {
                            return@run topY
                        }
                        val bottomY = (blockCenterY + blockHeight / 2).dp.roundToPx() + verticalPaddingPx + if (dy > 0) dy else 0
                        val bottomCutOff = constraints.maxHeight - canvasInsets.bottom.roundToPx()
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
 * A composable overload for [CanvasMenu.Companion.remember] that uses [CanvasMenuBuilder] to create and remember
 * a [CanvasMenu] instance. Check the documentation of overloaded [CanvasMenu.Companion.remember] function below for
 * more details.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder block that configures the [CanvasMenu].
 * @return a canvas menu that will be displayed when a design block is selected.
 */
@Composable
fun CanvasMenu.Companion.remember(builder: CanvasMenuBuilder.() -> Unit = {}): CanvasMenu<CanvasMenu.Scope> =
    remember(::CanvasMenuBuilder, builder)

/**
 * A composable function that creates and remembers a [CanvasMenu] instance.
 *
 * For example, if you want to have a canvas menu with the following functionality:
 *  - 1. Use CanvasMenu.Button.rememberBringForward button with a different icon.
 *  - 2. Add 2 custom buttons.
 *  - 3. Update first custom button text when second custom button is clicked with an incremented value.
 *  - 4. Show CanvasMenu.Button.rememberDuplicate when the counter is even.
 *  - 5. Force update all items on any engine event (that will be obvious from first custom button random icon).
 * you should invoke [CanvasMenu.Companion.remember] with [CanvasMenu.listBuilder] looking like this:
 *
 * ```kotlin
 * canvasMenu = {
 *     var counter by remember { mutableStateOf(0) }
 *     CanvasMenu.remember {
 *         scope = {
 *             val eventTrigger by EditorTrigger.remember {
 *                 editorContext.engine.event.subscribe()
 *             }
 *             remember(this, eventTrigger) {
 *                 CanvasMenu.Scope(parentScope = this)
 *             }
 *         }
 *         listBuilder = {
 *             CanvasMenu.ListBuilder.remember {
 *                 add {
 *                     CanvasMenu.Button.remember {
 *                         id = { EditorComponentId("my.package.canvasMenu.button.custom1") }
 *                         vectorIcon = { listOf(IconPack.Music, IconPack.PlayBox).random() }
 *                         textString = { "Custom1 $counter" }
 *                         onClick = {}
 *                     }
 *                 }
 *                 add {
 *                     CanvasMenu.Button.rememberBringForward {
 *                         vectorIcon = { IconPack.Music }
 *                     }
 *                 }
 *                 add { CanvasMenu.Button.rememberSendBackward() }
 *                 add {
 *                     CanvasMenu.Button.remember {
 *                         id = { EditorComponentId("my.package.canvasMenu.button.custom2") }
 *                         vectorIcon = { IconPack.PlayBox }
 *                         textString = { "Custom2" }
 *                         onClick = { counter++ }
 *                     }
 *                 }
 *                 add {
 *                     CanvasMenu.Button.rememberDuplicate {
 *                         visible = { counter % 2 == 0 }
 *                     }
 *                 }
 *                 add { CanvasMenu.Button.rememberDelete() }
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
 * @param builderFactory the factory that should be used to construct [CanvasMenu].
 * @param builder the builder block that configures the [CanvasMenu].
 * @return a canvas menu that will be displayed when a design block is selected.
 */
@Composable
fun <Scope : CanvasMenu.Scope, Builder : AbstractCanvasMenuBuilder<Scope>> CanvasMenu.Companion.remember(
    builderFactory: () -> Builder,
    builder: Builder.() -> Unit = {},
): CanvasMenu<Scope> = androidx.compose.runtime.remember { builderFactory().apply(builder) }.build()

/**
 * A composable function that creates and remembers a [CanvasMenu.ListBuilder] instance.
 *
 * @param builder the building block of [CanvasMenu.ListBuilder].
 * @return a new [CanvasMenu.ListBuilder] instance.
 */
@Composable
fun CanvasMenu.ListBuilder.remember(
    builder: HorizontalListBuilderScope<EditorComponent<*>>.() -> Unit,
): HorizontalListBuilder<EditorComponent<*>> = HorizontalListBuilder.remember(builder)

/**
 * A composable overload for [CanvasMenu.Button.remember] that uses [AbstractButtonBuilder] to create and remember
 * an [Button] instance. Check the documentation of overloaded [CanvasMenu.Button.remember] function below
 * for more details.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder block that configures the [Button].
 * @return a button that will be displayed in the canvas menu.
 */
@Composable
fun CanvasMenu.Button.remember(builder: CanvasMenu.ButtonBuilder.() -> Unit = {}): Button<CanvasMenu.ItemScope> =
    Button.remember({ CanvasMenu.ButtonBuilder() }, builder)

/**
 * A composable overload for [CanvasMenu.Divider.remember] that uses [CanvasMenu.DividerBuilder] to create and remember a [Divider] instance.
 * Check the documentation of overloaded [CanvasMenu.Divider.remember] function below for more details.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder block that configures the [Divider].
 * @return a divider that will be displayed.
 */
@Composable
fun CanvasMenu.Divider.remember(builder: CanvasMenu.DividerBuilder.() -> Unit = {}): Divider<CanvasMenu.ItemScope> =
    Divider.remember({ CanvasMenu.DividerBuilder() }, builder)
