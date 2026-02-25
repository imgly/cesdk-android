package ly.img.editor.base.timeline.thumbnail

import androidx.compose.ui.unit.Dp
import ly.img.editor.base.timeline.clip.Clip

/**
 * Sealed interface for timeline thumbnail providers.
 * Each clip type that needs visual representation has its own provider implementation:
 * - ThumbnailsImageProvider: For video/image/sticker/shape clips (generates thumbnails)
 * - ThumbnailsTextProvider: For text clips (fetches text content)
 * - ThumbnailsAudioProvider: For audio clips (generates waveform data)
 */
sealed interface ThumbnailsProvider {
    /**
     * Whether the provider is currently loading content.
     * Used to show shimmer/loading state in the UI.
     */
    val isLoading: Boolean

    /**
     * Loads the content for the given clip.
     * @param clip The clip to load content for
     * @param width The available width for display
     */
    fun loadContent(
        clip: Clip,
        width: Dp,
    )

    /**
     * Cancels any ongoing loading operations.
     */
    fun cancel()
}
