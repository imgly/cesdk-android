package ly.img.editor.base.engine

import ly.img.editor.base.R
import ly.img.editor.base.components.PropertyOption
import ly.img.engine.AnimationEasingType
import ly.img.engine.AnimationType

fun AnimationType.getAvailableProperties() = when (this) {
    AnimationType.Slide -> listOf(
        duration,
        easing,
        directionEnum(),
        fade(),
        writingStyle,
    )
    AnimationType.Pan -> listOf(
        duration,
        easing,
        directionEnum(),
        distance(),
        fade(),
        writingStyle,
    )
    AnimationType.Fade -> listOf(
        duration,
        easing,
        writingStyle,
    )
    AnimationType.Blur -> listOf(
        duration,
        easing,
        fade(),
        intensity(),
        writingStyle,
    )
    AnimationType.Grow -> listOf(
        duration,
        easing,
        directionPerpendicular(),
        writingStyle,
    )
    AnimationType.Zoom -> listOf(
        duration,
        easing,
        fade(),
        writingStyle,
    )
    AnimationType.Pop -> listOf(
        duration,
        writingStyle,
    )
    AnimationType.Wipe -> listOf(
        duration,
        easing,
        directionEnum(),
        writingStyle,
    )
    AnimationType.Baseline -> listOf(
        duration,
        easing,
        directionEnum(),
        writingStyle,
    )
    AnimationType.CropZoom -> listOf(
        duration,
        easing,
        fade(),
        scale(),
        writingStyle,
    )
    AnimationType.Spin -> listOf(
        duration,
        easing,
        directionClock(),
        fade(),
        intensity(),
        writingStyle,
    )
    AnimationType.SpinLoop -> listOf(
        duration,
        directionClock(),
    )
    AnimationType.FadeLoop -> listOf(
        duration,
    )
    AnimationType.BlurLoop -> listOf(
        duration,
        intensity(),
    )
    AnimationType.PulsatingLoop -> listOf(
        duration,
        intensity(),
    )
    AnimationType.BreathingLoop -> listOf(
        duration,
        intensity(),
    )
    AnimationType.JumpLoop -> listOf(
        duration,
        directionEnum(),
        intensity(),
    )
    AnimationType.SqueezeLoop -> listOf(
        duration,
    )
    AnimationType.SwayLoop -> listOf(
        duration,
        intensity(),
    )
    AnimationType.TypewriterText -> listOf(
        duration,
        writingStyleShort(),
    )
    AnimationType.BlockSwipeText -> listOf(
        duration,
        directionEnum(),
        writingStyle,
    )
    AnimationType.SpreadText -> listOf(
        duration,
        easing,
        fade(),
        intensity(),
    )
    AnimationType.MergeText -> listOf(
        duration,
        easing,
        directionEnum(),
        intensity(),
    )
    AnimationType.KenBurns -> listOf(
        duration,
        easing,
        directionEnum(),
        travelDistance(),
        zoomIntensity(),
        fade(),
    )
}

private val duration = Property(
    titleRes = R.string.ly_img_editor_animation_duration,
    key = "playback/duration",
    valueType = PropertyValueType.Double(),
)

private val easing = Property(
    titleRes = R.string.ly_img_editor_animation_easing,
    key = "animationEasing",
    valueType = PropertyValueType.StringEnum(
        options = listOf(
            PropertyOption(R.string.ly_img_editor_animation_easing_linear, AnimationEasingType.LINEAR.key),
            PropertyOption(R.string.ly_img_editor_animation_easing_smooth_accelerate, AnimationEasingType.EASE_IN_QUINT.key),
            PropertyOption(R.string.ly_img_editor_animation_easing_smooth_decelerate, AnimationEasingType.EASE_OUT_QUINT.key),
            PropertyOption(R.string.ly_img_editor_animation_easing_smooth_natural, AnimationEasingType.EASE_IN_OUT_QUINT.key),
            PropertyOption(R.string.ly_img_editor_animation_easing_bounce_away, AnimationEasingType.EASE_IN_BACK.key),
            PropertyOption(R.string.ly_img_editor_animation_easing_bounce_in, AnimationEasingType.EASE_OUT_BACK.key),
            PropertyOption(R.string.ly_img_editor_animation_easing_bounce_double, AnimationEasingType.EASE_IN_OUT_BACK.key),
            PropertyOption(R.string.ly_img_editor_animation_easing_wiggle_away, AnimationEasingType.EASE_IN_SPRING.key),
            PropertyOption(R.string.ly_img_editor_animation_easing_wiggle_in, AnimationEasingType.EASE_OUT_SPRING.key),
            PropertyOption(R.string.ly_img_editor_animation_easing_wiggle_double, AnimationEasingType.EASE_IN_OUT_SPRING.key),
        ),
    ),
)

