package com.sakethh.linkora.ui.screens.settings.section.data.sync

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sakethh.linkora.common.DependencyContainer
import com.sakethh.linkora.common.Localization
import com.sakethh.linkora.common.preferences.AppPreferences
import com.sakethh.linkora.common.utils.fillMaxWidthWithPadding
import com.sakethh.linkora.common.utils.rememberLocalizedString
import com.sakethh.linkora.domain.RemoteRoute
import com.sakethh.linkora.domain.SyncType
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.components.InfoCard
import com.sakethh.linkora.ui.domain.model.ServerConnection
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingsSectionScaffold
import com.sakethh.linkora.ui.screens.settings.section.data.LogsScreen
import com.sakethh.linkora.ui.screens.settings.section.data.components.ToggleButton
import com.sakethh.linkora.ui.utils.genericViewModelFactory
import com.sakethh.linkora.ui.utils.pulsateEffect
import com.sakethh.linkora.ui.utils.rememberMutableEnum
import com.sakethh.poppinsFontFamily

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSetupScreen(
) {
    val navController = LocalNavController.current
    val serverManagementViewModel =
        viewModel<ServerManagementViewModel>(factory = genericViewModelFactory {
            ServerManagementViewModel(
                DependencyContainer.networkRepo.value,
                DependencyContainer.preferencesRepo.value,
                DependencyContainer.remoteSyncRepo.value
            )
        })
    val serverUrl = rememberSaveable {
        mutableStateOf(AppPreferences.serverBaseUrl.value)
    }
    val securityToken = rememberSaveable {
        mutableStateOf(AppPreferences.serverSecurityToken.value)
    }
    val selectedSyncType = rememberMutableEnum(SyncType::class.java) {
        mutableStateOf(AppPreferences.serverSyncType.value)
    }
    val showImportLogsFromServer = rememberSaveable {
        mutableStateOf(false)
    }
    val selectedWebSocketScheme = rememberSaveable {
        mutableStateOf(AppPreferences.selectedWebsocketScheme.value)
    }
    SettingsSectionScaffold(
        topAppBarText = Navigation.Settings.Data.ServerSetupScreen.toString(),
        navController = navController
    ) { paddingValues, topAppBarScrollBehaviour ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
                .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection)
                .navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            item {
                Spacer(Modifier)
            }
            item {
                Text(
                    text = Localization.rememberLocalizedString(Localization.Key.Configuration),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(start = 15.dp, end = 15.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                TextField(
                    textStyle = TextStyle(fontFamily = poppinsFontFamily),
                    modifier = Modifier.fillMaxWidthWithPadding(),
                    value = serverUrl.value,
                    onValueChange = {
                        serverUrl.value = if (it.endsWith("/")) it else "$it/"
                    },
                    label = {
                        Text(
                            text = Localization.rememberLocalizedString(Localization.Key.ServerURL),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    supportingText = {
                        Text(
                            text = Localization.rememberLocalizedString(Localization.Key.ServerSetupInstruction),
                            style = MaterialTheme.typography.titleLarge
                        )
                    },
                    readOnly = serverManagementViewModel.serverSetupState.value.isConnectedSuccessfully && serverManagementViewModel.serverSetupState.value.isConnecting.not()
                )
            }

            item {
                Text(modifier = Modifier.fillMaxWidthWithPadding(),text = "WebSocket Scheme", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.height(5.dp))
                Text(modifier = Modifier.fillMaxWidthWithPadding(), style = MaterialTheme.typography.titleSmall,text = "If you're running the server behind a proxy that handles SSL/HTTPS connections, use wss instead of ws. For direct HTTP connections to the server, ws will work fine.")
                Spacer(Modifier.height(10.dp))
                Row(modifier = Modifier.fillMaxWidthWithPadding()) {
                    ToggleButton(
                        shape = RoundedCornerShape(
                            topStart = 15.dp, bottomStart = 15.dp, topEnd = 5.dp, bottomEnd = 5.dp
                        ), checked = selectedWebSocketScheme.value == "ws", onCheckedChange = {
                            selectedWebSocketScheme.value = "ws"
                        }) {
                        Text(
                            text = "ws",
                            style = if (selectedWebSocketScheme.value == "ws") MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                            color = if (selectedWebSocketScheme.value == "ws") MaterialTheme.colorScheme.onPrimary else LocalContentColor.current
                        )
                    }
                    Spacer(Modifier.width(5.dp))
                    ToggleButton(
                        shape = RoundedCornerShape(
                            topStart = 5.dp, bottomStart = 5.dp, topEnd = 15.dp, bottomEnd = 15.dp
                        ), checked = selectedWebSocketScheme.value == "wss", onCheckedChange = {
                            selectedWebSocketScheme.value = "wss"
                        }) {
                        Text(
                            text = "wss",
                            style = if (selectedWebSocketScheme.value == "wss") MaterialTheme.typography.titleMedium else MaterialTheme.typography.titleSmall,
                            color = if (selectedWebSocketScheme.value == "wss") MaterialTheme.colorScheme.onPrimary else LocalContentColor.current
                        )
                    }
                }
            }

            item {
                TextField(
                    textStyle = TextStyle(fontFamily = poppinsFontFamily),
                    modifier = Modifier.fillMaxWidthWithPadding(),
                    value = securityToken.value,
                    onValueChange = { newValue ->
                        securityToken.value = newValue
                    },
                    label = {
                        Text(
                            text = Localization.rememberLocalizedString(Localization.Key.SecurityToken),
                            style = MaterialTheme.typography.titleMedium
                        )
                    },
                    readOnly = serverManagementViewModel.serverSetupState.value.isConnectedSuccessfully && serverManagementViewModel.serverSetupState.value.isConnecting.not()
                )
            }

            item {
                if (serverManagementViewModel.serverSetupState.value.isConnecting) {
                    LinearProgressIndicator(
                        modifier = Modifier.fillMaxWidthWithPadding()
                    )
                } else if (serverManagementViewModel.serverSetupState.value.isConnectedSuccessfully) {
                    InfoCard(
                        info = Localization.rememberLocalizedString(Localization.Key.ServerIsReachable),
                        paddingValues = PaddingValues(start = 15.dp, end = 15.dp)
                    )
                } else {
                    Button(
                        onClick = {
                            serverManagementViewModel.testServerConnection(
                                serverUrl = serverUrl.value, token = securityToken.value
                            )
                        }, modifier = Modifier.fillMaxWidthWithPadding().pulsateEffect()
                    ) {
                        Text(
                            text = Localization.rememberLocalizedString(Localization.Key.TestServerAvailability),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
            }
            if (serverManagementViewModel.serverSetupState.value.isConnectedSuccessfully.not()) {
                return@LazyColumn
            }
            item {
                HorizontalDivider(modifier = Modifier.fillMaxWidthWithPadding())
            }
            item {
                Text(
                    text = Localization.rememberLocalizedString(Localization.Key.SyncType),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(start = 15.dp, end = 15.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                SyncType.entries.forEachIndexed { index, syncType ->
                    if (index > 0) {
                        Spacer(Modifier.height(5.dp))
                    } else {
                        Spacer(Modifier.height(15.dp))
                    }
                    Column(modifier = Modifier.clickable(onClick = {
                        selectedSyncType.value = syncType
                    }, indication = null, interactionSource = remember {
                        MutableInteractionSource()
                    }).pulsateEffect().fillMaxWidthWithPadding()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = syncType == selectedSyncType.value, onClick = {
                                    selectedSyncType.value = syncType
                                })
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = syncType.asUIString(),
                                style = MaterialTheme.typography.titleMedium
                            )
                        }
                        Text(
                            text = syncType.description(),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 15.dp)
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        serverManagementViewModel.saveServerConnectionAndSync(
                            serverConnection = ServerConnection(
                            serverUrl = serverUrl.value.substringBefore(RemoteRoute.SyncInLocalRoute.TEST_BEARER.name),
                            authToken = securityToken.value,
                            syncType = selectedSyncType.value,
                            webSocketScheme = selectedWebSocketScheme.value
                        ), onSyncStart = {
                            showImportLogsFromServer.value = true
                        }, onCompletion = {
                            showImportLogsFromServer.value = false
                            navController.navigateUp()
                        })
                    }, modifier = Modifier.fillMaxWidthWithPadding().pulsateEffect()
                ) {
                    Text(
                        text = Localization.rememberLocalizedString(Localization.Key.UseThisConnection),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            }
            item {
                Spacer(Modifier)
            }
        }
    }
    LogsScreen(
        isVisible = showImportLogsFromServer,
        operationDesc = Localization.Key.SyncingDataLabel.rememberLocalizedString(),
        operationTitle = Localization.Key.InitiateManualSyncDescAlt.rememberLocalizedString(),
        logs = serverManagementViewModel.dataSyncLogs,
        onCancel = {
            serverManagementViewModel.cancelServerConnectionAndSync()
        })
}