/*
 * Copyright 2020 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ly.img.editor.compose.bottomsheet

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.semantics.collapse
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.dismiss
import androidx.compose.ui.semantics.expand
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import ly.img.editor.compose.bottomsheet.ModalBottomSheetValue.Expanded
import ly.img.editor.compose.bottomsheet.ModalBottomSheetValue.HalfExpanded
import ly.img.editor.compose.bottomsheet.ModalBottomSheetValue.Hidden
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Possible values of [ModalBottomSheetState].
 */
enum class ModalBottomSheetValue {
    /**
     * The bottom sheet is not visible.
     */
    Hidden,

    /**
     * The bottom sheet is visible at full height.
     */
    Expanded,

    /**
     * The bottom sheet is partially visible at 50% of the screen height. This state is only
     * enabled if the height of the bottom sheet is more than 50% of the screen height.
     */
    HalfExpanded,
}

/**
 * State of the [ModalBottomSheetLayout] composable.
 *
 * @param initialValue The initial value of the state. <b>Must not be set to
 * [ModalBottomSheetValue.HalfExpanded] if [isSkipHalfExpanded] is set to true.</b>
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 * @param isSkipHalfExpanded Whether the half expanded state, if the sheet is tall enough, should
 * be skipped. If true, the sheet will always expand to the [Expanded] state and move to the
 * [Hidden] state when hiding the sheet, either programmatically or by user interaction.
 * <b>Must not be set to true if the initialValue is [ModalBottomSheetValue.HalfExpanded].</b>
 * If supplied with [ModalBottomSheetValue.HalfExpanded] for the initialValue, an
 * [IllegalArgumentException] will be thrown.
 */
@Suppress("Deprecation")
fun ModalBottomSheetState(
    initialValue: ModalBottomSheetValue,
    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
    confirmValueChange: (ModalBottomSheetValue) -> Boolean = { true },
    isSkipHalfExpanded: Boolean = false,
) = ModalBottomSheetState(
    initialValue = initialValue,
    animationSpec = animationSpec,
    isSkipHalfExpanded = isSkipHalfExpanded,
    confirmStateChange = confirmValueChange,
)

/**
 * State of the [ModalBottomSheetLayout] composable.
 *
 * @param initialValue The initial value of the state. <b>Must not be set to
 * [ModalBottomSheetValue.HalfExpanded] if [isSkipHalfExpanded] is set to true.</b>
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param isSkipHalfExpanded Whether the half expanded state, if the sheet is tall enough, should
 * be skipped. If true, the sheet will always expand to the [Expanded] state and move to the
 * [Hidden] state when hiding the sheet, either programmatically or by user interaction.
 * <b>Must not be set to true if the initialValue is [ModalBottomSheetValue.HalfExpanded].</b>
 * If supplied with [ModalBottomSheetValue.HalfExpanded] for the initialValue, an
 * [IllegalArgumentException] will be thrown.
 * @param confirmStateChange Optional callback invoked to confirm or veto a pending state change.
 */
