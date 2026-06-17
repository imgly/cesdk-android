package ly.img.editor.core.ui.library

import ly.img.editor.core.iconpack.IconPack
import ly.img.editor.core.iconpack.LibraryElements
import ly.img.editor.core.iconpack.LibraryElementsOutline
import ly.img.editor.core.library.AssetType
import ly.img.editor.core.library.LibraryCategory
import ly.img.editor.core.library.LibraryContent

object AppearanceLibraryCategory {
    val Filters by lazy {
        LibraryCategory(
            tabTitleRes = 0,
            tabSelectedIcon = IconPack.LibraryElements,
            tabUnselectedIcon = IconPack.LibraryElementsOutline,
            content = LibraryContent.Sections(
                titleRes = 0,
                sections = listOf(
                    LibraryContent.Section(
                        sourceTypes = listOf(AppearanceAssetSourceType.DuoToneFilter),
                        count = Int.MAX_VALUE,
                        assetType = AssetType.Filter,
                        expandContent = null,
                    ),
                    LibraryContent.Section(
                        sourceTypes = listOf(AppearanceAssetSourceType.LutFilter),
                        addGroupedSubSections = true,
                        count = Int.MAX_VALUE,
                        assetType = AssetType.Filter,
                        expandContent = null,
                    ),
                ),
            ),
        )
    }

    val FxEffects by lazy {
        LibraryCategory(
            tabTitleRes = 0,
            tabSelectedIcon = IconPack.LibraryElements,
            tabUnselectedIcon = IconPack.LibraryElementsOutline,
            content = LibraryContent.Sections(
                titleRes = 0,
                sections = listOf(
                    LibraryContent.Section(
                        sourceTypes = listOf(AppearanceAssetSourceType.FxEffect),
                        count = Int.MAX_VALUE,
                        assetType = AssetType.Effect,
                        expandContent = null,
                    ),
                ),
            ),
        )
    }

    val Blur by lazy {
        LibraryCategory(
            tabTitleRes = 0,
            tabSelectedIcon = IconPack.LibraryElements,
            tabUnselectedIcon = IconPack.LibraryElementsOutline,
            content = LibraryContent.Sections(
                titleRes = 0,
                sections = listOf(
                    LibraryContent.Section(
                        sourceTypes = listOf(AppearanceAssetSourceType.Blur),
                        count = Int.MAX_VALUE,
                        assetType = AssetType.Blur,
                        expandContent = null,
                    ),
                ),
            ),
        )
    }
}
