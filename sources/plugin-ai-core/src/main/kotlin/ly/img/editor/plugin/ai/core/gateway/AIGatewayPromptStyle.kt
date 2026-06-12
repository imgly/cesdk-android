package ly.img.editor.plugin.ai.core.gateway

/**
 * A style preset that steers AI content generation via prompt engineering.
 *
 * No gateway model exposes a native `style` parameter in its schema today,
 * so the showcase applies styles client-side by appending [promptSnippet]
 * to the user's prompt before sending the request to `gateway.img.ly`.
 *
 * Each style ships a thumbnail in `assets/style_thumbnails/<id>.jpeg`,
 * loaded via Coil from [thumbnailAssetUri]. The "none" entry has no
 * thumbnail — the picker falls back to a "no style" glyph.
 */
data class AIGatewayPromptStyle(
    val id: String,
    val displayName: String,
    val promptSnippet: String,
) {
    /**
     * Coil-compatible URI pointing at the bundled JPEG in
     * `assets/style_thumbnails/`. Empty for the `"none"` entry (the
     * picker treats empty as a no-style placeholder).
     */
    val thumbnailAssetUri: String
        get() = if (id == NONE_ID) "" else "file:///android_asset/$THUMBNAILS_DIR/$id.jpeg"

    companion object {
        const val NONE_ID = "none"
        private const val THUMBNAILS_DIR = "style_thumbnails"

        /**
         * The 14-style set the showcase ships with. To customize, edit this
         * array directly — each new style needs the three fields below, and
         * a matching `<id>.jpeg` under `assets/style_thumbnails/`.
         *
         * Non-breaking hyphens (U+2011) inside the snippets keep compound
         * words like `cel‑shaded` from splitting across model tokenisation.
         */
        val curated: List<AIGatewayPromptStyle> = listOf(
            AIGatewayPromptStyle(
                id = NONE_ID,
                displayName = "None",
                promptSnippet = "",
            ),
            AIGatewayPromptStyle(
                id = "anime-celshaded",
                displayName = "Anime",
                promptSnippet = "anime cel‑shaded, bright pastel palette, expressive eyes, clean line art ",
            ),
            AIGatewayPromptStyle(
                id = "cyberpunk-neon",
                displayName = "Cyberpunk",
                promptSnippet = "cyberpunk cityscape, glowing neon signage, reflective puddles, dark atmosphere",
            ),
            AIGatewayPromptStyle(
                id = "kodak-portra-400",
                displayName = "Kodak Portra 400",
                promptSnippet = "shot on Kodak Portra 400, soft grain, golden‑hour warmth, 35 mm photo",
            ),
            AIGatewayPromptStyle(
                id = "watercolor-storybook",
                displayName = "Watercolor",
                promptSnippet = "loose watercolor washes, gentle gradients, dreamy storybook feel",
            ),
            AIGatewayPromptStyle(
                id = "dark-fantasy-realism",
                displayName = "Dark Fantasy",
                promptSnippet = "dark fantasy realm, moody chiaroscuro lighting, hyper‑real textures",
            ),
            AIGatewayPromptStyle(
                id = "vaporwave-retrofuturism",
                displayName = "Vaporwave",
                promptSnippet = "retro‑futuristic vaporwave, pastel sunset gradient, chrome text, VHS scanlines",
            ),
            AIGatewayPromptStyle(
                id = "minimal-vector-flat",
                displayName = "Vector Flat",
                promptSnippet = "minimalist flat vector illustration, bold geometry, two‑tone palette",
            ),
            AIGatewayPromptStyle(
                id = "pixarstyle-3d-render",
                displayName = "3D Animation",
                promptSnippet =
                    "Pixar‑style 3D render, oversized eyes, subtle subsurface scattering, cinematic lighting",
            ),
            AIGatewayPromptStyle(
                id = "ukiyoe-woodblock",
                displayName = "Ukiyo-e",
                promptSnippet =
                    "ukiyo‑e woodblock print, Edo‑period style, visible washi texture, limited color ink",
            ),
            AIGatewayPromptStyle(
                id = "surreal-dreamscape",
                displayName = "Surreal",
                promptSnippet = "surreal dreamscape, floating objects, impossible architecture, vivid clouds",
            ),
            AIGatewayPromptStyle(
                id = "steampunk-victorian",
                displayName = "Steampunk",
                promptSnippet = "Victorian steampunk world, ornate brass gears, leather attire, atmospheric fog",
            ),
            AIGatewayPromptStyle(
                id = "nightstreet-photo-bokeh",
                displayName = "Night Bokeh",
                promptSnippet = "night‑time street shot, large aperture bokeh lights, candid urban mood",
            ),
            AIGatewayPromptStyle(
                id = "comicbook-pop-art",
                displayName = "Pop Art",
                promptSnippet =
                    "classic comic‑book panel, halftone shading, exaggerated action lines, CMYK pop colors",
            ),
        )
    }
}
