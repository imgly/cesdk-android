package ly.img.editor.base.ui

import android.graphics.Bitmap
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import ly.img.editor.base.engine.capturePageSnapshot
import ly.img.editor.core.ui.engine.deselectAllBlocks
import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import kotlin.math.roundToInt

/** Keeps the render surface bound while a bitmap bridges the canvas and the overview cell. */
internal class PageOverviewTransition(
    private val engine: Engine,
    private val scope: CoroutineScope,
    private val onError: (Throwable) -> Unit,
    private val renderView: () -> View?,
) {
    var image by mutableStateOf<Bitmap?>(null)
        private set
    var page by mutableStateOf<DesignBlock?>(null)
        private set
    var bounds by mutableStateOf(Rect.Zero)
        private set
    var cornerRadius by mutableStateOf(0f)
        private set
    var running by mutableStateOf(false)
        private set
    var heldPagesState by mutableStateOf<EditorPagesState?>(null)
        private set

    private val pageBounds = mutableStateMapOf<DesignBlock, Rect>()
    private var job: Job? = null
    private var requestId = 0

    fun onPageBounds(
        page: DesignBlock,
        bounds: Rect?,
    ) {
        if (bounds == null || bounds.isEmpty) pageBounds.remove(page) else pageBounds[page] = bounds
    }

    fun start(
        entering: Boolean,
        pagesState: EditorPagesState?,
        changeMode: () -> Unit,
    ) {
        cancel()
        if (entering) pageBounds.clear()
        val currentRequest = requestId
        job = scope.launch(start = CoroutineStart.LAZY) {
            var modeChanged = false

            fun changeModeOnce() {
                modeChanged = true
                changeMode()
            }
            running = true
            try {
                val view = renderView()
                if (view == null || !view.isAttachedToWindow) {
                    changeModeOnce()
                    return@launch
                }
                val targetPage = if (entering) engine.scene.getCurrentPage() else pagesState?.selectedPage?.block
                if (targetPage == null) {
                    changeModeOnce()
                    return@launch
                }
                if (entering) {
                    engine.deselectAllBlocks()
                    awaitCanvasLayout()
                    val snapshot = capturePageSnapshot(engine, view, targetPage)
                    if (snapshot == null) {
                        changeModeOnce()
                        return@launch
                    }
                    cornerRadius = 0f
                    image = snapshot.bitmap
                    bounds = snapshot.bounds
                    page = targetPage
                    changeModeOnce()
                    val cell = withTimeoutOrNull(500) {
                        snapshotFlow { pageBounds[targetPage] }.first { it != null }
                    } ?: return@launch
                    animate(snapshot.bounds, cell, entering = true, density = view.resources.displayMetrics.density)
                } else {
                    val cell = pageBounds[targetPage]
                    // Keep the grid visible until the destination page has rendered behind it.
                    heldPagesState = pagesState
                    changeModeOnce()
                    if (cell == null) return@launch
                    awaitCanvasLayout()
                    val snapshot = capturePageSnapshot(engine, view, targetPage) ?: return@launch
                    cornerRadius = 12f * view.resources.displayMetrics.density
                    image = snapshot.bitmap
                    bounds = cell
                    page = targetPage
                    heldPagesState = null
                    animate(cell, snapshot.bounds, entering = false, density = view.resources.displayMetrics.density)
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // A failed cosmetic transition must not swallow navigation or escape the UI scope.
                onError(e)
                if (!modeChanged) {
                    try {
                        changeModeOnce()
                    } catch (modeError: Exception) {
                        onError(modeError)
                    }
                }
            } finally {
                if (currentRequest == requestId) {
                    clear()
                    job = null
                }
            }
        }
        job?.start()
    }

    private suspend fun animate(
        from: Rect,
        to: Rect,
        entering: Boolean,
        density: Float,
    ) {
        Animatable(0f).animateTo(1f, tween(400, easing = FastOutSlowInEasing)) {
            bounds = lerp(from, to, value)
            cornerRadius = 12f * density * if (entering) value else 1f - value
        }
    }

    private suspend fun awaitCanvasLayout() {
        // Give Compose's inset updates and the engine render loop a frame each.
        repeat(3) { withFrameNanos { } }
    }

    fun cancel() {
        requestId++
        job?.cancel()
        job = null
        clear()
    }

    private fun clear() {
        // Compose may still draw the previous frame. Drop ownership instead of recycling a displayed bitmap.
        image = null
        page = null
        heldPagesState = null
        running = false
    }
}

@Composable
internal fun PageOverviewTransitionOverlay(transition: PageOverviewTransition) {
    if (!transition.running) return
    var origin by remember { mutableStateOf(Offset.Zero) }
    val bitmap = transition.image
    val image = remember(bitmap) { bitmap?.asImageBitmap() }
    val clip = remember { Path() }
    Canvas(
        Modifier
            .fillMaxSize()
            .onGloballyPositioned { origin = it.positionInWindow() }
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) awaitPointerEvent().changes.forEach { it.consume() }
                }
            },
    ) {
        val snapshot = image ?: return@Canvas
        val bounds = transition.bounds.translate(-origin)
        clip.reset()
        clip.addRoundRect(RoundRect(bounds, CornerRadius(transition.cornerRadius)))
        clipRect {
            clipPath(clip) {
                drawImage(
                    image = snapshot,
                    dstOffset = IntOffset(bounds.left.roundToInt(), bounds.top.roundToInt()),
                    dstSize = IntSize(bounds.width.roundToInt().coerceAtLeast(1), bounds.height.roundToInt().coerceAtLeast(1)),
                )
            }
        }
    }
}
