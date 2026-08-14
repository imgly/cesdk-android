package ly.img.editor.base.engine

import ly.img.editor.base.components.PropertyOption
import ly.img.editor.core.R
import ly.img.engine.AnimationEasingType
import ly.img.engine.TransitionType

fun TransitionType.getAvailableProperties(): List<Property> = when (this) {
    TransitionType.CrossFade -> listOf(
        duration,
        easing,
        morph(),
    )
    TransitionType.CrossBlur -> listOf(
        duration,
        easing,
        blur(),
        morph(),
    )
    TransitionType.CrossSpin -> listOf(
        duration,
        easing,
        spinDirection(),
        intensity(),
    )
    TransitionType.CrossZoom -> listOf(
        duration,
        easing,
        zoom(),
        morph(),
    )
    TransitionType.CrossWarp -> listOf(
        duration,
        easing,
        zoom(),
        morph(),
    )
    TransitionType.Push -> listOf(
        duration,
        easing,
        direction(),
        morph(),
    )
    TransitionType.Slide -> listOf(
        duration,
        easing,
        direction(),
        morph(),
    )
    TransitionType.Stack -> listOf(
        duration,
        easing,
        direction(),
        morph(),
    )
    TransitionType.Splice -> listOf(
        duration,
        easing,
        direction(),
        bandCount(),
        morph(),
    )
    TransitionType.DiagonalSplice -> listOf(
        duration,
        easing,
        diagonalSpliceDirection(),
        morph(),
    )
    TransitionType.Fade -> listOf(
        duration,
        easing,
        color(),
        morph(),
    )
    TransitionType.FadeToWhite -> listOf(
        duration,
        easing,
        morph(),
    )
    TransitionType.FadeToBlack -> listOf(
        duration,
        easing,
        morph(),
    )
    TransitionType.ColorWipe -> listOf(
        duration,
        easing,
        direction(),
        color(),
        morph(),
    )
    TransitionType.LineWipe -> listOf(
        duration,
        easing,
        morph(),
    )
    TransitionType.Wipe -> listOf(
        duration,
        easing,
        direction(),
        morph(),
    )
    TransitionType.ClockWipe -> listOf(
        duration,
        easing,
    )
    TransitionType.Chop -> listOf(
        duration,
        easing,
        corner(),
        spinDirection(),
        morph(),
    )
    TransitionType.GradientFade -> listOf(
        duration,
        easing,
        direction(),
        color(),
        morph(),
    )
    TransitionType.TwoStripes -> listOf(
        duration,
        easing,
        stripesDirection(),
        morph(),
    )
}

private val duration = Property(
    titleRes = R.string.ly_img_editor_sheet_transition_label_duration,
    key = "playback/duration",
    valueType = PropertyValueType.Double(),
)

private val easing = Property(
    titleRes = R.string.ly_img_editor_sheet_transition_label_easing,
    key = "animationEasing",
    valueType = PropertyValueType.StringEnum(
        listOf(
            PropertyOption(R.string.ly_img_editor_sheet_animations_easing_option_linear, AnimationEasingType.LINEAR.key),
            PropertyOption(R.string.ly_img_editor_sheet_animations_easing_option_smooth_accelerate, AnimationEasingType.EASE_IN_QUINT.key),
            PropertyOption(R.string.ly_img_editor_sheet_animations_easing_option_smooth_decelerate, AnimationEasingType.EASE_OUT_QUINT.key),
            PropertyOption(R.string.ly_img_editor_sheet_animations_easing_option_smooth_natural, AnimationEasingType.EASE_IN_OUT_QUINT.key),
            PropertyOption(R.string.ly_img_editor_sheet_animations_easing_option_bounce_away, AnimationEasingType.EASE_IN_BACK.key),
            PropertyOption(R.string.ly_img_editor_sheet_animations_easing_option_bounce_in, AnimationEasingType.EASE_OUT_BACK.key),
            PropertyOption(R.string.ly_img_editor_sheet_animations_easing_option_bounce_double, AnimationEasingType.EASE_IN_OUT_BACK.key),
            PropertyOption(R.string.ly_img_editor_sheet_animations_easing_option_wiggle_away, AnimationEasingType.EASE_IN_SPRING.key),
            PropertyOption(R.string.ly_img_editor_sheet_animations_easing_option_wiggle_in, AnimationEasingType.EASE_OUT_SPRING.key),
            PropertyOption(R.string.ly_img_editor_sheet_animations_easing_option_wiggle_double, AnimationEasingType.EASE_IN_OUT_SPRING.key),
        ),
    ),
)

