package ly.img.editor.core.ui.library

import ly.img.editor.core.library.LibraryContent

internal fun LibraryContent.referencesAssetSource(sourceId: String): Boolean = when (this) {
    is LibraryContent.Grid -> sourceType.sourceId == sourceId
    is LibraryContent.Sections -> sections.any { section ->
        section.sourceTypes.any { sourceType -> sourceType.sourceId == sourceId }
    }
}
