package ly.img.editor.version.details

import android.app.Activity
import android.app.Activity.RESULT_CANCELED
import android.content.Context
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.auth.api.identity.AuthorizationRequest
import com.google.android.gms.auth.api.identity.Identity
import com.google.android.gms.common.api.Scope
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.options
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import ly.img.editor.version.details.entity.Progress
import ly.img.editor.version.details.entity.Release
import org.json.JSONObject
import ru.solrudev.ackpine.installer.PackageInstaller
import ru.solrudev.ackpine.installer.parameters.InstallMode
import ru.solrudev.ackpine.installer.parameters.InstallParameters
import ru.solrudev.ackpine.installer.parameters.InstallerType
import ru.solrudev.ackpine.session.SessionResult
import ru.solrudev.ackpine.session.await
import ru.solrudev.ackpine.session.parameters.Confirmation
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.UUID
import javax.net.ssl.HttpsURLConnection
import kotlin.coroutines.cancellation.CancellationException

internal class VersionUpdateViewModel : ViewModel() {
    private val errorChannel = Channel<String>()
    val errorState = errorChannel.receiveAsFlow()

    private var accessToken: String? = null
    private var branchName: String = ""
    private var commitId: String = ""

    private val _showSignInButtonState = MutableStateFlow<IntentSenderRequest?>(null)
    val showSignInButtonState: StateFlow<IntentSenderRequest?> = _showSignInButtonState

    private val _updateRequestState = MutableStateFlow<Release?>(null)
    val updateRequestState: StateFlow<Release?> = _updateRequestState

    private val _updateProgressState = MutableStateFlow<Progress?>(null)
    val updateProgressState: StateFlow<Progress?> = _updateProgressState

    private val _releases = MutableStateFlow<List<Release>?>(null)
    val releases: StateFlow<List<Release>?> = _releases

    private var downloadJob: Job? = null

    fun init(
        activity: Activity,
        branchName: String,
        commitId: String,
    ) {
        this.branchName = branchName
        this.commitId = commitId
        viewModelScope.launch {
            try {
                val authorizationRequest = AuthorizationRequest
                    .builder()
                    .setRequestedScopes(listOf(Scope(APP_DISTRIBUTION_SCOPE)))
                    .build()
                Identity
                    .getAuthorizationClient(activity)
                    .authorize(authorizationRequest)
                    .await()
                    .let {
                        accessToken = it.accessToken
                        it.pendingIntent?.let {
                            _showSignInButtonState.value = IntentSenderRequest.Builder(it).build()
                        }
                    }
            } catch (exception: Exception) {
                Log.e(TAG, "init failed", exception)
                errorChannel.trySend(SIGN_IN_ERROR)
                return@launch
            }
            if (_showSignInButtonState.value == null) {
                checkForNewRelease()
            }
        }
    }

    fun onSignInResult(
        activity: Activity,
        result: ActivityResult,
    ) {
        if (result.resultCode == RESULT_CANCELED) {
            // We need to reset _showSignInButtonState as previous IntentSenderRequest is consumed
            init(activity, branchName, commitId)
            return
        }
        try {
            _showSignInButtonState.value = null
            accessToken = Identity
                .getAuthorizationClient(activity)
                .getAuthorizationResultFromIntent(result.data)
                .accessToken
            requireNotNull(accessToken)
            checkForNewRelease()
        } catch (exception: Exception) {
            Log.e(TAG, "onSignInResult failed", exception)
            errorChannel.trySend(SIGN_IN_ERROR)
            // We need to reset _showSignInButtonState as previous IntentSenderRequest is consumed
            init(activity, branchName, commitId)
        }
    }

    fun onSearch(text: String) = viewModelScope.launch {
        try {
            _releases.value = null
            val filter = text
                .takeIf { it.isNotEmpty() }
                ?.let { "*$it*" }
            _releases.value = getReleases(filter = filter)
        } catch (exception: Exception) {
            Log.e(TAG, "onSearch failed", exception)
            errorChannel.trySend("Error fetching releases with query $text")
            _releases.value = emptyList()
        }
    }

