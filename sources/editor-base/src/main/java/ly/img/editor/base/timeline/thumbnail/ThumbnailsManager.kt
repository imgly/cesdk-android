package ly.img.editor.base.timeline.thumbnail

import androidx.compose.ui.unit.Dp
import kotlinx.coroutines.CoroutineScope
import ly.img.editor.base.timeline.clip.Clip
import ly.img.editor.base.timeline.clip.ClipType
import ly.img.engine.DesignBlock
import ly.img.engine.Engine

/**
 * Manages thumbnail providers for timeline clips.
 * Creates the appropriate provider type based on clip type:
 * - ThumbnailsImageProvider: For video, image, sticker, shape, group clips
 * - ThumbnailsTextProvider: For text clips
 * - ThumbnailsAudioProvider: For audio clips (real waveform visualization)
 */
class ThumbnailsManager(
    private val engine: Engine,
    private val scope: CoroutineScope,
) {
    private val providers = hashMapOf<DesignBlock, ThumbnailsProvider>()

    fun getProvider(designBlock: DesignBlock): ThumbnailsProvider? = providers[designBlock]

    fun destroyProvider(designBlock: DesignBlock) {
        val provider = providers.remove(designBlock)
        provider?.cancel()
    }

    fun refreshThumbnails(
        clip: Clip,
        width: Dp,
    ) {
        val provider = providers.getOrPut(clip.id) {
            createProvider(clip.clipType)
        }
        provider.loadContent(clip, width)
    }

    /**
     * Creates the appropriate provider based on clip type.
     */
    private fun createProvider(clipType: ClipType): ThumbnailsProvider = when (clipType) {
        ClipType.Text -> ThumbnailsTextProvider(engine)
        ClipType.Audio -> ThumbnailsAudioProvider(engine, scope)
        else -> ThumbnailsImageProvider(engine, scope)
    }
}
