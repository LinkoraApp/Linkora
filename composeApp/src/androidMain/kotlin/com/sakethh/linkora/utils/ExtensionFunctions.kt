package com.sakethh.linkora.utils

import android.app.UiModeManager
import android.content.Context
import android.content.Context.UI_MODE_SERVICE
import android.content.res.Configuration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.documentfile.provider.DocumentFile
import androidx.window.layout.WindowMetricsCalculator
import com.sakethh.linkora.domain.Platform
import java.net.URI

fun String.isAllowedByWebCapturePolicies(
    whitelist: List<String>,
    blacklist: List<String>,
): Boolean {
    val host = try {
        URI(this).host ?: ""
    } catch (_: Exception) {
        ""
    }

    if (whitelist.isNotEmpty() && whitelist.none { host.endsWith(it) }) {
        return false
    }
    if (blacklist.any { host.endsWith(it) }) {
        return false
    }

    return true
}

fun DocumentFile.prepareWebCaptureFolder(
    folderUuid: String,
    saveAsVersions: Boolean,
    retainAllVersions: Boolean,
    maxVersions: Int,
): DocumentFile? {
    var linkWebCaptureFolder = this.findFile(folderUuid)

    if (!saveAsVersions) {
        if (linkWebCaptureFolder != null && linkWebCaptureFolder.exists()) {
            linkWebCaptureFolder.delete()
        }
        linkWebCaptureFolder = this.createDirectory(folderUuid)
    } else {
        if (linkWebCaptureFolder == null || !linkWebCaptureFolder.exists()) {
            linkWebCaptureFolder = this.createDirectory(folderUuid)
        }
        if (!retainAllVersions) {
            val existingFiles = linkWebCaptureFolder?.listFiles()
                ?.filter { it.isFile }
                ?.sortedBy { it.lastModified() }
                .orEmpty()
            if (existingFiles.size >= maxVersions) {
                existingFiles.take(existingFiles.size - maxVersions + 1)
                    .forEach { it.delete() }
            }
        }
    }
    return linkWebCaptureFolder
}

context(context: Context)
private fun onTV(): Boolean = (context.getSystemService(UI_MODE_SERVICE) as UiModeManager).currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

context(context: Context)
private fun onMobile(): Boolean {
    val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(context)
    val widthInPixels = metrics.bounds.width()
    return with(Density(context)) {
        widthInPixels.toDp() > 840.dp
    }
}

context(context: Context)
fun currentAndroidPlatform(): Platform = if (onTV()) {
    Platform.Android.TV
} else if (onMobile()) {
    Platform.Android.Mobile
} else {
    Platform.Android.Tablet
}
