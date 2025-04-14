package ly.img.editor.base.engine

import ly.img.editor.base.R
import ly.img.editor.base.components.PropertyOption
import ly.img.engine.AnimationEasingType
import ly.img.engine.AnimationType

fun AnimationType.getProperties(designBlockDuration: Double) = buildList {
    val isTextAnimation = key.endsWith("text")
    add(duration(designBlockDuration))
    addAll(getCustomProperties(isTextAnimation = isTextAnimation))
}

private fun AnimationType.getCustomProperties(isTextAnimation: Boolean) = when (this) {
    AnimationType.Slide -> buildList {
        add(easing)
        add(directionRadian())
        add(fade())
        if (isTextAnimation) {
            add(writingStyle)
        }
    }
    AnimationType.Pan -> buildList {
        add(easing)
        add(directionRadian())
        add(distance())
        add(fade())
        if (isTextAnimation) {
            add(writingStyle)
        }
    }
    AnimationType.Fade -> buildList {
        add(easing)
        if (isTextAnimation) {
            add(writingStyle)
        }
    }
    AnimationType.Blur -> buildList {
        add(easing)
        add(fade())
        add(intensity())
        if (isTextAnimation) {
            add(writingStyle)
        }
    }
    AnimationType.Grow -> buildList {
        add(easing)
        add(directionPerpendicular())
        if (isTextAnimation) {
            add(writingStyle)
        }
    }
    AnimationType.Zoom -> buildList {
        add(easing)
        add(fade())
        if (isTextAnimation) {
            add(writingStyle)
        }
    }
    AnimationType.Pop -> buildList {
        if (isTextAnimation) {
            add(writingStyle)
        }
    }
    AnimationType.Wipe -> buildList {
        add(easing)
        add(directionEnum())
        if (isTextAnimation) {
            add(writingStyle)
        }
    }
    AnimationType.Baseline -> buildList {
        add(easing)
        add(directionEnum())
        if (isTextAnimation) {
            add(writingStyle)
        }
    }
    AnimationType.CropZoom -> buildList {
        add(easing)
        add(fade())
        add(scale())
        if (isTextAnimation) {
            add(writingStyle)
        }
    }
    AnimationType.Spin -> buildList {
        add(easing)
        add(directionClock())
        add(fade())
        add(intensity())
        if (isTextAnimation) {
            add(writingStyle)
        }
    }
    AnimationType.SpinLoop -> buildList {
        add(directionClock())
    }
    AnimationType.FadeLoop -> emptyList()
    AnimationType.BlurLoop -> buildList {
        add(intensity())
    }
    AnimationType.PulsatingLoop -> buildList {
        add(intensity())
    }
    AnimationType.BreathingLoop -> buildList {
        add(intensity())
    }
    AnimationType.JumpLoop -> buildList {
        add(directionEnum())
        add(intensity())
    }
    AnimationType.SqueezeLoop -> emptyList()
    AnimationType.SwayLoop -> buildList {
        add(intensity())
    }
    AnimationType.TypewriterText -> buildList {
        add(writingStyleShort())
    }
    AnimationType.BlockSwipeText -> buildList {
        add(directionEnum())
        add(writingStyle)
    }
    AnimationType.SpreadText -> buildList {
        add(easing)
        add(fade())
        add(intensity())
    }
    AnimationType.MergeText -> buildList {
        add(easing)
        add(directionEnum())
        add(intensity())
    }
    AnimationType.KenBurns -> buildList {
        add(easing)
        add(directionEnum())
        add(travelDistance())
        add(zoomIntensity())
        add(fade())
    }
}

private fun duration(designBlockDuration: Double) = Property(
    titleRes = R.string.ly_img_editor_animation_duration,
    key = "playback/duration",
    valueType = PropertyValueType.Double(
        range = 0.1..designBlockDuration,
        step = 0.1,
    ),
)

private val easing = Property(
    titleRes = R.string.ly_img_editor_animation_easing,
    key = "animationEasing",
    valueType = PropertyValueType.StringEnum(
        options = listOf(
            PropertyOption(R.string.ly_img_editor_animation_easing_linear, AnimationEasingType.LINEAR.key),
            PropertyOption(R.string.ly_img_editor_animation_easing_smooth_accelerate, AnimationEasingType.EASE_IN.key),
            PropertyOption(R.string.ly_img_editor_animation_easing_smooth_decelerate, AnimationEasingType.EASE_OUT.key),
            PropertyOption(R.string.ly_img_editor_animation_easing_smooth_natural, AnimationEasingType.EASE_IN_OUT.key),
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
    valueType = PropertyValueType.Float(
        range = 0F..1F,
        step = 0.01F,
    ),
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

private fun AnimationType.directionRadian() = Property(
    titleRes = R.string.ly_img_editor_animation_direction,
    key = "$key/direction",
    valueType = PropertyValueType.FloatEnum(
        options = listOf(
            PropertyOption(R.string.ly_img_editor_animation_direction_up, 3F / 2F * Math.PI.toFloat()),
            PropertyOption(R.string.ly_img_editor_animation_direction_right, 0F),
            PropertyOption(R.string.ly_img_editor_animation_direction_down, 1F / 2F * Math.PI.toFloat()),
            PropertyOption(R.string.ly_img_editor_animation_direction_left, Math.PI.toFloat()),
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
    valueType = PropertyValueType.Float(
        range = 0F..1F,
        step = 0.01F,
    ),
)

private fun AnimationType.fade() = Property(
    titleRes = R.string.ly_img_editor_animation_fade,
    key = "$key/fade",
    valueType = PropertyValueType.Boolean,
)

private fun AnimationType.scale() = Property(
    titleRes = R.string.ly_img_editor_animation_scale,
    key = "$key/scale",
    valueType = PropertyValueType.Float(
        range = 1.1F..2.5F,
        step = 0.01F,
    ),
)

private val writingStyle = Property(
    titleRes = R.string.ly_img_editor_animation_writing_style,
    key = "textAnimationWritingStyle",
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
    valueType = PropertyValueType.Float(
        range = 0F..1.25F,
        step = 0.01F,
    ),
)

private fun AnimationType.zoomIntensity() = Property(
    titleRes = R.string.ly_img_editor_animation_zoom_intensity,
    key = "$key/zoomIntensity",
    valueType = PropertyValueType.Float(
        range = -3F..3F,
        step = 0.01F,
    ),
)