class ModalBottomSheetState
    @Deprecated(
        message =
            "This constructor is deprecated. confirmStateChange has been renamed to " +
                "confirmValueChange.",
        replaceWith =
            ReplaceWith(
                "ModalBottomSheetState(" +
                    "initialValue, animationSpec, confirmStateChange, isSkipHalfExpanded)",
            ),
    )
    constructor(
        val swipeableState: SwipeableV2State<ModalBottomSheetValue>,
        internal val animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
        internal val isSkipHalfExpanded: Boolean = false,
    ) {

        constructor(
            initialValue: ModalBottomSheetValue,
            animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
            isSkipHalfExpanded: Boolean,
            confirmStateChange: (ModalBottomSheetValue) -> Boolean,
        ) : this (
            swipeableState = SwipeableV2State(
                initialValue = initialValue,
                animationSpec = animationSpec,
                confirmValueChange = confirmStateChange,
                positionalThreshold = PositionalThreshold,
            ),
            animationSpec = animationSpec,
            isSkipHalfExpanded = isSkipHalfExpanded
        ) {
            if (isSkipHalfExpanded) {
                require(initialValue != HalfExpanded) {
                    "The initial value must not be set to HalfExpanded if skipHalfExpanded is set to" +
                        " true."
                }
            }
        }

        val currentValue: ModalBottomSheetValue
            get() = swipeableState.currentValue

        val targetValue: ModalBottomSheetValue
            get() = swipeableState.targetValue

        /**
         * Whether the bottom sheet is visible.
         */
        val isVisible: Boolean
            get() = swipeableState.currentValue != Hidden

        internal val hasHalfExpandedState: Boolean
            get() = swipeableState.hasAnchorForValue(HalfExpanded)

        /**
         * Show the bottom sheet with animation and suspend until it's shown. If the sheet is taller
         * than 50% of the parent's height, the bottom sheet will be half expanded. Otherwise it will be
         * fully expanded.
         *
         * @throws [CancellationException] if the animation is interrupted
         */
        suspend fun show() {
            val targetValue =
                when {
                    hasHalfExpandedState -> HalfExpanded
                    else -> Expanded
                }
            animateTo(targetValue)
        }

        /**
         * Half expand the bottom sheet if half expand is enabled with animation and suspend until it
         * animation is complete or cancelled
         *
         * @throws [CancellationException] if the animation is interrupted
         */
        suspend fun halfExpand() {
            if (!hasHalfExpandedState) {
                return
            }
            animateTo(HalfExpanded)
        }

        /**
         * Fully expand the bottom sheet with animation and suspend until it if fully expanded or
         * animation has been cancelled.
         * *
         * @throws [CancellationException] if the animation is interrupted
         */
        suspend fun expand() {
            if (!swipeableState.hasAnchorForValue(Expanded)) {
                return
            }
            animateTo(Expanded)
        }

        /**
         * Hide the bottom sheet with animation and suspend until it if fully hidden or animation has
         * been cancelled.
         *
         * @throws [CancellationException] if the animation is interrupted
         */
        suspend fun hide() = animateTo(Hidden)

        suspend fun animateTo(
            target: ModalBottomSheetValue,
            velocity: Float = swipeableState.lastVelocity,
        ) = swipeableState.animateTo(target, velocity)

        suspend fun snapTo(target: ModalBottomSheetValue) = swipeableState.snapTo(target)

        internal fun requireOffset() = swipeableState.requireOffset()

        internal val lastVelocity: Float get() = swipeableState.lastVelocity

        internal val isAnimationRunning: Boolean get() = swipeableState.isAnimationRunning

        companion object {
            /**
             * The default [Saver] implementation for [ModalBottomSheetState].
             * Saves the [currentValue] and recreates a [ModalBottomSheetState] with the saved value as
             * initial value.
             */
            fun Saver(
                animationSpec: AnimationSpec<Float>,
                confirmValueChange: (ModalBottomSheetValue) -> Boolean,
                skipHalfExpanded: Boolean,
            ): Saver<ModalBottomSheetState, *> =
                Saver(
                    save = { it.currentValue },
                    restore = {
                        ModalBottomSheetState(
                            initialValue = it,
                            animationSpec = animationSpec,
                            isSkipHalfExpanded = skipHalfExpanded,
                            confirmValueChange = confirmValueChange,
                        )
                    },
                )
        }
    }

/**
 * Create a [ModalBottomSheetState] and [remember] it.
 *
 * @param initialValue The initial value of the state.
 * @param animationSpec The default animation that will be used to animate to a new state.
 * @param confirmValueChange Optional callback invoked to confirm or veto a pending state change.
 * @param skipHalfExpanded Whether the half expanded state, if the sheet is tall enough, should
 * be skipped. If true, the sheet will always expand to the [Expanded] state and move to the
 * [Hidden] state when hiding the sheet, either programmatically or by user interaction.
 * <b>Must not be set to true if the [initialValue] is [ModalBottomSheetValue.HalfExpanded].</b>
 * If supplied with [ModalBottomSheetValue.HalfExpanded] for the [initialValue], an
 * [IllegalArgumentException] will be thrown.
 */
