package ly.img.editor.base.dock.options.format

import androidx.annotation.StringRes
import ly.img.editor.base.engine.effectiveTextRange
import ly.img.editor.base.engine.resolveTextFont
import ly.img.editor.base.engine.resolveTextListStyle
import ly.img.editor.core.library.LibraryCategory
import ly.img.editor.core.ui.engine.Scope
import ly.img.editor.core.ui.library.TypefaceLibraryCategory
import ly.img.editor.core.ui.library.data.font.FontData
import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import ly.img.engine.FontStyle
import ly.img.engine.FontUnit
import ly.img.engine.FontWeight
import ly.img.engine.HorizontalAlignment
import ly.img.engine.ListStyle
import ly.img.engine.SizeMode
import ly.img.engine.TextCase
import ly.img.engine.TextDecorationLine

data class FormatUiState(
    val libraryCategory: LibraryCategory,
    val fontFamily: String,
    val fontFamilyWeight: FontWeight?,
    val fontFamilyStyle: FontStyle?,
    val canToggleBold: Boolean,
    val canToggleItalic: Boolean,
    val isBold: Boolean,
    val isItalic: Boolean,
    val isUnderline: Boolean,
    val isStrikethrough: Boolean,
    val casing: TextCase,
    val listStyle: ListStyle?,
    val horizontalAlignment: HorizontalAlignment,
    val effectiveHorizontalAlignment: HorizontalAlignment,
    val verticalAlignment: VerticalAlignment,
    val fontSize: Float,
    // The unit in which `fontSize` (read via getFloat("text/fontSize")) is expressed. Driven by the
    // scene's `fontSizeUnit` and used to label the UI and choose an appropriate slider range.
    val fontSizeUnit: FontUnit,
    val letterSpacing: Float,
    val paragraphSpacing: Float,
    val lineHeight: Float,
    val isClipped: Boolean,
    val hasClippingOption: Boolean,
    @StringRes val sizeModeRes: Int,
    val isArrangeResizeAllowed: Boolean,
    val availableWeights: List<FontData>,
    val subFamily: String,
    val isSubFamilyMixed: Boolean,
)

internal fun createFormatUiState(
    designBlock: DesignBlock,
    engine: Engine,
): FormatUiState {
    val typeface = runCatching { engine.block.getTypeface(designBlock) }.getOrNull()
    val sizeMode = engine.block.getHeightMode(designBlock)

    val currentFont = typeface?.let { engine.block.resolveTextFont(designBlock) }

    return FormatUiState(
        libraryCategory = TypefaceLibraryCategory,
        fontFamily = typeface?.name ?: "Default",
        canToggleBold = typeface?.let {
            engine.block.canToggleBoldFont(designBlock)
        } ?: false,
        canToggleItalic = typeface?.let {
            engine.block.canToggleItalicFont(designBlock)
        } ?: false,
        isBold = typeface?.let {
            val weights = engine.block.getTextFontWeights(designBlock)
            weights.isNotEmpty() && weights.all { weight -> weight == FontWeight.BOLD }
        } ?: false,
        isItalic = typeface?.let {
            val styles = engine.block.getTextFontStyles(designBlock)
            styles.isNotEmpty() && styles.all { style -> style == FontStyle.ITALIC }
        } ?: false,
        isUnderline = runCatching {
            val decorations = engine.block.getTextDecorations(designBlock)
            decorations.isNotEmpty() && decorations.all { it.lines.contains(TextDecorationLine.UNDERLINE) }
        }.getOrDefault(false),
        isStrikethrough = runCatching {
            val decorations = engine.block.getTextDecorations(designBlock)
            decorations.isNotEmpty() && decorations.all { it.lines.contains(TextDecorationLine.STRIKETHROUGH) }
        }.getOrDefault(false),
        horizontalAlignment = HorizontalAlignment.valueOf(
            engine.block.getEnum(designBlock, "text/horizontalAlignment"),
        ),
        effectiveHorizontalAlignment = engine.block.getTextEffectiveHorizontalAlignment(designBlock),
        verticalAlignment = VerticalAlignment.valueOf(
            engine.block.getEnum(designBlock, "text/verticalAlignment"),
        ),
        fontSize = engine.block.getFloat(designBlock, "text/fontSize"),
        fontSizeUnit = engine.scene.getFontSizeUnit(),
        letterSpacing = engine.block.getFloat(designBlock, "text/letterSpacing"),
        lineHeight = engine.block.getFloat(designBlock, "text/lineHeight"),
        sizeModeRes = when (sizeMode) {
            SizeMode.ABSOLUTE -> SizeModeUi.ABSOLUTE
            SizeMode.AUTO ->
                when (engine.block.getWidthMode(designBlock)) {
                    SizeMode.AUTO -> SizeModeUi.AUTO_SIZE
                    SizeMode.ABSOLUTE -> SizeModeUi.AUTO_HEIGHT
                    SizeMode.PERCENT -> SizeModeUi.UNKNOWN
                }

            SizeMode.PERCENT -> SizeModeUi.UNKNOWN
        }.getText(),
        hasClippingOption = sizeMode == SizeMode.ABSOLUTE,
        isClipped = engine.block.getBoolean(designBlock, "text/clipLinesOutsideOfFrame"),
        isArrangeResizeAllowed = engine.block.isAllowedByScope(designBlock, Scope.LayerResize),
        casing = engine.block.effectiveTextRange(designBlock).let { range ->
            engine.block.getTextCases(designBlock, range.first, range.last).firstOrNull() ?: TextCase.NORMAL
        },
        listStyle = engine.block.resolveTextListStyle(designBlock),
        paragraphSpacing = engine.block.getFloat(designBlock, "text/paragraphSpacing"),
        fontFamilyWeight = currentFont?.weight,
        availableWeights = typeface?.fonts?.sortedBy { it.weight.value + if (it.style == FontStyle.ITALIC) 1000 else 0 }?.map {
            FontData(
                typeface = typeface,
                uri = it.uri,
                weight = androidx.compose.ui.text.font
                    .FontWeight(it.weight.value),
                style = it.style,
                subFamily = it.subFamily,
            )
        } ?: emptyList(),
        fontFamilyStyle = currentFont?.style,
        subFamily = currentFont?.subFamily ?: "",
        isSubFamilyMixed = typeface != null && currentFont == null,
    )
}
