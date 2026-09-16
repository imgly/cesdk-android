package ly.img.camera.core

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ParcelCompat
import kotlin.time.Duration
import kotlin.time.Duration.Companion.nanoseconds

/**
 * A single entry in a heterogeneous capture stack. Returned inside [CameraResult.Captures]
 * for any [CaptureType]: video-only sessions yield [Capture.Video] entries, photo sessions yield
 * [Capture.Photo] entries, and mixed sessions yield a stack of both.
 */
sealed interface Capture : Parcelable {
    /**
     * A still photo capture written to the app's files directory.
     *
     * @param uri the file URI of the captured JPEG image.
     * @param clipDuration the duration this photo occupies on the recording-segments ring and on the
     * editor timeline once converted into an image-fill graphic block. Sourced from
     * [CameraConfiguration.photoClipDuration] at capture time (defaults to 5 seconds).
     */
    data class Photo(
        val uri: Uri,
        val clipDuration: Duration,
    ) : Capture {
        override fun writeToParcel(
            dest: Parcel,
            flags: Int,
        ) {
            dest.writeInt(0)
            dest.writeParcelable(uri, flags)
            dest.writeLong(clipDuration.inWholeNanoseconds)
        }
    }

    /**
     * A video capture, wrapping an existing [Recording].
     *
     * @param recording the recorded video clip.
     */
    data class Video(
        val recording: Recording,
    ) : Capture {
        override fun writeToParcel(
            dest: Parcel,
            flags: Int,
        ) {
            dest.writeInt(1)
            dest.writeParcelable(recording, flags)
        }
    }

    abstract override fun writeToParcel(
        dest: Parcel,
        flags: Int,
    )

    override fun describeContents() = 0

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<Capture> {
            override fun createFromParcel(parcel: Parcel): Capture = when (parcel.readInt()) {
                0 ->
                    Photo(
                        uri = ParcelCompat.readParcelable(parcel, Uri::class.java.classLoader, Uri::class.java)!!,
                        clipDuration = parcel.readLong().nanoseconds,
                    )
                1 ->
                    Video(
                        recording = ParcelCompat.readParcelable(parcel, Recording::class.java.classLoader, Recording::class.java)!!,
                    )
                else -> throw IllegalArgumentException("Invalid Capture type")
            }

            override fun newArray(size: Int): Array<Capture?> = arrayOfNulls(size)
        }
    }
}
