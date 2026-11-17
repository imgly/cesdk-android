package ly.img.editor.base.dock.options.textBackground

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import ly.img.editor.base.engine.DesignBlockWithProperties
import ly.img.editor.base.engine.Property
import ly.img.editor.base.engine.PropertyControlKind
import ly.img.editor.base.engine.PropertyText
import ly.img.editor.base.engine.PropertyValue
import ly.img.editor.base.engine.PropertyValueCombineStrategy
import ly.img.editor.base.engine.PropertyValueType
import ly.img.editor.base.engine.abbreviation
import ly.img.editor.base.engine.combineWithValue
import ly.img.editor.base.engine.lengthDecimals
import ly.img.editor.core.R
import ly.img.engine.DesignBlock
import ly.img.engine.DesignBlockType
import ly.img.engine.Engine
import kotlin.math.max

@Immutable
data class TextBackgroundUiState(
    val designBlockWithProperties: DesignBlockWithProperties,
) {
    companion object {
        fun create(
            designBlock: DesignBlock,
            engine: Engine,
            colorPalette: List<Color>,
        ): TextBackgroundUiState {
            val frameWidth = engine.block.getFrameWidth(designBlock)
            val frameHeight = engine.block.getFrameHeight(designBlock)
            val maxVerticalPadding = listOf(
                engine.block.getFloat(designBlock, "backgroundColor/paddingTop"),
                engine.block.getFloat(designBlock, "backgroundColor/paddingBottom"),
                frameHeight,
            ).max()
            val maxHorizontalPadding = listOf(
                engine.block.getFloat(designBlock, "backgroundColor/paddingLeft"),
                engine.block.getFloat(designBlock, "backgroundColor/paddingRight"),
                frameWidth,
            ).max()
            val maxCornerRadius = run {
                if (frameWidth > frameHeight) {
                    frameHeight / 2 + maxVerticalPadding
                } else {
                    frameWidth / 2 + maxHorizontalPadding
                }.let { max(it, engine.block.getFloat(designBlock, "backgroundColor/cornerRadius")) }
            }
            // Padding and corner radius are lengths in the scene's design unit.
            val designUnit = engine.scene.getDesignUnit()
            // The step stays 0.1 in every unit.
            val lengthStepper = PropertyControlKind.Stepper(
                unit = designUnit.abbreviation,
                decimals = designUnit.lengthDecimals,
            )
            return DesignBlockWithProperties(
                designBlock = designBlock,
                objectType = DesignBlockType.Text,
                properties = buildList {
                    val colorProperty = Property(
                        title = PropertyText.Resource(R.string.ly_img_editor_sheet_text_background_label_color),
                        key = "backgroundColor/color",
                        valueType = PropertyValueType.Color(
                            enabledPropertyKey = "backgroundColor/enabled",
                            colorPalette = colorPalette,
                        ),
                    ).combineWithValue(engine, designBlock)
                    add(colorProperty)
                    if (colorProperty.value is PropertyValue.Color && colorProperty.value.value != null) {
                        Property(
                            title = PropertyText.Resource(R.string.ly_img_editor_sheet_text_background_label_vertical_padding),
                            keys = listOf(
                                "backgroundColor/paddingTop",
                                "backgroundColor/paddingBottom",
                            ),
                            valueType = PropertyValueType.Float(range = 0F..maxVerticalPadding),
                            combineStrategy = PropertyValueCombineStrategy.Min,
                            controlKind = lengthStepper,
                        ).combineWithValue(engine, designBlock).let(::add)
                        Property(
                            title = PropertyText.Resource(R.string.ly_img_editor_sheet_text_background_label_horizontal_padding),
                            keys = listOf(
                                "backgroundColor/paddingLeft",
                                "backgroundColor/paddingRight",
                            ),
                            valueType = PropertyValueType.Float(range = 0F..maxHorizontalPadding),
                            combineStrategy = PropertyValueCombineStrategy.Min,
                            controlKind = lengthStepper,
                        ).combineWithValue(engine, designBlock).let(::add)
                        Property(
                            title = PropertyText.Resource(R.string.ly_img_editor_sheet_text_background_label_round_corners),
                            key = "backgroundColor/cornerRadius",
                            valueType = PropertyValueType.Float(
                                range = 0F..maxCornerRadius,
                            ),
                            controlKind = lengthStepper,
                        ).combineWithValue(engine, designBlock).let(::add)
                    }
                },
            ).let { TextBackgroundUiState(it) }
        }
    }
}
