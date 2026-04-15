package ly.img.editor.featureFlag.flags

object RemoteAssetsFeature : BaseFeature() {
    override val id: String = "ly.img.editor.featureFlag.flags.RemoteAssetsFeature"
    override val title: String = "Remote Assets"

    override val description: String =
        "Skip the local asset bundle so remote assets are used instead. " +
            "Useful for testing branch-deployed remote assets."

    override val enabledByDefault: Boolean = false
}
