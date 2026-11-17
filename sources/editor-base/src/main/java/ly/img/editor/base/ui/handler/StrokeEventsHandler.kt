package ly.img.editor.base.ui.handler

import ly.img.editor.base.engine.toEngineColor
import ly.img.editor.base.ui.BlockEvent.OnChangeStrokeColor
import ly.img.editor.base.ui.BlockEvent.OnChangeStrokeCornerGeometry
import ly.img.editor.base.ui.BlockEvent.OnChangeStrokePosition
import ly.img.editor.base.ui.BlockEvent.OnChangeStrokeStyle
import ly.img.editor.base.ui.BlockEvent.OnChangeStrokeWidth
import ly.img.editor.base.ui.BlockEvent.OnDisableStroke
import ly.img.editor.core.ui.EventsHandler
import ly.img.editor.core.ui.inject
import ly.img.editor.core.ui.register
import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import kotlin.math.exp

@Suppress("NAME_SHADOWING")
fun EventsHandler.strokeEvents(
    engine: () -> Engine,
    block: () -> DesignBlock,
) {
    val engine by inject(engine)
    val block by inject(block)

    register<OnChangeStrokeCornerGeometry> {
        if (engine.block.getStrokeCornerGeometry(block) != it.join) {
            engine.block.setStrokeCornerGeometry(block, it.join)
            engine.editor.addUndoStep()
        }
    }

    register<OnChangeStrokePosition> {
        if (engine.block.getStrokePosition(block) != it.position) {
            engine.block.setStrokePosition(block, it.position)
            engine.editor.addUndoStep()
        }
    }

    register<OnChangeStrokeStyle> {
        if (engine.block.getStrokeStyle(block) != it.style) {
            engine.block.setStrokeStyle(block, it.style)
            engine.editor.addUndoStep()
        }
    }

    register<OnChangeStrokeWidth> {
        engine.block.setStrokeWidth(block, exp(it.width.toDouble()).toFloat())
    }

    register<OnChangeStrokeColor> {
        engine.block.setStrokeEnabled(block, true)
        engine.block.setStrokeColor(block, it.color.toEngineColor())
    }
    register<OnDisableStroke> {
        val isEnabled = engine.block.isStrokeEnabled(block)
        if (isEnabled) {
            engine.block.setStrokeEnabled(block, false)
            engine.editor.addUndoStep()
        }
    }
}
