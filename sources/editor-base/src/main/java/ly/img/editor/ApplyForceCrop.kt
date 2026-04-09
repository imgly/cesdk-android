package ly.img.editor

import ly.img.editor.core.event.EditorEvent
import ly.img.engine.DesignBlock

/**
 * An event for applying a force crop configuration to a specific [DesignBlock].
 */
class ApplyForceCrop(
    val block: DesignBlock,
    val configuration: ForceCropConfiguration,
) : EditorEvent
