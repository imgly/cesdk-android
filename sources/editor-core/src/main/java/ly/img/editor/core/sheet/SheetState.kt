package ly.img.editor.core.sheet

import androidx.compose.runtime.Stable
import ly.img.editor.core.UnstableEditorApi

@Stable
@UnstableEditorApi
interface SheetState {
    /**
     * The current value of the [SheetState].
     */
    val currentValue: SheetValue

    /**
     * The target value. This is the closest value to the current offset (taking into account
     * positional thresholds). If no interactions like animations or drags are in progress, this
     * will be the current value.
     */
    val targetValue: SheetValue

    /**
     * The current height of the content in swipeable sheet.
     */
    val contentHeight: Int

    /**
     * The current offset, or null if it has not been initialized yet.
     *
     * During the first composition, the offset will be null. In subsequent compositions, the offset
     * will be derived from the anchors of the previous pass.
     * Always prefer accessing the offset from a LaunchedEffect as it will be scheduled to be
     * executed the next frame, after layout.
     *
     * To guarantee stricter semantics, consider using [requireOffset].
     */
    val offset: Float?

    /**
     * Require the current offset.
     *
     * @throws IllegalStateException If the offset has not been initialized yet
     */
    fun requireOffset(): Float

    /**
     * Whether an animation is currently in progress.
     */
    val isAnimationRunning: Boolean

    /**
     * The fraction of the progress going from [minOffset] to [maxOffset], within [0f..1f]
     * bounds.
     *
     * The value is null if [offset] is null or if the anchors are not initialized yet.
     */
    val progress: Float?

    /**
     * The velocity of the last known animation. Gets reset to 0f when an animation completes
     * successfully, but does not get reset when an animation gets interrupted.
     * You can use this value to provide smooth reconciliation behavior when re-targeting an
     * animation.
     */
    val lastVelocity: Float

    /**
     * The minimum offset this state can reach. This will be the smallest anchor, or
     * [Float.NEGATIVE_INFINITY] if the anchors are not initialized yet.
     */
    val minOffset: Float

    /**
     * The maximum offset this state can reach. This will be the biggest anchor, or
     * [Float.POSITIVE_INFINITY] if the anchors are not initialized yet.
     */
    val maxOffset: Float
}