    fun install(
        context: Context,
        release: Release,
    ) = viewModelScope.launch {
        runCatching {
            _updateProgressState.value = Progress.Pending(0F)
            val uri = withContext(Dispatchers.IO) {
                val file = File(context.filesDir, "apks").let {
                    it.deleteRecursively()
                    it.mkdirs()
                    File(it, "${UUID.randomUUID()}.apk")
                }
                val connection = URL(release.downloadUrl).openConnection() as HttpsURLConnection
                connection.requestMethod = "GET"
                val responseCode = connection.responseCode
                require(responseCode in 200 until 300)
                val responseLength = connection.contentLength.toLong()

                connection.inputStream.use { inputStream ->
                    file.outputStream().use { outputStream ->
                        var totalBytesRead = 0L
                        val byteArray = ByteArray(DEFAULT_BUFFER_SIZE)
                        while (true) {
                            val readBytes = inputStream.read(byteArray)
                            if (readBytes == -1) break
                            totalBytesRead += readBytes
                            outputStream.write(byteArray, 0, readBytes)
                            val progress = totalBytesRead.toFloat() / responseLength
                            ensureActive()
                            _updateProgressState.value = Progress.Pending(progress)
                        }
                    }
                }
                FileProvider.getUriForFile(context, "${context.packageName}.ly.img.editor.fileprovider", file)
            }
            _updateProgressState.value = Progress.Installing
            val packageInstaller = PackageInstaller.getInstance(context)
            val installParameters = InstallParameters(uri) {
                installerType = InstallerType.SESSION_BASED
                confirmation = Confirmation.IMMEDIATE
                installMode = InstallMode.Full
            }
            val session = packageInstaller.createSession(installParameters)
            val result = session.await()
            if (result is SessionResult.Error) {
                error(result.cause.message ?: "")
            }
        }.onFailure {
            Log.e(TAG, "install failed", it)
            if (it !is CancellationException) {
                _updateProgressState.value = Progress.Error(it)
            }
        }
    }.also {
        downloadJob = it
    }

    fun cancelActive() {
        downloadJob?.cancel()
        downloadJob = null
        _updateProgressState.value = null
    }

    private fun checkForNewRelease() = viewModelScope.launch {
        runCatching {
            // First release that has the same branchName but a different commitId is considered an update.
            _updateRequestState.value = getReleases(filter = "$branchName*")
                .firstOrNull { it.branchName == branchName }
                ?.takeIf { it.commitId != commitId }
                ?: Release.UpToDate
        }.onFailure {
            Log.e(TAG, "checkForNewRelease failed", it)
            _updateRequestState.value = Release.UpToDate
        }
    }

    private suspend fun getReleases(filter: String?): List<Release> = withContext(Dispatchers.IO) {
        val url = GET_RELEASES_PATH.format(
            Firebase.options.gcmSenderId,
            Firebase.options.applicationId,
            filter?.let { "?filter=releaseNotes.text=\"$it\"" } ?: "",
        )
        val connection = URL(url).openConnection() as HttpURLConnection
        connection.setRequestProperty(HEADER_KEY_AUTHORIZATION, "Bearer $accessToken")
        val response = connection.inputStream
            .bufferedReader()
            .use { it.readText() }
            .let { JSONObject(it) }
        if (response.has(JSON_KEY_RELEASES).not()) return@withContext emptyList()
        val releases = response.getJSONArray(JSON_KEY_RELEASES)
        val sourceDateFormat = SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ss.SSSSSSXXX", Locale.US)
        val resultDateFormat = SimpleDateFormat("MMM dd',' yyyy 'at' HH:mm", Locale.US)
        List(releases.length()) {
            runCatching {
                // We place it in runCatching block as older releases will not be able to be parsed
                val release = releases.getJSONObject(it)
                val displayVersion = release.getString(JSON_KEY_DISPLAY_VERSION)
                val date = sourceDateFormat.parse(release.getString(JSON_KEY_CREATE_TIME))
                val createTime = resultDateFormat.format(requireNotNull(date))
                val downloadUrl = release.getString(JSON_KEY_DOWNLOAD_URL)
                val (branchName, commitId) = displayVersion.lastIndexOf('-').let {
                    require(it != -1)
                    displayVersion.substring(0, it).trim() to displayVersion.substring(it + 1).trim()
                }
                require(branchName.isNotEmpty())
                require(commitId.isNotEmpty())
                Release(
                    key = displayVersion,
                    createTime = createTime,
                    downloadUrl = downloadUrl,
                    branchName = branchName,
                    commitId = commitId,
                    isCurrentBuild = branchName == this@VersionUpdateViewModel.branchName &&
                        commitId == this@VersionUpdateViewModel.commitId,
                )
            }.getOrNull()
        }.filterNotNull().distinctBy { it.key }
    }

    companion object {
        private const val TAG = "VersionUpdateViewModel"

        private const val SIGN_IN_ERROR = "Failed to sign in. Check your account or try again later."

        private const val APP_DISTRIBUTION_SCOPE = "https://www.googleapis.com/auth/cloud-platform"
        private const val GET_RELEASES_PATH = "https://firebaseappdistribution.googleapis.com/v1/projects/%s/apps/%s/releases%s"

        private const val HEADER_KEY_AUTHORIZATION = "Authorization"
        private const val JSON_KEY_RELEASES = "releases"
        private const val JSON_KEY_DISPLAY_VERSION = "displayVersion"
        private const val JSON_KEY_CREATE_TIME = "createTime"
        private const val JSON_KEY_DOWNLOAD_URL = "binaryDownloadUri"
    }
}
