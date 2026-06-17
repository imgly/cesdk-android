package ly.img.editor.core.configuration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import ly.img.editor.core.EditorContext
import ly.img.editor.core.EditorContextImpl
import ly.img.editor.core.EditorScope
import ly.img.editor.core.LocalEditorScope
import ly.img.editor.core.ScopedProperty
import ly.img.editor.core.component.EditorComponent
import ly.img.editor.core.component.remember
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.event.EditorEventHandler
import ly.img.editor.core.library.AssetLibrary
import ly.img.editor.core.library.data.UploadAssetSourceType
import ly.img.engine.AssetDefinition

/**
 * Configuration class of the editor.
 */
@Immutable
data class EditorConfiguration(
    val onCreate: (suspend EditorScope.() -> Unit)?,
    val onLoaded: (suspend EditorScope.() -> Unit)?,
    val onExport: (suspend EditorScope.() -> Unit)?,
    val onClose: (suspend EditorScope.() -> Unit)?,
    val onEvent: (EditorScope.(EditorEvent) -> Unit)?,
    val onUpload: (suspend EditorScope.(AssetDefinition, UploadAssetSourceType) -> AssetDefinition)?,
    val onError: (suspend EditorScope.(Throwable) -> Unit)?,
    val colorPalette: List<Color>?,
    val assetLibrary: AssetLibrary?,
    val dock: EditorComponent<*>?,
    val navigationBar: EditorComponent<*>?,
    val inspectorBar: EditorComponent<*>?,
    val canvasMenu: EditorComponent<*>?,
    val bottomPanel: EditorComponent<*>?,
    val overlay: EditorComponent<*>?,
) {
    @Stable
    companion object
}

/**
 * Basic builder class for [EditorConfiguration].
 */
@Stable
open class EditorConfigurationBuilder {
    private val _editorContext = requireNotNull(EditorContextHolder.editorContext)

    /**
     * The context of the editor. This property should be used to access all the properties and functions within the editor.
     * It is an extension function on purpose to make accessing this object more obvious that it's part of the EditorScope and
     * not a customer's property (italic in Android Studio makes it more obvious).
     */
    val EditorConfigurationBuilder.editorContext: EditorContext
        get() = _editorContext

    /**
     * The configuration that was applied just before this builder via [EditorConfiguration.Companion.remember] or
     * [EditorConfiguration.then]. This property should be used to delegate invocations from this builder.
     *
     * For instance, you can access callbacks and components, override and modify them, extend behavior before or after:
     *
     * ```kotlin
     * onCreate = {
     *     // Does additional stuff before parent configuration's onCreate.
     *     ...
     *     // parentConfiguration?.onCreate invocation can be skipped too.
     *     parentConfiguration?.onCreate?.invoke(this)
     *     // Waits for parent configuration onCreate to finish, then does additional stuff.
     *     ...
     * }
     *
     * // Extend dock behavior only and only if parent defines dock.
     * dock = impl@{
     *     val parentDock = parentConfiguration?.dock as? Dock ?: return@impl null
     *     val updatedListBuilder = parentDock.listBuilder.modify {
     *         addFirst { Dock.Button.rememberElementsLibrary() }
     *     }
     *     remember(parentDock, updatedListBuilder) {
     *         parentDock.copy(listBuilder = updatedListBuilder)
     *     }
     * }
     *
     * // Add inspector bar no matter parent defines or not. If it is defined, then extend it.
     * inspectorBar = {
     *     // If parent configuration does not have an inspector bar, create an empty one as source.
     *     val sourceInspectorBar = parentConfiguration?.inspectorBar as? InspectorBar ?: InspectorBar.remember()
     *     val updatedListBuilder = sourceInspectorBar.listBuilder.modify {
     *         addLast { InspectorBar.Button.rememberCrop() }
     *     }
     *     remember(sourceInspectorBar, updatedListBuilder) {
     *         sourceInspectorBar.copy(listBuilder = updatedListBuilder)
     *     }
     * }
     * ```
     * Note that all null properties in [EditorConfigurationBuilder] are automatically delegated to corresponding properties in [parentConfiguration].
     * For instance, if [EditorConfigurationBuilder.onCreate] is null then the final [EditorConfiguration.onCreate] will match with
     * [EditorConfiguration.onCreate] of [parentConfiguration].
     */
    val parentConfiguration: EditorConfiguration? = EditorConfigurationHolder.editorConfiguration

