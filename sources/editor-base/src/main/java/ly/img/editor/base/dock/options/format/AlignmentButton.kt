package ly.img.editor.base.dock.options.format

import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import ly.img.editor.base.components.ToggleIconButton
import ly.img.editor.core.R
import ly.img.editor.core.ui.iconpack.FormatAlignLeftAuto
import ly.img.editor.core.ui.iconpack.FormatAlignRightAuto
import ly.img.editor.core.ui.iconpack.Formataligncenter
import ly.img.editor.core.ui.iconpack.Formatalignleft
import ly.img.editor.core.ui.iconpack.Formatalignright
import ly.img.editor.core.ui.iconpack.IconPack
import ly.img.editor.core.ui.iconpack.Verticalalignbottom
import ly.img.editor.core.ui.iconpack.Verticalaligncenter
import ly.img.editor.core.ui.iconpack.Verticalaligntop

@Composable
fun <T : Alignment> AlignmentButton(
    alignment: Alignment,
    currentAlignment: T,
    effectiveAlignment: HorizontalAlignment? = null,
    changeAlignment: (T) -> Unit,
) {
    ToggleIconButton(
        checked = currentAlignment == alignment,
        onCheckedChange = {
            @Suppress("UNCHECKED_CAST")
            changeAlignment(alignment as T)
        },
    ) {
        Icon(
            imageVector = when (alignment) {
                HorizontalAlignment.Left -> IconPack.Formatalignleft
                HorizontalAlignment.Center -> IconPack.Formataligncenter
                HorizontalAlignment.Right -> IconPack.Formatalignright
                HorizontalAlignment.Auto -> when (effectiveAlignment) {
                    HorizontalAlignment.Right -> IconPack.FormatAlignRightAuto
                    else -> IconPack.FormatAlignLeftAuto
                }
                VerticalAlignment.Bottom -> IconPack.Verticalalignbottom
                VerticalAlignment.Center -> IconPack.Verticalaligncenter
                VerticalAlignment.Top -> IconPack.Verticalaligntop
            },
            contentDescription = when (alignment) {
                HorizontalAlignment.Left -> stringResource(R.string.ly_img_editor_sheet_format_text_alignment_horizontal_option_left)
                HorizontalAlignment.Center -> stringResource(R.string.ly_img_editor_sheet_format_text_alignment_horizontal_option_center)
                HorizontalAlignment.Right -> stringResource(R.string.ly_img_editor_sheet_format_text_alignment_horizontal_option_right)
                HorizontalAlignment.Auto -> stringResource(R.string.ly_img_editor_sheet_format_text_alignment_horizontal_option_auto)
                VerticalAlignment.Bottom -> stringResource(R.string.ly_img_editor_sheet_format_text_alignment_vertical_option_bottom)
                VerticalAlignment.Center -> stringResource(R.string.ly_img_editor_sheet_format_text_alignment_vertical_option_center)
                VerticalAlignment.Top -> stringResource(R.string.ly_img_editor_sheet_format_text_alignment_vertical_option_top)
            },
        )
    }
}
