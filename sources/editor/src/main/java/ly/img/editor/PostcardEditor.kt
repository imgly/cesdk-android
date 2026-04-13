package ly.img.editor

import androidx.compose.runtime.Composable

@Deprecated(
    message =
        "PostcardEditor solution has become a starter kit. Check this migration guide for details: " +
            "https://img.ly/docs/cesdk/android/to-v1-73-ab14fb/",
    level = DeprecationLevel.ERROR,
)
@Composable
fun PostcardEditor(onClose: (Throwable?) -> Unit) {
    Editor(onClose = onClose)
}
