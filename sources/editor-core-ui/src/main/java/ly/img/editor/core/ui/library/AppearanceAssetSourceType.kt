package ly.img.editor.core.ui.library

import ly.img.editor.core.library.data.AssetSourceType

object AppearanceAssetSourceType {
    /**
     * The combined filter source covering both LUT and duotone effects. LUT vs duotone is
     * detected at runtime via asset metadata (`lightColor` / `darkColor` keys).
     */
    val Filter by lazy {
        AssetSourceType(sourceId = "ly.img.filter")
    }

    @Deprecated(
        message = "Use Filter which now covers both LUT and duotone effects.",
        replaceWith = ReplaceWith("Filter"),
    )
    val DuoToneFilter by lazy {
        AssetSourceType(sourceId = "ly.img.filter.duotone")
    }

    @Deprecated(
        message = "Use Filter which now covers both LUT and duotone effects.",
        replaceWith = ReplaceWith("Filter"),
    )
    val LutFilter by lazy {
        AssetSourceType(sourceId = "ly.img.filter.lut")
    }

    val FxEffect by lazy {
        AssetSourceType(sourceId = "ly.img.effect")
    }
    val Blur by lazy {
        AssetSourceType(sourceId = "ly.img.blur")
    }
}
