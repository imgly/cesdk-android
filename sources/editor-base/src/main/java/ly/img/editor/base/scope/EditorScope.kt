package ly.img.editor.base.scope

import android.net.Uri
import androidx.activity.ComponentActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import ly.img.editor.base.ui.EditorUiViewModel
import ly.img.editor.base.ui.EditorUiViewState
import ly.img.editor.base.ui.SingleEvent
import ly.img.editor.core.EditorScope
import ly.img.editor.core.LocalEditorScope
import ly.img.editor.core.MutableEditorContext
import ly.img.editor.core.UnstableEditorApi
import ly.img.editor.core.ui.Environment
import ly.img.editor.core.ui.library.LibraryViewModel
import ly.img.editor.core.ui.utils.activity

@OptIn(UnstableEditorApi::class)
@Composable
fun EditorScope(
    license: String?,
    userId: String?,
    baseUri: Uri,
    onClose: (Throwable?) -> Unit,
    content: @Composable EditorScope.(EditorUiViewState) -> Unit,
) = LocalEditorScope.current.run {
    val editorContext = editorContext as MutableEditorContext
    val editorViewModelStoreOwner = viewModel<EditorScopeViewModel>()
    val activity = LocalContext.current.activity as ComponentActivity
    remember {
        Environment.init(activity.application)
        mutableStateOf(Unit)
    }
    CompositionLocalProvider(
        LocalViewModelStoreOwner provides editorViewModelStoreOwner,
    ) {
        val libraryViewModel = viewModel {
            LibraryViewModel(
                editorScope = this@run,
            )
        }

        val viewModel = viewModel {
            EditorUiViewModel(
                editorScope = this@run,
                publicState = editorContext.state,
                libraryViewModel = libraryViewModel,
            )
        }
        remember {
            editorContext.init(
                license = license,
                userId = userId,
                baseUri = baseUri,
                activity = activity,
                eventHandler = viewModel,
                coroutineScope = viewModel.viewModelScope,
                timelineOwnerProvider = viewModel::provideTimelineOwner,
            )
            object : RememberObserver {
                override fun onAbandoned() {
                    editorViewModelStoreOwner.clear()
                    editorContext.clear()
                }

                override fun onForgotten() {
                    if (!activity.isChangingConfigurations) {
                        editorViewModelStoreOwner.clear()
                        editorContext.clear()
                    }
                }

                override fun onRemembered() = Unit
            }
        }
        val updatedOnClose by rememberUpdatedState(onClose)
        LaunchedEffect(viewModel) {
            viewModel.uiEvent.collect {
                if (it is SingleEvent.Exit) {
                    updatedOnClose(it.throwable)
                }
            }
        }
        val uiState by viewModel.uiState.collectAsState()
        this.content(uiState)
    }
}
