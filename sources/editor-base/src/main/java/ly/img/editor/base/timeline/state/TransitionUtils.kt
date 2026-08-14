package ly.img.editor.base.timeline.state

import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.seconds

internal fun Engine.canShowTransition(
    outgoing: DesignBlock,
    incoming: DesignBlock,
    outgoingEnd: Duration,
    incomingStart: Duration,
): Boolean = block.supportsTransition(outgoing) &&
    block.supportsTransition(incoming) &&
    incomingStart <= outgoingEnd + 0.001.seconds

internal fun Engine.transitionIncomingClip(outgoing: DesignBlock): DesignBlock? {
    if (!block.isValid(outgoing)) return null

    val siblings = block.getParent(outgoing)
        ?.let(block::getChildren)
        ?.sortedBy(block::getTimeOffset)
        .orEmpty()
    val incoming = siblings.getOrNull(siblings.indexOf(outgoing) + 1) ?: return null
    return incoming.takeIf {
        canShowTransition(
            outgoing = outgoing,
            incoming = incoming,
            outgoingEnd = (block.getTimeOffset(outgoing) + block.getDuration(outgoing)).seconds,
            incomingStart = block.getTimeOffset(incoming).seconds,
        )
    }
}

internal fun Engine.hasRealTransition(outgoing: DesignBlock): Boolean {
    val transition = block.getTransition(outgoing)
    return block.isValid(transition) && block.getType(transition) != "//ly.img.ubq/transition/none"
}

internal fun Engine.transitionTrim(clip: DesignBlock): TransitionTrim {
    val siblings = block.getParent(clip)?.let(block::getChildren).orEmpty()
    val index = siblings.indexOf(clip)
    if (index < 0) return TransitionTrim()

    val lead = siblings.getOrNull(index - 1)
        ?.let { outgoing -> transitionOverlap(outgoing, clip) / 2 }
        ?: ZERO
    val tail = siblings.getOrNull(index + 1)
        ?.let { incoming -> transitionOverlap(clip, incoming) / 2 }
        ?: ZERO
    return TransitionTrim(lead = lead, tail = tail)
}

internal fun Engine.transitionOverlap(
    outgoing: DesignBlock,
    incoming: DesignBlock,
): Duration {
    val outgoingDuration = block.getDuration(outgoing).seconds
    val incomingDuration = block.getDuration(incoming).seconds
    val outgoingEnd = block.getTimeOffset(outgoing).seconds + outgoingDuration
    val incomingStart = block.getTimeOffset(incoming).seconds
    if (!canShowTransition(outgoing, incoming, outgoingEnd, incomingStart) || !hasRealTransition(outgoing)) {
        return ZERO
    }
    return minOf(
        block.getDuration(block.getTransition(outgoing)).seconds,
        outgoingDuration / 2,
        incomingDuration / 2,
    ).coerceAtLeast(ZERO)
}

internal data class TransitionTrim(
    val lead: Duration = ZERO,
    val tail: Duration = ZERO,
)
