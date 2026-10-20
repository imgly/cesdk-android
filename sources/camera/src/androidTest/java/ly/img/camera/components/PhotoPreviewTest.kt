package ly.img.camera.components

import android.graphics.Bitmap
import android.net.Uri
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.toPixelMap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.captureToImage
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import java.io.File

class PhotoPreviewTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun landscapePhotoFitsAndRotatesWithoutFading() = checkPhoto(width = 300, height = 200)

    @Test
    fun portraitPhotoFitsAndRotatesWithoutFading() = checkPhoto(width = 200, height = 300)

    private fun checkPhoto(
        width: Int,
        height: Int,
    ) {
        compose.activityRule.scenario.onActivity {
            it.actionBar?.hide()
            it.window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        }
        val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                bitmap.setPixel(x, y, colors[(if (y < height / 2) 0 else 2) + (if (x < width / 2) 0 else 1)].toArgb())
            }
        }
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("photo-preview", ".png", context.cacheDir)
        file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        bitmap.recycle()
        try {
            val orientation = mutableStateOf(0)
            compose.setContent {
                val rotation = animateFloatAsState(orientation.value.toFloat(), tween(200), label = "Photo rotation")
                PhotoPreview(
                    uri = Uri.fromFile(file),
                    orientation = orientation.value,
                    rotation = { rotation.value },
                    modifier = Modifier.size(240.dp, 400.dp).testTag("preview"),
                )
            }
            val preview = compose.onNodeWithTag("preview")
            compose.waitUntil(timeoutMillis = 5000) {
                val pixels = preview.captureToImage().toPixelMap()
                pixels[pixels.width / 4, pixels.height * 3 / 8].red > 0.9f
            }
            val quadrants = listOf(listOf(0, 1, 2, 3), listOf(2, 0, 3, 1), listOf(3, 2, 1, 0), listOf(1, 3, 0, 2))
            for (turn in 0..3) {
                compose.mainClock.autoAdvance = false
                compose.runOnIdle { orientation.value = turn * 90 }
                compose.mainClock.advanceTimeBy(150)
                val moving = preview.captureToImage().toPixelMap()
                var brightest = 0f
                for (y in 0 until moving.height step 8) {
                    for (x in 0 until moving.width step 8) {
                        val pixel = moving[x, y]
                        brightest = maxOf(brightest, pixel.red, pixel.green, pixel.blue)
                    }
                }
                assertTrue("Photo must stay opaque during rotation", brightest > 0.99f)
                compose.mainClock.autoAdvance = true
                compose.waitForIdle()
                val screenshot = preview.captureToImage()
                File(context.cacheDir, "photo-preview-${width}x$height-${turn * 90}.png").outputStream().use {
                    screenshot.asAndroidBitmap().compress(Bitmap.CompressFormat.PNG, 100, it)
                }
                val pixels = screenshot.toPixelMap()
                val rotatedWidth = if (turn % 2 == 0) width else height
                val rotatedHeight = if (turn % 2 == 0) height else width
                val scale = minOf(pixels.width.toFloat() / rotatedWidth, pixels.height.toFloat() / rotatedHeight)
                val imageWidth = rotatedWidth * scale
                val imageHeight = rotatedHeight * scale
                val left = (pixels.width - imageWidth) / 2
                val top = (pixels.height - imageHeight) / 2
                for (quadrant in 0..3) {
                    val x = (left + imageWidth * (if (quadrant % 2 == 0) 0.25f else 0.75f)).toInt()
                    val y = (top + imageHeight * (if (quadrant < 2) 0.25f else 0.75f)).toInt()
                    assertEquals(
                        "Photo quadrant at ${turn * 90} degrees",
                        colors[quadrants[turn][quadrant]].toArgb(),
                        pixels[x, y].toArgb(),
                    )
                }
                if (top > 2) {
                    assertEquals("Vertical letterbox", Color.Black.toArgb(), pixels[pixels.width / 2, (top / 2).toInt()].toArgb())
                }
                if (left > 2) {
                    assertEquals("Horizontal letterbox", Color.Black.toArgb(), pixels[(left / 2).toInt(), pixels.height / 2].toArgb())
                }
            }
        } finally {
            file.delete()
        }
    }
}
