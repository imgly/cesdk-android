package ly.img.editor.base.ui.handler

import androidx.compose.ui.graphics.Color
import ly.img.editor.base.engine.effectiveTextRange
import ly.img.editor.base.engine.gradientColorStopsFrom
import ly.img.editor.base.engine.isCaption
import ly.img.editor.base.engine.setConicalGradientFill
import ly.img.editor.base.engine.setDefaultGradient
import ly.img.editor.base.engine.setFillType
import ly.img.editor.base.engine.setGradientColorAt
import ly.img.editor.base.engine.setLinearGradientRotation
import ly.img.editor.base.engine.setRadialGradientFill
import ly.img.editor.base.engine.toEngineColor
import ly.img.editor.base.ui.BlockEvent.OnChangeConicalGradientParams
import ly.img.editor.base.ui.BlockEvent.OnChangeFillColor
import ly.img.editor.base.ui.BlockEvent.OnChangeFillStyle
import ly.img.editor.base.ui.BlockEvent.OnChangeGradientFillColors
import ly.img.editor.base.ui.BlockEvent.OnChangeLinearGradientParams
import ly.img.editor.base.ui.BlockEvent.OnChangeRadialGradientParams
import ly.img.editor.base.ui.BlockEvent.OnDisableFill
import ly.img.editor.base.ui.BlockEvent.OnEnableFill
import ly.img.editor.core.ui.EventsHandler
import ly.img.editor.core.ui.engine.getFillType
import ly.img.editor.core.ui.engine.toRGBColor
import ly.img.editor.core.ui.inject
import ly.img.editor.core.ui.register
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import ly.img.engine.FillType
import ly.img.engine.GradientColorStop

@Suppress("NAME_SHADOWING")
fun EventsHandler.blockFillEvents(
    engine: () -> Engine,
    block: () -> DesignBlock,
) {
    val engine by inject(engine)
    val block by inject(block)

    fun enableFillIfDisabled() {
        if (!engine.block.isFillEnabled(block)) {
            engine.block.setFillEnabled(block, true)
        }
    }

    fun onChangeFillStyle(type: FillType) {
        enableFillIfDisabled()

        val currentFillType = engine.block.getFillType(block)
        if (currentFillType == type) return

        val colorStops = when (currentFillType) {
            FillType.Color -> gradientColorStopsFrom(engine.block.getFillSolidColor(block))

            FillType.LinearGradient,
            FillType.RadialGradient,
            FillType.ConicalGradient,
            -> {
                engine.block.getGradientColorStops(
                    engine.block.getFill(block),
                    "fill/gradient/colors",
                )
            }

            else -> {
                listOf(
                    GradientColorStop(0f, Color.White.toEngineColor()),
                    GradientColorStop(1f, Color.Black.toEngineColor()),
                )
            }
        }

        when (type) {
            FillType.Color -> {
                engine.block.setFillType(block, FillType.Color)
                engine.block.setFillSolidColor(
                    block,
                    colorStops.firstOrNull()?.color?.toRGBColor(engine) ?: Color.Black.toEngineColor(),
                )
            }
            FillType.LinearGradient,
            FillType.RadialGradient,
            FillType.ConicalGradient,
            -> engine.block.setDefaultGradient(engine.block.setFillType(block, type), type, colorStops)

            else -> throw UnsupportedOperationException("Fill type not supported")
        }

        engine.editor.addUndoStep()
    }

    register<OnChangeFillColor> {
        enableFillIfDisabled()
        engine.block.setFillType(block, FillType.Color)
        if (engine.block.isCaption(block) || DesignBlockType.getOrNull(engine.block.getType(block)) == DesignBlockType.Text) {
            val range = engine.block.effectiveTextRange(block)
            engine.block.setTextColor(block, it.color.toEngineColor(), range.first, range.last)
        } else {
            engine.block.setFillSolidColor(block, it.color.toEngineColor())
        }
    }

    register<OnChangeGradientFillColors> {
        enableFillIfDisabled()
        val fillType = engine.block.getFillType(block)
        if (fillType == FillType.ConicalGradient ||
            fillType == FillType.LinearGradient ||
            fillType == FillType.RadialGradient
        ) {
            engine.block.setGradientColorAt(engine.block.getFill(block), it.index, it.color.toEngineColor())
        } else {
            throw UnsupportedOperationException("Fill type is not a gradient fill")
        }
    }
    register<OnChangeLinearGradientParams> {
        engine.block.setLinearGradientRotation(engine.block.setFillType(block, FillType.LinearGradient), it.rotationInDegrees)
    }
    register<OnChangeRadialGradientParams> {
        engine.block.setRadialGradientFill(block, it.centerX, it.centerY, it.radius)
    }
    register<OnChangeConicalGradientParams> {
        engine.block.setConicalGradientFill(block, it.centerX, it.centerY)
    }
    register<OnDisableFill> {
        if (engine.block.isFillEnabled(block)) {
            engine.block.setFillEnabled(block, false)
            engine.editor.addUndoStep()
        }
    }
    register<OnEnableFill> {
        if (!engine.block.isFillEnabled(block)) {
            engine.block.setFillEnabled(block, true)
            engine.editor.addUndoStep()
        }
    }
    register<OnChangeFillStyle> {
        onChangeFillStyle(it.type)
    }
}
