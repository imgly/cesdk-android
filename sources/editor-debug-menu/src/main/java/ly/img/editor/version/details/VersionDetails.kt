package ly.img.editor.version.details

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import ly.img.editor.debug.menu.BottomSheetDialog
import ly.img.editor.debug.menu.R
import ly.img.editor.iconpack.Close
import ly.img.editor.iconpack.IconPack
import ly.img.editor.version.details.entity.Progress
import ly.img.editor.version.details.entity.Release

@Composable
fun VersionDetails(
    modifier: Modifier = Modifier,
    versionInfo: String,
    branchName: String,
    commitId: String,
) {
    Column(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (versionInfo.isNotEmpty()) {
            Text(
                text = versionInfo,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.titleSmall,
            )
        }
        if (branchName.isNotEmpty() && commitId.isNotEmpty()) {
            VersionUpdate(
                branchName = branchName,
                commitId = commitId,
            )
        }
    }
}

@Composable
private fun VersionUpdate(
    branchName: String,
    commitId: String,
) = Box {
    val activity = LocalContext.current as Activity
    val viewModel = viewModel { VersionUpdateViewModel() }
    val error by viewModel.errorState.collectAsState(null)
    val showSignInButton by viewModel.showSignInButtonState.collectAsState()
    val updateRequest by viewModel.updateRequestState.collectAsState()
    val updateProgress by viewModel.updateProgressState.collectAsState()
    val releases by viewModel.releases.collectAsState()
    var showBottomSheet by remember { mutableStateOf(false) }

    error?.let {
        Toast.makeText(activity, it, Toast.LENGTH_SHORT).show()
    }
    LaunchedEffect(Unit) {
        viewModel.init(
            activity = activity,
            branchName = branchName,
            commitId = commitId,
        )
    }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) {
        viewModel.onSignInResult(activity = activity, result = it)
    }
    if (showSignInButton != null) {
        Button(
            onClick = {
                launcher.launch(showSignInButton)
            },
        ) {
            Text(text = stringResource(id = R.string.ly_img_editor_debug_menu_sign_in))
        }
    } else {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            updateRequest?.let {
                if (it == Release.UpToDate) {
                    Text(
                        text = stringResource(id = R.string.ly_img_editor_debug_menu_up_to_date),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.titleSmall,
                    )
                } else {
                    Button(
                        onClick = {
                            viewModel.install(context = activity, release = it)
                        },
                    ) {
                        Text(text = stringResource(id = R.string.ly_img_editor_debug_menu_update))
                    }
                }
                Button(
                    onClick = {
                        showBottomSheet = true
                        viewModel.onSearch(text = "")
                    },
                ) {
                    Text(text = stringResource(id = R.string.ly_img_editor_debug_menu_switch_build))
                }
            } ?: run {
                CircularProgressIndicator(
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
    if (showBottomSheet) {
        BottomSheet(
            releases = releases,
            onDismissRequest = { showBottomSheet = false },
            onSearch = { viewModel.onSearch(text = it) },
            onDownloadClick = { viewModel.install(activity, it) },
        )
    }
    updateProgress?.let {
        UpdateProgressDialog(progress = it) {
            viewModel.cancelActive()
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun BottomSheet(
    releases: List<Release>?,
    onDismissRequest: () -> Unit,
    onSearch: (String) -> Unit,
    onDownloadClick: (Release) -> Unit,
) {
    BottomSheetDialog(onDismissRequest = onDismissRequest) {
        var query by remember { mutableStateOf("") }
        val keyboardController = LocalSoftwareKeyboardController.current

        Box(
            modifier = Modifier
                .padding(top = 12.dp, bottom = 8.dp)
                .size(width = 36.dp, height = 4.dp)
                .align(Alignment.CenterHorizontally)
                .background(
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                    shape = RoundedCornerShape(2.dp),
                ),
        )

        OutlinedTextField(
            value = query,
            onValueChange = {
                query = it
                onSearch(it)
            },
            singleLine = true,
            placeholder = {
                Text(text = stringResource(R.string.ly_img_editor_debug_menu_switch_build_hint))
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(
                onSearch = {
                    keyboardController?.hide()
                    onSearch(query)
                },
            ),
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            query = ""
                            onSearch("")
                        },
                    ) {
                        Icon(imageVector = IconPack.Close, contentDescription = null)
                    }
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceColorAtElevation(1.dp),
            ),
        )

        Spacer(modifier = Modifier.height(8.dp))
        releases?.let {
            if (releases.isEmpty()) {
                Text(
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                    text = stringResource(R.string.ly_img_editor_debug_menu_no_results_found),
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(items = releases, key = { it.key }) {
                        ReleaseRow(it, onDownloadClick = { onDownloadClick(it) })
                    }
                }
            }
        } ?: run {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.CenterHorizontally))
        }
    }
}

@Composable
private fun ReleaseRow(
    release: Release,
    onDownloadClick: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = release.branchName,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp),
        )
        Text(
            text = release.commitId,
            style = MaterialTheme.typography.titleLarge.copy(fontSize = 16.sp),
        )
        Text(
            text = release.createTime,
            style = MaterialTheme.typography.bodyMedium,
        )
        if (release.isCurrentBuild) {
            AssistChip(
                onClick = {},
                label = { Text(stringResource(R.string.ly_img_editor_debug_menu_current_build)) },
            )
        } else {
            Button(onClick = onDownloadClick) {
                Text(text = stringResource(id = R.string.ly_img_editor_debug_menu_install))
            }
        }
        Divider()
    }
}

@Composable
private fun UpdateProgressDialog(
    progress: Progress,
    onClearProgress: () -> Unit,
) {
    when (progress) {
        is Progress.Installing -> {
            Dialog(
                onDismissRequest = { },
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .background(color = AlertDialogDefaults.containerColor, shape = AlertDialogDefaults.shape),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(id = R.string.ly_img_editor_debug_menu_installing_dialog_title),
                        style = MaterialTheme.typography.titleMedium,
                    )
                }
            }
        }
        is Progress.Pending -> {
            Dialog(
                onDismissRequest = { },
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            ) {
                Column(
                    modifier = Modifier
                        .background(color = AlertDialogDefaults.containerColor, shape = AlertDialogDefaults.shape)
                        .padding(all = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(stringResource(id = R.string.ly_img_editor_debug_menu_downloading_dialog_title))
                    Box(
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(text = "${(progress.progress * 100).toInt()}%")
                        CircularProgressIndicator(
                            modifier = Modifier.size(60.dp),
                            progress = progress.progress,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Button(
                        onClick = { onClearProgress() },
                    ) {
                        Text(stringResource(R.string.ly_img_editor_debug_menu_cancel))
                    }
                }
            }
        }
        is Progress.Error -> {
            AlertDialog(
                title = {
                    Text(text = stringResource(R.string.ly_img_editor_debug_menu_error_dialog_title))
                },
                text = {
                    val text = stringResource(
                        R.string.ly_img_editor_debug_menu_error_dialog_description,
                        progress.throwable.message ?: "",
                    )
                    Text(text = text)
                },
                confirmButton = {
                    TextButton(
                        onClick = { onClearProgress() },
                    ) {
                        Text(stringResource(R.string.ly_img_editor_debug_menu_dismiss))
                    }
                },
                onDismissRequest = { },
                properties = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
            )
        }
    }
}
