package ly.img.editor.base.ui

import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Rect
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import ly.img.editor.base.dock.options.crop.CropBottomSheetContent
import ly.img.editor.base.dock.options.crop.createAllPageResizeUiState
import ly.img.editor.base.dock.options.crop.createCropUiState
import ly.img.editor.base.dock.options.effect.EffectUiState
import ly.img.editor.base.dock.options.fillstroke.FillStrokeUiState
import ly.img.editor.base.dock.options.format.createFormatUiState
import ly.img.editor.base.dock.options.layer.createLayerUiState
import ly.img.editor.base.dock.options.reorder.ReorderBottomSheetContent
import ly.img.editor.base.dock.options.shapeoptions.createShapeOptionsUiState
import ly.img.editor.base.dock.options.textBackground.TextBackgroundBottomSheetContent
import ly.img.editor.base.dock.options.textBackground.TextBackgroundUiState
import ly.img.editor.base.dock.options.volume.VolumeBottomSheetContent
import ly.img.editor.base.dock.options.volume.VolumeUiState
import ly.img.editor.base.engine.CROP_EDIT_MODE
import ly.img.editor.base.engine.TEXT_EDIT_MODE
import ly.img.editor.base.engine.TOUCH_ACTION_SCALE
import ly.img.editor.base.engine.TOUCH_ACTION_ZOOM
import ly.img.editor.base.engine.TRANSFORM_EDIT_MODE
import ly.img.editor.base.engine.delete
import ly.img.editor.base.engine.duplicate
import ly.img.editor.base.engine.isPlaceholder
import ly.img.editor.base.engine.resetHistory
import ly.img.editor.base.engine.setFillType
import ly.img.editor.base.engine.showPage
import ly.img.editor.base.engine.zoomToPage
import ly.img.editor.base.engine.zoomToSelectedText
import ly.img.editor.base.migration.EditorMigrationHelper
import ly.img.editor.base.sheet.LibraryAddToBackgroundTrackSheetType
import ly.img.editor.base.sheet.LibraryTabsSheetType
import ly.img.editor.base.timeline.state.TimelineState
import ly.img.editor.base.ui.handler.animationEvents
import ly.img.editor.base.ui.handler.appearanceEvents
import ly.img.editor.base.ui.handler.blockEvents
import ly.img.editor.base.ui.handler.blockFillEvents
import ly.img.editor.base.ui.handler.cropEvents
import ly.img.editor.base.ui.handler.shapeOptionEvents
import ly.img.editor.base.ui.handler.strokeEvents
import ly.img.editor.base.ui.handler.textBlockEvents
import ly.img.editor.base.ui.handler.timelineEvents
import ly.img.editor.base.ui.handler.volumeEvents
import ly.img.editor.compose.bottomsheet.ModalBottomSheetValue
import ly.img.editor.core.EditorContext
import ly.img.editor.core.EditorScope
import ly.img.editor.core.component.Dock
import ly.img.editor.core.component.rememberImglyCamera
import ly.img.editor.core.component.rememberSystemCamera
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.event.EditorEventHandler
import ly.img.editor.core.library.data.AssetSourceType
import ly.img.editor.core.library.data.UploadAssetSourceType
import ly.img.editor.core.sheet.SheetType
import ly.img.editor.core.state.EditorState
import ly.img.editor.core.state.EditorViewMode
import ly.img.editor.core.ui.EventsHandler
import ly.img.editor.core.ui.engine.BlockType
import ly.img.editor.core.ui.engine.Scope
import ly.img.editor.core.ui.engine.awaitEngineAndSceneLoad
import ly.img.editor.core.ui.engine.deselectAllBlocks
import ly.img.editor.core.ui.engine.dpToCanvasUnit
import ly.img.editor.core.ui.engine.getCamera
import ly.img.editor.core.ui.engine.getPage
import ly.img.editor.core.ui.engine.getScene
import ly.img.editor.core.ui.engine.isSceneModeVideo
import ly.img.editor.core.ui.engine.overrideAndRestore
import ly.img.editor.core.ui.library.AppearanceLibraryCategory
import ly.img.editor.core.ui.library.CropAssetSourceType
import ly.img.editor.core.ui.library.LibraryViewModel
import ly.img.editor.core.ui.library.util.LibraryEvent
import ly.img.editor.core.ui.register
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import ly.img.engine.FillType
import ly.img.engine.GlobalScope
import ly.img.engine.SceneMode
import ly.img.engine.UnstableEngineApi
import java.io.File
import kotlin.math.abs
import kotlin.math.max

