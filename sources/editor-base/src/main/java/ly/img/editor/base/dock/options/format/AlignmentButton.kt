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
import ly.img.engine.HorizontalAlignment

/**
 * The horizontal alignments the editor offers a button for, in display order.
 *
 * This is an explicit list, not [HorizontalAlignment.entries]. The engine offers more values than
 * this sheet can draw, so a mirror would add a button with no icon and no label.
 * [HorizontalAlignment.Justify] needs an icon and a label before the sheet can offer it.
 */
val editorHorizontalAlignments: List<HorizontalAlignment> = listOf(
    HorizontalAlignment.Left,
    HorizontalAlignment.Center,
    HorizontalAlignment.Right,
    HorizontalAlignment.Auto,
)

@Composable
fun AlignmentButton(
    alignment: HorizontalAlignment,
    currentAlignment: HorizontalAlignment,
    effectiveAlignment: HorizontalAlignment? = null,
    changeAlignment: (HorizontalAlignment) -> Unit,
) {
    ToggleIconButton(
        checked = currentAlignment == alignment,
        onCheckedChange = {
            changeAlignment(alignment)
        },
    ) {
        Icon(
            imageVector = when (alignment) {
                HorizontalAlignment.Left -> IconPack.Formatalignleft
                HorizontalAlignment.Center -> IconPack.Formataligncenter
                HorizontalAlignment.Right -> IconPack.Formatalignright
                HorizontalAlignment.Auto -> when {
                    // Only use effectiveAlignment when stored alignment is Auto, because that's
                    // when getTextEffectiveHorizontalAlignment resolves based on actual text direction.
                    currentAlignment == HorizontalAlignment.Auto &&
                        effectiveAlignment == HorizontalAlignment.Right -> IconPack.FormatAlignRightAuto
                    else -> IconPack.FormatAlignLeftAuto
                }
                // Placeholder. The sheet does not offer this value.
                // This stays an explicit branch so that the next engine value breaks the build.
                HorizontalAlignment.Justify -> IconPack.Formatalignleft
            },
            contentDescription = when (alignment) {
                HorizontalAlignment.Left -> stringResource(R.string.ly_img_editor_sheet_format_text_alignment_horizontal_option_left)
                HorizontalAlignment.Center -> stringResource(R.string.ly_img_editor_sheet_format_text_alignment_horizontal_option_center)
                HorizontalAlignment.Right -> stringResource(R.string.ly_img_editor_sheet_format_text_alignment_horizontal_option_right)
                HorizontalAlignment.Auto -> stringResource(R.string.ly_img_editor_sheet_format_text_alignment_horizontal_option_auto)
                // Placeholder, as above. It needs its own label first.
                HorizontalAlignment.Justify -> stringResource(R.string.ly_img_editor_sheet_format_text_alignment_horizontal_option_left)
            },
        )
    }
}

@Composable
fun AlignmentButton(
    alignment: VerticalAlignment,
    currentAlignment: VerticalAlignment,
    changeAlignment: (VerticalAlignment) -> Unit,
) {
    ToggleIconButton(
        checked = currentAlignment == alignment,
        onCheckedChange = {
            changeAlignment(alignment)
        },
    ) {
        Icon(
            imageVector = when (alignment) {
                VerticalAlignment.Bottom -> IconPack.Verticalalignbottom
                VerticalAlignment.Center -> IconPack.Verticalaligncenter
                VerticalAlignment.Top -> IconPack.Verticalaligntop
            },
            contentDescription = when (alignment) {
                VerticalAlignment.Bottom -> stringResource(R.string.ly_img_editor_sheet_format_text_alignment_vertical_option_bottom)
                VerticalAlignment.Center -> stringResource(R.string.ly_img_editor_sheet_format_text_alignment_vertical_option_center)
                VerticalAlignment.Top -> stringResource(R.string.ly_img_editor_sheet_format_text_alignment_vertical_option_top)
            },
        )
    }
}
