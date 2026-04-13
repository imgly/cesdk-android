package ly.img.editor

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import ly.img.editor.debug.menu.BottomSheetDialog
import ly.img.editor.debug.menu.R
import ly.img.editor.featureFlag.FeatureFlagsScreen
import ly.img.editor.featureFlag.FeatureFlagsViewModel
import ly.img.editor.version.details.VersionDetails

@Composable
fun DebugMenu(
    modifier: Modifier = Modifier,
    versionInfo: String,
    branchName: String,
    commitId: String,
) {
    var showBottomSheet by remember { mutableStateOf(false) }
    val applicationContext = LocalContext.current.applicationContext
    // Initialize FeatureFlagsViewModel so feature flags are synced from preferences
    viewModel<FeatureFlagsViewModel> { FeatureFlagsViewModel(applicationContext) }
    Card(
        modifier = modifier.fillMaxWidth(),
    ) {
        VersionDetails(
            modifier = Modifier.padding(top = 8.dp),
            versionInfo = versionInfo,
            branchName = branchName,
            commitId = commitId,
        )
        Button(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(bottom = 8.dp),
            onClick = { showBottomSheet = true },
        ) {
            Text(text = stringResource(R.string.ly_img_editor_debug_menu_feature_flags))
        }
    }
    if (showBottomSheet) {
        BottomSheetDialog(onDismissRequest = { showBottomSheet = false }) {
            FeatureFlagsScreen()
        }
    }
}
