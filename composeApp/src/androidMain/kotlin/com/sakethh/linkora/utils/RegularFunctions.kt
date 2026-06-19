@file:JvmName("AndroidRegularFunctions")

package com.sakethh.linkora.utils

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.sakethh.linkora.domain.ExportFileType
import com.sakethh.linkora.ui.screens.settings.section.data.ExportLocationType
import getFileNameWithTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun createNewFile(
    context: Context,
    exportLocation: String,
    exportFileType: ExportFileType,
    exportLocationType: ExportLocationType,
): Pair<DocumentFile?, String> {
    val exportFileName = getFileNameWithTimestamp(
        exportFileType = exportFileType,
        exportLocationType = exportLocationType
    )

    val directoryUri = exportLocation.toUri()
    return withContext(Dispatchers.IO) {
        val directory = DocumentFile.fromTreeUri(context, directoryUri)
        directory?.createFile(
            if (exportFileType == ExportFileType.HTML) "text/html" else "application/json",
            exportFileName
        ) to exportFileName
    }
}