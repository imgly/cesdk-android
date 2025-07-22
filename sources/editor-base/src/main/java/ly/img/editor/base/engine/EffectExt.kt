package ly.img.editor.base.engine

import ly.img.editor.core.R
import ly.img.engine.EffectType

/*
 * Only one effect of the same group can be applied at a time.
 * So if you have two effects of the same group, we need to remove the first one before applying the second one.
 * This Group helps to identify the group of an effect, in the effect list.
 */
enum class EffectGroup {
    Filter,
    FxEffect,
    Adjustments,
}

fun EffectType.getGroup() = when (this) {
    EffectType.LutFilter,
    EffectType.DuoToneFilter,
    ->
        EffectGroup.Filter

    EffectType.CrossCut,
    EffectType.DotPattern,
    EffectType.ExtrudeBlur,
    EffectType.Glow,
    EffectType.HalfTone,
    EffectType.Linocut,
    EffectType.Liquid,
    EffectType.Mirror,
    EffectType.Outliner,
    EffectType.Pixelize,
    EffectType.Posterize,
    EffectType.RadialPixel,
    EffectType.Sharpie,
    EffectType.Shifter,
    EffectType.TiltShift,
    EffectType.TvGlitch,
    EffectType.Vignette,
    EffectType.Recolor,
    EffectType.GreenScreen,
    ->
        EffectGroup.FxEffect

    EffectType.Adjustments ->
        EffectGroup.Adjustments
}

