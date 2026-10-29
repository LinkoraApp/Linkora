package com.sakethh.linkora.ui.screens.settings.section.data.sync

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.SaveServerConnectionAndSyncUseCase
import com.sakethh.linkora.domain.model.ServerConnection
import com.sakethh.linkora.domain.repository.LocalizationRepo
import com.sakethh.linkora.domain.repository.NetworkRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.remote.SyncSessionService
import com.sakethh.linkora.platform.FileManager
import com.sakethh.linkora.platform.Network
import com.sakethh.linkora.platform.PermissionManager
import com.sakethh.linkora.ui.AppVM
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.utils.collectIndefinitelyAndReportToUI
import com.sakethh.linkora.utils.pushSnackbar
import com.sakethh.linkora.utils.runSuspendCatching
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

open class ServerManagementViewModel(
    private val networkRepo: NetworkRepo,
    val preferencesRepository: PreferencesRepository,
    private val serverConnectionAndSyncUseCase: SaveServerConnectionAndSyncUseCase,
    private val syncSessionService: SyncSessionService,
    private val fileManager: FileManager,
    private val permissionManager: PermissionManager,
    private val network: Network,
    private val localizationRepo: LocalizationRepo.Local,
) : ViewModel() {
    val preferencesAsFlow = preferencesRepository.currentPreferencesStateAsFlow
    val localizedStrings
        get() = localizationRepo.localizedStrings.value

    val serverSetupState =
        mutableStateOf(
            ServerSetupState(
                isConnecting = false,
                isConnectedSuccessfully = false,
                isError = false,
            ),
        )

    fun reconfigureSyncServerClientAndTestConnection(
        serverUrl: String,
        token: String,
    ) {
        viewModelScope.launch {
            try {
                network.closeSyncServerClient()
                network.configureSyncServerClient(
                    bypassCertCheck =
                        preferencesRepository.getCurrentPreferencesState().skipCertCheckForSync,
                )

                serverSetupState.value =
                    ServerSetupState(
                        isConnecting = true,
                        isConnectedSuccessfully = false,
                        isError = false,
                    )

                val isSyncServerReachable =
                    try {
                        networkRepo.isSyncServerReachable(serverUrl, token)
                    } catch (e: Exception) {
                        pushUIEvent(UIEvent.Type.ShowSnackbar(e.message.toString()))
                        false
                    }

                if (isSyncServerReachable) {
                    serverSetupState.value =
                        ServerSetupState(
                            isConnecting = false,
                            isConnectedSuccessfully = true,
                            isError = false,
                        )
                    permissionManager.permittedToShowNotification()
                } else {
                    serverSetupState.value =
                        ServerSetupState(
                            isConnecting = false,
                            isConnectedSuccessfully = false,
                            isError = true,
                        )
                }
            } catch (e: Exception) {
                pushUIEvent(UIEvent.Type.ShowSnackbar(e.message.toString()))
            }
        }
    }

    val dataSyncLogs = mutableStateListOf<String>()
    private var saveServerConnectionAndSyncJob: Job? = null

    fun cancelServerConnectionAndSync(removeConnection: Boolean = true) {
        network.closeSyncServerClient()
        saveServerConnectionAndSyncJob?.cancel()
        if (!removeConnection) {
            return
        }
        viewModelScope.launch {
            preferencesRepository.writePreferenceToDisk(
                preference = AppPreferences.SERVER_URL,
                newValue = "",
            )
            preferencesRepository.writePreferenceToDisk(
                preference = AppPreferences.SERVER_AUTH_TOKEN,
                newValue = "",
            )
        }
    }

    private var syncServerCertificate: ByteArray? = null

    fun saveServerConnectionAndSync(
        serverConnection: ServerConnection,
        timeStampAfter: suspend () -> Long,
        onSyncStart: () -> Unit,
        onCompletion: () -> Unit,
    ) {
        fun finalInvocation() {
            AppVM.pauseSnapshots = false
            AppVM.forceSnapshot()
            onCompletion()
            dataSyncLogs.clear()
        }
        AppVM.pauseSnapshots = true
        saveServerConnectionAndSyncJob?.cancel()
        dataSyncLogs.clear()
        saveServerConnectionAndSyncJob = viewModelScope.launch {
            onSyncStart()
            runSuspendCatching {
                with({ localizedStrings }) {
                    serverConnectionAndSyncUseCase(
                            lastSynced = timeStampAfter(),
                            serverConnection = serverConnection,
                        )
                        .collectIndefinitelyAndReportToUI(
                            onConnectionFailure = { finalInvocation() },
                            onNewLog = { str ->
                                dataSyncLogs.add(str)
                            },
                            onSyncBatchHadFailure = {
                                pushUIEvent(
                                    UIEvent.Type.ShowSnackbar(
                                        "onSyncBatchHadFailure",
                                    ),
                                )
                                finalInvocation()
                            },
                            onSyncBatchSuccessful = {
                                pushUIEvent(
                                    UIEvent.Type.ShowSnackbar(
                                        localizedStrings.DataSynchronizationCompletedSuccessfully,
                                    ),
                                )
                                finalInvocation()
                            },
                        )
                }
            }
                .onSuccess {
                    // sync session has been cancelled/stopped
                    finalInvocation()
                }
                .onFailure {
                    finalInvocation()
                    it.pushSnackbar(this)
                }
        }
    }

    val existingCertificateInfo = mutableStateOf("")

    fun deleteTheConnection(onDeleted: () -> Unit) {
        viewModelScope
            .launch {
                syncSessionService.stop()

                network.closeSyncServerClient()

                preferencesRepository.writePreferenceToDisk(
                    preference = AppPreferences.SERVER_URL,
                    newValue = "",
                )

                preferencesRepository.writePreferenceToDisk(
                    preference = AppPreferences.SERVER_AUTH_TOKEN,
                    newValue = "",
                )

                pushUIEvent(
                    UIEvent.Type.ShowSnackbar(
                        localizedStrings.DeletedTheServerConnectionSuccessfully,
                    ),
                )
            }
            .invokeOnCompletion {
                onDeleted()
            }
    }

    private var certImportJob: Job? = null

    fun importSignedCertificate(
        onStart: () -> Unit,
        onCompletion: (filename: String) -> Unit,
    ) {
        syncServerCertificate = null
        onStart()
        certImportJob?.cancel()
        var certInfo = ""
        certImportJob = viewModelScope.launch {
            syncServerCertificate =
                fileManager.getSyncServerCertificate(
                    onCompletion = { info ->
                        certInfo = info
                    },
                )
            syncServerCertificate?.let { syncServerCertificate ->
                fileManager.saveSyncServerCertificateInternally(
                    certificate = syncServerCertificate,
                    onCompletion = {
                        onCompletion(certInfo)
                        pushUIEvent(
                            UIEvent.Type.ShowSnackbar(
                                localizedStrings.ServerCertificateSavedSuccessfully,
                            ),
                        )
                    },
                )
            }
        }
    }

    fun updateCertificateBypassRule(bypass: Boolean) {
        viewModelScope.launch {
            preferencesRepository.writePreferenceToDisk(
                preference = AppPreferences.SKIP_CERT_CHECK_FOR_SYNC_SERVER,
                newValue = bypass,
            )
        }
    }
}
