package ly.img.editor.base.timeline.dragdrop

import android.graphics.BlurMaskFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import ly.img.editor.base.timeline.clip.ClipBackgroundView
import ly.img.editor.base.timeline.state.TimelineConfiguration
import ly.img.editor.base.timeline.state.TimelineState
import ly.img.editor.core.theme.LocalExtendedColorScheme
import ly.img.editor.core.theme.surface3
import ly.img.editor.core.ui.utils.toDp
import kotlin.math.roundToInt

private val DROP_SHADOW_BLUR_RADIUS = 8.dp
private const val DROP_SHADOW_ALPHA = 80

/**
 * Clip shaped overlay that follows the finger while a drag is active; renders nothing while idle.
 *
 * Stays pinned to the grab point by subtracting `grabOffsetX/Y` (captured at gesture start);
 * without this, grabbing a clip at an edge would snap to centre under the finger. Never
 * intercepts pointer events — the source [ClipView]'s drag gesture continues to drive
 * `DragDropState`.
 */
@Composable
fun FloatingClipOverlay(timelineState: TimelineState) {
    var overlayCoords: LayoutCoordinates? by remember { mutableStateOf(null) }

    val clip = timelineState.dragDrop.draggedClip
    if (clip == null) {
        // Pre-warm `overlayCoords` for the active branch's first frame.
        Box(modifier = Modifier.onGloballyPositioned { overlayCoords = it })
        return
    }

    val zoomState = timelineState.zoomState
    val widthPx = zoomState.toPx(clip.duration)
    val height = TimelineConfiguration.clipHeight
    // Pointer over bg with a non-bg-compatible source (audio): visual hint only — the
    // resolver still falls back to a foreground target. `derivedStateOf` skips the
    // per-pointer-tick recompose unless the boolean actually transitions.
    val isInvalidDropZone by remember(timelineState) {
        derivedStateOf {
            val ctx = (timelineState.dragDrop.phase as? DragDropState.Dragging)?.context
                ?: return@derivedStateOf false
            val dragged = timelineState.dragDrop.draggedClip ?: return@derivedStateOf false
            if (dragged.isInBackgroundTrack || isBackgroundCompatible(dragged.clipType)) {
                return@derivedStateOf false
            }
            val backgroundTrack = timelineState.dataSource.backgroundTrack
            val backgroundFrame = timelineState.dragDrop.trackFrames[backgroundTrack.id]
                ?: return@derivedStateOf false
            ctx.currentTouchLocation.y in backgroundFrame.top..backgroundFrame.bottom
        }
    }
    val invalidColor = MaterialTheme.colorScheme.error
    val activeBorderColor = if (isInvalidDropZone) invalidColor else LocalExtendedColorScheme.current.yellow.color
    val shape = MaterialTheme.shapes.small
    val density = LocalDensity.current
    val widthDp = widthPx.toDp()
    val blurRadiusPx = with(density) { DROP_SHADOW_BLUR_RADIUS.toPx() }
    // Material 3 default for `shapes.small`; hardcoded rather than derived from `shape` so
    // we don't pay an outline build per drag.
    val cornerRadiusPx = with(density) { 8.dp.toPx() }
    val shadowPaint = remember(density) {
        android.graphics.Paint().apply {
            isAntiAlias = true
            color = android.graphics.Color.argb(DROP_SHADOW_ALPHA, 0, 0, 0)
            maskFilter = BlurMaskFilter(blurRadiusPx, BlurMaskFilter.Blur.OUTER)
        }
    }

    Box(modifier = Modifier.onGloballyPositioned { overlayCoords = it }) {
        val overlayOriginInWindow = overlayCoords?.localToWindow(Offset.Zero) ?: Offset.Zero

        // Outer Box pads the clip by [DROP_SHADOW_BLUR_RADIUS] to host a custom blur.
        // `Modifier.shadow(...)` would be simpler but its ambient inward bleed shows
        // through the 0.9 alpha wrapper as a recessed-well artifact inside the yellow
        // border; `BlurMaskFilter.Blur.OUTER` paints only outside the outline, no bleed.
        Box(
            modifier = Modifier
                .offset {
                    // Reading `phase` in placement scope re-places without recomposing
                    // the outer scope. Use the captured `overlayOriginInWindow` rather
                    // than `overlayCoords` — the latter resolves to a detached
                    // LayoutCoordinates on the first frame after slot transition.
                    val ctx = (timelineState.dragDrop.phase as? DragDropState.Dragging)?.context
                        ?: return@offset IntOffset.Zero
                    val clipLeadingX = ctx.currentTouchLocation.x - ctx.grabOffsetX - overlayOriginInWindow.x
                    val clipTopY = ctx.currentTouchLocation.y - ctx.grabOffsetY - overlayOriginInWindow.y
                    // Shift by `-blurRadiusPx` so the inner clip body (centred in the
                    // padded outer Box) lands at the original leading-top position.
                    IntOffset(
                        x = (clipLeadingX - blurRadiusPx).roundToInt(),
                        y = (clipTopY - blurRadiusPx).roundToInt(),
                    )
                }
                .width(widthDp + DROP_SHADOW_BLUR_RADIUS * 2)
                .height(height + DROP_SHADOW_BLUR_RADIUS * 2)
                .drawBehind {
                    drawIntoCanvas { canvas ->
                        canvas.nativeCanvas.drawRoundRect(
                            blurRadiusPx,
                            blurRadiusPx,
                            size.width - blurRadiusPx,
                            size.height - blurRadiusPx,
                            cornerRadiusPx,
                            cornerRadiusPx,
                            shadowPaint,
                        )
                    }
                },
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(widthDp)
                    .height(height)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.surface3.copy(alpha = 0.6f))
                    .alpha(0.9f),
            ) {
                ClipBackgroundView(
                    clip = clip,
                    timelineState = timelineState,
                    inTimeline = false,
                    clipWidth = widthDp,
                )
                if (isInvalidDropZone) {
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .background(invalidColor.copy(alpha = 0.35f), shape),
                    )
                }
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .border(2.dp, activeBorderColor, shape),
                )
            }
        }
    }
}
