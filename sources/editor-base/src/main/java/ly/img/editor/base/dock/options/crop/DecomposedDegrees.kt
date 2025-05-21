package ly.img.editor.base.dock.options.crop

import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import kotlin.math.roundToInt

fun getNormalizedDegrees(
    engine: Engine,
    designBlock: DesignBlock,
    offset: Int = 0,
): Int {
    val cropRotationDegrees = engine.block.getCropRotation(designBlock) * (180f / Math.PI.toFloat())
    val cropRotatedDegrees = cropRotationDegrees + offset
    var normalizedDegrees = cropRotatedDegrees % 360
    if (normalizedDegrees < 0) normalizedDegrees += 360
    return normalizedDegrees.roundToInt()
}

fun getRotationDegrees(
    engine: Engine,
    designBlock: DesignBlock,
): Int = getDecomposedDegrees(engine, designBlock).first

fun getStraightenDegrees(
    engine: Engine,
    designBlock: DesignBlock,
): Int = getDecomposedDegrees(engine, designBlock).second

private fun getDecomposedDegrees(
    engine: Engine,
    designBlock: DesignBlock,
): Pair<Int, Int> {
    val normalizedDegrees = getNormalizedDegrees(engine, designBlock)
    var rotationCounts = (normalizedDegrees / 90)
    var rotationDeg = 0

    fun straightenDegrees(): Int {
        rotationDeg = rotationCounts * 90
        return normalizedDegrees - rotationDeg
    }

    var straightenDeg = straightenDegrees()
    if (straightenDeg > 45) {
        rotationCounts += 1
        straightenDeg = straightenDegrees()
    }
    return rotationDeg to straightenDeg
}
