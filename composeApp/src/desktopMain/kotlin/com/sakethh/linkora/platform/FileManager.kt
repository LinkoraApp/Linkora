package com.sakethh.linkora.platform

import com.sakethh.linkora.Localization
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.domain.ExportFileType
import com.sakethh.linkora.domain.FileType
import com.sakethh.linkora.domain.LinkoraPlaceHolder
import com.sakethh.linkora.domain.RawExportString
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.asJSONExportSchema
import com.sakethh.linkora.domain.model.JSONExportSchema
import com.sakethh.linkora.domain.model.PanelForJSONExportSchema
import com.sakethh.linkora.domain.model.legacy.LegacyExportSchema
import com.sakethh.linkora.linkoraSpecificFolder
import com.sakethh.linkora.ui.screens.settings.section.data.ExportLocationType
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.linkoraLog
import com.sakethh.linkora.utils.Utils
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.getSystemEpochSeconds
import com.sakethh.linkora.utils.ifNot
import com.sakethh.linkora.utils.replaceFirstPlaceHolderWith
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.awt.FileDialog
import java.awt.Frame
import java.io.ByteArrayInputStream
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

actual class FileManager {

    actual suspend fun writeRawExportStringToFile(
        exportLocation: String,
        exportFileType: ExportFileType,
        exportLocationType: ExportLocationType,
        rawExportString: RawExportString,
        onCompletion: suspend (String) -> Unit
    ) {

        val exportsFolder = File(exportLocation)

        exportsFolder.exists().ifNot {
            exportsFolder.mkdirs()
        }

        // kinda repeated in Expected.android, but alright
        val simpleDateFormat = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US)
        val timestamp = simpleDateFormat.format(Date())
        val exportFileName =
            "${if (exportLocationType == ExportLocationType.EXPORT) "LinkoraExport" else "LinkoraSnapshot"}-$timestamp.${if (exportFileType == ExportFileType.HTML) "html" else "json"}"

        val exportFilePath = Paths.get(exportsFolder.absolutePath, exportFileName)

        withContext(Dispatchers.IO) {
            Files.write(exportFilePath, rawExportString.toByteArray())
        }
        onCompletion(exportFileName)
        linkoraLog(exportFileName)
    }

    actual suspend fun saveSyncServerCertificateInternally(
        certificate: ByteArray, onCompletion: () -> Unit
    ) {
        linkoraSpecificFolder.resolve("sync-server-cert.cer").writeBytes(certificate)
        onCompletion()
    }

    actual suspend fun exportSnapshotData(
        exportLocation: String,
        rawExportString: String,
        fileType: ExportFileType,
        onCompletion: suspend (String) -> Unit
    ) {
        writeRawExportStringToFile(
            exportLocation = exportLocation,
            exportFileType = fileType,
            rawExportString = rawExportString,
            onCompletion = onCompletion,
            exportLocationType = ExportLocationType.SNAPSHOT
        )
    }

    actual suspend fun pickADirectory(): String? {
        return "https://music.youtube.com/watch?v=LWUgT34GYhU"
    }

    actual fun getDefaultExportLocation(): String? {
        val userHomeDir = System.getProperty("user.home")
        return File(userHomeDir, "/Documents/Linkora/Exports").absolutePath
    }

    actual suspend fun deleteAutoBackups(
        backupLocation: String, threshold: Int, onCompletion: (Int) -> Unit
    ) {
        try {
            withContext(Dispatchers.IO) {
                File(backupLocation).listFiles {
                    it.nameWithoutExtension.startsWith("LinkoraSnapshot-")
                }?.let { snapshots ->
                    val snapshotsCount = snapshots.count()
                    if (snapshotsCount > threshold) {
                        snapshots.sortBy {
                            it.lastModified()
                        }
                        snapshots.take(snapshotsCount - threshold).apply {
                            forEach {
                                it.delete()
                            }
                            onCompletion(this.count())
                        }
                    } else {
                        onCompletion(0)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            UIEvent.pushUIEvent(UIEvent.Type.ShowSnackbar(e.message.toString()))
        }
    }

    private suspend fun getFile(fileType: FileType): File? {
        return try {
            val fileDialog = FileDialog(
                Frame(),
                Localization.Key.SelectAValidFile.getLocalizedString()
                    .replaceFirstPlaceHolderWith(fileType.name),
                FileDialog.LOAD
            )
            fileDialog.isVisible = true
            val sourceFile = File(fileDialog.directory, fileDialog.file)
            if (sourceFile.extension == fileType.name.lowercase()) {
                sourceFile
            } else if (sourceFile.extension != fileType.name.lowercase()) {
                UIEvent.pushUIEvent(
                    UIEvent.Type.ShowSnackbar(
                        Localization.Key.FileTypeNotSupportedOnDesktopImport.getLocalizedString()
                            .replace(LinkoraPlaceHolder.First.value, sourceFile.extension)
                            .replace(LinkoraPlaceHolder.Second.value, fileType.name)
                    )
                )
                null
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private suspend fun getFile(fileType: FileType, fileLocation: String): File? {
        return try {
            val sourceFile = File(fileLocation)
            if (sourceFile.extension == fileType.name.lowercase()) {
                sourceFile
            } else if (sourceFile.extension != fileType.name.lowercase()) {
                UIEvent.pushUIEvent(
                    UIEvent.Type.ShowSnackbar(
                        Localization.Key.FileTypeNotSupportedOnDesktopImport.getLocalizedString()
                            .replace(LinkoraPlaceHolder.First.value, sourceFile.extension)
                            .replace(LinkoraPlaceHolder.Second.value, fileType.name)
                    )
                )
                null
            } else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    actual suspend fun importFromJSONObj(): Flow<Result<JSONExportSchema>> = flow {
        val importFile =
            getFile(FileType.JSON) ?: return@flow emit(Result.Failure("Importing Failed."))

        getJsonObj(importFile, importFile.name)
    }

    private suspend fun FlowCollector<Result<JSONExportSchema>>.getJsonObj(
        file: File, fileName: String
    ) {
        try {
            emit(Result.Loading(message = "Starting data import from JSON file: $fileName"))

            val currentSystemEpochSeconds = getSystemEpochSeconds()

            val jsonContent = run {
                val strBuilder = StringBuilder()
                file.bufferedReader().forEachLine {
                    strBuilder.append(it)
                }
                strBuilder.toString()
            }

            val basedOnNewExportSchema =
                jsonContent.substringAfter("\"").substringBefore("\"") == "schemaVersion"

            emit(
                Result.Loading(
                    message = if (!basedOnNewExportSchema) {
                        "This JSON file is based on the legacy export schema."
                    } else {
                        "This JSON file is based on schema version."
                    }
                )
            )

            emit(Result.Loading(message = "Reading and deserializing JSON file: $fileName"))

            val jsonObj = if (!basedOnNewExportSchema) {
                Json.decodeFromString<LegacyExportSchema>(jsonContent).asJSONExportSchema(
                    DependencyContainer.preferencesRepo.getPreferences().primaryJsoupUserAgent
                )
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
        } catch (e: Exception) {
            emit(Result.Failure(e.message ?: "Import failed"))
        }
    }

    actual suspend fun importFromHTMLString(): Flow<Result<String>> = flow {
        val file = getFile(FileType.HTML) ?: return@flow emit(Result.Failure("Importing Failed."))
        getHtmlStr(file)
    }

    private suspend fun FlowCollector<Result<String>>.getHtmlStr(file: File) {
        try {
            val htmlStr = StringBuilder()
            val fileName = file.name
            emit(Result.Loading(message = "Reading the file $fileName"))
            file.bufferedReader().forEachLine {
                htmlStr.append(it)
            }
            emit(Result.Loading(message = "Read the file $fileName"))
            emit(Result.Success(htmlStr.toString()))
        } catch (e: Exception) {
            emit(Result.Failure(e.message ?: "Import failed"))
        }
    }

    actual suspend fun importFromJSONObj(fileLocation: String): Flow<Result<JSONExportSchema>> =
        flow {
            val importFile =
                getFile(fileType = FileType.JSON, fileLocation = fileLocation) ?: return@flow emit(
                    Result.Failure("Importing Failed.")
                )

            getJsonObj(importFile, importFile.name)
        }

    actual suspend fun importFromHTMLString(fileLocation: String): Flow<Result<String>> = flow {
        val file =
            getFile(fileType = FileType.HTML, fileLocation = fileLocation) ?: return@flow emit(
                Result.Failure("Importing Failed.")
            )
        getHtmlStr(file)
    }

    actual suspend fun getSyncServerCertificate(onCompletion: () -> Unit): ByteArray? {
        return try {
            val fileDialog = FileDialog(
                Frame(),
                Localization.Key.SelectAValidFile.getLocalizedString()
                    .replaceFirstPlaceHolderWith("CER"),
                FileDialog.LOAD
            )
            fileDialog.isVisible = true
            val sourceFile = File(fileDialog.directory, fileDialog.file)
            if (sourceFile.extension != "cer") {
                UIEvent.pushUIEvent(
                    UIEvent.Type.ShowSnackbar(
                        Localization.Key.FileTypeNotSupportedOnDesktopImport.getLocalizedString()
                            .replace(LinkoraPlaceHolder.First.value, sourceFile.extension)
                            .replace(LinkoraPlaceHolder.Second.value, "cer")
                    )
                )
                return null
            }
            val factory = CertificateFactory.getInstance("X.509")
            val inputStream = ByteArrayInputStream(sourceFile.readBytes())
            (factory.generateCertificate(inputStream) as X509Certificate).encoded
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}