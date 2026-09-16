package ly.img.camera.core

import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ParcelCompat

/**
 * Wraps the result of the camera.
 */
sealed interface CameraResult : Parcelable {
    /**
     * Recordings are now wrapped as [Capture.Video] inside [Captures]. `when` consumers must drop
     * the `is CameraResult.Record ->` arm and add an `is CameraResult.Captures ->` arm. Use the
     * [videos] extension on `List<Capture>` to extract `List<Recording>` from a heterogeneous
     * capture stack.
     */
    @Deprecated(
        level = DeprecationLevel.ERROR,
        message = "Recordings are wrapped as Capture.Video. Use Captures and the .videos extension.",
        replaceWith = ReplaceWith("Captures"),
    )
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

    /**
     * Result representing a heterogeneous stack of captures (photos and/or videos) produced by the
     * camera when configured with [CaptureType.Photo] or [CaptureType.Mixed].
     *
     * Named [Captures] (plural) rather than `Capture` so consumers can import [Capture]
     * (the sealed interface for individual entries) and the result case side-by-side without
     * naming collisions.
     *
     * @param captures the ordered list of [Capture]s in the order they were taken.
     */
    data class Captures(
        val captures: List<Capture>,
    ) : CameraResult {
        override fun writeToParcel(
            dest: Parcel,
            flags: Int,
        ) {
            dest.writeInt(2)
            dest.writeTypedList(captures)
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
                1 ->
                    Reaction(
                        video = ParcelCompat.readParcelable(parcel, Video::class.java.classLoader, Video::class.java)!!,
                        reaction = parcel.createTypedArrayList(Recording)!!,
                    )
                2 ->
                    Captures(
                        captures = parcel.createTypedArrayList(Capture.CREATOR)!!,
                    )
                else -> throw IllegalArgumentException("Invalid CameraResult type")
            }

            override fun newArray(size: Int): Array<CameraResult?> = arrayOfNulls(size)
        }
    }
}

/**
 * Extracts video recordings from a heterogeneous capture stack. Migration shortcut for hosts
 * that previously consumed [CameraResult.Record]'s `recordings: List<Recording>`.
 */
val List<Capture>.videos: List<Recording>
    get() = mapNotNull { (it as? Capture.Video)?.recording }
