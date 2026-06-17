package ly.img.editor.plugin.backgroundRemoval

/**
 * Background removal plugin configured with IMG.LY's ONNX Runtime segmentation backend.
 */
open class IMGLYBackgroundRemovalPlugin : BackgroundRemovalPlugin() {
    init {
        config = IMGLYBackgroundRemovalConfig()
    }
}
