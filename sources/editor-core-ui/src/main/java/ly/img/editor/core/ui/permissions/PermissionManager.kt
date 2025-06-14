package ly.img.editor.core.ui.permissions

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat.shouldShowRequestPermissionRationale
import androidx.core.content.ContextCompat
import ly.img.editor.core.ui.utils.activity

class PermissionManager(
    private val context: Context,
) {
    companion object {
        fun Context.hasCameraPermission() = hasPermission(Manifest.permission.CAMERA)

        fun Context.hasMicPermission() = hasPermission(Manifest.permission.RECORD_AUDIO)

        fun Context.hasCameraPermissionInManifest(): Boolean {
            val packageInfo = packageManager.getPackageInfo(packageName, PackageManager.GET_PERMISSIONS)
            return packageInfo.requestedPermissions?.any { it == Manifest.permission.CAMERA } ?: false
        }

        private fun Context.hasPermission(permission: String): Boolean =
            ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED
    }

    fun shouldOpenSettings(permission: String) = !context.hasPermission(permission) &&
        !shouldShowRequestPermissionRationale(checkNotNull(context.activity), permission)

    fun openAppSettings() {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }
}
