package ly.img.camera.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ly.img.camera.ActiveMixedSubMode
import ly.img.camera.core.R
import ly.img.camera.record.components.PhotoVideoToggle
import ly.img.editor.core.theme.LocalExtendedColorScheme
import ly.img.editor.core.ui.iconpack.FlashOff
import ly.img.editor.core.ui.iconpack.FlashOn
import ly.img.editor.core.ui.iconpack.FlashlightOff
import ly.img.editor.core.ui.iconpack.FlashlightOn
import ly.img.editor.core.ui.iconpack.FlipCamera
import ly.img.editor.core.ui.iconpack.IconPack
import ly.img.editor.core.ui.iconpack.SwapHoriz

@Composable
internal fun CameraControls(
    isCameraReady: Boolean,
    isFlashEnabled: Boolean,
    isFlashOn: Boolean,
    isSwappingAllowed: Boolean,
    showPhotoVideoToggle: Boolean,
    activeMixedSubMode: ActiveMixedSubMode,
    behavesAsPhoto: Boolean,
    toggleFlash: () -> Unit,
    toggleCamera: () -> Unit,
    swapLayoutPositions: () -> Unit,
    onSubModeChange: (ActiveMixedSubMode) -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(84.dp)
            .background(LocalExtendedColorScheme.current.black)
            .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 28.dp),
    ) {
        if (isCameraReady) {
            CompositionLocalProvider(
                LocalContentColor provides MaterialTheme.colorScheme.onSurfaceVariant,
            ) {
                if (isFlashEnabled) {
                    // Photo / Mixed-photo uses `ImageCapture.FLASH_MODE_*` (a true single-shot
                    // flash) — render the lightning-bolt icon. Video / Mixed-video uses the
                    // continuous-LED torch — render the flashlight icon.
                    val onIcon = if (behavesAsPhoto) IconPack.FlashOn else IconPack.FlashlightOn
                    val offIcon = if (behavesAsPhoto) IconPack.FlashOff else IconPack.FlashlightOff
                    IconButton(
                        modifier = Modifier.align(Alignment.CenterStart),
                        onClick = toggleFlash,
                    ) {
                        Icon(
                            if (isFlashOn) onIcon else offIcon,
                            contentDescription = stringResource(R.string.ly_img_camera_button_toggle_flash),
                        )
                    }
                }

                // The center slot is shared between the Reaction "swap-positions" button and the
                // Mixed-mode photo↔video toggle. `CameraMode.supports` forbids `Reaction × Mixed`,
                // so the two predicates are guaranteed mutually exclusive.
                if (isSwappingAllowed) {
                    IconButton(
                        modifier = Modifier.align(Alignment.Center),
                        onClick = swapLayoutPositions,
                    ) {
                        Icon(
                            IconPack.SwapHoriz,
                            contentDescription = stringResource(R.string.ly_img_camera_button_swap_positions),
                        )
                    }
                } else if (showPhotoVideoToggle) {
                    PhotoVideoToggle(
                        activeMixedSubMode = activeMixedSubMode,
                        onSubModeChange = onSubModeChange,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }

                IconButton(
                    modifier = Modifier.align(Alignment.CenterEnd),
                    onClick = toggleCamera,
                ) {
                    Icon(
                        IconPack.FlipCamera,
                        contentDescription = stringResource(R.string.ly_img_camera_button_flip_camera),
                    )
                }
            }
        }
    }
}
