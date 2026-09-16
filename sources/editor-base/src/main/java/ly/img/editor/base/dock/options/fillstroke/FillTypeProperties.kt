package ly.img.editor.base.dock.options.fillstroke

import ly.img.editor.base.components.PropertyOption
import ly.img.editor.base.engine.PropertyText
import ly.img.editor.core.R
import ly.img.engine.FillType

val fillTypeProperties = listOf<PropertyOption<FillType?>>(
    PropertyOption(
        text = PropertyText.Resource(R.string.ly_img_editor_sheet_fill_stroke_type_option_none),
        value = null,
    ),
    PropertyOption(
        text = PropertyText.Resource(R.string.ly_img_editor_sheet_fill_stroke_type_option_solid),
        value = FillType.Color,
    ),
    PropertyOption(
        text = PropertyText.Resource(R.string.ly_img_editor_sheet_fill_stroke_type_option_gradient_linear),
        value = FillType.LinearGradient,
    ),
    PropertyOption(
        text = PropertyText.Resource(R.string.ly_img_editor_sheet_fill_stroke_type_option_gradient_radial),
        value = FillType.RadialGradient,
        selectable = false,
    ),
    PropertyOption(
        text = PropertyText.Resource(R.string.ly_img_editor_sheet_fill_stroke_type_option_gradient_conical),
        value = FillType.ConicalGradient,
        selectable = false,
    ),
)
