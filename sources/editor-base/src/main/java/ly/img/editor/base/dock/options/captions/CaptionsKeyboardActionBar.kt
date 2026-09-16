package ly.img.editor.base.dock.options.captions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ly.img.editor.core.R
import ly.img.editor.core.iconpack.Delete
import ly.img.editor.core.iconpack.IconPack
import ly.img.editor.core.iconpack.Merge
import ly.img.editor.core.iconpack.Plus
import ly.img.editor.core.iconpack.Split
import ly.img.editor.core.ui.iconpack.ArrowDown
import ly.img.editor.core.ui.iconpack.ArrowUp
import ly.img.editor.core.ui.iconpack.MoreVert
import ly.img.editor.core.ui.library.components.ClipMenuItem
import ly.img.editor.core.ui.iconpack.IconPack as CoreUiIconPack

/** The bar shown above the keyboard while a caption is being edited. */
@Composable
internal fun CaptionsKeyboardActionBar(
    /** Window y of the bar's own top edge, so its menu can be placed above it. */
    barTopPx: Int,
    isFirst: Boolean,
    isLast: Boolean,
    isEnabled: Boolean,
    onDelete: () -> Unit,
    onMerge: () -> Unit,
    onSplit: () -> Unit,
    onAddAfter: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .fillMaxWidth()
            // A disabled button does not consume its tap, so the bar swallows taps to keep them off the list below.
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {},
            )
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        BarIconButton(
            icon = IconPack.Delete,
            contentDescription = stringResource(R.string.ly_img_editor_sheet_captions_row_delete),
            isEnabled = isEnabled,
            tint = MaterialTheme.colorScheme.error,
            onClick = onDelete,
        )
        StructuralActionsMenu(
            barTopPx = barTopPx,
            isEnabled = isEnabled,
            isFirst = isFirst,
            onMerge = onMerge,
            onSplit = onSplit,
            onAddAfter = onAddAfter,
        )

        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            BarIconButton(
                icon = CoreUiIconPack.ArrowUp,
                contentDescription = stringResource(R.string.ly_img_editor_sheet_captions_button_previous),
                isEnabled = isEnabled && !isFirst,
                onClick = onPrevious,
            )
            BarIconButton(
                icon = CoreUiIconPack.ArrowDown,
                contentDescription = stringResource(R.string.ly_img_editor_sheet_captions_button_next),
                isEnabled = isEnabled && !isLast,
                onClick = onNext,
            )
            TextButton(onClick = onDone, enabled = isEnabled) {
                Text(
                    text = stringResource(R.string.ly_img_editor_sheet_captions_button_done),
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        }
    }
}

/** The structural operations (merge, split, add after) behind an overflow menu. */
@Composable
private fun StructuralActionsMenu(
    barTopPx: Int,
    isEnabled: Boolean,
    isFirst: Boolean,
    onMerge: () -> Unit,
    onSplit: () -> Unit,
    onAddAfter: () -> Unit,
) {
    var isExpanded by remember { mutableStateOf(false) }
    Box {
        BarIconButton(
            icon = CoreUiIconPack.MoreVert,
            contentDescription = stringResource(R.string.ly_img_editor_sheet_captions_button_more),
            isEnabled = isEnabled,
            onClick = { isExpanded = true },
        )
        CaptionsActionMenu(
            expanded = isExpanded,
            menuBottomPx = barTopPx,
            onDismissRequest = { isExpanded = false },
        ) {
            if (!isFirst) {
                ClipMenuItem(R.string.ly_img_editor_sheet_captions_row_merge, IconPack.Merge) {
                    isExpanded = false
                    onMerge()
                }
            }
            ClipMenuItem(R.string.ly_img_editor_sheet_captions_row_split, IconPack.Split) {
                isExpanded = false
                onSplit()
            }
            ClipMenuItem(R.string.ly_img_editor_sheet_captions_row_add_after, IconPack.Plus) {
                isExpanded = false
                onAddAfter()
            }
        }
    }
}

/** One icon button of the action bar, sized and tinted consistently across the row. */
@Composable
private fun BarIconButton(
    icon: ImageVector,
    contentDescription: String,
    isEnabled: Boolean,
    onClick: () -> Unit,
    tint: Color = LocalContentColor.current,
) {
    IconButton(
        onClick = onClick,
        enabled = isEnabled,
        modifier = Modifier.size(captionActionBarButtonSize),
        // Tint via the button's own colours, not a `LocalContentColor` override, so disabled buttons still fade.
        colors = IconButtonDefaults.iconButtonColors(
            contentColor = tint,
            disabledContentColor = tint.copy(alpha = captionActionBarDisabledAlpha),
        ),
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription)
    }
}
