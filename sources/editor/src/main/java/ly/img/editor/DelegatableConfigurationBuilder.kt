package ly.img.editor

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import ly.img.editor.core.EditorScope
import ly.img.editor.core.ScopedProperty
import ly.img.editor.core.component.EditorComponent
import ly.img.editor.core.configuration.EditorConfiguration
import ly.img.editor.core.configuration.EditorConfigurationBuilder
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.library.AssetLibrary
import ly.img.editor.core.library.data.UploadAssetSourceType
import ly.img.engine.AssetDefinition

@Stable
open class DelegatableConfigurationBuilder(
    delegate: EditorConfiguration?,
) : EditorConfigurationBuilder() {
    override var onCreate: (suspend EditorScope.() -> Unit)? = delegate?.onCreate

    override var onLoaded: (suspend EditorScope.() -> Unit)? = delegate?.onLoaded

    override var onExport: (suspend EditorScope.() -> Unit)? = delegate?.onExport

    override var onClose: (suspend EditorScope.() -> Unit)? = delegate?.onClose

    override var onEvent: (EditorScope.(EditorEvent) -> Unit)? = delegate?.onEvent

    override var onUpload: (suspend EditorScope.(AssetDefinition, UploadAssetSourceType) -> AssetDefinition)? = delegate?.onUpload

    override var onError: (suspend EditorScope.(Throwable) -> Unit)? = delegate?.onError

    override var colorPalette: ScopedProperty<EditorScope, List<Color>>? = delegate?.colorPalette?.delegate()

    override var assetLibrary: ScopedProperty<EditorScope, AssetLibrary>? = delegate?.assetLibrary?.delegate()

    override var dock: ScopedProperty<EditorScope, EditorComponent<*>>? = delegate?.dock?.delegate()

    override var navigationBar: ScopedProperty<EditorScope, EditorComponent<*>>? = delegate?.navigationBar?.delegate()

    override var inspectorBar: ScopedProperty<EditorScope, EditorComponent<*>>? = delegate?.inspectorBar?.delegate()

    override var canvasMenu: ScopedProperty<EditorScope, EditorComponent<*>>? = delegate?.canvasMenu?.delegate()

    override var bottomPanel: ScopedProperty<EditorScope, EditorComponent<*>>? = delegate?.bottomPanel?.delegate()

    override var overlay: ScopedProperty<EditorScope, EditorComponent<*>>? = delegate?.overlay?.delegate()

    private fun <T : Any?> T.delegate(): ScopedProperty<EditorScope, T> = { this@delegate }
}
