package ly.img.editor.base.ui

import android.content.ContentValues
import android.graphics.Bitmap
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import ly.img.camera.core.CaptureMedia
import ly.img.editor.base.applyForceCrop
import ly.img.editor.base.components.createEditingTextCardUiState
import ly.img.editor.base.dock.AdjustmentSheetContent
import ly.img.editor.base.dock.BottomSheetContent
import ly.img.editor.base.dock.CustomBottomSheetContent
import ly.img.editor.base.dock.EffectSheetContent
import ly.img.editor.base.dock.FillStrokeBottomSheetContent
import ly.img.editor.base.dock.FormatBottomSheetContent
import ly.img.editor.base.dock.LayerBottomSheetContent
import ly.img.editor.base.dock.LibraryAddBottomSheetContent
import ly.img.editor.base.dock.LibraryReplaceBottomSheetContent
import ly.img.editor.base.dock.LibraryTabsBottomSheetContent
import ly.img.editor.base.dock.OptionsBottomSheetContent
import ly.img.editor.base.dock.options.adjustment.AdjustmentUiState
import ly.img.editor.base.dock.options.animation.AnimationBottomSheetContent
import ly.img.editor.base.dock.options.animation.AnimationUiState
import ly.img.editor.base.dock.options.colors.ColorsBottomSheetContent
import ly.img.editor.base.dock.options.colors.ColorsUiState
import ly.img.editor.base.dock.options.crop.CropBottomSheetContent
import ly.img.editor.base.dock.options.crop.createAllPageResizeUiState
import ly.img.editor.base.dock.options.crop.createCropUiState
import ly.img.editor.base.dock.options.effect.EffectUiState
import ly.img.editor.base.dock.options.fillstroke.FillStrokeUiState
import ly.img.editor.base.dock.options.font.FontBottomSheetContent
import ly.img.editor.base.dock.options.font.FontUiState
import ly.img.editor.base.dock.options.fontSize.FontSizeBottomSheetContent
import ly.img.editor.base.dock.options.fontSize.FontSizeUiState
import ly.img.editor.base.dock.options.format.createFormatUiState
import ly.img.editor.base.dock.options.layer.createLayerUiState
import ly.img.editor.base.dock.options.reorder.ReorderBottomSheetContent
import ly.img.editor.base.dock.options.shapeoptions.createShapeOptionsUiState
import ly.img.editor.base.dock.options.speed.SpeedBottomSheetContent
import ly.img.editor.base.dock.options.speed.SpeedUiState
import ly.img.editor.base.dock.options.textBackground.TextBackgroundBottomSheetContent
import ly.img.editor.base.dock.options.textBackground.TextBackgroundUiState
import ly.img.editor.base.dock.options.textonpath.TextOnPathBottomSheetContent
import ly.img.editor.base.dock.options.textonpath.TextOnPathUiState
import ly.img.editor.base.dock.options.voiceover.VoiceoverOptionsSheet
import ly.img.editor.base.dock.options.voiceover.VoiceoverUiStateFactory
import ly.img.editor.base.dock.options.volume.VolumeBottomSheetContent
import ly.img.editor.base.dock.options.volume.VolumeUiState
import ly.img.editor.base.engine.CROP_EDIT_MODE
import ly.img.editor.base.engine.FEATURE_PAGE_CAROUSEL_ENABLED
import ly.img.editor.base.engine.SOFTWARE_KEYBOARD_SUSPENDED_SETTING
import ly.img.editor.base.engine.TEXT_EDIT_MODE
import ly.img.editor.base.engine.TOUCH_ACTION_SCALE
import ly.img.editor.base.engine.TRANSFORM_EDIT_MODE
import ly.img.editor.base.engine.duplicate
import ly.img.editor.base.engine.effectiveTextRange
import ly.img.editor.base.engine.isPlaceholder
import ly.img.editor.base.engine.resetHistory
import ly.img.editor.base.engine.setFillType
import ly.img.editor.base.engine.showPage
import ly.img.editor.base.engine.toEngineColor
import ly.img.editor.base.engine.zoomToPage
import ly.img.editor.base.engine.zoomToSelectedText
import ly.img.editor.base.migration.EditorMigrationHelper
import ly.img.editor.base.sheet.LibraryAddToBackgroundTrackSheetType
import ly.img.editor.base.timeline.state.TimelineState
import ly.img.editor.base.ui.handler.animationEvents
import ly.img.editor.base.ui.handler.appearanceEvents
import ly.img.editor.base.ui.handler.blockEvents
import ly.img.editor.base.ui.handler.blockFillEvents
import ly.img.editor.base.ui.handler.cropEvents
import ly.img.editor.base.ui.handler.shapeOptionEvents
import ly.img.editor.base.ui.handler.speedEvents
import ly.img.editor.base.ui.handler.strokeEvents
import ly.img.editor.base.ui.handler.textBlockEvents
import ly.img.editor.base.ui.handler.timelineEvents
import ly.img.editor.base.ui.handler.volumeEvents
import ly.img.editor.core.EditorContext
import ly.img.editor.core.EditorScope
import ly.img.editor.core.UnstableEditorApi
import ly.img.editor.core.component.Dock
import ly.img.editor.core.component.TimelineOwner
import ly.img.editor.core.component.data.Size
import ly.img.editor.core.component.rememberImglyCamera
import ly.img.editor.core.component.rememberSystemCamera
import ly.img.editor.core.configuration.EditorConfiguration
import ly.img.editor.core.currentLanguageCode
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.event.EditorEventHandler
import ly.img.editor.core.library.data.AssetSourceType
import ly.img.editor.core.library.data.SystemGalleryPermission
import ly.img.editor.core.library.data.UploadAssetSourceType
import ly.img.editor.core.sheet.SheetState
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.sheet.SheetValue
import ly.img.editor.core.state.Dimensions
import ly.img.editor.core.state.EditorState
import ly.img.editor.core.state.EditorViewMode
import ly.img.editor.core.ui.EventsHandler
import ly.img.editor.core.ui.engine.BlockType
import ly.img.editor.core.ui.engine.Scope
import ly.img.editor.core.ui.engine.awaitEngineAndSceneLoad
import ly.img.editor.core.ui.engine.deselectAllBlocks
import ly.img.editor.core.ui.engine.dpToCanvasUnit
import ly.img.editor.core.ui.engine.getCamera
import ly.img.editor.core.ui.engine.getFillType
import ly.img.editor.core.ui.engine.getScene
import ly.img.editor.core.ui.engine.overrideAndRestore
import ly.img.editor.core.ui.library.AppearanceLibraryCategory
import ly.img.editor.core.ui.library.CropAssetSourceType
import ly.img.editor.core.ui.library.LibraryViewModel
import ly.img.editor.core.ui.library.util.LibraryEvent
import ly.img.editor.core.ui.register
import ly.img.editor.featureFlag.flags.IMGLYCameraFeature
import ly.img.engine.AssetTransformPreset
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import ly.img.engine.FillType
import ly.img.engine.GlobalScope
import ly.img.engine.Typeface
import ly.img.engine.UnstableEngineApi
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.max
import kotlin.time.Duration.Companion.ZERO

