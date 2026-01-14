package ly.img.editor.core.ui.utils

import android.content.Context
import android.graphics.Typeface
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.AndroidFont
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontLoadingStrategy
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ly.img.editor.core.ui.Environment
import ly.img.editor.core.ui.library.data.font.FontData
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.ConcurrentHashMap

val FontData.fontFamily: FontFamily
    @Composable
    get() {
        val font = uri.toAssetPathOrNull()?.let {
            Font(
                path = it,
                assetManager = LocalContext.current.assets,
                weight = weight,
            )
        } ?: uri.toFileOrNull()?.let {
            Font(
                file = it,
                weight = weight,
            )
        } ?: RemoteFont(uri = uri, weight = weight)
        return FontFamily(font)
    }

class RemoteFont(
    val uri: Uri,
    override val weight: FontWeight,
) : AndroidFont(
        loadingStrategy = FontLoadingStrategy.Async,
        typefaceLoader = RemoteTypefaceLoader(),
        variationSettings = FontVariation.Settings(),
    ) {
    override val style: FontStyle = FontStyle.Normal
}

class RemoteTypefaceLoader : AndroidFont.TypefaceLoader {
    override suspend fun awaitLoad(
        context: Context,
        font: AndroidFont,
    ): Typeface? {
        if (font !is RemoteFont) return null

        // Check cache first
        val cacheKey = font.uri.toString()
        typefaceCache[cacheKey]?.let { return it }

        return withContext(Dispatchers.IO) {
            // Double-check cache after switching to IO thread
            typefaceCache[cacheKey]?.let { return@withContext it }

            val fontFile = font.fontFile ?: return@withContext null
            val typeface = if (fontFile.exists()) {
                Typeface.createFromFile(fontFile)
            } else {
                fontFile.parentFile?.mkdirs()
                val url = URL(font.uri.toString())
                val connection = url.openConnection() as HttpURLConnection
                val responseCode = connection.responseCode
                require(responseCode == HttpURLConnection.HTTP_OK)
                connection.inputStream.use { cis ->
                    val tempFile = File(fontFile.absolutePath + ".temp")
                    tempFile.outputStream().use { fos ->
                        cis.copyTo(fos)
                    }
                    tempFile.renameTo(fontFile)
                }
                Typeface.createFromFile(fontFile)
            }

            typeface?.also { typefaceCache[cacheKey] = it }
        }
    }

    override fun loadBlocking(
        context: Context,
        font: AndroidFont,
    ): Typeface? {
        if (font !is RemoteFont) return null

        // Check cache first
        val cacheKey = font.uri.toString()
        typefaceCache[cacheKey]?.let { return it }

        return font.fontFile?.let { fontFile ->
            if (fontFile.exists()) {
                Typeface.createFromFile(fontFile)?.also { typefaceCache[cacheKey] = it }
            } else {
                null
            }
        }
    }

    private val AndroidFont.fontFile: File?
        get() {
            if (this !is RemoteFont) return null
            if (uri.scheme != "http" && uri.scheme != "https") {
                return null
            }
            return File(Environment.getEditorCacheDir(), requireNotNull(uri.path))
        }

    companion object {
        private val typefaceCache = ConcurrentHashMap<String, Typeface>()
    }
}
