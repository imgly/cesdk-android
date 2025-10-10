package ly.img.editor.core.library.data

import android.net.Uri

object SystemGalleryThumbnailUris {
    private const val SCHEME = "system-gallery-thumb"
    private const val AUTHORITY = "local"
    private const val PARAM_SOURCE = "source"

    fun create(source: Uri): String = Uri.Builder()
        .scheme(SCHEME)
        .authority(AUTHORITY)
        .appendQueryParameter(PARAM_SOURCE, source.toString())
        .build()
        .toString()

    fun resolve(data: Uri): Uri? {
        if (data.scheme != SCHEME || data.authority != AUTHORITY) {
            return null
        }
        val source = data.getQueryParameter(PARAM_SOURCE) ?: return null
        return runCatching { Uri.parse(source) }.getOrNull()
    }
}
