@file:JvmName("AndroidRegularFunctions")

package com.sakethh.linkora.utils

import android.content.Context
import android.content.Context.STORAGE_SERVICE
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.provider.DocumentsContract
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.ExportFileType
import com.sakethh.linkora.ui.screens.settings.section.data.ExportLocationType
import com.sakethh.linkora.ui.theme.DarkColors
import com.sakethh.linkora.ui.theme.LightColors
import getFileNameWithTimestamp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun createNewFile(
    context: Context,
    exportLocation: String,
    exportFileType: ExportFileType,
    exportLocationType: ExportLocationType,
): Pair<DocumentFile?, String> {
    val exportFileName =
        getFileNameWithTimestamp(
            exportFileType = exportFileType,
            exportLocationType = exportLocationType,
        )

    val directoryUri = exportLocation.toUri()
    return withContext(Dispatchers.IO) {
        val directory = DocumentFile.fromTreeUri(context, directoryUri)
        directory?.createFile(
            if (exportFileType == ExportFileType.HTML) "text/html" else "application/json",
            exportFileName,
        ) to exportFileName
    }
}

fun getAbsolutePathFromSafUri(context: Context, uri: Uri): String? {
    if (uri.authority != "com.android.externalstorage.documents") {
        return null
    }

    val rawDocId = DocumentsContract.getTreeDocumentId(uri)
    val decodedDocId = Uri.decode(rawDocId)

    val split = decodedDocId.split(":")
    val type = split[0]
    val path = if (split.size > 1) split[1] else ""

    if ("primary".equals(type, ignoreCase = true)) {
        return "${Environment.getExternalStorageDirectory().absolutePath}/$path".removeSuffix("/")
    }

    // resolve secondary storage (sd cards/usb) uuids to actual posix paths.
    // sqlite needs a real absolute path, raw saf uris will just crash it.
    // volume.directory for android 11+ and falls back to /storage/uuid for older apis.
    val storageManager = context.getSystemService(STORAGE_SERVICE) as StorageManager
    val storageVolumes = storageManager.storageVolumes

    for (volume in storageVolumes) {
        if (volume.uuid == type) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val dir = volume.directory
                if (dir != null) {
                    return "${dir.absolutePath}/$path".removeSuffix("/")
                }
            }
            return "/storage/$type/$path".removeSuffix("/")
        }
    }

    return null
}

fun getAppColorScheme(
    preferences: AppPreferences,
    context: Context,
    isSystemInDarkTheme: Boolean
): ColorScheme {
    val darkColors =
        DarkColors.copy(
            background =
                if (preferences.useAmoledTheme) Color(0xFF000000) else DarkColors.background,
            surface = if (preferences.useAmoledTheme) Color(0xFF000000) else DarkColors.surface,
        )

    return when {
        preferences.useDynamicTheming && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (preferences.useSystemTheme) {
                if (isSystemInDarkTheme) {
                    dynamicDarkColorScheme(context)
                        .copy(
                            background =
                                if (preferences.useAmoledTheme) {
                                    Color(
                                        0xFF000000,
                                    )
                                } else {
                                    dynamicDarkColorScheme(context).background
                                },
                            surface =
                                if (preferences.useAmoledTheme) {
                                    Color(
                                        0xFF000000,
                                    )
                                } else {
                                    dynamicDarkColorScheme(
                                        context,
                                    )
                                        .surface
                                },
                        )
                } else {
                    dynamicLightColorScheme(
                        context,
                    )
                }
            } else {
                if (preferences.useDarkTheme) {
                    dynamicDarkColorScheme(
                        context,
                    )
                        .copy(
                            background =
                                if (preferences.useAmoledTheme) {
                                    Color(
                                        0xFF000000,
                                    )
                                } else {
                                    dynamicDarkColorScheme(context).background
                                },
                            surface =
                                if (preferences.useAmoledTheme) {
                                    Color(
                                        0xFF000000,
                                    )
                                } else {
                                    dynamicDarkColorScheme(
                                        context,
                                    )
                                        .surface
                                },
                        )
                } else {
                    dynamicLightColorScheme(context)
                }
            }
        }

        else ->
            if (preferences.useSystemTheme) {
                if (isSystemInDarkTheme) darkColors else LightColors
            } else {
                if (preferences.useDarkTheme) darkColors else LightColors
            }
    }
}
