package ly.img.camera.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage

/**
 * Captured JPEG overlaid on top of `CameraEnginePreview` while previewing. Caller sizes
 * it — typically `fillMaxWidth().aspectRatio(W/H)` to match the engine's video frame.
 */
@Composable
internal fun PhotoPreview(
    uri: Uri,
    modifier: Modifier = Modifier,
) {
    AsyncImage(
        model = uri,
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.background(Color.Black),
    )
}
