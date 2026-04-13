package ly.img.editor.featureFlag

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
internal fun FeatureFlagsScreen() {
    val viewModel = viewModel<FeatureFlagsViewModel>()
    val state by viewModel.state.collectAsState()
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
    ) {
        items(
            items = state.features,
            key = { it.id },
        ) {
            FeatureFlagItem(it, viewModel::setFeatureEnabled)
            Divider()
        }
    }
}

@Composable
private fun FeatureFlagItem(
    feature: FeatureFlagsState.Feature,
    onCheckedChange: (FeatureFlagsState.Feature, Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1F)) {
            Text(
                text = feature.title,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                modifier = Modifier.padding(top = 4.dp),
                text = feature.description,
                style = MaterialTheme.typography.bodySmall,
            )
        }
        Switch(
            modifier = Modifier.padding(start = 16.dp),
            checked = feature.checked,
            onCheckedChange = { onCheckedChange(feature, it) },
        )
    }
}
