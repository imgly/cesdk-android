package ly.img.editor.base.ui.handler

import ly.img.editor.base.dock.options.captions.CaptionsEngine
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.core.ui.EventsHandler
import ly.img.editor.core.ui.inject
import ly.img.editor.core.ui.register
import ly.img.engine.Engine

/** Caption styling, routed through [CaptionsEngine] so the engine's caption rules live in one place. */
@Suppress("NAME_SHADOWING")
internal fun EventsHandler.captionEvents(
    engine: () -> Engine,
    onError: (Throwable) -> Unit,
) {
    val engine by inject(engine)

    register<BlockEvent.OnApplyCaptionPreset> {
        val captionsEngine = CaptionsEngine(engine, onError)
        // Any caption will do: the engine fans the style out to its siblings.
        val caption = captionsEngine.selectedCaption() ?: captionsEngine.captions().firstOrNull() ?: return@register
        captionsEngine.applyStylePreset(
            sourceId = it.sourceId,
            assetId = it.asset.id,
            asset = it.asset,
            caption = caption,
        )
    }
}
