package ly.img.editor.core.library.data

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import android.webkit.MimeTypeMap
import androidx.core.content.ContextCompat
import ly.img.editor.core.UnstableEditorApi
import java.util.Locale

/**
 * Centralized state for handling system gallery permissions.
 *
 * The editor uses this object to decide whether full gallery access is granted,
 * whether only user-selected items are available, or if access is denied.
 * State changes bump [permissionVersion] so UI flows can react.
 */
object SystemGalleryPermission {
    /**
     * Current permission mode for gallery access.
     *
     * - [UNDECIDED]: No decision yet; UI should ask for permission when needed.
     * - [ALL]: Full media access granted.
     * - [SELECTED]: Limited access to user-selected items.
     * - [DENIED]: Access denied; UI should surface a permission prompt.
     */
    enum class Mode { UNDECIDED, ALL, SELECTED, DENIED }

    var mode: Mode = Mode.UNDECIDED
        private set

    @Volatile
    private var permissionsVersion: Int = 0
    val permissionVersion: Int
        get() = permissionsVersion

    private val _selectedUris = mutableListOf<SelectedUri>()
    private var loaded = false

    @Volatile
    private var manualMode = false
    val selectedUris: List<Uri> get() = _selectedUris.map { it.uri }

    val isManualMode: Boolean
        get() = manualMode

    fun selectedForMimeType(mimeType: String?): List<SelectedUri> {
        val normalizedMimeType = mimeType?.lowercase(Locale.US)
        val needsVideo = normalizedMimeType?.startsWith("video/") == true
        val needsImage = normalizedMimeType?.startsWith("image/") == true
        val needsAnyVisuals = !needsVideo && !needsImage

        return _selectedUris.filter { entry ->
            val type = entry.mimeType?.lowercase(Locale.US)
            when {
                needsAnyVisuals -> true
                type.isNullOrBlank() -> false
                needsVideo -> type.startsWith("video/")
                needsImage -> type.startsWith("image/")
                else -> true
            }
        }
    }

    /**
     * Configure how the system gallery should behave.
     *
     * Marked unstable to make opt-in explicit for integrators.
     */
    @UnstableEditorApi
    fun setMode(configuration: SystemGalleryConfiguration) {
        val newManualMode = !configuration.enableAssetSource
        if (manualMode != newManualMode) {
            manualMode = newManualMode
            val changed = if (manualMode) {
                updateMode(Mode.SELECTED)
            } else {
                updateMode(Mode.UNDECIDED)
            }
            if (changed) {
                markPermissionsChanged()
            }
        }
    }

    /**
     * Mark full access granted and clear any selected-only state.
     */
    fun setAllGranted() {
        val cleared = _selectedUris.isNotEmpty()
        _selectedUris.clear()
        val modeChanged = updateMode(Mode.ALL)
        if (cleared || modeChanged) {
            markPermissionsChanged()
        }
    }

    /**
     * Add a single user-selected URI to the allowed set.
     */
    fun addSelected(
        uri: Uri,
        context: Context,
        mimeTypeHint: String? = null,
    ) {
        if (isInternalFileProviderUri(uri, context)) {
            Log.d("GalleryPermission", "Ignoring internal file-provider URI: $uri")
            return
        }
        persistUriPermission(uri, context)
        ensureLoaded(context)
        val resolvedMimeType = resolveMimeType(uri, context, mimeTypeHint)
        if (manualMode) {
            var changed = updateMode(Mode.SELECTED)
            changed = addOrUpdateSelected(uri, resolvedMimeType) || changed
            if (changed) {
                markPermissionsChanged()
            }
            return
        }

        val hasFullAccess = hasFullReadAccess(context)
        var changed = if (hasFullAccess) {
            updateMode(Mode.ALL)
        } else {
            updateMode(Mode.SELECTED)
        }
        changed = addOrUpdateSelected(uri, resolvedMimeType) || changed
        if (changed) {
            markPermissionsChanged()
        }
    }

    private fun addOrUpdateSelected(
        uri: Uri,
        mimeType: String?,
    ): Boolean {
        val existingIndex = _selectedUris.indexOfFirst { it.uri == uri }
        return if (existingIndex == -1) {
            _selectedUris.add(SelectedUri(uri, mimeType))
            true
        } else {
            val existing = _selectedUris[existingIndex]
            if (existing.mimeType == null && mimeType != null) {
                _selectedUris[existingIndex] = SelectedUri(uri, mimeType)
                true
            } else {
                false
            }
        }
    }

