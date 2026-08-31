package ly.img.editor.base.ui.handler

import ly.img.editor.base.timeline.state.AnimationPreview
import ly.img.editor.base.timeline.state.TimelineState
import ly.img.editor.base.timeline.state.transitionIncomingClip
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.core.ui.EventsHandler
import ly.img.editor.core.ui.inject
import ly.img.editor.core.ui.library.getMeta
import ly.img.editor.core.ui.register
import ly.img.engine.DesignBlock
import ly.img.engine.Engine

@Suppress("NAME_SHADOWING")
fun EventsHandler.animationEvents(
    engine: () -> Engine,
    timelineState: () -> TimelineState?,
) {
    val engine by inject(engine)
    val timelineState by inject(timelineState)

    register<BlockEvent.OnReplaceAnimation> {
        engine.asset.applyAssetSourceAsset(sourceId = it.sourceId, asset = it.asset, block = it.designBlock)
        if (it.asset.getMeta("type") != "none") {
            when (it.asset.getMeta("mode")) {
                "out" -> removeTransition(engine, it.designBlock)
                "in" -> previousSibling(engine, it.designBlock)?.let { previous -> removeTransition(engine, previous) }
            }
        }
        engine.editor.addUndoStep()
        when (it.asset.getMeta("mode")) {
            "in" -> timelineState?.animationPreview?.playAnimation(it.designBlock, AnimationPreview.Mode.In)
            "out" -> timelineState?.animationPreview?.playAnimation(it.designBlock, AnimationPreview.Mode.Out)
            "loop" -> timelineState?.animationPreview?.playAnimation(it.designBlock, AnimationPreview.Mode.Loop)
        }
    }

    register<BlockEvent.OnReplaceTransition> {
        engine.asset.applyAssetSourceAsset(sourceId = it.sourceId, asset = it.asset, block = it.outgoingBlock)
        if (it.asset.getMeta("type") != "none") {
            clearConflictingAnimations(engine, it.outgoingBlock)
        }
        engine.editor.addUndoStep()
        timelineState?.animationPreview?.playTransition(it.outgoingBlock)
    }

    register<BlockEvent.OnPreviewAnimation> {
        val mode = when (it.mode) {
            "in" -> AnimationPreview.Mode.In
            "out" -> AnimationPreview.Mode.Out
            "loop" -> AnimationPreview.Mode.Loop
            else -> return@register
        }
        timelineState?.animationPreview?.playAnimation(it.designBlock, mode)
    }

    register<BlockEvent.OnPreviewTransition> {
        timelineState?.animationPreview?.playTransition(it.outgoingBlock)
    }

    register<BlockEvent.OnApplyTransitionToTrack> {
        val activeTransition = engine.block.getTransition(it.outgoingBlock)
        if (!engine.block.isValid(activeTransition)) return@register
        val children = trackChildren(engine, it.outgoingBlock)
        children.forEach { outgoing ->
            if (outgoing == it.outgoingBlock) return@forEach
            val previousTransition = engine.block.getTransition(outgoing)
            val incoming = engine.transitionIncomingClip(outgoing) ?: return@forEach
            val duplicate = engine.block.duplicate(activeTransition, attachToParent = false)
            val maximumDuration = maxOf(
                engine.block.getDuration(outgoing) / 2,
                engine.block.getDuration(incoming) / 2,
            )
            val current = engine.block.getDuration(duplicate)
            engine.block.setDuration(
                block = duplicate,
                duration = minOf(current, maximumDuration),
            )
            engine.block.setTransition(outgoing, duplicate)
            previousTransition.takeIf { engine.block.isValid(it) }?.let(engine.block::destroy)
            clearConflictingAnimations(engine, outgoing)
        }
        engine.editor.addUndoStep()
    }

    register<BlockEvent.OnRemoveTransitionsFromTrack> {
        trackChildren(engine, it.outgoingBlock).forEach { clip ->
            if (engine.block.supportsTransition(clip)) {
                engine.block.getTransition(clip).takeIf { engine.block.isValid(it) }?.let(engine.block::destroy)
            }
        }
        engine.editor.addUndoStep()
    }
}

private fun trackChildren(
    engine: Engine,
    clip: DesignBlock,
): List<DesignBlock> = engine.block.getParent(clip)?.let(engine.block::getChildren).orEmpty()

private fun previousSibling(
    engine: Engine,
    clip: DesignBlock,
): DesignBlock? {
    val children = trackChildren(engine, clip)
    return children.getOrNull(children.indexOf(clip) - 1)
}

private fun nextSibling(
    engine: Engine,
    clip: DesignBlock,
): DesignBlock? {
    val children = trackChildren(engine, clip)
    return children.getOrNull(children.indexOf(clip) + 1)
}

private fun removeTransition(
    engine: Engine,
    outgoing: DesignBlock,
) {
    if (!engine.block.supportsTransition(outgoing)) return
    engine.block.getTransition(outgoing).takeIf { engine.block.isValid(it) }?.let(engine.block::destroy)
}

private fun clearConflictingAnimations(
    engine: Engine,
    outgoing: DesignBlock,
) {
    if (engine.block.supportsAnimation(outgoing)) {
        engine.block.getOutAnimation(outgoing).takeIf { engine.block.isValid(it) }?.let(engine.block::destroy)
    }
    nextSibling(engine, outgoing)?.let { incoming ->
        if (engine.block.supportsAnimation(incoming)) {
            engine.block.getInAnimation(incoming).takeIf { engine.block.isValid(it) }?.let(engine.block::destroy)
        }
    }
}
