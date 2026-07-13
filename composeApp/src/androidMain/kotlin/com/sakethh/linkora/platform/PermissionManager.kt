package com.sakethh.linkora.platform

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.sakethh.linkora.domain.PermissionStatus
import com.sakethh.linkora.utils.AndroidUIEvent

actual class PermissionManager(
    private val context: Context,
) {
    actual suspend fun permittedToShowNotification(): PermissionStatus = if (
        Build.VERSION.SDK_INT < 33 ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        PermissionStatus.Granted
    } else {
        AndroidUIEvent.pushUIEvent(AndroidUIEvent.Type.ShowRuntimePermissionForNotifications)
        PermissionStatus.NeedsRequest
    }

    actual suspend fun isStorageAccessPermitted(): PermissionStatus = if (
        Build.VERSION.SDK_INT > Build.VERSION_CODES.Q ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ) == PackageManager.PERMISSION_GRANTED
    ) {
        PermissionStatus.Granted
    } else {
        AndroidUIEvent.pushUIEvent(AndroidUIEvent.Type.ShowRuntimePermissionForStorage)
        PermissionStatus.NeedsRequest
    }
}
