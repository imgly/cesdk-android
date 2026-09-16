package ly.img.editor.base.engine

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.res.stringResource
import ly.img.editor.base.components.PropertyOption
import ly.img.editor.core.ui.engine.toComposeColor
import ly.img.editor.core.ui.engine.toRGBColor
import ly.img.engine.Asset
import ly.img.engine.AssetBooleanProperty
import ly.img.engine.AssetColorProperty
import ly.img.engine.AssetDoubleProperty
import ly.img.engine.AssetEnumProperty
import ly.img.engine.AssetFloatProperty
import ly.img.engine.AssetIntProperty
import ly.img.engine.AssetProperty
import ly.img.engine.AssetStringProperty
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

sealed interface PropertyText {
    val value: String
        @Composable get

    data class Raw(
        private val text: String,
    ) : PropertyText {
        override val value: String
            @Composable get() = text
    }

    data class Resource(
        @StringRes private val textRes: Int,
    ) : PropertyText {
        override val value: String
            @Composable get() = stringResource(textRes)
    }
}

data class Property(
    val title: PropertyText,
    val keys: List<String>,
    val valueType: PropertyValueType,
    val combineStrategy: PropertyValueCombineStrategy,
    val assetData: PropertyAssetData? = null,
) {
    constructor(
        title: PropertyText,
        key: String,
        valueType: PropertyValueType,
        assetData: PropertyAssetData? = null,
    ) : this(
        title = title,
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

/**
 * Builds PropertyAndValue list purely based on [AssetProperty] data.
 */
fun List<AssetProperty>.toPropertyAndValueList(
    engine: Engine,
    sourceId: String,
    asset: Asset,
): List<PropertyAndValue> = map { it.toProperty(sourceId, asset) }
    .zip(this)
    .map { (property, assetProperty) ->
        PropertyAndValue(
            property = property,
            value = assetProperty.getValue(engine),
        )
    }

/**
 * Builds list of [PropertyAndValue]s based on the list of [Property]s and current engine state.
 */
fun List<Property>.combineWithValues(
    engine: Engine,
    designBlock: DesignBlock,
): List<PropertyAndValue> = this.map {
    PropertyAndValue(
        property = it,
        value = it.getValue(engine, designBlock),
    )
}

/**
 * Builds list of [PropertyAndValue]s based on the list of [Property]s and current engine state, guided by [override].
 * This is a workaround legacy and should be avoided at all cost!
 *
 * This overload is used when List<Property>.combineWithValues is not sufficient to get the [PropertyValue] pairings.
 * For instance, "animation/slide/direction" is represented as an enum property in [override],
 * however the underlying type is a float. Calling setEnum or getEnum on the property will cause crash!
 *
 * We read the property value correctly thanks to the [AssetProperty] type in [override], and we write the value correctly
 * thanks to the [ly.img.engine.AssetApi.applyAssetSourceProperty] in BlockEventsHandler.
 *
 * This means that AssetProperty can be an enum, but underlying property can be a Float.
 *
 * That is why for animations we cannot call the very logical:
 *
 * ```
 * kotlin
 * animationType.getAvailableProperties().combineWithValues(engine, animation)
 * ```
 *
 */
fun List<Property>.combineWithValues(
    engine: Engine,
    sourceId: String,
    asset: Asset,
    override: List<AssetProperty>,
): List<PropertyAndValue> = override.mapNotNull { assetProperty ->
    val property = firstOrNull { it.keys.first().endsWith(assetProperty.property) } ?: return@mapNotNull null
    PropertyAndValue(
        property = property.getUpdatedProperty(sourceId, asset, assetProperty),
        value = assetProperty.getValue(engine),
    )
}

/**
 * Builds [PropertyAndValue] based on the [Property] and current engine state.
 */
fun Property.combineWithValue(
    engine: Engine,
    designBlock: DesignBlock,
): PropertyAndValue = PropertyAndValue(
    property = this,
    value = this.getValue(engine, designBlock),
)

private fun Property.getUpdatedProperty(
    sourceId: String,
    asset: Asset,
    assetProperty: AssetProperty,
) = when (this.valueType) {
    is PropertyValueType.Int -> (assetProperty as? AssetIntProperty)?.run {
        PropertyValueType.Int(min..max, step)
    }
    is PropertyValueType.Float -> (assetProperty as? AssetFloatProperty)?.run {
        PropertyValueType.Float(min..max, step)
    }
    is PropertyValueType.Double -> (assetProperty as? AssetDoubleProperty)?.run {
        PropertyValueType.Double(min..max, step)
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

private fun AssetProperty.toProperty(
    sourceId: String,
    asset: Asset,
) = Property(
    title = AssetPropertyLabels.title(sourceId, property),
    keys = listOf(property),
    valueType = when (this) {
        is AssetIntProperty -> PropertyValueType.Int(min..max, step)
        is AssetFloatProperty -> PropertyValueType.Float(min..max, step)
        is AssetDoubleProperty -> PropertyValueType.Double(min..max, step)
        is AssetBooleanProperty -> PropertyValueType.Boolean
        is AssetColorProperty -> PropertyValueType.Color()
        is AssetStringProperty -> PropertyValueType.String
        is AssetEnumProperty -> PropertyValueType.StringEnum(
            options = options.map {
                PropertyOption(
                    text = AssetPropertyLabels.option(sourceId, it),
                    value = it,
                )
            },
        )
    },
    combineStrategy = PropertyValueCombineStrategy.First,
    assetData = PropertyAssetData(
        sourceId = sourceId,
        asset = asset,
        assetProperty = this,
    ),
)

private fun AssetProperty.getValue(engine: Engine): PropertyValue = when (this) {
    is AssetIntProperty -> PropertyValue.Int(value)
    is AssetFloatProperty -> PropertyValue.Float(value)
    is AssetDoubleProperty -> PropertyValue.Double(value)
    is AssetBooleanProperty -> PropertyValue.Boolean(value)
    is AssetColorProperty -> PropertyValue.Color(value = value.toRGBColor(engine).toComposeColor())
    is AssetStringProperty -> PropertyValue.String(value)
    is AssetEnumProperty -> PropertyValue.Enum(value)
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
