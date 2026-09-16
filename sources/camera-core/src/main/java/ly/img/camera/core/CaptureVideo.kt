package ly.img.camera.core

import android.os.Parcel
import androidx.core.os.ParcelCompat

/**
 * Legacy entry point for the IMG.LY Camera. Renamed to [CaptureMedia], which supports photo,
 * video, and mixed capture sessions and delivers the modern [CameraResult.Captures] result.
 *
 * The symbol is preserved so existing call sites surface a precise compile error pointing at
 * [CaptureMedia] instead of an opaque "unresolved reference"; the class itself is unusable at
 * compile time.
 */
@Deprecated(
    message = "Renamed to CaptureMedia, which supports photo / video / mixed capture sessions and " +
        "returns the modern CameraResult.Captures shape.",
    replaceWith = ReplaceWith("CaptureMedia", "ly.img.camera.core.CaptureMedia"),
    level = DeprecationLevel.ERROR,
)
open class CaptureVideo : CaptureMedia() {
    companion object {
        @Deprecated(
            message = "Use CaptureMedia.INTENT_KEY_CAMERA_INPUT.",
            replaceWith = ReplaceWith("CaptureMedia.INTENT_KEY_CAMERA_INPUT", "ly.img.camera.core.CaptureMedia"),
            level = DeprecationLevel.ERROR,
        )
        const val INTENT_KEY_CAMERA_INPUT = CaptureMedia.INTENT_KEY_CAMERA_INPUT

        @Deprecated(
            message = "Use CaptureMedia.INTENT_KEY_CAMERA_RESULT.",
            replaceWith = ReplaceWith("CaptureMedia.INTENT_KEY_CAMERA_RESULT", "ly.img.camera.core.CaptureMedia"),
            level = DeprecationLevel.ERROR,
        )
        const val INTENT_KEY_CAMERA_RESULT = CaptureMedia.INTENT_KEY_CAMERA_RESULT
    }

    /**
     * Renamed to [CaptureMedia.Input]. See [CaptureVideo] for migration details.
     */
    @Deprecated(
        message = "Use CaptureMedia.Input.",
        replaceWith = ReplaceWith("CaptureMedia.Input", "ly.img.camera.core.CaptureMedia"),
        level = DeprecationLevel.ERROR,
    )
    class Input(
        engineConfiguration: EngineConfiguration,
        cameraConfiguration: CameraConfiguration = CameraConfiguration(),
        cameraMode: CameraMode = CameraMode.Standard(),
    ) : CaptureMedia.Input(engineConfiguration, cameraConfiguration, cameraMode) {
        @Suppress("DEPRECATION_ERROR")
        companion object CREATOR : android.os.Parcelable.Creator<Input> {
            override fun createFromParcel(parcel: Parcel): Input = Input(
                engineConfiguration = ParcelCompat.readParcelable(
                    parcel,
                    EngineConfiguration::class.java.classLoader,
                    EngineConfiguration::class.java,
                )!!,
                cameraConfiguration = ParcelCompat.readParcelable(
                    parcel,
                    CameraConfiguration::class.java.classLoader,
                    CameraConfiguration::class.java,
                )!!,
                cameraMode = ParcelCompat.readParcelable(
                    parcel,
                    CameraConfiguration::class.java.classLoader,
                    CameraMode::class.java,
                )!!,
            )

            override fun newArray(size: Int): Array<Input?> = arrayOfNulls(size)
        }
    }
}
