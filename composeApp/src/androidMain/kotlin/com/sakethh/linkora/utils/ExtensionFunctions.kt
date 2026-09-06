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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
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

suspend fun DocumentFile.prepareWebCaptureFolder(
    folderUuid: String,
    saveAsVersions: Boolean,
    retainAllVersions: Boolean,
    maxVersions: Int,
): DocumentFile? {
    return withContext(Dispatchers.IO) {
        val linkWebCaptureFolder = this@prepareWebCaptureFolder.findFile(folderUuid)
        if (!saveAsVersions) {
            if (linkWebCaptureFolder != null && linkWebCaptureFolder.exists()) {
                linkWebCaptureFolder.delete()
            }
            return@withContext linkWebCaptureFolder?.createDirectory(folderUuid)
        } else {
            if (linkWebCaptureFolder == null || !linkWebCaptureFolder.exists()) {
                linkWebCaptureFolder?.createDirectory(folderUuid)
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
            return@withContext linkWebCaptureFolder
        }
    }
}

suspend fun File.prepareWebCaptureFolder(
    folderUuid: String,
    saveAsVersions: Boolean,
    retainAllVersions: Boolean,
    maxVersions: Int,
): File {
    return withContext(Dispatchers.IO) {
        val linkWebCaptureFolder = File(this@prepareWebCaptureFolder, folderUuid)
        if (!saveAsVersions) {
            if (linkWebCaptureFolder.exists()) {
                linkWebCaptureFolder.delete()
            }
            linkWebCaptureFolder.mkdirs()
        } else {
            if (!linkWebCaptureFolder.exists()) {
                linkWebCaptureFolder.mkdirs()
            }
            if (!retainAllVersions) {
                val existingFiles = linkWebCaptureFolder.listFiles()
                    ?.filter { it.isFile }
                    ?.sortedBy { it.lastModified() }
                    .orEmpty()
                if (existingFiles.size >= maxVersions) {
                    existingFiles.take(existingFiles.size - maxVersions + 1)
                        .forEach { it.delete() }
                }
            }
        }
        return@withContext linkWebCaptureFolder
    }
}

context(context: Context)
fun onTV(): Boolean = (context.getSystemService(UI_MODE_SERVICE) as UiModeManager).currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

context(context: Context)
private fun onMobile(): Boolean {
    val metrics = WindowMetricsCalculator.getOrCreate().computeCurrentWindowMetrics(context)
    val widthInPixels = metrics.bounds.width()
    return with(Density(context)) {
        widthInPixels.toDp() < 840.dp
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
