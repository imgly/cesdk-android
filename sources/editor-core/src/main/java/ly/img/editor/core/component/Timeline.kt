@file:Suppress("UnusedReceiverParameter")

package ly.img.editor.core.component

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ly.img.editor.core.EditorScope
import ly.img.editor.core.ScopedDecoration
import ly.img.editor.core.ScopedProperty
import ly.img.editor.core.UnstableEditorApi
import ly.img.editor.core.component.data.Nothing
import ly.img.editor.core.component.data.nothing
import ly.img.editor.core.configuration.remember
import ly.img.editor.core.theme.surface1
import ly.img.engine.Engine

/**
 * A component for rendering the timeline.
 * Use [Timeline.Companion.remember] composable function to create an instance of this class.
 * Check [TimelineBuilder] and its superclasses to see what each property does.
 */
@Stable
data class Timeline(
    override val scope: Scope,
    override val id: EditorComponentId,
    override val modifier: Modifier,
    override val visible: Boolean,
    override val enterTransition: EnterTransition,
    override val exitTransition: ExitTransition,
    override val decoration: ScopedDecoration<Scope>,
) : EditorComponent<Timeline.Scope>() {
    @OptIn(UnstableEditorApi::class)
    @Composable
    override fun Scope.Content(animatedVisibilityScope: AnimatedVisibilityScope?) {
        (editorContext as TimelineOwner).TimelineContent()
    }

    /**
     * Scope of the [Timeline] component.
     *
     * @param parentScope the scope of the parent component.
     */
    @Stable
    open class Scope(
        parentScope: EditorScope,
    ) : EditorScope(parentScope)

    companion object
}

@UnstableEditorApi
@Stable
interface TimelineOwner {
    @UnstableEditorApi
    @Composable
    fun TimelineContent()
}

/**
 * Builder class for [Timeline].
 */
@Stable
open class TimelineBuilder : EditorComponentBuilder<Timeline, Timeline.Scope>() {
    /**
     * Scope of this component. Every new value will trigger recomposition of all [ScopedProperty]s
     * such as [visible], [enterTransition], [exitTransition] etc.
     * Consider using Compose [androidx.compose.runtime.State] objects in the lambdas for granular
     * recompositions over updating the scope, since scope change triggers full recomposition of the component.
     * Ideally, scope should be updated when the parent scope (scope of the parent component) is updated and when you want to
     * observe changes from the [Engine].
     * By default it is updated only when the parent scope (accessed via `this`) is updated.
     */
    override var scope: ScopedProperty<EditorScope, Timeline.Scope> = {
        remember(this) { Timeline.Scope(parentScope = this) }
    }

    /**
     * Unique id of this component.
     * By default the value is "ly.img.component.timeline".
     */
    override var id: ScopedProperty<Timeline.Scope, EditorComponentId> = {
        EditorComponentId("ly.img.component.timeline")
    }

    /**
     * Decoration of this component. Useful when you want to add custom background, foreground, shadow, paddings etc.
     * Default value is [Timeline.Companion.DefaultDecoration].
     */
    override var decoration: ScopedDecoration<Timeline.Scope> = {
        Timeline.DefaultDecoration(content = it)
    }

    @Composable
    override fun build(
        scope: Timeline.Scope,
        id: EditorComponentId,
        modifier: Modifier,
        visible: Boolean,
        enterTransition: EnterTransition,
        exitTransition: ExitTransition,
        decoration: ScopedDecoration<Timeline.Scope>,
    ): Timeline = remember(
        scope,
        id,
        modifier,
        visible,
        enterTransition,
        exitTransition,
        decoration,
    ) {
        Timeline(
            scope = scope,
            id = id,
            modifier = modifier,
            visible = visible,
            enterTransition = enterTransition,
            exitTransition = exitTransition,
            decoration = decoration,
        )
    }
}

/**
 * The default decoration of the timeline.
 *
 * Sets a background color and applies paddings to the timeline by adding a containing box.
 *
 * @param background the background of the containing box.
 * @param paddingValues the padding values of the containing box.
 * @param content the content of the timeline.
 */
@Composable
fun Timeline.Companion.DefaultDecoration(
    `_`: Nothing = nothing,
    background: Color = MaterialTheme.colorScheme.surface1,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    `__`: Nothing = nothing,
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .background(background)
            .padding(paddingValues),
    ) {
        content()
    }
}

/**
 * A composable overload for [Timeline.Companion.remember] that uses [TimelineBuilder] to create and remember
 * a [Timeline] instance. Check the documentation of overloaded [Timeline.Companion.remember] function below for
 * more details.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builder the builder block that configures the [Timeline].
 * @return a timeline that will be displayed when launching an editor.
 */
@Composable
fun Timeline.Companion.remember(builder: TimelineBuilder.() -> Unit = {}): Timeline = remember(::TimelineBuilder, builder)

/**
 * A composable function that creates and remembers a [Timeline] instance.
 * Note that both [builderFactory] and [builder] lambdas run only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builderFactory the factory that should be used to construct [Timeline].
 * @param builder the builder block that configures the [Timeline].
 * @return a timeline that will be displayed when launching an editor.
 */
@Composable
fun <Builder : TimelineBuilder> Timeline.Companion.remember(
    builderFactory: () -> Builder,
    builder: TimelineBuilder.() -> Unit = {},
): Timeline = androidx.compose.runtime.remember { builderFactory().apply(builder) }.build()
