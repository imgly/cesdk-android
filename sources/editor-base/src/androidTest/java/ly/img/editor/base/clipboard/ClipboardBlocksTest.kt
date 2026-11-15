package ly.img.editor.base.clipboard

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The block format that the web and iOS editors share. `bindings/wasm/js_web/src/clipboard/cesdkBlocks.test.ts` reads
 * [MOBILE_COPY] with the web parser, and the iOS `ClipboardTests.swift` writes the same HTML.
 */
class ClipboardBlocksTest {
    @Test
    fun writesTheWebFormat() {
        val html = ClipboardBlocks.html(
            id = "mobile-1",
            blocks = """{"a":"<b> & c"}""",
            resources = listOf("buffer://ly.img.clipboard/mobile-1/0" to ClipboardBlocks.dataUrl("image/png", byteArrayOf(1, 2, 3))),
            textHtml = "<p>Hi</p>",
        )
        assertEquals(MOBILE_COPY, html)
    }

    @Test
    fun readsTheWebFormat() {
        val payload = requireNotNull(ClipboardBlocks.parse(WEB_COPY))
        assertEquals("web-1", payload.id)
        assertEquals("""{"a":"<b> & c"}""", payload.blocks)
        // Only the URLs of the format: a forged copy cannot move a URL of the scene.
        assertEquals(setOf("buffer://ly.img.clipboard/web-1/0"), payload.resources.keys)
        val bytes = ClipboardBlocks.decodeDataUrl(payload.resources.getValue("buffer://ly.img.clipboard/web-1/0"))
        assertArrayEquals(byteArrayOf(1, 2, 3), bytes)
    }

    @Test
    fun readsOtherSerializations() {
        // Single quotes, unescaped `>` and numeric references, as other apps rewrite the HTML.
        val html = "<SPAN data-cesdk-blocks-id='x' data-cesdk-blocks-version='1' data-cesdk-blocks='a>b&#38;&#x3C;'>" +
            "<span data-cesdk-resource=\"buffer://ly.img.clipboard/x/0\" data-cesdk-resource-data=\"data:text/plain,a%20b%+1%\">"
        val payload = requireNotNull(ClipboardBlocks.parse(html))
        assertEquals("a>b&<", payload.blocks)
        val resource = payload.resources.getValue("buffer://ly.img.clipboard/x/0")
        assertArrayEquals("a b%+1%".toByteArray(), ClipboardBlocks.decodeDataUrl(resource))
        assertEquals("text/plain", ClipboardBlocks.dataUrlMimeType(resource))
    }

    @Test
    fun rejectsAnotherVersion() {
        assertNull(ClipboardBlocks.parse("""<span data-cesdk-blocks-version="2" data-cesdk-blocks="x"></span>"""))
        assertNull(ClipboardBlocks.parse("<p>no blocks</p>"))
    }

    @Test
    fun recognizesSvgMarkup() {
        assertTrue(
            ClipboardSvg.isMarkup(
                "\uFEFF<?xml version=\"1.0\"?>\n<!-- c --><!DOCTYPE svg [ <!ENTITY a \"b\"> ]><svg viewBox=\"0 0 1 1\"><rect/></svg>\n",
            ),
        )
        assertFalse(ClipboardSvg.isMarkup("<svgx></svgx>"))
        assertFalse(ClipboardSvg.isMarkup("<p><svg></svg></p>"))
        assertFalse(ClipboardSvg.isMarkup("<!-- open"))
    }

    private companion object {
        /** The HTML that the web editor writes, as Chrome serializes it and wraps it on the clipboard. */
        const val WEB_COPY = "<html><body><!--StartFragment--><span data-cesdk-blocks-version=\"1\" " +
            "data-cesdk-blocks-id=\"web-1\" data-cesdk-blocks=\"{&quot;a&quot;:&quot;&lt;b&gt; &amp; c&quot;}\"></span>" +
            "<span data-cesdk-resource=\"buffer://ly.img.clipboard/web-1/0\" " +
            "data-cesdk-resource-data=\"data:image/png;base64,AQID\"></span>" +
            "<span data-cesdk-resource=\"https://example.com/x.png\" data-cesdk-resource-data=\"data:image/png;base64,BAUG\"></span>" +
            "<p>Hi</p><!--EndFragment--></body></html>"

        const val MOBILE_COPY = "<span data-cesdk-blocks-version=\"1\" data-cesdk-blocks-id=\"mobile-1\" " +
            "data-cesdk-blocks=\"{&quot;a&quot;:&quot;&lt;b&gt; &amp; c&quot;}\"></span>" +
            "<span data-cesdk-resource=\"buffer://ly.img.clipboard/mobile-1/0\" " +
            "data-cesdk-resource-data=\"data:image/png;base64,AQID\"></span>" +
            "<p>Hi</p>"
    }
}
