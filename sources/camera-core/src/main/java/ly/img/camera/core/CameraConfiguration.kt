package ly.img.camera.core

import android.os.Parcel
import android.os.Parcelable
import android.util.SizeF
import androidx.compose.ui.graphics.Color
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/**
 * Configuration for the camera.
 *
 * @param recordingColor The color of the record button while recording, and all the other recording indicators.
 * @param maxTotalDuration The target duration for the recording.
 * In [CameraMode.Reaction], this is ignored and the total duration is limited by the duration of the video being reacted to.
 * @param allowExceedingMaxDuration Adjusts the segments visualization to use the max duration, but does not enforce the limit.
 * In [CameraMode.Reaction], this is ignored and always behaves as if set to false.
 * @param captureType What kind of media the camera produces. Defaults to [CaptureType.Video].
 * Not all combinations with [CameraMode] are supported — see [CameraMode.supports].
 * @param captureCount Whether the camera produces a single capture or a stack of captures. Defaults to [CaptureCount.Multi].
 * @param photoClipDuration The duration each photo capture occupies on the recording-segments ring and on the
 * editor timeline (when the photo is converted into an image-fill graphic block). Defaults to 5 seconds.
 * @param showsPhotoPreview Whether to show the full-screen preview screen after each photo capture. When `false`,
 * captured photos are committed directly to the capture stack without a confirm/discard step — useful for
 * rapid-fire multi-take photo sessions where the preview interrupts the flow. Defaults to `true`.
 */
class CameraConfiguration(
    val recordingColor: Color = Color(0xFFDE6F62),
    val maxTotalDuration: Duration = Duration.INFINITE,
    val allowExceedingMaxDuration: Boolean = false,
    val captureType: CaptureType = CaptureType.Video,
    val captureCount: CaptureCount = CaptureCount.Multi,
    val photoClipDuration: Duration = 5.seconds,
    val showsPhotoPreview: Boolean = true,
) : Parcelable {
    /**
     * Dimensions of the recorded video(s) / camera preview.
     */
    val videoSize = SizeF(1080f, 1920f)

    constructor(parcel: Parcel) : this(
        recordingColor = Color(parcel.readLong()),
        maxTotalDuration = parcel.readLong().milliseconds,
        allowExceedingMaxDuration = parcel.readByte() != 0.toByte(),
        captureType = CaptureType.valueOf(parcel.readString()!!),
        captureCount = CaptureCount.valueOf(parcel.readString()!!),
        photoClipDuration = parcel.readLong().milliseconds,
        showsPhotoPreview = parcel.readByte() != 0.toByte(),
    )

    override fun writeToParcel(
        parcel: Parcel,
        flags: Int,
    ) {
        // Look at Color(color: Long) implementation to understand why we need to shift the color
        parcel.writeLong((recordingColor.value shr 32).toLong())
        parcel.writeLong(maxTotalDuration.inWholeMilliseconds)
        parcel.writeByte(if (allowExceedingMaxDuration) 1 else 0)
        parcel.writeString(captureType.name)
        parcel.writeString(captureCount.name)
        parcel.writeLong(photoClipDuration.inWholeMilliseconds)
        parcel.writeByte(if (showsPhotoPreview) 1 else 0)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<CameraConfiguration> {
        override fun createFromParcel(parcel: Parcel): CameraConfiguration = CameraConfiguration(parcel)

        override fun newArray(size: Int): Array<CameraConfiguration?> = arrayOfNulls(size)
    }
}
