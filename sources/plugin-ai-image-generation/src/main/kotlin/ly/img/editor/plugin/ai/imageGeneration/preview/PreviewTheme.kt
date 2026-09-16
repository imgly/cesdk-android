package ly.img.editor.plugin.ai.imageGeneration.preview

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import ly.img.editor.core.theme.LocalIsDarkTheme

@Composable
internal fun PreviewTheme(content: @Composable () -> Unit) {
    val colorScheme = if (isSystemInDarkTheme()) darkColorScheme() else lightColorScheme()
    CompositionLocalProvider(
        LocalIsDarkTheme provides isSystemInDarkTheme(),
    ) {
        MaterialTheme(
            colorScheme = colorScheme,
            content = content,
        )
    }
}
