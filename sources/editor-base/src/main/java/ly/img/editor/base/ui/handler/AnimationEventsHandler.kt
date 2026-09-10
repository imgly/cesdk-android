package ly.img.editor.base.ui.handler

import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.core.ui.EventsHandler
import ly.img.editor.core.ui.inject
import ly.img.editor.core.ui.register
import ly.img.engine.Engine

@Suppress("NAME_SHADOWING")
fun EventsHandler.animationEvents(engine: () -> Engine) {
    val engine by inject(engine)

    register<BlockEvent.OnReplaceAnimation> {
        engine.asset.applyAssetSourceAsset(sourceId = it.sourceId, asset = it.asset)
    }
}
