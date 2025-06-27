package ly.img.editor.base.dock.options.layer

import ly.img.editor.base.R
import ly.img.engine.BlendMode

private val blendModes = linkedMapOf(
    BlendMode.PASS_THROUGH to R.string.ly_img_editor_blendmode_pass_through,
    BlendMode.NORMAL to R.string.ly_img_editor_blendmode_normal,
    BlendMode.DARKEN to R.string.ly_img_editor_blendmode_darken,
    BlendMode.MULTIPLY to R.string.ly_img_editor_blendmode_multiply,
    BlendMode.COLOR_BURN to R.string.ly_img_editor_blendmode_color_burn,
    BlendMode.LINEAR_BURN to R.string.ly_img_editor_blendmode_linear_burn,
    BlendMode.LIGHTEN to R.string.ly_img_editor_blendmode_lighten,
    BlendMode.SCREEN to R.string.ly_img_editor_blendmode_screen,
    BlendMode.COLOR_DODGE to R.string.ly_img_editor_blendmode_color_dodge,
    BlendMode.LINEAR_DODGE to R.string.ly_img_editor_blendmode_linear_dodge,
    BlendMode.LIGHTEN_COLOR to R.string.ly_img_editor_blendmode_lighten_color,
    BlendMode.DARKEN_COLOR to R.string.ly_img_editor_blendmode_darken_color,
    BlendMode.OVERLAY to R.string.ly_img_editor_blendmode_overlay,
    BlendMode.SOFT_LIGHT to R.string.ly_img_editor_blendmode_soft_light,
    BlendMode.HARD_LIGHT to R.string.ly_img_editor_blendmode_hard_light,
    BlendMode.VIVID_LIGHT to R.string.ly_img_editor_blendmode_vivid_light,
    BlendMode.LINEAR_LIGHT to R.string.ly_img_editor_blendmode_linear_light,
    BlendMode.PIN_LIGHT to R.string.ly_img_editor_blendmode_pin_light,
    BlendMode.HARD_MIX to R.string.ly_img_editor_blendmode_hard_mix,
    BlendMode.DIFFERENCE to R.string.ly_img_editor_blendmode_difference,
    BlendMode.EXCLUSION to R.string.ly_img_editor_blendmode_exclusion,
    BlendMode.SUBTRACT to R.string.ly_img_editor_blendmode_subtract,
    BlendMode.DIVIDE to R.string.ly_img_editor_blendmode_divide,
    BlendMode.HUE to R.string.ly_img_editor_blendmode_hue,
    BlendMode.SATURATION to R.string.ly_img_editor_blendmode_saturation,
    BlendMode.COLOR to R.string.ly_img_editor_blendmode_color,
    BlendMode.LUMINOSITY to R.string.ly_img_editor_blendmode_luminosity,
)

val blendModesList = blendModes.toList()

fun getBlendModeStringResource(blendMode: BlendMode) = blendModes[blendMode]
