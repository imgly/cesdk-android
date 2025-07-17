package ly.img.camera.components.sidemenu.layout

import androidx.annotation.StringRes
import androidx.compose.ui.graphics.vector.ImageVector
import ly.img.camera.R
import ly.img.editor.core.ui.iconpack.IconPack
import ly.img.editor.core.ui.iconpack.SplitscreenBottomOutline
import ly.img.editor.core.ui.iconpack.SplitscreenLeftOutline

internal sealed class LayoutMode(
    val icon: ImageVector,
    @StringRes val label: Int,
) {
    data object Vertical : LayoutMode(IconPack.SplitscreenBottomOutline, R.string.ly_img_camera_layout_vertical)

    data object Horizontal : LayoutMode(IconPack.SplitscreenLeftOutline, R.string.ly_img_camera_layout_horizontal)
}
