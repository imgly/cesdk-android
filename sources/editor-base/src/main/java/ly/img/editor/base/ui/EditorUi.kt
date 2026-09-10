package ly.img.editor.base.ui

import android.annotation.SuppressLint
import android.content.ActivityNotFoundException
import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import ly.img.editor.base.components.EditingTextCard
import ly.img.editor.base.dock.AdjustmentSheetContent
import ly.img.editor.base.dock.CustomBottomSheetContent
import ly.img.editor.base.dock.EffectSheetContent
import ly.img.editor.base.dock.FillStrokeBottomSheetContent
import ly.img.editor.base.dock.FormatBottomSheetContent
import ly.img.editor.base.dock.LayerBottomSheetContent
import ly.img.editor.base.dock.LibraryAddBottomSheetContent
import ly.img.editor.base.dock.LibraryReplaceBottomSheetContent
import ly.img.editor.base.dock.LibraryTabsBottomSheetContent
import ly.img.editor.base.dock.OptionsBottomSheetContent
import ly.img.editor.base.dock.options.adjustment.AdjustmentOptionsSheet
import ly.img.editor.base.dock.options.animation.AnimationBottomSheetContent
import ly.img.editor.base.dock.options.animation.AnimationSheet
import ly.img.editor.base.dock.options.colors.ColorsBottomSheetContent
import ly.img.editor.base.dock.options.colors.ColorsSheet
import ly.img.editor.base.dock.options.crop.CropBottomSheetContent
import ly.img.editor.base.dock.options.crop.CropSheet
import ly.img.editor.base.dock.options.effect.EffectSelectionSheet
import ly.img.editor.base.dock.options.fillstroke.FillStrokeOptionsSheet
import ly.img.editor.base.dock.options.font.FontBottomSheetContent
import ly.img.editor.base.dock.options.font.FontSheet
import ly.img.editor.base.dock.options.fontSize.FontSizeBottomSheetContent
import ly.img.editor.base.dock.options.fontSize.FontSizeSheet
import ly.img.editor.base.dock.options.format.FormatOptionsSheet
import ly.img.editor.base.dock.options.layer.LayerOptionsSheet
import ly.img.editor.base.dock.options.reorder.ReorderBottomSheetContent
import ly.img.editor.base.dock.options.reorder.ReorderSheet
import ly.img.editor.base.dock.options.shapeoptions.ShapeOptionsSheet
import ly.img.editor.base.dock.options.speed.SpeedBottomSheetContent
import ly.img.editor.base.dock.options.speed.SpeedSheet
import ly.img.editor.base.dock.options.textBackground.TextBackgroundBottomSheet
import ly.img.editor.base.dock.options.textBackground.TextBackgroundBottomSheetContent
import ly.img.editor.base.dock.options.volume.VolumeBottomSheetContent
import ly.img.editor.base.dock.options.volume.VolumeSheet
import ly.img.editor.base.engine.EngineCanvasView
import ly.img.editor.compose.bottomsheet.ModalBottomSheetDefaults
import ly.img.editor.compose.bottomsheet.ModalBottomSheetLayout
import ly.img.editor.compose.bottomsheet.ModalBottomSheetState
import ly.img.editor.compose.bottomsheet.rememberModalBottomSheetState
import ly.img.editor.core.EditorScope
import ly.img.editor.core.R
import ly.img.editor.core.UnstableEditorApi
import ly.img.editor.core.component.EditorComponent
import ly.img.editor.core.compose.rememberLastValue
import ly.img.editor.core.configuration.EditorConfiguration
import ly.img.editor.core.engine.EngineRenderTarget
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.iconpack.Close
import ly.img.editor.core.iconpack.IconPack
import ly.img.editor.core.navbar.SystemNavBar
import ly.img.editor.core.sheet.SheetStyle
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.sheet.SheetValue
import ly.img.editor.core.theme.surface1
import ly.img.editor.core.theme.surface2
import ly.img.editor.core.ui.AnyComposable
import ly.img.editor.core.ui.library.AddLibrarySheet
import ly.img.editor.core.ui.library.AddLibraryTabsSheet
import ly.img.editor.core.ui.library.ReplaceLibrarySheet
import ly.img.editor.core.ui.permissions.PermissionManager.Companion.hasCameraPermission
import ly.img.editor.core.ui.permissions.PermissionManager.Companion.hasCameraPermissionInManifest
import ly.img.editor.core.ui.permissions.PermissionsView
import ly.img.editor.core.ui.sheet.Sheet
import ly.img.editor.core.ui.utils.activity
import ly.img.editor.core.ui.utils.lifecycle.LifecycleEventEffect
import ly.img.editor.core.ui.utils.toPx

