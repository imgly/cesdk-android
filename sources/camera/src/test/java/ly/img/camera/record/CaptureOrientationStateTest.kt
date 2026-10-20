package ly.img.camera.record

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import ly.img.camera.preview.toCaptureOrientation
import org.junit.Assert.assertEquals
import org.junit.Test

class CaptureOrientationStateTest {
    @Test
    fun lockedRecordingKeepsItsCaptureRotationWhenTheDisplayRotates() {
        val state = CaptureOrientationState()
        state.update(90, 0)
        state.lock()
        for (displayRotation in listOf(1, 2, 3, 0)) {
            state.update(0, displayRotation)
            state.lock()
            assertEquals(90, state.outputOrientation)
            assertEquals(1, state.targetRotation)
            assertEquals((1 - displayRotation).mod(4) * 90, state.orientation)
        }
        state.update(0, 3)
        state.unlock()
        assertEquals(3, state.targetRotation)
        assertEquals(0, state.orientation)
    }

    @Test
    fun reactionPanesKeepTheFirstCanvasAcrossDisplayRotations() {
        val canvas = Size(1080f, 1920f)
        val source = Rect(0f, 0f, 1080f, 960f)
        val camera = Rect(0f, 960f, 1080f, 1920f)
        for (initialDisplay in 0..3) {
            for (initialOrientation in listOf(0, 90, 180, 270)) {
                val state = CaptureOrientationState()
                state.update(initialOrientation, initialDisplay)
                state.lock()
                val firstSource = source.toCaptureOrientation(state.outputOrientation, canvas)
                val firstCamera = camera.toCaptureOrientation(state.outputOrientation, canvas)
                val firstTarget = state.targetRotation
                for (display in 0..3) {
                    state.update(0, display)
                    state.lock()
                    assertEquals(firstSource, source.toCaptureOrientation(state.outputOrientation, canvas))
                    assertEquals(firstCamera, camera.toCaptureOrientation(state.outputOrientation, canvas))
                    assertEquals(firstTarget, state.targetRotation)
                }
                state.unlock()
                assertEquals(0, state.outputOrientation)
                state.update(270, 1)
                state.lock()
                assertEquals(270, state.outputOrientation)
            }
        }
    }

    @Test
    fun captureRotationAccountsForTheDisplaysNaturalOrientation() {
        val state = CaptureOrientationState()
        val expectedRotations = listOf(
            listOf(0, 1, 2, 3),
            listOf(1, 2, 3, 0),
            listOf(2, 3, 0, 1),
            listOf(3, 0, 1, 2),
        )
        expectedRotations.forEachIndexed { displayRotation, rotations ->
            listOf(0, 90, 180, 270).forEachIndexed { index, orientation ->
                state.update(orientation, displayRotation)
                assertEquals(orientation, state.orientation)
                assertEquals(rotations[index], state.targetRotation)
            }
        }
    }

    @Test
    fun subsequentRecordingsKeepTheFirstOrientation() {
        val state = CaptureOrientationState()
        state.update(90, 0)
        state.lock()
        for (orientation in listOf(180, 270, 0)) {
            state.update(orientation, 0)
            state.lock()
            assertEquals(90, state.orientation)
            assertEquals(90, state.outputOrientation)
            assertEquals(1, state.targetRotation)
        }
    }

    @Test
    fun deletingAllRecordingsResumesLiveOrientationAndAllowsANewLock() {
        val state = CaptureOrientationState()
        state.update(90, 0)
        state.lock()
        state.update(270, 0)
        state.unlock()
        assertEquals(270, state.orientation)
        assertEquals(3, state.targetRotation)
        state.lock()
        state.update(0, 0)
        assertEquals(270, state.orientation)
        assertEquals(3, state.targetRotation)
    }

    @Test
    fun unlockedCapturesFollowEachNewOrientation() {
        val state = CaptureOrientationState()
        state.update(180, 1)
        assertEquals(180, state.orientation)
        assertEquals(3, state.targetRotation)
        state.update(270, 1)
        assertEquals(270, state.orientation)
        assertEquals(0, state.targetRotation)
    }
}
