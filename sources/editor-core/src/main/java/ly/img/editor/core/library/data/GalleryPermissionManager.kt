package ly.img.editor.core.library.data

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

object GalleryPermissionManager {
    enum class Mode { UNDECIDED, ALL, SELECTED, DENIED }

    var mode: Mode = Mode.UNDECIDED
        private set

    @Volatile
    private var permissionsVersion: Int = 0
    val permissionVersion: Int
        get() = permissionsVersion

    // App-added persistent grants (unioniert mit Systemauswahl in der Anzeige)
    private val _selectedUris = mutableListOf<Uri>()
    private var loaded = false
    private const val PREFS = "imgly_gallery_permissions"
    private const val KEY_SELECTED = "selected_uris"
    val selectedUris: List<Uri> get() = _selectedUris

    fun setSelected(
        uris: List<Uri>,
        context: Context,
    ) {
        ensureLoaded(context)
        Log.d("GalleryPermissionManager", "Adding selected URIs: ${uris.size} items")
        var changed = updateMode(Mode.UNDECIDED) // Anzeige wird über MediaStore bestimmt; diese Liste ergänzt nur
        val existing = _selectedUris.map { it.toString() }.toMutableSet()
        uris.forEach { uri ->
            if (existing.add(uri.toString())) {
                _selectedUris.add(uri)
                changed = true
            }
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                Log.d("GalleryPermissionManager", "Persisted permission for: $uri")
            } catch (e: SecurityException) {
                Log.w("GalleryPermissionManager", "Failed persist for: $uri", e)
            }
        }
        persist(context)
        if (changed) {
            markPermissionsChanged()
        }
    }

    fun setAllGranted() {
        val cleared = _selectedUris.isNotEmpty()
        _selectedUris.clear()
        val modeChanged = updateMode(Mode.ALL)
        if (cleared || modeChanged) {
            markPermissionsChanged()
        }
    }

    fun setDenied() {
        val cleared = _selectedUris.isNotEmpty()
        _selectedUris.clear()
        val modeChanged = updateMode(Mode.DENIED)
        if (cleared || modeChanged) {
            markPermissionsChanged()
        }
    }

    fun addSelected(
        uri: Uri,
        context: Context,
    ) {
        ensureLoaded(context)
        // If we already have full access, keep ALL mode
        val hasFullAccess = if (
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            (
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) ==
                    PackageManager.PERMISSION_GRANTED ||
                    ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VIDEO) ==
                    PackageManager.PERMISSION_GRANTED
            )
        ) {
            true
        } else if (
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE,
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            true
        } else {
            false
        }

        var changed = if (!hasFullAccess) {
            updateMode(Mode.SELECTED)
        } else {
            updateMode(Mode.ALL)
        }
        try {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (e: SecurityException) {
            Log.w("GalleryPermissionManager", "Failed to persist permission for: $uri", e)
        }
        if (_selectedUris.none { it == uri }) {
            _selectedUris.add(uri)
            changed = true
        }
        persist(context)
        if (changed) {
            markPermissionsChanged()
        }
    }

    fun hasPermission(
        context: Context,
        mimeType: String?,
    ): Boolean {
        ensureLoaded(context)
        val needsVideo = mimeType?.startsWith("video/") == true
        val needsImage = mimeType?.startsWith("image/") == true
        val needsAnyVisuals = !needsVideo && !needsImage

        val legacyGranted = ContextCompat.checkSelfPermission(
            context,
            android.Manifest.permission.READ_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED

        val imagePermissionGranted =
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_IMAGES) ==
                PackageManager.PERMISSION_GRANTED
        val videoPermissionGranted =
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.READ_MEDIA_VIDEO) ==
                PackageManager.PERMISSION_GRANTED

        val fullAccess = when {
            legacyGranted -> true
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> when {
                needsVideo -> videoPermissionGranted
                needsImage -> imagePermissionGranted
                needsAnyVisuals -> imagePermissionGranted || videoPermissionGranted
                else -> imagePermissionGranted || videoPermissionGranted
            }
            else -> false
        }

        if (fullAccess) {
            Log.d("GalleryPermissionManager", "Full access granted for mimeType=$mimeType")
            if (mode != Mode.ALL) setAllGranted()
            // Ensure mode reflects the actual scope when only one of the media permissions is granted
            when {
                legacyGranted -> {
                    if (mode != Mode.ALL) setAllGranted()
                }
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
                needsAnyVisuals && !(imagePermissionGranted && videoPermissionGranted) -> {
                    if (updateMode(Mode.SELECTED)) {
                        markPermissionsChanged()
                    }
                }
            }
        } else {
            Log.d(
                "GalleryPermissionManager",
                "No full access for mimeType=$mimeType (legacy=$legacyGranted, image=$imagePermissionGranted, video=$videoPermissionGranted)",
            )
        }

        val partialAccess =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE &&
                ContextCompat.checkSelfPermission(
                    context,
                    android.Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
                ) == PackageManager.PERMISSION_GRANTED

        val result = when {
            fullAccess -> {
                // If we don't actually have the specific permission we need, treat as partial
                when {
                    needsVideo && !videoPermissionGranted -> false
                    needsImage && !imagePermissionGranted -> false
                    needsAnyVisuals && !(imagePermissionGranted && videoPermissionGranted) && !legacyGranted -> false
                    else -> true
                }
            }
            partialAccess -> {
                // User granted limited access; even wenn keine selectedUris lokal hinterlegt sind,
                // liefert MediaStore bereits die erlaubten Einträge. UI darf fortfahren.
                val changed = if (_selectedUris.isNotEmpty()) {
                    updateMode(Mode.SELECTED)
                } else {
                    updateMode(Mode.UNDECIDED)
                }
                if (changed) {
                    markPermissionsChanged()
                }
                true
            }
            else -> {
                if (updateMode(Mode.DENIED)) {
                    markPermissionsChanged()
                }
                false
            }
        }
        Log.d(
            "GalleryPermissionManager",
            "Permission check result: $result (fullAccess: $fullAccess, partialAccess: $partialAccess, selectedUris: ${_selectedUris.size})",
        )
        return result
    }

    fun requestAllPermission(
        activity: Activity,
        mimeType: String?,
    ) {
        val permissions = requiredPermission(mimeType)
        ActivityCompat.requestPermissions(activity, permissions, 0)
    }

    fun requiredPermission(mimeType: String?): Array<String?> = when {
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

    private fun updateMode(newMode: Mode): Boolean = if (mode != newMode) {
        mode = newMode
        true
    } else {
        false
    }

    private fun markPermissionsChanged() {
        permissionsVersion += 1
    }

    private fun persist(context: Context) {
        runCatching {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            prefs.edit().putStringSet(KEY_SELECTED, _selectedUris.map { it.toString() }.toSet()).apply()
        }
    }

    private fun ensureLoaded(context: Context) {
        if (loaded) return
        runCatching {
            val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val set = prefs.getStringSet(KEY_SELECTED, emptySet()) ?: emptySet()
            _selectedUris.clear()
            _selectedUris += set.mapNotNull { runCatching { Uri.parse(it) }.getOrNull() }
        }
        loaded = true
    }
}
