package ly.img.editor.base.dock.options.transition

import androidx.compose.runtime.Immutable
import ly.img.editor.base.engine.DesignBlockWithProperties
import ly.img.editor.base.engine.toPropertyAndValueList
import ly.img.editor.core.library.AssetType
import ly.img.editor.core.library.data.AssetSourceType
import ly.img.editor.core.ui.library.getMeta
import ly.img.editor.core.ui.library.state.WrappedAsset
import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import ly.img.engine.FindAssetsQuery
import ly.img.engine.TransitionType

@Immutable
data class TransitionUiState(
    val outgoingBlock: DesignBlock,
    val transitions: List<WrappedAsset>,
    val selectedTransition: DesignBlockWithProperties?,
    val thumbnailsBaseUri: String,
    val hasTransitionsInTrack: Boolean,
) {
    companion object {
        const val SOURCE_ID = "ly.img.transitions"

        suspend fun create(
            outgoingBlock: DesignBlock,
            engine: Engine,
            locale: String,
        ): TransitionUiState {
            val assets = engine.asset.findAssets(
                sourceId = SOURCE_ID,
                query = FindAssetsQuery(perPage = Int.MAX_VALUE, page = 0, locale = locale),
            ).assets.map {
                WrappedAsset.GenericAsset(it, AssetSourceType(SOURCE_ID), AssetType.Transition, isNone = it.getMeta("type") == "none")
            }
            val transition = engine.block.getTransition(outgoingBlock)
            val selectedAsset = transition.takeIf(engine.block::isValid)?.let { block ->
                val type = engine.block.getType(block).substringAfterLast("/")
                assets.firstOrNull { it.asset.getMeta("type") == type }?.asset
            }
            val type = selectedAsset?.getMeta("type")?.takeIf { it != "none" }?.let { key ->
                TransitionType.values().firstOrNull { it.key.substringAfterLast('/') == key }
            }
            val selected = if (type != null) {
                DesignBlockWithProperties(
                    designBlock = transition,
                    objectType = type,
                    properties = selectedAsset.payload.properties?.toPropertyAndValueList(
                        engine = engine,
                        sourceId = SOURCE_ID,
                        asset = selectedAsset,
                    ).orEmpty(),
                    // legacy way. Delete it when decision is 100% made regarding the desired approach.
//                    properties = selectedAsset.payload.properties?.let {
//                        type.getAvailableProperties().combineWithValues(
//                            engine = engine,
//                            sourceId = SOURCE_ID,
//                            asset = selectedAsset,
//                            guidance = it,
//                        )
//                    }.orEmpty(),
                    asset = selectedAsset,
                )
            } else {
                null
            }
            val children = engine.block.getParent(outgoingBlock)?.let(engine.block::getChildren).orEmpty()
            val hasTransitions = children.any { child ->
                engine.block.supportsTransition(child) &&
                    engine.block.getTransition(child).let { assigned ->
                        engine.block.isValid(assigned) && engine.block.getType(assigned) != "//ly.img.ubq/transition/none"
                    }
            }
            return TransitionUiState(
                outgoingBlock = outgoingBlock,
                transitions = assets,
                selectedTransition = selected,
                thumbnailsBaseUri = "${engine.editor.getSettingString("basePath")}/ly.img.transitions/thumbnails",
                hasTransitionsInTrack = hasTransitions,
            )
        }
    }
}
