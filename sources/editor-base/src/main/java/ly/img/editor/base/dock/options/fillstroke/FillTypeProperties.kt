package ly.img.editor.base.dock.options.fillstroke

import ly.img.editor.base.R
import ly.img.editor.base.components.PropertyOption
import ly.img.engine.FillType

val fillTypePropertiesList = listOf(
    PropertyOption(R.string.ly_img_editor_fill_none, "NONE"),
    PropertyOption(R.string.ly_img_editor_fill_solid, FillType.Color.key),
    PropertyOption(R.string.ly_img_editor_fill_type_gradient_linear, FillType.LinearGradient.key),
)
