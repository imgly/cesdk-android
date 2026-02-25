package ly.img.editor.base.timeline.clip

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import ly.img.editor.base.timeline.state.TimelineConfiguration

/**
 * Displays the text content of a text clip directly in the timeline.
 * Text clips show their actual content instead of rendered image thumbnails.
 * When the clip is scrolled off-screen to the left, the text pins to stay visible
 * (matching iOS behavior where text pins only when it fits without truncation).
 */
@Composable
fun ClipTextContentView(
    textContent: String,
    labelWidth: Dp,
    modifier: Modifier = Modifier,
    pinOffset: Dp = 0.dp,
) {
    val effectiveLabelWidth = labelWidth + pinOffset
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(
                start = effectiveLabelWidth + TimelineConfiguration.textContentHorizontalPadding,
                end = TimelineConfiguration.textContentHorizontalPadding,
            ),
        contentAlignment = Alignment.CenterStart,
    ) {
        Text(
            text = textContent,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
