package ly.img.editor.base.components

import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import ly.img.editor.base.engine.effectiveTextRange
import ly.img.editor.base.engine.resolveTextListStyle
import ly.img.editor.core.ui.engine.toComposeColor
import ly.img.editor.core.ui.engine.toRGBColor
import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import ly.img.engine.FontStyle
import ly.img.engine.FontWeight
import ly.img.engine.ListStyle
import ly.img.engine.TextCase
import ly.img.engine.TextDecorationLine

sealed interface EditingTextCardUiState {
    /**
     * For when the [DesignBlock] disallows character formatting via the "text/character" scope.
     */
    @Stable
    data object Disabled : EditingTextCardUiState

    @Stable
    data class Formatting(
        val isBold: Boolean,
        val isItalic: Boolean,
        val isUnderline: Boolean,
        val isStrikethrough: Boolean,
        val canToggleBold: Boolean,
        val canToggleItalic: Boolean,
        val listStyle: ListStyle?,
        // The first case in the queried range (matching Web). The row marks it active and
        // re-applying it is a no-op, so a mixed range can always be normalised in one tap.
        val casing: TextCase,
        val textColors: List<Color>,
    ) : EditingTextCardUiState
}

internal fun createEditingTextCardUiState(
    designBlock: DesignBlock,
    engine: Engine,
): EditingTextCardUiState {
    val canFormat = runCatching {
        engine.block.isAllowedByScope(designBlock, "text/character")
    }.getOrDefault(false)
    if (!canFormat) return EditingTextCardUiState.Disabled
    val typeface = runCatching { engine.block.getTypeface(designBlock) }.getOrNull()

    val range = engine.block.effectiveTextRange(designBlock)
    val from = range.first
    val to = range.last

    val textColors = runCatching {
        engine.block.getTextColors(designBlock, from, to).map { it.toRGBColor(engine).toComposeColor() }
    }.getOrDefault(emptyList()).ifEmpty { listOf(Color.Black) }

    val weights = runCatching {
        engine.block.getTextFontWeights(designBlock, from, to)
    }.getOrDefault(emptyList())
    val styles = runCatching {
        engine.block.getTextFontStyles(designBlock, from, to)
    }.getOrDefault(emptyList())
    val decorations = runCatching {
        engine.block.getTextDecorations(designBlock, from, to)
    }.getOrDefault(emptyList())
    val cases = runCatching {
        engine.block.getTextCases(designBlock, from, to)
    }.getOrDefault(emptyList())

    return EditingTextCardUiState.Formatting(
        isBold = typeface != null && weights.isNotEmpty() && weights.all { it == FontWeight.BOLD },
        isItalic = typeface != null && styles.isNotEmpty() && styles.all { it == FontStyle.ITALIC },
        isUnderline = decorations.isNotEmpty() &&
            decorations.all { it.lines.contains(TextDecorationLine.UNDERLINE) },
        isStrikethrough = decorations.isNotEmpty() &&
            decorations.all { it.lines.contains(TextDecorationLine.STRIKETHROUGH) },
        canToggleBold = typeface != null &&
            runCatching {
                engine.block.canToggleBoldFont(designBlock, from, to)
            }.getOrDefault(false),
        canToggleItalic = typeface != null &&
            runCatching {
                engine.block.canToggleItalicFont(designBlock, from, to)
            }.getOrDefault(false),
        listStyle = engine.block.resolveTextListStyle(designBlock),
        casing = cases.firstOrNull() ?: TextCase.NORMAL,
        textColors = textColors,
    )
}
