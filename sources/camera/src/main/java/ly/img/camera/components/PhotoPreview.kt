package ly.img.camera.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

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
        // Explicit key so Coil's FileKeyer doesn't stat the file (`lastModified`) on the main
        // thread mid-measure (StrictMode DiskReadViolation). Each capture writes a unique
        // path, so the path alone is a sound cache key.
        model = ImageRequest.Builder(LocalContext.current)
            .data(uri)
            .memoryCacheKey(uri.toString())
            .build(),
        contentDescription = null,
        contentScale = ContentScale.Crop,
        modifier = modifier.background(Color.Black),
    )
}
