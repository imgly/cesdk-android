package ly.img.editor.base.dock.options.animation

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import ly.img.editor.base.R
import ly.img.editor.base.components.TabItem
import ly.img.editor.base.engine.DesignBlockWithProperties
import ly.img.editor.base.engine.getAvailableProperties
import ly.img.editor.base.engine.toPropertyAndValueList
import ly.img.editor.core.library.AssetType
import ly.img.editor.core.library.data.AssetSourceType
import ly.img.editor.core.ui.library.getMeta
import ly.img.editor.core.ui.library.state.WrappedAsset
import ly.img.engine.AnimationType
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import ly.img.engine.FindAssetsQuery
import ly.img.engine.defaultAssetSourcesBaseUri

@Immutable
data class AnimationUiState(
    val categories: List<TabItem<Category>>,
) {
    data class Category(
        val sourceId: String,
        val group: String,
        val animations: List<WrappedAsset>,
        val selectedAnimation: DesignBlockWithProperties?,
        val thumbnailsBaseUri: String,
    )

    companion object {
        private const val ANIMATIONS_SOURCE_ID = "ly.img.animations"

        private suspend fun getTabItem(
            @StringRes titleRes: Int,
            thumbnailsBaseUri: String,
            engine: Engine,
            group: String,
            animationDesignBlock: DesignBlock,
        ): TabItem<Category> {
            val animations = engine.asset.findAssets(
                sourceId = ANIMATIONS_SOURCE_ID,
                query = FindAssetsQuery(
                    perPage = Int.MAX_VALUE,
                    page = 0,
                    groups = listOf(group),
                    locale = "en",
                ),
            ).assets.map {
                WrappedAsset.GenericAsset(
                    asset = it,
                    assetSourceType = AssetSourceType(ANIMATIONS_SOURCE_ID),
                    assetType = AssetType.Animation,
                    isNone = it.getMeta("type") == "none",
                )
            }
            val selectedAnimation = animations.firstOrNull { it.asset.active }?.asset
            val animationType = selectedAnimation?.getMeta("type")?.let { type ->
                AnimationType.values().firstOrNull { it.key.endsWith(type) }
            }
            return TabItem(
                titleRes = titleRes,
                isSmallIndicatorOn = animationType != null,
                data = Category(
                    sourceId = ANIMATIONS_SOURCE_ID,
                    group = group,
                    animations = animations,
                    selectedAnimation = animationType?.let {
                        DesignBlockWithProperties(
                            designBlock = animationDesignBlock,
                            objectType = animationType,
                            properties = selectedAnimation.payload.properties?.toPropertyAndValueList(
                                engine = engine,
                                sourceId = ANIMATIONS_SOURCE_ID,
                                asset = selectedAnimation,
                                availableProperties = animationType.getAvailableProperties(),
                            ) ?: emptyList(),
                            asset = selectedAnimation,
                        )
                    },
                    thumbnailsBaseUri = thumbnailsBaseUri,
                ),
            )
        }

        suspend fun create(
            designBlock: DesignBlock,
            engine: Engine,
        ): AnimationUiState {
            val isTextBlock = engine.block.getType(designBlock) == DesignBlockType.Text.key
            val defaultAssetSourcesBaseUri = engine.defaultAssetSourcesBaseUri
            val thumbnailsBaseUri = if (isTextBlock) {
                "$defaultAssetSourcesBaseUri/ly.img.animation.text/thumbnails"
            } else {
                "$defaultAssetSourcesBaseUri/ly.img.animation/thumbnails"
            }
            return AnimationUiState(
                categories = listOf(
                    getTabItem(
                        titleRes = R.string.ly_img_editor_animation_in,
                        thumbnailsBaseUri = thumbnailsBaseUri,
                        group = "in",
                        animationDesignBlock = engine.block.getInAnimation(designBlock),
                        engine = engine,
                    ),
                    getTabItem(
                        titleRes = R.string.ly_img_editor_animation_loop,
                        thumbnailsBaseUri = thumbnailsBaseUri,
                        group = "loop",
                        animationDesignBlock = engine.block.getLoopAnimation(designBlock),
                        engine = engine,
                    ),
                    getTabItem(
                        titleRes = R.string.ly_img_editor_animation_out,
                        thumbnailsBaseUri = thumbnailsBaseUri,
                        group = "out",
                        animationDesignBlock = engine.block.getOutAnimation(designBlock),
                        engine = engine,
                    ),
                ),
            )
        }
    }
}
