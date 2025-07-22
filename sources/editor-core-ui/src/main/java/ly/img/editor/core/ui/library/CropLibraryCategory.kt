package ly.img.editor.core.ui.library

import ly.img.editor.core.iconpack.IconPack
import ly.img.editor.core.iconpack.LibraryElements
import ly.img.editor.core.iconpack.LibraryElementsOutline
import ly.img.editor.core.library.AssetType
import ly.img.editor.core.library.LibraryCategory
import ly.img.editor.core.library.LibraryContent

object CropLibraryCategory {
    val Crop by lazy {
        LibraryCategory(
            tabTitleRes = 0,
            tabSelectedIcon = IconPack.LibraryElements,
            tabUnselectedIcon = IconPack.LibraryElementsOutline,
            content = LibraryContent.Sections(
                titleRes = 0,
                sections = listOf(
                    LibraryContent.Section(
                        sourceTypes = listOf(CropAssetSourceType.Crop),
                        count = Int.MAX_VALUE,
                        assetType = AssetType.Filter,
                        expandContent = null,
                    ),
                ),
            ),
        )
    }

    val Page by lazy {
        LibraryCategory(
            tabTitleRes = 0,
            tabSelectedIcon = IconPack.LibraryElements,
            tabUnselectedIcon = IconPack.LibraryElementsOutline,
            content = LibraryContent.Sections(
                titleRes = 0,
                sections = listOf(
                    LibraryContent.Section(
                        sourceTypes = listOf(
                            CropAssetSourceType.Page,
                        ),
                        count = Int.MAX_VALUE,
                        assetType = AssetType.Filter,
                        expandContent = null,
                    ),
                ),
            ),
        )
    }
}
