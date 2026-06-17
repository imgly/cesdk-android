package ly.img.camera

import android.app.Application
import android.util.Size
import androidx.camera.core.MirrorMode.MIRROR_MODE_ON_FRONT_ONLY
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ly.img.camera.components.sidemenu.layout.LayoutMode
import ly.img.camera.core.CameraConfiguration
import ly.img.camera.core.CameraLayoutMode
import ly.img.camera.core.CameraMode
import ly.img.camera.core.CameraResult
import ly.img.camera.core.CaptureVideo
import ly.img.camera.core.EngineConfiguration
import ly.img.camera.core.Video
import ly.img.camera.preview.CameraState
import ly.img.camera.preview.LayoutState
import ly.img.camera.record.RecordingManager
import ly.img.camera.record.VideoRecorder
import ly.img.camera.util.SingleEvent
import ly.img.editor.core.ui.engine.Scope
import ly.img.editor.core.ui.library.engine.setFrame
import ly.img.engine.Color
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import ly.img.engine.FillType
import ly.img.engine.GlobalScope
import ly.img.engine.ShapeType
import ly.img.engine.UnstableEngineApi
import ly.img.engine.camera.setCameraPreview
import java.io.File
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

internal class CameraViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle,
) : AndroidViewModel(application) {
    @OptIn(UnstableEngineApi::class)
    val engine = Engine.getInstance("ly.img.camera").also {
        it.idlingEnabled = true
    }

    private val cameraInput = savedStateHandle.get<CaptureVideo.Input>(CaptureVideo.INTENT_KEY_CAMERA_INPUT)
    val engineConfiguration: EngineConfiguration? = cameraInput?.engineConfiguration
    val cameraConfiguration: CameraConfiguration = cameraInput?.cameraConfiguration ?: CameraConfiguration()
    val cameraMode: CameraMode = cameraInput?.cameraMode ?: CameraMode.Standard()

    // Deferred filesDir to avoid StrictMode disk read violation during recording.
    // Completed on IO thread in init block, awaited when recording starts.
    private val filesDirDeferred = CompletableDeferred<File>()

    private var reactionVideoIsPlaying = false

    private val previewBuilder = Preview
        .Builder()
        .setResolutionSelector(
            ResolutionSelector
                .Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        Size(
                            cameraConfiguration.videoSize.width.toInt(),
                            cameraConfiguration.videoSize.height.toInt(),
                        ),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_HIGHER_THEN_LOWER,
                    ),
                ).setAspectRatioStrategy(AspectRatioStrategy.RATIO_16_9_FALLBACK_AUTO_STRATEGY)
                .build(),
        )

    private val videoCaptureBuilder = VideoCapture
        .Builder(Recorder.Builder().build())
        .setMirrorMode(MIRROR_MODE_ON_FRONT_ONLY)

    val cameraState = CameraState(
        previewBuilder = previewBuilder,
        videoCaptureBuilder = videoCaptureBuilder,
        startWithFrontCamera = cameraMode is CameraMode.Reaction,
    )

    val recordingManager = RecordingManager(
        maxDuration = cameraConfiguration.maxTotalDuration,
        allowExceedingMaxDuration = cameraConfiguration.allowExceedingMaxDuration,
        coroutineScope = viewModelScope,
        videoRecorder = VideoRecorder(
            videoCaptureProvider = { cameraState.videoCaptureUseCase },
            filesDirProvider = { filesDirDeferred.await() },
        ),
    )

    var cameraLayoutMode by mutableStateOf(
        when ((cameraMode as? CameraMode.Reaction)?.cameraLayoutMode) {
            CameraLayoutMode.Vertical -> LayoutMode.Vertical
            CameraLayoutMode.Horizontal -> LayoutMode.Horizontal
            null -> null
        },
    )
        private set

    private val _uiEvent = Channel<SingleEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private val layoutState = LayoutState(cameraMode)

    private var page: DesignBlock = 0
    private var pixelStreamFill1: DesignBlock = 0
    private var primaryBlock: DesignBlock = 0
    private var secondaryBlock: DesignBlock = 0

    init {
        viewModelScope.launch {
            val filesDir = withContext(Dispatchers.IO) {
                getApplication<Application>().filesDir
            }
            filesDirDeferred.complete(filesDir)
        }

        viewModelScope.launch {
            snapshotFlow { recordingManager.state.status }
                .distinctUntilChanged()
                .filter { status ->
                    status is RecordingManager.Status.Idle || status is RecordingManager.Status.Recording
                }.collect { status ->
                    playReactionVideo(
                        when (status) {
                            RecordingManager.Status.Idle -> false
                            is RecordingManager.Status.Recording -> true
                            else -> throw IllegalStateException()
                        },
                    )
                }
        }
    }

    fun loadScene() {
        if (engine.scene.get() != null) {
            page = engine.scene.getPages()[0]
            primaryBlock = engine.block.findByName(RECT1_NAME)[0]
            secondaryBlock = engine.block.findByName(RECT2_NAME)[0]
            pixelStreamFill1 = engine.block.getFill(primaryBlock)
        }

        val scene = engine.scene.createForVideo()
        engine.editor.setGlobalScope(Scope.EditorSelect, GlobalScope.DENY)
        engine.editor.setSettingBoolean("touch/singlePointPanning", false)
        engine.editor.setSettingFloat("positionSnappingThreshold", 8f)

        // Set up the page
        page = engine.block.create(DesignBlockType.Page)
        engine.block.appendChild(scene, page)

        val canvasWidth = cameraConfiguration.videoSize.width
        val canvasHeight = cameraConfiguration.videoSize.height

        engine.block.setWidth(scene, canvasWidth)
        engine.block.setHeight(scene, canvasHeight)

        engine.block.setWidth(page, canvasWidth)
        engine.block.setHeight(page, canvasHeight)

        // Set up the black background
        val backgroundRect = engine.block.create(DesignBlockType.Graphic)
        val backgroundShape = engine.block.createShape(ShapeType.Rect)
        engine.block.setShape(backgroundRect, backgroundShape)
        val backgroundFill = engine.block.createFill(FillType.Color)
        engine.block.setColor(backgroundFill, "fill/color/value", Color.fromRGBA(0, 0, 0, 255))

        engine.block.setFill(backgroundRect, backgroundFill)
        engine.block.setWidth(backgroundRect, canvasWidth)
        engine.block.setHeight(backgroundRect, canvasHeight)

        engine.block.appendChild(page, backgroundRect)

        // Set up the primary stream
        primaryBlock = engine.block.create(DesignBlockType.Graphic)
        val shape1 = engine.block.createShape(ShapeType.Rect)
        engine.block.setShape(primaryBlock, shape1)
        engine.block.setName(primaryBlock, RECT1_NAME)
        engine.block.setVisible(primaryBlock, false)

        engine.block.appendChild(page, primaryBlock)

        pixelStreamFill1 = engine.block.createFill(FillType.PixelStream)
        engine.block.setFill(primaryBlock, pixelStreamFill1)

        // Set up the secondary stream
        secondaryBlock = engine.block.create(DesignBlockType.Graphic)
        val shape2 = engine.block.createShape(ShapeType.Rect)
        engine.block.setShape(secondaryBlock, shape2)
        engine.block.setName(secondaryBlock, RECT2_NAME)
        engine.block.setVisible(secondaryBlock, false)

        engine.block.appendChild(page, secondaryBlock)
    }

    suspend fun setupLayout(
        maxWidth: Float,
        maxHeight: Float,
    ) {
        val zoomedHeight = maxWidth * (cameraConfiguration.videoSize.height / cameraConfiguration.videoSize.width)
        val topPadding = (maxHeight - zoomedHeight).coerceAtLeast(0f)
        engine.scene.zoomToBlock(page, paddingTop = topPadding)

        configureCameraMode()
    }

    fun setCameraPreview() {
        engine.setCameraPreview(pixelStreamFill1, cameraState.previewUseCase, mirrored = cameraState.showFrontCamera) {
            engine.block.setVisible(primaryBlock, true)
            cameraState.isReady = true
        }
    }

    fun toggleCamera() {
        engine.block.setVisible(primaryBlock, false)
        cameraState.toggleCamera()
        engine.setCameraPreview(pixelStreamFill1, cameraState.previewUseCase, mirrored = cameraState.showFrontCamera) {
            engine.block.setVisible(primaryBlock, true)
        }
    }

    fun updateLayoutMode(layoutMode: LayoutMode) {
        cameraLayoutMode = layoutMode
        resetLayout()
    }

    fun swapLayoutPositions() {
        layoutState.toggleSwapPositions()
        layoutState.updateLayout(cameraLayoutMode)
        resetLayout()
    }

    fun deletePreviousRecording() {
        recordingManager.deletePreviousRecording()
        if (cameraMode is CameraMode.Reaction) {
            val fill = engine.block.getFill(secondaryBlock)
            engine.block.setPlaybackTime(fill, recordingManager.state.totalRecordedDuration.toDouble(DurationUnit.SECONDS))
        }
    }

    fun getResult(): CameraResult = when (cameraMode) {
        is CameraMode.Reaction ->
            CameraResult.Reaction(
                video = Video(
                    uri = cameraMode.video,
                    rect = layoutState.rect1,
                ),
                reaction = ArrayList(recordingManager.state.recordings),
            )
        is CameraMode.Standard ->
            CameraResult.Record(
                recordings = ArrayList(recordingManager.state.recordings),
            )
    }

    private fun loadVideo() {
        viewModelScope.launch {
            try {
                val videoFill = engine.block.getFill(secondaryBlock)
                engine.block.forceLoadAVResource(videoFill)
                val duration = engine.block.getAVResourceTotalDuration(videoFill)
                engine.block.setDuration(secondaryBlock, duration)
                recordingManager.overrideMaxDuration(duration.seconds)
                onAssetLoaded()
            } catch (_: Exception) {
                sendSingleEvent(SingleEvent.ErrorLoadingVideo)
            }
        }
    }

    private fun playReactionVideo(play: Boolean) {
        if (cameraMode !is CameraMode.Reaction) return
        if (reactionVideoIsPlaying == play) return
        reactionVideoIsPlaying = play
        val fill = engine.block.getFill(secondaryBlock)
        engine.block.setPlaying(fill, play)
    }

    private fun configureCameraMode() {
        resetLayout()
        if (cameraMode is CameraMode.Reaction) {
            val videoFill = engine.block.createFill(FillType.Video)
            engine.block.setString(videoFill, "fill/video/fileURI", cameraMode.video.toString())
            engine.block.setFill(secondaryBlock, videoFill)
            engine.block.setSoloPlaybackEnabled(videoFill, true)
            engine.block.setVisible(secondaryBlock, true)
            loadVideo()
        } else {
            onAssetLoaded()
        }
    }

    private fun resetLayout() {
        layoutState.updateLayout(cameraLayoutMode)
        engine.block.setFrame(primaryBlock, layoutState.cameraRect)
        if (cameraMode is CameraMode.Reaction) {
            engine.block.setFrame(secondaryBlock, layoutState.rect1)
        }
        recordingManager.cameraRect = layoutState.cameraRect
    }

    private fun onAssetLoaded() {
        recordingManager.enable()
    }

    private fun sendSingleEvent(event: SingleEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }

    override fun onCleared() {
        super.onCleared()
        engine.stop()
    }

    companion object {
        private const val RECT1_NAME = "Rect"
        private const val RECT2_NAME = "Rect2"
    }
}