@OptIn(UnstableEditorApi::class)
@Stable
class EditorUiViewModel(
    private val editorScope: EditorScope,
    private val publicState: MutableStateFlow<EditorState>,
    private val libraryViewModel: LibraryViewModel,
) : ViewModel(),
    EditorEventHandler {
    val editor: EditorContext
        get() = editorScope.run { editorContext }
    val engine: Engine
        get() = editor.engine
    private val configuration: EditorConfiguration
        get() = requireNotNull(editor.configuration.value)
    private val colorPalette: List<Color>
        get() = configuration.colorPalette ?: emptyList()

    private val migrationHelper = EditorMigrationHelper()
    private var timelineState: TimelineState? = null
    private var firstLoad = true
    private var isStraighteningOrRotating = false
    private var initCropTranslationX = 0f
    private var initCropTranslationY = 0f
    private var isCropReset = false
    private var forceCropState: ForceCropState? = null
    private val isExporting = MutableStateFlow(false)
    private val isKeyboardShowing = MutableStateFlow(false)
    private val pageIndex = MutableStateFlow(0)
    private val isZoomedIn = MutableStateFlow(false)
    private val isSceneLoaded = MutableStateFlow(false)
    private var inPortraitMode = true

    @Suppress("ktlint:standard:backing-property-naming")
    private val _uiState = MutableStateFlow(EditorUiViewState())
    val uiState: StateFlow<EditorUiViewState> = _uiState

    private var viewMode: EditorViewMode
        get() = publicState.value.viewMode
        set(value) = publicState.update { it.copy(viewMode = value, isBackHandlerEnabled = canHandleBackPress(value)) }

    private val _uiEvent = MutableSharedFlow<SingleEvent>()
    val uiEvent: SharedFlow<SingleEvent> = _uiEvent

    private val imgLyInsets = MutableStateFlow(Rect.Zero)

    private var bottomSheetHeight: Float = 0F
    private var bottomStaticContentHeight: Float = 0F
    private var bottomStaticContentFullHeight: Float = 0F
    private var closingSheetContent: BottomSheetContent? = null
    private val _bottomSheetContent = MutableStateFlow<BottomSheetContent?>(null)
    val bottomSheetContent = _bottomSheetContent.asStateFlow()

    @Suppress("ktlint:standard:backing-property-naming")
    private val _historyChangeTrigger = MutableSharedFlow<Unit>()

    private var pagesSessionId = 0

    // In-memory thumbnail cache: survives pages mode exit/re-entry within the session.
    // Keyed by page UUID (stable across state rebuilds). Bounded to MAX_THUMBNAIL_CACHE_SIZE
    // entries to prevent unbounded memory growth in scenes with many pages.
    private val thumbnailCache = object : LinkedHashMap<String, Bitmap>(16, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Bitmap>?): Boolean = size > MAX_THUMBNAIL_CACHE_SIZE
    }

    private val defaultCropSheetType = SheetType.Crop(mode = SheetType.Crop.Mode.Element)
    private var currentCropSheetType: SheetType.Crop? = null
    internal var isVoiceOverRecordingInProgress by mutableStateOf(false)
    internal var voiceOverSheetTargetBlock: DesignBlock? by mutableStateOf(null)

    private var canvasMoveJob: Job? = null

    init {
        collectVideoDurationConstraintsData()
        collectAssetApplyErrors()
    }

    private fun collectAssetApplyErrors() {
        viewModelScope.launch {
            libraryViewModel.assetApplyError.collect { throwable ->
                sendSingleEvent(SingleEvent.SnackbarError(throwable))
            }
        }
    }

    private val eventHandler = EventsHandler(coroutineScope = viewModelScope) {
        cropEvents(
            engine = ::engine,
            block = { checkNotNull(getBlockForEvents()?.designBlock ?: editor.engine.scene.getCurrentPage()) },
            bewarePageState = @OptIn(UnstableEngineApi::class) { wrap ->
                val currentPage = pageIndex.value
                // Temporarily disable camera clamping as otherwise the page carousel breaks
                // while resizing as we cannot batch update the sizes for all pages.
                // Do not temporarily disable the page carousel because this leads to
                // visual bugs.
                // It is enabled again by the zoom update.
                if (engine.scene.isCameraZoomClampingEnabled(engine.getScene())) {
                    engine.scene.disableCameraZoomClamping()
                }
                if (engine.scene.isCameraPositionClampingEnabled(engine.getScene())) {
                    engine.scene.disableCameraPositionClamping()
                }
                wrap()
                if (pageIndex.value != currentPage) {
                    pageIndex.update { currentPage }
                }
                updateInsets(
                    bottomInset = max(bottomSheetHeight, bottomStaticContentHeight),
                )
            },
        )
        blockEvents(
            engine = ::engine,
            block = ::requireDesignBlockForEvents,
        )
        textBlockEvents(
            engine = ::engine,
            block = ::requireDesignBlockForEvents,
        )
        strokeEvents(
            engine = ::engine,
            block = ::requireDesignBlockForEvents,
        )
        blockFillEvents(
            engine = ::engine,
            block = ::requireDesignBlockForEvents,
        )
        shapeOptionEvents(
            engine = ::engine,
            block = ::requireDesignBlockForEvents,
        )
        appearanceEvents(
            engine = ::engine,
            block = ::requireDesignBlockForEvents,
        )
        speedEvents(
            engine = ::engine,
            block = ::requireDesignBlockForEvents,
            showToast = {
                sendSingleEvent(SingleEvent.Snackbar(it))
            },
        )
        volumeEvents(
            engine = ::engine,
            block = ::requireDesignBlockForEvents,
        )
        timelineEvents(
            engine = ::engine,
            timelineState = {
                requireNotNull(timelineState)
            },
            showError = {
                sendSingleEvent(SingleEvent.Snackbar(it))
            },
        )
        editorEvents()
        animationEvents(
            engine = ::engine,
        )
    }

    private fun EventsHandler.editorEvents() {
        register<Event.OnError> { onError(it.throwable) }
        register<Event.OnToast> { sendSingleEvent(SingleEvent.Snackbar(it.text)) }
        register<Event.OnBackPress> {
            onBackPress(
                bottomSheetOffset = it.bottomSheetOffset,
                bottomSheetMaxOffset = it.bottomSheetMaxOffset,
            )
        }
        register<Event.OnCloseInspectorBar> { engine.deselectAllBlocks() }
        register<Event.OnHideScrimSheet> { sendSingleEvent(SingleEvent.HideScrimSheet) }
        register<Event.OnCanvasMove> { onCanvasMove(it.move) }
        register<Event.OnCanvasTouch> { updateZoomState() }
        register<Event.OnResetZoom> {
            if (isSceneLoaded.value && engine.editor.getEditMode() == TRANSFORM_EDIT_MODE) {
                zoom(forceZoomToPage = true)
            }
        }
        register<Event.OnKeyboardClose> { onKeyboardClose() }
        register<Event.OnLoadScene> { loadScene(it.inPortraitMode) }
        register<Event.EnableHistory> { event -> publicState.update { it.copy(isHistoryEnabled = event.enable) } }
        register<Event.OnBottomSheetStateChange> { onBottomSheetStateChange(it.state) }
        register<Event.OnBottomSheetHeightChange> { onBottomSheetHeightChange(it.sheetHeightDp, it.sheetMaxHeightDp) }
        register<Event.OnEditorSizeChange> { onEditorSizeChange(it.widthDp, it.heightDp) }
        register<Event.OnNavigationBarSizeChange> { onNavigationBarSizeChange(it.widthDp, it.heightDp) }
        register<Event.OnBottomPanelSizeChange> { onBottomPanelSizeChange(it.widthDp, it.heightDp) }
        register<Event.OnDockSizeChange> { onDockSizeChange(it.widthDp, it.heightDp) }
        register<Event.OnInspectorBarSizeChange> { onInspectorBarSizeChange(it.widthDp, it.heightDp) }
        register<Event.OnKeyboardHeightChange> { onKeyboardHeightChange(it.heightInDp) }
        register<Event.OnPause> { timelineState?.playerState?.pause() }
        register<Event.OnPage> { setPage(it.page) }
        register<Event.OnAddPage> { onAddPage(it.index) }
        register<Event.OnPagesModePageSelectionChange> { onPagesSelectionChange(it.page) }
        register<Event.OnPagesModePageBind> { onPagesModePageBind(it.page, it.pageHeight) }
        register<Event.OnSystemCameraClick> {
            onSystemCameraClick(
                captureVideo = it.captureVideo,
                designBlock = it.designBlock,
                addToBackgroundTrack = it.addToBackgroundTrack,
            )
        }
        register<Event.OnLaunchGetContent> {
            onLaunchGetContent(it.mimeType, it.uploadAssetSourceType, it.designBlock, it.addToBackgroundTrack)
        }
        register<Event.OnVideoCameraClick> { onVideoCameraClick(it.callback) }
        register<Event.OnLaunchContractResult> { onLaunchContractResult(it.onResult, it.editorScope, it.result) }
        register<Event.OnTypefaceChange> {
            onTypefaceChange(it.designBlock, it.typeface)
        }
        register<Event.OnFontSizeChange> {
            onFontSizeChange(it.designBlock, it.size)
        }
        register<Event.OnColorChange> {
            onColorChange(it.name, it.color)
        }

        /** Customer exposed internal events **/
        register<EditorEvent.OnClose> { onClose() }
        register<EditorEvent.CloseEditor> { sendSingleEvent(SingleEvent.Exit(it.throwable)) }
        register<EditorEvent.Navigation.ToNextPage> { setPage(pageIndex.value + 1) }
        register<EditorEvent.Navigation.ToPreviousPage> { setPage(pageIndex.value - 1) }
        register<EditorEvent.Export.Start> { exportScene() }
        register<EditorEvent.Export.Cancel> { exportJob?.cancel() }
        @Suppress("DEPRECATION")
        register<EditorEvent.CancelExport> { exportJob?.cancel() }
        register<EditorEvent.SetViewMode> { setEditorViewMode(it.viewMode) }
        register<EditorEvent.Sheet.Open> { openSheet(it.type) }
        register<EditorEvent.Sheet.Expand> {
            sendSingleEvent(SingleEvent.ChangeSheetState(SheetValue.Expanded, it.animate))
        }
        register<EditorEvent.Sheet.HalfExpand> {
            sendSingleEvent(SingleEvent.ChangeSheetState(SheetValue.HalfExpanded, it.animate))
        }
        register<EditorEvent.Sheet.Close> { closeSheet(animate = it.animate) }
        register<EditorEvent.Sheet.OnExpanded> { /* do nothing */ }
        register<EditorEvent.Sheet.OnHalfExpanded> { /* do nothing */ }
        register<EditorEvent.Sheet.OnClosed> { onSheetClosed() }
        register<EditorEvent.ApplyForceCrop> { event ->
            val result = runCatching {
                editorScope.applyForceCrop(event.designBlock, event.configuration)
            }
            result.onFailure { throwable ->
                if (forceCropState?.block == event.designBlock) {
                    setForceCropState(null)
                }
                onError(throwable)
            }
            result.getOrNull()?.let { forceResult ->
                if (forceResult.applied) {
                    setForceCropState(
                        ForceCropState(
                            block = event.designBlock,
                            sourceId = forceResult.candidate.sourceId,
                            presetId = forceResult.candidate.presetId,
                            transformPreset = forceResult.transformPreset,
                        ),
                    )
                }
            }
        }
        register<EditorEvent.ApplyVideoDurationConstraints> {
            publicState.update { state -> state.copy(minVideoDuration = it.minDuration, maxVideoDuration = it.maxDuration) }
        }
        register<EditorEvent.LaunchContract<*, *>> { _uiState.update { state -> state.copy(openContract = it) } }
        register<EditorEvent.AddUriToScene> {
            // This event is triggered only from dock for now.
            // addToBackgroundTrack is always true for now as we do not want to expose it to customers due to unknown future.
            libraryViewModel.onEvent(
                LibraryEvent.OnAddUri(it.uploadAssetSourceType, it.uri, addToBackgroundTrack = it.addToBackgroundTrack),
            )
        }
        register<EditorEvent.ReplaceUriAtScene> {
            libraryViewModel.onEvent(
                LibraryEvent.OnReplaceUri(it.uploadAssetSourceType, it.uri, it.designBlock),
            )
        }
        @Suppress("DEPRECATION")
        register<EditorEvent.AddCameraRecordingsToScene> {
            libraryViewModel.onEvent(LibraryEvent.OnAddCameraRecordings(it.uploadAssetSourceType, it.recordings))
        }
        register<EditorEvent.AddCameraCapturesToScene> {
            libraryViewModel.onEvent(
                LibraryEvent.OnAddCameraCaptures(
                    photoAssetSource = it.photoUploadAssetSourceType,
                    videoAssetSource = it.videoUploadAssetSourceType,
                    captures = it.captures,
                    appendToBackgroundTrack = it.appendToBackgroundTrack,
                ),
            )
        }
        register<EditorEvent.Selection.EnterTextEditMode> {
            timelineState?.playerState?.pause()
            timelineState?.clampPlayheadPositionToSelectedClip()
            engine.editor.setEditMode(TEXT_EDIT_MODE)
        }
        register<EditorEvent.Selection.Duplicate> {
            timelineState?.playerState?.pause()
            timelineState?.clampPlayheadPositionToSelectedClip()
            getBlockForEvents()?.designBlock?.let(engine::duplicate)
        }
        register<EditorEvent.Selection.Split> {
            timelineState?.playerState?.pause()
            send(BlockEvent.OnSplit)
        }
        register<EditorEvent.Selection.MoveAsClip> {
            timelineState?.playerState?.pause()
            send(BlockEvent.OnToggleBackgroundTrackAttach)
        }
        register<EditorEvent.Selection.MoveAsOverlay> {
            timelineState?.playerState?.pause()
            send(BlockEvent.OnToggleBackgroundTrackAttach)
        }
        register<EditorEvent.Selection.EnterGroup> {
            timelineState?.playerState?.pause()
            getBlockForEvents()?.designBlock?.let(engine.block::enterGroup)
        }
        register<EditorEvent.Selection.SelectGroup> {
            timelineState?.playerState?.pause()
            timelineState?.clampPlayheadPositionToSelectedClip()
            getBlockForEvents()?.designBlock?.let(engine.block::exitGroup)
        }
        register<EditorEvent.Selection.Delete> {
            timelineState?.playerState?.pause()
            getBlockForEvents()?.designBlock?.let { designBlock ->
                send(BlockEvent.OnDeleteNonSelected(designBlock))
            }
        }
        register<EditorEvent.Selection.BringForward> {
            timelineState?.playerState?.pause()
            getBlockForEvents()?.designBlock?.let {
                engine.block.bringForward(it)
                engine.editor.addUndoStep()
            }
        }
        register<EditorEvent.Selection.SendBackward> {
            timelineState?.playerState?.pause()
            getBlockForEvents()?.designBlock?.let {
                engine.block.sendBackward(it)
                engine.editor.addUndoStep()
            }
        }
        register<EditorEvent.Insets.SetExtra> {
            publicState.update { state -> state.copy(extraInsets = it.insets) }
            updateInsets(zoomToPage = true)
        }
    }

    private fun closeSheet(animate: Boolean) {
        if (animate) {
            sendSingleEvent(SingleEvent.ChangeSheetState(SheetValue.Hidden, animate = true))
        } else {
            bottomSheetContent.value?.type?.let { send(EditorEvent.Sheet.OnClosed(it)) }
        }
    }

    private fun setEditorViewMode(viewMode: EditorViewMode) {
        when (viewMode) {
            is EditorViewMode.Preview -> setPreviewMode(viewMode)
            is EditorViewMode.Pages -> setPagesMode(viewMode)
            is EditorViewMode.Edit -> setEditMode(viewMode)
        }
    }

    @OptIn(UnstableEngineApi::class)
    private fun openSheet(type: SheetType) {
        val hasEventBlock = getBlockForEvents() != null
        val block by lazy { requireNotNull(getBlockForEvents()) }
        val designBlock by lazy {
            block.designBlock
        }
        // Opening the Fill & Stroke sheet from the EditingTextCard hands the IME slot to the
        // sheet while text-edit mode stays alive.
        if (type is SheetType.FillStroke && engine.editor.getEditMode() == TEXT_EDIT_MODE) {
            engine.editor.setSettingBoolean(SOFTWARE_KEYBOARD_SUSPENDED_SETTING, true)
            updateKeyboardShowing()
        }
        timelineState?.playerState?.pause()
        setBottomSheetContent {
            when (type) {
                // Cannot be invoked by customers
                is LibraryAddToBackgroundTrackSheetType -> {
                    LibraryAddBottomSheetContent(
                        type = type,
                        libraryCategory = type.libraryCategory,
                        addToBackgroundTrack = true,
                    )
                }
                // This sheet is triggered from the dock and certain timeline entry points.
                is SheetType.LibraryAdd -> type.libraryCategory?.let {
                    LibraryAddBottomSheetContent(
                        type = type,
                        libraryCategory = it,
                        addToBackgroundTrack = type.addToBackgroundTrack,
                    )
                } ?: run {
                    LibraryTabsBottomSheetContent(type = type)
                }
                is SheetType.LibraryReplace -> {
                    timelineState?.clampPlayheadPositionToSelectedClip()
                    LibraryReplaceBottomSheetContent(
                        type = type,
                        libraryCategory = type.libraryCategory,
                        designBlock = designBlock,
                    )
                }
                is SheetType.Custom -> {
                    CustomBottomSheetContent(
                        type = type,
                        content = type.content,
                    )
                }
                is SheetType.Voiceover -> {
                    VoiceoverUiStateFactory.create(editorScope)?.let { uiState ->
                        CustomBottomSheetContent(
                            type = type,
                            content = {
                                VoiceoverOptionsSheet(
                                    uiState = uiState,
                                    onEvent = editorContext.eventHandler::send,
                                )
                            },
                        )
                    }
                }
                is SheetType.Layer -> {
                    timelineState?.clampPlayheadPositionToSelectedClip()
                    LayerBottomSheetContent(
                        type = type,
                        uiState = createLayerUiState(designBlock, engine),
                    )
                }
                is SheetType.FormatText -> {
                    timelineState?.clampPlayheadPositionToSelectedClip()
                    FormatBottomSheetContent(
                        type = type,
                        uiState = createFormatUiState(designBlock, engine),
                    )
                }
                is SheetType.Shape -> {
                    timelineState?.clampPlayheadPositionToSelectedClip()
                    OptionsBottomSheetContent(
                        type = type,
                        uiState = createShapeOptionsUiState(designBlock, engine),
                    )
                }
                is SheetType.FillStroke -> {
                    timelineState?.clampPlayheadPositionToSelectedClip()
                    FillStrokeBottomSheetContent(
                        type = type,
                        uiState = FillStrokeUiState.create(block, engine, colorPalette, fillOnly = type.fillOnly),
                    )
                }

                is SheetType.ResizeAll -> {
                    currentCropSheetType = type
                    val currentPage = editor.engine.scene.getCurrentPage()
                    val forcedSources = currentPage?.let(::forcedCropSourcesFor)
                    val forceCropActive = currentPage?.let { isForceCropActiveFor(it) } ?: false
                    val allowContentFillMode = !forceCropActive
                    val allowResizeOption = !forceCropActive
                    CropBottomSheetContent(
                        type = type,
                        uiState = createAllPageResizeUiState(
                            engine = engine,
                            initCropTranslationX = initCropTranslationX,
                            initCropTranslationY = initCropTranslationY,
                            pageAssetSourceIds = forcedSources?.pageAssetSourceId ?: CropAssetSourceType.Page.sourceId,
                            selectedAssetKey = forcedSources?.selectedAssetKey,
                            allowContentFillMode = allowContentFillMode,
                            allowResizeOption = allowResizeOption,
                        ),
                    )
                }

                is SheetType.Crop -> {
                    timelineState?.clampPlayheadPositionToSelectedClip()
                    currentCropSheetType = type
                    if (hasEventBlock) {
                        engine.block.setScopeEnabled(designBlock, Scope.EditorSelect, enabled = true)
                        engine.block.setSelected(designBlock, selected = true)
                        engine.editor.setEditMode(CROP_EDIT_MODE)
                        null
                    } else {
                        val page = editor.engine.scene.getCurrentPage()
                        if (page != null) {
                            val forcedSources = forcedCropSourcesFor(page)
                            val forceCropActive = isForceCropActiveFor(page)
                            val allowContentFillMode = !forceCropActive
                            val allowResizeOption = !forceCropActive
                            CropBottomSheetContent(
                                type = type,
                                uiState = createCropUiState(
                                    page,
                                    engine,
                                    cropMode = type.mode,
                                    pageAssetSourceId = forcedSources?.pageAssetSourceId ?: CropAssetSourceType.Page.sourceId,
                                    cropAssetSourceId = forcedSources?.cropAssetSourceId ?: CropAssetSourceType.Crop.sourceId,
                                    initCropTranslationX = initCropTranslationX,
                                    initCropTranslationY = initCropTranslationY,
                                    selectedAssetKey = forcedSources?.selectedAssetKey,
                                    allowContentFillMode = allowContentFillMode,
                                    allowResizeOption = allowResizeOption,
                                ),
                            )
                        } else {
                            null
                        }
                    }
                }
                is SheetType.Reorder -> {
                    ReorderBottomSheetContent(
                        type = type,
                        timelineState = checkNotNull(timelineState),
                    )
                }
                is SheetType.Adjustments -> {
                    timelineState?.clampPlayheadPositionToSelectedClip()
                    AdjustmentSheetContent(
                        type = type,
                        uiState = AdjustmentUiState.create(designBlock, engine),
                    )
                }
                is SheetType.Filter -> {
                    timelineState?.clampPlayheadPositionToSelectedClip()
                    EffectSheetContent(
                        type = type,
                        uiState = EffectUiState.create(designBlock, engine, AppearanceLibraryCategory.Filters),
                    )
                }
                is SheetType.Effect -> {
                    timelineState?.clampPlayheadPositionToSelectedClip()
                    EffectSheetContent(
                        type = type,
                        uiState = EffectUiState.create(designBlock, engine, AppearanceLibraryCategory.FxEffects),
                    )
                }
                is SheetType.Blur -> {
                    timelineState?.clampPlayheadPositionToSelectedClip()
                    EffectSheetContent(
                        type = type,
                        uiState = EffectUiState.create(designBlock, engine, AppearanceLibraryCategory.Blur),
                    )
                }
                is SheetType.Speed -> {
                    timelineState?.clampPlayheadPositionToSelectedClip()
                    SpeedBottomSheetContent(
                        type = type,
                        uiState = SpeedUiState.create(designBlock, engine),
                    )
                }
                is SheetType.Volume -> {
                    timelineState?.clampPlayheadPositionToSelectedClip()
                    VolumeBottomSheetContent(
                        type = type,
                        uiState = VolumeUiState.create(designBlock, engine),
                    )
                }
                is SheetType.Animation -> {
                    timelineState?.clampPlayheadPositionToSelectedClip()
                    AnimationBottomSheetContent(
                        type = type,
                        uiState = AnimationUiState.create(designBlock, engine, editor.currentLanguageCode),
                    )
                }
                is SheetType.TextOnPath -> {
                    timelineState?.clampPlayheadPositionToSelectedClip()
                    TextOnPathBottomSheetContent(
                        type = type,
                        uiState = TextOnPathUiState.create(designBlock, engine, editor.currentLanguageCode),
                    )
                }
                is SheetType.TextBackground -> {
                    timelineState?.clampPlayheadPositionToSelectedClip()
                    TextBackgroundBottomSheetContent(
                        type = type,
                        uiState = TextBackgroundUiState.create(designBlock, engine, colorPalette),
                    )
                }
                is SheetType.Font -> {
                    FontBottomSheetContent(
                        type = type,
                        uiState = FontUiState.create(type.designBlock, type.fontFamilies, engine),
                    )
                }
                is SheetType.FontSize -> {
                    FontSizeBottomSheetContent(
                        type = type,
                        uiState = FontSizeUiState.create(type.designBlock, engine),
                    )
                }
                is SheetType.Colors -> {
                    ColorsBottomSheetContent(
                        type = type,
                        uiState = ColorsUiState.create(colorPalette, type, engine),
                    )
                }
                else -> {
                    error(
                        "Unknown sheet type ${type.javaClass.name}. Prefer using SheetType.Custom over inheriting from SheetType.",
                    )
                }
            }
        }
    }

    private fun updateVisiblePageState() {
        // In Pages mode pageIndex follows the grid selection, not the camera.
        if (_uiState.value.pagesState != null) return
        val pages = engine.scene.getPages().takeIf { it.isNotEmpty() } ?: return
        val page = engine.scene.getCurrentPage()
        val currentVisible = pages.indexOfFirst { page == it }
        if (currentVisible >= 0 && pageIndex.value != currentVisible) {
            setPage(index = currentVisible, deselectAllBlocks = false)
        }
    }

    override fun send(event: EditorEvent) {
        // TODO: remove this when a better solution is found.
        if (event is BlockEvent && bottomSheetContent.value is CropBottomSheetContent && event != BlockEvent.OnResetCrop) {
            isStraighteningOrRotating = true
        }
        if (event is BlockEvent.OnResetCrop) {
            isCropReset = true
        }
        eventHandler.handleEvent(event)
        // Do not fully send internal events to customer just yet. Over time we will expose more and more events.
        if (event !is Event) {
            configuration.onEvent?.invoke(editorScope, event)
        }
    }

    private fun getBlockForEvents(): Block? = engine.block
        .findAllSelected()
        .firstOrNull()
        ?.takeIf { engine.block.isValid(it) }
        ?.let { createBlock(it, engine) }

    private fun requireDesignBlockForEvents(): DesignBlock = checkNotNull(getBlockForEvents()?.designBlock)

    private fun onCanvasMove(move: Boolean) {
        canvasMoveJob?.cancel()
        canvasMoveJob = viewModelScope.launch {
            // May be just a click, that's why debounce 100ms
            if (move) delay(100)
            publicState.update {
                it.copy(isTouchActive = move)
            }
        }
    }

    private fun updateBottomSheetUiState() {
        _bottomSheetContent.value ?: return
        val block = getBlockForEvents()
        val designBlock = block?.designBlock
        // In the case when a block is deleted, the block is unselected after receiving the delete event
        if (bottomSheetContent.value is CropBottomSheetContent && designBlock == null) {
            setBottomSheetContent { content ->
                val currentCropSheetType = currentCropSheetType ?: defaultCropSheetType
                if (content is CropBottomSheetContent) {
                    val useOldScaleRatio = isStraighteningOrRotating
                    isStraighteningOrRotating = false
                    val page = editor.engine.scene.getCurrentPage()
                    if (page != null) {
                        val forcedSources = forcedCropSourcesFor(page)
                        val forceCropActive = isForceCropActiveFor(page)
                        val allowContentFillMode = !forceCropActive
                        val allowResizeOption = !forceCropActive
                        CropBottomSheetContent(
                            type = content.type,
                            uiState = createCropUiState(
                                page,
                                engine,
                                initCropTranslationX,
                                initCropTranslationY,
                                if (useOldScaleRatio) content.uiState.cropScaleRatio else null,
                                cropMode = currentCropSheetType.mode,
                                pageAssetSourceId = forcedSources?.pageAssetSourceId ?: CropAssetSourceType.Page.sourceId,
                                cropAssetSourceId = forcedSources?.cropAssetSourceId ?: CropAssetSourceType.Crop.sourceId,
                                selectedAssetKey = forcedSources?.selectedAssetKey,
                                allowContentFillMode = allowContentFillMode,
                                allowResizeOption = allowResizeOption,
                            ),
                        )
                    } else {
                        null
                    }
                } else {
                    null
                }
            }
        } else if (designBlock != null && engine.block.isValid(designBlock)) {
            setBottomSheetContent { content ->
                when (content) {
                    is LayerBottomSheetContent ->
                        LayerBottomSheetContent(
                            type = content.type,
                            uiState = createLayerUiState(designBlock, engine),
                        )
                    is OptionsBottomSheetContent ->
                        OptionsBottomSheetContent(
                            type = content.type,
                            uiState = createShapeOptionsUiState(designBlock, engine),
                        )

                    is FillStrokeBottomSheetContent -> {
                        FillStrokeBottomSheetContent(
                            type = content.type,
                            uiState = FillStrokeUiState.create(
                                block,
                                engine,
                                colorPalette,
                                fillOnly = (content.type as? SheetType.FillStroke)?.fillOnly == true,
                            ),
                        )
                    }

                    is AdjustmentSheetContent -> {
                        AdjustmentSheetContent(
                            type = content.type,
                            uiState = AdjustmentUiState.create(designBlock, engine),
                        )
                    }

                    is EffectSheetContent -> {
                        EffectSheetContent(
                            type = content.type,
                            uiState = EffectUiState.create(designBlock, engine, content.uiState.libraryCategory),
                        )
                    }

                    is FormatBottomSheetContent ->
                        FormatBottomSheetContent(
                            type = content.type,
                            uiState = createFormatUiState(designBlock, engine),
                        )

                    is CropBottomSheetContent -> {
                        val currentCropSheetType = currentCropSheetType ?: defaultCropSheetType
                        val useOldScaleRatio = isStraighteningOrRotating
                        isStraighteningOrRotating = false
                        if (isCropReset) {
                            setInitCropValues(designBlock)
                        }
                        val forcedSources = forcedCropSourcesFor(designBlock)
                        val forceCropActive = isForceCropActiveFor(designBlock)
                        val allowContentFillMode = !forceCropActive
                        val allowResizeOption = !forceCropActive
                        CropBottomSheetContent(
                            type = content.type,
                            uiState = createCropUiState(
                                designBlock,
                                engine,
                                initCropTranslationX,
                                initCropTranslationY,
                                if (useOldScaleRatio) content.uiState.cropScaleRatio else null,
                                cropMode = currentCropSheetType.mode,
                                pageAssetSourceId = forcedSources?.pageAssetSourceId ?: CropAssetSourceType.Page.sourceId,
                                cropAssetSourceId = forcedSources?.cropAssetSourceId ?: CropAssetSourceType.Crop.sourceId,
                                selectedAssetKey = forcedSources?.selectedAssetKey,
                                allowContentFillMode = allowContentFillMode,
                                allowResizeOption = allowResizeOption,
                            ),
                        )
                    }

                    is VolumeBottomSheetContent -> {
                        VolumeBottomSheetContent(
                            type = content.type,
                            uiState = VolumeUiState.create(designBlock, engine),
                        )
                    }

                    is SpeedBottomSheetContent -> {
                        SpeedBottomSheetContent(
                            type = content.type,
                            uiState = SpeedUiState.create(designBlock, engine),
                        )
                    }

                    is AnimationBottomSheetContent -> {
                        AnimationBottomSheetContent(
                            type = content.type,
                            uiState = AnimationUiState.create(designBlock, engine, editor.currentLanguageCode),
                        )
                    }

                    is TextOnPathBottomSheetContent -> {
                        TextOnPathBottomSheetContent(
                            type = content.type,
                            uiState = TextOnPathUiState.create(designBlock, engine, editor.currentLanguageCode),
                        )
                    }

                    is TextBackgroundBottomSheetContent -> {
                        TextBackgroundBottomSheetContent(
                            type = content.type,
                            uiState = TextBackgroundUiState.create(designBlock, engine, colorPalette),
                        )
                    }

                    else -> {
                        content
                    }
                }
            }
        } else {
            setBottomSheetContent { content ->
                when (content) {
                    is FontBottomSheetContent -> {
                        val contentType = content.type as SheetType.Font
                        FontBottomSheetContent(
                            type = contentType,
                            uiState = FontUiState.create(
                                designBlock = contentType.designBlock,
                                fontFamilies = contentType.fontFamilies,
                                engine = engine,
                            ),
                        )
                    }

                    is FontSizeBottomSheetContent -> {
                        FontSizeBottomSheetContent(
                            type = content.type,
                            uiState = FontSizeUiState.create(
                                designBlock = (content.type as SheetType.FontSize).designBlock,
                                engine = engine,
                            ),
                        )
                    }

                    is ColorsBottomSheetContent -> {
                        ColorsBottomSheetContent(
                            type = content.type,
                            uiState = ColorsUiState.create(
                                engine = engine,
                                colorPalette = colorPalette,
                                sheetType = content.type as SheetType.Colors,
                            ),
                        )
                    }
                    else -> content
                }
            }
        }
    }

    private fun showPage(
        index: Int,
        deselectAllBlocks: Boolean = true,
    ) {
        if (deselectAllBlocks) {
            engine.deselectAllBlocks()
        }
        if (engine.editor.getSettingBoolean(keypath = FEATURE_PAGE_CAROUSEL_ENABLED)) {
            zoom(forceZoomToPage = true)
        } else {
            engine.showPage(index)
        }
    }

    private fun currentForceCropState(): ForceCropState? = forceCropState?.takeIf { state ->
        engine.block.isValid(state.block)
    }

    private fun setForceCropState(newState: ForceCropState?) {
        val normalizedState = newState?.takeIf { engine.block.isValid(it.block) }
        if (forceCropState == normalizedState) {
            return
        }
        forceCropState = normalizedState
        if (_uiState.value.pagesState != null) {
            updateEditorPagesState()
        }
        updateBottomSheetUiState()
    }

    private data class ForceCropState(
        val block: DesignBlock,
        val sourceId: String,
        val presetId: String,
        val transformPreset: AssetTransformPreset?,
    )

    private data class ForcedCropSources(
        val pageAssetSourceId: String?,
        val cropAssetSourceId: String?,
        val selectedAssetKey: String?,
    )

    private fun forcedCropSourcesFor(block: DesignBlock): ForcedCropSources? {
        val state = currentForceCropState() ?: return null
        if (state.block != block) {
            return null
        }
        val transformPreset = state.transformPreset
        val treatAsPage = when (transformPreset) {
            is AssetTransformPreset.FixedSize -> true
            else -> state.sourceId == CropAssetSourceType.Page.sourceId
        }
        val pageAssetSourceId = if (treatAsPage) state.sourceId else null
        val cropAssetSourceId = if (treatAsPage) null else state.sourceId
        return ForcedCropSources(
            pageAssetSourceId = pageAssetSourceId,
            cropAssetSourceId = cropAssetSourceId,
            selectedAssetKey = state.presetId,
        )
    }

    private fun isForceCropActiveFor(block: DesignBlock?): Boolean {
        val state = currentForceCropState() ?: return false
        return block != null && state.block == block
    }

    private fun EditorPagesState.applyForceCropConstraints(): EditorPagesState {
        val forceState = currentForceCropState()
        if (forceState == null || forceState.block != selectedPage.block) {
            return this
        }
        val filteredDockOptions = dockOptions.filterNot { option ->
            option.actions.any { action ->
                action is EditorEvent.Sheet.Open && action.type is SheetType.ResizeAll
            }
        }
        return if (filteredDockOptions == dockOptions) {
            this
        } else {
            copy(dockOptions = filteredDockOptions)
        }
    }

    private fun setPage(
        index: Int,
        deselectAllBlocks: Boolean = true,
        closeSheet: Boolean = true,
    ) {
        if (index == pageIndex.value) return
        pageIndex.update { index }
        showPage(index, deselectAllBlocks)
        if (closeSheet) {
            send(EditorEvent.Sheet.Close(animate = false))
        }
    }

    private fun setPageIndex(index: Int) {
        if (index == pageIndex.value) return
        pageIndex.update { index }
    }

    private fun canHandleBackPress(viewMode: EditorViewMode) = bottomSheetContent.value != null ||
        viewMode !is EditorViewMode.Edit ||
        (engine.editor.getSettingBoolean(keypath = FEATURE_PAGE_CAROUSEL_ENABLED).not() && pageIndex.value > 0)

    private fun setBottomSheetContent(function: suspend (BottomSheetContent?) -> BottomSheetContent?) = viewModelScope.launch {
        val oldBottomSheetContent = bottomSheetContent.value
        val newValue = function(oldBottomSheetContent)
        if (newValue == null && oldBottomSheetContent != null && bottomSheetHeight > 0F) {
            // Means it is closing
            currentCropSheetType = null
            closingSheetContent = _bottomSheetContent.value
        }
        _bottomSheetContent.value = newValue
        publicState.update { it.copy(activeSheet = newValue?.type) }
    }

    @OptIn(UnstableEngineApi::class)
    private fun onSheetClosed() {
        currentCropSheetType = null
        bottomSheetHeight = 0F
        publicState.update { it.copy(activeSheetState = null) }
        setBottomSheetContent { null }
        // Resume the IME if we suspended it for the just closed sheet.
        if (engine.editor.getSettingBoolean(SOFTWARE_KEYBOARD_SUSPENDED_SETTING)) {
            engine.editor.setSettingBoolean(SOFTWARE_KEYBOARD_SUSPENDED_SETTING, false)
            updateKeyboardShowing()
        }
        if (engine.isEngineRunning().not()) return
        if (engine.editor.getEditMode() == CROP_EDIT_MODE) {
            engine.editor.setEditMode(TRANSFORM_EDIT_MODE)
        }
    }

    private fun onZoomFinish() {
        updateZoomState()
    }

    private var initiallySetEditorSelectGlobalScope = GlobalScope.DEFER

    private fun loadScene(inPortraitMode: Boolean) {
        this.inPortraitMode = inPortraitMode
        val isConfigChange = !firstLoad
        firstLoad = false
        if (isConfigChange) {
            setEditorViewMode(viewMode)
        } else {
            observeSelectedBlock()
            observeClicked()
            observeHistory()
            observeUiStateChanges()
            observeEvents()
            observeEditorStateChange()
            observeActiveScene()
            viewModelScope.launch {
                try {
                    migrationHelper.migrate()
                    setSettingsForEditorUi(engine, editor.baseUri)
                    configuration.onCreate?.invoke(editorScope)
                    requireNotNull(engine.scene.get()) { "onCreate body must contain scene creation." }
                    isSceneLoaded.update { true }
                    configuration.onLoaded?.invoke(editorScope)
                } catch (exception: Exception) {
                    onError(exception)
                }
            }
        }
    }

    private fun observeActiveScene() {
        viewModelScope.launch {
            var carouselPageChangeJob: Job? = null
            isSceneLoaded.first { it }
            engine.scene.onActiveChanged().onStart { emit(Unit) }.collect {
                setPageIndex(index = 0)
                setForceCropState(null)
                initiallySetEditorSelectGlobalScope = engine.editor.getGlobalScope(Scope.EditorSelect)
                carouselPageChangeJob?.cancel()
                carouselPageChangeJob = observeCarouselPageChanges()
                showPage(pageIndex.value, deselectAllBlocks = false)
                setEditMode(EditorViewMode.Edit(), deselectAllBlocks = false)
                zoom(forceZoomToPage = true)
                engine.resetHistory()
            }
        }
    }

    private fun onSystemCameraClick(
        captureVideo: Boolean,
        designBlock: DesignBlock?,
        addToBackgroundTrack: Boolean,
    ) = viewModelScope.launch {
        val context = editor.activity
        val uri = withContext(Dispatchers.IO) {
            val tempFile = File.createTempFile(
                "imgly_",
                if (captureVideo) ".mp4" else ".jpg",
                context.filesDir,
            )
            FileProvider.getUriForFile(context, "${context.packageName}.ly.img.editor.fileprovider", tempFile)
        }
        val (uploadSource, launchContract) = if (captureVideo) {
            AssetSourceType.VideoUploads to ActivityResultContracts.CaptureVideo()
        } else {
            AssetSourceType.ImageUploads to ActivityResultContracts.TakePicture()
        }
        EditorEvent.LaunchContract(launchContract, uri) { success ->
            if (!success) {
                return@LaunchContract
            }

            val event = designBlock?.let {
                LibraryEvent.OnReplaceUri(
                    uri = uri,
                    assetSource = uploadSource,
                    designBlock = designBlock,
                )
            } ?: LibraryEvent.OnAddUri(
                assetSource = uploadSource,
                uri = uri,
                addToBackgroundTrack = addToBackgroundTrack,
            )
            (editorContext.eventHandler as EditorUiViewModel).libraryViewModel.onEvent(event)

            viewModelScope.launch {
                val galleryUri = withContext(Dispatchers.IO) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        copyCaptureToMediaStore(context, uri, captureVideo)
                    } else {
                        copyCaptureToLegacyGallery(context, uri, captureVideo)
                        null
                    }
                }
                galleryUri?.let {
                    runCatching { SystemGalleryPermission.addSelected(it, context) }
                    runCatching { engine.asset.assetSourceContentsChanged(AssetSourceType.GalleryAllVisuals.sourceId) }
                }
            }
        }.let(::send)
    }

    private fun copyCaptureToMediaStore(
        context: android.content.Context,
        sourceUri: android.net.Uri,
        isVideo: Boolean,
    ): android.net.Uri? {
        val resolver = context.contentResolver
        val timestamp = System.currentTimeMillis()
        val fileName = if (isVideo) "VID_$timestamp.mp4" else "IMG_$timestamp.jpg"
        val collection = if (isVideo) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        } else {
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        }
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
            put(MediaStore.MediaColumns.MIME_TYPE, if (isVideo) "video/mp4" else "image/jpeg")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.MediaColumns.DATE_TAKEN, timestamp)
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES,
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val destination = resolver.insert(collection, values) ?: return null
        return try {
            resolver.openOutputStream(destination)?.use { output ->
                resolver.openInputStream(sourceUri)?.use { input ->
                    input.copyTo(output)
                } ?: throw IllegalStateException("Unable to open camera result stream")
            } ?: throw IllegalStateException("Unable to open MediaStore output stream")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val completeValues = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
                resolver.update(destination, completeValues, null, null)
            }
            destination
        } catch (throwable: Throwable) {
            resolver.delete(destination, null, null)
            null
        }
    }

    private fun copyCaptureToLegacyGallery(
        context: android.content.Context,
        sourceUri: android.net.Uri,
        isVideo: Boolean,
    ) {
        val directory = Environment.getExternalStoragePublicDirectory(
            if (isVideo) Environment.DIRECTORY_MOVIES else Environment.DIRECTORY_PICTURES,
        )
        if (!directory.exists()) {
            directory.mkdirs()
        }
        val timestamp = System.currentTimeMillis()
        val destinationFile = File(directory, if (isVideo) "VID_$timestamp.mp4" else "IMG_$timestamp.jpg")
        runCatching {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destinationFile).use { output ->
                    input.copyTo(output)
                }
            }
            MediaScannerConnection.scanFile(context, arrayOf(destinationFile.absolutePath), null, null)
        }
    }

    private fun onLaunchGetContent(
        mimeType: String,
        uploadAssetSourceType: UploadAssetSourceType,
        designBlock: DesignBlock?,
        addToBackgroundTrack: Boolean,
    ) {
        EditorEvent.LaunchContract(ActivityResultContracts.GetContent(), mimeType) { uri ->
            uri?.let {
                val event = designBlock?.let {
                    LibraryEvent.OnReplaceUri(
                        uri = uri,
                        assetSource = uploadAssetSourceType,
                        designBlock = designBlock,
                    )
                } ?: LibraryEvent.OnAddUri(
                    assetSource = uploadAssetSourceType,
                    uri = uri,
                    addToBackgroundTrack = addToBackgroundTrack,
                )
                // IMPORTANT! we cannot simply invoke it on this.libraryViewModel as it's the previous instance and it will result to a crash!
                // + we do not want to capture anything from previous instance to allow it GC.
                (editorContext.eventHandler as EditorUiViewModel).libraryViewModel.onEvent(event)
                editorContext.eventHandler.send(EditorEvent.Sheet.Close(animate = true))
            }
        }.let(::send)
    }

    private fun onVideoCameraClick(callback: (@Composable () -> Unit) -> Unit) = callback {
        val isImglyCameraAvailable = androidx.compose.runtime.remember {
            runCatching { CaptureMedia() }.isSuccess
        } &&
            IMGLYCameraFeature.enabled

        if (isImglyCameraAvailable) {
            // Reached only from the video timeline's add-clip button (Event.OnVideoCameraClick),
            // so the editor always accepts video → open the camera in Mixed/Multi.
            Dock.Button.rememberImglyCamera(acceptsVideoCapture = { true })
        } else {
            Dock.Button.rememberSystemCamera()
        }.onClick(Dock.ItemScope(editorScope))
    }

    private fun onLaunchContractResult(
        onResult: EditorScope.(Any?) -> Unit,
        editorScope: EditorScope,
        result: Any?,
    ) = viewModelScope.launch {
        editorScope.run {
            editorContext.engine.awaitEngineAndSceneLoad()
            onResult(result)
        }
    }

    private fun onBottomSheetStateChange(state: SheetState) {
        publicState.update { it.copy(activeSheetState = state) }
    }

    private fun onBottomSheetHeightChange(
        heightInDp: Float,
        sheetMaxHeightInDp: Float,
    ) {
        if (viewMode !is EditorViewMode.Edit || !isSceneLoaded.value) return
        val closingSheetContent = this.closingSheetContent
        if (heightInDp == 0F && closingSheetContent != null) {
            this.closingSheetContent = null
        }
        val bottomSheetContent = bottomSheetContent.value

        // we don't want to change zoom level for floating sheets
        if (bottomSheetContent?.isFloating == true) return
        if (closingSheetContent?.isFloating == true) return

        bottomSheetHeight = heightInDp
        if (bottomStaticContentFullHeight > sheetMaxHeightInDp && bottomSheetContent != null) {
            _uiState.update {
                it.copy(
                    bottomPanelMaxHeightInDp =
                        sheetMaxHeightInDp + ((bottomStaticContentFullHeight - sheetMaxHeightInDp) * (1 - heightInDp / sheetMaxHeightInDp)),
                )
            }
        } else if (engine.scene.getZoomLevel() == fitToPageZoomLevel) {
            _uiState.update {
                it.copy(bottomPanelMaxHeightInDp = Float.MAX_VALUE)
            }
            updateInsets(bottomInset = max(heightInDp, bottomStaticContentHeight))
        }
    }

    private fun onEditorSizeChange(
        widthInDp: Float,
        heightInDp: Float,
    ) {
        updateDimension(
            update = { it.copy(editor = Size(widthInDp.dp, heightInDp.dp)) },
        ) {
            updateInsets(zoomToPage = true)
        }
    }

    private fun onNavigationBarSizeChange(
        widthInDp: Float,
        heightInDp: Float,
    ) {
        updateDimension(
            update = { it.copy(navigationBar = Size(widthInDp.dp, heightInDp.dp)) },
        ) {
            updateInsets(
                topInset = heightInDp,
                zoomToPage = engine.scene.getZoomLevel() == fitToPageZoomLevel,
            )
        }
    }

    private fun onBottomPanelSizeChange(
        widthInDp: Float,
        heightInDp: Float,
    ) {
        updateDimension(
            update = { it.copy(bottomPanel = Size(widthInDp.dp, heightInDp.dp)) },
        ) {
            onBottomStaticContentChange()
        }
    }

    private fun onDockSizeChange(
        widthInDp: Float,
        heightInDp: Float,
    ) {
        updateDimension(
            update = { it.copy(dock = Size(widthInDp.dp, heightInDp.dp)) },
        ) {
            onBottomStaticContentChange()
        }
    }

    private fun onInspectorBarSizeChange(
        widthInDp: Float,
        heightInDp: Float,
    ) {
        updateDimension(
            update = { it.copy(inspectorBar = Size(widthInDp.dp, heightInDp.dp)) },
        )
    }

    private fun updateDimension(
        update: (Dimensions) -> Dimensions,
        onSuccess: () -> Unit = {},
    ) {
        val oldDimensions = publicState.value.dimensions
        val newDimensions = update(oldDimensions)
        if (newDimensions != oldDimensions) {
            publicState.update {
                it.copy(dimensions = newDimensions)
            }
            onSuccess()
        }
    }

    private fun onBottomStaticContentChange() {
        val heightInDp = publicState.value.dimensions.run {
            bottomPanel.height.value + dock.height.value
        }
        this.bottomStaticContentHeight = when {
            this.bottomSheetContent.value != null -> heightInDp
            this.closingSheetContent == null -> heightInDp.also { this.bottomStaticContentFullHeight = it }
            else -> bottomStaticContentFullHeight
        }
        updateInsets(
            bottomInset = max(bottomSheetHeight, heightInDp),
            zoomToPage = engine.scene.getZoomLevel() == fitToPageZoomLevel,
        )
    }

    private fun onKeyboardClose() {
        engine.editor.setEditMode(TRANSFORM_EDIT_MODE)
    }

    private fun onKeyboardHeightChange(heightInDp: Float) {
        val staticHeightInDp = publicState.value.dimensions.run {
            bottomPanel.height.value + dock.height.value
        }
        updateInsets(
            bottomInset = max(heightInDp, staticHeightInDp),
        )
    }

    private fun observeCarouselPageChanges(): Job? {
        if (engine.editor.getSettingBoolean(keypath = FEATURE_PAGE_CAROUSEL_ENABLED).not()) return null
        return viewModelScope.launch {
            engine.editor.onCarouselPageChanged()
                .onEach {
                    val pageIndex = engine.scene.getPages().indexOf(it)
                    setPageIndex(pageIndex)
                }.collect()
        }
    }

    private fun onAddPage(index: Int) {
        val currentPage = engine.scene.getCurrentPage() ?: return
        val parent = engine.block.getParent(currentPage) ?: return
        val newPage = engine.block.create(DesignBlockType.Page)
        engine.block.setWidth(newPage, engine.block.getWidth(currentPage))
        engine.block.setHeight(newPage, engine.block.getHeight(currentPage))
        engine.block.setFillType(newPage, FillType.Color)
        engine.block.insertChild(parent, newPage, index)
        engine.editor.addUndoStep()
    }

    private fun updateEditorPagesState() {
        _uiState.update {
            val pagesState = (
                it.pagesState
                    ?.copy(engine, markThumbnails = true)
                    ?: createEditorPagesState(pagesSessionId++, engine, pageIndex.value)
            ).applyForceCropConstraints()
            // Pre-populate cached thumbnails so they display instantly (no scroll debounce needed).
            // Pages whose content changed have mark = true and will be re-generated on bind;
            // unchanged pages get their cached bitmap immediately, avoiding a flash to loading state.
            val populatedState = pagesState.let { state ->
                val populatedPages = state.pages.map { page ->
                    if (page.thumbnail == null) {
                        val uuid = runCatching { engine.block.getUUID(page.block) }.getOrNull()
                        val cached = uuid?.let { thumbnailCache[it] }
                        if (cached != null) {
                            page.copy(mark = false).also { it.thumbnail = cached }
                        } else {
                            page
                        }
                    } else {
                        page
                    }
                }
                state.copy(pages = populatedPages)
            }
            setPage(populatedState.selectedPageIndex, closeSheet = false)
            it.copy(pagesState = populatedState)
        }
    }

    private fun onPagesSelectionChange(page: EditorPagesState.Page) {
        _uiState.update {
            if (page == it.pagesState?.selectedPage) {
                it
            } else {
                val updatedPagesState = it.pagesState
                    ?.copy(engine, selectedPage = page, markThumbnails = false)
                    ?.applyForceCropConstraints()
                it.copy(pagesState = updatedPagesState)
            }
        }
    }

    private var thumbnailQueueJob: Job? = null

    // LIFO list: newest requests at the end. The consumer always takes from the end
    // so the most recently visible pages are rendered first. Trimmed to 20 entries
    // to discard stale requests from pages the user scrolled past.
    //
    // Main-thread-only: every caller (`ensureThumbnailConsumerRunning`, `onPagesModePageBind`,
    // `onStopGenerateAllPageThumbnails`) is reached from `viewModelScope` / `EventsHandler`,
    // both of which default to `Dispatchers.Main.immediate`. Do not access from background
    // dispatchers without switching back to Main first.
    private val pendingThumbnailRequests = mutableListOf<ThumbnailRequest>()

    private data class ThumbnailRequest(
        val page: EditorPagesState.Page,
        val pageHeight: Int,
    )

    private fun ensureThumbnailConsumerRunning() {
        if (thumbnailQueueJob?.isActive == true) return
        thumbnailQueueJob = viewModelScope.launch {
            while (isActive) {
                // Trim to most recent 20 entries — discard older requests from
                // pages the user scrolled past during fast scroll
                if (pendingThumbnailRequests.size > 20) {
                    val recent = pendingThumbnailRequests.takeLast(20)
                    pendingThumbnailRequests.clear()
                    pendingThumbnailRequests.addAll(recent)
                }
                val request = pendingThumbnailRequests.removeLastOrNull()
                if (request != null) {
                    // Skip if already cached (page was processed while waiting in the list)
                    val uuid = try {
                        engine.block.getUUID(request.page.block)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (_: Exception) {
                        null
                    }
                    if (uuid != null && thumbnailCache.containsKey(uuid)) continue
                    yield()
                    generateSingleThumbnail(request)
                } else {
                    delay(50)
                }
            }
        }
    }

    private suspend fun generateSingleThumbnail(request: ThumbnailRequest) {
        // Render at the cell's measured pixel height; a fixed cap upscaled the
        // bitmap and blurred previews. Memory is bounded by the LRU thumbnailCache.
        val height = request.pageHeight
        val result = try {
            engine.block
                .generateVideoThumbnailSequence(
                    block = request.page.block,
                    thumbnailHeight = height,
                    timeBegin = 0.0,
                    timeEnd = 0.1,
                    numberOfFrames = 1,
                ).firstOrNull()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } ?: return
        val bitmap = withContext(Dispatchers.Default) {
            createBitmap(result.width, result.height).also {
                it.copyPixelsFromBuffer(result.imageData)
            }
        }
        // Store in cache keyed by page UUID
        val uuid = try {
            engine.block.getUUID(request.page.block)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }
        if (uuid != null) {
            thumbnailCache[uuid] = bitmap
        }
        val newPage = request.page.copy(mark = false).also {
            it.thumbnail = bitmap
        }
        _uiState.update {
            it.copy(pagesState = it.pagesState?.copy(updatedPage = newPage))
        }
    }

    private fun onPagesModePageBind(
        page: EditorPagesState.Page,
        pageHeight: Int,
    ) {
        if (page.mark.not()) return
        if (pageHeight <= 0) return

        // Check in-memory cache first (survives pages mode exit/re-entry)
        val uuid = runCatching { engine.block.getUUID(page.block) }.getOrNull()
        if (uuid != null) {
            val cached = thumbnailCache[uuid]
            if (cached != null) {
                val newPage = page.copy(mark = false).also { it.thumbnail = cached }
                _uiState.update {
                    it.copy(pagesState = it.pagesState?.copy(updatedPage = newPage))
                }
                return
            }
        }

        // Cache miss: add to LIFO list (newest at end, processed first).
        // See the pendingThumbnailRequests declaration for main-thread-only contract.
        pendingThumbnailRequests.removeAll { it.page.block == page.block }
        pendingThumbnailRequests.add(ThumbnailRequest(page, pageHeight))
        ensureThumbnailConsumerRunning()
    }

    private fun onStopGenerateAllPageThumbnails() {
        thumbnailQueueJob?.cancel()
        thumbnailQueueJob = null
        pendingThumbnailRequests.clear()
    }

    private fun updateInsets(
        topInset: Float = imgLyInsets.value.top,
        bottomInset: Float = imgLyInsets.value.bottom,
        leftInset: Float = imgLyInsets.value.left,
        rightInset: Float = imgLyInsets.value.right,
        zoomToPage: Boolean = false,
    ) {
        imgLyInsets.value = Rect(
            top = topInset,
            bottom = bottomInset,
            left = leftInset,
            right = rightInset,
        )
        publicState.update {
            val extraInsets = publicState.value.extraInsets
            it.copy(
                insets = it.insets.copy(
                    left = leftInset.dp + extraInsets.left,
                    top = topInset.dp + extraInsets.top,
                    right = rightInset.dp + extraInsets.right,
                    bottom = bottomInset.dp + extraInsets.bottom,
                ),
            )
        }
        // publicState.value.viewMode is EditorViewMode.Edit check is added here, because
        // we do not want to zoom to the page outside edit mode in Apparel editor. Since apparel
        // zooms to backdrop instead, there will be a jump. This way we leave entire control of preview
        // and pages modes fully on starter kit side.
        if (isSceneLoaded.value && publicState.value.viewMode is EditorViewMode.Edit) {
            zoom(forceZoomToPage = zoomToPage)
        }
    }

    private var zoomJob: Job? = null
    private var fitToPageZoomLevel = 0f

    @OptIn(UnstableEngineApi::class)
    private fun zoom(forceZoomToPage: Boolean = false): Job {
        zoomJob?.cancel()
        return viewModelScope
            .launch {
                val page = engine.scene.getPages().getOrNull(pageIndex.value) ?: return@launch
                val currentInsets = publicState.value.insets
                val selectedBlock = getBlockForEvents()
                val shouldZoomToPage = forceZoomToPage ||
                    selectedBlock == null ||
                    engine.scene.getZoomLevel() == fitToPageZoomLevel
                val blocks = buildList {
                    add(page)
                    if (engine.editor.getEditMode() == TEXT_EDIT_MODE && selectedBlock?.type == BlockType.Text) {
                        add(selectedBlock.designBlock)
                    }
                }
                val extraInsets = publicState.value.extraInsets
                engine.scene.enableCameraPositionClamping(
                    blocks = blocks,
                    paddingLeft = currentInsets.left.value - extraInsets.left.value,
                    paddingTop = currentInsets.top.value - extraInsets.top.value,
                    paddingRight = currentInsets.right.value - extraInsets.right.value,
                    paddingBottom = currentInsets.bottom.value - extraInsets.bottom.value,
                    scaledPaddingLeft = extraInsets.left.value,
                    scaledPaddingTop = extraInsets.top.value,
                    scaledPaddingRight = extraInsets.right.value,
                    scaledPaddingBottom = extraInsets.bottom.value,
                )

                if (shouldZoomToPage) {
                    engine.scene.enableCameraZoomClamping(
                        blocks = blocks,
                        minZoomLimit = 1.0F,
                        maxZoomLimit = 5.0F,
                        paddingLeft = currentInsets.left.value,
                        paddingTop = currentInsets.top.value,
                        paddingRight = currentInsets.right.value,
                        paddingBottom = currentInsets.bottom.value,
                    )
                    engine.zoomToPage(pageIndex.value, currentInsets)
                    fitToPageZoomLevel = engine.scene.getZoomLevel()
                }

                val selectedDesignBlock = selectedBlock?.designBlock

                if (selectedDesignBlock != null && !shouldZoomToPage && engine.editor.getEditMode() != TEXT_EDIT_MODE) {
                    // The delay acts as a debouncing mechanism.
                    delay(8)

                    if (engine.block.isValid(selectedDesignBlock)) {
                        val boundingBoxRect = engine.block.getScreenSpaceBoundingBoxRect(listOf(selectedDesignBlock))
                        val bottomSheetTop = (publicState.value.dimensions.editor.height - publicState.value.insets.bottom).value
                        val camera = engine.getCamera()
                        val oldCameraPosX = engine.block.getPositionX(camera)
                        val oldCameraPosY = engine.block.getPositionY(camera)
                        var newCameraPosX = oldCameraPosX
                        var newCameraPosY = oldCameraPosY
                        val canvasWidthDp = engine.block.getFloat(camera, "camera/resolution/width") /
                            engine.block.getFloat(camera, "camera/pixelRatio")
                        val selectedBlockCenterX = boundingBoxRect.centerX()

                        if (selectedBlockCenterX > canvasWidthDp) {
                            newCameraPosX = oldCameraPosX +
                                engine.dpToCanvasUnit(
                                    (canvasWidthDp / 2 - boundingBoxRect.width() / 2) + (boundingBoxRect.right - canvasWidthDp),
                                )
                        } else if (selectedBlockCenterX < 0) {
                            newCameraPosX = oldCameraPosX -
                                engine.dpToCanvasUnit(
                                    (canvasWidthDp / 2 - boundingBoxRect.width() / 2) - boundingBoxRect.left,
                                )
                        }

                        // bottom sheet is covering more than 50% of selected block
                        if (bottomSheetTop < boundingBoxRect.centerY()) {
                            newCameraPosY = oldCameraPosY + engine.dpToCanvasUnit(48 + boundingBoxRect.bottom - bottomSheetTop)
                        } else if (boundingBoxRect.centerY() < 64) {
                            newCameraPosY = oldCameraPosY - engine.dpToCanvasUnit(48 + bottomSheetTop - boundingBoxRect.bottom)
                        }

                        if (newCameraPosX != oldCameraPosX || newCameraPosY != oldCameraPosY) {
                            engine.overrideAndRestore(camera, Scope.LayerMove) {
                                engine.block.setPositionX(it, newCameraPosX)
                                engine.block.setPositionY(it, newCameraPosY)
                            }
                        }
                    }
                }
                if (engine.editor.getEditMode() == TEXT_EDIT_MODE) {
                    zoomToText()
                }
                onZoomFinish()
            }.also {
                zoomJob = it
            }
    }

    private fun zoomToText() {
        engine.zoomToSelectedText(
            insets = publicState.value.insets,
            canvasHeight = publicState.value.dimensions.editor.height.value,
        )
    }

    private fun observeUiStateChanges() {
        viewModelScope.launch {
            merge(
                isSceneLoaded,
                publicState,
                isExporting,
                isKeyboardShowing,
            ).collect {
                val pageCount = if (isSceneLoaded.value) engine.scene.getPages().size else 0
                val editingTextCardUiState = if (isKeyboardShowing.value) {
                    getBlockForEvents()?.designBlock?.let { createEditingTextCardUiState(it, engine) }
                } else {
                    null
                }
                _uiState.update {
                    it.copy(
                        allowEditorInteraction = viewMode is EditorViewMode.Edit,
                        isEditingText = isKeyboardShowing.value,
                        editingTextCardUiState = editingTextCardUiState,
                        timelineState = timelineState,
                        pageCount = pageCount,
                        isSceneLoaded = isSceneLoaded.value,
                    )
                }
            }
        }

        viewModelScope.launch {
            merge(
                pageIndex,
                bottomSheetContent,
            ).collect {
                publicState.update { it.copy(isBackHandlerEnabled = canHandleBackPress(it.viewMode)) }
            }
        }
    }

    private fun updateZoomState() {
        val newZoomState = isSceneLoaded.value && (abs(engine.scene.getZoomLevel() - fitToPageZoomLevel) > 0.001f)
        if (newZoomState != isZoomedIn.value) {
            isZoomedIn.update { newZoomState }
        }
    }

    private var cursorPos = 0f
    private var lastTextCursorRange: IntRange? = null

    // Dedup key for fill/stroke picker refreshes: cursor moves without a
    // selection change leave the effective range unchanged, so we skip the work.
    private var lastEffectiveTextRange: IntRange? = null

    private fun observeEvents() {
        viewModelScope.launch {
            engine.event.subscribe().collect {
                val durationBefore = timelineState?.totalDuration
                timelineState?.refresh(it)
                // Before dock configurability feature, unnecessary dock updates were updating the _uiState,
                // therefore, VideoUiViewModel.uiState were getting the updates. Since those updates are gone now,
                // we have to force state update.
                // if condition is absolutely necessary otherwise we will have text block jumping up and down
                // that was fixed in this PR https://github.com/imgly/ubq/pull/7807
                if (timelineState?.totalDuration != durationBefore) {
                    _uiState.update { state ->
                        state.copy(timelineTrigger = state.timelineTrigger.not())
                    }
                }
                // text handling
                if (engine.editor.getEditMode() != TEXT_EDIT_MODE) return@collect
                val textCursorPositionInScreenSpaceY = engine.editor.getTextCursorPositionInScreenSpaceY()
                if (textCursorPositionInScreenSpaceY != cursorPos) {
                    cursorPos = textCursorPositionInScreenSpaceY
                    zoomToText()
                }
                val cursorRange = runCatching { engine.block.getTextCursorRange() }.getOrNull()
                val designBlock = getBlockForEvents()?.designBlock
                val engineListStyle = if (designBlock != null && cursorRange != null) {
                    runCatching {
                        val idx =
                            engine.block.getTextParagraphIndices(designBlock, cursorRange.first, cursorRange.last).firstOrNull() ?: 0
                        engine.block.getTextListStyle(designBlock, idx)
                    }.getOrNull()
                } else {
                    null
                }
                val cursorChanged = cursorRange != lastTextCursorRange

                val formatSheet = bottomSheetContent.value as? FormatBottomSheetContent
                val formatSheetNeedsRefresh = formatSheet != null &&
                    (cursorChanged || (engineListStyle != null && engineListStyle != formatSheet.uiState.listStyle))

                // The fill/stroke picker's "current fill colour" mirrors the colour of
                // the live effective range.Only refresh when that range actually changes.
                val fillStrokePickerOpen = bottomSheetContent.value is FillStrokeBottomSheetContent
                val effectiveRange = if (fillStrokePickerOpen && designBlock != null) {
                    engine.block.effectiveTextRange(designBlock)
                } else {
                    null
                }
                val fillStrokeSheetNeedsRefresh = fillStrokePickerOpen &&
                    effectiveRange != lastEffectiveTextRange

                val editingCardNeedsRefresh = isKeyboardShowing.value && designBlock != null

                if (formatSheetNeedsRefresh || editingCardNeedsRefresh) {
                    lastTextCursorRange = cursorRange
                }
                if (fillStrokeSheetNeedsRefresh) {
                    lastEffectiveTextRange = effectiveRange
                }
                if (formatSheetNeedsRefresh || fillStrokeSheetNeedsRefresh) {
                    updateBottomSheetUiState()
                }
                if (editingCardNeedsRefresh && designBlock != null) {
                    val newState = createEditingTextCardUiState(designBlock, engine)
                    _uiState.update { state -> state.copy(editingTextCardUiState = newState) }
                }
            }
        }
    }

    private fun observeEditorStateChange() {
        viewModelScope.launch {
            var flag = false
            var defaultPinchAction = ""
            var previousEditMode: String? = null

            engine.editor.onStateChanged().map { engine.editor.getEditMode() }.distinctUntilChanged().collect { editMode ->
                when (editMode) {
                    TEXT_EDIT_MODE -> {
                        lastTextCursorRange = null
                        lastEffectiveTextRange = null
                        send(EditorEvent.Sheet.Close(animate = false))
                    }
                    CROP_EDIT_MODE -> {
                        val block = engine.block.findAllSelected().single()
                        defaultPinchAction = engine.editor.getSettingEnum("touch/pinchAction")
                        engine.editor.setSettingEnum("touch/pinchAction", TOUCH_ACTION_SCALE)
                        setInitCropValues(block)
                        // no need to send EditorEvent.Sheet.Open here
                        setBottomSheetContent {
                            /**
                             * Think about making default crop type configurable by customer.
                             */
                            val type = currentCropSheetType ?: defaultCropSheetType
                            val selection = engine.block.findAllSelected().single()
                            val forcedSources = forcedCropSourcesFor(selection)
                            val forceCropActive = isForceCropActiveFor(selection)
                            val allowContentFillMode = !forceCropActive
                            val allowResizeOption = !forceCropActive
                            CropBottomSheetContent(
                                type = type,
                                uiState = createCropUiState(
                                    selection,
                                    engine,
                                    cropMode = type.mode,
                                    pageAssetSourceId = forcedSources?.pageAssetSourceId ?: CropAssetSourceType.Page.sourceId,
                                    cropAssetSourceId = forcedSources?.cropAssetSourceId ?: CropAssetSourceType.Crop.sourceId,
                                    initCropTranslationX = initCropTranslationX,
                                    initCropTranslationY = initCropTranslationY,
                                    selectedAssetKey = forcedSources?.selectedAssetKey,
                                    allowContentFillMode = allowContentFillMode,
                                    allowResizeOption = allowResizeOption,
                                ),
                            )
                        }
                    }

                    else -> {
                        // Close crop bottom sheet if coming back from crop mode
                        if (bottomSheetContent.value is CropBottomSheetContent) {
                            send(EditorEvent.Sheet.Close(animate = false))
                        }

                        // restore pinchAction if coming from crop mode
                        if (previousEditMode == CROP_EDIT_MODE) {
                            engine.editor.setSettingEnum("touch/pinchAction", defaultPinchAction)
                        }

                        if (!flag) {
                            flag = true
                            zoom(forceZoomToPage = true)
                        }
                    }
                }
                updateKeyboardShowing()
                previousEditMode = editMode
            }
        }
    }

    @OptIn(UnstableEngineApi::class)
    private fun updateKeyboardShowing() {
        val showKeyboard = engine.editor.getEditMode() == TEXT_EDIT_MODE &&
            !engine.editor.getSettingBoolean(SOFTWARE_KEYBOARD_SUSPENDED_SETTING)
        if (isKeyboardShowing.value == showKeyboard) return
        if (isKeyboardShowing.value && !showKeyboard) {
            onKeyboardHeightChange(heightInDp = 0F)
        }
        isKeyboardShowing.update { showKeyboard }
    }

    private fun setInitCropValues(block: DesignBlock) {
        isStraighteningOrRotating = false
        isCropReset = false
        initCropTranslationX = engine.block.getCropTranslationX(block)
        initCropTranslationY = engine.block.getCropTranslationY(block)
    }

    private fun observeSelectedBlock() {
        viewModelScope.launch {
            var previousSelectedBlock: DesignBlock? = null
            engine.block.onSelectionChanged().filter { isSceneLoaded.value }.collect {
                val block = getBlockForEvents()
                if (previousSelectedBlock != block?.designBlock) {
                    if (bottomSheetContent.value?.type is SheetType.Voiceover) {
                        val activeVoiceOverTarget = voiceOverSheetTargetBlock
                        if (block?.designBlock != null && block.designBlock != activeVoiceOverTarget) {
                            send(EditorEvent.Sheet.Close(animate = false))
                        }
                        return@collect
                    }
                    if (block != null &&
                        bottomSheetContent.value is LibraryReplaceBottomSheetContent &&
                        engine.isPlaceholder(block.designBlock)
                    ) {
                        // The replace sheet is already open and we switched to another placeholder.
                        // Update the sheet's target block so the replacement applies to the new selection.
                        openPlaceholderSheetIfNeeded(block.designBlock)
                    } else if (block == null || bottomSheetContent.value != null) {
                        send(EditorEvent.Sheet.Close(animate = false))
                    }
                }
                previousSelectedBlock = block?.designBlock
            }
        }
    }

    private fun observeClicked() {
        viewModelScope.launch {
            engine.block.onClicked().filter { isSceneLoaded.value }.collect { clickedBlock ->
                openPlaceholderSheetIfNeeded(clickedBlock)
            }
        }
    }

    private fun openPlaceholderSheetIfNeeded(designBlock: DesignBlock) {
        if (!engine.isPlaceholder(designBlock)) return
        val block = createBlock(designBlock, engine)
        val assetLibrary = requireNotNull(libraryViewModel.assetLibrary)
        val libraryCategory = when (block.type) {
            BlockType.Sticker -> assetLibrary.stickers()
            BlockType.Image -> assetLibrary.images()
            BlockType.Audio -> assetLibrary.audios()
            BlockType.Video -> assetLibrary.videos()
            // A page holds its replaceable media in its fill, not in the block type, so
            // resolve the library from the fill type (mirrors the graphic cases above).
            // Non-media fills (color/gradient/none) have nothing to replace, so no sheet.
            BlockType.Page -> when (engine.block.getFillType(designBlock)) {
                FillType.Image -> assetLibrary.images()
                FillType.Video -> assetLibrary.videos()
                else -> return
            }
            else -> return
        }
        send(EditorEvent.Sheet.Open(SheetType.LibraryReplace(libraryCategory = libraryCategory)))
    }

    private fun observeHistory() {
        viewModelScope.launch {
            engine.editor.onHistoryUpdatedWithKind().collect {
                _historyChangeTrigger.emit(Unit)
                timelineState?.onHistoryUpdated()
                updateVisiblePageState()
                updateBottomSheetUiState()
                // Invalidate the page-overview thumbnail cache on every history step.
                // Cache entries are keyed by page UUID only, so we can't tell which pages
                // a given history step touched — any step could have changed any page's
                // content. Clearing unconditionally matches the pre-cache behaviour
                // (always regenerate after an edit) while still letting the cache survive
                // pure navigation (pages-mode enter/exit), which is its main purpose.
                thumbnailCache.clear()
                if (_uiState.value.pagesState != null) {
                    updateEditorPagesState()
                }
                if (bottomSheetContent.value is CropBottomSheetContent) {
                    updateInsets(
                        bottomInset = max(bottomSheetHeight, bottomStaticContentHeight),
                        zoomToPage = getBlockForEvents()?.designBlock?.let { engine.block.getType(it) == DesignBlockType.Page.key } == true,
                    )
                }
            }
        }
    }

    private fun collectVideoDurationConstraintsData() = viewModelScope.launch {
        publicState
            .map { it.minVideoDuration to it.maxVideoDuration }
            .distinctUntilChanged()
            .collect { applyVideoDurationConstraintsToTimeline() }
    }

    private fun applyVideoDurationConstraintsToTimeline() {
        val minDuration = publicState.value.minVideoDuration?.takeIf { it > ZERO }
        val maxDuration = publicState.value.maxVideoDuration
            ?.takeIf { it > ZERO }
            ?.takeIf { minDuration == null || it >= minDuration }
        timelineState?.playerState?.maxPlaybackDuration = maxDuration
    }

    internal fun updateVoiceOverRecordingInProgress(value: Boolean) {
        isVoiceOverRecordingInProgress = value
    }

    internal fun updateVoiceOverSheetTarget(targetBlock: DesignBlock?) {
        voiceOverSheetTargetBlock = targetBlock
    }

    private fun onClose() {
        viewModelScope.launch {
            configuration.onClose?.invoke(editorScope)
        }
    }

    private fun onError(throwable: Throwable) {
        viewModelScope.launch {
            configuration.onError?.invoke(editorScope, throwable)
        }
    }

    private fun onBackPress(
        bottomSheetOffset: Float,
        bottomSheetMaxOffset: Float,
    ) {
        when {
            bottomSheetOffset < bottomSheetMaxOffset -> {
                send(EditorEvent.Sheet.Close(animate = true))
            }
            viewMode is EditorViewMode.Preview -> {
                setEditorViewMode(viewMode = EditorViewMode.Edit())
            }
            viewMode is EditorViewMode.Pages -> {
                setEditorViewMode(viewMode = EditorViewMode.Edit())
            }
            engine.editor.getSettingBoolean(keypath = FEATURE_PAGE_CAROUSEL_ENABLED).not() && pageIndex.value > 0 -> {
                setPage(pageIndex.value - 1)
            }
        }
    }

    private fun setEditMode(
        viewMode: EditorViewMode.Edit,
        deselectAllBlocks: Boolean = true,
    ) {
        val previousViewMode = this.viewMode
        if (viewMode != previousViewMode) {
            // Close any open bottom sheet
            setBottomSheetContent { null }
        }
        this.viewMode = viewMode
        _uiState.update { it.copy(pagesState = null) }
        // Cancel all thumb generation jobs
        onStopGenerateAllPageThumbnails()
        engine.editor.setGlobalScope(Scope.EditorSelect, initiallySetEditorSelectGlobalScope)
        if (deselectAllBlocks) {
            engine.deselectAllBlocks()
        }
        // Zoom to the target page BEFORE entering edit mode, so the camera position
        // is correct when the canvas renders its first frame. Without this, viewport
        // culling may show a blank page because the camera is still at the old position
        // and the target page isn't in the active set.
        if (engine.editor.getSettingBoolean(keypath = FEATURE_PAGE_CAROUSEL_ENABLED).not()) {
            showPage(pageIndex.value, deselectAllBlocks = deselectAllBlocks)
        }
        if (previousViewMode is EditorViewMode.Preview) {
            engine.scene.getPages().forEach { page ->
                engine.overrideAndRestore(page, Scope.LayerClipping) {
                    engine.block.setClipped(it, clipped = _uiState.value.isClippedInEditMode)
                }
            }
        }
    }

    @OptIn(UnstableEngineApi::class)
    private fun setPreviewMode(viewMode: EditorViewMode.Preview) {
        if (viewMode != this.viewMode) {
            // Close any open bottom sheet
            closeSheet(animate = false)
        }
        send(EditorEvent.Sheet.Close(animate = false))
        engine.deselectAllBlocks()
        _uiState.update {
            it.copy(isClippedInEditMode = engine.scene.getPages().any { page -> engine.block.isClipped(page) })
        }
        engine.scene.getPages().forEach { page ->
            engine.overrideAndRestore(page, Scope.LayerClipping) {
                engine.block.setClipped(it, clipped = true)
            }
        }
        engine.editor.setGlobalScope(Scope.EditorSelect, GlobalScope.DENY)
        this.viewMode = viewMode
        val scene = engine.getScene()
        if (engine.scene.isCameraPositionClampingEnabled(scene)) {
            engine.scene.disableCameraPositionClamping()
        }
        if (engine.scene.isCameraZoomClampingEnabled(scene)) {
            engine.scene.disableCameraZoomClamping()
        }
    }

    private fun setPagesMode(viewMode: EditorViewMode.Pages) {
        if (viewMode != this.viewMode) {
            // Close any open bottom sheet
            closeSheet(animate = false)
        }
        engine.deselectAllBlocks()
        this.viewMode = viewMode
        updateEditorPagesState()
    }

    private var exportJob: Job? = null

    private fun exportScene() {
        if (isExporting.compareAndSet(expect = false, update = true)) {
            timelineState?.playerState?.pause()
            viewModelScope.launch {
                exportJob = launch {
                    configuration.onExport?.invoke(editorScope)
                }
                exportJob?.join()
                isExporting.update { false }
            }
        }
    }

    private fun sendSingleEvent(event: SingleEvent) {
        viewModelScope.launch {
            _uiEvent.emit(event)
        }
    }

    override fun onCleared() {
        thumbnailCache.clear()
        engine.stop()
    }

    private fun onFontSizeChange(
        designBlock: DesignBlock,
        size: Float,
    ) {
        engine.block.setFloat(designBlock, "text/fontSize", size)
        engine.editor.addUndoStep()
    }

    private fun onTypefaceChange(
        designBlock: DesignBlock,
        typeface: Typeface,
    ) {
        engine.overrideAndRestore(
            designBlock = designBlock,
            "text/character",
        ) {
            engine.block.setTypeface(
                block = designBlock,
                typeface = typeface,
            )
        }
        engine.editor.addUndoStep()
    }

    private fun onColorChange(
        name: String,
        color: Color,
    ) {
        val engineColor = color.toEngineColor()
        engine.block.findByName(name).forEach {
            if (engine.block.supportsFill(it) && engine.block.isFillEnabled(it)) {
                engine.overrideAndRestore(it, Scope.FillChange) { block ->
                    engine.block.setFillType(block, FillType.Color)
                    engine.block.setFillSolidColor(block, engineColor)
                }
            }
            if (engine.block.supportsStroke(it) && engine.block.isStrokeEnabled(it)) {
                engine.overrideAndRestore(it, Scope.StrokeChange) { block ->
                    engine.block.setStrokeColor(block, engineColor)
                }
            }
        }
    }

    fun provideTimelineOwner(): TimelineOwner = timelineState ?: TimelineState(engine, viewModelScope, this@EditorUiViewModel::send).also {
        timelineState = it
        applyVideoDurationConstraintsToTimeline()
    }

    companion object {
        private const val MAX_THUMBNAIL_CACHE_SIZE = 50
    }
}
