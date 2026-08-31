package ly.img.editor.base.engine

import ly.img.engine.BlockApi
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Font
import ly.img.engine.ListStyle

/** The engine's whole-block form for a text range — and the only one that registers the caption-track sync. */
private val WHOLE_BLOCK_TEXT_RANGE = -1..-1

/** Whether [designBlock] is a caption. Its text properties live under `caption/`, never `text/`. */
internal fun BlockApi.isCaption(designBlock: DesignBlock): Boolean =
    runCatching { getType(designBlock) }.getOrNull() == DesignBlockType.Caption.key

/** The namespace [designBlock] keeps its text properties in: `caption/` for a caption, `text/` for everything else. */
internal fun BlockApi.textNamespace(designBlock: DesignBlock): String = if (isCaption(designBlock)) "caption/" else "text/"

/** One of [designBlock]'s text properties, in the namespace that block keeps them in. */
internal fun BlockApi.textProperty(
    designBlock: DesignBlock,
    suffix: String,
): String = textNamespace(designBlock) + suffix

/**
 * The size [designBlock]'s text is laid out at.
 *
 * A run's size wins over the block property, so the property on its own reports a stale value — a caption
 * preset stamps a run and leaves the property behind, and text with mixed run sizes never matched it either.
 * Empty text has no run to read, so it falls back to the property.
 */
internal fun BlockApi.textFontSize(designBlock: DesignBlock): Float =
    runCatching { getTextFontSizes(designBlock).firstOrNull() }.getOrNull()
        ?: getFloat(designBlock, textProperty(designBlock, "fontSize"))

/**
 * Selection range when present, else the whole text. Avoids the engine's `-1, -1` default,
 * which collapses to a zero-length cursor range while editing.
 *
 * A caption is always whole-block: the engine only fans a run-level write out to the sibling captions when
 * the range is the `-1, -1` form.
 */
internal fun BlockApi.effectiveTextRange(designBlock: DesignBlock): IntRange {
    if (isCaption(designBlock)) return WHOLE_BLOCK_TEXT_RANGE
    val cursorRange = runCatching { getTextCursorRange() }.getOrNull()
    if (cursorRange != null && cursorRange.first != cursorRange.last) {
        return cursorRange
    }
    val length = runCatching { getString(designBlock, "text/text").length }.getOrDefault(0)
    return 0..length
}

/**
 * The shared [ListStyle] across the paragraphs at the text cursor/selection, or `null` if they use
 * mixed styles.
 */
internal fun BlockApi.resolveTextListStyle(designBlock: DesignBlock): ListStyle? {
    // No cross-caption sync for list style, and reading it would go through `text/text`.
    if (isCaption(designBlock)) return ListStyle.NONE
    val cursorRange = runCatching { getTextCursorRange() }.getOrNull()
    val paragraphIndices = runCatching {
        if (cursorRange != null) {
            getTextParagraphIndices(designBlock, cursorRange.first, cursorRange.last)
        } else {
            val length = getString(designBlock, "text/text").length
            getTextParagraphIndices(designBlock, 0, length)
        }
    }.getOrNull()
    if (paragraphIndices.isNullOrEmpty()) return ListStyle.NONE
    val styles = paragraphIndices.mapNotNull { index ->
        runCatching { getTextListStyle(designBlock, index) }.getOrNull()
    }
    if (styles.isEmpty()) return ListStyle.NONE
    val first = styles.first()
    return if (styles.all { it == first }) first else null
}

/**
 * The [Font] shared across the [effectiveTextRange], or `null` when the range mixes weights or styles.
 */
internal fun BlockApi.resolveTextFont(designBlock: DesignBlock): Font? {
    val range = effectiveTextRange(designBlock)
    val weight = runCatching { getTextFontWeights(designBlock, range.first, range.last) }
        .getOrNull()?.singleOrNull() ?: return null
    val style = runCatching { getTextFontStyles(designBlock, range.first, range.last) }
        .getOrNull()?.singleOrNull() ?: return null
    val typeface = runCatching { getTypeface(designBlock) }.getOrNull() ?: return null
    return typeface.fonts.firstOrNull { it.weight == weight && it.style == style }
}