@Composable
fun rememberModalBottomSheetState(
    initialValue: ModalBottomSheetValue,
    animationSpec: AnimationSpec<Float> = SwipeableDefaults.AnimationSpec,
    confirmValueChange: (ModalBottomSheetValue) -> Boolean = { true },
    skipHalfExpanded: Boolean = false,
): ModalBottomSheetState {
    // Key the rememberSaveable against the initial value. If it changed we don't want to attempt
    // to restore as the restored value could have been saved with a now invalid set of anchors.
    // b/152014032
    return key(initialValue) {
        rememberSaveable(
            initialValue,
            animationSpec,
            skipHalfExpanded,
            confirmValueChange,
            saver =
                ModalBottomSheetState.Saver(
                    animationSpec = animationSpec,
                    skipHalfExpanded = skipHalfExpanded,
                    confirmValueChange = confirmValueChange,
                ),
        ) {
            ModalBottomSheetState(
                initialValue = initialValue,
                animationSpec = animationSpec,
                isSkipHalfExpanded = skipHalfExpanded,
                confirmValueChange = confirmValueChange,
            )
        }
    }
}

/**
 * <a href="https://material.io/components/sheets-bottom#modal-bottom-sheet" class="external" target="_blank">Material Design modal bottom sheet</a>.
 *
 * Modal bottom sheets present a set of choices while blocking interaction with the rest of the
 * screen. They are an alternative to inline menus and simple dialogs, providing
 * additional room for content, iconography, and actions.
 *
 * ![Modal bottom sheet image](https://developer.android.com/images/reference/androidx/compose/material/modal-bottom-sheet.png)
 *
 * A simple example of a modal bottom sheet looks like this:
 *
 * @sample androidx.compose.material.samples.ModalBottomSheetSample
 *
 * @param sheetContent The content of the bottom sheet.
 * @param modifier Optional [Modifier] for the entire component.
 * @param sheetState The state of the bottom sheet.
 * @param sheetShape The shape of the bottom sheet.
 * @param sheetElevation The elevation of the bottom sheet.
 * @param sheetBackgroundColor The background color of the bottom sheet.
 * @param sheetContentColor The preferred content color provided by the bottom sheet to its
 * children. Defaults to the matching content color for [sheetBackgroundColor], or if that is not
 * a color from the theme, this will keep the same content color set above the bottom sheet.
 * @param content The content of rest of the screen.
 */
