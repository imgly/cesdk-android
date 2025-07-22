package ly.img.editor.base.dock.options.layer

import ly.img.editor.core.R
import ly.img.engine.BlendMode

private val blendModes = linkedMapOf(
    BlendMode.PASS_THROUGH to R.string.ly_img_editor_sheet_layer_blend_mode_option_pass_through,
    BlendMode.NORMAL to R.string.ly_img_editor_sheet_layer_blend_mode_option_normal,
    BlendMode.DARKEN to R.string.ly_img_editor_sheet_layer_blend_mode_option_darken,
    BlendMode.MULTIPLY to R.string.ly_img_editor_sheet_layer_blend_mode_option_multiply,
    BlendMode.COLOR_BURN to R.string.ly_img_editor_sheet_layer_blend_mode_option_color_burn,
    BlendMode.LINEAR_BURN to R.string.ly_img_editor_sheet_layer_blend_mode_option_linear_burn,
    BlendMode.LIGHTEN to R.string.ly_img_editor_sheet_layer_blend_mode_option_lighten,
    BlendMode.SCREEN to R.string.ly_img_editor_sheet_layer_blend_mode_option_screen,
    BlendMode.COLOR_DODGE to R.string.ly_img_editor_sheet_layer_blend_mode_option_color_dodge,
    BlendMode.LINEAR_DODGE to R.string.ly_img_editor_sheet_layer_blend_mode_option_linear_dodge,
    BlendMode.LIGHTEN_COLOR to R.string.ly_img_editor_sheet_layer_blend_mode_option_lighten_color,
    BlendMode.DARKEN_COLOR to R.string.ly_img_editor_sheet_layer_blend_mode_option_darken_color,
    BlendMode.OVERLAY to R.string.ly_img_editor_sheet_layer_blend_mode_option_overlay,
    BlendMode.SOFT_LIGHT to R.string.ly_img_editor_sheet_layer_blend_mode_option_soft_light,
    BlendMode.HARD_LIGHT to R.string.ly_img_editor_sheet_layer_blend_mode_option_hard_light,
    BlendMode.VIVID_LIGHT to R.string.ly_img_editor_sheet_layer_blend_mode_option_vivid_light,
    BlendMode.LINEAR_LIGHT to R.string.ly_img_editor_sheet_layer_blend_mode_option_linear_light,
    BlendMode.PIN_LIGHT to R.string.ly_img_editor_sheet_layer_blend_mode_option_pin_light,
    BlendMode.HARD_MIX to R.string.ly_img_editor_sheet_layer_blend_mode_option_hard_mix,
    BlendMode.DIFFERENCE to R.string.ly_img_editor_sheet_layer_blend_mode_option_difference,
    BlendMode.EXCLUSION to R.string.ly_img_editor_sheet_layer_blend_mode_option_exclusion,
    BlendMode.SUBTRACT to R.string.ly_img_editor_sheet_layer_blend_mode_option_subtract,
    BlendMode.DIVIDE to R.string.ly_img_editor_sheet_layer_blend_mode_option_divide,
    BlendMode.HUE to R.string.ly_img_editor_sheet_layer_blend_mode_option_hue,
    BlendMode.SATURATION to R.string.ly_img_editor_sheet_layer_blend_mode_option_saturation,
    BlendMode.COLOR to R.string.ly_img_editor_sheet_layer_blend_mode_option_color,
    BlendMode.LUMINOSITY to R.string.ly_img_editor_sheet_layer_blend_mode_option_luminosity,
)

val blendModesList = blendModes.toList()

fun getBlendModeStringResource(blendMode: BlendMode) = blendModes[blendMode]
