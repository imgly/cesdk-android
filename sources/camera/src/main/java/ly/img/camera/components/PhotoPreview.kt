package ly.img.camera.components

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.compose.AsyncImage
import coil.request.ImageRequest

/** Fits the captured photo inside the available preview area, following camera rotation. */
@Composable
internal fun PhotoPreview(
    uri: Uri,
    orientation: Int,
    rotation: () -> Float,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.background(Color.Black)) {
        // Coil applies JPEG orientation metadata. Place that upright image in the rotated UI.
        CameraRotatedLayout(
            modifier = Modifier.align(Alignment.Center),
            sideways = orientation == 90 || orientation == 270,
            rotation = rotation,
            alpha = { 1f },
        ) {
            AsyncImage(
                // Explicit key avoids statting the file on the main thread. Capture paths are unique.
                model = ImageRequest.Builder(LocalContext.current)
                    .data(uri)
                    .memoryCacheKey(uri.toString())
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
