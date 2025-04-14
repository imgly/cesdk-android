package ly.img.editor.base.dock.options.adjustment

import ly.img.editor.base.engine.DesignBlockWithProperties
import ly.img.editor.base.engine.combineWithValues
import ly.img.editor.base.engine.findEffect
import ly.img.editor.base.engine.getProperties
import ly.img.engine.DesignBlock
import ly.img.engine.EffectType
import ly.img.engine.Engine

data class AdjustmentUiState(
    val designBlockWithProperties: DesignBlockWithProperties,
) {
    companion object {
        fun create(
            designBlock: DesignBlock,
            engine: Engine,
        ): AdjustmentUiState {
            // If EffectType.Adjustments effect does not exist yet create and append it
            val effectDesignBlock = engine.block.findEffect(designBlock, EffectType.Adjustments) ?: run {
                val effect = engine.block.createEffect(EffectType.Adjustments)
                engine.block.appendEffect(block = designBlock, effectBlock = effect)
                effect
            }
            return AdjustmentUiState(
                designBlockWithProperties = DesignBlockWithProperties(
                    designBlock = effectDesignBlock,
                    objectType = EffectType.Adjustments,
                    properties = EffectType.Adjustments
                        .getProperties()
                        .combineWithValues(engine, effectDesignBlock),
                ),
            )
        }
    }
}