private fun TransitionType.direction() = property(
    suffix = "direction",
    title = R.string.ly_img_editor_sheet_transition_label_direction,
    valueType = transitionDirections,
)

private fun TransitionType.spinDirection() = property(
    suffix = "direction",
    title = R.string.ly_img_editor_sheet_transition_label_direction,
    valueType = spinDirections,
)

private fun TransitionType.diagonalSpliceDirection() = property(
    suffix = "direction",
    title = R.string.ly_img_editor_sheet_transition_label_direction,
    valueType = diagonalSpliceDirections,
)

private fun TransitionType.stripesDirection() = property(
    suffix = "direction",
    title = R.string.ly_img_editor_sheet_transition_label_direction,
    valueType = stripesDirections,
)

private fun TransitionType.corner() = property(
    suffix = "corner",
    title = R.string.ly_img_editor_sheet_transition_label_corner,
    valueType = corners,
)

private fun TransitionType.blur() = property(
    suffix = "sigma",
    title = R.string.ly_img_editor_sheet_transition_label_blur,
    valueType = PropertyValueType.Float(0f..100f, 1f),
)

private fun TransitionType.zoom() = property(
    suffix = "zoom",
    title = R.string.ly_img_editor_sheet_transition_label_zoom,
    valueType = PropertyValueType.Float(0f..2.5f, .05f),
)

private fun TransitionType.intensity() = property(
    suffix = "intensity",
    title = R.string.ly_img_editor_sheet_transition_label_intensity,
    valueType = PropertyValueType.Float(1f..3f, 1.0f),
)

private fun TransitionType.bandCount() = property(
    suffix = "bandCount",
    title = R.string.ly_img_editor_sheet_transition_label_band_count,
    valueType = PropertyValueType.Int(3..20),
)

private fun TransitionType.color() = property(
    suffix = "color",
    title = R.string.ly_img_editor_sheet_transition_label_color,
    valueType = PropertyValueType.Color(),
)

private fun TransitionType.morph() = property(
    suffix = "morph",
    title = R.string.ly_img_editor_sheet_transition_label_morph,
    valueType = PropertyValueType.Boolean,
)

private val transitionDirections = PropertyValueType.StringEnum(
    options = listOf(
        PropertyOption(R.string.ly_img_editor_sheet_transition_direction_up, "Up"),
        PropertyOption(R.string.ly_img_editor_sheet_transition_direction_right, "Right"),
        PropertyOption(R.string.ly_img_editor_sheet_transition_direction_down, "Down"),
        PropertyOption(R.string.ly_img_editor_sheet_transition_direction_left, "Left"),
    ),
)
private val spinDirections = PropertyValueType.StringEnum(
    options = listOf(
        PropertyOption(R.string.ly_img_editor_sheet_transition_direction_clockwise, "Clockwise"),
        PropertyOption(R.string.ly_img_editor_sheet_transition_direction_counter_clockwise, "CounterClockwise"),
    ),
)
private val diagonalSpliceDirections = PropertyValueType.StringEnum(
    options = listOf(
        PropertyOption(R.string.ly_img_editor_sheet_transition_direction_raised_ramp, "RaisedRamp"),
        PropertyOption(R.string.ly_img_editor_sheet_transition_direction_lowered_ramp, "LoweredRamp"),
    ),
)
private val stripesDirections = PropertyValueType.StringEnum(
    options = listOf(
        PropertyOption(R.string.ly_img_editor_sheet_transition_direction_horizontal, "Horizontal"),
        PropertyOption(R.string.ly_img_editor_sheet_transition_direction_vertical, "Vertical"),
    ),
)
private val corners = PropertyValueType.StringEnum(
    options = listOf(
        PropertyOption(R.string.ly_img_editor_sheet_transition_corner_top_left, "TopLeft"),
        PropertyOption(R.string.ly_img_editor_sheet_transition_corner_top_right, "TopRight"),
        PropertyOption(R.string.ly_img_editor_sheet_transition_corner_bottom_left, "BottomLeft"),
        PropertyOption(R.string.ly_img_editor_sheet_transition_corner_bottom_right, "BottomRight"),
    ),
)

private fun TransitionType.property(
    suffix: String,
    title: Int,
    valueType: PropertyValueType,
) = Property(
    titleRes = title,
    key = "$key/$suffix",
    valueType = valueType,
)
