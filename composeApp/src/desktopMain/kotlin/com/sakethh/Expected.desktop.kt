package com.sakethh

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import com.sakethh.linkora.RefreshAllLinksService
import com.sakethh.linkora.common.Localization
import com.sakethh.linkora.common.utils.Constants
import com.sakethh.linkora.common.utils.duplicate
import com.sakethh.linkora.common.utils.getLocalizedString
import com.sakethh.linkora.common.utils.ifNot
import com.sakethh.linkora.common.utils.isNotNull
import com.sakethh.linkora.common.utils.replaceFirstPlaceHolderWith
import com.sakethh.linkora.data.local.LocalDatabase
import com.sakethh.linkora.domain.ExportFileType
import com.sakethh.linkora.domain.ImportFileType
import com.sakethh.linkora.domain.LinkoraPlaceHolder
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.RawExportString
import com.sakethh.linkora.domain.model.Reminder
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.linkoraLog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import java.awt.FileDialog
import java.awt.Frame
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths
import java.text.DateFormat
import java.time.LocalDateTime
import java.util.Date

actual val showFollowSystemThemeOption: Boolean = true
actual val BUILD_FLAVOUR: String = "desktop"
actual val platform: @Composable () -> Platform = {
    Platform.Desktop
}
private val linkoraSpecificFolder = System.getProperty("user.home").run {
    val appDataDir = File(this, ".linkora")
    if (appDataDir.exists().not()) {
        appDataDir.mkdirs()
    }
    appDataDir
}

actual val localDatabase: LocalDatabase? =
    File(linkoraSpecificFolder, "${LocalDatabase.NAME}.db").run {
        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("CREATE TABLE IF NOT EXISTS `snapshot` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `content` TEXT NOT NULL)")
            }
        }
        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(connection: SQLiteConnection) {
                connection.execSQL("CREATE TABLE IF NOT EXISTS `reminder` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `linkId` INTEGER NOT NULL, `title` TEXT NOT NULL, `description` TEXT NOT NULL, `reminderType` TEXT NOT NULL, `reminderMode` TEXT NOT NULL, `date` TEXT NOT NULL, `time` TEXT NOT NULL, `linkView` TEXT NOT NULL)")
            }
        }
        Room.databaseBuilder<LocalDatabase>(name = this.absolutePath)
            .setDriver(BundledSQLiteDriver()).addMigrations(MIGRATION_9_10,MIGRATION_10_11).build()
    }
actual val linkoraDataStore: DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath {
    linkoraSpecificFolder.resolve(Constants.DATA_STORE_NAME).absolutePath.toPath()
}
actual val poppinsFontFamily: FontFamily = com.sakethh.linkora.ui.theme.poppinsFontFamily
actual val showDynamicThemingOption: Boolean = false

actual suspend fun writeRawExportStringToFile(
    exportFileType: ExportFileType,
    rawExportString: RawExportString,
    onCompletion: suspend (String) -> Unit
) {
    val userHomeDir = System.getProperty("user.home")
    val exportsFolder = File(userHomeDir, "/Documents/Linkora/Exports")

    exportsFolder.exists().ifNot {
        exportsFolder.mkdirs()
    }

    val exportFileName = "LinkoraExport-${
        DateFormat.getDateTimeInstance().format(Date()).replace(":", "").replace(" ", "")
    }.${if (exportFileType == ExportFileType.HTML) "html" else "json"}"

    val exportFilePath = Paths.get(exportsFolder.absolutePath, exportFileName)

    withContext(Dispatchers.IO) {
        Files.write(exportFilePath, rawExportString.toByteArray())
    }
    onCompletion(exportFileName)
    linkoraLog(exportFileName)
}

actual suspend fun isStorageAccessPermittedOnAndroid(): Boolean = false

actual suspend fun pickAValidFileForImporting(
    importFileType: ImportFileType, onStart: () -> Unit
): File? {
    val fileDialog = FileDialog(
        Frame(),
        Localization.Key.SelectAValidFile.getLocalizedString()
            .replaceFirstPlaceHolderWith(importFileType.name),
        FileDialog.LOAD
    )
    fileDialog.isVisible = true
    val sourceFile = File(fileDialog.directory, fileDialog.file).duplicate()
    return if (sourceFile.isNotNull() && sourceFile!!.extension == importFileType.name.lowercase()) {
        onStart()
        sourceFile
    } else if (sourceFile.isNotNull() && sourceFile!!.extension != importFileType.name.lowercase()) {
        UIEvent.pushUIEvent(
            UIEvent.Type.ShowSnackbar(
                Localization.Key.FileTypeNotSupportedOnDesktopImport.getLocalizedString()
                    .replace(LinkoraPlaceHolder.First.value, sourceFile.extension)
                    .replace(LinkoraPlaceHolder.Second.value, importFileType.name)
            )
        )
        null
    } else null
}

actual fun onShare(url: String) = Unit

actual suspend fun onRefreshAllLinks(
    localLinksRepo: LocalLinksRepo, preferencesRepository: PreferencesRepository
) {
    RefreshAllLinksService.invoke(localLinksRepo)
}

actual fun cancelRefreshingLinks() {
    RefreshAllLinksService.cancel()
}

actual suspend fun isAnyRefreshingScheduled(): Flow<Boolean?> {
    return emptyFlow()
}

@Composable
actual fun PlatformSpecificBackHandler(init: () -> Unit) = Unit


actual suspend fun permittedToShowNotification(): Boolean = false

actual fun platformSpecificLogging(string: String) {
    println("Linkora Log : $string")
}


actual class DataSyncingNotificationService actual constructor() {
    actual fun showNotification() = Unit
    actual fun clearNotification() = Unit
}

actual suspend fun exportSnapshotData(
    rawExportString: String, fileType: ExportFileType, onCompletion: suspend (String) -> Unit
) {
    writeRawExportStringToFile(
        exportFileType = fileType, rawExportString = rawExportString, onCompletion = onCompletion
    )
}

actual suspend fun scheduleAReminder(
    reminder: Reminder, graphicsLayer: GraphicsLayer, onCompletion: suspend (base64String: String) -> Unit
) = Unit