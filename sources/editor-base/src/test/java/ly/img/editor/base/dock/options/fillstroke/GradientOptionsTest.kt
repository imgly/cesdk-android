package ly.img.editor.base.dock.options.fillstroke

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import ly.img.editor.core.component.data.LinearGradientFill
import ly.img.engine.GradientColorStop
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import ly.img.engine.Color as EngineColor

/** The gradient angle ruler reads −180° to 180°, the same as iOS and the Web editor. */
@RunWith(AndroidJUnit4::class)
class GradientOptionsTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun aBottomToTopGradientReadsMinus90() {
        compose.setContent {
            GradientOptions(
                gradient = LinearGradientFill(
                    startPointX = 0.5f,
                    startPointY = 1f,
                    endPointX = 0.5f,
                    endPointY = 0f,
                    colorStops = listOf(
                        GradientColorStop(stop = 0f, color = EngineColor.fromRGBA(r = 1f, g = 0f, b = 0f, a = 1f)),
                        GradientColorStop(stop = 1f, color = EngineColor.fromRGBA(r = 0f, g = 0f, b = 1f, a = 1f)),
                    ),
                ),
                colorPalette = listOf(Color.Red, Color.Blue),
                onColorSelected = { _, _ -> },
                onRotationChange = {},
                onChangeFinish = {},
                openColorPicker = {},
            )
        }

        compose.onNodeWithContentDescription("Gradient Angle")
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "-90°"))
    }
}
