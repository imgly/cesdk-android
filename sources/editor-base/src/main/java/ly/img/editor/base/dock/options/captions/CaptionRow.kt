package ly.img.editor.base.dock.options.captions

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import ly.img.editor.core.R
import ly.img.editor.core.iconpack.Delete
import ly.img.editor.core.iconpack.IconPack
import ly.img.editor.core.iconpack.Merge
import ly.img.editor.core.iconpack.Plus
import ly.img.editor.core.theme.surface1
import ly.img.editor.core.theme.surface3
import kotlin.math.roundToInt

/**
 * One caption in the list: a card holding an always-present text field, with Merge, Add after and Delete
 * revealed by swiping it aside.
 *
 * The field is never swapped in on tap, so the keyboard survives a merge, a delete or an arrow step. Swiping
 * is withdrawn while editing: these actions mutate blocks another row may hold uncommitted text for.
 */
@Composable
internal fun CaptionRow(
    value: TextFieldValue,
    /** Bumped whenever [value] is rewritten by the sheet rather than by the field. */
    externalRevision: Int,
    onValueChange: (TextFieldValue) -> Unit,
    onOperation: (CaptionKeyOperation, String) -> Boolean,
    onFocusChanged: (Boolean) -> Unit,
    focusRequester: FocusRequester,
    isSelected: Boolean,
    hasPreviousCaption: Boolean,
    /** Whether swiping is offered at all — withdrawn while any caption is being edited. */
    areActionsAvailable: Boolean,
    /** Whether a screen reader may act on *this* row — withdrawn only while this row itself is edited. */
    areAccessibilityActionsAvailable: Boolean,
    /** Whether merging this row is offered to a screen reader; a row editing elsewhere holds uncommitted text. */
    isAccessibilityMergeAvailable: Boolean,
    /** Whether this row is the one currently swiped open. Only one row is ever open. */
    isRevealed: Boolean,
    onRevealChange: (Boolean) -> Unit,
    onDelete: () -> Unit,
    onAddAfter: () -> Unit,
    onMerge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(captionRowCornerRadius)
    val actionCount = if (hasPreviousCaption) 3 else 2
    val density = LocalDensity.current
    val revealPx = with(density) { (captionRowActionWidth * actionCount).toPx() }
    val flingVelocityPx = with(density) { captionRowFlingVelocity.toPx() }
    val offset = remember { Animatable(0f) }
    val coroutineScope = rememberCoroutineScope()

    // The sheet drives the reveal, so opening one row closes any other; the drag below only reports its intent.
    LaunchedEffect(isRevealed, revealPx) {
        offset.animateTo(if (isRevealed) -revealPx else 0f)
    }

    // A swipe is invisible to a screen reader, so the same operations are published as accessibility actions.
    // They stay reachable while another row is edited: each acts on its own caption, not on the focused one.
    val mergeLabel = stringResource(R.string.ly_img_editor_sheet_captions_row_merge)
    val addAfterLabel = stringResource(R.string.ly_img_editor_sheet_captions_row_add_after)
    val deleteLabel = stringResource(R.string.ly_img_editor_sheet_captions_row_delete)
    val accessibilityActions = if (!areAccessibilityActionsAvailable) {
        emptyList()
    } else {
        buildList {
            if (hasPreviousCaption && isAccessibilityMergeAvailable) {
                add(
                    CustomAccessibilityAction(mergeLabel) {
                        onMerge()
                        true
                    },
                )
            }
            add(
                CustomAccessibilityAction(addAfterLabel) {
                    onAddAfter()
                    true
                },
            )
            add(
                CustomAccessibilityAction(deleteLabel) {
                    onDelete()
                    true
                },
            )
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .semantics { customActions = accessibilityActions },
    ) {
        CaptionRowActions(
            hasPreviousCaption = hasPreviousCaption,
            isEnabled = areActionsAvailable,
            onDelete = onDelete,
            onAddAfter = onAddAfter,
            onMerge = onMerge,
            modifier = Modifier
                .matchParentSize()
                .clip(shape),
        )
        Row(
            modifier = Modifier
                .offset { IntOffset(x = offset.value.roundToInt(), y = 0) }
                .fillMaxWidth()
                .heightIn(min = captionRowMinHeight)
                .clip(shape)
                .background(MaterialTheme.colorScheme.surface1, shape)
                .then(
                    if (isSelected) {
                        Modifier.border(
                            width = captionRowSelectionRingWidth,
                            color = MaterialTheme.colorScheme.primary,
                            shape = shape,
                        )
                    } else {
                        Modifier
                    },
                )
                .draggable(
                    orientation = Orientation.Horizontal,
                    enabled = areActionsAvailable,
                    state = rememberDraggableState { delta ->
                        coroutineScope.launch {
                            offset.snapTo((offset.value + delta).coerceIn(-revealPx, 0f))
                        }
                    },
                    onDragStopped = { velocity ->
                        // Settle here rather than reacting to [isRevealed]: a sub-threshold drag leaves that state unchanged.
                        val shouldReveal = when {
                            velocity <= -flingVelocityPx -> true
                            velocity >= flingVelocityPx -> false
                            else -> offset.value < -revealPx / 2
                        }
                        offset.animateTo(if (shouldReveal) -revealPx else 0f)
                        onRevealChange(shouldReveal)
                    },
                )
                // Handles taps landing outside the field — its own padding, and the whole card once the tap-catcher is up.
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    if (isRevealed) {
                        onRevealChange(false)
                    } else {
                        runCatching { focusRequester.requestFocus() }
                    }
                }
                .padding(horizontal = 16.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(modifier = Modifier.fillMaxWidth()) {
                CaptionTextField(
                    value = value,
                    onValueChange = onValueChange,
                    onOperation = onOperation,
                    hasPreviousCaption = hasPreviousCaption,
                    externalRevision = externalRevision,
                    placeholder = stringResource(R.string.ly_img_editor_sheet_captions_row_placeholder),
                    focusRequester = focusRequester,
                    onFocusChanged = onFocusChanged,
                    modifier = Modifier.fillMaxWidth(),
                )
                if (isRevealed) {
                    // The field spans the row, so without this it takes the tap and starts editing instead of closing.
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                            ) { onRevealChange(false) },
                    )
                }
            }
        }
    }
}