@Composable
fun ModalBottomSheetLayout(
    sheetContent: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    sheetState: ModalBottomSheetState = rememberModalBottomSheetState(Hidden),
    sheetShape: Shape = MaterialTheme.shapes.large,
    sheetElevation: Dp = ModalBottomSheetDefaults.Elevation,
    sheetBackgroundColor: Color = MaterialTheme.colorScheme.surface,
    sheetContentColor: Color = contentColorFor(sheetBackgroundColor),
    scrimEnabled: Boolean = false,
    dismissContentDescription: String = "",
    content: @Composable () -> Unit,
) {
    val scope = rememberCoroutineScope()
    val orientation = Orientation.Vertical
    val anchorChangeHandler =
        remember(sheetState, scope) {
            ModalBottomSheetAnchorChangeHandler(
                state = sheetState,
                animateTo = { target, velocity ->
                    scope.launch { sheetState.animateTo(target, velocity = velocity) }
                },
                snapTo = { target -> scope.launch { sheetState.snapTo(target) } },
            )
        }
    BoxWithConstraints(modifier) {
        val fullHeight = constraints.maxHeight.toFloat()
        Box(Modifier.fillMaxSize()) {
            content()
            Scrim(
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.32f),
                dismissContentDescription = dismissContentDescription,
                onDismiss = {
                    if (sheetState.swipeableState.confirmValueChange(Hidden)) {
                        scope.launch { sheetState.hide() }
                    }
                },
                visible = scrimEnabled && sheetState.swipeableState.targetValue != Hidden,
            )
        }
        Surface(
            modifier =
                Modifier
                    .align(Alignment.TopCenter) // We offset from the top so we'll center from there
                    .widthIn(max = MaxModalBottomSheetWidth)
                    .fillMaxWidth()
                    .nestedScroll(
                        remember(sheetState.swipeableState, orientation) {
                            ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
                                state = sheetState.swipeableState,
                                orientation = orientation,
                            )
                        },
                    )
                    .onSizeChanged { sheetState.swipeableState.contentHeight = it.height }
                    .offset {
                        // Could not use Modifier.animateContentSize in the content of modal bottom sheet due to
                        // https://stackoverflow.com/questions/70527258/android-compose-modalbottomsheetlayout-jumps-on-content-size-change.
                        // Bugfix is quite simple: in case the y offset is calculated wrongly, so that there's a gap below
                        // the content of the sheet (which causes items behind sheet being visible + shadow visible), then we ignore
                        // swipeableState.offset information and set offset in a way that the sheet is glued to the bottom of the screen.
                        val yOffset = sheetState.swipeableState
                            .requireOffset()
                            .roundToInt()
                        val finalYOffset = if (yOffset + sheetState.swipeableState.contentHeight < fullHeight.toInt()) {
                            fullHeight.toInt() - sheetState.swipeableState.contentHeight
                        } else yOffset
                        IntOffset(0, finalYOffset)
                    }
                    .swipeableV2(
                        state = sheetState.swipeableState,
                        orientation = orientation,
                        enabled = sheetState.swipeableState.currentValue != Hidden,
                    )
                    .swipeAnchors(
                        state = sheetState.swipeableState,
                        possibleValues = setOf(Hidden, HalfExpanded, Expanded),
                        anchorChangeHandler = anchorChangeHandler,
                    ) { state, sheetSize ->
                        when (state) {
                            Hidden -> fullHeight
                            HalfExpanded ->
                                when {
                                    sheetSize.height < fullHeight / 2f -> null
                                    sheetState.isSkipHalfExpanded -> null
                                    else -> fullHeight / 2f
                                }

                            Expanded ->
                                if (sheetSize.height != 0) {
                                    max(0f, fullHeight - sheetSize.height)
                                } else {
                                    null
                                }
                        }
                    }
                    .semantics {
                        if (sheetState.isVisible) {
                            dismiss {
                                if (sheetState.swipeableState.confirmValueChange(Hidden)) {
                                    scope.launch { sheetState.hide() }
                                }
                                true
                            }
                            if (sheetState.swipeableState.currentValue == HalfExpanded) {
                                expand {
                                    if (sheetState.swipeableState.confirmValueChange(Expanded)) {
                                        scope.launch { sheetState.expand() }
                                    }
                                    true
                                }
                            } else if (sheetState.hasHalfExpandedState) {
                                collapse {
                                    if (sheetState.swipeableState.confirmValueChange(HalfExpanded)) {
                                        scope.launch { sheetState.halfExpand() }
                                    }
                                    true
                                }
                            }
                        }
                    },
            shape = sheetShape,
            shadowElevation = sheetElevation,
            color = sheetBackgroundColor,
            contentColor = sheetContentColor,
        ) {
            Column {
                sheetContent()
            }
        }
    }
}

