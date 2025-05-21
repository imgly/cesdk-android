package ly.img.editor.base.ui.handler

import ly.img.editor.base.engine.Property
import ly.img.editor.base.engine.PropertyValue
import ly.img.editor.base.engine.PropertyValueType
import ly.img.editor.base.engine.delete
import ly.img.editor.base.engine.duplicate
import ly.img.editor.base.engine.toEngineColor
import ly.img.editor.base.ui.BlockEvent.OnBackward
import ly.img.editor.base.ui.BlockEvent.OnBackwardNonSelected
import ly.img.editor.base.ui.BlockEvent.OnChangeBlendMode
import ly.img.editor.base.ui.BlockEvent.OnChangeFinish
import ly.img.editor.base.ui.BlockEvent.OnChangeOpacity
import ly.img.editor.base.ui.BlockEvent.OnChangeProperty
import ly.img.editor.base.ui.BlockEvent.OnDelete
import ly.img.editor.base.ui.BlockEvent.OnDeleteNonSelected
import ly.img.editor.base.ui.BlockEvent.OnDuplicate
import ly.img.editor.base.ui.BlockEvent.OnDuplicateNonSelected
import ly.img.editor.base.ui.BlockEvent.OnForward
import ly.img.editor.base.ui.BlockEvent.OnForwardNonSelected
import ly.img.editor.base.ui.BlockEvent.ToBack
import ly.img.editor.base.ui.BlockEvent.ToFront
import ly.img.editor.core.ui.EventsHandler
import ly.img.editor.core.ui.inject
import ly.img.editor.core.ui.register
import ly.img.engine.AssetBooleanProperty
import ly.img.engine.AssetColorProperty
import ly.img.engine.AssetDoubleProperty
import ly.img.engine.AssetEnumProperty
import ly.img.engine.AssetFloatProperty
import ly.img.engine.AssetIntProperty
import ly.img.engine.AssetStringProperty
import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import ly.img.engine.UnstableEngineApi

/**
 * Register events related to DesignBlocks.
 * @param engine Lambda returning the engine instance
 * @param block Lambda returning the block instance
 */
@Suppress("NAME_SHADOWING")
fun EventsHandler.blockEvents(
    engine: () -> Engine,
    block: () -> DesignBlock,
) {
    val engine by inject(engine)
    val block by inject(block)

    fun onBackward(designBlock: DesignBlock) {
        engine.block.sendBackward(designBlock)
        engine.editor.addUndoStep()
    }

    fun onForward(designBlock: DesignBlock) {
        engine.block.bringForward(designBlock)
        engine.editor.addUndoStep()
    }

    register<OnDelete> { engine.delete(block) }

    register<OnDeleteNonSelected> { engine.delete(it.block) }

    register<OnDuplicate> { engine.duplicate(block) }

    register<OnDuplicateNonSelected> {
        engine.block.duplicate(it.block)
        engine.editor.addUndoStep()
    }

    register<OnBackward> {
        onBackward(block)
    }

    register<OnBackwardNonSelected> {
        onBackward(it.block)
    }

    register<OnForward> {
        onForward(block)
    }

    register<OnForwardNonSelected> {
        onForward(it.block)
    }

    register<ToBack> {
        engine.block.sendToBack(block)
        engine.editor.addUndoStep()
    }

    register<ToFront> {
        engine.block.bringToFront(block)
        engine.editor.addUndoStep()
    }

    register<OnChangeFinish> { engine.editor.addUndoStep() }

    register<OnChangeBlendMode> {
        if (engine.block.getBlendMode(block) != it.blendMode) {
            engine.block.setBlendMode(block, it.blendMode)
            engine.editor.addUndoStep()
        }
    }

    register<OnChangeOpacity> { engine.block.setOpacity(block, it.opacity) }

    register<OnChangeProperty> {
        it.property.keys.forEach { key ->
            onChangeProperty(
                designBlock = it.designBlock,
                property = it.property,
                key = key,
                newValue = it.newValue,
                engine = engine,
            )
        }
    }
}

