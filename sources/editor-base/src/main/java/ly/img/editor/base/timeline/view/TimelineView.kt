package ly.img.editor.base.timeline.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import kotlinx.coroutines.flow.filterNotNull
import ly.img.editor.base.timeline.dragdrop.FloatingClipOverlay
import ly.img.editor.base.timeline.state.TimelineState
import ly.img.editor.core.component.EditorComponent
import ly.img.editor.core.component.HorizontalListBuilder
import ly.img.editor.core.component.data.TimelineHeight
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.ui.utils.Easing

@Composable
fun TimelineView(
    timelineState: TimelineState,
    onEvent: (EditorEvent) -> Unit,
    addClipButton: EditorComponent<*>?,
    addAudioButton: EditorComponent<*>?,
    headerListBuilder: HorizontalListBuilder<EditorComponent<*>>,
    height: TimelineHeight,
    expanded: Boolean,
) {
    Box {
        Column {
            PlayerHeader(headerListBuilder = headerListBuilder)

            val verticalScrollState = rememberLazyListState()
            var hasAppliedInitialBottomScroll by remember { mutableStateOf(false) }

            LaunchedEffect(
                expanded,
                timelineState.dataSource.tracks.size,
                timelineState.dataSource.backgroundTrack.clips.size,
            ) {
                if (!expanded || hasAppliedInitialBottomScroll) return@LaunchedEffect
                val hasTimelineContent = timelineState.dataSource.tracks.isNotEmpty() ||
                    timelineState.dataSource.backgroundTrack.clips.isNotEmpty()
                if (!hasTimelineContent) return@LaunchedEffect
                verticalScrollState.scrollToItem(timelineState.dataSource.tracks.size)
                hasAppliedInitialBottomScroll = true
            }

            LaunchedEffect(Unit) {
                snapshotFlow { timelineState.selectedClip }
                    .filterNotNull()
                    .collect { clip ->
                        val index = timelineState.dataSource.indexOf(clip)
                        if (index == -1) return@collect
                        val isClipAlreadyVisible =
                            verticalScrollState.layoutInfo.visibleItemsInfo.find { it.index == index }?.let { itemInfo ->
                                itemInfo.offset >= verticalScrollState.layoutInfo.viewportStartOffset &&
                                    (itemInfo.offset + itemInfo.size) <= verticalScrollState.layoutInfo.viewportEndOffset
                            } ?: false
                        if (!isClipAlreadyVisible) {
                            verticalScrollState.animateScrollToItem(index)
                        }
                    }
            }
            AnimatedVisibility(
                visible = expanded,
                enter = enterTransition(),
                exit = exitTransition(),
            ) {
                TimelineContentView(
                    timelineState = timelineState,
                    verticalScrollState = verticalScrollState,
                    onEvent = onEvent,
                    addClipButton = addClipButton,
                    addAudioButton = addAudioButton,
                    height = height,
                )
            }
        }
        // Mounted at the outer Box so the floating clip can move anywhere without getting clipped
        FloatingClipOverlay(timelineState = timelineState)
    }
}

private fun enterTransition() = fadeIn(tween(durationMillis = 500, easing = Easing.EmphasizedDecelerate)) +
    expandVertically(tween(durationMillis = 500, easing = Easing.EmphasizedDecelerate))

private fun exitTransition() = fadeOut(tween(durationMillis = 250, easing = Easing.EmphasizedDecelerate)) +
    shrinkVertically(
        tween(durationMillis = 350, easing = Easing.EmphasizedDecelerate),
    )
