package ly.img.editor.base.engine

import android.net.Uri
import android.view.DragEvent
import android.view.SurfaceView
import android.view.TextureView
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import ly.img.editor.base.R
import ly.img.editor.base.clipboard.detectClipboardLongPress
import ly.img.editor.core.engine.EngineRenderTarget
import ly.img.editor.core.theme.surface1
import ly.img.editor.core.ui.utils.activity
import ly.img.engine.Engine

@Composable
fun EngineCanvasView(
    renderTarget: EngineRenderTarget,
    engine: Engine,
    isCanvasVisible: Boolean,
    passTouches: Boolean,
    onMoveStart: () -> Unit,
    onMoveEnd: () -> Unit,
    loadScene: () -> Unit,
    onTouch: () -> Unit,
    onSizeChanged: () -> Unit,
    onLongPress: (Offset) -> Unit = {},
    canDrop: (mimeTypes: List<String>) -> Boolean = { false },
    onDrop: (uris: List<Uri>) -> Unit = {},
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    // The gesture outlives the composition that started it, so it reads the latest callback.
    val currentOnLongPress by rememberUpdatedState(onLongPress)
    val currentCanDrop by rememberUpdatedState(canDrop)
    val currentOnDrop by rememberUpdatedState(onDrop)
    val activity = requireNotNull(LocalContext.current.activity) {
        "Unable to find the activity. This is an internal error. Please report this issue."
    }
    val renderView = remember {
        when (renderTarget) {
            EngineRenderTarget.SURFACE_VIEW -> SurfaceView(activity)
            EngineRenderTarget.TEXTURE_VIEW -> TextureView(activity)
        }.apply { id = R.id.editor_render_view }
    }
    val renderViewHandler = remember {
        // For TextureView, more than one callbacks cannot be attached
        if (renderView is SurfaceView) RenderViewHandler(renderView) else null
    }
    renderView.alpha = if (isCanvasVisible) 1F else 0F
    val clearColor = MaterialTheme.colorScheme.surface1
    var onMoveStarted by remember { mutableStateOf(false) }
    var appIsPaused by remember { mutableStateOf(false) }

    fun bind() {
        if (appIsPaused.not() && engine.isEngineRunning() && engine.isBound().not()) {
            when (renderTarget) {
                EngineRenderTarget.SURFACE_VIEW -> {
                    engine.bindSurfaceView(renderView as SurfaceView)
                }

                EngineRenderTarget.TEXTURE_VIEW -> {
                    engine.bindTextureView(renderView as TextureView)
                }
            }
        }
    }

    AndroidView(
        factory = { renderView },
        modifier = Modifier
            .pointerInput(passTouches) {
                forEachGesture {
                    awaitPointerEventScope {
                        if (passTouches) {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event: PointerEvent = awaitPointerEvent(pass = PointerEventPass.Final)
                                if (!onMoveStarted) {
                                    onMoveStarted = true
                                    onMoveStart()
                                }
                            } while (event.changes.any { it.pressed })
                            if (onMoveStarted) {
                                onMoveStarted = false
                                onMoveEnd()
                            }
                        } else {
                            val awaitPointerEvent = awaitPointerEvent(pass = PointerEventPass.Initial)
                            awaitPointerEvent.changes.forEach {
                                it.consume()
                            }
                        }
                    }
                    onTouch()
                }
            }
            .pointerInput(passTouches) {
                if (passTouches) detectClipboardLongPress { currentOnLongPress(it) }
            },
    )
    // Files dragged from another app, such as Files or Photos, in split screen or on a desktop window.
    DisposableEffect(renderView) {
        renderView.setOnDragListener { _, event ->
            when (event.action) {
                DragEvent.ACTION_DRAG_STARTED -> {
                    val description = event.clipDescription
                    description != null && currentCanDrop((0 until description.mimeTypeCount).map(description::getMimeType))
                }
                DragEvent.ACTION_DROP -> {
                    // The grant to read the files of another app lasts while the activity lives.
                    activity.requestDragAndDropPermissions(event)
                    val clip = event.clipData
                    currentOnDrop((0 until clip.itemCount).mapNotNull { clip.getItemAt(it).uri })
                    true
                }
                else -> true
            }
        }
        onDispose { renderView.setOnDragListener(null) }
    }
    LaunchedEffect(clearColor) {
        engine.setClearColor(clearColor)
    }
    LaunchedEffect(Unit) {
        engine.editor.setAppIsPaused(appIsPaused)
        if (renderTarget == EngineRenderTarget.SURFACE_VIEW) {
            renderViewHandler?.addCallback(onSizeChanged)
        }
        bind()
        loadScene()
    }
    DisposableEffect(Unit) {
        val observer = LifecycleEventObserver { _, event ->
            if (engine.isEngineRunning().not()) return@LifecycleEventObserver
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    appIsPaused = true
                    engine.editor.setAppIsPaused(true)
                    engine.pause()
                }
                Lifecycle.Event.ON_RESUME -> {
                    appIsPaused = false
                    engine.unpause()
                    bind()
                    engine.editor.setAppIsPaused(false)
                }
                else -> {
                }
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (engine.isEngineRunning()) {
                engine.unbind()
            }
            renderViewHandler?.removeCallback()
        }
    }
}
