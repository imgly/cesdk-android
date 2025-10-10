package ly.img.editor.core.ui.library.components.asset

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.absoluteOffset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import ly.img.editor.core.library.AssetType
import ly.img.editor.core.library.data.AssetSourceType
import ly.img.editor.core.ui.library.components.LibraryImageCard
import ly.img.editor.core.ui.library.getFormattedDuration
import ly.img.editor.core.ui.library.getMeta
import ly.img.editor.core.ui.library.getThumbnailUri
import ly.img.editor.core.ui.library.state.WrappedAsset
import ly.img.editor.core.ui.library.util.AssetLibraryUiConfig
import ly.img.engine.Asset

@Composable
internal fun AssetImage(
    wrappedAsset: WrappedAsset,
    assetType: AssetType,
    onAssetClick: (WrappedAsset) -> Unit,
    onAssetLongClick: (WrappedAsset) -> Unit,
) {
    LibraryImageCard(
        uri = wrappedAsset.asset.getThumbnailUri(),
        isVideo = wrappedAsset.asset.getMeta("kind") == "video",
        onClick = { onAssetClick(wrappedAsset) },
        onLongClick = { onAssetLongClick(wrappedAsset) },
        contentPadding = AssetLibraryUiConfig.contentPadding(assetType),
        contentScale = AssetLibraryUiConfig.contentScale(assetType),
        tintImages = AssetLibraryUiConfig.shouldTintImages(assetType),
        overlayContent = {
            if (wrappedAsset.asset.getMeta("kind") == "video") {
                val duration = wrappedAsset.asset.getFormattedDuration()
                if (duration.isNotEmpty()) {
                    Text(
                        text = duration,
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .absoluteOffset(x = 8.dp, y = -8.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(Color(0x66000000))
                            .padding(4.dp),
                    )
                }
            }
        },
    )
}

@Preview
@Composable
fun DPreview() {
    Box(Modifier.size(196.dp)) {
        AssetImage(
            wrappedAsset = WrappedAsset.GenericAsset(
                asset = Asset(
                    id = "",
                    context = null,
                    label = "Hallo World",
                    meta = mapOf(
                        "kind" to "video",
                        "duration" to "83",
                    ),
                ),
                assetType = AssetType.Video,
                assetSourceType = AssetSourceType.Videos,
            ),
            assetType = AssetType.Video,
            onAssetClick = {},
            onAssetLongClick = {},
        )
    }
}
