package ly.img.editor.base.timeline.thumbnail

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.unit.Dp
import ly.img.editor.base.timeline.clip.Clip
import ly.img.engine.Engine

/**
 * Provider for text clip content.
 * Fetches and holds the text content from the engine block.
 *
 * The provider fetches text content from the engine and the view displays
 * the text directly (not as a rendered image).
 *
 * Text loading is synchronous and instant, so isLoading is always false.
 */
class ThumbnailsTextProvider(
    private val engine: Engine,
) : ThumbnailsProvider {
    private var _text = mutableStateOf("")
    val text: String
        get() = _text.value

    /**
     * Text content loads synchronously (engine property read), so loading is always instant.
     */
    override val isLoading: Boolean
        get() = false

    override fun loadContent(
        clip: Clip,
        width: Dp,
    ) {
        _text.value = runCatching {
            engine.block.getString(clip.id, "text/text")
                // Replace engine line/paragraph separators (\u2028, \u2029) that Compose can't render.
                .replace(Regex("[\u2028\u2029\n]"), " ")
        }.getOrDefault("")
    }

    override fun cancel() {
        // No async operation to cancel for text content
    }
}
