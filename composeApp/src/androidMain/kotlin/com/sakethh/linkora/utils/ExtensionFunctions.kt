package com.sakethh.linkora.utils

import android.content.Context
import android.content.Context.STORAGE_SERVICE
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import androidx.documentfile.provider.DocumentFile
import com.sakethh.linkora.domain.ExportFileType
import com.sakethh.linkora.ui.screens.settings.section.data.ExportLocationType
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
