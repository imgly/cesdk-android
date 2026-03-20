package ly.img.editor.base.dock.options.voiceover

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

internal object VoiceoverAudioCodec {
    suspend fun writeMonoPcm16FileAsStereoFloatWav(
        inputFile: File,
        outputFile: File,
        sampleRate: Int,
    ) = withContext(Dispatchers.IO) {
        val pcmByteCount = inputFile.length().coerceAtLeast(0L)
        require(pcmByteCount % PCM_BYTES_PER_SAMPLE == 0L) { "Voiceover PCM recording is malformed" }

        val sampleCount = pcmByteCount / PCM_BYTES_PER_SAMPLE
        val dataSize = sampleCount * VOICEOVER_WAV_BLOCK_ALIGN
        require(dataSize <= 0xFFFFFFFFL - 36L) { "Voiceover recording is too large for WAV" }

        FileOutputStream(outputFile).use { output ->
            output.write("RIFF".encodeToByteArray())
            output.writeLittleEndianInt(36L + dataSize)
            output.write("WAVE".encodeToByteArray())
            output.write("fmt ".encodeToByteArray())
            output.writeLittleEndianInt(16L)
            output.writeLittleEndianShort(WAVE_FORMAT_IEEE_FLOAT)
            output.writeLittleEndianShort(VOICEOVER_WAV_CHANNELS)
            output.writeLittleEndianInt(sampleRate.toLong())
            output.writeLittleEndianInt(sampleRate.toLong() * VOICEOVER_WAV_BLOCK_ALIGN)
            output.writeLittleEndianShort(VOICEOVER_WAV_BLOCK_ALIGN)
            output.writeLittleEndianShort(VOICEOVER_WAV_BITS_PER_SAMPLE)
            output.write("data".encodeToByteArray())
            output.writeLittleEndianInt(dataSize)

            inputFile.inputStream().use { input ->
                val pcmBuffer = ByteArray(16 * 1024)
                val wavBuffer = ByteArray((pcmBuffer.size / PCM_BYTES_PER_SAMPLE) * VOICEOVER_WAV_BLOCK_ALIGN)
                while (true) {
                    val bytesRead = input.read(pcmBuffer)
                    if (bytesRead <= 0) break

                    val evenBytesRead = bytesRead - (bytesRead % PCM_BYTES_PER_SAMPLE)
                    var inputIndex = 0
                    var outputIndex = 0
                    while (inputIndex < evenBytesRead) {
                        val sample = (
                            (pcmBuffer[inputIndex].toInt() and 0xFF) or
                                (pcmBuffer[inputIndex + 1].toInt() shl 8)
                        ).toShort()
                        val normalized = (sample / Short.MAX_VALUE.toFloat()).coerceIn(-1f, 1f)
                        val floatBits = normalized.toRawBits()
                        repeat(VOICEOVER_WAV_CHANNELS) {
                            wavBuffer[outputIndex++] = (floatBits and 0xFF).toByte()
                            wavBuffer[outputIndex++] = ((floatBits shr 8) and 0xFF).toByte()
                            wavBuffer[outputIndex++] = ((floatBits shr 16) and 0xFF).toByte()
                            wavBuffer[outputIndex++] = ((floatBits shr 24) and 0xFF).toByte()
                        }
                        inputIndex += PCM_BYTES_PER_SAMPLE
                    }
                    output.write(wavBuffer, 0, outputIndex)
                }
            }
        }
    }

    fun pcmBytesToDurationSeconds(pcmByteCount: Long): Double {
        val sampleCount = pcmByteCount / PCM_BYTES_PER_SAMPLE
        return sampleCount.toDouble() / SAMPLE_RATE.toDouble()
    }

    suspend fun toStereoFloatPcmChunk(pcmChunk: ByteArray): ByteArray = withContext(Dispatchers.Default) {
        if (pcmChunk.size < PCM_BYTES_PER_SAMPLE) return@withContext ByteArray(0)
        val evenBytesRead = pcmChunk.size - (pcmChunk.size % PCM_BYTES_PER_SAMPLE)
        val output = ByteArray((evenBytesRead / PCM_BYTES_PER_SAMPLE) * VOICEOVER_WAV_BLOCK_ALIGN)
        var inputIndex = 0
        var outputIndex = 0
        while (inputIndex < evenBytesRead) {
            val sample = (
                (pcmChunk[inputIndex].toInt() and 0xFF) or
                    (pcmChunk[inputIndex + 1].toInt() shl 8)
            ).toShort()
            val normalized = (sample / Short.MAX_VALUE.toFloat()).coerceIn(-1f, 1f)
            val floatBits = normalized.toRawBits()
            repeat(VOICEOVER_WAV_CHANNELS) {
                output[outputIndex++] = (floatBits and 0xFF).toByte()
                output[outputIndex++] = ((floatBits shr 8) and 0xFF).toByte()
                output[outputIndex++] = ((floatBits shr 16) and 0xFF).toByte()
                output[outputIndex++] = ((floatBits shr 24) and 0xFF).toByte()
            }
            inputIndex += PCM_BYTES_PER_SAMPLE
        }
        output
    }

    private fun FileOutputStream.writeLittleEndianInt(value: Long) {
        write((value and 0xFF).toInt())
        write(((value shr 8) and 0xFF).toInt())
        write(((value shr 16) and 0xFF).toInt())
        write(((value shr 24) and 0xFF).toInt())
    }

    private fun FileOutputStream.writeLittleEndianShort(value: Int) {
        write(value and 0xFF)
        write((value shr 8) and 0xFF)
    }
}
