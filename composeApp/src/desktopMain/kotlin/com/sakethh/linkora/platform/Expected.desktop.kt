package com.sakethh.linkora.platform

import AndroidDesktopWebCapture
import RefreshAllLinksService
import androidx.compose.runtime.Composable
import androidx.compose.runtime.snapshotFlow
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.sakethh.linkora.Localization
import com.sakethh.linkora.WebCaptureService
import com.sakethh.linkora.data.local.WebCaptureDatabaseManager
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.PermissionStatus
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.PreferenceKey
import com.sakethh.linkora.domain.Result
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.local.RefreshLinksRepo
import com.sakethh.linkora.domain.repository.local.WebCaptureRepo
import com.sakethh.linkora.linkoraSpecificFolder
import com.sakethh.linkora.model.WebCaptureRequest
import com.sakethh.linkora.ui.screens.settings.section.data.DataSettingsScreenVM
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.ui.utils.linkoraLog
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.getLocalizedString
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.cio.CIO
import io.ktor.client.engine.cio.CIOEngineConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.plugins.websocket.WebSockets
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import okio.Path.Companion.toPath
import readAllPreferences
import readPreferenceValue
import writePreferenceValue
import java.io.File
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager
import kotlin.io.inputStream
import kotlin.io.println
import kotlin.io.resolve
import kotlin.use

actual val showFollowSystemThemeOption: Boolean = false
actual val platform: Platform = Platform.Desktop

actual val showDynamicThemingOption: Boolean = false

@Composable
actual fun PlatformSpecificBackHandler(init: () -> Unit) = Unit

actual fun platformSpecificLogging(string: String) {
    println("Linkora Log : $string")
}

actual class PermissionManager {
    actual suspend fun permittedToShowNotification(): PermissionStatus = PermissionStatus.Granted

    actual suspend fun isStorageAccessPermitted(): PermissionStatus = PermissionStatus.Granted
}

actual class NativeUtils {

    actual fun onShare(url: String) = Unit

    actual suspend fun onRefreshAllLinks(
        localLinksRepo: LocalLinksRepo,
        preferencesRepository: PreferencesRepository,
        refreshLinksRepo: RefreshLinksRepo,
    ) {
        RefreshAllLinksService.invoke(localLinksRepo)
    }

    actual fun isAnyRefreshingEnqueued(): Flow<Boolean?> = flow {
        emit(false)
    }

    actual fun cancelRefreshingLinks() {
        RefreshAllLinksService.cancel()
    }

    actual class DataSyncingNotificationService {
        actual fun showNotification() = Unit

        actual fun clearNotification() = Unit
    }

    actual fun onIconChange(
        allIconCodes: List<String>,
        newIconCode: String,
        onCompletion: () -> Unit,
    ) = Unit

    actual fun <T> platformRunBlocking(block: suspend () -> T): T? = runBlocking {
        block()
    }

    actual class WebCapture {
        val androidDesktopWebCapture = AndroidDesktopWebCapture()

        actual suspend fun init(): Result<Boolean> = androidDesktopWebCapture.init()

        actual suspend fun saveHTMLPage(
            nativeFolderPath: String,
            url: String,
            userAgent: String,
            timeout: Long,
            allowInsecureProtocol: Boolean,
            ignoreDocErrors: Boolean,
            useCss: Boolean,
            embedFonts: Boolean,
            embedImages: Boolean,
            restrictJs: Boolean,
            logStuff: Boolean,
            includeAudioElements: Boolean,
            includeVideoElements: Boolean,
            includeMetadata: Boolean,
        ): Result<Boolean> {
            val request = WebCaptureRequest(
                nativeFolderPath = nativeFolderPath,
                url = url,
                userAgent = userAgent,
                timeout = timeout,
                allowInsecureProtocol = allowInsecureProtocol,
                ignoreDocErrors = ignoreDocErrors,
                useCss = useCss,
                embedFonts = embedFonts,
                embedImages = embedImages,
                restrictJs = restrictJs,
                logStuff = logStuff,
                includeAudioElements = includeAudioElements,
                includeVideoElements = includeVideoElements,
                includeMetadata = includeMetadata,
            )

            WebCaptureService.queueCapture(request)

            return Result.Success(true)
        }

        actual suspend fun onCaptureAllWebPages(
            preferences: AppPreferences,
            localLinksRepo: LocalLinksRepo,
            webCaptureRepo: WebCaptureRepo,
            webCapture: WebCapture,
        ) {
            AllLinksWebCaptureService.captureAllWebPages(
                preferences = preferences,
                localLinksRepo = localLinksRepo,
                webCaptureRepo = webCaptureRepo,
            )
        }

        actual fun cancelBulkWebCapture() {
            AllLinksWebCaptureService.cancel()
        }

        actual fun isWebCaptureWorkerEnqueued(): Flow<Boolean?> = combine(
            WebCaptureService.isProcessing,
            snapshotFlow {
                DataSettingsScreenVM.onGoingWebCaptureState
            },
        ) { b1, b2 ->
            b1 || b2.isInProgress
        }.distinctUntilChanged()

        actual suspend fun nuke() {
            androidDesktopWebCapture.nuke()
        }

        actual suspend fun prepareExternalDatabase(
            captureLocation: String,
            webCaptureDatabaseManager: WebCaptureDatabaseManager
        ) {
            webCaptureDatabaseManager.initAndGetDatabase(captureLocation)
        }
    }
}