private fun AnimationType.intensity() = Property(
    titleRes = R.string.ly_img_editor_animation_intensity,
    key = "$key/intensity",
    valueType = PropertyValueType.Int(),
)

private fun AnimationType.directionEnum() = Property(
    titleRes = R.string.ly_img_editor_animation_direction,
    key = "$key/direction",
    valueType = PropertyValueType.StringEnum(
        options = listOf(
            PropertyOption(R.string.ly_img_editor_animation_direction_up, "Up"),
            PropertyOption(R.string.ly_img_editor_animation_direction_right, "Right"),
            PropertyOption(R.string.ly_img_editor_animation_direction_down, "Down"),
            PropertyOption(R.string.ly_img_editor_animation_direction_left, "Left"),
        ),
    ),
)

private fun AnimationType.directionClock() = Property(
    titleRes = R.string.ly_img_editor_animation_direction,
    key = "$key/direction",
    valueType = PropertyValueType.StringEnum(
        options = listOf(
            PropertyOption(R.string.ly_img_editor_animation_direction_clockwise, "Clockwise"),
            PropertyOption(R.string.ly_img_editor_animation_direction_counter_clockwise, "CounterClockwise"),
        ),
    ),
)

private fun AnimationType.directionPerpendicular() = Property(
    titleRes = R.string.ly_img_editor_animation_direction,
    key = "$key/direction",
    valueType = PropertyValueType.StringEnum(
        options = listOf(
            PropertyOption(R.string.ly_img_editor_animation_direction_horizontal, "Horizontal"),
            PropertyOption(R.string.ly_img_editor_animation_direction_vertical, "Vertical"),
            PropertyOption(R.string.ly_img_editor_animation_direction_all, "All"),
        ),
    ),
)

private fun AnimationType.distance() = Property(
    titleRes = R.string.ly_img_editor_animation_distance,
    key = "$key/distance",
    valueType = PropertyValueType.Float(),
)

private fun AnimationType.fade() = Property(
    titleRes = R.string.ly_img_editor_animation_fade,
    key = "$key/fade",
    valueType = PropertyValueType.Boolean,
)

private fun AnimationType.scale() = Property(
    titleRes = R.string.ly_img_editor_animation_scale,
    key = "$key/scale",
    valueType = PropertyValueType.Float(),
)

private val writingStyle = Property(
    titleRes = R.string.ly_img_editor_animation_writing_style,
    key = "textWritingStyle",
    valueType = PropertyValueType.StringEnum(
        options = listOf(
            PropertyOption(R.string.ly_img_editor_animation_writing_style_block, "Block"),
            PropertyOption(R.string.ly_img_editor_animation_writing_style_line, "Line"),
            PropertyOption(R.string.ly_img_editor_animation_writing_style_character, "Character"),
            PropertyOption(R.string.ly_img_editor_animation_writing_style_word, "Word"),
        ),
    ),
)

private fun AnimationType.writingStyleShort() = Property(
    titleRes = R.string.ly_img_editor_animation_writing_style,
    key = "$key/writingStyle",
    valueType = PropertyValueType.StringEnum(
        options = listOf(
            PropertyOption(R.string.ly_img_editor_animation_writing_style_character, "Character"),
            PropertyOption(R.string.ly_img_editor_animation_writing_style_word, "Word"),
        ),
    ),
)

private fun AnimationType.travelDistance() = Property(
    titleRes = R.string.ly_img_editor_animation_distance,
    key = "$key/travelDistanceRatio",
    valueType = PropertyValueType.Float(),
)

private fun AnimationType.zoomIntensity() = Property(
    titleRes = R.string.ly_img_editor_animation_zoom_intensity,
    key = "$key/zoomIntensity",
    valueType = PropertyValueType.Float(),
)
