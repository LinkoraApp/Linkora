package com.sakethh.linkora

import com.sakethh.linkora.domain.AppPreferences
import java.io.File

fun File.prepareWebCaptureDir(
    folderUuid: String,
    preferences: AppPreferences,
): File {
    val linkWebCaptureFolder = File(this, folderUuid)

    if (!preferences.webCaptureSaveAsVersions) {
        if (linkWebCaptureFolder.exists()) {
            linkWebCaptureFolder.deleteRecursively()
        }
        linkWebCaptureFolder.mkdirs()
    } else {
        if (!linkWebCaptureFolder.exists()) linkWebCaptureFolder.mkdirs()
        if (!preferences.webCaptureRetainAllVersions) {
            val existingFiles = linkWebCaptureFolder.listFiles()?.filter { it.isFile }
                ?.sortedBy { it.lastModified() }.orEmpty()

            if (existingFiles.size >= preferences.webCaptureMaxVersions) {
                existingFiles.take(existingFiles.size - preferences.webCaptureMaxVersions + 1)
                    .forEach { it.delete() }
            }
        }
    }
    return linkWebCaptureFolder
}
