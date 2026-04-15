@file:Suppress("UnusedReceiverParameter")

package ly.img.editor.core.component

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import ly.img.editor.core.EditorScope
import ly.img.editor.core.ScopedDecoration
import ly.img.editor.core.configuration.remember

/**
 * A component that represents a divider.
 */
@Stable
data class Divider<Scope : EditorScope>(
    override val scope: Scope,
    override val id: EditorComponentId,
    override val modifier: Modifier,
    override val visible: Boolean,
    override val enterTransition: EnterTransition,
    override val exitTransition: ExitTransition,
    override val decoration: ScopedDecoration<Scope>,
) : EditorComponent<Scope>() {
    /**
     * Scope of the [Divider] component.
     *
     * @param parentScope the scope of the parent component.
     */
    @Stable
    open class Scope(
        parentScope: EditorScope,
    ) : CanvasMenu.ItemScope(parentScope)

    @Composable
    override fun Scope.Content(animatedVisibilityScope: AnimatedVisibilityScope?) {
        androidx.compose.material3.Divider(
            modifier = modifier,
        )
    }

    companion object
}

/**
 * Builder class for [Divider].
 */
@Stable
abstract class AbstractDividerBuilder<Scope : EditorScope> : EditorComponentBuilder<Divider<Scope>, Scope>() {
    @Composable
    override fun build(
        scope: Scope,
        id: EditorComponentId,
        modifier: Modifier,
        visible: Boolean,
        enterTransition: EnterTransition,
        exitTransition: ExitTransition,
        decoration: ScopedDecoration<Scope>,
    ): Divider<Scope> = remember(
        scope,
        id,
        modifier,
        visible,
        enterTransition,
        exitTransition,
        decoration,
    ) {
        Divider(
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
 * A composable function that creates and remembers a [Divider] instance.
 * Note that both [builderFactory] and [builder] lambdas run only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builderFactory the factory that should be used to construct [Divider].
 * @param builder the builder block that configures the [Divider].
 * @return a divider that will be displayed.
 */
@Composable
fun <Scope : EditorScope, Builder : AbstractDividerBuilder<Scope>> Divider.Companion.remember(
    builderFactory: () -> Builder,
    builder: Builder.() -> Unit = {},
): Divider<Scope> = androidx.compose.runtime.remember { builderFactory().apply(builder) }.build()
