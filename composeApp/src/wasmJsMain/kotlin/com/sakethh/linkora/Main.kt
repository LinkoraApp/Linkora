package com.sakethh.linkora

import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.bindToBrowserNavigation
import androidx.navigation.compose.rememberNavController
import androidx.room3.Room
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import com.sakethh.linkora.data.local.LocalDatabase
import com.sakethh.linkora.di.LinkoraSDK
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.platform.FileManager
import com.sakethh.linkora.platform.NativeUtils
import com.sakethh.linkora.platform.Network
import com.sakethh.linkora.platform.PermissionManager
import com.sakethh.linkora.platform.PlatformPreference
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.App
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.LocalPlatform
import com.sakethh.linkora.ui.theme.DarkColors
import com.sakethh.linkora.ui.theme.LightColors
import com.sakethh.linkora.ui.theme.LinkoraTheme
import org.w3c.dom.Worker

@OptIn(ExperimentalWasmJsInterop::class)
private fun createWorker(): Worker =
    js("""new Worker(new URL("sqlite-wasm-worker/worker.js", import.meta.url))""")

@OptIn(ExperimentalComposeUiApi::class, ExperimentalBrowserHistoryApi::class)
fun main() {

    LinkoraSDK.set(
        linkoraSdk = LinkoraSDK(
            nativeUtils = NativeUtils(),
            fileManager = FileManager(),
            permissionManager = PermissionManager(),
            localDatabase = Room.databaseBuilder<LocalDatabase>("${LocalDatabase.NAME}.db")
                .setDriver(WebWorkerSQLiteDriver(createWorker())).build(),
            platformPreference = PlatformPreference,
            network = Network,
            dataSyncingNotificationService = NativeUtils.DataSyncingNotificationService()
        )
    )

    /*MainScope().launch {
        awaitAll(async {
            AppPreferences.readAll(
                defaultExportLocation = LinkoraSDK.getInstance().fileManager.getDefaultExportLocation(),
                preferencesRepository = DependencyContainer.preferencesRepo
            )
        }, async {
            Localization.loadLocalizedStrings(
                AppPreferences.preferredAppLanguageCode.value
            )
        })
    }*/

    ComposeViewport {
        val navController = rememberNavController()
        LaunchedEffect(Unit) {
            navController.bindToBrowserNavigation()
        }
        CompositionLocalProvider(
            LocalNavController provides navController, LocalPlatform provides Platform.Web
        ) {
            LinkoraTheme(
                colorScheme = if (AppPreferences.useDarkTheme.value) DarkColors else LightColors
            ) {
                Surface {
                    App()
                }
            }
        }
    }
}