@OptIn(FlowPreview::class, UnstableEditorApi::class)
@SuppressLint("UnusedMaterial3ScaffoldPaddingParameter")
@Composable
fun EditorScope.EditorUi(
    configuration: EditorConfiguration,
    renderTarget: EngineRenderTarget,
    uiState: EditorUiViewState,
) {
    val viewModel = viewModel<EditorUiViewModel>()
    // Passing onEvent via params causes unnecessary recompositions which affects performance.
    // Reason is unknown.
    val onEvent = remember(viewModel) { { event: EditorEvent -> viewModel.send(event) } }
    val uiScope = rememberCoroutineScope()
    val bottomSheetContent by viewModel.bottomSheetContent.collectAsState()
    val snackBarHostState = remember { SnackbarHostState() }
    var sheetStyle by remember(bottomSheetContent?.type) { mutableStateOf(bottomSheetContent?.type?.style) }
    val bottomSheetState = rememberLastValue<ModalBottomSheetState>(sheetStyle) {
        val internalSheetStyle = sheetStyle
        val initialValue = when {
            internalSheetStyle == null -> SheetValue.Hidden
            internalSheetStyle.isHalfExpandingEnabled && internalSheetStyle.isHalfExpandedInitially -> SheetValue.HalfExpanded
            else -> SheetValue.Expanded
        }
        if (isValueSet) {
            ModalBottomSheetState(
                swipeableState = lastValue.swipeableState,
                isSkipHalfExpanded = sheetStyle?.isHalfExpandingEnabled?.not() ?: true,
            )
        } else {
            ModalBottomSheetState(
                initialValue = if (sheetStyle?.animateInitialValue == true) SheetValue.Hidden else initialValue,
                isSkipHalfExpanded = sheetStyle?.isHalfExpandingEnabled?.not() ?: true,
            )
        }.also {
            viewModel.send(Event.OnBottomSheetStateChange(state = it.swipeableState))
        }
    }

    val isBackHandlerEnabledFlow = remember { editorContext.state.map { it.isBackHandlerEnabled } }
    val isBackHandlerEnabled by isBackHandlerEnabledFlow.collectAsState(false)
    BackHandler(isBackHandlerEnabled) {
        viewModel.send(
            Event.OnBackPress(
                bottomSheetOffset = bottomSheetState.swipeableState.offset ?: Float.MAX_VALUE,
                bottomSheetMaxOffset = bottomSheetState.swipeableState.maxOffset,
            ),
        )
    }
    LaunchedEffect(bottomSheetContent?.type) {
        if (bottomSheetContent == null && bottomSheetState.isVisible) bottomSheetState.snapTo(SheetValue.Hidden)
        val sheetContent = bottomSheetContent
        if (sheetContent != null && sheetStyle?.animateInitialValue == true) {
            if (sheetContent.isHalfExpandingEnabled && sheetContent.isInitialExpandHalf) {
                bottomSheetState.halfExpand()
            } else {
                bottomSheetState.expand()
            }
        }
    }

    LifecycleEventEffect(event = Lifecycle.Event.ON_PAUSE) {
        viewModel.send(Event.OnPause)
    }

    var anyComposable: AnyComposable? by remember {
        mutableStateOf(null)
    }

    val surfaceColor = colorScheme.surface
    val canvasColor = colorScheme.surface1
    val libraryColor = colorScheme.surface2
    var navigationBarColor by remember(surfaceColor) {
        mutableStateOf(surfaceColor)
    }
    var navigationBarHeightPx by remember {
        mutableStateOf(0F)
    }
    val navigationBarHeight = WindowInsets.navigationBars
        .asPaddingValues()
        .calculateBottomPadding()
    navigationBarHeightPx = navigationBarHeight.toPx()
    val activity = requireNotNull(LocalContext.current.activity) {
        "Unable to find the activity. This is an internal error. Please report this issue."
    }
    val oneDpInPx = 1.dp.toPx()
    LaunchedEffect(bottomSheetState) {
        val swipeableState = bottomSheetState.swipeableState
        launch {
            // Reset bottomSheetContent
            // The debounce is added to avoid the situation when another block is selected
            // This is needed because there is no other way currently to figure out when the bottom sheet has been dismissed by dragging
            snapshotFlow { swipeableState.offset }
                .debounce(16)
                .collect { offset ->
                    bottomSheetContent?.let {
                        if (swipeableState.offset == swipeableState.maxOffset &&
                            swipeableState.targetValue == SheetValue.Hidden
                        ) {
                            viewModel.send(EditorEvent.Sheet.OnClosed(it.type))
                        }
                        if (offset == 0F && swipeableState.currentValue == SheetValue.Expanded) {
                            viewModel.send(EditorEvent.Sheet.OnExpanded(it.type))
                        }
                        if (offset == swipeableState.maxOffset / 2 && swipeableState.currentValue == SheetValue.HalfExpanded) {
                            viewModel.send(EditorEvent.Sheet.OnHalfExpanded(it.type))
                        }
                    }
                }
        }
        launch {
            snapshotFlow { swipeableState.offset }.collectLatest { offset ->
                if (offset == null) return@collectLatest
                if (swipeableState.maxOffset == 0F) return@collectLatest
                val bottomSheetHeight =
                    ((swipeableState.maxOffset - offset).coerceAtMost(0.7f * swipeableState.maxOffset) - navigationBarHeightPx)
                        .coerceAtLeast(0F)
                val bottomSheetHeightInDp = bottomSheetHeight / oneDpInPx
                val bottomSheetMaxHeightInDp = (swipeableState.contentHeight - navigationBarHeightPx).coerceAtLeast(0F) / oneDpInPx
                viewModel.send(Event.OnBottomSheetHeightChange(bottomSheetHeightInDp, bottomSheetMaxHeightInDp))
            }
        }
    }

    val scrimBottomSheetState = rememberModalBottomSheetState(SheetValue.Hidden)

    val showScrimBottomSheet = remember {
        { composable: AnyComposable ->
            anyComposable = composable
            uiScope.launch {
                // FIXME: Without the delay, the bottom sheet state change does not animate. My hypothesis is that setting the
                //  anyComposable causes a re-composition in the middle of the animation which cancels the animation(?)
                delay(16)
                scrimBottomSheetState.show()
            }
        }
    }

    LaunchedEffect(Unit) {
        snapshotFlow { scrimBottomSheetState.isVisible }.collectLatest {
            if (!it) anyComposable = null
        }
    }

    var showCameraPermissionsView by remember { mutableStateOf(false) }
    LaunchedEffect(bottomSheetState) {
        viewModel.uiEvent.collect {
            when (it) {
                is SingleEvent.ChangeSheetState -> {
                    uiScope.launch {
                        if (it.animate) {
                            bottomSheetState.animateTo(it.state)
                        } else {
                            bottomSheetState.snapTo(it.state)
                        }
                    }
                }

                SingleEvent.HideScrimSheet -> {
                    uiScope.launch {
                        scrimBottomSheetState.hide()
                    }
                }

                is SingleEvent.Snackbar -> {
                    uiScope.launch {
                        snackBarHostState.showSnackbar(
                            // stringResource() doesn't work inside of a LaunchedEffect as it is not a composable
                            message = activity.resources.getString(it.text),
                            duration = it.duration,
                        )
                    }
                }
                else -> {}
            }
        }
    }
    var isOverlayVisible by remember { mutableStateOf(false) }
    var contentPadding by remember {
        mutableStateOf<PaddingValues?>(null)
    }
    val openContract = rememberSaveable(
        inputs = arrayOf(uiState.openContract),
        saver = Saver(
            save = {
                EditorEvent.LaunchContract.current = it
                true
            },
            restore = {
                EditorEvent.LaunchContract.current.also {
                    EditorEvent.LaunchContract.current = null
                }
            },
        ),
    ) { uiState.openContract }
    val contract = openContract.contract

    @Suppress("UNCHECKED_CAST")
    val launcher = rememberLauncherForActivityResult(openContract.contract) { result ->
        (openContract.onOutput as? EditorScope.(Any?) -> Unit)?.let {
            viewModel.send(
                Event.OnLaunchContractResult(
                    onResult = it,
                    editorScope = this,
                    result = result,
                ),
            )
        }
    } as ManagedActivityResultLauncher<Any?, Any?>

    fun launchContract() {
        if (contract is DummyContract) return
        if (openContract.launched) return
        openContract.launched = true
        try {
            launcher.launch(openContract.input)
        } catch (e: ActivityNotFoundException) {
            viewModel.send(Event.OnToast(R.string.ly_img_editor_error_activity_not_found))
        }
    }
    LaunchedEffect(contract) {
        val isSystemCameraContract = contract is ActivityResultContracts.CaptureVideo ||
            contract is ActivityResultContracts.TakePicture
        if (isSystemCameraContract && activity.hasCameraPermissionInManifest() && !activity.hasCameraPermission()) {
            showCameraPermissionsView = true
        } else {
            launchContract()
        }
    }

    Box(
        modifier = Modifier
            .background(colorScheme.surface)
            .onSizeChanged {
                viewModel.send(Event.OnEditorSizeChange(it.width / oneDpInPx, it.height / oneDpInPx))
            },
    ) {
        ModalBottomSheetLayout(
            sheetState = scrimBottomSheetState,
            sheetContent = {
                anyComposable?.Content()
            },
            scrimEnabled = true,
            sheetShape = RoundedCornerShape(
                topStart = 28.0.dp,
                topEnd = 28.0.dp,
                bottomEnd = 0.0.dp,
                bottomStart = 0.0.dp,
            ),
        ) {
            val cornerRadius = if (bottomSheetState.swipeableState.offset != 0f) 28.dp else 0.dp
            val sheetElevation = if (bottomSheetState.swipeableState.offset != 0f) ModalBottomSheetDefaults.Elevation else 0.dp
            ModalBottomSheetLayout(
                sheetState = bottomSheetState,
                modifier = Modifier.statusBarsPadding(),
                sheetElevation = sheetElevation,
                sheetContent = {
                    val content = bottomSheetContent
                    if (content != null) {
                        Sheet(style = requireNotNull(sheetStyle)) {
                            Column {
                                val onColorPickerActiveChanged = remember {
                                    { active: Boolean ->
                                        sheetStyle = if (active) {
                                            SheetStyle(
                                                maxHeight = null,
                                                isHalfExpandingEnabled = true,
                                                isHalfExpandedInitially = true,
                                                animateInitialValue = false,
                                            )
                                        } else {
                                            val isHalfExpandingEnabled = bottomSheetState.currentValue == SheetValue.HalfExpanded
                                            content.type.style.copy(
                                                isHalfExpandingEnabled = isHalfExpandingEnabled,
                                            )
                                        }
                                    }
                                }
                                if (content !is CustomBottomSheetContent && content.type !is SheetType.Voiceover) {
                                    Spacer(Modifier.height(8.dp))
                                }
                                when (content) {
                                    is LibraryTabsBottomSheetContent ->
                                        AddLibraryTabsSheet(
                                            swipeableState = bottomSheetState.swipeableState,
                                            onClose = {
                                                viewModel.send(EditorEvent.Sheet.Close(animate = true))
                                            },
                                            onCloseAssetDetails = {
                                                viewModel.send(Event.OnHideScrimSheet)
                                            },
                                            onSearchFocus = {
                                                viewModel.send(EditorEvent.Sheet.Expand(animate = true))
                                            },
                                            showAnyComposable = {
                                                showScrimBottomSheet(it)
                                            },
                                            launchGetContent = { mimeType, uploadAssetSourceType, designBlock ->
                                                viewModel.send(
                                                    Event.OnLaunchGetContent(mimeType, uploadAssetSourceType, designBlock),
                                                )
                                            },
                                            launchCamera = { captureVideo, designBlock ->
                                                viewModel.send(Event.OnSystemCameraClick(captureVideo, designBlock))
                                            },
                                        )
                                    is LibraryAddBottomSheetContent ->
                                        AddLibrarySheet(
                                            libraryCategory = content.libraryCategory,
                                            addToBackgroundTrack = content.addToBackgroundTrack,
                                            onClose = {
                                                viewModel.send(EditorEvent.Sheet.Close(animate = true))
                                            },
                                            onCloseAssetDetails = {
                                                viewModel.send(Event.OnHideScrimSheet)
                                            },
                                            onSearchFocus = {
                                                viewModel.send(EditorEvent.Sheet.Expand(animate = true))
                                            },
                                            showAnyComposable = {
                                                showScrimBottomSheet(it)
                                            },
                                            launchGetContent = { mimeType, uploadAssetSourceType, designBlock ->
                                                Event.OnLaunchGetContent(
                                                    mimeType = mimeType,
                                                    uploadAssetSourceType = uploadAssetSourceType,
                                                    designBlock = designBlock,
                                                    addToBackgroundTrack = content.addToBackgroundTrack,
                                                ).let(onEvent)
                                            },
                                            launchCamera = { captureVideo, designBlock ->
                                                viewModel.send(
                                                    Event.OnSystemCameraClick(
                                                        captureVideo = captureVideo,
                                                        designBlock = designBlock,
                                                        addToBackgroundTrack = content.addToBackgroundTrack,
                                                    ),
                                                )
                                            },
                                        )

                                    is LibraryReplaceBottomSheetContent ->
                                        ReplaceLibrarySheet(
                                            libraryCategory = content.libraryCategory,
                                            designBlock = content.designBlock,
                                            onClose = {
                                                viewModel.send(EditorEvent.Sheet.Close(animate = true))
                                            },
                                            onCloseAssetDetails = {
                                                viewModel.send(Event.OnHideScrimSheet)
                                            },
                                            onSearchFocus = {
                                                viewModel.send(EditorEvent.Sheet.Expand(animate = true))
                                            },
                                            showAnyComposable = {
                                                showScrimBottomSheet(it)
                                            },
                                            launchGetContent = { mimeType, uploadAssetSourceType, designBlock ->
                                                Event.OnLaunchGetContent(
                                                    mimeType = mimeType,
                                                    uploadAssetSourceType = uploadAssetSourceType,
                                                    designBlock = designBlock,
                                                ).let(onEvent)
                                            },
                                            launchCamera = { captureVideo, designBlock ->
                                                viewModel.send(
                                                    Event.OnSystemCameraClick(
                                                        captureVideo = captureVideo,
                                                        designBlock = designBlock,
                                                    ),
                                                )
                                            },
                                        )

                                    is LayerBottomSheetContent -> LayerOptionsSheet(content.uiState, onEvent)
                                    is FillStrokeBottomSheetContent ->
                                        FillStrokeOptionsSheet(
                                            uiState = content.uiState,
                                            onColorPickerActiveChanged = onColorPickerActiveChanged,
                                            onEvent = onEvent,
                                        )
                                    is OptionsBottomSheetContent -> ShapeOptionsSheet(content.uiState, onEvent)
                                    is FormatBottomSheetContent -> FormatOptionsSheet(content.uiState, onEvent)
                                    is CropBottomSheetContent -> CropSheet(content.uiState, onEvent)
                                    is AdjustmentSheetContent -> AdjustmentOptionsSheet(content.uiState, onEvent)
                                    is EffectSheetContent ->
                                        EffectSelectionSheet(
                                            uiState = content.uiState,
                                            onColorPickerActiveChanged = onColorPickerActiveChanged,
                                            onEvent = onEvent,
                                        )
                                    is SpeedBottomSheetContent -> SpeedSheet(content.uiState, onEvent)
                                    is VolumeBottomSheetContent -> VolumeSheet(content.uiState, onEvent)
                                    is ReorderBottomSheetContent -> ReorderSheet(content.timelineState, onEvent)
                                    is AnimationBottomSheetContent -> AnimationSheet(content.uiState, onEvent)
                                    is TextBackgroundBottomSheetContent -> TextBackgroundBottomSheet(
                                        uiState = content.uiState,
                                        onColorPickerActiveChanged = onColorPickerActiveChanged,
                                        onEvent = onEvent,
                                    )
                                    is FontBottomSheetContent ->
                                        FontSheet(
                                            uiState = content.uiState,
                                            onEvent = onEvent,
                                        )
                                    is FontSizeBottomSheetContent ->
                                        FontSizeSheet(
                                            uiState = content.uiState,
                                            onEvent = onEvent,
                                        )
                                    is ColorsBottomSheetContent ->
                                        ColorsSheet(
                                            uiState = content.uiState,
                                            onColorPickerActiveChanged = onColorPickerActiveChanged,
                                            onEvent = onEvent,
                                        )
                                    is CustomBottomSheetContent -> content.content(this@EditorUi)
                                }
                                if (content !is CustomBottomSheetContent && content.type !is SheetType.Voiceover) {
                                    Spacer(Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                },
                sheetShape = RoundedCornerShape(topStart = cornerRadius, topEnd = cornerRadius),
            ) {
                Scaffold(
                    modifier = Modifier.navigationBarsPadding(),
                    topBar = {
                        if (uiState.isSceneLoaded) {
                            configuration.navigationBar?.let {
                                EditorComponent(
                                    modifier = Modifier.onSizeChanged {
                                        viewModel.send(Event.OnNavigationBarSizeChange(it.width / oneDpInPx, it.height / oneDpInPx))
                                    },
                                    component = it,
                                    onHide = {
                                        viewModel.send(Event.OnNavigationBarSizeChange(0F, 0F))
                                    },
                                )
                            }
                        }
                    },
                ) { paddingValues ->
                    contentPadding = paddingValues
                    Box {
                        val orientation = LocalConfiguration.current.orientation
                        val blocksCanvasAndTimelineInteraction =
                            bottomSheetContent?.type is SheetType.Voiceover &&
                                viewModel.isVoiceOverRecordingInProgress
                        EngineCanvasView(
                            renderTarget = renderTarget,
                            engine = viewModel.engine,
                            isCanvasVisible = uiState.isSceneLoaded,
                            passTouches = uiState.allowEditorInteraction && !blocksCanvasAndTimelineInteraction,
                            onMoveStart = { viewModel.send(Event.OnCanvasMove(true)) },
                            onMoveEnd = { viewModel.send(Event.OnCanvasMove(false)) },
                            onTouch = { viewModel.send(Event.OnCanvasTouch) },
                            onSizeChanged = { viewModel.send(Event.OnResetZoom) },
                            loadScene = {
                                isOverlayVisible = true
                                viewModel.send(
                                    Event.OnLoadScene(
                                        inPortraitMode = orientation != Configuration.ORIENTATION_LANDSCAPE,
                                    ),
                                )
                            },
                        )
                        if (uiState.isSceneLoaded) {
                            Column(
                                Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = uiState.bottomPanelMaxHeightInDp.dp)
                                    .align(Alignment.BottomStart),
                            ) {
                                configuration.bottomPanel?.let {
                                    EditorComponent(
                                        modifier = Modifier.onSizeChanged {
                                            viewModel.send(Event.OnBottomPanelSizeChange(it.width / oneDpInPx, it.height / oneDpInPx))
                                        },
                                        component = it,
                                        onHide = {
                                            viewModel.send(Event.OnBottomPanelSizeChange(0F, 0F))
                                        },
                                    )
                                }
                                configuration.dock?.let {
                                    EditorComponent(
                                        modifier = Modifier.onSizeChanged {
                                            viewModel.send(Event.OnDockSizeChange(it.width / oneDpInPx, it.height / oneDpInPx))
                                        },
                                        component = it,
                                        onHide = {
                                            viewModel.send(Event.OnDockSizeChange(0F, 0F))
                                        },
                                    )
                                }
                            }

                            if (uiState.isEditingText) {
                                EditingTextCard(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .onGloballyPositioned {
                                            viewModel.send(Event.OnKeyboardHeightChange(it.size.height / oneDpInPx))
                                        },
                                    onClose = { viewModel.send(Event.OnKeyboardClose) },
                                )
                            }

                            configuration.canvasMenu?.let {
                                EditorComponent(component = it)
                            }

                            val inspectorBar = configuration.inspectorBar
                            inspectorBar?.let {
                                EditorComponent(
                                    modifier = Modifier
                                        .align(Alignment.BottomStart)
                                        .onSizeChanged {
                                            viewModel.send(Event.OnInspectorBarSizeChange(it.width / oneDpInPx, it.height / oneDpInPx))
                                        },
                                    component = it,
                                    onHide = {
                                        viewModel.send(Event.OnInspectorBarSizeChange(0F, 0F))
                                    },
                                )
                            }
                            LaunchedEffect(bottomSheetContent, inspectorBar?.visible, uiState.pagesState, anyComposable) {
                                navigationBarColor = when {
                                    anyComposable != null -> surfaceColor
                                    bottomSheetContent == null -> {
                                        when {
                                            uiState.pagesState != null -> libraryColor
                                            inspectorBar?.visible == true -> surfaceColor
                                            else -> canvasColor
                                        }
                                    }
                                    bottomSheetContent is LibraryTabsBottomSheetContent -> {
                                        libraryColor
                                    }
                                    else -> {
                                        surfaceColor
                                    }
                                }
                            }

                            if (blocksCanvasAndTimelineInteraction) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .pointerInput(Unit) {
                                            awaitPointerEventScope {
                                                while (true) {
                                                    awaitPointerEvent().changes.forEach { change ->
                                                        change.consume()
                                                    }
                                                }
                                            }
                                        },
                                )
                            }
                        }
                    }
                }
                contentPadding?.let {
                    Box {
                        EditorPagesUi(
                            modifier = Modifier
                                .statusBarsPadding()
                                .padding(top = it.calculateTopPadding(), bottom = it.calculateBottomPadding() + 84.dp)
                                .fillMaxSize(),
                            state = uiState.pagesState,
                            onEvent = onEvent,
                        )
                        EditorPagesDock(
                            modifier = Modifier
                                .align(Alignment.BottomStart)
                                .systemBarsPadding()
                                .height(84.dp),
                            state = uiState.pagesState,
                            onEvent = onEvent,
                        )
                    }
                }
            }
        }
        SystemNavBar(navigationBarColor)
        Surface(
            color = navigationBarColor,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .height(navigationBarHeight),
        ) {}
        SnackbarHost(
            hostState = snackBarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(12.dp),
            snackbar = { snackbarData ->
                Snackbar(
                    action = {
                        IconButton(onClick = { snackbarData.dismiss() }) {
                            Icon(IconPack.Close, contentDescription = null)
                        }
                    },
                    actionOnNewLine = false,
                    content = { Text(snackbarData.visuals.message) },
                )
            },
        )

        if (isOverlayVisible) {
            configuration.overlay?.let {
                EditorComponent(component = it)
            }
        }

        if (showCameraPermissionsView) {
            Column(
                modifier = Modifier
                    .systemBarsPadding()
                    .background(canvasColor)
                    .fillMaxSize()
                    .align(Alignment.TopCenter)
                    .requiredWidth(264.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PermissionsView(
                    requestOnlyCameraPermission = true,
                    onAllPermissionsGranted = {
                        showCameraPermissionsView = false
                        launchContract()
                    },
                    onClose = {
                        showCameraPermissionsView = false
                    },
                )
            }
            BackHandler(showCameraPermissionsView) {
                showCameraPermissionsView = false
            }
        }
    }
}
