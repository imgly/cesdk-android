package ly.img.editor.base.timeline.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
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
import ly.img.editor.base.timeline.state.TimelineState
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.ui.utils.Easing

@Composable
fun TimelineView(
    timelineState: TimelineState,
    onEvent: (EditorEvent) -> Unit,
) {
    Column {
        PlayerHeader(
            timelineState = timelineState,
            expanded = timelineState.expanded,
            onToggleExpand = { timelineState.expanded = timelineState.expanded.not() },
        )

        val verticalScrollState = rememberLazyListState()
        var hasAppliedInitialBottomScroll by remember { mutableStateOf(false) }

        LaunchedEffect(
            timelineState.expanded,
            timelineState.dataSource.tracks.size,
            timelineState.dataSource.backgroundTrack.clips.size,
        ) {
            if (!timelineState.expanded || hasAppliedInitialBottomScroll) return@LaunchedEffect
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
                    val isClipAlreadyVisible = verticalScrollState.layoutInfo.visibleItemsInfo.find { it.index == index }?.let { itemInfo ->
                        itemInfo.offset >= verticalScrollState.layoutInfo.viewportStartOffset &&
                            (itemInfo.offset + itemInfo.size) <= verticalScrollState.layoutInfo.viewportEndOffset
                    } ?: false
                    if (!isClipAlreadyVisible) {
                        verticalScrollState.animateScrollToItem(index)
                    }
                }
        }
        AnimatedVisibility(
            visible = timelineState.expanded,
            enter = enterTransition(),
            exit = exitTransition(),
        ) {
            TimelineContentView(
                timelineState = timelineState,
                verticalScrollState = verticalScrollState,
                onEvent = onEvent,
            )
        }
    }
}

private fun enterTransition() = fadeIn(tween(durationMillis = 500, easing = Easing.EmphasizedDecelerate)) +
    expandVertically(tween(durationMillis = 500, easing = Easing.EmphasizedDecelerate))

private fun exitTransition() = fadeOut(tween(durationMillis = 250, easing = Easing.EmphasizedDecelerate)) +
    shrinkVertically(
        tween(durationMillis = 350, easing = Easing.EmphasizedDecelerate),
    )