    /**
     * Returns true if the current permission state allows reading the given mimeType.
     */
    fun hasPermission(
        context: Context,
        mimeType: String?,
    ): Boolean {
        if (manualMode) {
            val changed = updateMode(Mode.SELECTED)
            if (changed) {
                markPermissionsChanged()
            }
            return true
        }
        ensureLoaded(context)

        val needsVideo = mimeType?.startsWith("video/") == true
        val needsImage = mimeType?.startsWith("image/") == true
        val needsAnyVisuals = !needsVideo && !needsImage

        val legacyGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED

        val imagePermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_MEDIA_IMAGES,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
        }
        val videoPermissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_MEDIA_VIDEO,
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            imagePermissionGranted
        }

        val fullAccess = if (legacyGranted) {
            true
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            when {
                needsVideo -> videoPermissionGranted
                needsImage -> imagePermissionGranted
                needsAnyVisuals -> imagePermissionGranted || videoPermissionGranted
                else -> imagePermissionGranted || videoPermissionGranted
            }
        } else {
            false
        }

        if (legacyGranted) {
            Log.d("GalleryPermission", "Legacy full access granted for mime=$mimeType")
            if (mode != Mode.ALL) setAllGranted()
        } else if (fullAccess) {
            Log.d("GalleryPermission", "Full access granted for mime=$mimeType")
            if (mode != Mode.ALL) setAllGranted()
            when {
                needsVideo && !videoPermissionGranted -> {
                    if (updateMode(Mode.SELECTED)) {
                        markPermissionsChanged()
                    }
                }
                needsImage && !imagePermissionGranted -> {
                    if (updateMode(Mode.SELECTED)) {
                        markPermissionsChanged()
                    }
                }
                needsAnyVisuals &&
                    !(imagePermissionGranted && videoPermissionGranted) -> {
                    if (updateMode(Mode.SELECTED)) {
                        markPermissionsChanged()
                    }
                }
            }
        }

        val partialAccess =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                ) == PackageManager.PERMISSION_GRANTED

        val result = when {
            legacyGranted -> true
            fullAccess -> {
                when {
                    needsVideo && !videoPermissionGranted -> false
                    needsImage && !imagePermissionGranted -> false
                    needsAnyVisuals &&
                        !(imagePermissionGranted && videoPermissionGranted) &&
                        !legacyGranted -> false
                    else -> true
                }
            }
            partialAccess -> {
                val hasSelectionForMimeType = selectedForMimeType(mimeType).isNotEmpty()
                val changed = if (hasSelectionForMimeType) {
                    updateMode(Mode.SELECTED)
                } else {
                    updateMode(Mode.UNDECIDED)
                }
                if (changed) {
                    markPermissionsChanged()
                }
                hasSelectionForMimeType
            }
            else -> {
                if (updateMode(Mode.DENIED)) {
                    markPermissionsChanged()
                }
                false
            }
        }
        Log.d(
            "GalleryPermission",
            "Permission result=$result full=$fullAccess " +
                "partial=$partialAccess selected=${_selectedUris.size}",
        )
        return result
    }

    /**
     * Returns the permission set required for the provided mimeType.
     */
    fun requiredPermission(mimeType: String?): Array<String?> = when {
        manualMode -> emptyArray()
        Build.VERSION.SDK_INT >= 34 -> when {
            mimeType?.startsWith("video/") == true -> arrayOf(
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            )
            mimeType?.startsWith("image/") == true -> arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            )
            else -> arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
            )
        }
        Build.VERSION.SDK_INT >= 33 -> when {
            mimeType?.startsWith("video/") == true -> arrayOf(
                android.Manifest.permission.READ_MEDIA_VIDEO,
                android.Manifest.permission.READ_MEDIA_IMAGES,
            )
            mimeType?.startsWith("image/") == true -> arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
            )
            else -> arrayOf(
                android.Manifest.permission.READ_MEDIA_IMAGES,
                android.Manifest.permission.READ_MEDIA_VIDEO,
            )
        }
        else -> arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun hasFullReadAccess(context: Context): Boolean {
        val legacyGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
        if (legacyGranted) return true

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false

        val imagesGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_MEDIA_IMAGES,
        ) == PackageManager.PERMISSION_GRANTED
        val videoGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_MEDIA_VIDEO,
        ) == PackageManager.PERMISSION_GRANTED
        return imagesGranted || videoGranted
    }

    private fun updateMode(newMode: Mode): Boolean = if (mode != newMode) {
        mode = newMode
        true
    } else {
        false
    }

    private fun markPermissionsChanged() {
        permissionsVersion += 1
    }

    private fun ensureLoaded(context: Context) {
        if (!loaded) {
            loaded = true
        }
    }

    private fun resolveMimeType(
        uri: Uri,
        context: Context,
        mimeTypeHint: String?,
    ): String? {
        val resolverType = runCatching { context.contentResolver.getType(uri) }.getOrNull()
            ?.takeIf { it.isNotBlank() }
        if (resolverType != null) return normalizeMimeType(resolverType)

        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            ?.lowercase(Locale.US)
        if (!extension.isNullOrBlank()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)?.let {
                return normalizeMimeType(it)
            }
        }
        val normalizedHint = normalizeMimeType(mimeTypeHint)
        if (normalizedHint != null && normalizedHint != "*/*") return normalizedHint
        return null
    }

    private fun normalizeMimeType(mimeType: String?): String? = mimeType?.takeIf { it.isNotBlank() }?.lowercase(Locale.US)

    private fun isInternalFileProviderUri(
        uri: Uri,
        context: Context,
    ): Boolean {
        val authority = uri.authority ?: return false
        val expectedAuthority = "${context.packageName}.ly.img.editor.fileprovider"
        return uri.scheme == ContentResolver.SCHEME_CONTENT && authority == expectedAuthority
    }

    private fun persistUriPermission(
        uri: Uri,
        context: Context,
    ) {
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
            Log.d("GalleryPermission", "Persisted permission for: $uri")
        } catch (e: SecurityException) {
            Log.w("GalleryPermission", "Failed persist for: $uri", e)
        }
    }

    data class SelectedUri(
        val uri: Uri,
        val mimeType: String?,
    )
}
