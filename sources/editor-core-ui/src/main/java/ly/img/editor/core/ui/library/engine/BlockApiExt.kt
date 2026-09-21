package ly.img.editor.core.ui.library.engine

import android.graphics.PointF
import android.graphics.RectF
import android.util.SizeF
import ly.img.engine.BlockApi
import ly.img.engine.DesignBlock
import ly.img.engine.FillType

fun BlockApi.isVideoBlock(designBlock: DesignBlock): Boolean {
    val fill = if (hasFill(designBlock)) getFill(designBlock) else null
    return fill != null && FillType.get(getType(fill)) is FillType.Video
}

fun BlockApi.setFrame(
    designBlock: DesignBlock,
    rect: RectF,
) {
    setSize(designBlock, SizeF(rect.width(), rect.height()))
    setPosition(designBlock, PointF(rect.left, rect.top))
}

fun BlockApi.setSize(
    designBlock: DesignBlock,
    size: SizeF,
) {
    setWidth(designBlock, size.width)
    setHeight(designBlock, size.height)
}

fun BlockApi.setPosition(
    designBlock: DesignBlock,
    point: PointF,
) {
    setPositionX(designBlock, point.x)
    setPositionY(designBlock, point.y)
}
