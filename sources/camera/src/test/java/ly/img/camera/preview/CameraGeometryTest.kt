package ly.img.camera.preview

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class CameraGeometryTest {
    private val canvas = Size(1080f, 1920f)
    private val topPane = Rect(0f, 0f, 1080f, 960f)
    private val bottomPane = Rect(0f, 960f, 1080f, 1920f)

    @Test
    fun landscapeCaptureTurnsStackedPanesIntoSideBySidePanes() {
        assertEquals(Rect(0f, 0f, 960f, 1080f), topPane.toCaptureOrientation(90, canvas))
        assertEquals(Rect(960f, 0f, 1920f, 1080f), bottomPane.toCaptureOrientation(90, canvas))
        assertEquals(Rect(960f, 0f, 1920f, 1080f), topPane.toCaptureOrientation(270, canvas))
        assertEquals(Rect(0f, 0f, 960f, 1080f), bottomPane.toCaptureOrientation(270, canvas))
    }

    @Test
    fun upsideDownCaptureSwapsPanePositions() {
        assertEquals(bottomPane, topPane.toCaptureOrientation(180, canvas))
        assertEquals(topPane, bottomPane.toCaptureOrientation(180, canvas))
    }

    @Test
    fun portraitCapturePreservesPanePositions() {
        assertEquals(topPane, topPane.toCaptureOrientation(0, canvas))
        assertEquals(bottomPane, bottomPane.toCaptureOrientation(0, canvas))
    }

    @Test
    fun landscapeCaptureTurnsSideBySidePanesIntoStackedPanes() {
        val left = Rect(0f, 0f, 540f, 1920f)
        val right = Rect(540f, 0f, 1080f, 1920f)
        assertEquals(Rect(0f, 540f, 1920f, 1080f), left.toCaptureOrientation(90, canvas))
        assertEquals(Rect(0f, 0f, 1920f, 540f), right.toCaptureOrientation(90, canvas))
        assertEquals(Rect(0f, 0f, 1920f, 540f), left.toCaptureOrientation(270, canvas))
        assertEquals(Rect(0f, 540f, 1920f, 1080f), right.toCaptureOrientation(270, canvas))
    }

    @Test
    fun fullFrameMatchesUprightCaptureDimensions() {
        val fullFrame = Rect(0f, 0f, canvas.width, canvas.height)
        val landscape = Rect(0f, 0f, canvas.height, canvas.width)
        assertEquals(fullFrame, fullFrame.toCaptureOrientation(0, canvas))
        assertEquals(landscape, fullFrame.toCaptureOrientation(90, canvas))
        assertEquals(fullFrame, fullFrame.toCaptureOrientation(180, canvas))
        assertEquals(landscape, fullFrame.toCaptureOrientation(270, canvas))
    }

    @Test
    fun rotationRoundTripPreservesAnOffCenterCrop() {
        val crop = Rect(120f, 250f, 800f, 1400f)
        val landscapeCanvas = Size(canvas.height, canvas.width)
        assertEquals(crop, crop.toCaptureOrientation(90, canvas).toCaptureOrientation(270, landscapeCanvas))
        assertEquals(crop, crop.toCaptureOrientation(180, canvas).toCaptureOrientation(180, canvas))
        assertEquals(crop, crop.toCaptureOrientation(270, canvas).toCaptureOrientation(90, landscapeCanvas))
    }

    @Test(expected = IllegalStateException::class)
    fun rejectsNonQuarterTurnOrientation() {
        topPane.toCaptureOrientation(45, canvas)
    }
}
