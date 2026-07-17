package ly.img.editor.core.ui.library

import ly.img.editor.core.library.AssetType
import ly.img.editor.core.library.LibraryContent
import ly.img.editor.core.library.data.AssetSourceType
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryContentExtTest {
    private val updatedSource = AssetSourceType("updated")
    private val otherSource = AssetSourceType("other")

    @Test
    fun `detects referenced asset sources`() {
        assertTrue(grid(updatedSource).referencesAssetSource(updatedSource.sourceId))
        assertFalse(grid(otherSource).referencesAssetSource(updatedSource.sourceId))
        assertTrue(sections(otherSource, updatedSource).referencesAssetSource(updatedSource.sourceId))
        assertFalse(sections(otherSource).referencesAssetSource(updatedSource.sourceId))
    }

    private fun grid(sourceType: AssetSourceType) = LibraryContent.Grid(
        titleRes = 0,
        sourceType = sourceType,
        assetType = AssetType.Image,
    )

    private fun sections(vararg sourceTypes: AssetSourceType) = LibraryContent.Sections(
        titleRes = 0,
        sections = listOf(
            LibraryContent.Section(
                titleRes = 0,
                sourceTypes = sourceTypes.toList(),
                assetType = AssetType.Image,
            ),
        ),
    )
}