abstract class EditorUiViewModel(
    private val onCreate: suspend EditorScope.() -> Unit,
    private val onExport: suspend EditorScope.() -> Unit,
    private val onClose: suspend EditorScope.(Boolean) -> Unit,
    private val onError: suspend EditorScope.(Throwable) -> Unit,
    private val libraryViewModel: LibraryViewModel,
) : ViewModel(),
    EditorEventHandler {
    private val migrationHelper = EditorMigrationHelper()
    private lateinit var editorScope: EditorScope
    val editor: EditorContext
        get() = editorScope.run { editorContext }
    val engine: Engine
        get() = editor.engine
    protected var timelineState: TimelineState? = null

    private var firstLoad = true
    private var uiInsets = Rect.Zero
    protected val defaultInsets: Rect
        get() {
            return uiInsets.translate(horizontalPageInset, verticalPageInset)
        }

    private var canvasHeight = 0f

    private var isStraighteningOrRotating = false
    private var initCropTranslationX = 0f
    private var initCropTranslationY = 0f
    private var isCropReset = false

    private val isExporting = MutableStateFlow(false)

    private val selectedBlock = MutableStateFlow<Block?>(null)
    private val isKeyboardShowing = MutableStateFlow(false)
    protected val pageIndex = MutableStateFlow(0)
    private val isZoomedIn = MutableStateFlow(false)

    @Suppress("ktlint:standard:backing-property-naming")
    private val _isSceneLoaded = MutableStateFlow(false)
    protected val isSceneLoaded: StateFlow<Boolean> = _isSceneLoaded

    protected var inPortraitMode = true

    @Suppress("ktlint:standard:backing-property-naming")
    private val _uiState = MutableStateFlow(EditorUiViewState())
    protected val baseUiState: StateFlow<EditorUiViewState> = _uiState

    private val _publicState = MutableStateFlow(EditorState())
    val publicState: StateFlow<EditorState> = _publicState

    private val _uiEvent = Channel<SingleEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val externalEventChannel = Channel<EditorEvent>()
    val externalEvent = externalEventChannel.receiveAsFlow()

    private var bottomSheetHeight: Float = 0F
    private var timelineHeight: Float = 0F
    private var timelineFullHeight: Float = 0F
    private var closingSheetContent: BottomSheetContent? = null
    private val _bottomSheetContent = MutableStateFlow<BottomSheetContent?>(null)
    val bottomSheetContent = _bottomSheetContent.asStateFlow()

    @Suppress("ktlint:standard:backing-property-naming")
    private val _historyChangeTrigger = MutableSharedFlow<Unit>()
    protected val historyChangeTrigger: SharedFlow<Unit> = _historyChangeTrigger

    private val thumbnailGenerationJobs: MutableMap<DesignBlock, Job?> = mutableMapOf()
    private var pagesSessionId = 0

    protected open val defaultCropSheetType = SheetType.Crop(mode = SheetType.Crop.Mode.Element)
    private var currentCropSheetType: SheetType.Crop? = null

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
                zoom(max(bottomSheetHeight, timelineHeight))
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
                sendSingleEvent(SingleEvent.Error(it))
            },
        )
        editorEvents()
        extraEvents()
        animationEvents(
            engine = ::engine,
        )
    }

    protected open fun EventsHandler.extraEvents() = Unit

    private fun EventsHandler.editorEvents() {
        register<Event.OnError> { onError(it.throwable) }
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
            if (_isSceneLoaded.value && engine.editor.getEditMode() == TRANSFORM_EDIT_MODE) {
                zoom(zoomToPage = true)
            }
        }
        register<Event.OnKeyboardClose> { onKeyboardClose() }
        register<Event.OnLoadScene> { loadScene(it.height, it.insets, it.inPortraitMode) }
        register<Event.EnableHistory> { event -> _publicState.update { it.copy(isHistoryEnabled = event.enable) } }
        register<Event.OnBottomSheetHeightChange> { onBottomSheetHeightChange(it.sheetHeightInDp, it.sheetMaxHeightInDp) }
        register<Event.OnTimelineHeightChange> { onTimelineHeightChange(it.timelineHeightInDp) }
        register<Event.OnKeyboardHeightChange> { onKeyboardHeightChange(it.heightInDp) }
        register<Event.OnPause> { if (engine.isSceneModeVideo) timelineState?.playerState?.pause() }
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

        /** Customer exposed internal events **/
        register<EditorEvent.OnClose> { onClose() }
        register<EditorEvent.CloseEditor> { sendSingleEvent(SingleEvent.Exit(it.throwable)) }
        register<EditorEvent.Navigation.ToNextPage> { setPage(pageIndex.value + 1) }
        register<EditorEvent.Navigation.ToPreviousPage> { setPage(pageIndex.value - 1) }
        register<EditorEvent.Export.Start> { exportScene() }
        register<EditorEvent.Export.Cancel> { exportJob?.cancel() }
        @Suppress("DEPRECATION")
        register<EditorEvent.CancelExport> { exportJob?.cancel() }
        register<EditorEvent.SetViewMode> { setViewMode(it.viewMode) }
        register<EditorEvent.Sheet.Open> { openSheet(it.type) }
        register<EditorEvent.Sheet.Expand> {
            sendSingleEvent(SingleEvent.ChangeSheetState(ModalBottomSheetValue.Expanded, it.animate))
        }
        register<EditorEvent.Sheet.HalfExpand> {
            sendSingleEvent(SingleEvent.ChangeSheetState(ModalBottomSheetValue.HalfExpanded, it.animate))
        }
        register<EditorEvent.Sheet.Close> { closeSheet(animate = it.animate) }
        register<EditorEvent.Sheet.OnExpanded> { /* do nothing */ }
        register<EditorEvent.Sheet.OnHalfExpanded> { /* do nothing */ }
        register<EditorEvent.Sheet.OnClosed> { onSheetClosed() }
        register<EditorEvent.LaunchContract<*, *>> { _uiState.update { state -> state.copy(openContract = it) } }
        register<EditorEvent.AddUriToScene> {
            // This event is triggered only from dock for now.
            // addToBackgroundTrack is always true for now as we do not want to expose it to customers due to unknown future.
            libraryViewModel.onEvent(
                LibraryEvent.OnAddUri(it.uploadAssetSourceType, it.uri, addToBackgroundTrack = true),
            )
        }
        register<EditorEvent.ReplaceUriAtScene> {
            libraryViewModel.onEvent(
                LibraryEvent.OnReplaceUri(it.uploadAssetSourceType, it.uri, it.designBlock),
            )
        }
        register<EditorEvent.AddCameraRecordingsToScene> {
            libraryViewModel.onEvent(LibraryEvent.OnAddCameraRecordings(it.uploadAssetSourceType, it.recordings))
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
            getBlockForEvents()?.designBlock?.let(engine::delete)
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
    }

    private fun closeSheet(animate: Boolean) {
        if (animate) {
            sendSingleEvent(SingleEvent.ChangeSheetState(ModalBottomSheetValue.Hidden, animate = true))
        } else {
            bottomSheetContent.value?.type?.let { send(EditorEvent.Sheet.OnClosed(it)) }
        }
    }

    private fun setViewMode(viewMode: EditorViewMode) {
        when (viewMode) {
            is EditorViewMode.Preview -> setPreviewMode(viewMode)
            is EditorViewMode.Pages -> setPagesMode(viewMode)
            is EditorViewMode.Edit -> setEditMode(viewMode)
        }
    }

    protected open fun openSheet(type: SheetType) {
        val eventBlock = getBlockForEvents()
        val hasEventBlock = eventBlock != null
        val block by lazy { requireNotNull(eventBlock) }
        val designBlock by lazy {
            block.designBlock
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
                // Cannot be invoked by customers
                is LibraryTabsSheetType -> {
                    LibraryTabsBottomSheetContent(
                        type = type,
                    )
                }
                // This sheet triggered only from dock for now.
                // addToBackgroundTrack is always false for now as we do not want to expose it to customers due to unknown future.
                is SheetType.LibraryAdd -> {
                    LibraryAddBottomSheetContent(
                        type = type,
                        libraryCategory = type.libraryCategory,
                        addToBackgroundTrack = false,
                    )
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
                        uiState = FillStrokeUiState.create(block, engine, editor.colorPalette),
                    )
                }

                is SheetType.ResizeAll -> {
                    currentCropSheetType = type
                    CropBottomSheetContent(
                        type = type,
                        uiState = createAllPageResizeUiState(
                            engine = engine,
                            initCropTranslationX = initCropTranslationX,
                            initCropTranslationY = initCropTranslationY,
                            pageAssetSourceIds = CropAssetSourceType.Page.sourceId,
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
                            CropBottomSheetContent(
                                type = type,
                                uiState = createCropUiState(
                                    page,
                                    engine,
                                    cropMode = type.mode,
                                    pageAssetSourceId = CropAssetSourceType.Page.sourceId,
                                    cropAssetSourceId = CropAssetSourceType.Crop.sourceId,
                                    initCropTranslationX = initCropTranslationX,
                                    initCropTranslationY = initCropTranslationY,
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
                        uiState = AnimationUiState.create(designBlock, engine),
                    )
                }
                is SheetType.TextBackground -> {
                    timelineState?.clampPlayheadPositionToSelectedClip()
                    TextBackgroundBottomSheetContent(
                        type = type,
                        uiState = TextBackgroundUiState.create(designBlock, engine, editor.colorPalette),
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
        val pages = engine.scene.getPages()
        val page = engine.scene.getCurrentPage()
        val currentVisible = pages.indexOfFirst { page == it }
        if (pageIndex.value != currentVisible) {
            setPage(index = currentVisible)
        }
    }

    final override fun send(event: EditorEvent) {
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
            viewModelScope.launch {
                externalEventChannel.send(event)
            }
        }
    }

    open fun getBlockForEvents(): Block? = selectedBlock.value

    private fun requireDesignBlockForEvents(): DesignBlock = checkNotNull(getBlockForEvents()?.designBlock)

    protected open fun getSelectedBlock(): DesignBlock? = engine.block.findAllSelected().firstOrNull()

    protected open fun setSelectedBlock(block: Block?) {
        selectedBlock.update { block }
    }

    protected open fun onCanvasMove(move: Boolean) {
        _publicState.update { it.copy(isTouchActive = move) }
    }

    protected open fun hasUnsavedChanges(): Boolean = engine.editor.canUndo()

    protected open fun updateBottomSheetUiState() {
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
                    CropBottomSheetContent(
                        type = content.type,
                        uiState = createCropUiState(
                            engine.getScene(),
                            engine,
                            initCropTranslationX,
                            initCropTranslationY,
                            if (useOldScaleRatio) content.uiState.cropScaleRatio else null,
                            cropMode = currentCropSheetType.mode,
                            pageAssetSourceId = CropAssetSourceType.Page.sourceId,
                            cropAssetSourceId = CropAssetSourceType.Crop.sourceId,
                        ),
                    )
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
                            uiState = FillStrokeUiState.create(block, engine, editor.colorPalette),
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
                        CropBottomSheetContent(
                            type = content.type,
                            uiState = createCropUiState(
                                designBlock,
                                engine,
                                initCropTranslationX,
                                initCropTranslationY,
                                if (useOldScaleRatio) content.uiState.cropScaleRatio else null,
                                cropMode = currentCropSheetType.mode,
                                pageAssetSourceId = CropAssetSourceType.Page.sourceId,
                                cropAssetSourceId = CropAssetSourceType.Crop.sourceId,
                            ),
                        )
                    }

                    is VolumeBottomSheetContent -> {
                        VolumeBottomSheetContent(
                            type = content.type,
                            uiState = VolumeUiState.create(designBlock, engine),
                        )
                    }

                    is AnimationBottomSheetContent -> {
                        AnimationBottomSheetContent(
                            type = content.type,
                            uiState = AnimationUiState.create(designBlock, engine),
                        )
                    }

                    is TextBackgroundBottomSheetContent -> {
                        TextBackgroundBottomSheetContent(
                            type = content.type,
                            uiState = TextBackgroundUiState.create(designBlock, engine, editor.colorPalette),
                        )
                    }

                    else -> {
                        content
                    }
                }
            }
        }
    }

    protected open fun showPage(index: Int) {
        engine.showPage(index)
    }

    protected open fun setPage(index: Int) {
        if (index == pageIndex.value) return
        pageIndex.update { index }
        showPage(index)
        send(EditorEvent.Sheet.Close(animate = false))
    }

    protected fun setPageIndex(index: Int) {
        if (index == pageIndex.value) return
        pageIndex.update { index }
    }

    protected open fun handleBackPress(
        bottomSheetOffset: Float,
        bottomSheetMaxOffset: Float,
    ): Boolean = when {
        bottomSheetOffset < bottomSheetMaxOffset -> {
            send(EditorEvent.Sheet.Close(animate = true))
            true
        }
        publicState.value.viewMode is EditorViewMode.Preview -> {
            setViewMode(viewMode = EditorViewMode.Edit())
            true
        }
        publicState.value.viewMode is EditorViewMode.Pages -> {
            setViewMode(viewMode = EditorViewMode.Edit())
            true
        }
        else -> false
    }

    protected fun setBottomSheetContent(function: suspend (BottomSheetContent?) -> BottomSheetContent?) = viewModelScope.launch {
        val oldBottomSheetContent = bottomSheetContent.value
        val newValue = function(oldBottomSheetContent)
        if (newValue == null && oldBottomSheetContent != null && bottomSheetHeight > 0F) {
            // Means it is closing
            currentCropSheetType = null
            closingSheetContent = _bottomSheetContent.value
        }
        _bottomSheetContent.value = newValue
        _publicState.update { it.copy(activeSheet = newValue?.type) }
    }

    private fun onSheetClosed() {
        currentCropSheetType = null
        setBottomSheetContent { null }
        if (engine.isEngineRunning().not()) return
        if (engine.editor.getEditMode() == CROP_EDIT_MODE) {
            engine.editor.setEditMode(TRANSFORM_EDIT_MODE)
            zoom(bottomInset = 0F)
        }
    }

    private fun onZoomFinish() {
        updateZoomState()
    }

    protected open val horizontalPageInset: Float = DEFAULT_PAGE_INSET

    protected open val verticalPageInset: Float = DEFAULT_PAGE_INSET

    private var initiallySetEditorSelectGlobalScope = GlobalScope.DEFER

    private fun loadScene(
        height: Float,
        insets: Rect,
        inPortraitMode: Boolean,
    ) {
        uiInsets = insets
        canvasHeight = height
        this.inPortraitMode = inPortraitMode
        val isConfigChange = !firstLoad
        firstLoad = false
        if (isConfigChange) {
            setViewMode(publicState.value.viewMode)
        } else {
            if (engine.scene.get() == null) {
                viewModelScope.launch {
                    engine.scene
                        .onActiveChanged()
                        .onEach { onSceneLoaded() }
                        .first()
                }
            }
            viewModelScope.launch {
                runCatching {
                    migrationHelper.migrate()
                    onPreCreate()
                    val isSceneRestorationFlow = engine.scene.get() != null
                    // Make sure to set all settings before calling `onCreate` so that the consumer can change them if needed!
                    onCreate(editorScope)
                    // Invoke onSceneLoaded only when engine was restored, because in the regular flow
                    // we have another coroutine that observes scene change and invokes this funcion.
                    if (isSceneRestorationFlow) {
                        onSceneLoaded()
                    }
                    initiallySetEditorSelectGlobalScope = engine.editor.getGlobalScope(Scope.EditorSelect)
                    requireNotNull(engine.scene.get()) { "onCreate body must contain scene creation." }
                    if (engine.isSceneModeVideo) {
                        timelineState = TimelineState(engine, viewModelScope)
                    }
                    showPage(pageIndex.value)
                    setEditMode(EditorViewMode.Edit()).join()
                    engine.resetHistory()
                }.onSuccess {
                    _isSceneLoaded.update { true }
                }.onFailure {
                    onError(it)
                }
            }

            observeSelectedBlock()
            observeHistory()
            observeUiStateChanges()
            observeEvents()
            observeEditorStateChange()
        }
    }

    private fun onSystemCameraClick(
        captureVideo: Boolean,
        designBlock: DesignBlock?,
        addToBackgroundTrack: Boolean,
    ) {
        val context = editor.activity
        val uri = File.createTempFile("imgly_", null, context.filesDir).let {
            FileProvider.getUriForFile(context, "${context.packageName}.ly.img.editor.fileprovider", it)
        }
        val launchContract = if (captureVideo) {
            ActivityResultContracts.CaptureVideo()
        } else {
            ActivityResultContracts.TakePicture()
        }
        EditorEvent.LaunchContract(launchContract, uri) { success ->
            if (success) {
                val assetSource = if (captureVideo) AssetSourceType.VideoUploads else AssetSourceType.ImageUploads
                val event = designBlock?.let {
                    LibraryEvent.OnReplaceUri(
                        uri = uri,
                        assetSource = assetSource,
                        designBlock = designBlock,
                    )
                } ?: LibraryEvent.OnAddUri(
                    assetSource = assetSource,
                    uri = uri,
                    addToBackgroundTrack = addToBackgroundTrack,
                )
                // IMPORTANT! we cannot invoke simply invoke it on this.libraryViewModel as it's the previous instance and it will result to a crash!
                // + we do not want to capture anything from previous instance to allow it GC.
                (editorContext.eventHandler as EditorUiViewModel).libraryViewModel.onEvent(event)
                editorContext.eventHandler.send(EditorEvent.Sheet.Close(animate = true))
            }
        }.let(::send)
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
                // IMPORTANT! we cannot invoke simply invoke it on this.libraryViewModel as it's the previous instance and it will result to a crash!
                // + we do not want to capture anything from previous instance to allow it GC.
                (editorContext.eventHandler as EditorUiViewModel).libraryViewModel.onEvent(event)
                editorContext.eventHandler.send(EditorEvent.Sheet.Close(animate = true))
            }
        }.let(::send)
    }

    private fun onVideoCameraClick(callback: (@Composable () -> Unit) -> Unit) = callback {
        // If imgly camera is missing, then use system camera.
        runCatching {
            Dock.Button.rememberImglyCamera()
        }.getOrElse {
            Dock.Button.rememberSystemCamera()
        }.onClick(Dock.ButtonScope(editorScope))
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

    private fun onBottomSheetHeightChange(
        heightInDp: Float,
        sheetMaxHeightInDp: Float,
    ) {
        if (publicState.value.viewMode !is EditorViewMode.Edit || !_isSceneLoaded.value) return
        val closingSheetContent = this.closingSheetContent
        if (heightInDp == 0F && closingSheetContent != null) {
            this.closingSheetContent = null
        }
        val bottomSheetContent = bottomSheetContent.value

        // we don't want to change zoom level for floating sheets
        if (bottomSheetContent?.isFloating == true) return
        if (closingSheetContent?.isFloating == true) return

        bottomSheetHeight = heightInDp
        if (timelineFullHeight > sheetMaxHeightInDp && bottomSheetContent != null) {
            _uiState.update {
                it.copy(
                    timelineMaxHeightInDp =
                        sheetMaxHeightInDp + ((timelineFullHeight - sheetMaxHeightInDp) * (1 - heightInDp / sheetMaxHeightInDp)),
                )
            }
        } else if (engine.scene.getZoomLevel() == fitToPageZoomLevel) {
            _uiState.update {
                it.copy(timelineMaxHeightInDp = Float.MAX_VALUE)
            }
            zoom(max(heightInDp, timelineHeight))
        }
    }

    private fun onTimelineHeightChange(timelineHeight: Float) {
        if (engine.scene.getMode() == SceneMode.DESIGN) {
            return
        }
        this.timelineHeight = when {
            this.bottomSheetContent.value != null -> timelineHeight
            this.closingSheetContent == null -> timelineHeight.also { this.timelineFullHeight = it }
            else -> timelineFullHeight
        }
        if (engine.scene.getZoomLevel() == fitToPageZoomLevel) {
            zoom(max(bottomSheetHeight, timelineHeight))
        }
    }

    private fun onKeyboardClose() {
        engine.editor.setEditMode(TRANSFORM_EDIT_MODE)
        zoom(bottomInset = 0F)
    }

    private fun onKeyboardHeightChange(heightInDp: Float) {
        zoom(heightInDp)
    }

    protected open fun onSceneLoaded() {
        engine.deselectAllBlocks()
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
            val pagesState = it.pagesState
                ?.copy(engine, markThumbnails = true)
                ?: createEditorPagesState(pagesSessionId++, engine, pageIndex.value)
            setPage(pagesState.selectedPageIndex)
            it.copy(pagesState = pagesState)
        }
    }

    private fun onPagesSelectionChange(page: EditorPagesState.Page) {
        _uiState.update {
            if (page == it.pagesState?.selectedPage) {
                it
            } else {
                it.copy(pagesState = it.pagesState?.copy(engine, selectedPage = page, markThumbnails = false))
            }
        }
    }

    private fun onPagesModePageBind(
        page: EditorPagesState.Page,
        pageHeight: Int,
    ) {
        if (page.mark.not()) return
        if (pageHeight <= 0) return
        thumbnailGenerationJobs[page.block] = viewModelScope.launch {
            val result = runCatching {
                engine.block
                    .generateVideoThumbnailSequence(
                        block = page.block,
                        thumbnailHeight = pageHeight,
                        timeBegin = 0.0,
                        timeEnd = 0.1,
                        numberOfFrames = 1,
                    ).firstOrNull()
            }.getOrNull() ?: return@launch
            val bitmap = withContext(Dispatchers.Default) {
                createBitmap(result.width, result.height).also {
                    it.copyPixelsFromBuffer(result.imageData)
                }
            }
            val newPage = page.copy(mark = false).also {
                it.thumbnail = bitmap
            }
            _uiState.update {
                it.copy(pagesState = it.pagesState?.copy(updatedPage = newPage))
            }
        }
    }

    private fun onStopGenerateAllPageThumbnails() {
        val iterator = thumbnailGenerationJobs.iterator()
        while (iterator.hasNext()) {
            iterator.next().value?.cancel()
            iterator.remove()
        }
    }

    private fun zoom(bottomInset: Float) {
        val realBottomInset = bottomInset + verticalPageInset
        if (realBottomInset <= defaultInsets.bottom && _publicState.value.canvasInsets.bottom == defaultInsets.bottom) return
        val coercedBottomInset = defaultInsets.bottom
        zoom(defaultInsets.copy(bottom = realBottomInset.coerceAtLeast(coercedBottomInset)))
    }

    private var zoomJob: Job? = null
    private var fitToPageZoomLevel = 0f

    @OptIn(UnstableEngineApi::class)
    protected fun zoom(
        insets: Rect = defaultInsets,
        zoomToPage: Boolean = false,
        clampOnly: Boolean = false,
    ): Job {
        zoomJob?.cancel()
        _publicState.update { it.copy(canvasInsets = insets) }
        return viewModelScope
            .launch {
                if (_uiState.value.isInPreviewMode) {
                    preEnterPreviewMode()
                    enterPreviewMode()
                } else {
                    val page = engine.getPage(pageIndex.value)
                    val selectedBlock = selectedBlock.value
                    val shouldZoomToPage = engine.isSceneModeVideo ||
                        (!clampOnly && (zoomToPage || selectedBlock == null)) ||
                        engine.scene.getZoomLevel() == fitToPageZoomLevel
                    val blocks = buildList {
                        add(page)
                        if (engine.editor.getEditMode() == TEXT_EDIT_MODE && selectedBlock?.type == BlockType.Text) {
                            add(selectedBlock.designBlock)
                        }
                    }
                    val currentInsets = _publicState.value.canvasInsets
                    engine.scene.enableCameraPositionClamping(
                        blocks = blocks,
                        paddingLeft = currentInsets.left - horizontalPageInset,
                        paddingTop = currentInsets.top - verticalPageInset,
                        paddingRight = currentInsets.right - horizontalPageInset,
                        paddingBottom = currentInsets.bottom - verticalPageInset,
                        scaledPaddingLeft = horizontalPageInset,
                        scaledPaddingTop = verticalPageInset,
                        scaledPaddingRight = horizontalPageInset,
                        scaledPaddingBottom = verticalPageInset,
                    )

                    if (shouldZoomToPage) {
                        engine.scene.enableCameraZoomClamping(
                            blocks = blocks,
                            minZoomLimit = 1.0F,
                            maxZoomLimit = 5.0F,
                            paddingLeft = currentInsets.left,
                            paddingTop = currentInsets.top,
                            paddingRight = currentInsets.right,
                            paddingBottom = currentInsets.bottom,
                        )
                        engine.zoomToPage(pageIndex.value, currentInsets)
                        fitToPageZoomLevel = engine.scene.getZoomLevel()
                    }

                    val selectedDesignBlock = selectedBlock?.designBlock

                    if (selectedDesignBlock != null && !shouldZoomToPage && engine.editor.getEditMode() != TEXT_EDIT_MODE) {
                        // The delay acts as a debouncing mechanism.
                        delay(8)

                        val boundingBoxRect = engine.block.getScreenSpaceBoundingBoxRect(listOf(selectedDesignBlock))
                        val bottomSheetTop = canvasHeight - _publicState.value.canvasInsets.bottom
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
                    if (engine.editor.getEditMode() == TEXT_EDIT_MODE) {
                        zoomToText()
                    }
                }
                onZoomFinish()
            }.also {
                zoomJob = it
            }
    }

    private fun zoomToText() {
        engine.zoomToSelectedText(
            insets = _publicState.value.canvasInsets,
            canvasHeight = canvasHeight,
        )
    }

    private fun observeUiStateChanges() {
        viewModelScope.launch {
            merge(
                historyChangeTrigger,
                _isSceneLoaded,
                publicState,
                isExporting,
                selectedBlock,
                isKeyboardShowing,
            ).collect {
                val pageCount = if (_isSceneLoaded.value) engine.scene.getPages().size else 0
                val viewMode = publicState.value.viewMode
                _uiState.update {
                    it.copy(
                        isInPreviewMode = viewMode is EditorViewMode.Preview,
                        allowEditorInteraction = viewMode is EditorViewMode.Edit,
                        selectedBlock = selectedBlock.value,
                        isEditingText = isKeyboardShowing.value,
                        timelineState = timelineState,
                        pageCount = pageCount,
                        isSceneLoaded = _isSceneLoaded.value,
                    )
                }
            }
        }
    }

    private fun updateZoomState() {
        val newZoomState = _isSceneLoaded.value && (abs(engine.scene.getZoomLevel() - fitToPageZoomLevel) > 0.001f)
        if (newZoomState != isZoomedIn.value) {
            isZoomedIn.update { newZoomState }
        }
    }

    private var cursorPos = 0f

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
            }
        }
    }

    protected open fun onEditModeChanged(editMode: String) = Unit

    private fun observeEditorStateChange() {
        var flag = false
        viewModelScope.launch {
            engine.editor.onStateChanged().map { engine.editor.getEditMode() }.distinctUntilChanged().collect { editMode ->
                onEditModeChanged(editMode)
                when (editMode) {
                    TEXT_EDIT_MODE -> send(EditorEvent.Sheet.Close(animate = false))
                    CROP_EDIT_MODE -> {
                        val block = engine.block.findAllSelected().single()
                        engine.editor.setSettingEnum("touch/pinchAction", TOUCH_ACTION_SCALE)
                        setInitCropValues(block)
                        // no need to send EditorEvent.Sheet.Open here
                        setBottomSheetContent {
                            /**
                             * Think about making default crop type configurable by customer.
                             */
                            val type = currentCropSheetType ?: defaultCropSheetType
                            val selection = engine.block.findAllSelected().single()
                            CropBottomSheetContent(
                                type = type,
                                uiState = createCropUiState(
                                    selection,
                                    engine,
                                    cropMode = type.mode,
                                    pageAssetSourceId = CropAssetSourceType.Page.sourceId,
                                    cropAssetSourceId = CropAssetSourceType.Crop.sourceId,
                                    initCropTranslationX = initCropTranslationX,
                                    initCropTranslationY = initCropTranslationY,
                                ),
                            )
                        }
                    }

                    else -> {
                        // Close crop bottom sheet if coming back from crop mode
                        if (bottomSheetContent.value is CropBottomSheetContent) {
                            send(EditorEvent.Sheet.Close(animate = false))
                        }

                        // restore pinchAction, for video scenes, it is already set to scale
                        if (!engine.isSceneModeVideo) {
                            engine.editor.setSettingEnum("touch/pinchAction", TOUCH_ACTION_ZOOM)
                        }

                        if (!flag) {
                            flag = true
                            zoom(zoomToPage = true)
                        }
                    }
                }
                val showKeyboard = editMode == TEXT_EDIT_MODE
                if (isKeyboardShowing.value && showKeyboard.not()) {
                    zoom(bottomInset = 0F)
                }
                isKeyboardShowing.update { showKeyboard }
            }
        }
    }

    private fun setInitCropValues(block: DesignBlock) {
        isStraighteningOrRotating = false
        isCropReset = false
        initCropTranslationX = engine.block.getCropTranslationX(block)
        initCropTranslationY = engine.block.getCropTranslationY(block)
    }

    private fun observeSelectedBlock() {
        viewModelScope.launch {
            // filter is added, because page dock becomes visible while postcard UI is still loading
            merge(engine.block.onSelectionChanged()).filter { _isSceneLoaded.value }.collect {
                val block = getSelectedBlock()?.let { createBlock(it, engine) }
                val oldBlock = getBlockForEvents()?.designBlock
                // Even if the block is the same, this will update the fill/stroke color in the dock option
                setSelectedBlock(block)
                if (oldBlock != block?.designBlock) {
                    if (block != null && engine.isPlaceholder(block.designBlock)) {
                        val libraryCategory = when (block.type) {
                            BlockType.Sticker -> libraryViewModel.assetLibrary.stickers(libraryViewModel.sceneMode)
                            BlockType.Image -> libraryViewModel.assetLibrary.images(libraryViewModel.sceneMode)
                            BlockType.Audio -> libraryViewModel.assetLibrary.audios(libraryViewModel.sceneMode)
                            BlockType.Video -> libraryViewModel.assetLibrary.videos(libraryViewModel.sceneMode)
                            else -> throw IllegalArgumentException(
                                "Replace is not supported for ${block.type.name}.",
                            )
                        }
                        send(EditorEvent.Sheet.Open(SheetType.LibraryReplace(libraryCategory = libraryCategory)))
                    } else if (block == null || bottomSheetContent.value != null) {
                        send(EditorEvent.Sheet.Close(animate = false))
                    }
                }
            }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            engine.editor.onHistoryUpdated().collect {
                _historyChangeTrigger.emit(Unit)
                timelineState?.onHistoryUpdated()
                updateVisiblePageState()
                updateBottomSheetUiState()
                if (_uiState.value.pagesState != null) {
                    updateEditorPagesState()
                }
                if (bottomSheetContent.value is CropBottomSheetContent) {
                    zoom(max(bottomSheetHeight, timelineHeight))
                }
            }
        }
    }

    private fun onClose() {
        viewModelScope.launch {
            onClose(editorScope, hasUnsavedChanges())
        }
    }

    private fun onError(throwable: Throwable) {
        viewModelScope.launch {
            onError(editorScope, throwable)
        }
    }

    private fun onBackPress(
        bottomSheetOffset: Float,
        bottomSheetMaxOffset: Float,
    ) {
        if (!handleBackPress(bottomSheetOffset = bottomSheetOffset, bottomSheetMaxOffset = bottomSheetMaxOffset)) {
            onClose()
        }
    }

    private fun setEditMode(viewMode: EditorViewMode.Edit): Job {
        if (viewMode != publicState.value.viewMode) {
            // Close any open bottom sheet
            setBottomSheetContent { null }
        }
        _publicState.update { state -> state.copy(viewMode = viewMode) }
        _uiState.update { it.copy(pagesState = null) }
        // Cancel all thumb generation jobs
        onStopGenerateAllPageThumbnails()
        engine.editor.setGlobalScope(Scope.EditorSelect, initiallySetEditorSelectGlobalScope)
        enterEditMode()
        return zoom(zoomToPage = true)
    }

    private fun setPreviewMode(viewMode: EditorViewMode.Preview) {
        if (viewMode != publicState.value.viewMode) {
            // Close any open bottom sheet
            setBottomSheetContent { null }
        }
        _publicState.update { state -> state.copy(viewMode = viewMode) }
        send(EditorEvent.Sheet.Close(animate = false))
        engine.editor.setGlobalScope(Scope.EditorSelect, GlobalScope.DENY)
        enterPreviewMode()
        zoom(defaultInsets.copy(bottom = verticalPageInset))
    }

    private fun setPagesMode(viewMode: EditorViewMode.Pages) {
        if (viewMode != publicState.value.viewMode) {
            // Close any open bottom sheet
            setBottomSheetContent { null }
        }
        _publicState.update { state -> state.copy(viewMode = viewMode) }
        engine.deselectAllBlocks()
        updateEditorPagesState()
    }

    private var exportJob: Job? = null

    private fun exportScene() {
        if (isExporting.compareAndSet(expect = false, update = true)) {
            timelineState?.playerState?.pause()
            viewModelScope.launch {
                exportJob = launch {
                    onExport(editorScope)
                }
                exportJob?.join()
                isExporting.update { false }
            }
        }
    }

    private fun sendSingleEvent(event: SingleEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }

    override fun onCleared() {
        engine.stop()
    }

    abstract fun enterEditMode()

    abstract fun enterPreviewMode()

    @OptIn(UnstableEngineApi::class)
    open fun preEnterPreviewMode() {
        val scene = engine.getScene()
        if (engine.scene.isCameraPositionClampingEnabled(scene)) {
            engine.scene.disableCameraPositionClamping()
        }
        if (engine.scene.isCameraZoomClampingEnabled(scene)) {
            engine.scene.disableCameraZoomClamping()
        }
    }

    open fun onPreCreate() {
        setSettingsForEditorUi(engine, editor.baseUri)
    }

    fun setEditorScope(editorScope: EditorScope) {
        this.editorScope = editorScope
    }

    private companion object {
        const val DEFAULT_PAGE_INSET = 16F
    }
}
