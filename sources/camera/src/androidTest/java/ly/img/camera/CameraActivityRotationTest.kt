package ly.img.camera

import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.os.ParcelFileDescriptor
import android.os.SystemClock
import androidx.camera.core.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ViewModelProvider
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import ly.img.camera.core.CaptureMedia
import ly.img.camera.core.EngineConfiguration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraActivityRotationTest {
    @Test
    fun forcedWindowRotationKeepsTheCameraActivityResumed() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        for (permission in listOf("android.permission.CAMERA", "android.permission.RECORD_AUDIO")) {
            val descriptor = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand(
                "pm grant ${context.packageName} $permission",
            )
            ParcelFileDescriptor.AutoCloseInputStream(descriptor).use { it.readBytes() }
        }
        val intent = CaptureMedia().createIntent(context, CaptureMedia.Input(EngineConfiguration()))
        ActivityScenario.launch<CameraActivity>(intent).use { scenario ->
            lateinit var originalActivity: CameraActivity
            var stops = 0
            lateinit var viewModel: CameraViewModel
            lateinit var originalPreview: Preview
            var previousZoom = 0f
            scenario.onActivity { activity ->
                originalActivity = activity
                viewModel = ViewModelProvider(activity)[CameraViewModel::class.java]
                activity.lifecycle.addObserver(
                    LifecycleEventObserver { _, event ->
                        if (event == Lifecycle.Event.ON_STOP) stops++
                    },
                )
            }

            waitUntil(timeoutMillis = 30_000) {
                var ready = false
                scenario.onActivity { ready = viewModel.cameraState.isReady }
                ready
            }
            scenario.onActivity {
                originalPreview = viewModel.cameraState.previewUseCase
                previousZoom = viewModel.engine.scene.getZoomLevel()
            }

            for ((requested, expected) in listOf(
                ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE to Configuration.ORIENTATION_LANDSCAPE,
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT to Configuration.ORIENTATION_PORTRAIT,
            )) {
                scenario.onActivity { it.requestedOrientation = requested }
                var diagnostics = ""
                waitUntil(timeoutMillis = 10_000, message = { diagnostics }) {
                    var rotated = false
                    scenario.onActivity {
                        diagnostics = "configuration=${it.resources.configuration.orientation}, expected=$expected, " +
                            "preview=${originalPreview.targetRotation}, display=${it.window.decorView.display.rotation}, " +
                            "zoom=${viewModel.engine.scene.getZoomLevel()}, previousZoom=$previousZoom"
                        rotated = it.resources.configuration.orientation == expected &&
                            originalPreview.targetRotation == it.window.decorView.display.rotation &&
                            viewModel.engine.scene.getZoomLevel() != previousZoom
                    }
                    rotated
                }
                InstrumentationRegistry.getInstrumentation().waitForIdleSync()
                scenario.onActivity {
                    assertSame(originalActivity, it)
                    assertEquals(Lifecycle.State.RESUMED, it.lifecycle.currentState)
                    assertEquals(0, stops)
                    assertSame(originalPreview, viewModel.cameraState.previewUseCase)
                    assertEquals(it.window.decorView.display.rotation, originalPreview.targetRotation)
                    val zoom = viewModel.engine.scene.getZoomLevel()
                    assertNotEquals(previousZoom, zoom)
                    previousZoom = zoom
                }
            }
        }
    }

    private fun waitUntil(
        timeoutMillis: Long,
        message: () -> String = { "" },
        condition: () -> Boolean,
    ) {
        val deadline = SystemClock.uptimeMillis() + timeoutMillis
        while (!condition()) {
            assertTrue("Condition not satisfied within $timeoutMillis ms: ${message()}", SystemClock.uptimeMillis() < deadline)
            SystemClock.sleep(50)
        }
    }
}
