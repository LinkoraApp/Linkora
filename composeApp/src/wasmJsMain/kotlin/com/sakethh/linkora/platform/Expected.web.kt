package com.sakethh.linkora.platform

import RefreshAllLinksService
import androidx.compose.runtime.Composable
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.ExportFileType
import com.sakethh.linkora.domain.PermissionStatus
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.PreferenceKey
import com.sakethh.linkora.domain.RawExportString
import com.sakethh.linkora.domain.RefreshLinkType
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.SnapshotFormat
import com.sakethh.linkora.domain.SyncType
import com.sakethh.linkora.domain.model.JSONExportSchema
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.local.RefreshLinksRepo
import com.sakethh.linkora.ui.domain.AppIconCode
import com.sakethh.linkora.ui.domain.Font
import com.sakethh.linkora.ui.domain.Layout
import com.sakethh.linkora.ui.domain.SortingType
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.settings.section.data.ExportLocationType
import com.sakethh.linkora.ui.utils.linkoraLog
import com.sakethh.linkora.utils.Constants
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
        localStorage[preferenceKey.key] = newValue.toString()
    }

    @Suppress("UNCHECKED_CAST")
    actual suspend fun <T> readPreferenceValue(preferenceKey: PreferenceKey<T>): T? {
        val rawValue = localStorage.getItem(preferenceKey.key) ?: return null

        return when (preferenceKey) {
            is PreferenceKey.BooleanPreferencesKey -> rawValue.toBooleanStrictOrNull()
            is PreferenceKey.LongPreferencesKey -> rawValue.toLongOrNull()
            is PreferenceKey.StringPreferencesKey -> rawValue
            is PreferenceKey.IntPreferencesKey -> rawValue.toIntOrNull()
        } as T?
    }

    actual suspend fun readAllPreferences(): AppPreferences {
        return AppPreferences(
            useDarkTheme = localStorage.getItem(AppPreferences.DARK_THEME.key)
                ?.toBooleanStrictOrNull() ?: true,
            useSystemTheme = localStorage.getItem(AppPreferences.FOLLOW_SYSTEM_THEME.key)
                ?.toBooleanStrictOrNull() ?: false,
            useAmoledTheme = localStorage.getItem(AppPreferences.AMOLED_THEME_STATE.key)
                ?.toBooleanStrictOrNull() ?: false,
            useDynamicTheming = localStorage.getItem(AppPreferences.DYNAMIC_THEMING.key)
                ?.toBooleanStrictOrNull() ?: false,
            isAutoDetectTitleForLinksEnabled = localStorage.getItem(AppPreferences.AUTO_DETECT_TITLE_FOR_LINK.key)
                ?.toBooleanStrictOrNull() ?: false,
            showAssociatedImageInLinkMenu = localStorage.getItem(AppPreferences.ASSOCIATED_IMAGES_IN_LINK_MENU_VISIBILITY.key)
                ?.toBooleanStrictOrNull() ?: false,
            isHomeScreenEnabled = localStorage.getItem(AppPreferences.HOME_SCREEN_VISIBILITY.key)
                ?.toBooleanStrictOrNull() ?: true,
            useRemoteStrings = localStorage.getItem(AppPreferences.USE_REMOTE_LANGUAGE_STRINGS.key)
                ?.toBooleanStrictOrNull() ?: false,
            selectedSortingType = localStorage.getItem(AppPreferences.SORTING_PREFERENCE.key)
                ?: SortingType.NEW_TO_OLD.name,
            primaryJsoupUserAgent = localStorage.getItem(AppPreferences.JSOUP_USER_AGENT.key)
                ?: Constants.DEFAULT_USER_AGENT,
            localizationServerURL = localStorage.getItem(AppPreferences.LOCALIZATION_SERVER_URL.key)
                ?: Constants.LOCALIZATION_SERVER_URL,
            preferredAppLanguageName = localStorage.getItem(AppPreferences.APP_LANGUAGE_NAME.key)
                ?: "English",
            preferredAppLanguageCode = localStorage.getItem(AppPreferences.APP_LANGUAGE_CODE.key)
                ?: "en",
            selectedLinkLayout = localStorage.getItem(AppPreferences.CURRENTLY_SELECTED_LINK_VIEW.key)
                ?: Layout.REGULAR_LIST_VIEW.name,
            showTitleInLinkGridView = localStorage.getItem(AppPreferences.TITLE_VISIBILITY_FOR_NON_LIST_VIEWS.key)
                ?.toBooleanStrictOrNull() ?: true,
            showHostInLinkListView = localStorage.getItem(AppPreferences.BASE_URL_VISIBILITY_FOR_NON_LIST_VIEWS.key)
                ?.toBooleanStrictOrNull() ?: true,
            enableFadedEdgeForNonListViews = localStorage.getItem(AppPreferences.FADED_EDGE_VISIBILITY_FOR_NON_LIST_VIEWS.key)
                ?.toBooleanStrictOrNull() ?: true,
            forceSaveWithoutFetchingAnyMetaData = localStorage.getItem(AppPreferences.FORCE_SAVE_WITHOUT_FETCHING_META_DATA.key)
                ?.toBooleanStrictOrNull() ?: false,
            skipSavingExistingLink = localStorage.getItem(AppPreferences.SKIP_SAVING_EXISTING_LINK.key)
                ?.toBooleanStrictOrNull() ?: true,
            useProxy = localStorage.getItem(AppPreferences.USE_PROXY.key)?.toBooleanStrictOrNull()
                ?: true,
            proxyUrl = localStorage.getItem(AppPreferences.PROXY_URL.key)
                ?: Constants.PROXY_SERVER_URL,
            startDestination = localStorage.getItem(AppPreferences.INITIAL_ROUTE.key)
                ?: Navigation.Root.HomeScreen.toString(),
            serverBaseUrl = localStorage.getItem(AppPreferences.SERVER_URL.key) ?: "",
            serverSecurityToken = localStorage.getItem(AppPreferences.SERVER_AUTH_TOKEN.key) ?: "",
            serverSyncType = localStorage.getItem(AppPreferences.SERVER_SYNC_TYPE.key)
                ?.let { SyncType.valueOf(it) } ?: SyncType.TwoWay,
            useLinkoraTopDecoratorOnDesktop = localStorage.getItem(AppPreferences.DESKTOP_TOP_DECORATOR.key)
                ?.toBooleanStrictOrNull() ?: true,
            refreshLinksWorkerTag = localStorage.getItem(AppPreferences.CURRENT_WORK_MANAGER_WORK_UUID.key)
                ?: "52ae3f4a-d37f-4fdb-a6b6-4397b99ef1bd",
            showVideoTagOnUIIfApplicable = localStorage.getItem(AppPreferences.SHOW_VIDEO_TAG_IF_APPLICABLE.key)
                ?.toBooleanStrictOrNull() ?: false,
            forceShuffleLinks = localStorage.getItem(AppPreferences.FORCE_SHUFFLE_LINKS.key)
                ?.toBooleanStrictOrNull() ?: false,
            showNoteInLinkView = localStorage.getItem(AppPreferences.NOTE_VISIBILITY_IN_LIST_VIEWS.key)
                ?.toBooleanStrictOrNull() ?: true,
            showDateInLinkView = localStorage.getItem(AppPreferences.SHOW_DATE_IN_LINK_VIEW.key)
                ?.toBooleanStrictOrNull() ?: true,
            showTagsInLinkView = localStorage.getItem(AppPreferences.SHOW_TAGS_IN_LINK_VIEW.key)
                ?.toBooleanStrictOrNull() ?: true,
            areSnapshotsEnabled = localStorage.getItem(AppPreferences.USE_SNAPSHOTS.key)
                ?.toBooleanStrictOrNull() ?: false,
            snapshotExportFormatID = localStorage.getItem(AppPreferences.SNAPSHOTS_EXPORT_TYPE.key)
                ?: SnapshotFormat.JSON.id.toString(),
            skipCertCheckForSync = localStorage.getItem(AppPreferences.SKIP_CERT_CHECK_FOR_SYNC_SERVER.key)
                ?.toBooleanStrictOrNull() ?: false,
            currentExportLocation = localStorage.getItem(AppPreferences.EXPORT_LOCATION.key) ?: "",
            currentBackupLocation = localStorage.getItem(AppPreferences.BACKUP_LOCATION.key) ?: "",
            backupAutoDeleteThreshold = localStorage.getItem(AppPreferences.BACKUP_AUTO_DELETION_THRESHOLD.key)
                ?.toIntOrNull() ?: 10,
            backupAutoDeletionEnabled = localStorage.getItem(AppPreferences.BACKUP_AUTO_DELETION_ENABLED.key)
                ?.toBooleanStrictOrNull() ?: false,
            selectedCollectionSourceId = localStorage.getItem(AppPreferences.COLLECTION_SOURCE_ID.key)
                ?.toIntOrNull() ?: 0,
            selectedAppIcon = localStorage.getItem(AppPreferences.SELECTED_APP_ICON.key)
                ?: AppIconCode.new_logo.name,
            showTagsInAddNewLinkDialogBox = localStorage.getItem(AppPreferences.SHOW_TAGS_BY_DEFAULT_IN_ADD_LINK.key)
                ?.toBooleanStrictOrNull() ?: false,
            showMenuOnGridLinkClick = localStorage.getItem(AppPreferences.SHOW_MENU_ON_GRID_LINK_CLICK.key)
                ?.toBooleanStrictOrNull() ?: true,
            autoSaveOnShareIntent = localStorage.getItem(AppPreferences.AUTO_SAVE_ON_SHARE_INTENT.key)
                ?.toBooleanStrictOrNull() ?: false,
            forceSaveIfRetrievalFails = localStorage.getItem(AppPreferences.FORCE_SAVE_LINKS.key)
                ?.toBooleanStrictOrNull() ?: true,
            selectedFont = localStorage.getItem(AppPreferences.FONT_TYPE.key)
                ?.let { Font.valueOf(it) } ?: Font.POPPINS,
            selectedLinkRefreshType = localStorage.getItem(AppPreferences.REFRESH_LINK_TYPE.key)
                ?.let { RefreshLinkType.valueOf(it) } ?: RefreshLinkType.Both,
            maxConcurrentRefreshCount = localStorage.getItem(AppPreferences.MAX_CONCURRENT_REFRESH_COUNT.key)
                ?.toIntOrNull() ?: 15
        )
    }
}