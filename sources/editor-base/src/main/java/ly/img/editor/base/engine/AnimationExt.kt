package ly.img.editor.base.engine

import ly.img.editor.base.components.PropertyOption
import ly.img.editor.core.R
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
        scaleFactor(),
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
    AnimationType.ScaleLoop -> listOf(
        duration,
        easing,
        directionScaleLoop(),
        startScale(),
        endScale(),
        startDelay(),
        holdDuration(),
        easingDuration(),
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
    title = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_duration),
    key = "playback/duration",
    valueType = PropertyValueType.Double(),
)

private val easing = Property(
    title = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_easing),
    key = "animationEasing",
    valueType = PropertyValueType.StringEnum(
        options = listOf(
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_easing_option_linear),
                value = AnimationEasingType.LINEAR.key,
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_easing_option_smooth_accelerate),
                value = AnimationEasingType.EASE_IN_QUINT.key,
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_easing_option_smooth_decelerate),
                value = AnimationEasingType.EASE_OUT_QUINT.key,
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_easing_option_smooth_natural),
                value = AnimationEasingType.EASE_IN_OUT_QUINT.key,
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_easing_option_bounce_away),
                value = AnimationEasingType.EASE_IN_BACK.key,
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_easing_option_bounce_in),
                value = AnimationEasingType.EASE_OUT_BACK.key,
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_easing_option_bounce_double),
                value = AnimationEasingType.EASE_IN_OUT_BACK.key,
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_easing_option_wiggle_away),
                value = AnimationEasingType.EASE_IN_SPRING.key,
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_easing_option_wiggle_in),
                value = AnimationEasingType.EASE_OUT_SPRING.key,
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_easing_option_wiggle_double),
                value = AnimationEasingType.EASE_IN_OUT_SPRING.key,
            ),
        ),
    ),
)

private fun AnimationType.intensity() = Property(
    title = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_intensity),
    key = "$key/intensity",
    valueType = PropertyValueType.Int(),
)

private fun AnimationType.directionEnum() = Property(
    title = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_direction),
    key = "$key/direction",
    valueType = PropertyValueType.StringEnum(
        options = listOf(
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_direction_up),
                value = "Up",
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_direction_right),
                value = "Right",
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_direction_down),
                value = "Down",
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_direction_left),
                value = "Left",
            ),
        ),
    ),
)

private fun AnimationType.directionClock() = Property(
    title = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_direction),
    key = "$key/direction",
    valueType = PropertyValueType.StringEnum(
        options = listOf(
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_direction_clockwise),
                value = "Clockwise",
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_direction_counter_clockwise),
                value = "CounterClockwise",
            ),
        ),
    ),
)

private fun AnimationType.directionPerpendicular() = Property(
    title = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_direction),
    key = "$key/direction",
    valueType = PropertyValueType.StringEnum(
        options = listOf(
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_direction_horizontal),
                value = "Horizontal",
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_direction_vertical),
                value = "Vertical",
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_direction_all),
                value = "All",
            ),
        ),
    ),
)

private fun AnimationType.distance() = Property(
    title = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_distance),
    key = "$key/distance",
    valueType = PropertyValueType.Float(),
)

private fun AnimationType.fade() = Property(
    title = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_fade),
    key = "$key/fade",
    valueType = PropertyValueType.Boolean,
)

private fun AnimationType.scale() = Property(
    title = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_scale),
    key = "$key/scale",
    valueType = PropertyValueType.Float(),
)

private fun AnimationType.scaleFactor() = Property(
    title = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_scale_factor),
    key = "$key/scaleFactor",
    valueType = PropertyValueType.Float(
        range = 0f..1f,
        step = 0.01f,
    ),
)

private val writingStyle = Property(
    title = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_writing_style),
    key = "textWritingStyle",
    valueType = PropertyValueType.StringEnum(
        options = listOf(
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_writing_style_option_block),
                value = "Block",
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_writing_style_option_line),
                value = "Line",
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_writing_style_option_character),
                value = "Character",
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_writing_style_option_word),
                value = "Word",
            ),
        ),
    ),
)

private fun AnimationType.writingStyleShort() = Property(
    title = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_writing_style),
    key = "$key/writingStyle",
    valueType = PropertyValueType.StringEnum(
        options = listOf(
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_writing_style_option_character),
                value = "Character",
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_writing_style_option_word),
                value = "Word",
            ),
        ),
    ),
)

private fun AnimationType.travelDistance() = Property(
    title = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_distance),
    key = "$key/travelDistanceRatio",
    valueType = PropertyValueType.Float(),
)

private fun AnimationType.zoomIntensity() = Property(
    title = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_zoom_intensity),
    key = "$key/zoomIntensity",
    valueType = PropertyValueType.Float(),
)

private fun AnimationType.directionScaleLoop() = Property(
    title = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_direction),
    key = "$key/direction",
    valueType = PropertyValueType.StringEnum(
        options = listOf(
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_direction_all),
                value = "All",
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_direction_horizontal),
                value = "Horizontal",
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_direction_vertical),
                value = "Vertical",
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_direction_top_left),
                value = "TopLeft",
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_direction_top_right),
                value = "TopRight",
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_direction_bottom_left),
                value = "BottomLeft",
            ),
            PropertyOption(
                text = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_direction_bottom_right),
                value = "BottomRight",
            ),
        ),
    ),
)

private fun AnimationType.startScale() = Property(
    title = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_start_scale),
    key = "$key/startScale",
    valueType = PropertyValueType.Float(),
)

private fun AnimationType.endScale() = Property(
    title = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_end_scale),
    key = "$key/endScale",
    valueType = PropertyValueType.Float(),
)

private fun AnimationType.startDelay() = Property(
    title = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_start_delay),
    key = "$key/startDelay",
    valueType = PropertyValueType.Double(),
)

private fun AnimationType.holdDuration() = Property(
    title = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_hold_duration),
    key = "$key/holdDuration",
    valueType = PropertyValueType.Double(),
)

private fun AnimationType.easingDuration() = Property(
    title = PropertyText.Resource(R.string.ly_img_editor_sheet_animations_label_easing_duration),
    key = "$key/easingDuration",
    valueType = PropertyValueType.Double(),
)
