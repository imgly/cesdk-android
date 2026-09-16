package ly.img.editor.base.dock.options.captions

import android.content.ActivityNotFoundException
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.imeAnimationTarget
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import ly.img.editor.base.dock.BottomSheetContent
import ly.img.editor.base.ui.Event
import ly.img.editor.core.LocalEditorScope
import ly.img.editor.core.R
import ly.img.editor.core.UnstableEditorApi
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.ui.SheetHeader
import ly.img.engine.DesignBlock
import java.io.File

/**
 * Captions sheet: an "Add Captions" state that creates the first caption, switching to an "Edit Captions" list
 * once captions exist. All engine access goes through the `isValid`-guarded [CaptionsEngine].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CaptionsSheet(
    uiState: CaptionsUiState,
    onEvent: (EditorEvent) -> Unit,
) {
    val editorScope = LocalEditorScope.current
    val editorContext = with(editorScope) { editorContext }
    val engine = editorContext.engine
    // `mutableStateOf`, not `stateOf`: `stateOf` throws when the key was never declared, which is the case
    // whenever the plugin is not installed.
    val captionsGeneration = editorContext.mutableStateOf<(suspend () -> File?)?>(
        key = AUTO_CAPTIONS_GENERATOR_KEY,
        initial = null,
    ).value
    val coroutineScope = rememberCoroutineScope()
    val captionsEngine = remember(engine) {
        CaptionsEngine(engine) { throwable -> onEvent(Event.OnError(throwable)) }
    }
    val focusManager = LocalFocusManager.current
    val controller = rememberCaptionsEditingController(
        captionsEngine = captionsEngine,
        coroutineScope = coroutineScope,
        // Editor-scoped: a generation survives the sheet being dismissed, and a reopened sheet finds it running.
        generationScope = editorContext.coroutineScope,
        generationJobState = editorContext.mutableStateOf(key = GENERATION_JOB_KEY, initial = null),
        clearFocus = { focusManager.clearFocus() },
    )
    val captions = controller.captions
    val lazyListState = rememberLazyListState()
    var isDeleteAllConfirmationVisible by remember { mutableStateOf(false) }
    var importErrorMessage by remember { mutableStateOf<String?>(null) }
    // Only one row is swiped open at a time; editing closes it because its actions mutate uncommitted text.
    var revealedCaption by remember { mutableStateOf<DesignBlock?>(null) }

    // The picker is launched from here rather than through `EditorEvent.LaunchContract` because the outcome drives this
    // sheet: the Import button's progress and, on failure, an alert with import-specific copy — neither of which that
    // event's result callback may capture. The captions sheet is composed by the host activity, so it owns a launcher.
    val context = LocalContext.current
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { pickedUri ->
        val uri = pickedUri ?: return@rememberLauncherForActivityResult
        controller.importCaptions(
            stageFile = { stageCaptionFileForImport(context.contentResolver, uri) },
            onFailure = { throwable -> importErrorMessage = context.captionImportErrorMessage(throwable) },
        )
    }

    // Undo/redo run outside the sheet, so funnel the engine's history signal into the sheet's own reload path.
    LaunchedEffect(engine) {
        engine.editor.onHistoryUpdatedWithKind().collect {
            controller.refresh()
            controller.reloadEditingValueIfChanged()
        }
    }

    // Open on the caption selected on the canvas rather than at the top of the list.
    LaunchedEffect(uiState.deepLinkTarget) {
        val target = uiState.deepLinkTarget ?: return@LaunchedEffect
        val index = captionsEngine.captions().indexOf(target)
        if (index >= 0) {
            controller.select(target)
            lazyListState.animateScrollToItem(index)
        }
    }

    val editingCaption = controller.editingCaption
    val isEditing = editingCaption != null
    LaunchedEffect(isEditing) {
        if (isEditing) revealedCaption = null
    }
    LaunchedEffect(editingCaption, captions) {
        val target = editingCaption ?: return@LaunchedEffect
        val index = captions.indexOf(target).takeIf { it >= 0 } ?: return@LaunchedEffect
        // Only scroll an off-screen row: `scrollToItem` snaps it flush to the top, yanking a visible list under the finger.
        val isVisible = lazyListState.layoutInfo.visibleItemsInfo.any { it.index == index }
        if (!isVisible) lazyListState.scrollToItem(index)
        // `requestFocus` fails until the row is attached, so retry across a few frames.
        repeat(FOCUS_ATTEMPTS) {
            if (controller.requestFocus(target)) return@LaunchedEffect
            withFrameNanos { }
        }
    }

    // Editing changes the content height; the host only recomputes its drag anchors when the sheet re-settles.
    LaunchedEffect(isEditing) {
        onEvent(EditorEvent.Sheet.Expand(animate = true))
    }

    // Compose keeps a field focused when the system hides the keyboard, so a back press would otherwise leave the row
    // editing with no keyboard under it. Waiting for the keyboard to have shown first skips the frames it animates in over.
    // Derived, not read directly: the inset changes on every frame the keyboard animates over, while the flag it
    // feeds changes twice — read raw, each of those frames would recompose the sheet.
    val imeInsets = WindowInsets.ime
    val density = LocalDensity.current
    val isImeVisible by remember(imeInsets, density) { derivedStateOf { imeInsets.getBottom(density) > 0 } }
    var hasImeShown by remember(editingCaption) { mutableStateOf(false) }
    LaunchedEffect(editingCaption, isImeVisible) {
        if (editingCaption == null) return@LaunchedEffect
        if (isImeVisible) {
            hasImeShown = true
        } else if (hasImeShown) {
            // The keyboard was dismissed, not the caption, so it stays selected.
            controller.dismissKeyboard()
        }
    }

    DisposableEffect(editorContext) {
        onDispose { controller.commitEditingText() }
    }

    // The bar floats in its own window (see [ImeTopPositionProvider]), so the space it covers is reserved here.
    var barHeightPx by remember { mutableIntStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            // The *target* inset, not the animating one: the sheet is sized by its content and the host samples
            // that height only when it settles, so an animating inset anchors it to a transient value.
            .windowInsetsPadding(WindowInsets.imeAnimationTarget)
            .padding(
                bottom = with(LocalDensity.current) {
                    (if (editingCaption != null) barHeightPx else 0).toDp()
                },
            ),
    ) {
        SheetHeader(
            title = stringResource(
                if (captions.isEmpty()) {
                    R.string.ly_img_editor_sheet_captions_title_add
                } else {
                    R.string.ly_img_editor_sheet_captions_title_edit
                },
            ),
            onClose = { onEvent(EditorEvent.Sheet.Close(animate = true)) },
        )

        if (captions.isEmpty()) {
            if (controller.generationJob != null) {
                CaptionsGeneratingContent(onCancel = controller::cancelGeneration)
            } else {
                CaptionsAddContent(
                    isEnabled = !controller.isMutating,
                    isImporting = controller.isImporting,
                    // Generation is offered only when a callback is configured, and only when there is audio to
                    // transcribe.
                    canGenerate = if (captionsGeneration == null) {
                        null
                    } else {
                        remember(controller.refreshToken) { captionsEngine.hasAudioVisualContent() }
                    },
                    onGenerate = generate@{
                        val generation = captionsGeneration ?: return@generate
                        controller.generateCaptions(
                            generate = { generation() },
                            // Reported through the editor's snackbar rather than a dialog this sheet owns: a
                            // generation outlives the sheet, so a failure can land while it is closed, and a
                            // sheet-bound dialog would sit unseen until the user happened to reopen.
                            onNoSpeech = {
                                onEvent(Event.OnToast(R.string.ly_img_editor_sheet_captions_generate_error_no_speech))
                            },
                            onFailure = { throwable ->
                                onEvent(Event.OnToast(R.string.ly_img_editor_sheet_captions_generate_error_generic))
                                // The message stays generic; the cause reaches the integrator's `onError` callback.
                                onEvent(Event.OnError(throwable))
                            },
                        )
                    },
                    onCreate = controller::addCaption,
                    onImport = {
                        try {
                            importLauncher.launch(CAPTION_IMPORT_MIME_TYPES)
                        } catch (_: ActivityNotFoundException) {
                            onEvent(Event.OnToast(R.string.ly_img_editor_error_activity_not_found))
                        }
                    },
                )
            }
        } else {
            CaptionsList(
                captions = captions,
                controller = controller,
                lazyListState = lazyListState,
                revealedCaption = revealedCaption,
                onRevealChange = { caption, revealed -> revealedCaption = caption.takeIf { revealed } },
                onDeleteAll = {
                    // Drop the keyboard first so the dialog isn't presented over it; the row stays selected behind it.
                    controller.dismissKeyboard()
                    isDeleteAllConfirmationVisible = true
                },
                modifier = Modifier.weight(1f, fill = false),
            )
        }
    }

    if (editingCaption != null) {
        CaptionsKeyboardActionBarPopup(
            editingCaption = editingCaption,
            captions = captions,
            controller = controller,
            barHeightPx = barHeightPx,
            onBarHeightChange = { barHeightPx = it },
        )
    }

    if (isDeleteAllConfirmationVisible) {
        DeleteAllCaptionsDialog(
            onDismiss = { isDeleteAllConfirmationVisible = false },
            onConfirm = {
                isDeleteAllConfirmationVisible = false
                controller.deleteAllCaptions()
            },
        )
    }

    importErrorMessage?.let { message ->
        CaptionsErrorDialog(
            title = stringResource(R.string.ly_img_editor_dialog_captions_import_error_title),
            message = message,
            onDismiss = { importErrorMessage = null },
        )
    }
}

/**
 * The keyboard action bar, floated on top of the keyboard rather than placed in the sheet — see
 * [ImeTopPositionProvider].
 *
 * Its own composable because it reads the live IME inset, which changes on every frame the keyboard animates
 * over: kept in the sheet's body, that read would recompose the whole sheet, list included, for each of them.
 */
