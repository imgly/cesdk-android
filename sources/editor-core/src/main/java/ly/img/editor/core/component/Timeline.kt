@file:Suppress("UnusedReceiverParameter")

package ly.img.editor.core.component

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ly.img.editor.core.EditorContext
import ly.img.editor.core.EditorScope
import ly.img.editor.core.R
import ly.img.editor.core.ScopedDecoration
import ly.img.editor.core.ScopedProperty
import ly.img.editor.core.UnstableEditorApi
import ly.img.editor.core.component.data.Nothing
import ly.img.editor.core.component.data.TimelineHeight
import ly.img.editor.core.component.data.nothing
import ly.img.editor.core.configuration.remember
import ly.img.editor.core.iconpack.IconPack
import ly.img.editor.core.iconpack.Plus
import ly.img.editor.core.theme.surface1
import ly.img.editor.core.theme.surface3
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
    val addClipButton: EditorComponent<*>?,
    val addAudioButton: EditorComponent<*>?,
    val headerListBuilder: HorizontalListBuilder<EditorComponent<*>>,
    val height: TimelineHeight,
) : EditorComponent<Timeline.Scope>() {
    @OptIn(UnstableEditorApi::class)
    @Composable
    override fun Scope.Content(animatedVisibilityScope: AnimatedVisibilityScope?) {
        (editorContext as TimelineOwner).TimelineContent(
            addClipButton = addClipButton,
            addAudioButton = addAudioButton,
            headerListBuilder = headerListBuilder,
            height = height,
            expanded = editorContext.expandedState.value,
        )
    }

    /**
     * Scope of the [Timeline] component.
     *
     * @param parentScope the scope of the parent component.
     */
    @Stable
    open class Scope(
        parentScope: EditorScope,
        private val expandedState: MutableState<Boolean>,
    ) : EditorScope(parentScope) {
        /**
         * Current selection of the editor.
         */
        val EditorContext.expandedState: MutableState<Boolean>
            get() = this@Scope.expandedState
    }

    /**
     * Scope of the items inside the [Timeline] header.
     *
     * @param parentScope the scope of the parent component.
     */
    @Stable
    open class ItemScope(
        parentScope: EditorScope,
    ) : EditorScope(parentScope) {
        val EditorContext.expandedState by lazy {
            (parentScope as Scope).run {
                editorContext.expandedState
            }
        }
    }

    // todo replace with nested typealias with kotlin 2.0 bump
    object Button {
        object Id
    }

    // todo replace with nested typealias with kotlin 2.0 bump
    object Label {
        object Id
    }

    /**
     * Builder class of custom-rendered components inside the [Timeline] header.
     */
    @Stable
    open class ItemBuilder : NoContentEditorComponentBuilder<ItemScope>() {
        override var scope: ScopedProperty<EditorScope, ItemScope> = {
            remember(this) { ItemScope(parentScope = this) }
        }
    }

    // todo replace with nested typealias with kotlin 2.0 bump
    object HeaderListBuilder

    /**
     * Builder class of [ly.img.editor.core.component.Button] components inside the [Timeline] header.
     */
    @Stable
    open class ButtonBuilder : AbstractButtonBuilder<ItemScope>() {
        override var scope: ScopedProperty<EditorScope, ItemScope> = {
            remember(this) {
                ItemScope(parentScope = this)
            }
        }

        /**
         * Modifier of this component.
         * By default, the size matches the touch target of the built-in header buttons.
         */
        override var modifier: ScopedProperty<ItemScope, Modifier> = {
            Modifier.size(48.dp)
        }
    }

    /**
     * Builder class shared by the entries of the [Timeline]'s "Add Clip" and "Add Audio" menus.
     * The properties match the other configurable components, so an entry is configured the way a
     * [Dock] or [InspectorBar] button is.
     */
    @Stable
    abstract class AbstractOptionBuilder<Target : EditorComponent<ItemScope>> : EditorComponentBuilder<Target, ItemScope>() {
        /**
         * Scope of this component.
         * By default, it is updated only when the parent scope (accessed via `this`) is updated.
         */
        override var scope: ScopedProperty<EditorScope, ItemScope> = {
            remember(this) { ItemScope(parentScope = this) }
        }

        /**
         * Callback that is invoked when the entry is clicked.
         * By default, it does nothing.
         */
        open var onClick: ItemScope.() -> Unit = {}

        /**
         * Composable function that is used to render an icon. Can be used to draw ambiguous content.
         * By default, no icon is applied.
         */
        open var icon: (@Composable ItemScope.() -> Unit)? = null

        /**
         * Custom implementation of [icon] that provides an icon from a vector resource.
         * By default no vector icon is applied.
         */
        open var vectorIcon: ScopedProperty<ItemScope, ImageVector>? = null
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
        open var text: (@Composable ItemScope.() -> Unit)? = null

        /**
         * Custom implementation of [text] that provides a text from a string.
         * By default no text string is applied.
         */
        open var textString: ScopedProperty<ItemScope, String>? = null
            set(value) {
                field = value
                text = value?.let { { Text(text = value(this)) } }
            }

        /**
         * Content description of this entry. Useful for handling accessibility issues.
         * Default value is null.
         */
        open var contentDescription: (@Composable ItemScope.() -> String)? = null

        /**
         * Whether the entry is enabled or not.
         * Default value is always true.
         */
        open var enabled: ScopedProperty<ItemScope, Boolean> = alwaysEnabled
    }

    /**
     * A single entry in the [Timeline]'s "Add Clip" menu.
     * Use [Timeline.AddClipOption.Companion.rememberCamera] and its siblings to create an instance of this class.
     */
    @Stable
    data class AddClipOption(
        override val scope: ItemScope,
        override val id: EditorComponentId,
        override val modifier: Modifier,
        override val visible: Boolean,
        override val enterTransition: EnterTransition,
        override val exitTransition: ExitTransition,
        override val decoration: ScopedDecoration<ItemScope>,
        val onClick: ItemScope.() -> Unit,
        val icon: (@Composable ItemScope.() -> Unit)?,
        val text: (@Composable ItemScope.() -> Unit)?,
        val enabled: Boolean,
    ) : EditorComponent<ItemScope>() {
        @Composable
        override fun ItemScope.Content(animatedVisibilityScope: AnimatedVisibilityScope?) {
            OptionRow(
                modifier = modifier,
                text = text,
                icon = icon,
                enabled = enabled,
            )
        }

        object Id

        companion object
    }

    /**
     * A single entry in the [Timeline]'s "Add Audio" menu. See [AddClipOption] for the model.
     */
    @Stable
    data class AddAudioOption(
        override val scope: ItemScope,
        override val id: EditorComponentId,
        override val modifier: Modifier,
        override val visible: Boolean,
        override val enterTransition: EnterTransition,
        override val exitTransition: ExitTransition,
        override val decoration: ScopedDecoration<ItemScope>,
        val onClick: ItemScope.() -> Unit,
        val icon: (@Composable ItemScope.() -> Unit)?,
        val text: (@Composable ItemScope.() -> Unit)?,
        val enabled: Boolean,
    ) : EditorComponent<ItemScope>() {
        @Composable
        override fun ItemScope.Content(animatedVisibilityScope: AnimatedVisibilityScope?) {
            OptionRow(
                modifier = modifier,
                text = text,
                icon = icon,
                enabled = enabled,
            )
        }

        object Id

        companion object
    }

    /**
     * Builder class for [AddClipOption].
     */
    @Stable
    open class AddClipOptionBuilder : AbstractOptionBuilder<AddClipOption>() {
        @Composable
        override fun build(
            scope: ItemScope,
            id: EditorComponentId,
            modifier: Modifier,
            visible: Boolean,
            enterTransition: EnterTransition,
            exitTransition: ExitTransition,
            decoration: ScopedDecoration<ItemScope>,
        ): AddClipOption {
            val enabled = enabled(scope)
            return remember(
                scope,
                id,
                modifier,
                visible,
                enterTransition,
                exitTransition,
                decoration,
                enabled,
            ) {
                AddClipOption(
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
                    enabled = enabled,
                )
            }
        }
    }

    /**
     * Builder class for [AddAudioOption].
     */
    @Stable
    open class AddAudioOptionBuilder : AbstractOptionBuilder<AddAudioOption>() {
        @Composable
        override fun build(
            scope: ItemScope,
            id: EditorComponentId,
            modifier: Modifier,
            visible: Boolean,
            enterTransition: EnterTransition,
            exitTransition: ExitTransition,
            decoration: ScopedDecoration<ItemScope>,
        ): AddAudioOption {
            val enabled = enabled(scope)
            return remember(
                scope,
                id,
                modifier,
                visible,
                enterTransition,
                exitTransition,
                decoration,
                enabled,
            ) {
                AddAudioOption(
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
                    enabled = enabled,
                )
            }
        }
    }

    /**
     * Builder class shared by the [Timeline]'s "Add Clip" and "Add Audio" buttons. The properties match
     * the other configurable components, so the buttons are configured the way a [Dock] button is.
     */
    @Stable
    abstract class AbstractAddButtonBuilder<Target : EditorComponent<ItemScope>> : EditorComponentBuilder<Target, ItemScope>() {
        /**
         * Scope of this component.
         * By default it is updated only when the parent scope (accessed via `this`) is updated.
         */
        override var scope: ScopedProperty<EditorScope, ItemScope> = {
            remember(this) { ItemScope(parentScope = this) }
        }

        /**
         * Composable function that is used to render an icon. Can be used to draw ambiguous content.
         * Default value is [IconPack.Plus].
         */
        open var icon: (@Composable ItemScope.() -> Unit)? = {
            Icon(
                imageVector = IconPack.Plus,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
            )
        }

        /**
         * Custom implementation of [icon] that provides an icon from a vector resource.
         * The icon is sized to match the built-in one.
         */
        open var vectorIcon: ScopedProperty<ItemScope, ImageVector>? = null
            set(value) {
                field = value
                icon = value?.let {
                    {
                        Icon(
                            imageVector = value(this),
                            contentDescription = contentDescription?.invoke(this),
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

        /**
         * Composable function that is used to render a text. Can be used to draw ambiguous content.
         * Every subclass sets a default.
         */
        open var text: (@Composable ItemScope.() -> Unit)? = null

        /**
         * Custom implementation of [text] that provides a text from a string.
         * Assigning it also drops the built-in naming of a lone menu entry.
         */
        open var textString: ScopedProperty<ItemScope, String>? = null
            set(value) {
                field = value
                text = value?.let { { Text(text = value(this)) } }
            }

        /**
         * Content description of this button. Useful for handling accessibility issues.
         * Default value is null.
         */
        open var contentDescription: (@Composable ItemScope.() -> String)? = null

        /**
         * Whether the button is enabled or not.
         * Default value is always true.
         */
        open var enabled: ScopedProperty<ItemScope, Boolean> = alwaysEnabled

        /**
         * Tint of this button.
         * Default value is always onSurface from [MaterialTheme.colorScheme].
         */
        open var tint: ScopedProperty<ItemScope, Color> = { MaterialTheme.colorScheme.onSurface }

        /**
         * Content padding of the button.
         * By default the padding matches the built-in lane buttons.
         */
        open var contentPadding: ScopedProperty<ItemScope, PaddingValues> = {
            ButtonDefaults.TextButtonWithIconContentPadding
        }

        /**
         * Container (background) color of the button.
         * Every subclass sets a default.
         */
        abstract var containerColor: ScopedProperty<ItemScope, Color>
    }

    /**
     * The built-in "Add Clip" button of the [Timeline].
     * Use [Timeline.Button.rememberAddClip] composable function to create an instance of this class.
     */
    @Stable
    data class AddClipButton(
        override val scope: ItemScope,
        override val id: EditorComponentId,
        override val modifier: Modifier,
        override val visible: Boolean,
        override val enterTransition: EnterTransition,
        override val exitTransition: ExitTransition,
        override val decoration: ScopedDecoration<ItemScope>,
        val optionsBuilder: UnalignedListBuilder<AddClipOption>,
        val icon: (@Composable ItemScope.() -> Unit)?,
        val text: (@Composable ItemScope.() -> Unit)?,
        val enabled: Boolean,
        val tint: Color,
        val contentPadding: PaddingValues,
        val containerColor: Color,
    ) : EditorComponent<ItemScope>() {
        @OptIn(UnstableEditorApi::class)
        @Composable
        override fun ItemScope.Content(animatedVisibilityScope: AnimatedVisibilityScope?) {
            Box(modifier = modifier) {
                (editorContext as TimelineOwner).AddClipButtonContent(this@AddClipButton)
            }
        }

        companion object
    }

    /**
     * The built-in "Add Audio" button of the [Timeline].
     * Use [Timeline.Button.rememberAddAudio] composable function to create an instance of this class.
     */
    @Stable
    data class AddAudioButton(
        override val scope: ItemScope,
        override val id: EditorComponentId,
        override val modifier: Modifier,
        override val visible: Boolean,
        override val enterTransition: EnterTransition,
        override val exitTransition: ExitTransition,
        override val decoration: ScopedDecoration<ItemScope>,
        val optionsBuilder: UnalignedListBuilder<AddAudioOption>,
        val icon: (@Composable ItemScope.() -> Unit)?,
        val text: (@Composable ItemScope.() -> Unit)?,
        val enabled: Boolean,
        val tint: Color,
        val contentPadding: PaddingValues,
        val containerColor: Color,
    ) : EditorComponent<ItemScope>() {
        @OptIn(UnstableEditorApi::class)
        @Composable
        override fun ItemScope.Content(animatedVisibilityScope: AnimatedVisibilityScope?) {
            Box(modifier = modifier) {
                (editorContext as TimelineOwner).AddAudioButtonContent(this@AddAudioButton)
            }
        }

        companion object
    }

    /**
     * Builder class for [AddClipButton].
     */
    @Stable
    open class AddClipButtonBuilder : AbstractAddButtonBuilder<AddClipButton>() {
        /**
         * Unique id of this component.
         * By default the value is [Timeline.Button.Id.addClip].
         */
        override var id: ScopedProperty<ItemScope, EditorComponentId> = {
            Button.Id.addClip
        }

        /**
         * The entries that are displayed in the button's menu. When only a single entry remains, clicking
         * the button triggers it directly instead of opening a menu.
         */
        open var optionsBuilder: ScopedProperty<ItemScope, UnalignedListBuilder<AddClipOption>> = {
            UnalignedListBuilder.remember {
                add { AddClipOption.rememberCamera() }
                add { AddClipOption.rememberLibrary() }
            }
        }

        /**
         * Whether the button should be visible.
         * By default, the button is visible as long as one option is.
         */
        override var visible: ScopedProperty<ItemScope, Boolean> = {
            val options = optionsBuilder(this).build(this)[null]?.items
            options?.any { it.visible } ?: false
        }

        /**
         * The label of the button.
         * A lone option has no menu to open, so by default the button names that option instead.
         */
        override var text: (@Composable ItemScope.() -> Unit)? = {
            val options = optionsBuilder(this).build(this)[null]?.items
            val entryText = options?.singleOrNull { it.visible }?.text
            if (entryText != null) {
                entryText(this)
            } else {
                Text(text = stringResource(R.string.ly_img_editor_timeline_button_add_clip))
            }
        }

        /**
         * Container (background) color of the button.
         * Default value is always surface3 from [MaterialTheme.colorScheme].
         */
        override var containerColor: ScopedProperty<ItemScope, Color> = {
            MaterialTheme.colorScheme.surface3
        }

        @Composable
        override fun build(
            scope: ItemScope,
            id: EditorComponentId,
            modifier: Modifier,
            visible: Boolean,
            enterTransition: EnterTransition,
            exitTransition: ExitTransition,
            decoration: ScopedDecoration<ItemScope>,
        ): AddClipButton {
            // The options lambda is held rather than a resolved list, mirroring how [Dock] holds its
            // list builder. It is remembered once, so the component stays stable across recompositions
            // while the menu contents are still re-evaluated on every composition.
            val optionsBuilder = optionsBuilder(scope)
            val enabled = enabled(scope)
            val tint = tint(scope)
            val contentPadding = contentPadding(scope)
            val containerColor = containerColor(scope)
            return remember(
                scope,
                id,
                modifier,
                visible,
                enterTransition,
                exitTransition,
                decoration,
                optionsBuilder,
                enabled,
                tint,
                contentPadding,
                containerColor,
            ) {
                AddClipButton(
                    scope = scope,
                    id = id,
                    modifier = modifier,
                    visible = visible,
                    enterTransition = enterTransition,
                    exitTransition = exitTransition,
                    decoration = decoration,
                    optionsBuilder = optionsBuilder,
                    icon = this.icon,
                    text = this.text,
                    enabled = enabled,
                    tint = tint,
                    contentPadding = contentPadding,
                    containerColor = containerColor,
                )
            }
        }
    }

    /**
     * Builder class for [AddAudioButton].
     */
    @Stable
    open class AddAudioButtonBuilder : AbstractAddButtonBuilder<AddAudioButton>() {
        /**
         * Unique id of this component.
         * By default, the value is [Timeline.Button.Id.addAudio].
         */
        override var id: ScopedProperty<ItemScope, EditorComponentId> = {
            Button.Id.addAudio
        }

        /**
         * The entries that are displayed in the button's menu. When only a single entry remains, clicking
         * the button triggers it directly instead of opening a menu.
         */
        open var optionsBuilder: ScopedProperty<ItemScope, UnalignedListBuilder<AddAudioOption>> = {
            UnalignedListBuilder.remember {
                add { AddAudioOption.rememberMusic() }
                add { AddAudioOption.rememberVoiceover() }
            }
        }

        /**
         * Whether the button should be visible.
         * By default, the button is visible as long as one option is.
         */
        override var visible: ScopedProperty<ItemScope, Boolean> = {
            val options = optionsBuilder(this).build(this)[null]?.items
            options?.any { it.visible } ?: false
        }

        /**
         * The label of the button.
         * A lone option has no menu to open, so by default the button names that option instead.
         */
        override var text: (@Composable ItemScope.() -> Unit)? = {
            val options = optionsBuilder(this).build(this)[null]?.items
            val entryText = options?.singleOrNull { it.visible }?.text
            if (entryText != null) {
                entryText(this)
            } else {
                Text(text = stringResource(R.string.ly_img_editor_timeline_button_add_audio))
            }
        }

        /**
         * Container (background) color of the button.
         * Default value is always [Color.Transparent].
         */
        override var containerColor: ScopedProperty<ItemScope, Color> = { Color.Transparent }

        @Composable
        override fun build(
            scope: ItemScope,
            id: EditorComponentId,
            modifier: Modifier,
            visible: Boolean,
            enterTransition: EnterTransition,
            exitTransition: ExitTransition,
            decoration: ScopedDecoration<ItemScope>,
        ): AddAudioButton {
            // See [AddClipButtonBuilder.build] for why the lambda is held instead of a list.
            val optionsBuilder = optionsBuilder(scope)
            val enabled = enabled(scope)
            val tint = tint(scope)
            val contentPadding = contentPadding(scope)
            val containerColor = containerColor(scope)
            return remember(
                scope,
                id,
                modifier,
                visible,
                enterTransition,
                exitTransition,
                decoration,
                optionsBuilder,
                enabled,
                tint,
                contentPadding,
                containerColor,
            ) {
                AddAudioButton(
                    scope = scope,
                    id = id,
                    modifier = modifier,
                    visible = visible,
                    enterTransition = enterTransition,
                    exitTransition = exitTransition,
                    decoration = decoration,
                    optionsBuilder = optionsBuilder,
                    icon = this.icon,
                    text = this.text,
                    enabled = enabled,
                    tint = tint,
                    contentPadding = contentPadding,
                    containerColor = containerColor,
                )
            }
        }
    }

    companion object
}

/**
 * Renders the editor's [Timeline] component and its built-in add buttons.
 */
@UnstableEditorApi
@Stable
interface TimelineOwner {
    /**
     * Renders the timeline for the given configuration.
     *
     * @param addClipButton the component rendered as the "Add Clip" button, or null to render none.
     * @param addAudioButton the component rendered as the "Add Audio" button, or null to render none.
     * @param headerListBuilder the items of the timeline header, grouped by alignment.
     * @param height the height of the timeline, in track rows.
     * @param expanded whether the timeline is expanded.
     */
    @UnstableEditorApi
    @Composable
    fun TimelineContent(
        addClipButton: EditorComponent<*>?,
        addAudioButton: EditorComponent<*>?,
        headerListBuilder: HorizontalListBuilder<EditorComponent<*>>,
        height: TimelineHeight,
        expanded: Boolean,
    )

    /**
     * Renders the contents of the built-in "Add Clip" button.
     *
     * @param button the built button, holding its resolved icon, label, options and enabled state.
     */
    @UnstableEditorApi
    @Composable
    fun AddClipButtonContent(button: Timeline.AddClipButton)

    /**
     * Renders the contents of the built-in "Add Audio" button.
     *
     * @param button the built button, holding its resolved icon, label, options and enabled state.
     */
    @UnstableEditorApi
    @Composable
    fun AddAudioButtonContent(button: Timeline.AddAudioButton)
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
     * By default, it is updated only when the parent scope (accessed via `this`) is updated.
     */
    override var scope: ScopedProperty<EditorScope, Timeline.Scope> = {
        remember(this) {
            Timeline.Scope(
                parentScope = this,
                expandedState = editorContext.mutableStateOf(key = EXPANDED_STATE_KEY, initial = true),
            )
        }
    }

    /**
     * Unique id of this component.
     * By default, the value is "ly.img.component.timeline".
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

    /**
     * The component that is rendered as the timeline's "Add Clip" button.
     * By default, the timeline shows no such button. Assign [Timeline.Button.rememberAddClip] to get
     * the built-in one, or your own [EditorComponent] to replace it entirely.
     */
    open var addClipButton: ScopedProperty<Timeline.Scope, EditorComponent<*>?> = { null }

    /**
     * The component that is rendered as the timeline's "Add Audio" button.
     * By default, the timeline shows no such button. Assign [Timeline.Button.rememberAddAudio] to get
     * the built-in one, or your own [EditorComponent] to replace it entirely.
     */
    open var addAudioButton: ScopedProperty<Timeline.Scope, EditorComponent<*>?> = { null }

    /**
     * The list of items displayed in the timeline header, grouped by alignment.
     * By default, the header is empty, which renders the timeline as tracks alone. Removing every
     * header item removes the player bar with it.
     */
    open var headerListBuilder: ScopedProperty<Timeline.Scope, HorizontalListBuilder<EditorComponent<*>>> = {
        Timeline.HeaderListBuilder.remember { }
    }

    /**
     * The height of the timeline, in track rows. Defaults to [TimelineHeight.Dynamic].
     */
    open var height: ScopedProperty<Timeline.Scope, TimelineHeight> = {
        TimelineHeight.Dynamic()
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
    ): Timeline {
        val addClipButton = addClipButton(scope)
        val addAudioButton = addAudioButton(scope)
        val headerListBuilder = headerListBuilder(scope)
        val height = height(scope)
        return remember(
            scope,
            id,
            modifier,
            visible,
            enterTransition,
            exitTransition,
            decoration,
            addClipButton,
            addAudioButton,
            headerListBuilder,
            height,
        ) {
            Timeline(
                scope = scope,
                id = id,
                modifier = modifier,
                visible = visible,
                enterTransition = enterTransition,
                exitTransition = exitTransition,
                decoration = decoration,
                addClipButton = addClipButton,
                addAudioButton = addAudioButton,
                headerListBuilder = headerListBuilder,
                height = height,
            )
        }
    }

    private companion object {
        const val EXPANDED_STATE_KEY = "ly.img.component.timeline.state.expanded"
    }
}

/**
 * The menu row both option types render. Declared once so the two stay visually identical.
 */
@Composable
private fun Timeline.ItemScope.OptionRow(
    modifier: Modifier,
    text: (@Composable Timeline.ItemScope.() -> Unit)?,
    icon: (@Composable Timeline.ItemScope.() -> Unit)?,
    enabled: Boolean,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.widthIn(min = 72.dp)) {
            text?.invoke(this@OptionRow)
        }
        icon?.let {
            Spacer(modifier = Modifier.width(16.dp))
            // The row renders the icon itself rather than through DropdownMenuItem's trailingIcon
            // slot, so it has to provide the colour that slot would have applied, disabled included.
            val iconColor = MaterialTheme.colorScheme.onSurfaceVariant
            CompositionLocalProvider(
                LocalContentColor provides if (enabled) {
                    iconColor
                } else {
                    iconColor.copy(alpha = iconColor.alpha * 0.38F)
                },
            ) {
                it(this@OptionRow)
            }
        }
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

/**
 * A composable function that creates and remembers a [Timeline.AddClipOption] instance.
 * Note that both [builderFactory] and [builder] lambdas run only once, therefore you should not have builder property
 * reassignments based on conditions.
 *
 * @param builderFactory the factory that should be used to construct the option.
 * @param builder the builder block that configures the option.
 * @return a new entry for the "Add Clip" menu.
 */
@Composable
fun <Builder : Timeline.AddClipOptionBuilder> Timeline.AddClipOption.Companion.remember(
    builderFactory: () -> Builder,
    builder: Builder.() -> Unit = {},
): Timeline.AddClipOption = androidx.compose.runtime.remember { builderFactory().apply(builder) }.build()

/**
 * A composable function that creates and remembers a [Timeline.AddAudioOption] instance.
 * Note that both [builderFactory] and [builder] lambdas run only once, therefore you should not have builder property
 * reassignments based on conditions.
 *
 * @param builderFactory the factory that should be used to construct the option.
 * @param builder the builder block that configures the option.
 * @return a new entry for the "Add Audio" menu.
 */
@Composable
fun <Builder : Timeline.AddAudioOptionBuilder> Timeline.AddAudioOption.Companion.remember(
    builderFactory: () -> Builder,
    builder: Builder.() -> Unit = {},
): Timeline.AddAudioOption = androidx.compose.runtime.remember { builderFactory().apply(builder) }.build()

/**
 * A composable function that creates and remembers a custom entry for the "Add Clip" menu.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 *
 * @param builder the builder block that configures the entry.
 * @return a new entry for the "Add Clip" menu.
 */
@Composable
fun Timeline.AddClipOption.Companion.remember(builder: Timeline.AddClipOptionBuilder.() -> Unit = {}): Timeline.AddClipOption =
    Timeline.AddClipOption.remember(Timeline::AddClipOptionBuilder, builder)

/**
 * A composable function that creates and remembers a custom entry for the "Add Audio" menu.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions.
 *
 * @param builder the builder block that configures the entry.
 * @return a new entry for the "Add Audio" menu.
 */
@Composable
fun Timeline.AddAudioOption.Companion.remember(builder: Timeline.AddAudioOptionBuilder.() -> Unit = {}): Timeline.AddAudioOption =
    Timeline.AddAudioOption.remember(Timeline::AddAudioOptionBuilder, builder)
