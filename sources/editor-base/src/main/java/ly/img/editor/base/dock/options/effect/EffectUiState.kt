package ly.img.editor.base.dock.options.effect

import androidx.annotation.StringRes
import ly.img.editor.base.engine.DesignBlockWithProperties
import ly.img.editor.base.engine.EffectGroup
import ly.img.editor.base.engine.combineWithValues
import ly.img.editor.base.engine.getGroup
import ly.img.editor.base.engine.getProperties
import ly.img.editor.core.R
import ly.img.editor.core.library.LibraryCategory
import ly.img.editor.core.ui.library.AppearanceAssetSourceType
import ly.img.editor.core.ui.library.AppearanceLibraryCategory
import ly.img.editor.core.ui.library.getMeta
import ly.img.editor.core.ui.library.state.WrappedAsset
import ly.img.engine.BlurType
import ly.img.engine.Color
import ly.img.engine.DesignBlock
import ly.img.engine.EffectType
import ly.img.engine.Engine

data class EffectUiState(
    @StringRes val titleRes: Int,
    val selectedAssetKey: String?,
    val effect: DesignBlockWithProperties?,
    val libraryCategory: LibraryCategory,
) {
    fun getAssetKey(asset: WrappedAsset): String? {
        return when (asset.assetSourceType) {
            AppearanceAssetSourceType.LutFilter -> {
                asset.asset.id
            }
            AppearanceAssetSourceType.DuoToneFilter -> {
                val darkColor = asset.asset.getMeta("darkColor") ?: return null
                val lightColor = asset.asset.getMeta("lightColor") ?: return null
                buildDuotoneUri(
                    darkColor = Color.fromHex(darkColor),
                    lightColor = Color.fromHex(lightColor),
                )
            }
            AppearanceAssetSourceType.FxEffect -> {
                asset.asset.getMeta("effectType")
            }
            AppearanceAssetSourceType.Blur -> {
                asset.asset.getMeta("blurType")
            }
            else -> null
        }
    }

    companion object {
        private fun getBlur(
            engine: Engine,
            designBlock: DesignBlock,
        ): Pair<DesignBlock, BlurType>? {
            if (engine.block.isBlurEnabled(designBlock).not()) return null
            val blur = engine.block.getBlur(designBlock)
            blur.takeIf { engine.block.isValid(it) } ?: return null
            val blurType = BlurType.getOrNull(engine.block.getType(blur)) ?: return null
            return blur to blurType
        }

        private fun getEffect(
            engine: Engine,
            designBlock: DesignBlock,
            isFilter: Boolean,
        ): Pair<DesignBlock, EffectType>? {
            val neededGroup = if (isFilter) EffectGroup.Filter else EffectGroup.FxEffect
            engine.block.getEffects(designBlock).forEach { effect ->
                val effectType = EffectType.getOrNull(engine.block.getType(effect))
                if (effectType != null && effectType.getGroup() == neededGroup) {
                    return effect to effectType
                }
            }
            return null
        }

        private fun getEffectUri(
            engine: Engine,
            effect: DesignBlockWithProperties?,
        ): String? {
            effect ?: return null
            return when (val filterType = effect.objectType) {
                EffectType.DuoToneFilter -> {
                    val darkColor = engine.block.getColor(effect.designBlock, "${filterType.key}/darkColor")
                    val lightColor = engine.block.getColor(effect.designBlock, "${filterType.key}/lightColor")
                    buildDuotoneUri(darkColor, lightColor)
                }
                EffectType.LutFilter -> {
                    val filterId = engine.block.getString(effect.designBlock, "${filterType.key}/filterId")
                    filterId.ifEmpty { null }
                }
                else -> filterType.key
            }
        }

        private fun buildDuotoneUri(
            darkColor: Color,
            lightColor: Color,
        ): String = "DuotoneFilter:$darkColor:$lightColor"

        /**
         * Create a EffectUiState instance based on the given Block and Engine.
         */
        fun create(
            designBlock: DesignBlock,
            engine: Engine,
            libraryCategory: LibraryCategory,
        ): EffectUiState {
            val effect = if (libraryCategory == AppearanceLibraryCategory.Blur) {
                getBlur(
                    engine = engine,
                    designBlock = designBlock,
                )?.let { (blur, blurType) ->
                    DesignBlockWithProperties(
                        designBlock = blur,
                        objectType = blurType,
                        properties = blurType.getProperties().combineWithValues(engine, blur),
                    )
                }
            } else {
                getEffect(
                    engine = engine,
                    designBlock = designBlock,
                    isFilter = libraryCategory == AppearanceLibraryCategory.Filters,
                )?.let { (effect, effectType) ->
                    DesignBlockWithProperties(
                        designBlock = effect,
                        objectType = effectType,
                        properties = effectType.getProperties().combineWithValues(engine, effect),
                    )
                }
            }
            val titleRes = when (libraryCategory) {
                AppearanceLibraryCategory.FxEffects -> R.string.ly_img_editor_sheet_effect_title
                AppearanceLibraryCategory.Filters -> R.string.ly_img_editor_sheet_filter_title
                AppearanceLibraryCategory.Blur -> R.string.ly_img_editor_sheet_blur_title
                else -> throw IllegalArgumentException("Unsupported library category: $libraryCategory")
            }
            return EffectUiState(
                titleRes = titleRes,
                selectedAssetKey = getEffectUri(engine, effect),
                effect = effect,
                libraryCategory = libraryCategory,
            )
        }
    }
}
