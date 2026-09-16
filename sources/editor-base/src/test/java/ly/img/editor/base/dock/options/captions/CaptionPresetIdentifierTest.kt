package ly.img.editor.base.dock.options.captions

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The preset stamp recorded on a caption track. Reading it back wrong silently resets a styled track to the
 * default preset — and a scene authored elsewhere must resolve to the same preset here.
 */
class CaptionPresetIdentifierTest {
    @Test
    fun `a bare asset id is the stamp itself`() {
        assertEquals(
            "ly.img.caption.presets.outline",
            CaptionsEngine.stampedAssetId("ly.img.caption.presets.outline"),
        )
    }

    @Test
    fun `a stamp written by an earlier build still resolves to its asset id`() {
        assertEquals(
            "ly.img.caption.presets.outline",
            CaptionsEngine.stampedAssetId("ly.img.caption.presets|ly.img.caption.presets.outline"),
        )
    }

    @Test
    fun `an escaped separator in a legacy source id does not leak into the asset id`() {
        assertEquals("preset", CaptionsEngine.stampedAssetId("custom%7Csource|preset"))
    }

    @Test
    fun `an empty stamp stays empty`() {
        assertEquals("", CaptionsEngine.stampedAssetId(""))
    }
}
