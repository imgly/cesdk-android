package ly.img.editor.base.dock.options

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import ly.img.editor.base.dock.options.adjustment.AdjustmentOptionsSheet
import ly.img.editor.base.dock.options.adjustment.AdjustmentUiState
import ly.img.editor.base.dock.options.format.FormatOptionsSheet
import ly.img.editor.base.dock.options.format.FormatUiState
import ly.img.editor.base.dock.options.format.SizeModeUi
import ly.img.editor.base.dock.options.format.VerticalAlignment
import ly.img.editor.base.dock.options.layer.LayerOptionsSheet
import ly.img.editor.base.dock.options.layer.LayerUiState
import ly.img.editor.base.dock.options.shapeoptions.ShapeOptionsSheet
import ly.img.editor.base.dock.options.shapeoptions.StarShapeOptionsUiState
import ly.img.editor.base.dock.options.volume.VolumeSheet
import ly.img.editor.base.dock.options.volume.VolumeUiState
import ly.img.editor.base.engine.DesignBlockWithProperties
import ly.img.editor.base.engine.PropertyAndValue
import ly.img.editor.base.engine.PropertyControlKind
import ly.img.editor.base.engine.PropertyValue
import ly.img.editor.base.engine.getProperties
import ly.img.editor.base.ui.BlockEvent
import ly.img.editor.core.event.EditorEvent
import ly.img.editor.core.library.LibraryCategory
import ly.img.engine.DesignBlockType
import ly.img.engine.EffectType
import ly.img.engine.FontStyle
import ly.img.engine.FontUnit
import ly.img.engine.FontWeight
import ly.img.engine.HorizontalAlignment
import ly.img.engine.ListStyle
import ly.img.engine.ScriptStyle
import ly.img.engine.TextCase
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Each numeric control is one TalkBack element named by its title, so these tests find it by that name. */
@RunWith(AndroidJUnit4::class)
class NumericControlsSheetsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun hasState(state: String) = SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, state)

    /** The positions between the ends, so 99 is a 1% step on a 0–100 readout. */
    private fun hasSteps(steps: Int) = SemanticsMatcher("has $steps steps") { node ->
        node.config.getOrNull(SemanticsProperties.ProgressBarRangeInfo)?.steps == steps
    }

    private val isStepper = SemanticsMatcher("is a stepper") { node ->
        node.config.getOrNull(SemanticsActions.CustomActions)?.size == 2
    }

    @Test
    fun fontSizeIsAStepperWithItsUnitInTheField() {
        compose.setContent { FormatOptionsSheet(uiState = formatUiState(fontSize = 16f), onEvent = {}) }

        compose.onNodeWithContentDescription("Font Size").assert(isStepper).assert(hasState("16 pt"))
    }

    /** The engine has no maximum font size, so a step up from a large size is kept. */
    @Test
    fun fontSizeHasNoMaximum() {
        val events = mutableListOf<EditorEvent>()
        compose.setContent { FormatOptionsSheet(uiState = formatUiState(fontSize = 200f), onEvent = { events += it }) }

        val increase = compose.onNodeWithContentDescription("Font Size").fetchSemanticsNode()
            .config[SemanticsActions.CustomActions].first()
        compose.runOnUiThread { increase.action() }

        val sizes = events.filterIsInstance<BlockEvent.OnChangeFontSize>().map { it.fontSize }
        assertEquals(1, sizes.size)
        assertEquals(200.1f, sizes.single(), 0.001f)
    }

    @Test
    fun starPointsIsAStepperAndInnerDiameterReadsNoFalsePercentage() {
        compose.setContent {
            ShapeOptionsSheet(uiState = StarShapeOptionsUiState(points = 5f, innerDiameter = 0.1f), onEvent = {})
        }

        compose.onNodeWithContentDescription("Points").assert(isStepper).assert(hasState("5"))
        compose.onNodeWithContentDescription("Inner Diameter").assert(hasState("0.1"))
    }

    @Test
    fun theReadoutIsNotASecondTalkBackElement() {
        compose.setContent { LayerOptionsSheet(uiState = layerUiState(opacity = 1f), onEvent = {}) }

        compose.onNodeWithText("100").assertDoesNotExist()
    }

    @Test
    fun opacityReadsAPercentage() {
        compose.setContent { LayerOptionsSheet(uiState = layerUiState(opacity = 1f), onEvent = {}) }

        compose.onNodeWithContentDescription("Opacity").assert(hasState("100%"))
    }

    @Test
    fun opacityMovesInOnePercentSteps() {
        compose.setContent { LayerOptionsSheet(uiState = layerUiState(opacity = 1f), onEvent = {}) }

        compose.onNodeWithContentDescription("Opacity").assert(hasSteps(99))
    }

    @Test
    fun adjustmentsReadMinus100To100Percent() {
        compose.setContent { AdjustmentOptionsSheet(uiState = adjustmentUiState(brightness = -1f), onEvent = {}) }
        compose.onNodeWithContentDescription("Brightness").assert(hasState("-100%"))
    }

    @Test
    fun adjustmentsMoveInStepsOfOne() {
        compose.setContent { AdjustmentOptionsSheet(uiState = adjustmentUiState(brightness = -1f), onEvent = {}) }

        compose.onNodeWithContentDescription("Brightness").assert(hasSteps(199))
    }

    @Test
    fun everyAdjustmentSnapsAtZero() {
        val detents = EffectType.Adjustments.getProperties().map { (it.controlKind as PropertyControlKind.Slider).detents }

        assertEquals(List(12) { listOf(0f) }, detents)
    }

    @Test
    fun volumeReadsAPercentageToTalkBack() {
        compose.setContent { VolumeSheet(uiState = VolumeUiState(volume = 1f), onEvent = {}) }

        compose.onNodeWithContentDescription("Volume").assert(hasState("100%"))
    }

    private fun adjustmentUiState(brightness: Float) = AdjustmentUiState(
        DesignBlockWithProperties(
            designBlock = 1,
            objectType = DesignBlockType.Graphic,
            properties = EffectType.Adjustments.getProperties().map {
                val value = if (it.keys.first().endsWith("/brightness")) brightness else 0f
                PropertyAndValue(it, PropertyValue.Float(value))
            },
        ),
    )

    private fun layerUiState(opacity: Float) = LayerUiState(
        opacity = opacity,
        isMoveAllowed = false,
        isDuplicateAllowed = false,
        isDeleteAllowed = false,
        canBringForward = false,
        canSendBackward = false,
    )

    private fun formatUiState(fontSize: Float) = FormatUiState(
        libraryCategory = LibraryCategory.Text,
        fontFamily = "Roboto",
        fontFamilyWeight = FontWeight.NORMAL,
        fontFamilyStyle = FontStyle.NORMAL,
        canToggleBold = true,
        canToggleItalic = true,
        isBold = false,
        isItalic = false,
        isUnderline = false,
        isStrikethrough = false,
        casing = TextCase.NORMAL,
        listStyle = ListStyle.NONE,
        scriptStyle = ScriptStyle.NORMAL,
        horizontalAlignment = HorizontalAlignment.Left,
        effectiveHorizontalAlignment = HorizontalAlignment.Left,
        verticalAlignment = VerticalAlignment.Top,
        fontSize = fontSize,
        fontSizeUnit = FontUnit.POINT,
        letterSpacing = 0f,
        paragraphSpacing = 0f,
        lineHeight = 1f,
        isClipped = false,
        hasClippingOption = false,
        isTextOnPath = false,
        sizeMode = SizeModeUi.ABSOLUTE,
        isArrangeResizeAllowed = true,
        availableWeights = emptyList(),
        subFamily = "Regular",
        isSubFamilyMixed = false,
        isCaption = false,
    )
}
