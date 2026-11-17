package ly.img.editor.base.clipboard

import kotlinx.coroutines.CancellationException
import ly.img.engine.Asset
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import ly.img.engine.FindAssetsQuery
import ly.img.engine.FontStyle
import ly.img.engine.FontWeight
import ly.img.engine.Typeface
import org.json.JSONException
import org.json.JSONObject

/**
 * The typeface of a text block that a paste creates, as the web editor picks it, so formatted text keeps its bold and
 * italic: the typeface that the copy names, if the scene has it, then the family of most text presets, then the first
 * typeface of the library. Each of the last two needs a regular, a bold and an italic font.
 */
internal object PastedTextTypeface {
    private const val TEXT_PRESET_SOURCE_ID = "ly.img.text"
    private const val TYPEFACE_SOURCE_ID = "ly.img.typeface"
    private const val PAGE_SIZE = 100
    private val typefaceAttribute = Regex("data-cesdk-typeface=\"([^\"]*)\"")

    suspend fun find(
        engine: Engine,
        html: String,
    ): Typeface? {
        typefaceName(html)?.let { name ->
            for (text in engine.block.findByType(DesignBlockType.Text)) {
                runCatching { engine.block.getTypefaces(text) }.getOrNull()?.firstOrNull { it.name == name }?.let { return it }
            }
        }
        val sources = engine.asset.findAllSources()
        if (TEXT_PRESET_SOURCE_ID in sources) {
            typefaceOfTextPresets(engine)?.takeIf(::hasRegularBoldAndItalic)?.let { return it }
        }
        if (TYPEFACE_SOURCE_ID !in sources) return null
        var page = 0
        while (true) {
            val result = runCatching {
                engine.asset.findAssets(TYPEFACE_SOURCE_ID, FindAssetsQuery(perPage = PAGE_SIZE, page = page))
            }.getOrNull() ?: return null
            result.assets.firstNotNullOfOrNull { asset -> asset.payload?.typeface?.takeIf(::hasRegularBoldAndItalic) }?.let { return it }
            if (result.nextPage <= page) return null
            page = result.nextPage
        }
    }

    /** The value of `data-cesdk-typeface`, which the engine writes in the HTML of a copied text. */
    private fun typefaceName(html: String): String? = typefaceAttribute
        .find(html)
        ?.groupValues
        ?.get(1)
        ?.replace("&quot;", "\"")
        ?.replace("&lt;", "<")
        ?.replace("&gt;", ">")
        ?.replace("&amp;", "&")

    private suspend fun typefaceOfTextPresets(engine: Engine): Typeface? {
        val assets = runCatching {
            engine.asset.findAssets(TEXT_PRESET_SOURCE_ID, FindAssetsQuery(perPage = PAGE_SIZE, page = 0)).assets
        }.getOrNull() ?: return null
        val byFamily = LinkedHashMap<String, MutableList<Asset>>()
        for (asset in assets) {
            val preset = asset.payload?.stylePreset ?: continue
            val json = try {
                JSONObject(preset)
            } catch (_: JSONException) {
                continue
            }
            if (json.optString("blockType", DesignBlockType.Text.key) != DesignBlockType.Text.key) continue
            val family = json.optJSONObject("typeface")?.optString("family")?.takeIf { it.isNotEmpty() } ?: continue
            byFamily.getOrPut(family) { mutableListOf() } += asset
        }
        // The first family with the most presets, as the web editor's reduce keeps the first maximum.
        val preset = byFamily.values.fold(emptyList<Asset>()) { most, presets -> if (presets.size > most.size) presets else most }
            .firstOrNull() ?: return null
        val scratch = engine.block.create(DesignBlockType.Text)
        return try {
            engine.asset.applyAssetSourceAsset(TEXT_PRESET_SOURCE_ID, preset, scratch)
            engine.block.getTypeface(scratch)
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            null
        } finally {
            engine.block.destroy(scratch)
        }
    }

    private fun hasRegularBoldAndItalic(typeface: Typeface): Boolean {
        fun has(
            weight: FontWeight,
            style: FontStyle,
        ) = typeface.fonts.any { it.weight == weight && it.style == style }
        return has(FontWeight.NORMAL, FontStyle.NORMAL) &&
            has(FontWeight.BOLD, FontStyle.NORMAL) &&
            has(FontWeight.NORMAL, FontStyle.ITALIC)
    }
}
