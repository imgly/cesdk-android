package ly.img.editor.base.ui.handler

import ly.img.editor.base.engine.toEngineColor
import ly.img.editor.base.ui.BlockEvent.OnChangeStrokeColor
import ly.img.editor.base.ui.BlockEvent.OnChangeStrokeJoin
import ly.img.editor.base.ui.BlockEvent.OnChangeStrokePosition
import ly.img.editor.base.ui.BlockEvent.OnChangeStrokeStyle
import ly.img.editor.base.ui.BlockEvent.OnChangeStrokeWidth
import ly.img.editor.base.ui.BlockEvent.OnDisableStroke
import ly.img.editor.core.ui.EventsHandler
import ly.img.editor.core.ui.inject
import ly.img.editor.core.ui.register
import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import ly.img.engine.StrokeCap
import ly.img.engine.StrokeCornerGeometry
import ly.img.engine.StrokePosition
import ly.img.engine.StrokeStyle
import kotlin.math.exp

@Suppress("NAME_SHADOWING")
fun EventsHandler.strokeEvents(
    engine: () -> Engine,
    block: () -> DesignBlock,
) {
    val engine by inject(engine)
    val block by inject(block)

    register<OnChangeStrokeJoin> {
        val strokeJoinEnum = StrokeCornerGeometry.valueOf(it.join)
        if (engine.block.getStrokeCornerGeometry(block) != strokeJoinEnum) {
            engine.block.setStrokeCornerGeometry(block, strokeJoinEnum)
            engine.editor.addUndoStep()
        }
    }

    register<OnChangeStrokePosition> {
        val strokePositionEnum = StrokePosition.valueOf(it.position)
        if (engine.block.getStrokePosition(block) != strokePositionEnum) {
            engine.block.setStrokePosition(block, strokePositionEnum)
            engine.editor.addUndoStep()
        }
    }

    register<OnChangeStrokeStyle> {
        val strokeStyleEnum = StrokeStyle.valueOf(it.style)
        if (engine.block.getStrokeStyle(block) != strokeStyleEnum) {
            engine.block.setStrokeStyle(block, strokeStyleEnum)
            // Apply the cap the preset implies, like the web editor. Dotted/*Round presets need
            // a Round cap, else a Dotted stroke is invisible (ANDROID-814). All four caps are set
            // equal for the renderer's fast path; the dash pattern comes from the style preset.
            val cap = strokeStyleEnum.presetCap()
            engine.block.setStrokeStartCap(block, cap)
            engine.block.setStrokeEndCap(block, cap)
            engine.block.setStrokeDashStartCap(block, cap)
            engine.block.setStrokeDashEndCap(block, cap)
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

// Round-cap presets (Dotted, DashedRound, LongDashedRound) imply round dash/line caps, matching
// the web editor and the engine's preset rendering; the rest use a butt cap.
private fun StrokeStyle.presetCap(): StrokeCap = when (this) {
    StrokeStyle.DOTTED, StrokeStyle.DASHED_ROUND, StrokeStyle.LONG_DASHED_ROUND -> StrokeCap.ROUND
    else -> StrokeCap.BUTT
}
