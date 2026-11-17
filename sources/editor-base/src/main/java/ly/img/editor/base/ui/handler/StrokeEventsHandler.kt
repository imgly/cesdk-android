package ly.img.editor.base.ui.handler

import ly.img.editor.base.engine.gradientColorStopsFrom
import ly.img.editor.base.engine.setDefaultGradient
import ly.img.editor.base.engine.setGradientColorAt
import ly.img.editor.base.engine.setLinearGradientRotation
import ly.img.editor.base.engine.toEngineColor
import ly.img.editor.base.ui.BlockEvent.OnChangeStrokeColor
import ly.img.editor.base.ui.BlockEvent.OnChangeStrokeCornerGeometry
import ly.img.editor.base.ui.BlockEvent.OnChangeStrokeFillStyle
import ly.img.editor.base.ui.BlockEvent.OnChangeStrokeGradientColors
import ly.img.editor.base.ui.BlockEvent.OnChangeStrokeLinearGradientParams
import ly.img.editor.base.ui.BlockEvent.OnChangeStrokePosition
import ly.img.editor.base.ui.BlockEvent.OnChangeStrokeStyle
import ly.img.editor.base.ui.BlockEvent.OnChangeStrokeWidth
import ly.img.editor.base.ui.BlockEvent.OnDisableStroke
import ly.img.editor.core.ui.EventsHandler
import ly.img.editor.core.ui.engine.canPaintStrokeWithFill
import ly.img.editor.core.ui.engine.toRGBColor
import ly.img.editor.core.ui.inject
import ly.img.editor.core.ui.register
import ly.img.engine.Color
import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import ly.img.engine.FillType

@Suppress("NAME_SHADOWING")
fun EventsHandler.strokeEvents(
    engine: () -> Engine,
    block: () -> DesignBlock,
) {
    val engine by inject(engine)
    val block by inject(block)

    fun getStrokeFill(): DesignBlock? = if (engine.block.canPaintStrokeWithFill(block)) engine.block.getStrokeFill(block) else null

    // The stroke color paints the stroke only while no fill paints it.
    fun clearStrokeFill() {
        val strokeFill = getStrokeFill() ?: return
        engine.block.setStrokeFill(block, null)
        engine.block.destroy(strokeFill)
    }

    fun onChangeStrokeFillStyle(type: FillType) {
        val wasEnabled = engine.block.isStrokeEnabled(block)
        engine.block.setStrokeEnabled(block, true)
        val strokeFill = getStrokeFill()
        val currentType = if (strokeFill == null) FillType.Color else FillType.getOrNull(engine.block.getType(strokeFill))
        val gradientStops = strokeFill?.takeIf { currentType in GRADIENT_FILL_TYPES }?.let {
            engine.block.getGradientColorStops(it, "fill/gradient/colors")
        }
        var changed = !wasEnabled
        if (type == FillType.Color) {
            if (strokeFill != null) {
                gradientStops?.firstOrNull()?.let { engine.block.setStrokeColor(block, it.color) }
                clearStrokeFill()
                changed = true
            }
        } else if (currentType != type) {
            // A stroke color with alpha 0 hides a stroke fill too, otherwise the gradient never shows.
            val strokeColor = engine.block.getStrokeColor(block).toRGBColor(engine)
            val opaqueColor = if (strokeColor.a == 0f) {
                Color.fromRGBA(r = strokeColor.r, g = strokeColor.g, b = strokeColor.b, a = 1f)
            } else {
                strokeColor
            }
            if (opaqueColor != strokeColor) engine.block.setStrokeColor(block, opaqueColor)
            val newFill = engine.block.createFill(type)
            engine.block.setDefaultGradient(newFill, type, gradientStops ?: gradientColorStopsFrom(opaqueColor))
            engine.block.setStrokeFill(block, newFill)
            strokeFill?.let { engine.block.destroy(it) }
            changed = true
        }
        if (changed) {
            engine.editor.addUndoStep()
        }
    }

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
        engine.block.setStrokeWidth(block, it.width)
    }

    register<OnChangeStrokeColor> {
        engine.block.setStrokeEnabled(block, true)
        clearStrokeFill()
        engine.block.setStrokeColor(block, it.color.toEngineColor())
    }

    register<OnChangeStrokeFillStyle> {
        onChangeStrokeFillStyle(it.type)
    }

    // An undo can remove the stroke fill while its color picker is open, so a later edit has no fill to change.
    register<OnChangeStrokeGradientColors> {
        getStrokeFill()?.let { fill -> engine.block.setGradientColorAt(fill, it.index, it.color.toEngineColor()) }
    }

    register<OnChangeStrokeLinearGradientParams> {
        getStrokeFill()?.let { fill -> engine.block.setLinearGradientRotation(fill, it.rotationInDegrees) }
    }
    register<OnDisableStroke> {
        val isEnabled = engine.block.isStrokeEnabled(block)
        if (isEnabled) {
            engine.block.setStrokeEnabled(block, false)
            engine.editor.addUndoStep()
        }
    }
}

private val GRADIENT_FILL_TYPES = setOf(FillType.LinearGradient, FillType.RadialGradient, FillType.ConicalGradient)
