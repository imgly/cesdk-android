package ly.img.editor.base.engine

import androidx.annotation.StringRes
import androidx.compose.runtime.Immutable
import ly.img.editor.base.components.PropertyOption
import ly.img.editor.core.ui.engine.toComposeColor
import ly.img.editor.core.ui.engine.toRGBColor
import ly.img.engine.Asset
import ly.img.engine.AssetBooleanProperty
import ly.img.engine.AssetColor
import ly.img.engine.AssetColorProperty
import ly.img.engine.AssetDoubleProperty
import ly.img.engine.AssetEnumProperty
import ly.img.engine.AssetFloatProperty
import ly.img.engine.AssetIntProperty
import ly.img.engine.AssetProperty
import ly.img.engine.AssetStringProperty
import ly.img.engine.Color
import ly.img.engine.DesignBlock
import ly.img.engine.Engine
import ly.img.engine.ObjectType

@Immutable
data class DesignBlockWithProperties(
    val designBlock: DesignBlock,
    val objectType: ObjectType,
    val properties: List<PropertyAndValue>,
    val asset: Asset? = null,
)

data class PropertyAndValue(
    val property: Property,
    val value: PropertyValue,
)

data class Property(
    @StringRes val titleRes: Int,
    val keys: List<String>,
    val valueType: PropertyValueType,
    val combineStrategy: PropertyValueCombineStrategy,
    val assetData: PropertyAssetData? = null,
) {
    constructor(
        titleRes: Int,
        key: String,
        valueType: PropertyValueType,
        assetData: PropertyAssetData? = null,
    ) : this(
        titleRes = titleRes,
        keys = listOf(key),
        valueType = valueType,
        combineStrategy = PropertyValueCombineStrategy.First,
        assetData = assetData,
    )
}

data class PropertyAssetData(
    val sourceId: String,
    val asset: Asset,
    val assetProperty: AssetProperty,
)

sealed interface PropertyValue {
    fun compare(other: PropertyValue): kotlin.Int?

    data class Int(
        val value: kotlin.Int,
    ) : PropertyValue {
        override fun compare(other: PropertyValue): kotlin.Int? = (other as? Int)?.value?.let { value.compareTo(it) }
    }

    data class Double(
        val value: kotlin.Double,
    ) : PropertyValue {
        override fun compare(other: PropertyValue): kotlin.Int? = (other as? Double)?.value?.let { value.compareTo(it) }
    }

    data class Float(
        val value: kotlin.Float,
    ) : PropertyValue {
        override fun compare(other: PropertyValue): kotlin.Int? = (other as? Float)?.value?.let { value.compareTo(it) }
    }

    data class Boolean(
        val value: kotlin.Boolean,
    ) : PropertyValue {
        override fun compare(other: PropertyValue): kotlin.Int? = (other as? Boolean)?.value?.let { value.compareTo(it) }
    }

    data class Color(
        val value: androidx.compose.ui.graphics.Color?,
    ) : PropertyValue {
        override fun compare(other: PropertyValue): kotlin.Int? = null
    }

    data class String(
        val value: kotlin.String,
    ) : PropertyValue {
        override fun compare(other: PropertyValue): kotlin.Int? = null
    }

    data class Enum(
        val value: kotlin.String,
    ) : PropertyValue {
        override fun compare(other: PropertyValue): kotlin.Int? = null
    }
}

sealed interface PropertyValueCombineStrategy {
    fun combine(
        property: Property,
        values: List<PropertyValue>,
    ): PropertyValue

    data object First : PropertyValueCombineStrategy {
        override fun combine(
            property: Property,
            values: List<PropertyValue>,
        ) = values.first()
    }

    data object Min : PropertyValueCombineStrategy {
        override fun combine(
            property: Property,
            values: List<PropertyValue>,
        ) = values.minWith { a, b -> a.compare(b) ?: 0 }
    }
}

sealed interface PropertyValueType {
    @Immutable
    data class Int(
        val range: IntRange = 0..100,
        val step: kotlin.Int = 1,
    ) : PropertyValueType

    @Immutable
    data class Float(
        val range: ClosedFloatingPointRange<kotlin.Float> = 0.0F..1.0F,
        val step: kotlin.Float = 0.1F,
    ) : PropertyValueType

    @Immutable
    data class Double(
        val range: ClosedFloatingPointRange<kotlin.Double> = 0.0..1.0,
        val step: kotlin.Double = 0.1,
    ) : PropertyValueType

    @Immutable
    data object Boolean : PropertyValueType

