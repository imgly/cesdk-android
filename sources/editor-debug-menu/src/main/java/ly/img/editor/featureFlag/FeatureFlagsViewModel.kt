package ly.img.editor.featureFlag

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import ly.img.editor.featureFlag.flags.IMGLYCameraFeature
import ly.img.editor.featureFlag.flags.RemoteAssetsFeature
import ly.img.editor.featureFlag.flags.SystemGalleryFeature

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "feature_flags")

internal class FeatureFlagsViewModel(
    applicationContext: Context,
) : ViewModel() {
    private val _state = MutableStateFlow(FeatureFlagsState())
    val state: StateFlow<FeatureFlagsState> = _state

    private val dataStore: DataStore<Preferences> = applicationContext.dataStore

    /**
     * All new features should be registered here
     */
    private val allFeatures = listOf(
        IMGLYCameraFeature,
        RemoteAssetsFeature,
        SystemGalleryFeature,
    )

    init {
        viewModelScope.launch {
            val mappedFeatures = withContext(Dispatchers.IO) {
                val preferences = dataStore.data.first()
                allFeatures.map { feature ->
                    FeatureFlagsState.Feature(
                        id = feature.id,
                        title = feature.title,
                        description = feature.description,
                        checked = preferences[booleanPreferencesKey(feature.id)] ?: feature.enabled,
                    )
                }
            }
            mappedFeatures.indices.forEach {
                allFeatures[it].overrideEnabled = mappedFeatures[it].checked
            }
            _state.update { it.copy(features = mappedFeatures) }
        }
    }

    fun setFeatureEnabled(
        feature: FeatureFlagsState.Feature,
        enabled: Boolean,
    ) {
        allFeatures.firstOrNull { it.id == feature.id }?.overrideEnabled = enabled
        _state.update { state ->
            state.copy(
                features = state.features.map {
                    if (it.id == feature.id) it.copy(checked = enabled) else it
                },
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            dataStore.edit {
                it[booleanPreferencesKey(feature.id)] = enabled
            }
        }
    }
}
