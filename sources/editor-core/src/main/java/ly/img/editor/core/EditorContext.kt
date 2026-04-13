package ly.img.editor.core

import android.app.Activity
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import ly.img.editor.core.component.TimelineOwner
import ly.img.editor.core.configuration.EditorConfiguration
import ly.img.editor.core.event.EditorEventHandler
import ly.img.editor.core.state.EditorState
import ly.img.engine.Engine
import ly.img.engine.UnstableEngineApi

/**
 * An umbrella interface containing all the useful properties and functions of the current editor:
 * Following can be found:
 *
 * 1. Properties that were provided when calling the Editor composable.
 * 2. [Engine], [EditorEventHandler] and [Activity] of the current editor.
 * 3. [CoroutineScope] of the current editor.
 * 4. Collectable [EditorState] of the editor.
 */
@Stable
interface EditorContext {
    /**
     * The license provided via param when launching the editor.
     */
    val license: String?

    /**
     * The userId provided via param when launching the editor.
     */
    val userId: String?

    /**
     * The baseUri provided via param when launching the editor.
     */
    val baseUri: Uri

    /**
     * The configuration of the current editor.
     */
    val configuration: StateFlow<EditorConfiguration?>

    /**
     * The engine of the current editor.
     */
    val engine: Engine

    /**
     * The activity where the current editor is running.
     */
    val activity: Activity

    /**
     * The coroutine scope that is always alive while editor is running. It also survives configuration
     * changes.
     */
    val coroutineScope: CoroutineScope

    /**
     * The event handler of the current editor.
     */
    val eventHandler: EditorEventHandler

    /**
     * The state flow of the [EditorState].
     */
    val state: StateFlow<EditorState>
}

interface MutableEditorContext : EditorContext {
    override val state: MutableStateFlow<EditorState>

    @OptIn(UnstableEditorApi::class)
    fun init(
        license: String?,
        userId: String?,
        baseUri: Uri,
        activity: Activity,
        eventHandler: EditorEventHandler,
        coroutineScope: CoroutineScope,
        timelineOwnerProvider: () -> TimelineOwner,
    )

    fun updateConfiguration(configuration: EditorConfiguration)

    fun clear()
}

@OptIn(UnstableEditorApi::class)
@Stable
internal class EditorContextImpl :
    MutableEditorContext,
    TimelineOwner {
    @OptIn(UnstableEngineApi::class)
    override val engine: Engine by lazy {
        Engine.getInstance(id = "ly.img.editor").also {
            it.idlingEnabled = true
        }
    }

    override var license: String? = null

    override var userId: String? = null

    private var _baseUri: Uri? = null
    override val baseUri: Uri
        get() = requireNotNull(_baseUri) { "baseUri is not initialized yet." }

    override val configuration: MutableStateFlow<EditorConfiguration?> = MutableStateFlow(null)

    private var _activity: Activity? = null
    override val activity: Activity
        get() = requireNotNull(_activity) { "Activity is not initialized yet." }

    private var _eventHandler: EditorEventHandler? = null
    override val eventHandler: EditorEventHandler
        get() = requireNotNull(_eventHandler) { "EditorEventHandler is not initialized yet." }

    private var _coroutineScope: CoroutineScope? = null
    override val coroutineScope: CoroutineScope
        get() = requireNotNull(_coroutineScope) { "CoroutineScope is not initialized yet." }

    override val state: MutableStateFlow<EditorState> = MutableStateFlow(EditorState())

    private var timelineOwnerProvider: (() -> TimelineOwner)? = null

    override fun init(
        license: String?,
        userId: String?,
        baseUri: Uri,
        activity: Activity,
        eventHandler: EditorEventHandler,
        coroutineScope: CoroutineScope,
        timelineOwnerProvider: () -> TimelineOwner,
    ) {
        this.license = license
        this.userId = userId
        _baseUri = baseUri
        _activity = activity
        _eventHandler = eventHandler
        _coroutineScope = coroutineScope
        this.timelineOwnerProvider = timelineOwnerProvider
    }

    override fun updateConfiguration(configuration: EditorConfiguration) {
        this.configuration.value = configuration
    }

    @Composable
    override fun TimelineContent() {
        remember { requireNotNull(timelineOwnerProvider)() }.TimelineContent()
    }

    override fun clear() {
        license = null
        userId = null
        configuration.value = null
        _baseUri = null
        _activity = null
        _eventHandler = null
        _coroutineScope = null
        timelineOwnerProvider = null
        state.value = EditorState()
    }
}
