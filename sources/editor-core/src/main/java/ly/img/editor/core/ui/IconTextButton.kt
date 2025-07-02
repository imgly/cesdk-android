package ly.img.editor.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ly.img.editor.core.component.data.EditorIcon

/**
 * A composable function that renders a button with [icon] and [text], positioned vertically.
 *
 * @param onClick the callback that is invoked when the button is clicked.
 * @param modifier the [Modifier] to be applied to this button.
 * @param icon the icon that should be rendered.
 * @param text the text that should be rendered.
 * @param enabled whether the button is enabled.
 * @param contentPadding the content padding of the button.
 * @param tint the tint color of the button.
 */
@Composable
fun IconTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    text: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp, horizontal = 4.dp),
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    if (text == null) {
        IconButton(
            onClick = onClick,
            enabled = enabled,
            colors = IconButtonDefaults.iconButtonColors(
                contentColor = tint,
                disabledContentColor = tint.copy(alpha = tint.alpha * 0.38F),
            ),
            modifier = modifier.padding(contentPadding),
        ) {
            icon?.invoke()
        }
    } else {
        Button(
            modifier = modifier,
            colors = ButtonDefaults.textButtonColors(
                contentColor = tint,
                disabledContentColor = tint.copy(alpha = tint.alpha * 0.5F),
            ),
            onClick = onClick,
            enabled = enabled,
            contentPadding = contentPadding,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
                modifier = Modifier.padding(horizontal = 4.dp),
            ) {
                icon?.invoke()
                text()
            }
        }
    }
}

/**
 * A composable function that renders a button with [editorIcon] and [text], positioned vertically.
 *
 * @param onClick the callback that is invoked when the button is clicked.
 * @param modifier the [Modifier] to be applied to this button.
 * @param editorIcon the icon content of the button as an [EditorIcon]. If null then icon is not rendered.
 * @param text the text content of the button as a string. If null then text is not rendered. If null then text is not rendered.
 * @param enabled whether the button is enabled.
 * @param contentPadding the content padding of the button.
 * @param tint the tint color of the button.
 */
@Composable
fun IconTextButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    editorIcon: EditorIcon? = null,
    text: String? = null,
    enabled: Boolean = true,
    contentPadding: PaddingValues = PaddingValues(vertical = 10.dp, horizontal = 4.dp),
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    IconTextButton(
        modifier = modifier,
        icon = editorIcon?.let {
            { EditorIcon(it) }
        },
        text = text?.let {
            {
                Text(
                    text = text,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        },
        enabled = enabled,
        onClick = onClick,
        contentPadding = contentPadding,
        tint = tint,
    )
}