actual val PlatformIODispatcher: CoroutineDispatcher = Dispatchers.IO

actual object Network {

    private fun HttpClientConfig<CIOEngineConfig>.installLogger() {
        install(Logging) {
            logger =
                object : Logger {
                    override fun log(message: String) {
                        linkoraLog("HTTP CLIENT:\n$message")
                    }
                }
            level = LogLevel.ALL
        }
    }

    private fun HttpClientConfig<CIOEngineConfig>.installContentNegotiation() {
        val jsonConfig = Json {
            ignoreUnknownKeys = true
            isLenient = true
            encodeDefaults = true
        }
        install(ContentNegotiation) {
            json(jsonConfig)
        }
    }

    actual val standardClient =
        HttpClient(CIO) {
            installContentNegotiation()
            installLogger()
        }

    private var syncServerClient: HttpClient? = null

    actual fun getSyncServerClient(): HttpClient = syncServerClient
        ?: error(Localization.Key.SyncServerConfigurationError.getLocalizedString())

    actual fun closeSyncServerClient() {
        syncServerClient?.close()
        syncServerClient = null
    }

    actual suspend fun configureSyncServerClient(bypassCertCheck: Boolean) {
        if (syncServerClient != null) return

        val certificateFactory = CertificateFactory.getInstance("X.509")
        var signedCertificate: X509Certificate? = null
        val syncServerCert = linkoraSpecificFolder.resolve("sync-server-cert.cer")

        if (syncServerCert.exists() && !bypassCertCheck) {
            syncServerCert.inputStream().use {
                try {
                    signedCertificate =
                        certificateFactory.generateCertificate(it) as X509Certificate
                } catch (e: Exception) {
                    pushUIEvent(UIEvent.Type.ShowSnackbar(e.message.toString()))
                    null
                }
            }
        }

        if (!syncServerCert.exists() && !bypassCertCheck) {
            error(Localization.Key.SyncServerConfigurationError.getLocalizedString())
        }

        syncServerClient =
            HttpClient(CIO) {
                install(HttpTimeout) {
                    this.socketTimeoutMillis = 240_000
                    this.connectTimeoutMillis = 240_000
                    this.requestTimeoutMillis = 240_000
                }
                engine {
                    https {
                        trustManager =
                            object : X509TrustManager {
                                override fun checkClientTrusted(
                                    chain: Array<out X509Certificate?>?,
                                    authType: String?,
                                ) {
                                }

                                override fun checkServerTrusted(
                                    chain: Array<out X509Certificate?>?,
                                    authType: String?,
                                ) {
                                    if (bypassCertCheck) {
                                        linkoraLog("Bypassing checkServerTrusted")
                                        return
                                    }

                                    if (chain?.isEmpty() == true) {
                                        throw CertificateException("Certificate chain is empty") as Throwable
                                    }

                                    val serverCert = chain?.get(0)
                                    signedCertificate?.let {
                                        serverCert?.verify(it.publicKey)
                                    }
                                    serverCert?.checkValidity()
                                }

                                override fun getAcceptedIssuers(): Array<out X509Certificate?> =
                                    if (bypassCertCheck) arrayOf() else arrayOf(signedCertificate)
                            }
                    }
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

    private val dataStore =
        PreferenceDataStoreFactory.createWithPath(
            produceFile = {
                "${linkoraSpecificFolder.absolutePath}/${Constants.DATA_STORE_NAME}".toPath()
            },
        )

    actual suspend fun <T> writePreferenceValue(
        preferenceKey: PreferenceKey<T>,
        newValue: T,
    ) {
        writePreferenceValue(
            dataStore = dataStore,
            preferenceKey = preferenceKey,
            newValue = newValue,
        )
    }

    actual suspend fun <T> readPreferenceValue(preferenceKey: PreferenceKey<T>): T? =
        readPreferenceValue(dataStore = dataStore, preferenceKey = preferenceKey)

    actual suspend fun readAllPreferences(): AppPreferences {
        val prefs = dataStore.data.first()
        return readAllPreferences(
            prefs,
            externalAction = { externalAction -> externalAction(this) },
        )
    }
}

actual fun defaultExportLocation(): String? {
    val userHomeDir = System.getProperty("user.home")
    return File(userHomeDir, "/Documents/Linkora/Exports").absolutePath
}

actual fun defaultSnapshotLocation(): String? {
    val userHomeDir = System.getProperty("user.home")
    return File(userHomeDir, "/Documents/Linkora/Snapshots").absolutePath
}
