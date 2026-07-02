package ly.img.editor.base.dock.options.textonpath

import androidx.compose.runtime.Immutable
import ly.img.editor.base.dock.options.format.VerticalAlignment
import ly.img.editor.core.library.AssetType
import ly.img.editor.core.library.data.AssetSourceType
import ly.img.editor.core.ui.library.state.WrappedAsset
import ly.img.engine.Asset
import ly.img.engine.AssetContext
import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import ly.img.engine.FindAssetsQuery

@Immutable
data class TextOnPathUiState(
    val curves: List<WrappedAsset>,
    val hasPath: Boolean,
    val verticalAlignment: VerticalAlignment,
    val isFlipped: Boolean,
    val offset: Float,
) {
    companion object {
        const val SOURCE_ID = "ly.img.text.curves"

        suspend fun create(
            designBlock: DesignBlock,
            engine: Engine,
            locale: String,
        ): TextOnPathUiState {
            val hasPath = engine.block.getTextOnPath(designBlock) != null
            // A manually edited path clears the hint, so it highlights neither None (a path exists) nor a tile.
            val externalRef = engine.block.getString(designBlock, "text/pathExternalRef")
            val assetSourceType = AssetSourceType(SOURCE_ID)
            val noneAsset = WrappedAsset.GenericAsset(
                asset = Asset(
                    id = "none",
                    context = AssetContext(sourceId = SOURCE_ID),
                    active = !hasPath,
                ),
                assetSourceType = assetSourceType,
                assetType = AssetType.Shape,
                isNone = true,
            )
            val curves = engine.asset.findAssets(
                sourceId = SOURCE_ID,
                query = FindAssetsQuery(
                    perPage = Int.MAX_VALUE,
                    page = 0,
                    locale = locale,
                ),
            ).assets.map {
                WrappedAsset.GenericAsset(
                    asset = it.copy(active = externalRef.isNotEmpty() && it.externalRef() == externalRef),
                    assetSourceType = assetSourceType,
                    assetType = AssetType.Shape,
                )
            }
            return TextOnPathUiState(
                curves = listOf(noneAsset) + curves,
                hasPath = hasPath,
                verticalAlignment = VerticalAlignment.valueOf(
                    engine.block.getEnum(designBlock, "text/verticalAlignment"),
                ),
                isFlipped = engine.block.getTextOnPathFlipped(designBlock),
                offset = engine.block.getTextOnPathOffset(designBlock),
            )
        }

        private fun Asset.externalRef(): String = "$SOURCE_ID|$id"
    }
}