fun EffectType.getProperties() = when (this) {
    EffectType.Adjustments -> listOf(
        Property(
            key = "$key/brightness",
            titleRes = R.string.ly_img_editor_sheet_adjustments_label_brightness,
            valueType = PropertyValueType.Float(
                range = -1F..1F,
            ),
        ),
        Property(
            key = "$key/saturation",
            titleRes = R.string.ly_img_editor_sheet_adjustments_label_saturation,
            valueType = PropertyValueType.Float(
                range = -1F..1F,
            ),
        ),
        Property(
            key = "$key/contrast",
            titleRes = R.string.ly_img_editor_sheet_adjustments_label_contrast,
            valueType = PropertyValueType.Float(
                range = -1F..1F,
            ),
        ),
        Property(
            key = "$key/gamma",
            titleRes = R.string.ly_img_editor_sheet_adjustments_label_gamma,
            valueType = PropertyValueType.Float(
                range = -1F..1F,
            ),
        ),
        Property(
            key = "$key/clarity",
            titleRes = R.string.ly_img_editor_sheet_adjustments_label_clarity,
            valueType = PropertyValueType.Float(
                range = -1F..1F,
            ),
        ),
        Property(
            key = "$key/exposure",
            titleRes = R.string.ly_img_editor_sheet_adjustments_label_exposure,
            valueType = PropertyValueType.Float(
                range = -1F..1F,
            ),
        ),
        Property(
            key = "$key/shadows",
            titleRes = R.string.ly_img_editor_sheet_adjustments_label_shadows,
            valueType = PropertyValueType.Float(
                range = -1F..1F,
            ),
        ),
        Property(
            key = "$key/highlights",
            titleRes = R.string.ly_img_editor_sheet_adjustments_label_highlights,
            valueType = PropertyValueType.Float(
                range = -1F..1F,
            ),
        ),
        Property(
            key = "$key/blacks",
            titleRes = R.string.ly_img_editor_sheet_adjustments_label_blacks,
            valueType = PropertyValueType.Float(
                range = -1F..1F,
            ),
        ),
        Property(
            key = "$key/whites",
            titleRes = R.string.ly_img_editor_sheet_adjustments_label_whites,
            valueType = PropertyValueType.Float(
                range = -1F..1F,
            ),
        ),
        Property(
            key = "$key/temperature",
            titleRes = R.string.ly_img_editor_sheet_adjustments_label_temperature,
            valueType = PropertyValueType.Float(
                range = -1F..1F,
            ),
        ),
        Property(
            key = "$key/sharpness",
            titleRes = R.string.ly_img_editor_sheet_adjustments_label_sharpness,
            valueType = PropertyValueType.Float(
                range = -1F..1F,
            ),
        ),
    )
    EffectType.LutFilter -> listOf(
        Property(
            key = "$key/intensity",
            titleRes = R.string.ly_img_editor_sheet_filter_label_intensity,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
            ),
        ),
    )
    EffectType.DuoToneFilter -> listOf(
        Property(
            key = "$key/intensity",
            titleRes = R.string.ly_img_editor_sheet_filter_label_intensity,
            valueType = PropertyValueType.Float(
                range = -1F..1F,
            ),
        ),
    )
    EffectType.Pixelize -> listOf(
        Property(
            key = "$key/horizontalPixelSize",
            titleRes = R.string.ly_img_editor_sheet_effect_label_pixelize_horizontal_count,
            valueType = PropertyValueType.Int(
                range = 5..50,
            ),
        ),
        Property(
            key = "$key/verticalPixelSize",
            titleRes = R.string.ly_img_editor_sheet_effect_label_pixelize_vertical_count,
            valueType = PropertyValueType.Int(
                range = 5..50,
            ),
        ),
    )
    EffectType.RadialPixel -> listOf(
        // Radial Pixel Effect
        Property(
            key = "$key/radius",
            titleRes = R.string.ly_img_editor_sheet_effect_label_radial_pixel_row_pixels,
            valueType = PropertyValueType.Float(
                range = 0.05F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/segments",
            titleRes = R.string.ly_img_editor_sheet_effect_label_radial_pixel_row_size,
            valueType = PropertyValueType.Float(
                range = 0.01F..1F,
                step = 0.01F,
            ),
        ),
    )
    EffectType.CrossCut -> listOf(
        Property(
            key = "$key/slices",
            titleRes = R.string.ly_img_editor_sheet_effect_label_cross_cut_horizontal_cuts,
            valueType = PropertyValueType.Float(
                range = 1F..10F,
                step = 1F,
            ),
        ),
        Property(
            key = "$key/offset",
            titleRes = R.string.ly_img_editor_sheet_effect_label_cross_cut_horizontal_offset,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/speedV",
            titleRes = R.string.ly_img_editor_sheet_effect_label_cross_cut_vertical_offset,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/time",
            titleRes = R.string.ly_img_editor_sheet_effect_label_cross_cut_variation,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
    )
    EffectType.Liquid -> listOf(
        // Liquid Effect
        Property(
            key = "$key/amount",
            titleRes = R.string.ly_img_editor_sheet_effect_label_liquid_intensity,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/scale",
            titleRes = R.string.ly_img_editor_sheet_effect_label_liquid_scale,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/time",
            titleRes = R.string.ly_img_editor_sheet_effect_label_liquid_variation,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
    )
    EffectType.Outliner -> listOf(
        Property(
            key = "$key/amount",
            titleRes = R.string.ly_img_editor_sheet_effect_label_outliner_intensity,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/passthrough",
            titleRes = R.string.ly_img_editor_sheet_effect_label_outliner_blending,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
    )
    EffectType.DotPattern -> listOf(
        Property(
            key = "$key/dots",
            titleRes = R.string.ly_img_editor_sheet_effect_label_dot_pattern_dots,
            valueType = PropertyValueType.Float(
                range = 1F..80F,
                step = 1F,
            ),
        ),
        Property(
            key = "$key/size",
            titleRes = R.string.ly_img_editor_sheet_effect_label_dot_pattern_size,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/blur",
            titleRes = R.string.ly_img_editor_sheet_effect_label_dot_pattern_blur,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
    )
    EffectType.Posterize -> listOf(
        Property(
            key = "$key/levels",
            titleRes = R.string.ly_img_editor_sheet_effect_label_posterize_levels,
            valueType = PropertyValueType.Float(
                range = 1F..15F,
                step = 1F,
            ),
        ),
    )
    EffectType.TvGlitch -> listOf(
        Property(
            key = "$key/distortion",
            titleRes = R.string.ly_img_editor_sheet_effect_label_tv_glitch_rough_distortion,
            valueType = PropertyValueType.Float(
                range = 0F..10F,
                step = 0.1F,
            ),
        ),
        Property(
            key = "$key/distortion2",
            titleRes = R.string.ly_img_editor_sheet_effect_label_tv_glitch_fine_distortion,
            valueType = PropertyValueType.Float(
                range = 0F..5F,
                step = 0.05F,
            ),
        ),
        Property(
            key = "$key/speed",
            titleRes = R.string.ly_img_editor_sheet_effect_label_tv_glitch_variance,
            valueType = PropertyValueType.Float(
                range = 0F..5F,
                step = 0.05F,
            ),
        ),
        Property(
            key = "$key/rollSpeed",
            titleRes = R.string.ly_img_editor_sheet_effect_label_tv_glitch_vertical_offset,
            valueType = PropertyValueType.Float(
                range = 0F..3F,
                step = 0.1F,
            ),
        ),
    )
    EffectType.HalfTone -> listOf(
        Property(
            key = "$key/angle",
            titleRes = R.string.ly_img_editor_sheet_effect_label_half_tone_angle,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/scale",
            titleRes = R.string.ly_img_editor_sheet_effect_label_half_tone_scale,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
    )
    EffectType.Linocut -> listOf(
        Property(
            key = "$key/scale",
            titleRes = R.string.ly_img_editor_sheet_effect_label_linocut_scale,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
    )
    EffectType.Shifter -> listOf(
        Property(
            key = "$key/amount",
            titleRes = R.string.ly_img_editor_sheet_effect_label_shifter_distance,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/angle",
            titleRes = R.string.ly_img_editor_sheet_effect_label_shifter_direction,
            valueType = PropertyValueType.Float(
                range = 0F..6.3F,
                step = 0.1F,
            ),
        ),
    )
    EffectType.Mirror -> listOf(
        Property(
            key = "$key/side",
            titleRes = R.string.ly_img_editor_sheet_effect_label_mirror_side,
            valueType = PropertyValueType.Int(
                range = 0..3,
            ),
        ),
    )
    EffectType.Glow -> listOf(
        Property(
            key = "$key/size",
            titleRes = R.string.ly_img_editor_sheet_effect_label_glow_bloom,
            valueType = PropertyValueType.Float(
                range = 0F..10F,
                step = 0.1F,
            ),
        ),
        Property(
            key = "$key/amount",
            titleRes = R.string.ly_img_editor_sheet_effect_label_glow_intensity,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/darkness",
            titleRes = R.string.ly_img_editor_sheet_effect_label_glow_darkening,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
    )
    EffectType.Vignette -> listOf(
        // Vignette Effect
        Property(
            key = "$key/offset",
            titleRes = R.string.ly_img_editor_sheet_effect_label_vignette_size,
            valueType = PropertyValueType.Float(
                range = 0F..5F,
                step = 0.05F,
            ),
        ),
        Property(
            key = "$key/darkness",
            titleRes = R.string.ly_img_editor_sheet_effect_label_vignette_color,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
    )
    EffectType.TiltShift -> listOf(
        Property(
            key = "$key/amount",
            titleRes = R.string.ly_img_editor_sheet_effect_label_tilt_shift_intensity,
            valueType = PropertyValueType.Float(
                range = 0F..0.02F,
                step = 0.001F,
            ),
        ),
        Property(
            key = "$key/position",
            titleRes = R.string.ly_img_editor_sheet_effect_label_tilt_shift_position,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
    )
    EffectType.Recolor -> listOf(
        Property(
            key = "$key/fromColor",
            titleRes = R.string.ly_img_editor_sheet_effect_label_recolor_source_color,
            valueType = PropertyValueType.Color(),
        ),
        Property(
            key = "$key/toColor",
            titleRes = R.string.ly_img_editor_sheet_effect_label_recolor_target_color,
            valueType = PropertyValueType.Color(),
        ),
        Property(
            key = "$key/colorMatch",
            titleRes = R.string.ly_img_editor_sheet_effect_label_recolor_color_match,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/brightnessMatch",
            titleRes = R.string.ly_img_editor_sheet_effect_label_recolor_brightness_match,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/smoothness",
            titleRes = R.string.ly_img_editor_sheet_effect_label_recolor_smoothness,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
    )
    EffectType.GreenScreen -> listOf(
        Property(
            key = "$key/fromColor",
            titleRes = R.string.ly_img_editor_sheet_effect_label_green_screen_source_color,
            valueType = PropertyValueType.Color(),
        ),
        Property(
            key = "$key/colorMatch",
            titleRes = R.string.ly_img_editor_sheet_effect_label_green_screen_color_match,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/smoothness",
            titleRes = R.string.ly_img_editor_sheet_effect_label_green_screen_smoothness,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/spill",
            titleRes = R.string.ly_img_editor_sheet_effect_label_green_screen_spill,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
    )
    EffectType.ExtrudeBlur -> listOf(
        Property(
            key = "$key/amount",
            titleRes = R.string.ly_img_editor_sheet_effect_label_extrude_blur_intensity,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
    )
    EffectType.Sharpie -> emptyList()
}
