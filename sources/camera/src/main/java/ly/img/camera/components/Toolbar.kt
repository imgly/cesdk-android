package ly.img.camera.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import ly.img.camera.core.R
import ly.img.camera.record.components.TimecodeView
import ly.img.editor.core.iconpack.Close
import ly.img.editor.core.iconpack.IconPack
import ly.img.editor.core.theme.LocalExtendedColorScheme
import kotlin.time.Duration

internal val TOOLBAR_HEIGHT = 64.dp

@Composable
internal fun BoxScope.Toolbar(
    orientation: Int,
    rotation: () -> Float,
    isRecording: Boolean,
    duration: Duration,
    maxDuration: Duration,
    recordingColor: Color,
    showTimecode: Boolean,
    closeEnabled: Boolean,
    onCloseClick: () -> Unit,
) {
    CameraOrientationLayout(
        orientation = orientation,
        rotation = rotation,
        alignmentForRotation = { rotation ->
            when (rotation) {
                90 -> AbsoluteAlignment.TopRight
                180 -> AbsoluteAlignment.BottomRight
                270 -> AbsoluteAlignment.BottomLeft
                else -> AbsoluteAlignment.TopLeft
            }
        },
    ) {
        IconButton(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            enabled = closeEnabled,
            onClick = onCloseClick,
        ) {
            Icon(
                IconPack.Close,
                tint = LocalExtendedColorScheme.current.white,
                contentDescription = stringResource(R.string.ly_img_camera_button_close),
            )
        }
    }

    if (showTimecode) {
        CameraOrientationLayout(
            orientation = orientation,
            rotation = rotation,
            alignmentForRotation = { rotation ->
                when (rotation) {
                    90 -> AbsoluteAlignment.CenterRight
                    180 -> Alignment.TopCenter
                    270 -> AbsoluteAlignment.CenterLeft
                    else -> Alignment.TopCenter
                }
            },
        ) {
            Box(
                modifier = Modifier.height(TOOLBAR_HEIGHT),
                contentAlignment = Alignment.Center,
            ) {
                TimecodeView(
                    modifier = Modifier,
                    duration = duration,
                    maxDuration = maxDuration,
                    isRecording = isRecording,
                    recordingColor = recordingColor,
                )
            }
        }
    }
}
