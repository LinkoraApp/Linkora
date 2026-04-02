package com.sakethh.linkora.platform

import androidx.compose.runtime.Composable
import androidx.room3.RoomDatabaseConstructor
import com.sakethh.linkora.data.local.LocalDatabase
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.ExportFileType
import com.sakethh.linkora.domain.PermissionStatus
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.PreferenceKey
import com.sakethh.linkora.domain.RawExportString
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.model.JSONExportSchema
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.local.RefreshLinksRepo
import com.sakethh.linkora.ui.screens.settings.section.data.ExportLocationType
import io.ktor.client.HttpClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow

expect val showFollowSystemThemeOption: Boolean
expect val showDynamicThemingOption: Boolean

expect val platform: Platform

@Composable
expect fun PlatformSpecificBackHandler(init: () -> Unit = {})

expect val PlatformIODispatcher: CoroutineDispatcher


expect fun platformSpecificLogging(string: String)

expect class PermissionManager {
    suspend fun permittedToShowNotification(): PermissionStatus
    suspend fun isStorageAccessPermitted(): PermissionStatus
}

expect class Network {
    val standardClient: HttpClient

    fun closeSyncServerClient()

    fun getSyncServerClient(): HttpClient

    suspend fun configureSyncServerClient(bypassCertCheck: Boolean)
}

expect class FileManager {
    suspend fun writeRawExportStringToFile(
        exportLocation: String,
        exportFileType: ExportFileType,
        exportLocationType: ExportLocationType,
        rawExportString: RawExportString,
        onCompletion: suspend (String) -> Unit
    )


    suspend fun importFromJSONObj(
    ): Flow<Result<JSONExportSchema>>

    suspend fun importFromJSONObj(
        fileLocation: String
    ): Flow<Result<JSONExportSchema>>

    suspend fun importFromHTMLString(
    ): Flow<Result<String>>

    suspend fun importFromHTMLString(
        fileLocation: String
    ): Flow<Result<String>>

    suspend fun saveSyncServerCertificateInternally(
        certificate: ByteArray,
        onCompletion: () -> Unit
    )

    suspend fun getSyncServerCertificate(
        onCompletion: () -> Unit
    ): ByteArray?

    suspend fun exportSnapshotData(
        exportLocation: String,
        rawExportString: String,
        fileType: ExportFileType,
        onCompletion: suspend (String) -> Unit = {}
    )

    suspend fun pickADirectory(): String?

    fun getDefaultExportLocation(): String?

    suspend fun deleteAutoBackups(
        backupLocation: String,
        // maximum number of backups allowed to keep
        threshold: Int, onCompletion: (deletionCount: Int) -> Unit
    )

}

expect class NativeUtils {
    fun onShare(url: String)

    suspend fun onRefreshAllLinks(
        localLinksRepo: LocalLinksRepo,
        preferencesRepository: PreferencesRepository,
        refreshLinksRepo: RefreshLinksRepo
    )

    suspend fun isAnyRefreshingScheduled(): Flow<Boolean?>

    fun cancelRefreshingLinks()

    class DataSyncingNotificationService {
        fun showNotification()
        fun clearNotification()
    }

    fun onIconChange(allIconCodes: List<String>, newIconCode: String, onCompletion: () -> Unit)

    /**
     * THE WEB IMPLEMENTATION IS A HACK TO KEEP COMPILER HAPPY, THIS FUNCTION SHOULD NOT BE USED RANDOMLY

     * PLATFORM IMPLICATIONS:
     * - On JVM (Android/Desktop): This behaves like a traditional `runBlocking`. It will physically
     * block the current coroutine and wait for the [block] to complete before moving to the next line.
     * - On Web (Wasm/JS): True blocking is impossible on the browser's single event loop. This
     * behaves as a "fire-and-forget" asynchronous launch. The function returns instantly, and
     * any code written immediately after calling this will execute BEFORE the [block] finishes.
     * * Do NOT use this if subsequent synchronous code relies on the outcome of the [block] on Web.
     */
    fun <T> platformRunBlocking(block: suspend () -> T): T?
}

expect class PlatformPreference {
    suspend fun <T> writePreferenceValue(
        preferenceKey: PreferenceKey<T>,
        newValue: T,
    )

    suspend fun readAllPreferences(): AppPreferences

    suspend fun <T> readPreferenceValue(preferenceKey: PreferenceKey<T>): T?
}

@Suppress("KotlinNoActualForExpect")
expect object LocalDatabaseConstructor : RoomDatabaseConstructor<LocalDatabase> {
    override fun initialize(): LocalDatabase
}