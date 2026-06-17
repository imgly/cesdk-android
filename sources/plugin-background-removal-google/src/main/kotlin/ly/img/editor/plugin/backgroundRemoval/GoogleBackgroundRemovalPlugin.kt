package ly.img.editor.plugin.backgroundRemoval

/**
 * Background removal plugin configured with Google's on-device ML Kit segmentation backend.
 */
open class GoogleBackgroundRemovalPlugin : BackgroundRemovalPlugin() {
    init {
        config = GoogleBackgroundRemovalConfig()
    }
}
