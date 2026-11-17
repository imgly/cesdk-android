package ly.img.editor.base.timeline.state

import ly.img.editor.base.timeline.clip.Clip
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
    return block.isValid(transition)
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

/**
 * Converts a rendered timeline duration to raw engine timing using the transition inset that
 * will apply after the duration changes. A transition edge is either capped or contributes one
 * quarter of the raw clip duration, yielding three linear regions.
 */
internal fun Engine.transitionTiming(
    clip: DesignBlock,
    renderedDuration: Duration,
): TransitionTiming {
    val siblings = block.getParent(clip)?.let(block::getChildren).orEmpty()
    val index = siblings.indexOf(clip)
    if (index < 0) return TransitionTiming(rawDuration = renderedDuration)

    fun trimCap(
        outgoing: DesignBlock,
        incoming: DesignBlock,
        adjacent: DesignBlock,
    ): Duration {
        if (transitionOverlap(outgoing, incoming) == ZERO) return ZERO
        return minOf(
            block.getDuration(block.getTransition(outgoing)).seconds / 2,
            block.getDuration(adjacent).seconds / 4,
        )
    }

    val leadingCap = siblings.getOrNull(index - 1)
        ?.let { outgoing -> trimCap(outgoing, clip, outgoing) }
        ?: ZERO
    val trailingCap = siblings.getOrNull(index + 1)
        ?.let { incoming -> trimCap(clip, incoming, incoming) }
        ?: ZERO
    val smallerCap = minOf(leadingCap, trailingCap)
    val largerCap = maxOf(leadingCap, trailingCap)

    val rawDuration = when {
        renderedDuration <= smallerCap * 2 -> renderedDuration * 2
        renderedDuration <= largerCap * 3 - smallerCap -> (renderedDuration + smallerCap) * 4 / 3
        else -> renderedDuration + smallerCap + largerCap
    }
    return TransitionTiming(
        rawDuration = rawDuration,
        trim = TransitionTrim(
            lead = minOf(leadingCap, rawDuration / 4),
            tail = minOf(trailingCap, rawDuration / 4),
        ),
    )
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

/** Maps each clip on [tracks] that has a transition to the clip that follows it on its track. */
internal fun Engine.transitionCuts(tracks: Iterable<DesignBlock>): Map<DesignBlock, DesignBlock?> = buildMap {
    tracks.forEach { track ->
        val children = block.getChildren(track)
        children.forEachIndexed { index, clip ->
            if (hasRealTransition(clip)) put(clip, children.getOrNull(index + 1))
        }
    }
}

/**
 * Destroys the transition of each cut in [cuts] whose clip is now followed by a different clip. A transition belongs
 * to its cut, so it does not move to the new neighbour. Destroying it, unlike `removeTransition`, shifts no clip.
 *
 * @return The number of destroyed transitions.
 */
internal fun Engine.removeChangedTransitionCuts(cuts: Map<DesignBlock, DesignBlock?>): Int {
    var removed = 0
    cuts.forEach { (outgoing, incoming) ->
        if (!block.isValid(outgoing)) return@forEach
        val transition = block.getTransition(outgoing)
        if (!block.isValid(transition)) return@forEach
        val siblings = block.getParent(outgoing)?.let(block::getChildren).orEmpty()
        if (siblings.getOrNull(siblings.indexOf(outgoing) + 1) == incoming) return@forEach
        block.destroy(transition)
        removed++
    }
    return removed
}

/**
 * Converts the rendered offsets of a drop to raw engine offsets. [clips] are the clips of the target track in track
 * order, with raw engine timing. A clip without a rendered offset keeps its raw offset. Then each clip moves right
 * until it overlaps its predecessor by no more than their transition, because a removed transition gives the
 * outgoing clip back the tail that the timeline trimmed.
 *
 * @return The raw offsets that differ from the current ones.
 */
internal fun computeDropOffsets(
    clips: List<Clip>,
    renderedOffsets: Map<DesignBlock, Duration>,
    transitionOverlap: (outgoing: Clip, incoming: Clip) -> Duration,
): Map<DesignBlock, Duration> {
    val offsets = mutableMapOf<DesignBlock, Duration>()
    var previous: Clip? = null
    var previousEnd = ZERO
    for (clip in clips) {
        val overlap = previous?.let { transitionOverlap(it, clip) } ?: ZERO
        var start = renderedOffsets[clip.id]?.let { it - overlap / 2 } ?: clip.timeOffset
        if (previous != null) start = maxOf(start, previousEnd - overlap)
        if (start != clip.timeOffset) offsets[clip.id] = start
        previous = clip
        previousEnd = start + clip.duration
    }
    return offsets
}

internal data class TransitionTrim(
    val lead: Duration = ZERO,
    val tail: Duration = ZERO,
)

internal data class TransitionTiming(
    val rawDuration: Duration,
    val trim: TransitionTrim = TransitionTrim(),
)
