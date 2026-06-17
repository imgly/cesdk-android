package ly.img.editor.plugin.backgroundRemoval.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream

object FileLoader {
    /**
     * Loads [uri] into an [InputStream].
     *
     * @param context the context of the application.
     * @param uri the uri to load.
     * @param httpClient the http client to load remote resources.
     */
    suspend fun loadUri(
        context: Context,
        uri: Uri,
        httpClient: OkHttpClient,
    ): InputStream = withContext(Dispatchers.IO) {
        when (uri.scheme) {
            "http", "https" -> {
                val request = Request.Builder()
                    .url(uri.toString())
                    .get()
                    .build()
                val response = httpClient.newCall(request).execute()
                try {
                    require(response.isSuccessful) {
                        "HTTP error: code = ${response.code}, message = ${response.message}"
                    }
                    requireNotNull(response.body?.byteStream()) {
                        "Response body is null."
                    }
                } catch (exception: Exception) {
                    response.close()
                    throw exception
                }
            }
            "file" -> {
                val absolutePath = uri.toString()
                val assetsPrefix = "file:///android_asset/"
                if (absolutePath.startsWith(assetsPrefix)) {
                    context.assets.open(absolutePath.removePrefix(assetsPrefix))
                } else {
                    File(requireNotNull(uri.path)).inputStream()
                }
            }
            else -> {
                requireNotNull(context.contentResolver.openInputStream(uri)) {
                    "Content resolver could not resolve $uri."
                }
            }
        }
    }
}
