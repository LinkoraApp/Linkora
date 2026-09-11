package com.sakethh.linkora.ui.screens.settings.section.data.sync

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sakethh.linkora.di.linkoraViewModel
import com.sakethh.linkora.domain.SyncServerRoute
import com.sakethh.linkora.domain.SyncType
import com.sakethh.linkora.domain.model.settings.SettingComponentParam
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.LocalizedStrings
import com.sakethh.linkora.ui.components.HorizontalInfoCard
import com.sakethh.linkora.ui.domain.model.ServerConnection
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.collections.components.ItemDivider
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingComponent
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingsSectionScaffold
import com.sakethh.linkora.ui.screens.settings.section.data.LogsScreen
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.addEdgeToEdgeScaffoldPadding
import com.sakethh.linkora.utils.asUIString
import com.sakethh.linkora.utils.description
import com.sakethh.linkora.utils.fillMaxWidthWithPadding
import com.sakethh.linkora.utils.highlightOnFocused
import com.sakethh.linkora.utils.replaceActual

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerSetupScreen() {
    val localizedStrings = LocalizedStrings.current
    val navController = LocalNavController.current
    val serverManagementViewModel: ServerManagementViewModel = linkoraViewModel()
    val preferences by serverManagementViewModel.preferencesAsFlow.collectAsStateWithLifecycle()
    val serverUrl = rememberSaveable {
        mutableStateOf(preferences.serverBaseUrl)
    }
    val securityToken = rememberSaveable {
        mutableStateOf(preferences.serverSecurityToken)
    }
    val isSecurityTokenVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val selectedSyncType = retain {
        mutableStateOf(preferences.serverSyncType)
    }
    var showImportLogsFromServer by rememberSaveable {
        mutableStateOf(false)
    }

    val isCertificateInProcessing = rememberSaveable {
        mutableStateOf(false)
    }

    val importedCertInfo = rememberSaveable {
        mutableStateOf("")
    }

    val pwdVisualTransformation = retain {
        PasswordVisualTransformation()
    }

    SettingsSectionScaffold(
        topAppBarText = localizedStrings.LinkoraServerSetup,
    ) { paddingValues, topAppBarScrollBehaviour ->
        LazyColumn(
            modifier =
                Modifier.fillMaxSize()
                    .addEdgeToEdgeScaffoldPadding(paddingValues)
                    .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            item {
                Spacer(Modifier)
            }
            item {
                Text(
                    text = localizedStrings.Configuration,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(start = 15.dp, end = 15.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            item {
                TextField(
                    textStyle = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidthWithPadding(),
                    value = serverUrl.value,
                    onValueChange = {
                        serverUrl.value = if (it.endsWith("/")) it else "$it/"
                    },
                    label = {
                        Text(
                            text = localizedStrings.ServerURL,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    supportingText = {
                        Text(
                            text =
                                localizedStrings.ServerSetupInstruction,
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    readOnly =
                        serverManagementViewModel.serverSetupState.value.isConnectedSuccessfully &&
                                serverManagementViewModel.serverSetupState.value.isConnecting.not(),
                )
            }

            item {
                TextField(
                    textStyle = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.fillMaxWidthWithPadding(),
                    value = securityToken.value,
                    onValueChange = { newValue ->
                        securityToken.value = newValue
                    },
                    label = {
                        Text(
                            text = localizedStrings.SecurityToken,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    },
                    readOnly =
                        serverManagementViewModel.serverSetupState.value.isConnectedSuccessfully &&
                                serverManagementViewModel.serverSetupState.value.isConnecting.not(),
                    visualTransformation =
                        if (isSecurityTokenVisible.value) {
                            VisualTransformation.None
                        } else {
                            pwdVisualTransformation
                        },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    trailingIcon = {
                        IconButton(
                            modifier = Modifier.pointerHoverIcon(PointerIcon.Hand),
                            onClick = {
                                isSecurityTokenVisible.value = !isSecurityTokenVisible.value
                            },
                        ) {
                            Icon(
                                imageVector =
                                    if (isSecurityTokenVisible.value) {
                                        Icons.Default.Visibility
                                    } else {
                                        Icons.Default.VisibilityOff
                                    },
                                contentDescription = null,
                            )
                        }
                    },
                )
            }

            if (serverManagementViewModel.existingCertificateInfo.value.isNotBlank()) {
                item {
                    Text(
                        text =
                            localizedStrings.ServerCertificateAlreadyImported
                                .replaceActual(
                                    serverManagementViewModel.existingCertificateInfo.value,
                                ),
                        style = MaterialTheme.typography.titleSmall,
                        modifier =
                            Modifier.padding(
                                start = 15.dp,
                                end = 15.dp,
                                top = 5.dp,
                            ),
                        softWrap = true,
                    )
                }
            }

            item {
                Card(modifier = Modifier.animateContentSize().padding(start = 15.dp, end = 15.dp)) {
                    if (!preferences.skipCertCheckForSync) {
                        Text(
                            modifier =
                                Modifier.padding(
                                    start = 15.dp,
                                    end = 15.dp,
                                    top = 15.dp,
                                    bottom = 5.dp,
                                ),
                            text =
                                if (importedCertInfo.value.isNotBlank()) {
                                    localizedStrings.ImportedServerCertificate
                                        .replaceActual(
                                            importedCertInfo.value,
                                        )
                                } else if (isCertificateInProcessing.value) {
                                    localizedStrings.ProcessingCertificate
                                } else {
                                    localizedStrings.ImportServerCertificateDescription
                                },
                            style = MaterialTheme.typography.titleSmall,
                        )

                        if (isCertificateInProcessing.value) {
                            LinearProgressIndicator(
                                modifier =
                                    Modifier.fillMaxWidth()
                                        .padding(
                                            start = 15.dp,
                                            end = 15.dp,
                                            bottom = 15.dp,
                                            top = 10.dp,
                                        ),
                            )
                        } else {
                            ElevatedButton(
                                onClick = {
                                    if (
                                        !serverManagementViewModel.serverSetupState.value.isConnectedSuccessfully &&
                                        !serverManagementViewModel.serverSetupState.value.isConnecting
                                    ) {
                                        serverManagementViewModel.importSignedCertificate(
                                            onStart = {
                                                isCertificateInProcessing.value = true
                                            },
                                            onCompletion = {
                                                importedCertInfo.value = it
                                                isCertificateInProcessing.value = false
                                            },
                                        )
                                    }
                                },
                                modifier =
                                    Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                        .fillMaxWidth()
                                        .padding(
                                            start = 15.dp,
                                            end = 15.dp,
                                            bottom = 15.dp,
                                        )
                                        .pressScaleEffect(),
                            ) {
                                Text(
                                    text = localizedStrings.ImportServerCertificate,
                                    style = MaterialTheme.typography.titleMedium,
                                )
                            }
                        }
                        ItemDivider(paddingValues = PaddingValues())
                    }
                    Box(
                        modifier =
                            Modifier.then(
                                if (preferences.skipCertCheckForSync) {
                                    Modifier.background(MaterialTheme.colorScheme.errorContainer)
                                } else {
                                    Modifier
                                },
                            )
                                .padding(
                                    top = 15.dp,
                                    bottom = 15.dp,
                                ),
                    ) {
                        SettingComponent(
                            SettingComponentParam(
                                title =
                                    localizedStrings.ForceBypassCertificateChecking,
                                doesDescriptionExists = true,
                                description =
                                    localizedStrings.ForceBypassCertificateCheckingDescription,
                                isSwitchNeeded = true,
                                isSwitchEnabled = preferences.skipCertCheckForSync,
                                onSwitchStateChange = {
                                    if (
                                        !serverManagementViewModel.serverSetupState.value
                                            .isConnectedSuccessfully &&
                                        !serverManagementViewModel.serverSetupState.value.isConnecting
                                    ) {
                                        serverManagementViewModel.updateCertificateBypassRule(it)
                                    }
                                },
                                isIconNeeded = false,
                            ),
                        )
                    }
                }
            }

            item {
                if (serverManagementViewModel.serverSetupState.value.isConnecting) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidthWithPadding())
                } else if (serverManagementViewModel.serverSetupState.value.isConnectedSuccessfully) {
                    HorizontalInfoCard(
                        info = localizedStrings.ServerIsReachable,
                        paddingValues = PaddingValues(start = 15.dp, end = 15.dp),
                    )
                } else {
                    Button(
                        onClick = {
                            serverManagementViewModel.testServerConnection(
                                serverUrl = serverUrl.value,
                                token = securityToken.value,
                            )
                        },
                        modifier =
                            Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                .fillMaxWidthWithPadding()
                                .pressScaleEffect()
                                .highlightOnFocused(shape = ButtonDefaults.shape),
                    ) {
                        Text(
                            text =
                                localizedStrings.TestServerAvailability,
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }
                    Spacer(Modifier.height(50.dp))
                }
            }
            if (!serverManagementViewModel.serverSetupState.value.isConnectedSuccessfully) {
                return@LazyColumn
            }
            item {
                HorizontalDivider(modifier = Modifier.fillMaxWidthWithPadding())
            }
            item {
                Text(
                    text = localizedStrings.SyncType,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.padding(start = 15.dp, end = 15.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
                SyncType.entries.forEachIndexed { index, syncType ->
                    if (index > 0) {
                        Spacer(Modifier.height(5.dp))
                    } else {
                        Spacer(Modifier.height(15.dp))
                    }
                    Column(
                        modifier =
                            Modifier.pointerHoverIcon(icon = PointerIcon.Hand).highlightOnFocused()
                                .clickable(
                                    onClick = {
                                        selectedSyncType.value = syncType
                                    },
                                    indication = null,
                                    interactionSource =
                                        remember {
                                            MutableInteractionSource()
                                        },
                                )
                                .pressScaleEffect()
                                .fillMaxWidthWithPadding(),
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand),
                                selected = syncType == selectedSyncType.value,
                                onClick = {
                                    selectedSyncType.value = syncType
                                },
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = syncType.asUIString(),
                                style = MaterialTheme.typography.titleMedium,
                            )
                        }
                        Text(
                            text = syncType.description(),
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 15.dp),
                        )
                    }
                }
            }
            item {
                Button(
                    onClick = {
                        serverManagementViewModel.saveServerConnectionAndSync(
                            serverConnection =
                                ServerConnection(
                                    serverUrl =
                                        serverUrl.value.substringBefore(SyncServerRoute.TEST_BEARER.name),
                                    authToken = securityToken.value,
                                    syncType = selectedSyncType.value,
                                ),
                            onSyncStart = {
                                showImportLogsFromServer = true
                            },
                            onCompletion = {
                                showImportLogsFromServer = false
                                navController.navigateUp()
                            },
                        )
                    },
                    modifier =
                        Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                            .fillMaxWidthWithPadding()
                            .pressScaleEffect().highlightOnFocused(shape = ButtonDefaults.shape),
                ) {
                    Text(
                        text = localizedStrings.UseThisConnection,
                        style = MaterialTheme.typography.titleMedium,
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
        operationDesc = localizedStrings.SyncingDataLabel,
        operationTitle = localizedStrings.InitiateManualSyncDescAlt,
        logs = serverManagementViewModel.dataSyncLogs,
        onCancel = {
            serverManagementViewModel.cancelServerConnectionAndSync()
        },
    )
}
