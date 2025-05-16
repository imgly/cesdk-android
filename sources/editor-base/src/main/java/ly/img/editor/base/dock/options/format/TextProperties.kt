package ly.img.editor.base.dock.options.format

import ly.img.editor.base.R
import ly.img.editor.base.components.PropertyOption
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
            R.string.ly_img_editor_auto_size,
            SizeModeUi.AUTO_SIZE.name,
            IconPack.AutoSize,
        ),
    SizeModeUi.AUTO_HEIGHT to
        PropertyOption(
            R.string.ly_img_editor_auto_height,
            SizeModeUi.AUTO_HEIGHT.name,
            IconPack.Textautoheight,
        ),
    SizeModeUi.ABSOLUTE to
        PropertyOption(
            R.string.ly_img_editor_fixed_size,
            SizeModeUi.ABSOLUTE.name,
            IconPack.Textfixedsize,
        ),
)

val sizeModeList = sizeModes.map { it.value }

fun SizeModeUi.getText() = requireNotNull(sizeModes[this]).textRes