@Composable
private fun CaptionsKeyboardActionBarPopup(
    editingCaption: DesignBlock,
    captions: List<DesignBlock>,
    controller: CaptionsEditingController,
    barHeightPx: Int,
    onBarHeightChange: (Int) -> Unit,
) {
    val imeHeightPx = WindowInsets.ime.getBottom(LocalDensity.current)
    val windowHeightPx = LocalView.current.rootView.height
    Popup(
        popupPositionProvider = remember(windowHeightPx, imeHeightPx) {
            ImeTopPositionProvider(windowHeightPx = windowHeightPx, imeHeightPx = imeHeightPx)
        },
        properties = PopupProperties(focusable = false),
    ) {
        Surface(
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier.onSizeChanged { onBarHeightChange(it.height) },
        ) {
            CaptionsKeyboardActionBar(
                barTopPx = windowHeightPx - imeHeightPx - barHeightPx,
                isFirst = captions.firstOrNull() == editingCaption,
                isLast = captions.lastOrNull() == editingCaption,
                isEnabled = !controller.isMutating,
                onDelete = { controller.deleteCaption(editingCaption) },
                onMerge = { controller.mergeWithPrevious(editingCaption) },
                onSplit = {
                    controller.splitAtCaret(
                        caption = editingCaption,
                        caretUtf16 = controller.editingValue?.selection?.min ?: 0,
                    )
                },
                onAddAfter = { controller.addCaptionAfter(editingCaption) },
                onPrevious = { controller.focusRelative(-1) },
                onNext = { controller.focusRelative(1) },
                onDone = controller::endEditing,
            )
        }
    }
}