/**
 * The actions behind a caption card: Merge · Add after · Delete, destructive one last at the swiped edge.
 * Split is absent because it cuts at the caret, which only exists while editing — it lives in the action bar.
 */
@Composable
private fun CaptionRowActions(
    hasPreviousCaption: Boolean,
    isEnabled: Boolean,
    onDelete: () -> Unit,
    onAddAfter: () -> Unit,
    onMerge: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.background(MaterialTheme.colorScheme.surface3),
        horizontalArrangement = Arrangement.End,
    ) {
        if (hasPreviousCaption) {
            CaptionRowAction(
                icon = IconPack.Merge,
                contentDescription = stringResource(R.string.ly_img_editor_sheet_captions_row_merge),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                isEnabled = isEnabled,
                onClick = onMerge,
            )
        }
        CaptionRowAction(
            icon = IconPack.Plus,
            contentDescription = stringResource(R.string.ly_img_editor_sheet_captions_row_add_after),
            tint = MaterialTheme.colorScheme.primary,
            isEnabled = isEnabled,
            onClick = onAddAfter,
        )
        CaptionRowAction(
            icon = IconPack.Delete,
            contentDescription = stringResource(R.string.ly_img_editor_sheet_captions_row_delete),
            tint = MaterialTheme.colorScheme.error,
            isEnabled = isEnabled,
            onClick = onDelete,
        )
    }
}

/** One fixed-width icon action in the strip behind a caption row. */
@Composable
private fun CaptionRowAction(
    icon: ImageVector,
    contentDescription: String,
    tint: Color,
    isEnabled: Boolean,
    onClick: () -> Unit,
) {
    Box(
        modifier = Modifier
            .width(captionRowActionWidth)
            .fillMaxHeight()
            .clickable(enabled = isEnabled, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Icon(imageVector = icon, contentDescription = contentDescription, tint = tint)
    }
}
