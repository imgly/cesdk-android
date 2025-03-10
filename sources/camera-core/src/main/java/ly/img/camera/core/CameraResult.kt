package ly.img.camera.core

import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ParcelCompat

/**
 * Wraps the result of the camera.
 */
sealed interface CameraResult : Parcelable {
    /**
     * Result representing the recordings done by the user using the standard camera mode.
     *
     * @param recordings Recordings done by the user.
     */
    data class Record(
        val recordings: List<Recording>,
    ) : CameraResult {
        override fun writeToParcel(
            dest: Parcel,
            flags: Int,
        ) {
            dest.writeInt(0)
            dest.writeTypedList(recordings)
        }
    }

    /**
     * Result representing the recordings done by the user using the reaction camera mode.
     *
     * @param video The video that was reacted to.
     * @param reaction Recordings of the user's reaction to the video.
     */
    data class Reaction(
        val video: Video,
        val reaction: List<Recording>,
    ) : CameraResult {
        override fun writeToParcel(
            dest: Parcel,
            flags: Int,
        ) {
            dest.writeInt(1)
            dest.writeParcelable(video, flags)
            dest.writeTypedList(reaction)
        }
    }

    abstract override fun writeToParcel(
        dest: Parcel,
        flags: Int,
    )

    override fun describeContents() = 0

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<CameraResult> {
            override fun createFromParcel(parcel: Parcel): CameraResult = when (parcel.readInt()) {
                0 ->
                    Record(
                        recordings = parcel.createTypedArrayList(Recording)!!,
                    )
                1 ->
                    Reaction(
                        video = ParcelCompat.readParcelable(parcel, Video::class.java.classLoader, Video::class.java)!!,
                        reaction = parcel.createTypedArrayList(Recording)!!,
                    )
                else -> throw IllegalArgumentException("Invalid CameraResult type")
            }

            override fun newArray(size: Int): Array<CameraResult?> = arrayOfNulls(size)
        }
    }
}
