package ly.img.editor.plugin.voiceIsolation

/**
 * Voice isolation plugin configured with IMG.LY's on-device DeepFilterNet3 model.
 */
open class IMGLYVoiceIsolationPlugin : VoiceIsolationPlugin() {
    init {
        config = IMGLYVoiceIsolationConfig()
    }
}
