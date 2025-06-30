package ly.img.editor.core.ui.library

import ly.img.editor.core.library.data.AssetSourceType

object CropAssetSourceType {
    val Crop by lazy {
        AssetSourceType(sourceId = "ly.img.crop.presets")
    }
    val Page by lazy {
        AssetSourceType(sourceId = "ly.img.page.presets")
    }
}
