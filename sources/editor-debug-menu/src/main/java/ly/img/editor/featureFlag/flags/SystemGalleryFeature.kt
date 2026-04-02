package ly.img.editor.featureFlag.flags

object SystemGalleryFeature : BaseFeature() {
    override val id: String = "ly.img.editor.featureFlag.flags.SystemGalleryFeature"
    override val title: String = "System Gallery Sources"

    override val description: String =
        "Toggle MediaStore-backed system gallery sources. Disable to show only manually added media in the gallery."

    override val enabledByDefault: Boolean = true
}
