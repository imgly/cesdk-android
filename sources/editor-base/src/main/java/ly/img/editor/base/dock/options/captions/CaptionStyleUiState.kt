package ly.img.editor.base.dock.options.captions

import androidx.compose.runtime.Immutable
import ly.img.editor.core.library.AssetType
import ly.img.editor.core.library.data.AssetSourceType
import ly.img.editor.core.ui.library.state.WrappedAsset
import ly.img.engine.Engine
import ly.img.engine.FindAssetsQuery

/** The caption style presets, with the one currently applied to the track marked active. */
@Immutable
internal data class CaptionStyleUiState(
    val presets: List<WrappedAsset>,
) {
    companion object {
        /**
         * @return `null` when the source is not registered or holds no presets, so the sheet does not open on
         * an empty grid.
         */
        suspend fun create(
            engine: Engine,
            locale: String,
        ): CaptionStyleUiState? {
            if (CAPTION_PRESETS_SOURCE_ID !in engine.asset.findAllSources()) return null
            val applied = CaptionsEngine(engine).appliedPresetIdentifier()
            val presets = engine.asset.findAssets(
                sourceId = CAPTION_PRESETS_SOURCE_ID,
                query = FindAssetsQuery(
                    perPage = Int.MAX_VALUE,
                    page = 0,
                    locale = locale,
                ),
            ).assets.map { asset ->
                WrappedAsset.GenericAsset(
                    asset = asset.copy(active = asset.id == applied),
                    assetSourceType = AssetSourceType.CaptionPresets,
                    assetType = AssetType.TextStylePreset,
                )
            }
            return presets.takeIf { it.isNotEmpty() }?.let(::CaptionStyleUiState)
        }
    }
}