    /**
     * The callback that is invoked when the editor is created. This is the main initialization block of both the editor
     * and engine. Normally, you should create/load a scene, prepare asset sources and apply editor settings in this block.
     * We recommend that you check the availability of the scene before creating/loading a new scene since a recreated scene may already
     * exist if the callback is invoked after a process recreation.
     * In addition to scene creation, it is highly recommended to register all the asset sources in this callback.
     * Note that the "create" coroutine job will survive configuration changes and will be cancelled only if the editor is closed or the process is killed
     * when in the background.
     */
    open var onCreate: (suspend EditorScope.() -> Unit)? = null

    /**
     * The callback that is invoked when the editor is loaded and ready to be used.
     * The callback is invoked right after [onCreate] when launching the editor for the first time or after process recreation.
     * The callback is not invoked after configuration changes.
     * It is best to register callbacks, collect flows returned by the engine.
     * Note that the "load" coroutine job will survive configuration changes and will be cancelled only if the editor is closed or the process is killed
     * when in the background.
     */
    open var onLoaded: (suspend EditorScope.() -> Unit)? = null

    /**
     * The callback that is invoked when the export button is clicked.
     * You may want to call one of the following functions in this callback: [ly.img.engine.BlockApi.export],
     * [ly.img.engine.BlockApi.exportWithColorMask], [ly.img.engine.BlockApi.exportVideo].
     * Note that the "export" coroutine job will survive configuration changes and will be cancelled only if the editor is closed or the process is killed
     * when in the background.
     */
    open var onExport: (suspend EditorScope.() -> Unit)? = null

    /**
     * The callback that is invoked after [EditorEvent.OnClose] event is triggered or when the system back button is clicked
     * and editor cannot handle the event internally.
     * Note that the "close" coroutine job will survive configuration changes and will be cancelled only if the editor is closed or
     * the process is killed when in the background.
     */
    open var onClose: (suspend EditorScope.() -> Unit)? = null

    /**
     * The callback that is invoked every time an event is sent via [EditorEventHandler].
     *
     * Note that [ly.img.editor.core.event.EditorEventHandler.send] can be called from any of the callbacks and components of the [EditorConfiguration].
     */
    open var onEvent: (EditorScope.(EditorEvent) -> Unit)? = null

    /**
     * The callback that is invoked after an asset is added to [UploadAssetSourceType]. When selecting an asset to upload,
     * a default [AssetDefinition] object is constructed based on the selected asset and the callback is invoked. You can either leave
     * the asset definition unmodified and do nothing (that's what the default implementation of the callback does), or adjust the properties
     * of the object, or maybe even upload the asset file to your server and adjust the uri property of the asset.
     * Note that the "upload" coroutine job will survive configuration changes and will be cancelled only if the editor is closed or the process is killed
     * when in the background.
     */
    open var onUpload: (suspend EditorScope.(AssetDefinition, UploadAssetSourceType) -> AssetDefinition)? = null

    /**
     * The callback that is invoked after the editor captures an error.
     * Note that the "error" coroutine job will survive configuration changes and will be cancelled only if the editor is closed or
     * the process is killed when in the background.
     */
    open var onError: (suspend EditorScope.(Throwable) -> Unit)? = null

    /**
     * The default color palette used in the UI elements that contain color modifiers such as "Fill color",
     * "Stroke Color" etc.
     */
    open var colorPalette: ScopedProperty<EditorScope, List<Color>?>? = null

    /**
     * The configuration of the [AssetLibrary]. Check the documentation of [AssetLibrary] for more details.
     */
    open var assetLibrary: ScopedProperty<EditorScope, AssetLibrary?>? = null

