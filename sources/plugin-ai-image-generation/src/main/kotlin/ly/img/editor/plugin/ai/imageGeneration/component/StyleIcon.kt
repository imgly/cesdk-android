package ly.img.editor.plugin.ai.imageGeneration.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ly.img.editor.plugin.ai.core.gateway.AIGatewayPromptStyle
import ly.img.editor.plugin.ai.imageGeneration.preview.PreviewTheme

private val styleIconShape = RoundedCornerShape(
    topStart = 24.dp,
    bottomStart = 24.dp,
    topEnd = 8.dp,
    bottomEnd = 8.dp,
)

@Composable
internal fun StyleIcon(
    style: AIGatewayPromptStyle,
    modifier: Modifier = Modifier,
) {
    val isNone = style.id == AIGatewayPromptStyle.NONE_ID
    // "None" sits on the same neutral chip background as the Format
    // selector next to it. Every other style is fully covered by a
    // loaded thumbnail, so its base colour is just a brief placeholder.
    val backgroundColor = if (isNone) {
        MaterialTheme.colorScheme.surfaceVariant
    } else {
        MaterialTheme.colorScheme.primary
    }
    val baseModifier = modifier
        .padding(2.dp)
        .clip(styleIconShape)
        .background(color = backgroundColor)
        .border(
            width = 1.dp,
            color = Color.Black.copy(alpha = .1f),
            shape = styleIconShape,
        )

    if (isNone) {
        Box(
            modifier = baseModifier,
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Icons.Filled.Clear,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(4.dp),
            )
        }
    } else {
        AsyncImage(
            model = style.thumbnailAssetUri,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = baseModifier,
        )
    }
}

@Composable
@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
internal fun StyleIconPreview() {
    PreviewTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Style Icons",
                    style = MaterialTheme.typography.titleMedium,
                )

                AIGatewayPromptStyle.curated.take(4).forEach { style ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        StyleIcon(
                            style = style,
                            modifier = Modifier.size(40.dp),
                        )
                        Text(
                            text = style.displayName,
                            style = MaterialTheme.typography.labelSmall,
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }
        }
    }
}
