package com.sakethh.linkora.worker

import android.content.Context
import androidx.documentfile.provider.DocumentFile
import com.sakethh.linkora.domain.ExportFileType
import com.sakethh.linkora.ui.screens.settings.section.data.ExportLocationType
import com.sakethh.linkora.utils.createNewFile
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

suspend fun Context.createWebCaptureFileDescriptor(folderUriString: String): Int? {
    return try {
        val (webCaptureFile, _) = createNewFile(
            context = this,
            exportLocation = folderUriString,
            exportFileType = ExportFileType.HTML,
            exportLocationType = ExportLocationType.WEB_CAPTURE,
        )
        this.contentResolver.openFileDescriptor(
            webCaptureFile?.uri ?: return null,
            "w",
        )?.detachFd()
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}
