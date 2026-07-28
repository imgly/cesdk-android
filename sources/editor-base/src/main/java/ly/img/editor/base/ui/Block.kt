package ly.img.editor.base.ui

import ly.img.editor.core.component.data.Selection
import ly.img.editor.core.ui.engine.BlockKind
import ly.img.editor.core.ui.engine.BlockType
import ly.img.editor.core.ui.engine.getFillType
import ly.img.editor.core.ui.engine.getKindEnum
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import ly.img.engine.FillType

data class Block(
    val designBlock: DesignBlock,
    val type: BlockType,
)

/**
 * Returns null for design block types the editor UI does not support yet.
 */
internal fun createBlock(
    designBlock: DesignBlock,
    engine: Engine,
): Block? {
    val type = DesignBlockType.getOrNull(engine.block.getType(designBlock))
        ?.takeIf { it in Selection.supportedDesignBlockTypes } ?: return null
    val blockType = when (type) {
        DesignBlockType.Text -> BlockType.Text
        DesignBlockType.Group -> BlockType.Group
        DesignBlockType.Page -> BlockType.Page
        DesignBlockType.Audio -> BlockType.Audio
        DesignBlockType.Graphic -> {
            when (engine.block.getFillType(designBlock)) {
                FillType.Image -> {
                    val kind = engine.block.getKindEnum(designBlock)
                    if (kind == BlockKind.Sticker) BlockType.Sticker else BlockType.Image
                }
                FillType.Video -> BlockType.Video
                else -> BlockType.Shape
            }
        }
        else -> return null
    }
    return Block(
        designBlock = designBlock,
        type = blockType,
    )
}