/** Editor-scoped so a generation, and the view showing it, survive the sheet being dismissed. */
private const val GENERATION_JOB_KEY = "ly.img.editor.captions.generationJob"

/** How many frames focus is retried for while a freshly created row lays out. */
private const val FOCUS_ATTEMPTS = 10

/**
 * The empty state: generate the captions automatically, create the first one, or import a file.
 *
 * Import is offered here only. Replacing existing captions goes through Delete All first, so the list state
 * never has to reconcile an import with captions the user is editing.
 *
 * @param canGenerate `null` when no generation callback is configured, which hides the action entirely; `false`
 * when one is but the scene has no audio to transcribe.
 */
@Composable
private fun CaptionsAddContent(
    isEnabled: Boolean,
    isImporting: Boolean,
    canGenerate: Boolean?,
    onGenerate: () -> Unit,
    onCreate: () -> Unit,
    onImport: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (canGenerate != null) {
            // Only automatic transcription gets the filled treatment; the other two stay tonal.
            Button(
                onClick = onGenerate,
                enabled = isEnabled && canGenerate,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(text = stringResource(R.string.ly_img_editor_sheet_captions_button_generate))
            }
        }
        CaptionsActionButton(
            text = stringResource(R.string.ly_img_editor_sheet_captions_button_create),
            isEnabled = isEnabled,
            onClick = onCreate,
        )
        CaptionsActionButton(
            text = stringResource(R.string.ly_img_editor_sheet_captions_button_import),
            isEnabled = isEnabled,
            isLoading = isImporting,
            onClick = onImport,
        )
    }
}

