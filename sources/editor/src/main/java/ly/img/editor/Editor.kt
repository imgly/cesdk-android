package ly.img.editor

import android.net.Uri
import android.util.Log
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalSavedStateRegistryOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.net.toUri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ly.img.editor.base.scope.EditorScope
import ly.img.editor.base.ui.EditorUi
import ly.img.editor.core.EditorScope
import ly.img.editor.core.MutableEditorContext
import ly.img.editor.core.R
import ly.img.editor.core.ScopedProperty
import ly.img.editor.core.configuration.EditorConfiguration
import ly.img.editor.core.configuration.remember
import ly.img.editor.core.engine.EngineRenderTarget
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.library.AssetLibrary
import ly.img.editor.core.theme.EditorTheme
import ly.img.editor.core.theme.fillAndStrokeColors
import ly.img.engine.DesignBlockType

/**
 * The default baseUri value used in [Editor] composable.
 */
val defaultBaseUri: Uri by lazy {
    "https://cdn.img.ly/packages/imgly/cesdk-android/1.74.0-rc.0/assets".toUri()
}

/**
 * Built to provide versatile photo and video editing capabilities. Toggling between edit, preview and pages modes enables users to evaluate their
 * edited photos/videos before export.
 *
 * @param license the license to activate the [ly.img.engine.Engine] with.
 * @param userId an optional unique ID tied to your application's user. This helps us accurately calculate monthly active users (MAU).
 * Especially useful when one person uses the app on multiple devices with a sign-in feature, ensuring they're counted once.
 * Providing this aids in better data accuracy.
 * @param baseUri the base Uri that is used to construct absolute paths from relative paths.
 * absolutePath = baseUri + relativePath.
 * For instance, baseUri can be set to android assets: file:///android_asset/.
 * After setting this path you can, for instance, load example.scene from assets/scenes folder using relative path:
 *      engine.scene.load(sceneUri = Uri.parse("scenes/example.scene"))
 * Default value is [defaultBaseUri].
 * @param engineRenderTarget the target which should be used by the [ly.img.engine.Engine] to render.
 * Default value is [EngineRenderTarget.SURFACE_VIEW].
 * @param configuration the configuration object of the editor. Check [EditorConfiguration.Companion.remember] documentation
 * on how to create and configure the object.
 * Note that this parameter is optional. The default behavior is specified in [EditorConfiguration.Companion.remember].
 * IMPORTANT: [EditorConfiguration] should only be created inside [configuration] composable lambda,
 * otherwise crashes may occur.
 * @param onClose the callback that is invoked when the editor is closed. Here are some of the recommended implementations:
 * 1. activity.finish() if the editor is launched in a standalone activity.
 * 2. fragmentManager.popBackStack() if the editor is launched in a fragment.
 * 3. navController.popBackStack() if the editor is launched as a new composable destination in NavHost.
 * If the optional parameter [Throwable] is not null, it means that the editor is closed due to an error.
 * The value is propagated from [ly.img.editor.core.event.EditorEvent.CloseEditor]. Unless custom types are sent
 * to this function, the throwable is always going to be either an [EditorException] or [ly.img.engine.EngineException].
 */
@Composable
fun Editor(
    license: String? = null,
    userId: String? = null,
    baseUri: Uri = defaultBaseUri,
    engineRenderTarget: EngineRenderTarget = EngineRenderTarget.SURFACE_VIEW,
    uiMode: EditorUiMode = EditorUiMode.SYSTEM,
    configuration: ScopedProperty<EditorScope, EditorConfiguration> = { EditorConfiguration.remember() },
    onClose: (Throwable?) -> Unit = {},
) {
    EditorTheme(
        useDarkTheme = uiMode.useDarkTheme,
    ) {
        EditorScope(
            license = license,
            userId = userId,
            baseUri = baseUri,
            onClose = onClose,
        ) { uiState ->
            EngineInitializer(
                loading = {
                    Dialog(
                        onDismissRequest = {},
                        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                        )
                    }
                },
                success = {
                    val configuration = complete(configuration())
                    EditorUi(
                        configuration = configuration,
                        renderTarget = engineRenderTarget,
                        uiState = uiState,
                    )
                },
                error = { error ->
                    AlertDialog(
                        onDismissRequest = {},
                        properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
                        title = { Text(text = stringResource(R.string.ly_img_editor_dialog_error_title)) },
                        text = { Text(text = error.message ?: "") },
                        confirmButton = {
                            TextButton(onClick = { onClose(error) }) {
                                Text(stringResource(R.string.ly_img_editor_dialog_error_confirm_text))
                            }
                        },
                    )
                },
            )
        }
    }
}

@Composable
private inline fun EditorScope.EngineInitializer(
    loading: @Composable () -> Unit,
    success: @Composable () -> Unit,
    error: @Composable (Exception) -> Unit,
) {
    var status by remember { mutableStateOf<EngineInitStatus?>(null) }
    val savedStateRegistryOwner = LocalSavedStateRegistryOwner.current
    LaunchedEffect(Unit) {
        val loadingJob = launch {
            // Showing loading spinner should be delayed by 300ms as engine may be ready faster than that.
            delay(300)
            status = EngineInitStatus.Loading
        }
        launch {
            try {
                val start = System.currentTimeMillis()
                editorContext.engine.start(
                    license = editorContext.license,
                    userId = editorContext.userId,
                    savedStateRegistryOwner = savedStateRegistryOwner,
                )
                loadingJob.cancelAndJoin()
                Log.d("CESDK", "Engine initialization took ${System.currentTimeMillis() - start} ms")
                status = EngineInitStatus.Success
            } catch (exception: Exception) {
                if (exception is CancellationException) {
                    throw exception
                }
                loadingJob.cancelAndJoin()
                status = EngineInitStatus.Error(exception)
            }
        }
    }
    val internalStatus = status
    when (internalStatus) {
        null -> {}
        is EngineInitStatus.Loading -> loading()
        is EngineInitStatus.Success -> success()
        is EngineInitStatus.Error -> error(internalStatus.exception)
    }
}

private sealed class EngineInitStatus {
    data object Loading : EngineInitStatus()

    data object Success : EngineInitStatus()

    data class Error(
        val exception: Exception,
    ) : EngineInitStatus()
}

@Composable
private fun EditorScope.complete(source: EditorConfiguration): EditorConfiguration {
    val assetLibrary = source.assetLibrary ?: remember { AssetLibrary.getDefault() }
    return remember(source) {
        source.copy(
            onCreate = source.onCreate ?: {
                // Create a scene and append an empty page
                val scene = editorContext.engine.scene.create()
                val page = editorContext.engine.block.create(DesignBlockType.Page)
                editorContext.engine.block.setWidth(block = page, value = 1080F)
                editorContext.engine.block.setHeight(block = page, value = 1080F)
                editorContext.engine.block.appendChild(parent = scene, child = page)
            },
            onLoaded = source.onLoaded ?: {},
            onExport = source.onExport ?: {
                Log.w("CESDK", "EditorConfigurationBuilder.onExport is not implemented.")
                Unit
            },
            onError = source.onError ?: {
                Log.e("CESDK", "Editor/Engine error.", it)
                Unit
            },
            onClose = source.onClose ?: {
                editorContext.eventHandler.send(EditorEvent.CloseEditor())
            },
            onUpload = source.onUpload ?: { definition, _ -> definition },
            onEvent = source.onEvent ?: {},
            assetLibrary = assetLibrary,
            colorPalette = source.colorPalette ?: fillAndStrokeColors,
        ).also {
            (editorContext as? MutableEditorContext)?.updateConfiguration(it)
        }
    }
}
