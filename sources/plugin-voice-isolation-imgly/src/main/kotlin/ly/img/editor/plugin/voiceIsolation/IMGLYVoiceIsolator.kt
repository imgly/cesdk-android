package ly.img.editor.plugin.voiceIsolation

import android.content.Context
import android.os.SystemClock
import android.util.Log
import androidx.core.net.toUri
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import ly.img.editor.plugin.voiceIsolation.deepFilterNet3.DeepFilterNet3
import ly.img.editor.plugin.voiceIsolation.deepFilterNet3.Pcm16WavReader
import ly.img.editor.plugin.voiceIsolation.deepFilterNet3.Pcm16WavWriter
import ly.img.editor.plugin.voiceIsolation.deepFilterNet3.enhanceWav
import ly.img.editor.plugin.voiceIsolation.util.FileLoader
import java.io.File
import java.util.UUID

/**
 * IMG.LY's voice isolator. It runs the DeepFilterNet3 model on the device with ONNX Runtime, on each channel of the
 * input.
 *
 * Call [isolateVoice] from your own [VoiceIsolator] to chain your model with this one.
 *
 * @param config IMG.LY voice isolation configuration.
 */
open class IMGLYVoiceIsolator(
    private val config: IMGLYVoiceIsolationConfig,
) : VoiceIsolator {
    /**
     * Downloads the model when [IMGLYVoiceIsolationConfig.loadMode] is [IMGLYVoiceIsolationConfig.LoadMode.EAGER].
     */
    override suspend fun prewarm(context: Context) {
        if (config.loadMode == IMGLYVoiceIsolationConfig.LoadMode.EAGER) awaitModelFolder(context)
    }

    /**
     * Downloads the model on first use, then writes the cleaned audio of [input] to [output].
     *
     * @throws VoiceIsolationException.ModelDownloadFailed when the model cannot be downloaded or loaded.
     */
    override suspend fun isolateVoice(
        context: Context,
        input: File,
        output: File,
    ) {
        val reader = withContext(Dispatchers.IO) { Pcm16WavReader(input) }
        reader.use {
            val modelFolder = awaitModelFolder(context)
            val start = SystemClock.elapsedRealtime()
            withContext(Dispatchers.Default) {
                loadModel(modelFolder).use { model ->
                    require(reader.sampleRate == model.sampleRate) {
                        "The input has ${reader.sampleRate} Hz, the model needs ${model.sampleRate} Hz."
                    }
                    Pcm16WavWriter(output, reader.sampleRate, reader.channels).use { writer ->
                        enhanceWav(input = reader, output = writer, newStream = { model.Stream() })
                    }
                }
            }
            val seconds = reader.frames.toDouble() / reader.sampleRate
            Log.i(
                TAG,
                "Isolated the voice of $seconds s of audio with ${reader.channels} channels in ${SystemClock.elapsedRealtime() - start} ms.",
            )
        }
    }

    /**
     * The folder with the model files, downloaded from [IMGLYVoiceIsolationConfig.modelBaseUri] on first use. Each base
     * URI gets its own folder, so a new model version is downloaded again.
     */
    private suspend fun awaitModelFolder(context: Context): File = modelLock.withLock {
        withContext(Dispatchers.IO) {
            val baseUri = config.modelBaseUri.toString().trimEnd('/')
            val folder = File(context.filesDir, "$MODEL_FOLDER/${UUID.nameUUIDFromBytes(baseUri.toByteArray())}")
            for (name in MODEL_FILES) {
                val file = File(folder, name)
                if (file.exists()) continue
                folder.mkdirs()
                // A temporary file, otherwise an interrupted download leaves a file that looks complete.
                val temporaryFile = File(folder, "$name.temp")
                try {
                    FileLoader.download(
                        context = context,
                        uri = "$baseUri/$name".toUri(),
                        httpClient = config.httpClient,
                        file = temporaryFile,
                    )
                    check(temporaryFile.renameTo(file)) { "Could not move $name into place." }
                } catch (exception: CancellationException) {
                    temporaryFile.delete()
                    throw exception
                } catch (exception: Exception) {
                    temporaryFile.delete()
                    throw VoiceIsolationException.ModelDownloadFailed(exception)
                }
            }
            folder
        }
    }

    /**
     * The model in [folder]. A server can answer with other files and HTTP 200, such as the login page of a hotspot, so
     * a model that does not load is deleted, and the next run downloads it again. The load holds the download's lock,
     * otherwise a failed load can delete the files that another run just downloaded.
     */
    private suspend fun loadModel(folder: File): DeepFilterNet3 = modelLock.withLock {
        try {
            DeepFilterNet3(folder)
        } catch (exception: Exception) {
            folder.deleteRecursively()
            throw VoiceIsolationException.ModelDownloadFailed(exception)
        }
    }

    private companion object {
        const val TAG = "IMGLYVoiceIsolator"

        // Apart from the results in ly.img.plugin.voiceIsolation, which an app may move or delete.
        const val MODEL_FOLDER = "ly.img.editor/model/DeepFilterNet3"
        val MODEL_FILES = listOf("enc.onnx", "erb_dec.onnx", "df_dec.onnx", "config.ini")
        val modelLock = Mutex()
    }
}
