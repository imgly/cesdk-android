package ly.img.editor

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import ly.img.editor.core.R
import ly.img.editor.core.UnstableEditorApi
import ly.img.editor.core.component.data.Insets
import ly.img.editor.core.configuration.EditorConfigurationBuilder
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.iconpack.CloudAlertOutline
import ly.img.editor.core.iconpack.IconPack
import ly.img.editor.core.iconpack.WifiCancel
import ly.img.editor.core.library.data.SystemGalleryConfiguration
import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import ly.img.engine.MimeType
import java.io.File
import java.nio.ByteBuffer
import java.util.UUID

/**
 * Base scope class for all the solutions. Contains helper functions, as well as state objects that render
 * overlay content.
 */
@Stable
open class BasicConfigurationBuilder : EditorConfigurationBuilder() {
    /**
     * Whether the in-app system gallery integration is active.
     */
    var systemGalleryConfiguration: SystemGalleryConfiguration by editorContext.mutableStateOf(
        key = "ly.img.editor.state.systemGalleryConfiguration",
        initial = SystemGalleryConfiguration.Disabled,
    )

    /**
     * Whether [Loading] composable should be visible in the overlay.
     */
    var showLoading: Boolean by editorContext.mutableStateOf(
        key = "ly.img.editor.state.showLoading",
        initial = false,
    )

    /**
     * Whether [CloseConfirmationDialog] composable should be visible in the overlay.
     */
    var showCloseConfirmationDialog: Boolean by editorContext.mutableStateOf(
        key = "ly.img.editor.state.showCloseConfirmationDialog",
        initial = false,
    )

    /**
     * Latest error of the editor. Depending on the type, [NoInternetDialog] or [ErrorDialog]
     * composable is visible in the overlay.
     */
    var error: Throwable? by editorContext.mutableStateOf(
        key = "ly.img.editor.state.error",
        initial = null,
    )

    /**
     * A helper function that returns the currently active scene or creates it from the given [sceneUri].
     *
     * @param sceneUri the uri to the scene string that should be used to load if scene is not available
     * @return a scene design block.
     */
    suspend fun getOrLoadScene(
        `_`: Nothing = nothing,
        sceneUri: Uri,
    ): DesignBlock = editorContext.engine.scene.get() ?: editorContext.engine.scene.load(sceneUri = sceneUri)

    /**
     * A helper function that observes the edit mode of the the editor and sets extra canvas insets.
     *
     * @param extraInsets the extra insets to be set whenever edit mode changes.
     * @param callback callback that is invoked whenever the edit mode changes.
     */
    @OptIn(ExperimentalCoroutinesApi::class, UnstableEditorApi::class)
    suspend fun observeEditorEditMode(
        `_`: Nothing = nothing,
        extraInsets: (String) -> Insets = { editMode ->
            Insets(value = if (editMode == "Crop") 24.dp else 16.dp)
        },
        callback: suspend (String) -> Unit = {},
    ) {
        editorContext.engine.editor.onStateChanged()
            .map { editorContext.engine.editor.getEditMode() }
            .onStart { emit(editorContext.engine.editor.getEditMode()) }
            .distinctUntilChanged()
            .onEach { callback(it) }
            .flatMapLatest { editMode ->
                val state = editorContext.state.value
                val oldExtraInsets = state.extraInsets
                val newExtraInsets = extraInsets(editMode)
                if (oldExtraInsets == newExtraInsets || state.activeSheet == null) {
                    flowOf(newExtraInsets)
                } else {
                    val diff = newExtraInsets - oldExtraInsets
                    editorContext.state
                        .mapNotNull { it.activeSheetState }
                        .flatMapLatest { activeSheetState -> snapshotFlow { activeSheetState.progress } }
                        .filterNotNull()
                        .map { oldExtraInsets + diff * it }
                }
            }
            .collect {
                editorContext.eventHandler.send(event = EditorEvent.Insets.SetExtra(it))
            }
    }

    /**
     * A helper function that exports the [block].
     *
     * @param block the design block that should be exported.
     * @param mimeType the mime type to export to.
     * @param preExport the block that should be run right before export on the background engine.
     * @return the exported content as [ByteBuffer].
     */
    suspend fun export(
        `_`: Nothing = nothing,
        block: DesignBlock = requireNotNull(editorContext.engine.scene.get()),
        mimeType: MimeType = MimeType.PDF,
        preExport: suspend Engine.() -> Unit = {
            // The engine instance is background engine here and has nothing to do with editorContext.engine.
            this.scene.getPages().forEach {
                this.block.setScopeEnabled(it, key = "layer/visibility", enabled = true)
                this.block.setVisible(it, visible = true)
            }
        },
    ): ByteBuffer = editorContext.engine.block.export(
        block = block,
        mimeType = mimeType,
        onPreExport = preExport,
    )

