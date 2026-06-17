package ly.img.camera.core

import android.os.Parcel
import android.os.Parcelable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

/**
 * A camera recording.
 *
 * @param videos the list of [Video]s in the recording.
 * @param duration the duration of the recording.
 */
data class Recording(
    val videos: List<Video>,
    val duration: Duration,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        videos = parcel.createTypedArrayList(Video)!!,
        duration = parcel.readLong().milliseconds,
    )

    override fun writeToParcel(
        parcel: Parcel,
        flags: Int,
    ) {
        parcel.writeTypedList(videos)
        parcel.writeLong(duration.inWholeMilliseconds)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Recording> {
        override fun createFromParcel(parcel: Parcel): Recording = Recording(parcel)

        override fun newArray(size: Int): Array<Recording?> = arrayOfNulls(size)
    }
}
