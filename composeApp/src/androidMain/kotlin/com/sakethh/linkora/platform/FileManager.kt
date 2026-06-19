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
import com.sakethh.linkora.ui.utils.linkoraLog
import com.sakethh.linkora.utils.AndroidUIEvent
import com.sakethh.linkora.utils.Utils
import com.sakethh.linkora.utils.createNewFile
import com.sakethh.linkora.utils.getSystemEpochSeconds
import com.sakethh.linkora.utils.pushSnackbar
import com.sakethh.linkora.worker.SnapshotWorker
import getCertificateInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.io.ByteArrayInputStream
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate

actual class FileManager(private val context: Context) {

    private suspend fun writeToFile(
        exportLocation: String,
        exportFileType: ExportFileType,
        exportLocationType: ExportLocationType,
        byteArray: ByteArray,
        onCompletion: suspend (String) -> Unit
    ) {
        val (newFile, exportFileName) = createNewFile(
            context = context,
            exportLocation = exportLocation,
            exportFileType = exportFileType,
            exportLocationType = exportLocationType
        )
        val isSuccess = withContext(Dispatchers.IO) {
            try {
                newFile?.uri?.let { fileUri ->
                    context.contentResolver.openOutputStream(fileUri)?.use { outputStream ->
                        outputStream.write(byteArray)
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
        if (isSuccess) {
            onCompletion(exportFileName)
        } else {
            pushUIEvent(UIEvent.Type.ShowSnackbar("Failed to write file"))
        }
    }

    actual suspend fun writeRawExportStringToFile(
        exportLocation: String,
        exportFileType: ExportFileType,
        exportLocationType: ExportLocationType,
        rawExportString: RawExportString,
        onCompletion: suspend (String) -> Unit
    ) {
        writeToFile(
            exportLocation = exportLocation,
            exportFileType = exportFileType,
            exportLocationType = exportLocationType,
            byteArray = rawExportString.toByteArray(),
            onCompletion = onCompletion
        )
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
        return try {
            val event =
                AndroidUIEvent.androidUIEventChannel.first() as AndroidUIEvent.Type.PickedDirectory
            event.uri?.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
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

        val importEvent = try {
            AndroidUIEvent.androidUIEventChannel.first() as? AndroidUIEvent.Type.UriOfTheFileForImporting
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }

        val uri = importEvent?.uri ?: return null

        return withContext(Dispatchers.IO) {
            try {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    inputStream.bufferedReader().use { it.readText() }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
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

    actual suspend fun getSyncServerCertificate(onCompletion: (certInfo: String) -> Unit): ByteArray? {
        AndroidUIEvent.pushUIEvent(
            AndroidUIEvent.Type.ImportAFile(
                fileType = "*/*"
            )
        )
        var certInfo = ""
        return try {
            val (uri) = AndroidUIEvent.androidUIEventChannel.first() as AndroidUIEvent.Type.UriOfTheFileForImporting
            if (uri == null) {
                return null
            }
            linkoraLog("Importing the certificate")
            withContext(Dispatchers.IO) {
                context.contentResolver.openInputStream(uri)?.use { inputStream ->
                    val factory = CertificateFactory.getInstance("X.509")
                    val inputStreamBytes = inputStream.readBytes()
                    certInfo = getCertificateInfo(
                        factory = factory, inputStream = ByteArrayInputStream(inputStreamBytes)
                    )
                    (factory.generateCertificate(ByteArrayInputStream(inputStreamBytes)) as X509Certificate).encoded
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        } finally {
            onCompletion(certInfo)
        }
    }

    // these two operations aren't called on android
    actual suspend fun importFromJSONObj(fileLocation: String): Flow<Result<JSONExportSchema>> =
        emptyFlow()

    actual suspend fun importFromHTMLString(fileLocation: String): Flow<Result<String>> =
        emptyFlow()
}