@OptIn(UnstableEngineApi::class)
private suspend fun onChangeProperty(
    designBlock: DesignBlock,
    property: Property,
    key: String,
    newValue: PropertyValue,
    engine: Engine,
) {
    when (newValue) {
        is PropertyValue.Int -> {
            property.assetData?.let { assetData ->
                engine.asset.applyAssetSourceProperty(
                    sourceId = assetData.sourceId,
                    asset = assetData.asset,
                    property = (assetData.assetProperty as AssetIntProperty).copy(value = newValue.value),
                )
            } ?: run {
                engine.block.setInt(
                    block = designBlock,
                    property = key,
                    value = newValue.value,
                )
            }
        }
        is PropertyValue.Float -> {
            property.assetData?.let { assetData ->
                engine.asset.applyAssetSourceProperty(
                    sourceId = assetData.sourceId,
                    asset = assetData.asset,
                    property = (assetData.assetProperty as AssetFloatProperty).copy(value = newValue.value),
                )
            } ?: run {
                engine.block.setFloat(
                    block = designBlock,
                    property = key,
                    value = newValue.value,
                )
            }
        }
        is PropertyValue.Double -> {
            property.assetData?.let { assetData ->
                engine.asset.applyAssetSourceProperty(
                    sourceId = assetData.sourceId,
                    asset = assetData.asset,
                    property = (assetData.assetProperty as AssetDoubleProperty).copy(value = newValue.value),
                )
            } ?: run {
                engine.block.setDouble(
                    block = designBlock,
                    property = key,
                    value = newValue.value,
                )
            }
        }
        is PropertyValue.Boolean -> {
            property.assetData?.let { assetData ->
                engine.asset.applyAssetSourceProperty(
                    sourceId = assetData.sourceId,
                    asset = assetData.asset,
                    property = (assetData.assetProperty as AssetBooleanProperty).copy(value = newValue.value),
                )
            } ?: run {
                engine.block.setBoolean(
                    block = designBlock,
                    property = key,
                    value = newValue.value,
                )
            }
        }
        is PropertyValue.Color -> {
            property.assetData?.let { assetData ->
                engine.asset.applyAssetSourceProperty(
                    sourceId = assetData.sourceId,
                    asset = assetData.asset,
                    property = (assetData.assetProperty as AssetColorProperty).copy(value = requireNotNull(newValue.value).toEngineColor()),
                )
            } ?: run {
                val enabledPropertyKey = (property.valueType as PropertyValueType.Color).enabledPropertyKey
                when {
                    newValue.value == null && enabledPropertyKey != null -> {
                        engine.block.setBoolean(
                            block = designBlock,
                            property = enabledPropertyKey,
                            value = false,
                        )
                    }
                    newValue.value != null -> {
                        if (enabledPropertyKey != null) {
                            engine.block.setBoolean(
                                block = designBlock,
                                property = enabledPropertyKey,
                                value = true,
                            )
                        }
                        engine.block.setColor(
                            block = designBlock,
                            property = key,
                            value = newValue.value.toEngineColor(),
                        )
                    }
                }
            }
        }
        is PropertyValue.String -> {
            property.assetData?.let { assetData ->
                engine.asset.applyAssetSourceProperty(
                    sourceId = assetData.sourceId,
                    asset = assetData.asset,
                    property = (assetData.assetProperty as AssetStringProperty).copy(value = newValue.value),
                )
            } ?: run {
                engine.block.setString(
                    block = designBlock,
                    property = key,
                    value = newValue.value,
                )
            }
        }
        is PropertyValue.Enum -> {
            property.assetData?.let { assetData ->
                engine.asset.applyAssetSourceProperty(
                    sourceId = assetData.sourceId,
                    asset = assetData.asset,
                    property = (assetData.assetProperty as AssetEnumProperty).copy(value = newValue.value),
                )
            } ?: run {
                engine.block.setEnum(
                    block = designBlock,
                    property = key,
                    value = newValue.value,
                )
            }
        }
    }
}
