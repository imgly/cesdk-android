package ly.img.editor.base.dock.options.fillstroke

import ly.img.editor.base.components.PropertyOption
import ly.img.editor.base.engine.PropertyText
import ly.img.editor.core.R
import ly.img.editor.core.ui.iconpack.IconPack
import ly.img.editor.core.ui.iconpack.Joinbevel
import ly.img.editor.core.ui.iconpack.Joinmiter
import ly.img.editor.core.ui.iconpack.Joinround
import ly.img.editor.core.ui.iconpack.Strokepositioncenter
import ly.img.editor.core.ui.iconpack.Strokepositioninside
import ly.img.editor.core.ui.iconpack.Strokepositionoutside
import ly.img.engine.StrokeCornerGeometry
import ly.img.engine.StrokePosition
import ly.img.engine.StrokeStyle

val strokeStyleProperties = listOf(
    PropertyOption(
        text = PropertyText.Resource(R.string.ly_img_editor_sheet_fill_stroke_style_option_solid),
        value = StrokeStyle.SOLID,
    ),
    PropertyOption(
        text = PropertyText.Resource(R.string.ly_img_editor_sheet_fill_stroke_style_option_dashed),
        value = StrokeStyle.DASHED,
    ),
    PropertyOption(
        text = PropertyText.Resource(R.string.ly_img_editor_sheet_fill_stroke_style_option_dashed_round),
        value = StrokeStyle.DASHED_ROUND,
    ),
    PropertyOption(
        text = PropertyText.Resource(R.string.ly_img_editor_sheet_fill_stroke_style_option_long_dashed),
        value = StrokeStyle.LONG_DASHED,
    ),
    PropertyOption(
        text = PropertyText.Resource(R.string.ly_img_editor_sheet_fill_stroke_style_option_long_dashed_round),
        value = StrokeStyle.LONG_DASHED_ROUND,
    ),
    PropertyOption(
        text = PropertyText.Resource(R.string.ly_img_editor_sheet_fill_stroke_style_option_dotted),
        value = StrokeStyle.DOTTED,
    ),
)

val strokePositionProperties = listOf(
    PropertyOption(
        text = PropertyText.Resource(R.string.ly_img_editor_sheet_fill_stroke_position_option_inside),
        value = StrokePosition.INNER,
        icon = IconPack.Strokepositioninside,
    ),
    PropertyOption(
        text = PropertyText.Resource(R.string.ly_img_editor_sheet_fill_stroke_position_option_center),
        value = StrokePosition.CENTER,
        icon = IconPack.Strokepositioncenter,
    ),
    PropertyOption(
        text = PropertyText.Resource(R.string.ly_img_editor_sheet_fill_stroke_position_option_outside),
        value = StrokePosition.OUTER,
        icon = IconPack.Strokepositionoutside,
    ),
)

val strokeJoinProperties = listOf(
    PropertyOption(
        text = PropertyText.Resource(R.string.ly_img_editor_sheet_fill_stroke_join_option_miter),
        value = StrokeCornerGeometry.MITER,
        icon = IconPack.Joinmiter,
    ),
    PropertyOption(
        text = PropertyText.Resource(R.string.ly_img_editor_sheet_fill_stroke_join_option_bevel),
        value = StrokeCornerGeometry.BEVEL,
        icon = IconPack.Joinbevel,
    ),
    PropertyOption(
        text = PropertyText.Resource(R.string.ly_img_editor_sheet_fill_stroke_join_option_round),
        value = StrokeCornerGeometry.ROUND,
        icon = IconPack.Joinround,
    ),
)
