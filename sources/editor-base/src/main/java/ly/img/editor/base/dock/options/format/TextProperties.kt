package ly.img.editor.base.dock.options.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ly.img.editor.base.components.PropertyOption
import ly.img.editor.base.engine.PropertyText
import ly.img.editor.core.R
import ly.img.editor.core.ui.iconpack.AutoSize
import ly.img.editor.core.ui.iconpack.IconPack
import ly.img.editor.core.ui.iconpack.Textautoheight
import ly.img.editor.core.ui.iconpack.Textfixedsize

enum class SizeModeUi {
    ABSOLUTE,
    AUTO_HEIGHT,
    AUTO_SIZE,
    UNKNOWN,
}

val sizeModeProperties = listOf(
    PropertyOption(
        text = PropertyText.Resource(R.string.ly_img_editor_sheet_format_text_frame_behavior_option_auto_size),
        value = SizeModeUi.AUTO_SIZE,
        icon = IconPack.AutoSize,
    ),
    PropertyOption(
        text = PropertyText.Resource(R.string.ly_img_editor_sheet_format_text_frame_behavior_option_auto_height),
        value = SizeModeUi.AUTO_HEIGHT,
        icon = IconPack.Textautoheight,
    ),
    PropertyOption(
        text = PropertyText.Resource(R.string.ly_img_editor_sheet_format_text_frame_behavior_option_fixed_size),
        value = SizeModeUi.ABSOLUTE,
        icon = IconPack.Textfixedsize,
    ),
)

fun getSubFamilyStringResource(subFamily: String): Int = when (subFamily) {
    "Thin" -> R.string.ly_img_editor_sheet_format_text_font_subfamily_thin
    "ExtraLight" -> R.string.ly_img_editor_sheet_format_text_font_subfamily_extralight
    "Light" -> R.string.ly_img_editor_sheet_format_text_font_subfamily_light
    "Regular" -> R.string.ly_img_editor_sheet_format_text_font_subfamily_regular
    "Medium" -> R.string.ly_img_editor_sheet_format_text_font_subfamily_medium
    "SemiBold" -> R.string.ly_img_editor_sheet_format_text_font_subfamily_semibold
    "Bold" -> R.string.ly_img_editor_sheet_format_text_font_subfamily_bold
    "ExtraBold" -> R.string.ly_img_editor_sheet_format_text_font_subfamily_extrabold
    "Black" -> R.string.ly_img_editor_sheet_format_text_font_subfamily_black
    "Thin Italic" -> R.string.ly_img_editor_sheet_format_text_font_subfamily_thin_italic
    "ExtraLight Italic" -> R.string.ly_img_editor_sheet_format_text_font_subfamily_extralight_italic
    "Light Italic" -> R.string.ly_img_editor_sheet_format_text_font_subfamily_light_italic
    "Italic" -> R.string.ly_img_editor_sheet_format_text_font_subfamily_italic
    "Medium Italic" -> R.string.ly_img_editor_sheet_format_text_font_subfamily_medium_italic
    "SemiBold Italic" -> R.string.ly_img_editor_sheet_format_text_font_subfamily_semibold_italic
    "Bold Italic" -> R.string.ly_img_editor_sheet_format_text_font_subfamily_bold_italic
    "ExtraBold Italic" -> R.string.ly_img_editor_sheet_format_text_font_subfamily_extrabold_italic
    "Black Italic" -> R.string.ly_img_editor_sheet_format_text_font_subfamily_black_italic
    else -> throw IllegalArgumentException()
}

@Composable
fun getSubFamilyString(subFamily: String): String {
    val subFamilyStringResource = try {
        getSubFamilyStringResource(subFamily)
    } catch (_: IllegalArgumentException) {
        return subFamily
    }
    return stringResource(subFamilyStringResource)
}
