package ly.img.editor.core.ui.scope

import android.app.Activity
import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.StateFlow
import ly.img.editor.core.EditorContext
import ly.img.editor.core.EditorScope
import ly.img.editor.core.LocalEditorScope
import ly.img.editor.core.component.CanvasMenu
import ly.img.editor.core.component.Dock
import ly.img.editor.core.component.InspectorBar
import ly.img.editor.core.component.NavigationBar
import ly.img.editor.core.event.EditorEventHandler
import ly.img.editor.core.library.AssetLibrary
import ly.img.editor.core.state.EditorState
import ly.img.editor.core.ui.utils.activity
import ly.img.engine.Engine
import ly.img.engine.UnstableEngineApi
import kotlin.time.Duration

@Composable
fun EditorScope(
    editorScope: EditorScope,
    content: @Composable () -> Unit,
) {
    val editorViewModelStoreOwner = viewModel<EditorScopeViewModel>()
    val activity = LocalContext.current.activity
    remember {
        object : RememberObserver {
            override fun onAbandoned() {
                editorViewModelStoreOwner.clear()
            }

            override fun onForgotten() {
                if (activity?.isChangingConfigurations == false) {
                    editorViewModelStoreOwner.clear()
                }
            }

            override fun onRemembered() = Unit
        }
    }
    CompositionLocalProvider(
        LocalViewModelStoreOwner provides editorViewModelStoreOwner,
        LocalEditorScope provides editorScope,
        content = content,
    )
}

class EditorContextImpl(
    override val license: String?,
    override val userId: String?,
    override val baseUri: Uri,
    override val colorPalette: List<Color>,
    override val assetLibrary: AssetLibrary,
    override val overlay: (@Composable (EditorScope.(Parcelable) -> Unit)?),
    override val dock: (@Composable (EditorScope.() -> Dock))?,
    override val inspectorBar: (@Composable (EditorScope.() -> InspectorBar))?,
    override val canvasMenu: (@Composable (EditorScope.() -> CanvasMenu))?,
    override val navigationBar: @Composable (EditorScope.() -> NavigationBar)?,
) : EditorContext {
    private var minimumVideoDurationState by mutableStateOf<Duration?>(null)
    private var maximumVideoDurationState by mutableStateOf<Duration?>(null)

    override val minimumVideoDuration: Duration?
        get() = minimumVideoDurationState

    override val maximumVideoDuration: Duration?
        get() = maximumVideoDurationState

    override fun setVideoDurationConstraints(
        minimumVideoDuration: Duration?,
        maximumVideoDuration: Duration?,
    ) {
        minimumVideoDurationState = minimumVideoDuration
        maximumVideoDurationState = maximumVideoDuration
    }

    @OptIn(UnstableEngineApi::class)
    override val engine: Engine by lazy {
        Engine.getInstance(id = "ly.img.editor").also {
            it.idlingEnabled = true
        }
    }

    private var _activity: Activity? = null
    override val activity: Activity
        get() = requireNotNull(_activity) { "Activity is not initialized yet." }

    private var _eventHandler: EditorEventHandler? = null
    override val eventHandler: EditorEventHandler
        get() = requireNotNull(_eventHandler) { "EditorEventHandler is not initialized yet." }

    private var _state: StateFlow<EditorState>? = null
    override val state: StateFlow<EditorState>
        get() = requireNotNull(_state) { "StateFlow<EditorState> is not initialized yet." }

    fun init(
        activity: Activity,
        eventHandler: EditorEventHandler,
        state: StateFlow<EditorState>,
    ) {
        _activity = activity
        _eventHandler = eventHandler
        _state = state
    }

    fun clear() {
        _activity = null
        _eventHandler = null
        _state = null
    }
}
