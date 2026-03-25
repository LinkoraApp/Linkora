package com.sakethh.linkora.platform

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.PreferenceKey
import com.sakethh.linkora.ui.AppVM
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.ui.utils.linkoraLog
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import java.security.cert.CertificateException
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import javax.net.ssl.X509TrustManager
import kotlin.io.inputStream
import kotlin.io.resolve
import kotlin.use

actual val showFollowSystemThemeOption: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q
actual val platform: Platform = Platform.Android

actual val showDynamicThemingOption: Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

@Composable
actual fun PlatformSpecificBackHandler(init: () -> Unit) {
    val navController = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()
    BackHandler(onBack = {
        if (AppVM.isMainFabRotated.value) {
            AppVM.isMainFabRotated.value = false
        } else if (navController.previousBackStackEntry == null) {
            coroutineScope.launch {
                UIEvent.pushUIEvent(UIEvent.Type.MinimizeTheApp)
            }
        } else {
            init()
        }
    })
}

actual fun platformSpecificLogging(string: String) {
    Log.d("Linkora Log", string)
}

actual val PlatformIODispatcher: CoroutineDispatcher = Dispatchers.IO

actual class Network(private val context: Context) {

    private fun HttpClientConfig<CIOEngineConfig>.installLogger() {
        install(Logging) {
            logger = object : Logger {
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

    actual val standardClient = HttpClient(CIO) {
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

        val certificateFactory = CertificateFactory.getInstance("X.509")
        var signedCertificate: X509Certificate? = null
        val syncServerCert = context.filesDir.resolve("sync-server-cert.cer")

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

        syncServerClient = HttpClient(CIO) {
            install(HttpTimeout) {
                this.socketTimeoutMillis = 240_000
                this.connectTimeoutMillis = 240_000
                this.requestTimeoutMillis = 240_000
            }
            engine {
                https {
                    trustManager = object : X509TrustManager {
                        override fun checkClientTrusted(
                            chain: Array<out X509Certificate?>?, authType: String?
                        ) {
                        }

                        override fun checkServerTrusted(
                            chain: Array<out X509Certificate?>?, authType: String?
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

                        override fun getAcceptedIssuers(): Array<out X509Certificate?> {
                            return if (bypassCertCheck) arrayOf() else arrayOf(signedCertificate)
                        }

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

actual class PlatformPreference(
    private val dataStore: DataStore<Preferences>
) {
    actual suspend fun <T> writePreferenceValue(
        preferenceKey: PreferenceKey<T>, newValue: T
    ) {
        dataStore.edit {
            when (preferenceKey) {
                is PreferenceKey.BooleanPreferencesKey -> {
                    it[booleanPreferencesKey(preferenceKey.key)] = newValue as Boolean
                }

                is PreferenceKey.LongPreferencesKey -> {
                    it[longPreferencesKey(preferenceKey.key)] = newValue as Long
                }

                is PreferenceKey.StringPreferencesKey -> {
                    it[stringPreferencesKey(preferenceKey.key)] = newValue as String
                }

                is PreferenceKey.IntPreferencesKey -> {
                    it[intPreferencesKey(preferenceKey.key)] = newValue as Int
                }
            }
        }
    }

    actual suspend fun <T> readPreferenceValue(preferenceKey: PreferenceKey<T>): T? {
        return when (preferenceKey) {
            is PreferenceKey.BooleanPreferencesKey -> {
                dataStore.data.first()[
                    booleanPreferencesKey(
                        preferenceKey.key,
                    ),
                ]
            }

            is PreferenceKey.LongPreferencesKey -> {
                dataStore.data.first()[
                    longPreferencesKey(
                        preferenceKey.key,
                    ),
                ]
            }

            is PreferenceKey.StringPreferencesKey -> {
                dataStore.data.first()[
                    stringPreferencesKey(
                        preferenceKey.key,
                    ),
                ]
            }

            is PreferenceKey.IntPreferencesKey -> dataStore.data.first()[
                intPreferencesKey(
                    preferenceKey.key,
                ),
            ]
        } as T?
    }
}