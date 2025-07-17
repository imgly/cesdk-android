package ly.img.editor.base.engine

import ly.img.editor.base.R
import ly.img.engine.BlurType

fun BlurType.getProperties() = when (this) {
    BlurType.Uniform -> listOf(
        Property(
            key = "$key/intensity",
            titleRes = R.string.ly_img_editor_filter_blur_uniform_intensity,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
    )
    BlurType.Linear -> listOf(
        Property(
            key = "$key/blurRadius",
            titleRes = R.string.ly_img_editor_filter_blur_linear_blurradius,
            valueType = PropertyValueType.Float(
                range = 0F..100F,
                step = 0.5F,
            ),
        ),
        Property(
            key = "$key/x1",
            titleRes = R.string.ly_img_editor_filter_blur_linear_x1,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/y1",
            titleRes = R.string.ly_img_editor_filter_blur_linear_y1,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/x2",
            titleRes = R.string.ly_img_editor_filter_blur_linear_x2,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/y2",
            titleRes = R.string.ly_img_editor_filter_blur_linear_y2,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
    )
    BlurType.Mirrored -> listOf(
        Property(
            key = "$key/blurRadius",
            titleRes = R.string.ly_img_editor_filter_blur_mirrored_blurradius,
            valueType = PropertyValueType.Float(
                range = 0F..100F,
                step = 1F,
            ),
        ),
        Property(
            key = "$key/gradientSize",
            titleRes = R.string.ly_img_editor_filter_blur_mirrored_gradientsize,
            valueType = PropertyValueType.Float(
                range = 0F..1000F,
                step = 1F,
            ),
        ),
        Property(
            key = "$key/size",
            titleRes = R.string.ly_img_editor_filter_blur_mirrored_size,
            valueType = PropertyValueType.Float(
                range = 0F..1000F,
                step = 1F,
            ),
        ),
        Property(
            key = "$key/x1",
            titleRes = R.string.ly_img_editor_filter_blur_mirrored_x1,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/y1",
            titleRes = R.string.ly_img_editor_filter_blur_mirrored_y1,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/x2",
            titleRes = R.string.ly_img_editor_filter_blur_mirrored_x2,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/y2",
            titleRes = R.string.ly_img_editor_filter_blur_mirrored_y2,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
    )
    BlurType.Radial -> listOf(
        Property(
            key = "$key/blurRadius",
            titleRes = R.string.ly_img_editor_filter_blur_radial_blurradius,
            valueType = PropertyValueType.Float(
                range = 0F..100F,
                step = 1F,
            ),
        ),
        Property(
            key = "$key/gradientRadius",
            titleRes = R.string.ly_img_editor_filter_blur_radial_gradientradius,
            valueType = PropertyValueType.Float(
                range = 0F..1000F,
                step = 1F,
            ),
        ),
        Property(
            key = "$key/radius",
            titleRes = R.string.ly_img_editor_filter_blur_radial_radius,
            valueType = PropertyValueType.Float(
                range = 0F..1000F,
                step = 1F,
            ),
        ),
        Property(
            key = "$key/x",
            titleRes = R.string.ly_img_editor_filter_blur_radial_x,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/y",
            titleRes = R.string.ly_img_editor_filter_blur_radial_y,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
    )
}
