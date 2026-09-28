package ly.img.editor.base.timeline.view

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import ly.img.editor.base.timeline.state.TimelineConfiguration

@Composable
fun TimelineButton(
    text: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    icon: (@Composable () -> Unit)? = null,
    containerColor: Color = Color.Transparent,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    contentPadding: PaddingValues = ButtonDefaults.TextButtonWithIconContentPadding,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    OutlinedButton(
        modifier = modifier.height(TimelineConfiguration.clipHeight),
        enabled = enabled,
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = containerColor,
            contentColor = tint,
        ),
        contentPadding = contentPadding,
        border = ButtonDefaults.outlinedButtonBorder.copy(
            brush = SolidColor(MaterialTheme.colorScheme.outlineVariant),
        ),
        shape = MaterialTheme.shapes.small,
        onClick = onClick,
    ) {
        icon?.let {
            it()
            Spacer(modifier = Modifier.width(8.dp))
        }
        // Provided here rather than by each caller, so a configured label is styled like the built-in one.
        CompositionLocalProvider(LocalTextStyle provides MaterialTheme.typography.labelLarge) {
            text()
        }
    }
}