    @Immutable
    data class Color(
        val enabledPropertyKey: kotlin.String? = null,
        val colorPalette: List<androidx.compose.ui.graphics.Color>? = null,
    ) : PropertyValueType

    @Immutable
    data object String : PropertyValueType

    @Immutable
    data class StringEnum(
        val options: List<PropertyOption<kotlin.String>>,
    ) : PropertyValueType
}

fun List<AssetProperty>.toPropertyAndValueList(
    engine: Engine,
    sourceId: String,
    asset: Asset,
    availableProperties: List<Property>,
): List<PropertyAndValue> = this.mapNotNull { assetProperty ->
    val property = availableProperties
        .firstOrNull { it.keys.first().endsWith(assetProperty.property) } ?: return@mapNotNull null
    PropertyAndValue(
        property = property.getUpdatedProperty(sourceId, asset, assetProperty),
        value = assetProperty.getValue(engine),
    )
}

private fun Property.getUpdatedProperty(
    sourceId: String,
    asset: Asset,
    assetProperty: AssetProperty,
) = when (this.valueType) {
    is PropertyValueType.Int -> (assetProperty as? AssetIntProperty)?.run {
        PropertyValueType.Int(min..max)
    }
    is PropertyValueType.Float -> (assetProperty as? AssetFloatProperty)?.run {
        PropertyValueType.Float(min..max)
    }
    is PropertyValueType.Double -> (assetProperty as? AssetDoubleProperty)?.run {
        PropertyValueType.Double(min..max)
    }
    is PropertyValueType.StringEnum -> (assetProperty as? AssetEnumProperty)?.run {
        PropertyValueType.StringEnum(this@getUpdatedProperty.valueType.options)
    }
    else -> valueType
}.let { updatedValueType ->
    copy(
        valueType = updatedValueType ?: valueType,
        assetData = PropertyAssetData(
            sourceId = sourceId,
            asset = asset,
            assetProperty = assetProperty,
        ),
    )
}

private fun AssetProperty.getValue(engine: Engine): PropertyValue = when (this) {
    is AssetIntProperty -> PropertyValue.Int(value)
    is AssetFloatProperty -> PropertyValue.Float(value)
    is AssetDoubleProperty -> PropertyValue.Double(value)
    is AssetBooleanProperty -> PropertyValue.Boolean(value)
    is AssetColorProperty -> PropertyValue.Color(value = value.toComposeColor(engine))
    is AssetStringProperty -> PropertyValue.String(value)
    is AssetEnumProperty -> PropertyValue.Enum(value)
}

private fun AssetColor.toComposeColor(engine: Engine): androidx.compose.ui.graphics.Color = when (this) {
    is AssetColor.RGB -> {
        Color.fromRGBA(r = r, g = g, b = b).toComposeColor()
    }
    is AssetColor.CMYK -> {
        Color.fromCMYK(c = c, m = m, y = y, k = k).toRGBColor(engine).toComposeColor()
    }
    is AssetColor.SpotColor -> representation.toComposeColor(engine)
}

fun Property.combineWithValue(
    engine: Engine,
    designBlock: DesignBlock,
): PropertyAndValue = PropertyAndValue(
    property = this,
    value = this.getValue(engine, designBlock),
)

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
    is PropertyValueType.Int -> keys.map {
        PropertyValue.Int(engine.block.getInt(designBlock, it))
    }
    is PropertyValueType.Float -> keys.map {
        PropertyValue.Float(engine.block.getFloat(designBlock, it))
    }
    is PropertyValueType.Double -> keys.map {
        PropertyValue.Double(engine.block.getDouble(designBlock, it))
    }
    is PropertyValueType.Boolean -> keys.map {
        PropertyValue.Boolean(engine.block.getBoolean(designBlock, it))
    }
    is PropertyValueType.Color -> keys.map {
        if (valueType.enabledPropertyKey != null && engine.block.getBoolean(designBlock, valueType.enabledPropertyKey).not()) {
            PropertyValue.Color(null)
        } else {
            PropertyValue.Color(engine.block.getColor(designBlock, it).toRGBColor(engine).toComposeColor())
        }
    }
    is PropertyValueType.String -> keys.map {
        PropertyValue.String(engine.block.getString(designBlock, it))
    }
    is PropertyValueType.StringEnum -> keys.map {
        val engineValue = engine.block.getEnum(designBlock, it)
        val value = valueType.options
            .firstOrNull { it.value == engineValue }
            ?: valueType.options[0]
        PropertyValue.Enum(value.value)
    }
}.let { combineStrategy.combine(this, it) }
