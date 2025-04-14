package ly.img.editor.base.engine

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import ly.img.editor.base.components.PropertyOption
import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import ly.img.engine.ObjectType
import ly.img.engine.RGBAColor
import kotlin.math.abs

@Immutable
data class DesignBlockWithProperties(
    val designBlock: DesignBlock,
    val objectType: ObjectType,
    val properties: List<PropertyAndValue>,
)

data class PropertyAndValue(
    val property: Property,
    val value: PropertyValue,
)

data class Property(
    @StringRes val titleRes: Int,
    val key: String,
    val valueType: PropertyValueType,
)

sealed interface PropertyValue {
    data class Int(
        val value: kotlin.Int,
    ) : PropertyValue

    data class Double(
        val value: kotlin.Double,
    ) : PropertyValue

    data class Float(
        val value: kotlin.Float,
    ) : PropertyValue

    data class Color(
        val value: androidx.compose.ui.graphics.Color,
    ) : PropertyValue

    data class Boolean(
        val value: kotlin.Boolean,
    ) : PropertyValue

    data class Enum(
        val value: String,
    ) : PropertyValue
}

sealed interface PropertyValueType {
    @Immutable
    data class Int(
        val range: IntRange,
        val step: kotlin.Int = 1,
    ) : PropertyValueType

    @Immutable
    data class Float(
        val range: ClosedFloatingPointRange<kotlin.Float>,
        val step: kotlin.Float = 0.1F,
    ) : PropertyValueType

    @Immutable
    data class Double(
        val range: ClosedFloatingPointRange<kotlin.Double>,
        val step: kotlin.Double = 0.1,
    ) : PropertyValueType

    @Immutable
    data object Color : PropertyValueType

    @Immutable
    data object Boolean : PropertyValueType

    @Immutable
    data class StringEnum(
        val options: List<PropertyOption<String>>,
    ) : PropertyValueType

    @Immutable
    data class FloatEnum(
        val options: List<PropertyOption<kotlin.Float>>,
    ) : PropertyValueType
}

fun List<Property>.combineWithValues(
    engine: Engine,
    designBlock: DesignBlock,
): List<PropertyAndValue> = this.map {
    PropertyAndValue(
        property = it,
        value = it.getValue(engine, designBlock),
    )
}

private fun Property.getValue(
    engine: Engine,
    designBlock: DesignBlock,
): PropertyValue = when (valueType) {
    is PropertyValueType.Int -> {
        PropertyValue.Int(engine.block.getInt(designBlock, key))
    }
    is PropertyValueType.Float -> {
        PropertyValue.Float(engine.block.getFloat(designBlock, key))
    }
    is PropertyValueType.Double -> {
        PropertyValue.Double(engine.block.getDouble(designBlock, key))
    }
    is PropertyValueType.Boolean -> {
        PropertyValue.Boolean(engine.block.getBoolean(designBlock, key))
    }
    is PropertyValueType.Color -> {
        PropertyValue.Color((engine.block.getColor(designBlock, key) as RGBAColor).toComposeColor())
    }
    is PropertyValueType.StringEnum -> {
        val engineValue = engine.block.getEnum(designBlock, key)
        val value = valueType.options
            .firstOrNull { it.value == engineValue }
            ?: valueType.options[0]
        PropertyValue.Enum(value.value)
    }
    is PropertyValueType.FloatEnum -> {
        val engineValue = engine.block.getFloat(designBlock, key)
        val value = valueType.options
            .firstOrNull { abs(it.value - engineValue) < 0.1 }
            ?: valueType.options[0]
        PropertyValue.Float(value.value)
    }
}
