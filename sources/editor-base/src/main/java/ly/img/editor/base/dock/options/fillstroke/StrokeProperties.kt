package ly.img.editor.base.dock.options.fillstroke

import ly.img.editor.base.components.PropertyOption
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

private val strokeStyles = linkedMapOf(
    StrokeStyle.SOLID to PropertyOption(R.string.ly_img_editor_sheet_fill_stroke_style_option_solid, StrokeStyle.SOLID.name),
    StrokeStyle.DASHED to PropertyOption(R.string.ly_img_editor_sheet_fill_stroke_style_option_dashed, StrokeStyle.DASHED.name),
    StrokeStyle.DASHED_ROUND to
        PropertyOption(R.string.ly_img_editor_sheet_fill_stroke_style_option_dashed_round, StrokeStyle.DASHED_ROUND.name),
    StrokeStyle.LONG_DASHED to
        PropertyOption(R.string.ly_img_editor_sheet_fill_stroke_style_option_long_dashed, StrokeStyle.LONG_DASHED.name),
    StrokeStyle.LONG_DASHED_ROUND to
        PropertyOption(
            R.string.ly_img_editor_sheet_fill_stroke_style_option_long_dashed_round,
            StrokeStyle.LONG_DASHED_ROUND.name,
        ),
    StrokeStyle.DOTTED to PropertyOption(R.string.ly_img_editor_sheet_fill_stroke_style_option_dotted, StrokeStyle.DOTTED.name),
)

private val strokePositions = linkedMapOf(
    StrokePosition.INNER to
        PropertyOption(
            R.string.ly_img_editor_sheet_fill_stroke_position_option_inside,
            StrokePosition.INNER.name,
            IconPack.Strokepositioninside,
        ),
    StrokePosition.CENTER to
        PropertyOption(
            R.string.ly_img_editor_sheet_fill_stroke_position_option_center,
            StrokePosition.CENTER.name,
            IconPack.Strokepositioncenter,
        ),
    StrokePosition.OUTER to
        PropertyOption(
            R.string.ly_img_editor_sheet_fill_stroke_position_option_outside,
            StrokePosition.OUTER.name,
            IconPack.Strokepositionoutside,
        ),
)

private val strokeJoins = linkedMapOf(
    StrokeCornerGeometry.MITER to
        PropertyOption(
            R.string.ly_img_editor_sheet_fill_stroke_join_option_miter,
            StrokeCornerGeometry.MITER.name,
            IconPack.Joinmiter,
        ),
    StrokeCornerGeometry.BEVEL to
        PropertyOption(
            R.string.ly_img_editor_sheet_fill_stroke_join_option_bevel,
            StrokeCornerGeometry.BEVEL.name,
            IconPack.Joinbevel,
        ),
    StrokeCornerGeometry.ROUND to
        PropertyOption(
            R.string.ly_img_editor_sheet_fill_stroke_join_option_round,
            StrokeCornerGeometry.ROUND.name,
            IconPack.Joinround,
        ),
)

val strokeStylePropertiesList = strokeStyles.map { it.value }
val strokePositionPropertiesList = strokePositions.map { it.value }
val strokeJoinPropertiesList = strokeJoins.map { it.value }

fun StrokeStyle.getText() = requireNotNull(strokeStyles[this]).textRes

fun StrokePosition.getText() = requireNotNull(strokePositions[this]).textRes

fun StrokeCornerGeometry.getText() = requireNotNull(strokeJoins[this]).textRes
