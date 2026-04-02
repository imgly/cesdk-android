package ly.img.editor.featureFlag

import androidx.compose.runtime.Immutable

@Immutable
internal data class FeatureFlagsState(
    val features: List<Feature> = emptyList(),
) {
    data class Feature(
        val id: String,
        val title: String,
        val description: String,
        val checked: Boolean,
    )
}
