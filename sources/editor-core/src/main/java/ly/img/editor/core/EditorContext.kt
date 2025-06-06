package ly.img.editor.core

import android.app.Activity
import android.net.Uri
import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.flow.StateFlow
import ly.img.editor.core.component.CanvasMenu
import ly.img.editor.core.component.Dock
import ly.img.editor.core.component.InspectorBar
import ly.img.editor.core.component.NavigationBar
import ly.img.editor.core.event.EditorEventHandler
import ly.img.editor.core.library.AssetLibrary
import ly.img.editor.core.state.EditorState
import ly.img.engine.Engine

/**
 * An umbrella interface containing all the useful properties and functions of the current editor:
 * Following can be found:
 *
 * 1. Properties that were provided via [ly.img.editor.EngineConfiguration] and [ly.img.editor.EditorConfiguration]
 * 2. [Engine], [EditorEventHandler] and [Activity] of the current editor.
 * 3. Useful functions for getting information on the current state of the editor.
 */
@Stable
interface EditorContext {
    /**
     * The license provided via [ly.img.editor.EngineConfiguration.license].
     */
    val license: String

    /**
     * The userId provided via [ly.img.editor.EngineConfiguration.userId].
     */
    val userId: String?

    /**
     * The baseUri provided via [ly.img.editor.EngineConfiguration.baseUri].
     */
    val baseUri: Uri

    /**
     * The colorPalette provided via [ly.img.editor.EditorConfiguration.colorPalette].
     */
    val colorPalette: List<Color>

    /**
     * The assetLibrary provided via [ly.img.editor.EditorConfiguration.assetLibrary].
     */
    val assetLibrary: AssetLibrary

    /**
     * The overlay provided via [ly.img.editor.EditorConfiguration.overlay].
     */
    val overlay: (@Composable (EditorScope.(Parcelable) -> Unit))?

    /**
     * The dock provided via [ly.img.editor.EditorConfiguration.dock].
     */
    val dock: (@Composable (EditorScope.() -> Dock))?

    /**
     * The inspector bar provided via [ly.img.editor.EditorConfiguration.inspectorBar].
     */
    val inspectorBar: @Composable ((EditorScope.() -> InspectorBar))?

    /**
     * The canvas menu provided via [ly.img.editor.EditorConfiguration.canvasMenu].
     */
    val canvasMenu: @Composable ((EditorScope.() -> CanvasMenu))?

    /**
     * The navigation bar provided via [ly.img.editor.EditorConfiguration.navigationBar].
     */
    val navigationBar: @Composable ((EditorScope.() -> NavigationBar))?

    /**
     * The engine of the current editor.
     */
    val engine: Engine

    /**
     * The activity where the current editor is running.
     */
    val activity: Activity

    /**
     * The event handler of the current editor.
     */
    val eventHandler: EditorEventHandler

    /**
     * The state flow of the [EditorState]. state.current returns the current state of the editor.
     */
    val state: StateFlow<EditorState>
}
