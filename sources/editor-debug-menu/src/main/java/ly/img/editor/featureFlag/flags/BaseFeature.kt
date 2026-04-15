package ly.img.editor.featureFlag.flags

abstract class BaseFeature {
    internal abstract val id: String

    internal abstract val title: String

    internal abstract val description: String

    internal abstract val enabledByDefault: Boolean

    internal var overrideEnabled: Boolean? = null

    val enabled: Boolean
        get() = overrideEnabled ?: enabledByDefault
}
