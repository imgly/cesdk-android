package ly.img.editor.base.ui.handler

import android.util.Log
import ly.img.editor.base.dock.options.crop.getNormalizedDegrees
import ly.img.editor.base.dock.options.crop.getRotationDegrees
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.core.ui.EventsHandler
import ly.img.editor.core.ui.engine.getScene
import ly.img.editor.core.ui.inject
import ly.img.editor.core.ui.register
import ly.img.engine.AssetTransformPreset
import ly.img.engine.ContentFillMode
import ly.img.engine.DesignBlock
import ly.img.engine.DesignUnit
import ly.img.engine.Engine

/**
 * Register events related to Crop.
 * @param engine Lambda returning the engine instance
 * @param block Lambda returning the block instance
 */
@Suppress("NAME_SHADOWING")
fun EventsHandler.cropEvents(
    engine: () -> Engine,
    block: () -> DesignBlock,
    bewarePageState: suspend (wrap: suspend () -> Unit) -> Unit,
) {
    val engine by inject(engine)
    val block by inject(block)

    fun onCropRotateDegrees(
        scaleRatio: Float,
        angle: Float,
        addUndo: Boolean = true,
    ) {
        val cropRotationRadians = angle * (Math.PI.toFloat() / 180f)
        engine.block.setCropRotation(block, cropRotationRadians)
        val contentFillMode = engine.block.getContentFillMode(block)
        if (contentFillMode == ContentFillMode.CROP) {
            engine.block.adjustCropToFillFrame(block, scaleRatio)
        }
        if (addUndo) {
            engine.editor.addUndoStep()
        }
    }

    register<BlockEvent.OnResetCrop> {
        engine.block.resetCrop(block)
        if (engine.block.supportsContentFillMode(block)) {
            engine.block.setContentFillMode(block, ContentFillMode.CROP)
        }
        engine.editor.addUndoStep()
    }
    register<BlockEvent.OnFlipCropHorizontal> {
        engine.block.flipCropHorizontal(block)
        engine.editor.addUndoStep()
    }
    register<BlockEvent.OnCropRotate> {
        val normalizedDegrees = getNormalizedDegrees(engine, block, offset = -90)
        onCropRotateDegrees(it.scaleRatio, normalizedDegrees)
    }
    register<BlockEvent.OnCropStraighten> {
        val rotationDegrees = getRotationDegrees(engine, block) + it.angle
        onCropRotateDegrees(it.scaleRatio, rotationDegrees, addUndo = false)
    }

    register<BlockEvent.OnReplaceCropPreset> {
        val cropAsset = it.wrappedAsset?.asset
        if (cropAsset != null) {
            bewarePageState {
                if (it.applyOnAllPages) {
                    engine.scene.getPages().forEach { page ->
                        engine.asset.defaultApplyAsset(
                            asset = cropAsset,
                            block = page,
                        )
                    }
                } else {
                    engine.asset.defaultApplyAsset(
                        asset = cropAsset,
                        block = block,
                    )
                }
                val scene = engine.getScene()
                when (val transformPreset = it.wrappedAsset.asset.payload.transformPreset) {
                    is AssetTransformPreset.FixedSize -> {
                        engine.block.setFloat(scene, "scene/pageDimensions/width", transformPreset.width)
                        engine.block.setFloat(scene, "scene/pageDimensions/height", transformPreset.height)
                        engine.block.setFloat(scene, "scene/dpi", if (transformPreset.designUnit == DesignUnit.PIXEL) 72f else 300f)
                    }
                    is AssetTransformPreset.FreeAspectRatio,
                    is AssetTransformPreset.FixedAspectRatio,
                    -> {
                        val reference = if (it.applyOnAllPages) {
                            requireNotNull(engine.scene.getPages().firstOrNull())
                        } else {
                            block
                        }
                        engine.block.setFloat(scene, "scene/pageDimensions/width", engine.block.getWidth(reference))
                        engine.block.setFloat(scene, "scene/pageDimensions/height", engine.block.getHeight(reference))
                    }
                    else -> {}
                }
                engine.editor.addUndoStep()
            }
        } else {
            Log.i("CropEventsHandler", "No crop asset provided for replacement")
        }
    }

    register<BlockEvent.OnChangeFillMode> {
        bewarePageState {
            engine.block.setContentFillMode(block, it.contentFillMode)
            engine.editor.addUndoStep()
        }
    }

    register<BlockEvent.OnChangePageSize> {
        bewarePageState {
            val width = it.width
            val height = it.height

            val scene = engine.getScene()
            engine.block.setFloat(scene, "scene/pageDimensions/width", width)
            engine.block.setFloat(scene, "scene/pageDimensions/height", height)
            engine.scene.setDesignUnit(it.unit)
            when (it.unit) {
                DesignUnit.INCH, DesignUnit.MILLIMETER -> {
                    engine.block.setFloat(scene, "scene/dpi", it.unitValue)
                }
                DesignUnit.PIXEL -> {
                    engine.block.setFloat(scene, "scene/pixelScaleFactor", it.unitValue)
                }
            }

            if (it.applyOnAllPages) {
                engine.scene.getPages().forEach { page ->
                    engine.block.setWidth(page, width)
                    engine.block.setHeight(page, height)
                }
                engine.block.resizeContentAware(engine.scene.getPages(), width, height)
            } else {
                engine.block.setWidth(block, width)
                engine.block.setHeight(block, height)
                engine.block.resizeContentAware(listOf(block), width, height)
            }

            engine.editor.addUndoStep()
        }
    }
}
