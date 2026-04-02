package ly.img.editor.base.dock.options.voiceover

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

internal class VoiceOverRecordSegmentRecorder {
    private var audioRecord: AudioRecord? = null
    private var writingJob: Job? = null
    private var bytesWritten: Long = 0L
    private val active = AtomicBoolean(false)

    @SuppressLint("MissingPermission")
    fun start(
        scope: CoroutineScope,
        outputFile: File,
        onChunkRecorded: (suspend (ByteArray) -> Unit)? = null,
        onRecorderError: ((Throwable) -> Unit)? = null,
    ): Boolean {
        if (active.get()) return false
        val minBufferSize = AudioRecord.getMinBufferSize(
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
        )
        if (minBufferSize <= 0) return false
        val bufferSize = minBufferSize * 2
        val record = AudioRecord(
            MediaRecorder.AudioSource.MIC,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            bufferSize,
        )
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            record.release()
            return false
        }

        audioRecord = record
        bytesWritten = 0L
        active.set(true)

        val started = runCatching {
            record.startRecording()
            record.recordingState == AudioRecord.RECORDSTATE_RECORDING
        }.getOrDefault(false)
        if (!started) {
            active.set(false)
            audioRecord = null
            record.release()
            return false
        }
        writingJob = scope.launch(Dispatchers.IO) {
            FileOutputStream(outputFile).use { fos ->
                val buffer = ByteArray(bufferSize)
                while (isActive && active.get()) {
                    val read = try {
                        record.read(buffer, 0, buffer.size)
                    } catch (error: Throwable) {
                        active.set(false)
                        onRecorderError?.invoke(error)
                        break
                    }
                    when {
                        read > 0 -> {
                            fos.write(buffer, 0, read)
                            bytesWritten += read
                            onChunkRecorded?.invoke(buffer.copyOf(read))
                        }

                        read < 0 -> {
                            active.set(false)
                            onRecorderError?.invoke(
                                IllegalStateException("AudioRecord read failed with code $read."),
                            )
                            break
                        }
                    }
                }
            }
        }
        return true
    }

    suspend fun stop(): Long {
        if (!active.get()) return 0L
        active.set(false)
        val record = audioRecord
        audioRecord = null
        runCatching { record?.stop() }
        runCatching { record?.release() }
        val job = writingJob
        writingJob = null
        runCatching { job?.cancelAndJoin() }
        val sampleCount = bytesWritten / PCM_BYTES_PER_SAMPLE
        return ((sampleCount * 1000L) / SAMPLE_RATE.toLong()).coerceAtLeast(0L)
    }

    fun abort() {
        if (active.get()) {
            active.set(false)
        }
        runCatching { audioRecord?.stop() }
        runCatching { audioRecord?.release() }
        audioRecord = null
        runCatching { writingJob?.cancel() }
        writingJob = null
        bytesWritten = 0L
    }
}
