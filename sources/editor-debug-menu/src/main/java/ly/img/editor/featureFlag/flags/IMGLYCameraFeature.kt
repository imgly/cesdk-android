package ly.img.editor.featureFlag.flags

object IMGLYCameraFeature : BaseFeature() {
    override val id: String = "ly.img.editor.featureFlag.flags.IMGLYCameraFeature"
    override val title: String = "IMG.LY Camera Feature"

    override val description: String = "If enabled IMG.LY camera will be used in the editor, if disable system camera will be used."

    override val enabledByDefault: Boolean = true
}
