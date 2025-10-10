package ly.img.editor.core.engine

import android.content.Context
import ly.img.editor.core.library.data.AssetSourceType
import ly.img.editor.core.library.data.SystemGalleryAssetSource
import ly.img.engine.Engine

/**
 * Adds an asset source that exposes images and videos from the device gallery.
 * Please also declare the following permissions in your app manifest so the runtime requests can be triggered.
 * - `READ_MEDIA_IMAGES` and `READ_MEDIA_VIDEO` on Android 13+
 * - `READ_MEDIA_VISUAL_USER_SELECTED` on Android 14+ (to support the *selected photos* mode)
 * - Fallback to `READ_EXTERNAL_STORAGE` on Android 12 and lower
 */
fun Engine.addSystemGalleryAssetSources() {
    val context: Context = applicationContext
    asset.addSource(SystemGalleryAssetSource(context, AssetSourceType.GalleryAllVisuals))
    asset.addSource(SystemGalleryAssetSource(context, AssetSourceType.GalleryImage))
    asset.addSource(SystemGalleryAssetSource(context, AssetSourceType.GalleryVideo))
}
