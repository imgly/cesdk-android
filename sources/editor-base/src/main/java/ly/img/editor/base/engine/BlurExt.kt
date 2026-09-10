package ly.img.editor.base.engine

import ly.img.editor.core.R
import ly.img.engine.BlurType

fun BlurType.getProperties() = when (this) {
    BlurType.Uniform -> listOf(
        Property(
            key = "$key/intensity",
            titleRes = R.string.ly_img_editor_sheet_blur_label_intensity,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
    )
    BlurType.Linear -> listOf(
        Property(
            key = "$key/blurRadius",
            titleRes = R.string.ly_img_editor_sheet_blur_label_intensity,
            valueType = PropertyValueType.Float(
                range = 0F..100F,
                step = 0.5F,
            ),
        ),
        Property(
            key = "$key/x1",
            titleRes = R.string.ly_img_editor_sheet_blur_label_point_x1,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/y1",
            titleRes = R.string.ly_img_editor_sheet_blur_label_point_y1,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/x2",
            titleRes = R.string.ly_img_editor_sheet_blur_label_point_x2,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/y2",
            titleRes = R.string.ly_img_editor_sheet_blur_label_point_y2,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
    )
    BlurType.Mirrored -> listOf(
        Property(
            key = "$key/blurRadius",
            titleRes = R.string.ly_img_editor_sheet_blur_label_intensity,
            valueType = PropertyValueType.Float(
                range = 0F..100F,
                step = 1F,
            ),
        ),
        Property(
            key = "$key/gradientSize",
            titleRes = R.string.ly_img_editor_sheet_blur_label_gradient_size,
            valueType = PropertyValueType.Float(
                range = 0F..1000F,
                step = 1F,
            ),
        ),
        Property(
            key = "$key/size",
            titleRes = R.string.ly_img_editor_sheet_blur_label_non_blurred_size,
            valueType = PropertyValueType.Float(
                range = 0F..1000F,
                step = 1F,
            ),
        ),
        Property(
            key = "$key/x1",
            titleRes = R.string.ly_img_editor_sheet_blur_label_point_x1,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/y1",
            titleRes = R.string.ly_img_editor_sheet_blur_label_point_y1,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/x2",
            titleRes = R.string.ly_img_editor_sheet_blur_label_point_x2,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/y2",
            titleRes = R.string.ly_img_editor_sheet_blur_label_point_y2,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
    )
    BlurType.Radial -> listOf(
        Property(
            key = "$key/blurRadius",
            titleRes = R.string.ly_img_editor_sheet_blur_label_intensity,
            valueType = PropertyValueType.Float(
                range = 0F..100F,
                step = 1F,
            ),
        ),
        Property(
            key = "$key/gradientRadius",
            titleRes = R.string.ly_img_editor_sheet_blur_label_gradient_size,
            valueType = PropertyValueType.Float(
                range = 0F..1000F,
                step = 1F,
            ),
        ),
        Property(
            key = "$key/radius",
            titleRes = R.string.ly_img_editor_sheet_blur_label_non_blurred_size,
            valueType = PropertyValueType.Float(
                range = 0F..1000F,
                step = 1F,
            ),
        ),
        Property(
            key = "$key/x",
            titleRes = R.string.ly_img_editor_sheet_blur_label_point_x,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
        Property(
            key = "$key/y",
            titleRes = R.string.ly_img_editor_sheet_blur_label_point_y,
            valueType = PropertyValueType.Float(
                range = 0F..1F,
                step = 0.01F,
            ),
        ),
    )
}