    /**
     * The configuration of the component that is displayed as horizontal list of items at the bottom of the editor.
     * Check [ly.img.editor.core.component.Dock] for our implementation or consider using [EditorComponent.Companion.remember]
     * to fully customize it.
     */
    open var dock: ScopedProperty<EditorScope, EditorComponent<*>?>? = null

    /**
     * The configuration of the component that is displayed as horizontal list of items at the top of the editor.
     * Check [ly.img.editor.core.component.NavigationBar] for our implementation or consider using [EditorComponent.Companion.remember]
     * to fully customize it.
     */
    open var navigationBar: ScopedProperty<EditorScope, EditorComponent<*>?>? = null

    /**
     * The configuration of the component that is displayed as horizontal list of items at the
     * bottom of the editor when a design block is selected.
     * Check [ly.img.editor.core.component.InspectorBar] for our implementation or consider using [EditorComponent.Companion.remember]
     * to fully customize it.
     */
    open var inspectorBar: ScopedProperty<EditorScope, EditorComponent<*>?>? = null

    /**
     * The configuration of the component that is displayed as horizontal list of items next to
     * the selected design block.
     * Check [ly.img.editor.core.component.CanvasMenu] for our implementation or consider using [EditorComponent.Companion.remember]
     * to fully customize it.
     */
    open var canvasMenu: ScopedProperty<EditorScope, EditorComponent<*>?>? = null

    /**
     * The configuration of the component that is displayed as a fixed bottom panel at the bottom of the editor, just above the [dock].
     * For example, it can be used if you want to render a timeline in a video editor via [ly.img.editor.core.component.Timeline].
     */
    open var bottomPanel: ScopedProperty<EditorScope, EditorComponent<*>?>? = null

    /**
     * The configuration of the component that is displayed over the editor. It is useful if you want to display a popup dialog or anything in the
     * overlay. For example, you can update a composable state object in one of the callbacks and based on that state render
     * a composable function.
     * Consider using [EditorComponent.Companion.remember] to implement it.
     * Note that the overlay is edge-to-edge, therefore it is your responsibility to draw over system bars too.
     */
    open var overlay: ScopedProperty<EditorScope, EditorComponent<*>?>? = null

    /**
     * Decorates final [EditorConfiguration] built by this builder.
     *
     * Useful when you want to wrap and apply pre-existing external configuration builders:
     *
     * ```kotlin
     * decorator = {
     *     this
     *         .then(::ExistingConfigurationBuilder)
     *         .then(::SomePlugin) {
     *             configOption = "external value"
     *         }
     * }
     * ```
     */
    protected open var decorator: @Composable EditorConfiguration.() -> EditorConfiguration = { this }

    @Composable
    internal fun build() = decorator(buildInternal())

    @Composable
    private fun buildInternal(): EditorConfiguration {
        val scope = LocalEditorScope.current
        val finalOnCreate = onCreate ?: parentConfiguration?.onCreate
        val finalOnLoaded = onLoaded ?: parentConfiguration?.onLoaded
        val finalOnExport = onExport ?: parentConfiguration?.onExport
        val finalOnClose = onClose ?: parentConfiguration?.onClose
        val finalOnEvent = onEvent ?: parentConfiguration?.onEvent
        val finalOnUpload = onUpload ?: parentConfiguration?.onUpload
        val finalOnError = onError ?: parentConfiguration?.onError
        val finalColorPalette = if (colorPalette != null) colorPalette?.invoke(scope) else parentConfiguration?.colorPalette
        val finalAssetLibrary = if (assetLibrary != null) assetLibrary?.invoke(scope) else parentConfiguration?.assetLibrary
        val finalDock = if (dock != null) dock?.invoke(scope) else parentConfiguration?.dock
        val finalNavigationBar = if (navigationBar != null) navigationBar?.invoke(scope) else parentConfiguration?.navigationBar
        val finalInspectorBar = if (inspectorBar != null) inspectorBar?.invoke(scope) else parentConfiguration?.inspectorBar
        val finalCanvasMenu = if (canvasMenu != null) canvasMenu?.invoke(scope) else parentConfiguration?.canvasMenu
        val finalBottomPanel = if (bottomPanel != null) bottomPanel?.invoke(scope) else parentConfiguration?.bottomPanel
        val finalOverlay = if (overlay != null) overlay?.invoke(scope) else parentConfiguration?.overlay
        return remember(
            finalOnCreate,
            finalOnLoaded,
            finalOnExport,
            finalOnClose,
            finalOnEvent,
            finalOnUpload,
            finalOnError,
            finalColorPalette,
            finalAssetLibrary,
            finalDock,
            finalNavigationBar,
            finalInspectorBar,
            finalCanvasMenu,
            finalBottomPanel,
            finalOverlay,
            parentConfiguration,
        ) {
            EditorConfiguration(
                onCreate = finalOnCreate,
                onLoaded = finalOnLoaded,
                onExport = finalOnExport,
                onClose = finalOnClose,
                onEvent = finalOnEvent,
                onUpload = finalOnUpload,
                onError = finalOnError,
                colorPalette = finalColorPalette,
                assetLibrary = finalAssetLibrary,
                dock = finalDock,
                navigationBar = finalNavigationBar,
                inspectorBar = finalInspectorBar,
                canvasMenu = finalCanvasMenu,
                bottomPanel = finalBottomPanel,
                overlay = finalOverlay,
            )
        }
    }
}

