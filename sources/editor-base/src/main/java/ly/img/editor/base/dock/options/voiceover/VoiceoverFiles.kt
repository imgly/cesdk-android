package ly.img.editor.base.dock.options.voiceover

import android.net.Uri
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import java.io.File

private const val OWNED_VOICEOVER_FILE_PREFIX = "voiceover-"
private const val OWNED_VOICEOVER_FILE_EXTENSION = ".wav"

internal object VoiceoverFiles {
    fun createOwnedVoiceOverFile(
        filesDir: File,
        designBlock: DesignBlock,
    ): File = File(
        filesDir,
        "$OWNED_VOICEOVER_FILE_PREFIX${System.currentTimeMillis()}-$designBlock$OWNED_VOICEOVER_FILE_EXTENSION",
    )

    fun deleteFileAsync(
        coroutineScope: CoroutineScope,
        file: File?,
    ) {
        file ?: return
        coroutineScope.launch(Dispatchers.IO) {
            file.delete()
        }
    }

    fun destroyBufferQuietly(
        engine: Engine,
        bufferUri: Uri,
    ) {
        runCatching { engine.editor.destroyBuffer(bufferUri) }
    }
}
