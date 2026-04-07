package com.sakethh.linkora.platform

import android.content.Context
import androidx.core.net.toUri
import androidx.documentfile.provider.DocumentFile
import androidx.work.Data
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.domain.ExportFileType
import com.sakethh.linkora.domain.FileType
import com.sakethh.linkora.domain.ImportFileType
import com.sakethh.linkora.domain.RawExportString
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.asJSONExportSchema
import com.sakethh.linkora.domain.model.JSONExportSchema
import com.sakethh.linkora.domain.model.PanelForJSONExportSchema
import com.sakethh.linkora.domain.model.Snapshot
import com.sakethh.linkora.domain.model.legacy.LegacyExportSchema
import com.sakethh.linkora.ui.screens.settings.section.data.ExportLocationType
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.utils.AndroidUIEvent
import com.sakethh.linkora.utils.Utils
import com.sakethh.linkora.utils.getSystemEpochSeconds
import com.sakethh.linkora.utils.pushSnackbar
import com.sakethh.linkora.worker.SnapshotWorker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.coroutines.coroutineContext
import kotlin.coroutines.resume

actual class FileManager(private val context: Context) {
    actual suspend fun writeRawExportStringToFile(
        exportLocation: String,
        exportFileType: ExportFileType,
        exportLocationType: ExportLocationType,
        rawExportString: RawExportString,
        onCompletion: suspend (String) -> Unit
    ) {

        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss-SSS", Locale.US)
        val timestamp = simpleDateFormat.format(Date())
        val exportFileName =
            "${if (exportLocationType == ExportLocationType.EXPORT) "LinkoraExport" else "LinkoraSnapshot"}-$timestamp.${if (exportFileType == ExportFileType.HTML) "html" else "json"}"

        val directoryUri = exportLocation.toUri()
        val directory = DocumentFile.fromTreeUri(context, directoryUri)
        val newFile = directory?.createFile(
            if (exportFileType == ExportFileType.HTML) "text/html" else "application/json",
            exportFileName
        )
        newFile?.uri?.let { fileUri ->
            try {
                context.contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                    outputStream.write(rawExportString.toByteArray())
                }
                onCompletion(exportFileName)
            } catch (e: Exception) {
                withContext(coroutineContext) {
                    pushUIEvent(UIEvent.Type.ShowSnackbar(e.message.toString()))
                }
                e.printStackTrace()
            }
        }
    }


    actual suspend fun saveSyncServerCertificateInternally(
        certificate: ByteArray, onCompletion: () -> Unit
    ) {
        context.filesDir.resolve("sync-server-cert.cer").writeBytes(certificate)
        onCompletion()
    }

    actual suspend fun exportSnapshotData(
        exportLocation: String,
        rawExportString: String,
        fileType: ExportFileType,
        onCompletion: suspend (String) -> Unit
    ) {
        val snapshotWorker = OneTimeWorkRequestBuilder<SnapshotWorker>()
        val rawExportStringID: Long =
            DependencyContainer.snapshotRepo.addASnapshot(Snapshot(content = rawExportString))

        val parameters =
            Data.Builder().putLong(key = "rawExportStringID", value = rawExportStringID)
                .putString(key = "fileType", value = fileType.name).build()
        snapshotWorker.setInputData(parameters)
        WorkManager.getInstance(context).enqueue(snapshotWorker.build())
    }

    actual suspend fun pickADirectory(): String? {
        AndroidUIEvent.pushUIEvent(AndroidUIEvent.Type.PickADirectory)
        return suspendCancellableCoroutine { continuation ->
            val listenerJob = CoroutineScope(continuation.context).launch {
                val (uri) = AndroidUIEvent.androidUIEventChannel.first() as AndroidUIEvent.Type.PickedDirectory
                try {
                    continuation.resume(uri?.toString())
                } catch (e: Exception) {
                    e.printStackTrace()
                    continuation.cancel()
                }
            }
            continuation.invokeOnCancellation {
                listenerJob.cancel()
            }
        }
    }

    actual suspend fun deleteAutoBackups(
        backupLocation: String, threshold: Int, onCompletion: (Int) -> Unit
    ) {
        try {
            withContext(Dispatchers.IO) {
                DocumentFile.fromTreeUri(context, backupLocation.toUri())?.listFiles()?.filter {
                    it.name?.startsWith("LinkoraSnapshot-") == true
                }?.let { snapshots ->
                    val snapshotsCount = snapshots.count()
                    if (snapshotsCount > threshold) {
                        snapshots.sortedBy {
                            it.lastModified()
                        }.take(snapshotsCount - threshold).apply {
                            forEach {
                                it.delete()
                            }
                            onCompletion(count())
                        }
                    } else {
                        onCompletion(0)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            e.pushSnackbar()
        }
    }

    suspend fun importFile(importFileType: ImportFileType): String? {
        AndroidUIEvent.pushUIEvent(
            AndroidUIEvent.Type.ImportAFile(
                fileType = when (importFileType) {
                    ImportFileType.JSON -> "application/json"
                    ImportFileType.HTML -> "text/html"
                    else -> "*/*"
                }
            )
        )
        return suspendCancellableCoroutine { continuation ->
            val listenerJob = CoroutineScope(continuation.context).launch {
                try {
                    val (uri) = AndroidUIEvent.androidUIEventChannel.first() as AndroidUIEvent.Type.UriOfTheFileForImporting
                    if (uri == null) {
                        // if picking the file didn't go as expected, then just return null
                        // we can throw and catch and then resume with null but this is aight
                        continuation.resume(null)
                        return@launch
                    }
                    val dataStr = StringBuilder()
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader().forEachLine { line ->
                            dataStr.append(line)
                        }
                    }
                    continuation.resume(dataStr.toString())
                } catch (e: Exception) {
                    e.printStackTrace()
                    continuation.cancel()
                }
            }
            continuation.invokeOnCancellation {
                listenerJob.cancel()
            }
        }
    }

    actual suspend fun importFromJSONObj(): Flow<Result<JSONExportSchema>> = flow {
        val jsonContent =
            importFile(FileType.JSON) ?: return@flow emit(Result.Failure("Importing Failed."))

        emit(Result.Loading(message = "Reading and deserializing JSON file"))
        val currentSystemEpochSeconds = getSystemEpochSeconds()

        val basedOnNewExportSchema =
            jsonContent.substringAfter("\"").substringBefore("\"") == "schemaVersion"

        emit(
            Result.Loading(
                message = if (!basedOnNewExportSchema) {
                    "This JSON file is based on the legacy schema."
                } else {
                    "This JSON file is based on latest schema."
                }
            )
        )

        val jsonObj = if (!basedOnNewExportSchema) {
            Json.decodeFromString<LegacyExportSchema>(jsonContent)
                .asJSONExportSchema(userAgent = DependencyContainer.preferencesRepo.getPreferences().primaryJsoupUserAgent)
        } else Utils.json.decodeFromString<JSONExportSchema>(jsonContent).run {
            JSONExportSchema(schemaVersion = schemaVersion, links = links.map {
                it.copy(remoteId = null, lastModified = currentSystemEpochSeconds)
            }, folders = folders.map {
                it.copy(remoteId = null, lastModified = currentSystemEpochSeconds)
            }, panels = PanelForJSONExportSchema(panels = panels.panels.map {
                it.copy(remoteId = null, lastModified = currentSystemEpochSeconds)
            }, panelFolders = panels.panelFolders.map {
                it.copy(remoteId = null, lastModified = currentSystemEpochSeconds)
            }), tags = tags.map {
                it.copy(
                    remoteId = null, lastModified = currentSystemEpochSeconds
                )
            }, linkTags = linkTags.map {
                it.copy(
                    remoteId = null, lastModified = currentSystemEpochSeconds
                )
            })
        }
        emit(Result.Success(jsonObj))
    }

    actual suspend fun importFromHTMLString(): Flow<Result<String>> = flow {
        val importContent =
            importFile(FileType.HTML) ?: return@flow emit(Result.Failure("Importing Failed."))

        emit(Result.Loading(message = "Reading the file"))
        emit(Result.Success(importContent))
    }

    actual suspend fun getSyncServerCertificate(onCompletion: () -> Unit): ByteArray? {
        AndroidUIEvent.pushUIEvent(
            AndroidUIEvent.Type.ImportAFile(
                fileType = "*/*"
            )
        )
        return try {
            val (uri) = AndroidUIEvent.androidUIEventChannel.first() as AndroidUIEvent.Type.UriOfTheFileForImporting
            if (uri == null) {
                return null
            }
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val factory = CertificateFactory.getInstance("X.509")
                val inputStream = ByteArrayInputStream(inputStream.readBytes())
                (factory.generateCertificate(inputStream) as X509Certificate).encoded
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    // these two operations aren't called on android
    actual suspend fun importFromJSONObj(fileLocation: String): Flow<Result<JSONExportSchema>> =
        emptyFlow()

    actual suspend fun importFromHTMLString(fileLocation: String): Flow<Result<String>> =
        emptyFlow()
}