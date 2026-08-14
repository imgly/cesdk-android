package ly.img.editor.base.timeline.track

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import ly.img.editor.base.timeline.state.TimelineConfiguration
import ly.img.editor.core.iconpack.IconPack
import ly.img.editor.core.iconpack.Plus
import ly.img.editor.core.iconpack.Transition
import kotlin.math.roundToInt

@Composable
internal fun TransitionSeamView(
    offsetPx: () -> Float,
    hasTransition: Boolean,
    isCompact: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .size(TimelineConfiguration.clipHeight)
            .zIndex(2f)
            .offset { IntOffset((offsetPx() - TimelineConfiguration.clipHeight.toPx() / 2).roundToInt(), 0) }
            .clip(CircleShape)
            .then(if (isCompact) Modifier else Modifier.clickable(onClick = onClick)),
        contentAlignment = Alignment.Center,
    ) {
        Surface(
            modifier = Modifier.size(
                if (isCompact) TimelineConfiguration.compactTransitionSeamSize else TimelineConfiguration.transitionSeamSize,
            ),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface,
            contentColor = MaterialTheme.colorScheme.onSurface,
            shadowElevation = 4.dp,
        ) {
            if (isCompact) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier.size(6.dp),
                        shape = CircleShape,
                        color = LocalContentColor.current,
                        content = {},
                    )
                }
            } else {
                Icon(
                    modifier = Modifier
                        .matchParentSize()
                        .padding(4.dp),
                    imageVector = if (hasTransition) IconPack.Transition else IconPack.Plus,
                    contentDescription = null,
                )
            }
        }
    }
}
