package ly.img.editor.core.ui.utils

import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.MutableState

suspend fun LazyListState.centerSelectedItem(
    index: Int,
    animate: Boolean = true,
    isInitialPositionSettled: MutableState<Boolean>,
) {
    val center = layoutInfo.viewportEndOffset / 2
    val target = layoutInfo.visibleItemsInfo.firstOrNull {
        it.index == index
    } ?: run {
        scrollToItem(index)
        if (animate) {
            centerSelectedItem(index = index, animate = false, isInitialPositionSettled = isInitialPositionSettled)
        }
        return
    }
    val childCenter = target.offset + target.size / 2
    val diff = (childCenter - center).toFloat()
    if (animate && isInitialPositionSettled.value) {
        animateScrollBy(diff)
    } else {
        scrollBy(diff)
        if (isInitialPositionSettled.value.not()) {
            isInitialPositionSettled.value = true
        }
    }
}
