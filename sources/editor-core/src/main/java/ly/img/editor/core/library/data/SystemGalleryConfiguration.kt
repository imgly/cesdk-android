package ly.img.editor.core.library.data

/**
 * Configuration that controls whether the in-app system gallery integration is active.
 * When disabled, the editor still exposes the `SystemGalleryAssetSource` but skips querying the
 * device `MediaStore`. The gallery UI stays available and lists only the media that the user adds
 * through the upload buttons, camera intents, or limited selections during the current session.
 */
data class SystemGalleryConfiguration(
    val enableAssetSource: Boolean = false,
) {
    companion object {
        val Disabled = SystemGalleryConfiguration(enableAssetSource = false)
        val Enabled = SystemGalleryConfiguration(enableAssetSource = true)
    }
}
