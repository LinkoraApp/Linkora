package com.sakethh.linkora.platform

import androidx.compose.runtime.Composable
import com.sakethh.linkora.Localization
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
import com.sakethh.linkora.ui.utils.linkoraLog
import com.sakethh.linkora.utils.getLocalizedString
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.js.Js
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.browser.localStorage
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import org.w3c.dom.get
import org.w3c.dom.set

actual val showFollowSystemThemeOption: Boolean = true
actual val showDynamicThemingOption: Boolean = false

actual val platform: Platform = Platform.Web

actual class PermissionManager {
    actual suspend fun permittedToShowNotification(): PermissionStatus =
        PermissionStatus.NeedsRequest

    actual suspend fun isStorageAccessPermitted(): PermissionStatus = PermissionStatus.NeedsRequest
}

actual fun platformSpecificLogging(string: String) {
    println(string)
}

@Composable
actual fun PlatformSpecificBackHandler(init: () -> Unit) = Unit

actual class FileManager {
    actual suspend fun writeRawExportStringToFile(
        exportLocation: String,
        exportFileType: ExportFileType,
        exportLocationType: ExportLocationType,
        rawExportString: RawExportString,
        onCompletion: suspend (String) -> Unit
    ) = Unit


    actual suspend fun exportSnapshotData(
        exportLocation: String,
        rawExportString: String,
        fileType: ExportFileType,
        onCompletion: suspend (String) -> Unit
    ) = Unit

    actual suspend fun pickADirectory(): String? = null

    actual fun getDefaultExportLocation(): String? = null

    actual suspend fun deleteAutoBackups(
        backupLocation: String, threshold: Int, onCompletion: (deletionCount: Int) -> Unit
    ) = Unit


    actual suspend fun saveSyncServerCertificateInternally(
        certificate: ByteArray, onCompletion: () -> Unit
    ) = Unit

    actual suspend fun importFromJSONObj(): Flow<Result<JSONExportSchema>> = emptyFlow()

    actual suspend fun importFromHTMLString(): Flow<Result<String>> = emptyFlow()
    actual suspend fun importFromJSONObj(fileLocation: String): Flow<Result<JSONExportSchema>> =
        emptyFlow()

    actual suspend fun importFromHTMLString(fileLocation: String): Flow<Result<String>> =
        emptyFlow()

    actual suspend fun getSyncServerCertificate(onCompletion: () -> Unit): ByteArray? = null

}

actual class NativeUtils {
    actual fun onShare(url: String) = Unit

    actual suspend fun onRefreshAllLinks(
        localLinksRepo: LocalLinksRepo,
        preferencesRepository: PreferencesRepository,
        refreshLinksRepo: RefreshLinksRepo
    ): Unit = RefreshAllLinksService.invoke(localLinksRepo)

    actual suspend fun isAnyRefreshingScheduled(): Flow<Boolean?> = flowOf(false)

    actual fun cancelRefreshingLinks(): Unit = RefreshAllLinksService.cancel()

    actual fun onIconChange(
        allIconCodes: List<String>, newIconCode: String, onCompletion: () -> Unit
    ) = Unit

    actual class DataSyncingNotificationService {
        actual fun showNotification() = Unit

        actual fun clearNotification() = Unit
    }

    private val mainScope = MainScope()

    /**
     * THIS FUNCTION SHOULD NOT BE USED RANDOMLY

     * True blocking is impossible on the browser's single event loop. This
     * behaves as a "fire-and-forget" asynchronous launch. The function returns instantly, and
     * any code written immediately after calling this will execute BEFORE the [block] finishes.
     *
     * Do NOT use this if subsequent synchronous code relies on the outcome of the [block].
     */
    actual fun <T> platformRunBlocking(block: suspend () -> T): T? {
        mainScope.launch {
            block()
        }
        return null
    }
}

actual val PlatformIODispatcher: CoroutineDispatcher = Dispatchers.Default

actual object Network {

    private fun <T : HttpClientEngineConfig> HttpClientConfig<T>.installLogger() {
        install(Logging) {
            logger = object : Logger {
                override fun log(message: String) {
                    linkoraLog("HTTP CLIENT:\n$message")
                }
            }
            level = LogLevel.ALL
        }
    }

    private fun <T : HttpClientEngineConfig> HttpClientConfig<T>.installContentNegotiation() {
        val jsonConfig = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
        install(ContentNegotiation) {
            json(jsonConfig)
        }
    }

    actual val standardClient = HttpClient(Js) {
        installContentNegotiation()
        installLogger()
    }

    private var syncServerClient: HttpClient? = null

    actual fun getSyncServerClient(): HttpClient {
        return syncServerClient
            ?: error(Localization.Key.SyncServerConfigurationError.getLocalizedString())
    }

    actual fun closeSyncServerClient() {
        syncServerClient?.close()
        syncServerClient = null
    }

    actual suspend fun configureSyncServerClient(bypassCertCheck: Boolean) {
        if (syncServerClient != null) return

        syncServerClient = HttpClient(Js) {
            install(HttpTimeout) {
                this.socketTimeoutMillis = 240_000
                this.connectTimeoutMillis = 240_000
                this.requestTimeoutMillis = 240_000
            }

            installContentNegotiation()
            installLogger()

            install(WebSockets) {
                pingIntervalMillis = 20_000
            }
        }
    }
}

actual object PlatformPreference {
    actual suspend fun <T> writePreferenceValue(
        preferenceKey: PreferenceKey<T>, newValue: T
    ) {
        when (preferenceKey) {
            is PreferenceKey.BooleanPreferencesKey -> {
                localStorage[preferenceKey.key] = newValue.toString()
            }

            is PreferenceKey.LongPreferencesKey -> {
                localStorage[preferenceKey.key] = newValue.toString()
            }

            is PreferenceKey.StringPreferencesKey -> {
                localStorage[preferenceKey.key] = newValue.toString()
            }

            is PreferenceKey.IntPreferencesKey -> localStorage[preferenceKey.key] =
                newValue.toString()
        }
    }

    actual suspend fun <T> readPreferenceValue(preferenceKey: PreferenceKey<T>): T? =
        when (preferenceKey) {
            is PreferenceKey.BooleanPreferencesKey -> {
                localStorage[preferenceKey.key]
            }

            is PreferenceKey.LongPreferencesKey -> {
                localStorage[preferenceKey.key]
            }

            is PreferenceKey.StringPreferencesKey -> {
                localStorage[preferenceKey.key]
            }

            is PreferenceKey.IntPreferencesKey -> localStorage[preferenceKey.key]
        } as T?
}