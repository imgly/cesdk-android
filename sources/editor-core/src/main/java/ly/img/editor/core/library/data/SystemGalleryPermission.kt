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

    private val selectedUris = mutableListOf<SelectedUri>()

    @Volatile
    private var manualMode = false

    val isManualMode: Boolean
        get() = manualMode

    private data class MimeTypeCoverage(
        val needsVideo: Boolean,
        val needsImage: Boolean,
        val needsAnyVisuals: Boolean,
    )

    private fun resolveMimeTypeCoverage(mimeTypes: List<String>?): MimeTypeCoverage {
        val normalized = mimeTypes
            ?.mapNotNull { normalizeMimeType(it) }
            ?.filter { it.isNotBlank() }
            .orEmpty()

        if (normalized.isEmpty()) {
            return MimeTypeCoverage(needsVideo = true, needsImage = true, needsAnyVisuals = false)
        }

        val hasWildcard = normalized.any { it.startsWith("*") }
        val needsVideo = hasWildcard || normalized.any { it.startsWith("video/") }
        val needsImage = hasWildcard || normalized.any { it.startsWith("image/") }
        val needsAnyVisuals = !needsVideo && !needsImage
        return MimeTypeCoverage(needsVideo = needsVideo, needsImage = needsImage, needsAnyVisuals = needsAnyVisuals)
    }

    fun selectedForMimeType(mimeType: String?) = selectedForMimeTypes(mimeType?.let { listOf(it) })

    fun selectedForMimeTypes(mimeTypes: List<String>?): List<SelectedUri> {
        val coverage = resolveMimeTypeCoverage(mimeTypes)
        val needsVideo = coverage.needsVideo
        val needsImage = coverage.needsImage
        val needsAnyVisuals = coverage.needsAnyVisuals

        return selectedUris.filter { entry ->
            val type = entry.mimeType?.lowercase(Locale.US)
            when {
                needsAnyVisuals -> true
                type.isNullOrBlank() -> false
                needsVideo && needsImage -> type.startsWith("video/") || type.startsWith("image/")
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
            if (manualMode) {
                updateMode(Mode.SELECTED)
            } else {
                updateMode(Mode.UNDECIDED)
            }
        }
    }

    /**
     * Mark full access granted and clear any selected-only state.
     */
    fun setAllGranted() {
        selectedUris.clear()
        updateMode(Mode.ALL)
    }

    /**
     * Add a single user-selected URI to the allowed set.
     */
    fun addSelected(
        uri: Uri,
        context: Context,
    ) {
        if (isInternalFileProviderUri(uri, context)) {
            Log.d("GalleryPermission", "Ignoring internal file-provider URI: $uri")
            return
        }
        persistUriPermission(uri, context)
        val resolvedMimeType = resolveMimeType(uri, context)
        if (manualMode) {
            updateMode(Mode.SELECTED)
            addOrUpdateSelected(uri, resolvedMimeType)
            return
        }

        val hasFullAccess = hasFullReadAccess(context)
        if (hasFullAccess) {
            updateMode(Mode.ALL)
        } else {
            updateMode(Mode.SELECTED)
        }
        addOrUpdateSelected(uri, resolvedMimeType)
    }

    private fun addOrUpdateSelected(
        uri: Uri,
        mimeType: String?,
    ): Boolean {
        val existingIndex = selectedUris.indexOfFirst { it.uri == uri }
        return if (existingIndex == -1) {
            selectedUris.add(SelectedUri(uri, mimeType))
            true
        } else {
            val existing = selectedUris[existingIndex]
            if (existing.mimeType == null && mimeType != null) {
                selectedUris[existingIndex] = SelectedUri(uri, mimeType)
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
    ): Boolean = hasPermissionForMimeTypes(context, mimeType?.let { listOf(it) })

    fun hasPermissionForMimeTypes(
        context: Context,
        mimeTypes: List<String>?,
    ): Boolean {
        if (manualMode) {
            updateMode(Mode.SELECTED)
            return true
        }
        val coverage = resolveMimeTypeCoverage(mimeTypes)
        val needsVideo = coverage.needsVideo
        val needsImage = coverage.needsImage
        val needsAnyVisuals = coverage.needsAnyVisuals

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
                needsVideo && needsImage -> videoPermissionGranted && imagePermissionGranted
                needsVideo -> videoPermissionGranted
                needsImage -> imagePermissionGranted
                else -> imagePermissionGranted || videoPermissionGranted
            }
        } else {
            false
        }

        if (legacyGranted) {
            Log.d("GalleryPermission", "Legacy full access granted for mimeTypes=$mimeTypes")
            if (mode != Mode.ALL) setAllGranted()
        } else if (fullAccess) {
            Log.d("GalleryPermission", "Full access granted for mimeTypes=$mimeTypes")
            if (mode != Mode.ALL) setAllGranted()
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
                        !(imagePermissionGranted && videoPermissionGranted) -> false
                    else -> true
                }
            }
            partialAccess -> {
                val hasSelectionForMimeType = selectedForMimeTypes(mimeTypes).isNotEmpty()
                if (hasSelectionForMimeType) {
                    updateMode(Mode.SELECTED)
                } else {
                    updateMode(Mode.UNDECIDED)
                }
                hasSelectionForMimeType
            }
            else -> {
                updateMode(Mode.DENIED)
                false
            }
        }
        Log.d(
            "GalleryPermission",
            "Permission result=$result full=$fullAccess " +
                "partial=$partialAccess selected=${selectedUris.size}",
        )
        return result
    }

    /**
     * Returns the permission set required for the provided mimeTypes.
     */
    fun requiredPermission(mimeTypes: List<String>): Array<String?> = when {
        manualMode -> emptyArray()
        Build.VERSION.SDK_INT >= 34 -> {
            val coverage = resolveMimeTypeCoverage(mimeTypes)
            when {
                coverage.needsVideo && !coverage.needsImage -> arrayOf(
                    android.Manifest.permission.READ_MEDIA_VIDEO,
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                )
                coverage.needsImage && !coverage.needsVideo -> arrayOf(
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
        }
        Build.VERSION.SDK_INT >= 33 -> {
            val coverage = resolveMimeTypeCoverage(mimeTypes)
            when {
                coverage.needsVideo && !coverage.needsImage -> arrayOf(
                    android.Manifest.permission.READ_MEDIA_VIDEO,
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                )
                coverage.needsImage && !coverage.needsVideo -> arrayOf(
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_VIDEO,
                )
                else -> arrayOf(
                    android.Manifest.permission.READ_MEDIA_IMAGES,
                    android.Manifest.permission.READ_MEDIA_VIDEO,
                )
            }
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

    private fun updateMode(newMode: Mode) {
        if (mode != newMode) {
            mode = newMode
        }
    }

    private fun resolveMimeType(
        uri: Uri,
        context: Context,
    ): String? {
        val resolverType = runCatching { context.contentResolver.getType(uri) }.getOrNull()
            ?.takeIf { it.isNotBlank() }
            ?.let(::normalizeMimeType)
            ?.takeUnless { it == "*/*" }
        if (resolverType != null) return resolverType

        val extension = MimeTypeMap.getFileExtensionFromUrl(uri.toString())
            ?.lowercase(Locale.US)
        if (!extension.isNullOrBlank()) {
            MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension)?.let {
                return normalizeMimeType(it)
            }
        }
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

    private fun hasMimeFiltered(
        mimeTypes: List<String>,
        mime: String,
    ): Boolean {
        val normalizedMime = mime.lowercase(Locale.US)
        val wildCardType = normalizedMime.substringBefore("/") + "/"
        val isWildCardSearch = normalizedMime.endsWith("/*")
        mimeTypes.forEach {
            val normalizedFilter = it.lowercase(Locale.US)
            if (normalizedFilter.startsWith("*")) {
                return true
            } else if ((normalizedFilter.endsWith("/*") || isWildCardSearch) && normalizedFilter.startsWith(wildCardType)) {
                return true
            } else if (normalizedFilter == normalizedMime) {
                return true
            }
        }
        return false
    }

    fun hasVideoType(mimeTypes: List<String>) = hasMimeFiltered(mimeTypes, "video/*")

    fun hasImageType(mimeTypes: List<String>) = hasMimeFiltered(mimeTypes, "image/*")

    data class SelectedUri(
        val uri: Uri,
        val mimeType: String?,
    )
}
