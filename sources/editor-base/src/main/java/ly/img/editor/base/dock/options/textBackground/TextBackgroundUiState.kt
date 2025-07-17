package ly.img.editor.base.dock.options.textBackground

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import ly.img.editor.base.R
import ly.img.editor.base.engine.DesignBlockWithProperties
import ly.img.editor.base.engine.Property
import ly.img.editor.base.engine.PropertyValue
import ly.img.editor.base.engine.PropertyValueCombineStrategy
import ly.img.editor.base.engine.PropertyValueType
import ly.img.editor.base.engine.combineWithValue
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
            return DesignBlockWithProperties(
                designBlock = designBlock,
                objectType = DesignBlockType.Text,
                properties = buildList {
                    val colorProperty = Property(
                        titleRes = R.string.ly_img_editor_text_background_color,
                        key = "backgroundColor/color",
                        valueType = PropertyValueType.Color(
                            enabledPropertyKey = "backgroundColor/enabled",
                            colorPalette = colorPalette,
                        ),
                    ).combineWithValue(engine, designBlock)
                    add(colorProperty)
                    if (colorProperty.value is PropertyValue.Color && colorProperty.value.value != null) {
                        Property(
                            titleRes = R.string.ly_img_editor_text_vertical_padding,
                            keys = listOf(
                                "backgroundColor/paddingTop",
                                "backgroundColor/paddingBottom",
                            ),
                            valueType = PropertyValueType.Float(range = 0F..maxVerticalPadding),
                            combineStrategy = PropertyValueCombineStrategy.Min,
                        ).combineWithValue(engine, designBlock).let(::add)
                        Property(
                            titleRes = R.string.ly_img_editor_text_horizontal_padding,
                            keys = listOf(
                                "backgroundColor/paddingLeft",
                                "backgroundColor/paddingRight",
                            ),
                            valueType = PropertyValueType.Float(range = 0F..maxHorizontalPadding),
                            combineStrategy = PropertyValueCombineStrategy.Min,
                        ).combineWithValue(engine, designBlock).let(::add)
                        Property(
                            titleRes = R.string.ly_img_editor_text_corner_radius,
                            key = "backgroundColor/cornerRadius",
                            valueType = PropertyValueType.Float(
                                range = 0F..maxCornerRadius,
                            ),
                        ).combineWithValue(engine, designBlock).let(::add)
                    }
                },
            ).let { TextBackgroundUiState(it) }
        }
    }
}