@Composable
private fun Scrim(
    color: Color,
    dismissContentDescription: String,
    onDismiss: () -> Unit,
    visible: Boolean,
) {
    if (color.isSpecified) {
        val alpha by animateFloatAsState(
            targetValue = if (visible) 1f else 0f,
            animationSpec = TweenSpec(),
        )
        val dismissModifier =
            if (visible) {
                Modifier
                    .pointerInput(onDismiss) { detectTapGestures { onDismiss() } }
                    .semantics(mergeDescendants = true) {
                        contentDescription = dismissContentDescription
                        onClick {
                            onDismiss()
                            true
                        }
                    }
            } else {
                Modifier
            }

        Canvas(
            Modifier
                .fillMaxSize()
                .then(dismissModifier),
        ) {
            drawRect(color = color, alpha = alpha)
        }
    }
}

/**
 * Contains useful Defaults for [ModalBottomSheetLayout].
 */
object ModalBottomSheetDefaults {
    /**
     * The default elevation used by [ModalBottomSheetLayout].
     */
    val Elevation = 16.dp
}

private fun ConsumeSwipeWithinBottomSheetBoundsNestedScrollConnection(
    state: SwipeableV2State<*>,
    orientation: Orientation,
): NestedScrollConnection =
    object : NestedScrollConnection {
        override fun onPreScroll(
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            val delta = available.toFloat()
            return if (delta < 0 && source == NestedScrollSource.Drag) {
                state.dispatchRawDelta(delta).toOffset()
            } else {
                Offset.Zero
            }
        }

        override fun onPostScroll(
            consumed: Offset,
            available: Offset,
            source: NestedScrollSource,
        ): Offset {
            return if (source == NestedScrollSource.Drag) {
                state.dispatchRawDelta(available.toFloat()).toOffset()
            } else {
                Offset.Zero
            }
        }

        override suspend fun onPreFling(available: Velocity): Velocity {
            val toFling = available.toFloat()
            val currentOffset = state.requireOffset()
            return if (toFling < 0 && currentOffset > state.minOffset) {
                state.settle(velocity = toFling)
                // since we go to the anchor with tween settling, consume all for the best UX
                available
            } else {
                Velocity.Zero
            }
        }

        override suspend fun onPostFling(
            consumed: Velocity,
            available: Velocity,
        ): Velocity {
            state.settle(velocity = available.toFloat())
            return available
        }

        private fun Float.toOffset(): Offset =
            Offset(
                x = if (orientation == Orientation.Horizontal) this else 0f,
                y = if (orientation == Orientation.Vertical) this else 0f,
            )

        @JvmName("velocityToFloat")
        private fun Velocity.toFloat() = if (orientation == Orientation.Horizontal) x else y

        @JvmName("offsetToFloat")
        private fun Offset.toFloat(): Float = if (orientation == Orientation.Horizontal) x else y
    }

private fun ModalBottomSheetAnchorChangeHandler(
    state: ModalBottomSheetState,
    animateTo: (target: ModalBottomSheetValue, velocity: Float) -> Unit,
    snapTo: (target: ModalBottomSheetValue) -> Unit,
) = AnchorChangeHandler<ModalBottomSheetValue> { previousTarget, previousAnchors, newAnchors ->
    val previousTargetOffset = previousAnchors[previousTarget]
    val newTarget =
        when (previousTarget) {
            Hidden -> Hidden
            HalfExpanded, Expanded -> {
                val hasHalfExpandedState = newAnchors.containsKey(HalfExpanded)
                val newTarget =
                    if (hasHalfExpandedState) {
                        HalfExpanded
                    } else if (newAnchors.containsKey(Expanded)) {
                        Expanded
                    } else {
                        Hidden
                    }
                newTarget
            }
        }
    val newTargetOffset = newAnchors.getValue(newTarget)
    if (newTargetOffset != previousTargetOffset) {
        if (state.isAnimationRunning) {
            // Re-target the animation to the new offset if it changed
            animateTo(newTarget, state.lastVelocity)
        } else {
            // Snap to the new offset value of the target if no animation was running
            snapTo(newTarget)
        }
    }
}

private val PositionalThreshold: Density.(Float) -> Float = { 56.dp.toPx() }
private val VelocityThreshold = 125.dp
private val MaxModalBottomSheetWidth = 640.dp