/** Shown in place of the Add actions while a generation is in flight. */
@Composable
private fun CaptionsGeneratingContent(onCancel: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        CircularProgressIndicator(modifier = Modifier.padding(vertical = 16.dp))
        Text(
            text = stringResource(R.string.ly_img_editor_sheet_captions_generating),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 16.dp),
        )
        CaptionsActionButton(
            text = stringResource(R.string.ly_img_editor_button_cancel),
            isEnabled = true,
            onClick = onCancel,
        )
    }
}

/** The caption rows, followed by the Add and Delete all actions. */
@Composable
private fun CaptionsList(
    captions: List<DesignBlock>,
    controller: CaptionsEditingController,
    lazyListState: androidx.compose.foundation.lazy.LazyListState,
    revealedCaption: DesignBlock?,
    onRevealChange: (DesignBlock, Boolean) -> Unit,
    onDeleteAll: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val editingCaption = controller.editingCaption
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = lazyListState,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        items(captions, key = { it }) { caption ->
            val isEditing = editingCaption == caption
            val engineText = remember(caption, controller.refreshToken) { controller.textOf(caption) }
            CaptionRow(
                value = if (isEditing) {
                    controller.editingValue ?: TextFieldValue(engineText)
                } else {
                    TextFieldValue(engineText)
                },
                externalRevision = controller.editingValueRevision,
                onValueChange = { newValue -> controller.onRowValueChange(caption, newValue) },
                onOperation = { operation, liveText ->
                    controller.runKeyOperation(operation, caption, liveText)
                },
                onFocusChanged = { isFocused -> controller.onRowFocusChanged(caption, isFocused) },
                focusRequester = controller.focusRequester(caption),
                isSelected = controller.selectedCaption == caption,
                hasPreviousCaption = captions.firstOrNull() != caption,
                areActionsAvailable = editingCaption == null && !controller.isMutating,
                areAccessibilityActionsAvailable = editingCaption != caption && !controller.isMutating,
                isAccessibilityMergeAvailable = editingCaption == null && !controller.isMutating,
                isRevealed = revealedCaption == caption,
                onRevealChange = { revealed -> onRevealChange(caption, revealed) },
                onDelete = {
                    onRevealChange(caption, false)
                    controller.deleteCaption(caption)
                },
                onAddAfter = {
                    onRevealChange(caption, false)
                    controller.addCaptionAfter(caption)
                },
                onMerge = {
                    onRevealChange(caption, false)
                    controller.mergeWithPrevious(caption)
                },
            )
        }
        item {
            CaptionsActionButton(
                text = stringResource(R.string.ly_img_editor_sheet_captions_button_add),
                isEnabled = !controller.isMutating,
                onClick = controller::addCaption,
                modifier = Modifier.padding(top = 8.dp),
            )
        }
        item {
            TextButton(
                onClick = onDeleteAll,
                enabled = !controller.isMutating,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
            ) {
                Text(text = stringResource(R.string.ly_img_editor_sheet_captions_button_delete_all))
            }
        }
    }
}

/**
 * The sheet's full-width tonal button, shared by the Add and Import actions.
 *
 * While [isLoading] the label gives way to a progress indicator, sized to sit within the button's padding so
 * the sheet does not jump at the default font scale.
 */
@Composable
private fun CaptionsActionButton(
    text: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
) {
    Button(
        onClick = onClick,
        enabled = isEnabled && !isLoading,
        modifier = modifier.fillMaxWidth(),
        colors = ButtonDefaults.filledTonalButtonColors(),
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                // The button's own content color, so the spinner fades with it while the button is disabled.
                color = LocalContentColor.current,
                strokeWidth = captionActionButtonProgressStroke,
                modifier = Modifier.size(captionActionButtonProgressSize),
            )
        } else {
            Text(text = text)
        }
    }
}

/** Hosts [CaptionsSheet] in the editor's bottom sheet. */
@OptIn(UnstableEditorApi::class)
internal class CaptionsBottomSheetContent(
    override val type: SheetType,
    val uiState: CaptionsUiState,
) : BottomSheetContent

/** The key `ly.img:plugin-auto-captions` publishes its generator under. Duplicated there — keep both in step. */
private const val AUTO_CAPTIONS_GENERATOR_KEY = "ly.img.editor.plugin.autoCaptions.generator"
