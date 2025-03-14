package ly.img.editor.core.ui.library

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import ly.img.editor.core.library.LibraryCategory
import ly.img.editor.core.library.data.UploadAssetSourceType
import ly.img.editor.core.ui.AnyComposable
import ly.img.editor.core.ui.library.util.LibraryEvent
import ly.img.engine.DesignBlock

@Composable
fun AddLibrarySheet(
    libraryCategory: LibraryCategory,
    addToBackgroundTrack: Boolean,
    onClose: () -> Unit,
    onCloseAssetDetails: () -> Unit,
    onSearchFocus: () -> Unit,
    showAnyComposable: (AnyComposable) -> Unit,
    launchGetContent: (String, UploadAssetSourceType, DesignBlock?) -> Unit,
    launchCamera: (Boolean, DesignBlock?) -> Unit,
) {
    val viewModel = viewModel<LibraryViewModel>()
    LibraryCategorySheet(
        libraryCategory = libraryCategory,
        onAssetClick = { wrappedAsset ->
            viewModel.onEvent(
                LibraryEvent.OnAddAsset(
                    wrappedAsset = wrappedAsset,
                    addToBackgroundTrack = addToBackgroundTrack,
                ),
            )
            onClose()
        },
        onUriPick = { assetSource, uri ->
            viewModel.onEvent(
                LibraryEvent.OnAddUri(
                    assetSource = assetSource,
                    uri = uri,
                    addToBackgroundTrack = addToBackgroundTrack,
                ),
            )
            onClose()
        },
        onClose = onClose,
        onCloseAssetDetails = onCloseAssetDetails,
        onSearchFocus = onSearchFocus,
        showAnyComposable = showAnyComposable,
        launchGetContent = { mimeType, uploadAssetSourceType ->
            launchGetContent(mimeType, uploadAssetSourceType, null)
        },
        launchCamera = { launchCamera(it, null) },
    )
}
