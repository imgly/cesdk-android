package ly.img.camera.components

import android.net.Uri
import androidx.camera.core.ImageCapture
import androidx.camera.core.Preview
import androidx.camera.video.Recorder
import androidx.camera.video.VideoCapture
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import ly.img.camera.ActiveMixedSubMode
import ly.img.camera.core.CameraConfiguration
import ly.img.camera.core.CaptureType
import ly.img.camera.core.R
import ly.img.camera.preview.CameraState
import ly.img.camera.record.PhotoCapture
import ly.img.camera.record.RecordingManager
import ly.img.camera.record.VideoRecorder
import ly.img.camera.record.components.CameraDock
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CameraDockLayoutTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun deleteAndNextStayOnOppositeSidesInBothLayoutDirections() {
        val orientation = mutableStateOf(0)
        val direction = mutableStateOf(LayoutDirection.Ltr)
        compose.setContent {
            val scope = rememberCoroutineScope()
            val configuration = remember { CameraConfiguration(captureType = CaptureType.Photo) }
            val camera = remember {
                CameraState(
                    Preview.Builder(),
                    VideoCapture.Builder(Recorder.Builder().build()),
                    ImageCapture.Builder(),
                    CaptureType.Photo,
                    false,
                )
            }
            val recordings = remember {
                RecordingManager(
                    maxDuration = configuration.maxTotalDuration,
                    allowExceedingMaxDuration = false,
                    photoClipDuration = configuration.photoClipDuration,
                    coroutineScope = scope,
                    videoRecorder = VideoRecorder({ error("No recording in this test") }, { error("No file in this test") }),
                    photoCapture = PhotoCapture({ null }, { error("No file in this test") }),
                    recordingGeometryProvider = { error("No recording in this test") },
                ).apply {
                    enable()
                    addPhoto(Uri.EMPTY)
                }
            }
            CompositionLocalProvider(LocalLayoutDirection provides direction.value) {
                MaterialTheme {
                    CameraDock(
                        orientation = orientation.value,
                        rotation = { orientation.value.toFloat() },
                        modifier = Modifier.width(320.dp),
                        cameraState = camera,
                        recordingManager = recordings,
                        cameraConfiguration = configuration,
                        activeMixedSubMode = ActiveMixedSubMode.Photo,
                        deletePreviousRecording = {},
                        capturePhoto = {},
                        setResult = {},
                    )
                }
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (layoutDirection in listOf(LayoutDirection.Ltr, LayoutDirection.Rtl)) {
            for (angle in listOf(0, 180)) {
                compose.runOnIdle {
                    direction.value = layoutDirection
                    orientation.value = angle
                }
                val delete = compose.onNodeWithContentDescription(context.getString(R.string.ly_img_camera_button_delete_last_recording))
                    .fetchSemanticsNode().boundsInRoot
                val next = compose.onNodeWithContentDescription(context.getString(R.string.ly_img_camera_button_continue))
                    .fetchSemanticsNode().boundsInRoot
                val deleteIsLeft = (layoutDirection == LayoutDirection.Ltr) == (angle == 0)
                assertTrue("$layoutDirection at $angle", if (deleteIsLeft) delete.right < next.left else next.right < delete.left)
            }
        }
    }
}
