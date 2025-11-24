package ly.img.editor

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ly.img.editor.core.EditorContext
import ly.img.editor.core.EditorScope
import ly.img.editor.core.library.data.GalleryPermissionManager
import ly.img.editor.core.ui.scope.EditorContextImpl

@Composable
internal fun rememberEditorScope(
    engineConfiguration: EngineConfiguration,
    editorConfiguration: EditorConfiguration<*>,
): EditorScope = remember(engineConfiguration, editorConfiguration) {
    GalleryPermissionManager.applyConfiguration(engineConfiguration.systemGallery)
    object : EditorScope() {
        override val impl: EditorContext =
            EditorContextImpl(
                license = engineConfiguration.license,
                userId = engineConfiguration.userId,
                baseUri = engineConfiguration.baseUri,
                colorPalette = editorConfiguration.colorPalette,
                assetLibrary = editorConfiguration.assetLibrary,
                dock = editorConfiguration.dock,
                inspectorBar = editorConfiguration.inspectorBar,
                canvasMenu = editorConfiguration.canvasMenu,
                navigationBar = editorConfiguration.navigationBar,
                overlay = (editorConfiguration as EditorConfiguration<Parcelable>).overlay,
            )
    }
}