    /**
     * A helper function that writes [byteBuffer] into a file.
     *
     * @param byteBuffer the data that should be written in the temporary file.
     * @param mimeType the mime type of the file. Note that it is used to derive the extension of the newly created file.
     * @param file the file that should be written to.
     * @return a file with the content of [byteBuffer].
     */
    suspend fun writeToFile(
        `_`: Nothing = nothing,
        byteBuffer: ByteBuffer,
        mimeType: MimeType = MimeType.PDF,
        file: suspend () -> File = {
            val extension = "." + mimeType.key.split("/").last()
            File.createTempFile(UUID.randomUUID().toString(), extension)
        },
    ): File = withContext(Dispatchers.IO) {
        file().apply {
            outputStream().use { it.channel.write(byteBuffer) }
        }
    }

    /**
     * A helper function that opens a system dialog to share the [file].
     *
     * @param authority the authority of [FileProvider] defined in a <provider> element in your app's manifest.
     * @param file the file that should be shared.
     * @param mimeType the mime type of the file that is used to open the system dialog.
     */
    fun shareFile(
        `_`: Nothing = nothing,
        authority: String = "${editorContext.activity.packageName}.ly.img.editor.fileprovider",
        file: File,
        mimeType: MimeType,
    ) {
        val uri = FileProvider.getUriForFile(editorContext.activity, authority, file)
        shareUri(uri = uri, mimeType = mimeType)
    }

    /**
     * A helper function that opens a system dialog to share the [uri].
     *
     * @param uri the uri that should be shared.
     * @param mimeType the mime type of the content of the uri that is used to open the system dialog.
     */
    fun shareUri(
        `_`: Nothing = nothing,
        uri: Uri,
        mimeType: MimeType,
    ) {
        val shareIntent = Intent().apply {
            action = Intent.ACTION_SEND
            type = mimeType.key
            putExtra(Intent.EXTRA_STREAM, uri)
        }
        editorContext.activity.startActivity(Intent.createChooser(shareIntent, null))
    }

    /**
     * A helper function that:
     * 1. Closes the editor if the editor history is empty.
     * 2. Shows confirmation dialog if the editor history is not empty.
     */
    fun showConfirmationOrCloseEditor() {
        if (editorContext.engine.editor.canUndo()) {
            showCloseConfirmationDialog = true
        } else {
            editorContext.eventHandler.send(event = EditorEvent.CloseEditor())
        }
    }

    /**
     * A helper function that renders the overlay based on [showLoading], [showCloseConfirmationDialog] and [error] state objects.
     *
     * @param backHandler the back handler logic.
     * @param loading the loading overlay logic.
     * @param errorDialog the error dialog logic.
     * @param closeConfirmationDialog the close confirmation dialog logic.
     */
    @Composable
    fun Overlay(
        `_`: Nothing = nothing,
        backHandler: @Composable () -> Unit = {
            val isBackHandlerEnabled by remember {
                combine(
                    editorContext.engine.editor.onHistoryUpdated().map {
                        editorContext.engine.editor.canUndo()
                    },
                    editorContext.state.map { it.isBackHandlerEnabled },
                ) { canUndo, isBackHandlerEnabled ->
                    canUndo && !isBackHandlerEnabled
                }
            }.collectAsState(false)

            BackHandler(isBackHandlerEnabled) {
                editorContext.eventHandler.send(EditorEvent.OnClose())
            }
        },
        loading: @Composable () -> Unit = {
            if (showLoading) {
                Loading()
            }
        },
        errorDialog: @Composable () -> Unit = {
            this@BasicConfigurationBuilder.error?.let {
                ErrorDialog(throwable = it)
            }
        },
        closeConfirmationDialog: @Composable () -> Unit = {
            if (showCloseConfirmationDialog) {
                CloseConfirmationDialog()
            }
        },
        `__`: Nothing = nothing,
    ) {
        backHandler()
        loading()
        errorDialog()
        closeConfirmationDialog()
    }

    /**
     * A helper composable function for displaying a loading overlay.
     *
     * @param onDismissRequest executes when the user tries to dismiss the dialog.
     * @param properties the properties of the dialog.
     * @param content the content of the dialog.
     */
    @Composable
    fun Loading(
        `_`: Nothing = nothing,
        onDismissRequest: () -> Unit = {},
        properties: DialogProperties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        content: @Composable () -> Unit = {
            CircularProgressIndicator(
                modifier = Modifier.testTag("MainLoading"),
                color = MaterialTheme.colorScheme.onPrimary,
            )
        },
    ) {
        Dialog(
            onDismissRequest = onDismissRequest,
            properties = properties,
            content = content,
        )
    }

