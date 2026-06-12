package ly.img.editor.plugin.ai.imageGeneration.component

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import ly.img.editor.core.ui.SheetHeader
import ly.img.editor.plugin.ai.core.gateway.AIGatewayPromptStyle
import ly.img.editor.plugin.ai.imageGeneration.R
import ly.img.editor.plugin.ai.imageGeneration.preview.PreviewTheme
import androidx.compose.foundation.lazy.grid.items as gridItems

@Composable
internal fun StyleSelectionView(
    selectedStyle: AIGatewayPromptStyle,
    onStyleSelected: (AIGatewayPromptStyle) -> Unit,
    onBack: () -> Unit,
    onCloseSheet: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(vertical = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        SheetHeader(
            title = stringResource(R.string.ly_img_plugin_ai_image_generation_text_style_selection_title),
            onClose = onCloseSheet,
            actionContent = {
                IconButton(
                    onClick = onBack,
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = stringResource(ly.img.editor.core.R.string.ly_img_editor_sheet_button_back),
                    )
                }
            },
        )

        StyleSelectionContent(
            modifier = Modifier.weight(1f),
            selectedStyle = selectedStyle,
            onStyleSelected = onStyleSelected,
        )
    }
}

@Composable
internal fun StyleSelectionContent(
    selectedStyle: AIGatewayPromptStyle,
    onStyleSelected: (AIGatewayPromptStyle) -> Unit,
    modifier: Modifier = Modifier,
) {
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 96.dp),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = 56.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        gridItems(AIGatewayPromptStyle.curated) { style ->
            StyleSelectionItem(
                style = style,
                isSelected = style == selectedStyle,
                onClick = { onStyleSelected(style) },
            )
        }
    }
}

@Composable
internal fun StyleSelectionItem(
    style: AIGatewayPromptStyle,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        SelectionBorder(
            isSelected = isSelected,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant,
                        shape = RoundedCornerShape(8.dp),
                    )
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onClick() },
                contentAlignment = Alignment.Center,
            ) {
                if (style.id == AIGatewayPromptStyle.NONE_ID) {
                    // No-style placeholder — Android-idiomatic "clear" glyph.
                    Icon(
                        imageVector = Icons.Filled.Clear,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxSize(.4f),
                    )
                } else {
                    AsyncImage(
                        model = style.thumbnailAssetUri,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(6.dp)),
                    )

                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .fillMaxHeight(.5f)
                            .background(
                                brush = Brush.verticalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Black.copy(alpha = .3f),
                                    ),
                                ),
                            ),
                    )
                }
            }
        }

        Text(
            text = style.displayName,
            modifier = Modifier,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            overflow = TextOverflow.Ellipsis,
            maxLines = 2,
        )
    }
}

@Composable
@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
internal fun StyleSelectionViewPreview() {
    PreviewTheme {
        Surface {
            StyleSelectionView(
                selectedStyle = AIGatewayPromptStyle.curated.first(),
                onStyleSelected = {},
                onBack = {},
                onCloseSheet = {},
            )
        }
    }
}

@Composable
@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
internal fun StyleSelectionItemPreview() {
    PreviewTheme {
        Surface {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    StyleSelectionItem(
                        style = AIGatewayPromptStyle.curated[1],
                        isSelected = true,
                        onClick = {},
                        modifier = Modifier.weight(1f),
                    )

                    StyleSelectionItem(
                        style = AIGatewayPromptStyle.curated.first(),
                        isSelected = false,
                        onClick = {},
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
}
