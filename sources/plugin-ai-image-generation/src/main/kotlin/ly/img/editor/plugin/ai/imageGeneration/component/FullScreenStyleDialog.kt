package ly.img.editor.plugin.ai.imageGeneration.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import ly.img.editor.plugin.ai.core.gateway.AIGatewayPromptStyle
import ly.img.editor.plugin.ai.imageGeneration.preview.PreviewTheme

@Composable
internal fun FullScreenStyleDialog(
    selectedStyle: AIGatewayPromptStyle,
    onStyleSelected: (AIGatewayPromptStyle) -> Unit,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
        ),
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            StyleSelectionView(
                selectedStyle = selectedStyle,
                onStyleSelected = onStyleSelected,
                onBack = onDismiss,
                onCloseSheet = onDismiss,
            )
        }
    }
}

@Composable
@Preview(name = "Light")
@Preview(name = "Dark", uiMode = Configuration.UI_MODE_NIGHT_YES)
internal fun FullScreenStyleDialogPreview() {
    PreviewTheme {
        Surface {
            FullScreenStyleDialog(
                selectedStyle = AIGatewayPromptStyle.curated.first(),
                onStyleSelected = {},
                onDismiss = {},
            )
        }
    }
}
