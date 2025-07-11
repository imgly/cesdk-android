package ly.img.editor.base.dock.options.format

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ly.img.editor.base.components.PropertyOption
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

private val sizeModes = linkedMapOf(
    SizeModeUi.AUTO_SIZE to
        PropertyOption(
            R.string.ly_img_editor_sheet_format_text_frame_behavior_option_auto_size,
            SizeModeUi.AUTO_SIZE.name,
            IconPack.AutoSize,
        ),
    SizeModeUi.AUTO_HEIGHT to
        PropertyOption(
            R.string.ly_img_editor_sheet_format_text_frame_behavior_option_auto_height,
            SizeModeUi.AUTO_HEIGHT.name,
            IconPack.Textautoheight,
        ),
    SizeModeUi.ABSOLUTE to
        PropertyOption(
            R.string.ly_img_editor_sheet_format_text_frame_behavior_option_fixed_size,
            SizeModeUi.ABSOLUTE.name,
            IconPack.Textfixedsize,
        ),
)

val sizeModeList = sizeModes.map { it.value }

fun SizeModeUi.getText() = requireNotNull(sizeModes[this]).textRes

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
