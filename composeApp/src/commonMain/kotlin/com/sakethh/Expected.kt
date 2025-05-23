package com.sakethh

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.text.font.FontFamily
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.sakethh.linkora.data.local.LocalDatabase
import com.sakethh.linkora.domain.ExportFileType
import com.sakethh.linkora.domain.ImportFileType
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.RawExportString
import com.sakethh.linkora.domain.model.Reminder
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import java.io.File

expect val showFollowSystemThemeOption: Boolean
expect val showDynamicThemingOption: Boolean
expect val BUILD_FLAVOUR: String

expect val platform: @Composable () -> Platform

expect val localDatabase: LocalDatabase?
expect val linkoraDataStore: DataStore<Preferences>

expect val poppinsFontFamily: FontFamily

expect suspend fun writeRawExportStringToFile(
    exportFileType: ExportFileType,
    rawExportString: RawExportString,
    onCompletion: suspend (String) -> Unit
)

expect suspend fun isStorageAccessPermittedOnAndroid(): Boolean

expect suspend fun pickAValidFileForImporting(
    importFileType: ImportFileType, onStart: () -> Unit
): File?

expect fun onShare(url: String)

expect suspend fun onRefreshAllLinks(
    localLinksRepo: LocalLinksRepo, preferencesRepository: PreferencesRepository
)

expect suspend fun isAnyRefreshingScheduled(): Flow<Boolean?>

expect fun cancelRefreshingLinks()

@Composable
expect fun PlatformSpecificBackHandler(init: () -> Unit = {})

expect suspend fun permittedToShowNotification(): Boolean

expect fun platformSpecificLogging(string: String)

expect class DataSyncingNotificationService() {
    fun showNotification()
    fun clearNotification()
}

expect suspend fun exportSnapshotData(
    rawExportString: String, fileType: ExportFileType, onCompletion: suspend (String) -> Unit = {}
)

// although this is android specific (for now), but if in case linkora goes ios, the foundation for this feature already gets done in the common module, so this is completely fine
expect suspend fun scheduleAReminder(
    reminder: Reminder,
    // bitmap conversion is a mess in the common module
    // since Skia's implementation doesn't work on android,
    // so this is the only way i can think of for now
    graphicsLayer: GraphicsLayer, onCompletion: suspend (base64String: String) -> Unit
)

expect fun canScheduleAlarms(): Boolean