package ly.img.editor.core.component

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import ly.img.editor.core.EditorScope
import ly.img.editor.core.ScopedDecoration
import ly.img.editor.core.ScopedProperty
import ly.img.editor.core.configuration.remember
import ly.img.editor.core.ui.IconTextButton

/**
 * A component for rendering an icon button.
 * Check [AbstractButtonBuilder] and its superclasses to see what each property does.
 */
@Stable
data class Button<Scope : EditorScope>(
    override val scope: Scope,
    override val id: EditorComponentId,
    override val modifier: Modifier,
    override val visible: Boolean,
    override val enterTransition: EnterTransition,
    override val exitTransition: ExitTransition,
    override val decoration: ScopedDecoration<Scope>,
    val onClick: Scope.() -> Unit,
    val icon: (@Composable Scope.() -> Unit)?,
    val text: (@Composable Scope.() -> Unit)?,
    val tint: Color,
    val enabled: Boolean,
    val contentPadding: PaddingValues,
) : EditorComponent<Scope>() {
    @Composable
    override fun Scope.Content(animatedVisibilityScope: AnimatedVisibilityScope?) {
        IconTextButton(
            modifier = modifier,
            onClick = { onClick() },
            enabled = enabled,
            contentPadding = contentPadding,
            tint = tint,
            icon = icon?.let { { it() } },
            text = text?.let { { it() } },
        )
    }

    class Id {
        companion object Companion
    }

    companion object Companion
}

/**
 * Abstract builder class for [Button].
 */
@Stable
abstract class AbstractButtonBuilder<Scope : EditorScope> : EditorComponentBuilder<Button<Scope>, Scope>() {
    /**
     * Callback that is invoked when the button is clicked.
     * By default it does nothing.
     */
    open var onClick: Scope.() -> Unit = {}

    /**
     * Composable function that is used to render an icon. Can be used to draw ambiguous content.
     * By default no icon is applied.
     */
    open var icon: (@Composable Scope.() -> Unit)? = null

    /**
     * Custom implementation of [icon] that provides an icon from a vector resource.
     * By default no vector icon is applied.
     */
    open var vectorIcon: ScopedProperty<Scope, ImageVector>? = null
        set(value) {
            field = value
            icon = value?.let {
                {
                    Icon(
                        imageVector = value(this),
                        contentDescription = contentDescription?.invoke(this),
                    )
                }
            }
        }

    /**
     * Composable function that is used to render a text. Can be used to draw ambiguous content.
     * By default no text is applied.
     */
    open var text: (@Composable Scope.() -> Unit)? = null

    /**
     * Custom implementation of [text] that provides a text from a string.
     * By default no text string is applied.
     */
    open var textString: ScopedProperty<Scope, String>? = null
        set(value) {
            field = value
            text = value?.let {
                {
                    Text(
                        text = value(this),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }
        }

    /**
     * Content description of this button. Useful for handling accessibility issues.
     * Always provide this value if the button does not contain visual text explaining what it does.
     * Default value is null.
     */
    open var contentDescription: (@Composable Scope.() -> String)? = null

    /**
     * Tint of this button.
     * Default value is always onSurfaceVariant from [MaterialTheme.colorScheme].
     */
    open var tint: ScopedProperty<Scope, Color> = { MaterialTheme.colorScheme.onSurfaceVariant }

    /**
     * Whether button is enabled or not.
     * Default value is always true.
     */
    open var enabled: ScopedProperty<Scope, Boolean> = alwaysEnabled

    /**
     * Content padding of the button.
     * By default both vertical and horizontal values are applied.
     */
    open var contentPadding: ScopedProperty<Scope, PaddingValues> = {
        PaddingValues(vertical = 10.dp, horizontal = 4.dp)
    }

    @Composable
    override fun build(
        scope: Scope,
        id: EditorComponentId,
        modifier: Modifier,
        visible: Boolean,
        enterTransition: EnterTransition,
        exitTransition: ExitTransition,
        decoration: ScopedDecoration<Scope>,
    ): Button<Scope> {
        val tint = tint(scope)
        val enabled = enabled(scope)
        val contentPadding = contentPadding(scope)
        return remember(
            scope,
            id,
            modifier,
            visible,
            enterTransition,
            exitTransition,
            decoration,
            tint,
            enabled,
            contentPadding,
        ) {
            Button(
                scope = scope,
                id = id,
                modifier = modifier,
                visible = visible,
                enterTransition = enterTransition,
                exitTransition = exitTransition,
                decoration = decoration,
                onClick = this.onClick,
                icon = this.icon,
                text = this.text,
                tint = tint,
                enabled = enabled,
                contentPadding = contentPadding,
            )
        }
    }
}

/**
 * A composable function that creates and remembers an [Button] instance.
 * Note that both [builderFactory] and [builder] lambdas run only once, therefore you should not have builder property reassignments based on conditions.
 * Check [ly.img.editor.core.configuration.EditorConfiguration.Companion.remember] for more details on this pattern.
 *
 * @param builderFactory the factory that should be used to construct [Button].
 * @param builder the builder block that configures the [Button].
 * @return a new icon button.
 */
@Composable
fun <Scope : EditorScope, Builder : AbstractButtonBuilder<Scope>> Button.Companion.remember(
    builderFactory: () -> Builder,
    builder: Builder.() -> Unit = {},
): Button<Scope> = androidx.compose.runtime.remember { builderFactory().apply(builder) }.build()
