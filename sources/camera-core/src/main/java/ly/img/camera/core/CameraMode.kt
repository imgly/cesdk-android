package ly.img.camera.core

import android.net.Uri
import android.os.Parcel
import android.os.Parcelable

/**
 * Represents the different modes the camera can operate in.
 */
sealed interface CameraMode : Parcelable {
    /**
     * The standard main camera.
     */
    class Standard : CameraMode {
        override fun writeToParcel(
            dest: Parcel,
            flags: Int,
        ) {
            dest.writeInt(0)
        }
    }

    /**
     * Records with one camera while playing the [video].
     *
     * @param video Uri of the video to react to.
     * @param cameraLayoutMode The layout mode. By default, the video and the camera are placed vertically.
     * @param positionsSwapped A boolean indicating if the video and camera feed should swap positions.
     * By default, it is false and the video being reacted to is on the top/left (depending on `cameraLayoutMode`)
     */
    class Reaction(
        val video: Uri,
        val cameraLayoutMode: CameraLayoutMode = CameraLayoutMode.Vertical,
        val positionsSwapped: Boolean = false,
    ) : CameraMode {
        override fun writeToParcel(
            dest: Parcel,
            flags: Int,
        ) {
            dest.writeInt(1)
            dest.writeString(video.toString())
            dest.writeString(cameraLayoutMode.toString())
            dest.writeInt(if (positionsSwapped) 1 else 0)
        }
    }

    abstract override fun writeToParcel(
        dest: Parcel,
        flags: Int,
    )

    override fun describeContents() = 0

    companion object {
        @JvmField
        val CREATOR = object : Parcelable.Creator<CameraMode> {
            override fun createFromParcel(parcel: Parcel): CameraMode = when (parcel.readInt()) {
                0 -> Standard()
                1 ->
                    Reaction(
                        video = Uri.parse(parcel.readString()),
                        cameraLayoutMode = CameraLayoutMode.valueOf(parcel.readString()!!),
                        positionsSwapped = parcel.readInt() == 1,
                    )
                else -> throw IllegalArgumentException("Invalid CameraMode type")
            }

            override fun newArray(size: Int): Array<CameraMode?> = arrayOfNulls(size)
        }
    }
}
