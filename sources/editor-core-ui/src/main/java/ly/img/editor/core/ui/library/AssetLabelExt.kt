package ly.img.editor.core.ui.library

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import ly.img.engine.Asset

/**
 * Resolves the localized label for an engine asset by deriving a resource key from the
 * trailing segment of the asset id (mirrors CE.SDK Web).
 *
 * Key convention: `ly_img_editor_asset_label_<trailing_segment>` (e.g. `in/fade` → `..._fade`).
 * Customers override by declaring the same key in their app's `strings.xml`. Falls back to the
 * engine-provided label if no resource matches.
 */
@Composable
fun Asset.localizedLabel(): String {
    val engineLabel = label.orEmpty()
    val assetKey = id.substringAfterLast('/')
    if (assetKey.isEmpty()) return engineLabel
    val ctx = LocalContext.current
    val resName = "ly_img_editor_asset_label_$assetKey"
    val resId = remember(resName) {
        ctx.resources.getIdentifier(resName, "string", ctx.packageName)
    }
    return if (resId != 0) ctx.getString(resId) else engineLabel
}
