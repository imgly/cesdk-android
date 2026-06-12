package ly.img.editor.base.engine

import ly.img.engine.BlockApi
import ly.img.engine.DesignBlock
import ly.img.engine.Font
import ly.img.engine.ListStyle

/**
 * Selection range when present, else the whole text. Avoids the engine's `-1, -1` default,
 * which collapses to a zero-length cursor range while editing.
 */
internal fun BlockApi.effectiveTextRange(designBlock: DesignBlock): IntRange {
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