/**
 * A composable overload for [EditorConfiguration.Companion.remember] that uses [EditorConfigurationBuilder] to create and remember an
 * [EditorConfiguration] instance.
 * Check the documentation of overloaded [EditorConfiguration.Companion.remember] function below for more details.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions:
 *
 * ```kotlin
 * EditorConfiguration.remember {
 *     // WRONG! onCreate will not be updated.
 *     onCreate = if (condition) {
 *         { ... }
 *     } else {
 *         { ... }
 *     }
 * ```
 *
 * ```kotlin
 * EditorConfiguration.remember {
 *     // CORRECT! onCreate will use the updated logic based on the condition.
 *     onCreate = {
 *         if (condition) {
 *             ...
 *         } else {
 *             ...
 *         }
 *     }
 * ```
 *
 * @param builder the builder block that configures the [EditorConfiguration].
 * @return an object that is used to configure the editor.
 */
@Composable
fun EditorConfiguration.Companion.remember(builder: EditorConfigurationBuilder.() -> Unit = {}): EditorConfiguration =
    remember(::EditorConfigurationBuilder, builder)

/**
 * A composable function that creates and remembers an [EditorConfiguration] instance.
 * Check the documentation of overloaded [EditorConfiguration.Companion.remember] function below for more details.
 * Note that both [builderFactory] and [builder] lambdas run only once, therefore you should not have builder property reassignments based on conditions:
 *
 * ```kotlin
 * EditorConfiguration.remember(::EditorConfigurationBuilder) {
 *     // WRONG! onCreate will not be updated.
 *     onCreate = if (condition) {
 *         { ... }
 *     } else {
 *         { ... }
 *     }
 * ```
 *
 * ```kotlin
 * EditorConfiguration.remember(::EditorConfigurationBuilder) {
 *     // CORRECT! onCreate will use the updated logic based on the condition.
 *     onCreate = {
 *         if (condition) {
 *             ...
 *         } else {
 *             ...
 *         }
 *     }
 * ```
 *
 * @param builderFactory the factory that should be used to construct [EditorConfiguration].
 * @param builder the builder block that configures the [EditorConfiguration].
 * @return an object that is used to configure the editor.
 */
@Composable
fun <Builder : EditorConfigurationBuilder> EditorConfiguration.Companion.remember(
    builderFactory: () -> Builder,
    builder: Builder.() -> Unit = {},
): EditorConfiguration = LocalEditorScope.current.run {
    // This can be moved to compile time in the future when we start to use context parameters.
    require((this.editorContext as EditorContextImpl).isValid) {
        "EditorConfiguration.remember must be invoked only in `configuration` lambda of `Editor` composable."
    }
    androidx.compose.runtime.remember {
        EditorContextHolder.editorContext = editorContext
        EditorConfigurationHolder.editorConfiguration = null
        builderFactory()
            .also { EditorContextHolder.editorContext = null }
            .apply(builder)
    }.build()
}

