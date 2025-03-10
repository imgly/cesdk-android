package ly.img.editor.core.component.data

import android.graphics.RectF
import androidx.compose.runtime.Stable
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import ly.img.engine.FillType

/**
 * A class containing information on the current selection in the editor.
 *
 * @param designBlock the design block that is currently selected.
 * @param parentDesignBlock the parent design block of the [designBlock].
 * @param type the type of the [designBlock].
 * @param fillType the optional fill type of the [designBlock].
 * @param kind the kind of the [designBlock].
 * @param indexInParent the index of the [designBlock] in [parentDesignBlock]. If [parentDesignBlock] is null, the value is -1.
 * @param screenSpaceBoundingBoxRect the rect of the [designBlock] in screen space.
 * @param isVisibleAtCurrentPlaybackTime whether the [designBlock] is visible at current playback time.
 * The value is always true for [ly.img.engine.SceneMode.DESIGN] scenes.
 */
@Stable
data class Selection(
    val designBlock: DesignBlock,
    val parentDesignBlock: DesignBlock?,
    val type: DesignBlockType,
    val fillType: FillType?,
    val kind: String?,
    val indexInParent: Int,
    val screenSpaceBoundingBoxRect: RectF,
    val isVisibleAtCurrentPlaybackTime: Boolean,
) {
    companion object {
        /**
         * Default implementation of [Selection] for a given [designBlock].
         *
         * @param engine the engine of the current editor.
         * @param designBlock the design block for which [Selection] should be constructed.
         */
        fun getDefault(
            engine: Engine,
            designBlock: DesignBlock,
        ): Selection {
            val parentDesignBlock = engine.block.getParent(designBlock)
            return Selection(
                designBlock = designBlock,
                parentDesignBlock = parentDesignBlock,
                type = DesignBlockType.get(engine.block.getType(designBlock)),
                fillType = if (engine.block.supportsFill(designBlock)) {
                    FillType.get(engine.block.getType(engine.block.getFill(designBlock)))
                } else {
                    null
                },
                kind = engine.block.getKind(designBlock),
                indexInParent = parentDesignBlock?.let { engine.block.getChildren(it).indexOf(designBlock) } ?: -1,
                screenSpaceBoundingBoxRect = engine.block.getScreenSpaceBoundingBoxRect(listOf(designBlock)),
                isVisibleAtCurrentPlaybackTime = engine.block.isVisibleAtCurrentPlaybackTime(designBlock),
            )
        }
    }
}
