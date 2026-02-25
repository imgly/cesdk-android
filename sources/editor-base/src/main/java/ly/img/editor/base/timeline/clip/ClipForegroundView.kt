package ly.img.editor.base.timeline.clip

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex

@Composable
fun ClipForegroundView(
    clip: Clip,
    isSelected: Boolean,
    clipDurationText: String,
    pinOffset: Dp = 0.dp,
    overlayWidth: Dp,
    overlayShape: Shape? = null,
    onLabelWidthMeasured: (Dp) -> Unit = {},
) {
    val density = LocalDensity.current

    Box(Modifier.fillMaxSize()) {
        // Center the label vertically, align to start horizontally
        ClipLabelView(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .zIndex(1f)
                .onGloballyPositioned { coordinates ->
                    val widthDp = with(density) { coordinates.size.width.toDp() }
                    onLabelWidthMeasured(widthDp)
                }
                .offset(x = pinOffset),
            clip = clip,
            duration = clipDurationText,
            isSelected = isSelected,
        )

        if (overlayShape != null) {
            ClipOverlay(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .clip(overlayShape)
                    .width(overlayWidth)
                    .fillMaxHeight(),
            )
        }
    }
}
