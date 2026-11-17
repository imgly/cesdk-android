package ly.img.editor.base.clipboard

internal object ClipboardSvg {
    /**
     * Whether [text] is SVG markup: its root element is `<svg>`, after an optional byte order mark, XML declaration,
     * comments and doctype. Adobe Illustrator copies its selection as such markup in plain text. A scan instead of one
     * regular expression, because a pattern over repeated comments backtracks for a long time on text that is no SVG.
     */
    fun isMarkup(text: String): Boolean {
        var index = if (text.startsWith('\uFEFF')) 1 else 0
        while (true) {
            while (index < text.length && text[index].isWhitespace()) index += 1
            val next = afterPrologPart(text, index)
            if (next < 0) return false
            if (next == index) break
            index = next
        }
        val start = text.regionMatches(index, "<svg", 0, 4, ignoreCase = true) &&
            index + 4 < text.length &&
            (text[index + 4].isWhitespace() || text[index + 4] == '>')
        return start && text.trimEnd().let { it.endsWith(">") && it.dropLast(1).trimEnd().endsWith("</svg", ignoreCase = true) }
    }

    /**
     * The index after the prolog part at [index]: a declaration, a comment or a doctype with its internal subset.
     * [index] itself when no prolog part starts there, and -1 when the part has no end.
     */
    private fun afterPrologPart(
        text: String,
        index: Int,
    ): Int {
        for ((start, end) in listOf("<?" to "?>", "<!--" to "-->")) {
            if (text.startsWith(start, index)) {
                val close = text.indexOf(end, index + start.length)
                return if (close < 0) -1 else close + end.length
            }
        }
        if (!text.regionMatches(index, "<!DOCTYPE", 0, 9, ignoreCase = true)) return index
        val close = text.indexOf('>', index)
        val subset = text.indexOf('[', index)
        if (close < 0 || subset < 0 || subset > close) return if (close < 0) -1 else close + 1
        val subsetEnd = text.indexOf(']', subset)
        val subsetClose = if (subsetEnd < 0) -1 else text.indexOf('>', subsetEnd)
        return if (subsetClose < 0) -1 else subsetClose + 1
    }
}
