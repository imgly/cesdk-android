package ly.img.editor

import androidx.compose.runtime.Composable

@Deprecated(
    message =
        "ApparelEditor solution is moved to a starter kit package. Check this migration guide for details: " +
            "https://img.ly/docs/cesdk/android/to-v1-73-ab14fb/",
    level = DeprecationLevel.ERROR,
)
@Composable
fun ApparelEditor(onClose: (Throwable?) -> Unit) {
    Editor(onClose = onClose)
}
