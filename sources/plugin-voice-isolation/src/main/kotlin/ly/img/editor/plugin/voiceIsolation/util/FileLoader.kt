package ly.img.editor.plugin.voiceIsolation.util

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.InputStream

/**
 * Opens the files of the voice isolation plugin: the audio of a clip and the files of a model.
 */
object FileLoader {
    /**
     * Opens [uri] as an [InputStream]. Reads `http` and `https` with [httpClient], `file:///android_asset/` from the
     * app's assets, other `file` URIs from the file system, and any other URI, such as `content`, through the content
     * resolver.
     *
     * @param context the context of the application.
     * @param uri the uri to load.
     * @param httpClient the http client to load remote resources.
     * @return a stream that the caller closes.
     */
    suspend fun loadUri(
        context: Context,
        uri: Uri,
        httpClient: OkHttpClient,
    ): InputStream = withContext(Dispatchers.IO) {
        when (uri.scheme?.lowercase()) {
            "http", "https" -> {
                val response = httpClient.newCall(Request.Builder().url(uri.toString()).get().build()).execute()
                try {
                    require(response.isSuccessful) { "HTTP error: code = ${response.code}, message = ${response.message}" }
                    requireNotNull(response.body?.byteStream()) { "Response body is null." }
                } catch (exception: Exception) {
                    response.close()
                    throw exception
                }
            }
            "file" -> {
                val path = requireNotNull(uri.path) { "The file URI $uri has no path." }
                if (path.startsWith(ANDROID_ASSET_PATH)) {
                    context.assets.open(path.removePrefix(ANDROID_ASSET_PATH))
                } else {
                    File(path).inputStream()
                }
            }
            else -> requireNotNull(context.contentResolver.openInputStream(uri)) { "Content resolver could not resolve $uri." }
        }
    }

    /**
     * Writes the content of [uri] to [file]. Reads [uri] as [loadUri] does, and stops when the coroutine is cancelled.
     *
     * @param context the context of the application.
     * @param uri the uri to load.
     * @param httpClient the http client to load remote resources.
     * @param file the file to write.
     */
    suspend fun download(
        context: Context,
        uri: Uri,
        httpClient: OkHttpClient,
        file: File,
    ) = withContext(Dispatchers.IO) {
        loadUri(context = context, uri = uri, httpClient = httpClient).use { input ->
            file.outputStream().use { output ->
                val buffer = ByteArray(BUFFER_SIZE)
                while (true) {
                    ensureActive()
                    val count = input.read(buffer)
                    if (count < 0) break
                    output.write(buffer, 0, count)
                }
            }
        }
    }

    private const val ANDROID_ASSET_PATH = "/android_asset/"
    private const val BUFFER_SIZE = 64 * 1024
}