/**
 * A composable overload for [EditorConfiguration.then] that uses [EditorConfigurationBuilder] to create and remember an
 * [EditorConfiguration] instance.
 * Check the documentation of overloaded [EditorConfiguration.then] function below for more details.
 * Note that [builder] lambda runs only once, therefore you should not have builder property reassignments based on conditions:
 *
 * ```kotlin
 * editorConfiguration.then {
 *     // WRONG! onCreate will not be updated.
 *     onCreate = if (condition) {
 *         { ... }
 *     } else {
 *         { ... }
 *     }
 * ```
 *
 * ```kotlin
 * editorConfiguration.then {
 *     // CORRECT! onCreate will use the updated logic based on the condition.
 *     onCreate = {
 *         if (condition) {
 *             ...
 *         } else {
 *             ...
 *         }
 *     }
 * ```
 *
 * @param builder the builder block that configures the [EditorConfiguration].
 * @return an object that is used to configure the editor.
 */
@Composable
fun EditorConfiguration.then(builder: EditorConfigurationBuilder.() -> Unit = {}): EditorConfiguration = then(
    builderFactory = ::EditorConfigurationBuilder,
    builder = builder,
)

/**
 * A composable function that creates and remembers an [EditorConfiguration] instance on top of an existing [EditorConfiguration].
 * Existing configuration effectively becomes the parent configuration for the newly created [EditorConfiguration].
 * It is useful to use this function when you want to extend the editor configuration by preexisting [EditorConfigurationBuilder]s,
 * such as plugins provided by IMG.LY.
 *
 * You can chain as many configurations as you want:
 *
 * ```kotlin
 * Editor(
 *     configuration = {
 *         EditorConfiguration.remember {
 *             onCreate = { ... }
 *             dock = { ... }
 *         }.then(::SomePlugin) {
 *             pluginConfigurationProperty = ...
 *         }.then(::AnotherPlugin)
 *     }
 * )
 * ```
 *
 * Note that both [builderFactory] and [builder] lambdas run only once, therefore you should not have builder property reassignments based on conditions:
 *
 * ```kotlin
 * editorConfiguration.then(::SomePlugin) {
 *     // WRONG! onCreate will not be updated.
 *     onCreate = if (condition) {
 *         { ... }
 *     } else {
 *         { ... }
 *     }
 * ```
 *
 * ```kotlin
 * editorConfiguration.then(::OtherEditorConfigurationBuilder) {
 *     // CORRECT! onCreate will use the updated logic based on the condition.
 *     onCreate = {
 *         if (condition) {
 *             ...
 *         } else {
 *             ...
 *         }
 *     }
 * ```
 *
 * @param builderFactory the factory that should be used to construct [EditorConfiguration].
 * @param builder the builder block that configures the [EditorConfiguration].
 * @return an object that is used to configure the editor.
 */
@Composable
fun <Builder : EditorConfigurationBuilder> EditorConfiguration.then(
    builderFactory: () -> Builder,
    builder: Builder.() -> Unit = {},
): EditorConfiguration = LocalEditorScope.current.run {
    // This can be moved to compile time in the future when we start to use context parameters.
    require((this.editorContext as EditorContextImpl).isValid) {
        "EditorConfiguration.then extension must be invoked only in `configuration` lambda of `Editor` composable."
    }
    androidx.compose.runtime.remember(this@then) {
        EditorContextHolder.editorContext = editorContext
        EditorConfigurationHolder.editorConfiguration = this@then
        builderFactory()
            .also {
                EditorConfigurationHolder.editorConfiguration = null
                EditorContextHolder.editorContext = null
            }
            .apply(builder)
    }.build()
}

internal object EditorContextHolder {
    var editorContext: EditorContext? = null
}

internal object EditorConfigurationHolder {
    var editorConfiguration: EditorConfiguration? = null
}
