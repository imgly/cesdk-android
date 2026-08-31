package ly.img.editor.base.dock.options.animation

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import ly.img.editor.base.components.TabItem
import ly.img.editor.base.engine.DesignBlockWithProperties
import ly.img.editor.base.engine.toPropertyAndValueList
import ly.img.editor.core.R
import ly.img.editor.core.library.AssetType
import ly.img.editor.core.library.data.AssetSourceType
import ly.img.editor.core.ui.library.getMeta
import ly.img.editor.core.ui.library.state.WrappedAsset
import ly.img.engine.AnimationType
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import ly.img.engine.FindAssetsQuery

@Immutable
data class AnimationUiState(
    val categories: List<TabItem<Category>>,
) {
    data class Category(
        val designBlock: DesignBlock,
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
            designBlock: DesignBlock,
            engine: Engine,
            group: String,
            animationDesignBlock: DesignBlock,
            locale: String,
        ): TabItem<Category> {
            val animations = engine.asset.findAssets(
                sourceId = ANIMATIONS_SOURCE_ID,
                query = FindAssetsQuery(
                    perPage = Int.MAX_VALUE,
                    page = 0,
                    groups = listOf(group),
                    locale = locale,
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
                    designBlock = designBlock,
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
                            ).orEmpty(),
                            // legacy way. Delete it when decision is 100% made regarding the desired approach.
//                            properties = selectedAnimation.payload.properties?.let {
//                                animationType.getAvailableProperties().combineWithValues(
//                                    engine = engine,
//                                    sourceId = ANIMATIONS_SOURCE_ID,
//                                    asset = selectedAnimation,
//                                    guidance = it,
//                                )
//                            }.orEmpty(),
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
            locale: String,
        ): AnimationUiState {
            val type = engine.block.getType(designBlock)
            val isTextBlock = type == DesignBlockType.Text.key
            // Resolve from the editor's configured base path (the single source of truth, set via
            // EditorUiSettings). engine.defaultAssetSourcesBaseUri is null once an app registers
            // sources via Engine.asset.addLocalSourceFromJSON instead of addDefaultAssetSources.
            val basePath = engine.editor.getSettingString("basePath")
            val thumbnailsBaseUri = if (isTextBlock) {
                "$basePath/ly.img.animation.text/thumbnails"
            } else {
                "$basePath/ly.img.animation/thumbnails"
            }
            return AnimationUiState(
                categories = listOf(
                    getTabItem(
                        titleRes = R.string.ly_img_editor_sheet_animations_tab_in,
                        thumbnailsBaseUri = thumbnailsBaseUri,
                        designBlock = designBlock,
                        group = "in",
                        animationDesignBlock = engine.block.getInAnimation(designBlock),
                        engine = engine,
                        locale = locale,
                    ),
                    getTabItem(
                        titleRes = R.string.ly_img_editor_sheet_animations_tab_loop,
                        thumbnailsBaseUri = thumbnailsBaseUri,
                        designBlock = designBlock,
                        group = "loop",
                        animationDesignBlock = engine.block.getLoopAnimation(designBlock),
                        engine = engine,
                        locale = locale,
                    ),
                    getTabItem(
                        titleRes = R.string.ly_img_editor_sheet_animations_tab_out,
                        thumbnailsBaseUri = thumbnailsBaseUri,
                        designBlock = designBlock,
                        group = "out",
                        animationDesignBlock = engine.block.getOutAnimation(designBlock),
                        engine = engine,
                        locale = locale,
                    ),
                ),
            )
        }
    }
}
