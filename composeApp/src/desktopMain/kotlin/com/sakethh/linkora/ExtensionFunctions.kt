package com.sakethh.linkora

import com.sakethh.linkora.domain.AppPreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

suspend fun File.prepareWebCaptureDir(
    folderUuid: String,
    preferences: AppPreferences,
): File = withContext(Dispatchers.IO) {
    val linkWebCaptureFolder = File(this@prepareWebCaptureDir, folderUuid)

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
    return@withContext linkWebCaptureFolder
}
