package ly.img.camera.components

import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import ly.img.camera.setup.SetupView
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class CameraRemainingRotationTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun errorTextAndCloseButtonRotateTogether() {
        checkSetup(permissions = false)
    }

    @Test
    fun permissionPromptRotatesAndCancelRemainsReachable() {
        checkSetup(permissions = true)
    }

    private fun checkSetup(permissions: Boolean) {
        val orientation = mutableStateOf(0)
        var closes = 0
        compose.setContent {
            MaterialTheme {
                SetupView(
                    orientation = orientation.value,
                    rotation = { orientation.value.toFloat() },
                    modifier = Modifier.size(width = 320.dp, height = 640.dp),
                    hasRequiredPermissions = !permissions,
                    isLoading = false,
                    error = IllegalStateException(),
                    onClose = { closes++ },
                    onAllPermissionsGranted = {},
                )
            }
        }
        val title = if (permissions) {
            "Please allow the camera and microphone to have access."
        } else {
            "Cannot connect to service"
        }
        val action = if (permissions) "Cancel" else "Close"
        for (angle in listOf(0, 90, 180, 270)) {
            compose.runOnIdle { orientation.value = angle }
            compose.onNodeWithText(title).performScrollTo().assertIsDisplayed()
            assertTextRotation(title, angle)
            // Landscape has less vertical space; the final action must still be reachable.
            if (permissions) {
                compose.onNode(hasScrollAction()).performTouchInput {
                    when (angle) {
                        90 -> swipeRight()
                        180 -> swipeDown()
                        270 -> swipeLeft()
                        else -> swipeUp()
                    }
                }
            }
            compose.onNodeWithText(action).assertIsDisplayed()
            assertTextRotation(action, angle)
            compose.onNodeWithText(action).performTouchInput { click() }
        }
        compose.runOnIdle { assertEquals(4, closes) }
    }

    @Test
    fun photoPreviewButtonsRotateAndRemainClickable() {
        val orientation = mutableStateOf(0)
        var backs = 0
        var dones = 0
        compose.setContent {
            MaterialTheme {
                Box(Modifier.size(width = 320.dp, height = 640.dp), contentAlignment = Alignment.BottomCenter) {
                    PhotoPreviewActions(
                        orientation = orientation.value,
                        rotation = { orientation.value.toFloat() },
                        modifier = Modifier.fillMaxWidth(),
                        onBack = { backs++ },
                        onDone = { dones++ },
                    )
                }
            }
        }
        for (angle in listOf(0, 90, 180, 270)) {
            compose.runOnIdle { orientation.value = angle }
            for (label in listOf("Back", "Done")) {
                compose.onNodeWithText(label).assertIsDisplayed()
                assertTextRotation(label, angle)
                compose.onNodeWithText(label).performTouchInput { click() }
            }
        }
        compose.runOnIdle {
            assertEquals(4, backs)
            assertEquals(4, dones)
        }
    }

    @Test
    fun permissionSettingsDialogUsesCameraRotation() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val packageName = instrumentation.targetContext.packageName

        fun permissionFlags(operation: String) {
            val descriptor = instrumentation.uiAutomation.executeShellCommand(
                "pm $operation-permission-flags $packageName android.permission.CAMERA user-set user-fixed",
            )
            ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
        }
        // Simulate a previously denied permission without interacting with an OS permission prompt.
        permissionFlags("set")
        try {
            val orientation = mutableStateOf(0)
            compose.setContent {
                MaterialTheme {
                    SetupView(
                        orientation = orientation.value,
                        rotation = { orientation.value.toFloat() },
                        modifier = Modifier.fillMaxSize(),
                        hasRequiredPermissions = false,
                        isLoading = false,
                        error = null,
                        onClose = {},
                        onAllPermissionsGranted = {},
                        requestOnlyCameraPermission = true,
                    )
                }
            }
            compose.onNodeWithText("Allow camera").performScrollTo().performTouchInput { click() }
            val title = "Camera permission needed"
            compose.waitUntil(timeoutMillis = 5000) {
                compose.onAllNodesWithText(title).fetchSemanticsNodes().isNotEmpty()
            }
            for (angle in listOf(0, 90, 180, 270)) {
                compose.runOnIdle { orientation.value = angle }
                compose.onNodeWithText(title).assertIsDisplayed()
                compose.onNodeWithText("Settings").assertIsDisplayed()
                assertTextRotation(title, angle)
                assertTextRotation("Settings", angle)
            }
        } finally {
            permissionFlags("clear")
        }
    }

    private fun assertTextRotation(
        text: String,
        angle: Int,
    ) {
        val bounds = compose.onNodeWithText(text).fetchSemanticsNode().boundsInRoot
        val sideways = angle == 90 || angle == 270
        assertTrue(
            "Incorrect rotation for $text at $angle: $bounds",
            if (sideways) {
                bounds.height > bounds.width
            } else {
                bounds.width >
                    bounds.height
            },
        )
    }
}
