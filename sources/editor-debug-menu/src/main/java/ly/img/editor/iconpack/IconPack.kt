package ly.img.editor.iconpack

import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector

internal object IconPack

@Composable
internal fun ImageVector.IconPreview() = Icon(this, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimary)
