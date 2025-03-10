package ly.img.camera.core

import android.graphics.RectF
import android.net.Uri
import android.os.Parcel
import android.os.Parcelable
import androidx.core.os.ParcelCompat

/**
 * A video in a [Recording].
 *
 * @param uri the uri of the video
 * @param rect the position and size of the video
 */
data class Video(
    val uri: Uri,
    val rect: RectF,
) : Parcelable {
    constructor(parcel: Parcel) : this(
        uri = ParcelCompat.readParcelable(parcel, Uri::class.java.classLoader, Uri::class.java)!!,
        rect = ParcelCompat.readParcelable(parcel, RectF::class.java.classLoader, RectF::class.java)!!,
    )

    override fun writeToParcel(
        parcel: Parcel,
        flags: Int,
    ) {
        parcel.writeParcelable(uri, flags)
        parcel.writeParcelable(rect, flags)
    }

    override fun describeContents(): Int = 0

    companion object CREATOR : Parcelable.Creator<Video> {
        override fun createFromParcel(parcel: Parcel): Video = Video(parcel)

        override fun newArray(size: Int): Array<Video?> = arrayOfNulls(size)
    }
}
