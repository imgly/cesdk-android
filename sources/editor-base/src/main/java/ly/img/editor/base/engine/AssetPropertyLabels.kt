package ly.img.editor.base.engine

import ly.img.editor.core.R

object AssetPropertyLabels {
    private val animationTitles = mapOf(
        "playback/duration" to R.string.ly_img_editor_sheet_animations_label_duration,
        "animationEasing" to R.string.ly_img_editor_sheet_animations_label_easing,
        "textWritingStyle" to R.string.ly_img_editor_sheet_animations_label_writing_style,
    )
    private val animationSuffixTitles = mapOf(
        "/direction" to R.string.ly_img_editor_sheet_animations_label_direction,
        "/fade" to R.string.ly_img_editor_sheet_animations_label_fade,
        "/intensity" to R.string.ly_img_editor_sheet_animations_label_intensity,
        "/distance" to R.string.ly_img_editor_sheet_animations_label_distance,
        "/scale" to R.string.ly_img_editor_sheet_animations_label_scale,
        "/scaleFactor" to R.string.ly_img_editor_sheet_animations_label_scale_factor,
        "/travelDistanceRatio" to R.string.ly_img_editor_sheet_animations_label_distance,
        "/zoomIntensity" to R.string.ly_img_editor_sheet_animations_label_zoom_intensity,
        "/writingStyle" to R.string.ly_img_editor_sheet_animations_label_writing_style,
        "/startScale" to R.string.ly_img_editor_sheet_animations_label_start_scale,
        "/endScale" to R.string.ly_img_editor_sheet_animations_label_end_scale,
        "/startDelay" to R.string.ly_img_editor_sheet_animations_label_start_delay,
        "/holdDuration" to R.string.ly_img_editor_sheet_animations_label_hold_duration,
        "/easingDuration" to R.string.ly_img_editor_sheet_animations_label_easing_duration,
    )
    private val transitionTitles = mapOf(
        "playback/duration" to R.string.ly_img_editor_sheet_transition_label_duration,
        "animationEasing" to R.string.ly_img_editor_sheet_transition_label_easing,
    )
    private val transitionSuffixTitles = mapOf(
        "/direction" to R.string.ly_img_editor_sheet_transition_label_direction,
        "/sigma" to R.string.ly_img_editor_sheet_transition_label_blur,
        "/zoom" to R.string.ly_img_editor_sheet_transition_label_zoom,
        "/intensity" to R.string.ly_img_editor_sheet_transition_label_intensity,
        "/bandCount" to R.string.ly_img_editor_sheet_transition_label_band_count,
        "/color" to R.string.ly_img_editor_sheet_transition_label_color,
        "/morph" to R.string.ly_img_editor_sheet_transition_label_morph,
        "/corner" to R.string.ly_img_editor_sheet_transition_label_corner,
    )
    private val easingOptions = mapOf(
        "Linear" to R.string.ly_img_editor_sheet_animations_easing_option_linear,
        "EaseInQuint" to R.string.ly_img_editor_sheet_animations_easing_option_smooth_accelerate,
        "EaseOutQuint" to R.string.ly_img_editor_sheet_animations_easing_option_smooth_decelerate,
        "EaseInOutQuint" to R.string.ly_img_editor_sheet_animations_easing_option_smooth_natural,
        "EaseInBack" to R.string.ly_img_editor_sheet_animations_easing_option_bounce_away,
        "EaseOutBack" to R.string.ly_img_editor_sheet_animations_easing_option_bounce_in,
        "EaseInOutBack" to R.string.ly_img_editor_sheet_animations_easing_option_bounce_double,
        "EaseInSpring" to R.string.ly_img_editor_sheet_animations_easing_option_wiggle_away,
        "EaseOutSpring" to R.string.ly_img_editor_sheet_animations_easing_option_wiggle_in,
        "EaseInOutSpring" to R.string.ly_img_editor_sheet_animations_easing_option_wiggle_double,
    )
    private val animationOptions = easingOptions + mapOf(
        "Up" to R.string.ly_img_editor_sheet_animations_label_direction_up,
        "Right" to R.string.ly_img_editor_sheet_animations_label_direction_right,
        "Down" to R.string.ly_img_editor_sheet_animations_label_direction_down,
        "Left" to R.string.ly_img_editor_sheet_animations_label_direction_left,
        "Clockwise" to R.string.ly_img_editor_sheet_animations_label_direction_clockwise,
        "CounterClockwise" to R.string.ly_img_editor_sheet_animations_label_direction_counter_clockwise,
        "Horizontal" to R.string.ly_img_editor_sheet_animations_label_direction_horizontal,
        "Vertical" to R.string.ly_img_editor_sheet_animations_label_direction_vertical,
        "All" to R.string.ly_img_editor_sheet_animations_label_direction_all,
        "TopLeft" to R.string.ly_img_editor_sheet_animations_label_direction_top_left,
        "TopRight" to R.string.ly_img_editor_sheet_animations_label_direction_top_right,
        "BottomLeft" to R.string.ly_img_editor_sheet_animations_label_direction_bottom_left,
        "BottomRight" to R.string.ly_img_editor_sheet_animations_label_direction_bottom_right,
        "Block" to R.string.ly_img_editor_sheet_animations_writing_style_option_block,
        "Line" to R.string.ly_img_editor_sheet_animations_writing_style_option_line,
        "Character" to R.string.ly_img_editor_sheet_animations_writing_style_option_character,
        "Word" to R.string.ly_img_editor_sheet_animations_writing_style_option_word,
    )
    private val transitionOptions = easingOptions + mapOf(
        "Up" to R.string.ly_img_editor_sheet_transition_direction_up,
        "Right" to R.string.ly_img_editor_sheet_transition_direction_right,
        "Down" to R.string.ly_img_editor_sheet_transition_direction_down,
        "Left" to R.string.ly_img_editor_sheet_transition_direction_left,
        "Clockwise" to R.string.ly_img_editor_sheet_transition_direction_clockwise,
        "CounterClockwise" to R.string.ly_img_editor_sheet_transition_direction_counter_clockwise,
        "Horizontal" to R.string.ly_img_editor_sheet_transition_direction_horizontal,
        "Vertical" to R.string.ly_img_editor_sheet_transition_direction_vertical,
        "RaisedRamp" to R.string.ly_img_editor_sheet_transition_direction_raised_ramp,
        "LoweredRamp" to R.string.ly_img_editor_sheet_transition_direction_lowered_ramp,
        "TopLeft" to R.string.ly_img_editor_sheet_transition_corner_top_left,
        "TopRight" to R.string.ly_img_editor_sheet_transition_corner_top_right,
        "BottomLeft" to R.string.ly_img_editor_sheet_transition_corner_bottom_left,
        "BottomRight" to R.string.ly_img_editor_sheet_transition_corner_bottom_right,
    )

    fun title(
        sourceId: String,
        property: String,
    ): PropertyText = when (sourceId) {
        "ly.img.animations" -> animationTitles[property]
            ?: animationSuffixTitles.entries.firstOrNull { property.endsWith(it.key) }?.value
        "ly.img.transitions" -> transitionTitles[property]
            ?: transitionSuffixTitles.entries.firstOrNull { property.endsWith(it.key) }?.value
        else -> null
    }?.let(PropertyText::Resource) ?: PropertyText.Raw(property)

    fun option(
        sourceId: String,
        value: String,
    ): PropertyText = when (sourceId) {
        "ly.img.animations" -> animationOptions[value]
        "ly.img.transitions" -> transitionOptions[value]
        else -> null
    }?.let(PropertyText::Resource) ?: PropertyText.Raw(value)
}
