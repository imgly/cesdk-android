package ly.img.editor.base.dock.options.fillstroke

import ly.img.editor.base.components.PropertyOption
import ly.img.editor.core.R
import ly.img.engine.FillType

val fillTypePropertiesList = listOf(
    PropertyOption(R.string.ly_img_editor_sheet_fill_stroke_type_option_none, "NONE"),
    PropertyOption(R.string.ly_img_editor_sheet_fill_stroke_type_option_solid, FillType.Color.key),
    PropertyOption(R.string.ly_img_editor_sheet_fill_stroke_type_option_gradient_linear, FillType.LinearGradient.key),
)