    /**
     * A helper composable function for displaying a dialog when there is not internet.
     *
     * @param modifier the modifier to apply to the dialog.
     * @param icon the icon to display in the dialog.
     * @param title the title of the dialog.
     * @param text the text of the dialog.
     * @param confirmButton the confirm button of the dialog.
     * @param dismissButton the dismiss button of the dialog.
     * @param onDismissRequest executes when the user tries to dismiss the dialog.
     * @param properties the properties of the dialog.
     */
    @Composable
    fun NoInternetDialog(
        `_`: Nothing = nothing,
        modifier: Modifier = Modifier,
        icon: @Composable (() -> Unit)? = {
            Icon(imageVector = IconPack.WifiCancel, contentDescription = null)
        },
        title: @Composable (() -> Unit)? = {
            Text(text = stringResource(R.string.ly_img_editor_dialog_no_internet_title))
        },
        text: @Composable (() -> Unit)? = {
            Text(text = stringResource(R.string.ly_img_editor_dialog_no_internet_text))
        },
        confirmButton: @Composable () -> Unit = {
            TextButton(
                onClick = {
                    showLoading = false
                    editorContext.eventHandler.send(EditorEvent.CloseEditor(EditorException(EditorException.Code.NO_INTERNET)))
                },
            ) {
                Text(stringResource(R.string.ly_img_editor_dialog_no_internet_button_confirm))
            }
        },
        dismissButton: @Composable (() -> Unit)? = null,
        onDismissRequest: () -> Unit = {},
        properties: DialogProperties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        `__`: Nothing = nothing,
    ) {
        AlertDialog(
            modifier = modifier,
            icon = icon,
            title = title,
            text = text,
            confirmButton = confirmButton,
            dismissButton = dismissButton,
            onDismissRequest = onDismissRequest,
            properties = properties,
        )
    }

    /**
     * A helper composable function for displaying a dialog when the editor captures a [throwable].
     *
     * @param throwable the throwable that was thrown.
     * @param modifier the modifier to apply to the dialog.
     * @param icon the icon to display in the dialog.
     * @param title the title of the dialog.
     * @param text the text of the dialog.
     * @param confirmButton the confirm button of the dialog.
     * @param dismissButton the dismiss button of the dialog.
     * @param onDismissRequest executes when the user tries to dismiss the dialog.
     * @param properties the properties of the dialog.
     */
    @Composable
    fun ErrorDialog(
        `_`: Nothing = nothing,
        throwable: Throwable,
        modifier: Modifier = Modifier,
        icon: @Composable (() -> Unit)? = null,
        title: @Composable (() -> Unit)? = {
            Text(text = stringResource(R.string.ly_img_editor_dialog_error_title))
        },
        text: @Composable (() -> Unit)? = {
            Text(text = throwable.message ?: "")
        },
        confirmButton: @Composable () -> Unit = {
            TextButton(
                onClick = {
                    error = null
                    editorContext.eventHandler.send(EditorEvent.CloseEditor(throwable))
                },
            ) {
                Text(stringResource(R.string.ly_img_editor_dialog_error_confirm_text))
            }
        },
        dismissButton: @Composable (() -> Unit)? = null,
        onDismissRequest: () -> Unit = {},
        properties: DialogProperties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        `__`: Nothing = nothing,
    ) {
        AlertDialog(
            modifier = modifier,
            icon = icon,
            title = title,
            text = text,
            confirmButton = confirmButton,
            dismissButton = dismissButton,
            onDismissRequest = onDismissRequest,
            properties = properties,
        )
    }

    /**
     * A helper composable function for displaying a confirmation dialog when closing the editor.
     *
     * @param modifier the modifier to apply to the dialog.
     * @param icon the icon to display in the dialog.
     * @param title the title of the dialog.
     * @param text the text of the dialog.
     * @param confirmButton the confirm button of the dialog.
     * @param dismissButton the dismiss button of the dialog.
     * @param onDismissRequest executes when the user tries to dismiss the dialog.
     * @param properties the properties of the dialog.
     */
    @Composable
    fun CloseConfirmationDialog(
        `_`: Nothing = nothing,
        modifier: Modifier = Modifier,
        icon: @Composable (() -> Unit)? = {
            Icon(imageVector = IconPack.CloudAlertOutline, contentDescription = null)
        },
        title: @Composable (() -> Unit)? = {
            Text(text = stringResource(R.string.ly_img_editor_dialog_close_confirm_title))
        },
        text: @Composable (() -> Unit)? = {
            Text(text = stringResource(R.string.ly_img_editor_dialog_close_confirm_text))
        },
        onDismissRequest: () -> Unit = {
            showCloseConfirmationDialog = false
        },
        confirmButton: @Composable () -> Unit = {
            TextButton(
                onClick = {
                    onDismissRequest()
                    editorContext.eventHandler.send(EditorEvent.CloseEditor())
                },
            ) {
                Text(stringResource(R.string.ly_img_editor_dialog_close_confirm_button_confirm))
            }
        },
        dismissButton: @Composable (() -> Unit)? = {
            TextButton(
                onClick = onDismissRequest,
            ) {
                Text(stringResource(R.string.ly_img_editor_dialog_close_confirm_button_dismiss))
            }
        },
        properties: DialogProperties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        `__`: Nothing = nothing,
    ) {
        AlertDialog(
            modifier = modifier,
            icon = icon,
            title = title,
            text = text,
            confirmButton = confirmButton,
            dismissButton = dismissButton,
            onDismissRequest = onDismissRequest,
            properties = properties,
        )
    }
}
