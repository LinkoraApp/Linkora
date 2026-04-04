package com.sakethh.linkora

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.ComposeViewport
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.ExperimentalBrowserHistoryApi
import androidx.navigation.bindToBrowserNavigation
import androidx.navigation.compose.rememberNavController
import androidx.room3.Room
import androidx.sqlite.driver.web.WebWorkerSQLiteDriver
import com.sakethh.linkora.data.local.LocalDatabase
import com.sakethh.linkora.di.DependencyContainer
import com.sakethh.linkora.di.LinkoraSDK
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.platform.FileManager
import com.sakethh.linkora.platform.NativeUtils
import com.sakethh.linkora.platform.Network
import com.sakethh.linkora.platform.PermissionManager
import com.sakethh.linkora.platform.PlatformPreference
import com.sakethh.linkora.ui.App
import com.sakethh.linkora.ui.FabStateController
import com.sakethh.linkora.ui.LocalFabController
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.LocalPlatform
import com.sakethh.linkora.ui.theme.DarkColors
import com.sakethh.linkora.ui.theme.LightColors
import com.sakethh.linkora.ui.theme.LinkoraTheme
import com.sakethh.linkora.ui.utils.pressScaleEffect
import kotlinx.browser.localStorage
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import org.w3c.dom.Worker
import org.w3c.dom.set

@OptIn(ExperimentalWasmJsInterop::class)
private fun createWorker(): Worker =
    js("""new Worker(new URL("sqlite-wasm-worker/worker.js", import.meta.url))""")

private const val SHOW_WEB_NOTICE = "SHOW_WEB_NOTICE"

@OptIn(
    ExperimentalComposeUiApi::class,
    ExperimentalBrowserHistoryApi::class,
    ExperimentalMaterial3Api::class
)
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

    MainScope().launch {
        DependencyContainer.preferencesRepo.loadPersistedPreferences()
        val preferences = DependencyContainer.preferencesRepo.getPreferences()
        Localization.loadLocalizedStrings(
            preferences,
            languageName = preferences.preferredAppLanguageName,
            languageCode = preferences.preferredAppLanguageCode,
        )?.join()
    }

    ComposeViewport {
        val preferences by DependencyContainer.preferencesRepo.preferencesAsFlow.collectAsStateWithLifecycle()
        val navController = rememberNavController()
        LaunchedEffect(Unit) {
            navController.bindToBrowserNavigation()
        }
        val webNoticeState =
            rememberModalBottomSheetState(skipPartiallyExpanded = true, confirmValueChange = {
                if (it == SheetValue.Hidden) {
                    localStorage.getItem(SHOW_WEB_NOTICE)?.toBooleanStrictOrNull() == false
                } else {
                    true
                }
            })
        var showWebNoticeState by rememberSaveable {
            mutableStateOf(true)
        }
        val coroutineScope = rememberCoroutineScope()
        CompositionLocalProvider(
            LocalNavController provides navController,
            LocalFabController provides retain {
                FabStateController()
            }, LocalPlatform provides Platform.Web
        ) {
            LinkoraTheme(
                colorScheme = if (preferences.useDarkTheme) DarkColors else LightColors,
                preferredFont = preferences.selectedFont
            ) {
                Surface {
                    App()

                    if (showWebNoticeState && localStorage.getItem(SHOW_WEB_NOTICE)
                            ?.toBooleanStrictOrNull() != false
                    ) {
                        ModalBottomSheet(sheetState = webNoticeState, onDismissRequest = {
                            showWebNoticeState = false
                        }) {
                            Column(
                                modifier = Modifier.padding(
                                    start = 15.dp, end = 15.dp, bottom = 7.5.dp
                                ),
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Info,
                                    contentDescription = null,
                                )
                                Spacer(modifier = Modifier.height(7.5.dp))
                                Text(
                                    text = "Room handles database operations on Linkora Web using OPFS, which has strict browser locking limits. " +
                                            "Room Web is still in early alpha, so the Linkora web port is highly experimental and can be unstable at times. " +
                                            "It is not recommended for daily use yet, unlike Linkora Android and Desktop. " +
                                            "If something breaks, please report it on GitHub with your browser version and console logs.",
                                    fontSize = 16.sp,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.height(15.dp))
                                HorizontalDivider(thickness = 1.dp)
                                Spacer(modifier = Modifier.height(15.dp))
                                Text(
                                    text = "Linkora Web relies on a dedicated proxy to fetch images and metadata. Since only one public instance is available, you may encounter issues.\n" +
                                            "To ensure reliable image display and metadata retrieval, use a self-hosted Linkora proxy. Update your proxy address in the Advanced Settings screen.",
                                    fontSize = 16.sp,
                                    style = MaterialTheme.typography.titleSmall
                                )
                                Spacer(modifier = Modifier.height(5.dp))
                                Button(
                                    onClick = {
                                        localStorage.set(
                                            key = SHOW_WEB_NOTICE, value = "false"
                                        )
                                        coroutineScope.launch {
                                            webNoticeState.hide()
                                        }.invokeOnCompletion {
                                            showWebNoticeState = false
                                        }
                                    },
                                    modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                        .pressScaleEffect().fillMaxWidth()
                                ) {
                                    Text(
                                        text = "Got It",
                                        style = MaterialTheme.typography.titleMedium
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

