package com.sakethh.linkora.ui.screens.settings.section.data.sync

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.onFailure
import com.sakethh.linkora.domain.onLoading
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.domain.repository.NetworkRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.remote.RemoteSyncRepo
import com.sakethh.linkora.platform.FileManager
import com.sakethh.linkora.platform.Network
import com.sakethh.linkora.platform.PermissionManager
import com.sakethh.linkora.ui.AppVM
import com.sakethh.linkora.ui.domain.model.ServerConnection
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.utils.booleanPreferencesKey
import com.sakethh.linkora.utils.canPushToServer
import com.sakethh.linkora.utils.canReadFromServer
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.pushSnackbarOnFailure
import com.sakethh.linkora.utils.stringPreferencesKey
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

open class ServerManagementViewModel(
    private val networkRepo: NetworkRepo,
    val preferencesRepository: PreferencesRepository,
    private val remoteSyncRepo: RemoteSyncRepo,
    private val fileManager: FileManager,
    private val permissionManager: PermissionManager,
    private val network: Network,
) : ViewModel() {
    val preferencesAsFlow = preferencesRepository.preferencesAsFlow
    val serverSetupState = mutableStateOf(
        ServerSetupState(
            isConnecting = false, isConnectedSuccessfully = false, isError = false
        )
    )

    fun testServerConnection(serverUrl: String, token: String) {
        viewModelScope.launch {
            try {

                network.closeSyncServerClient()
                network.configureSyncServerClient(
                    bypassCertCheck = preferencesRepository.getPreferences().skipCertCheckForSync
                )

                networkRepo.testServerConnection(serverUrl, token).collectLatest {
                    it.onSuccess {
                        serverSetupState.value = ServerSetupState(
                            isConnecting = false, isConnectedSuccessfully = true, isError = false
                        )
                        permissionManager.permittedToShowNotification()
                    }.onFailure { failureMessage ->
                        serverSetupState.value = ServerSetupState(
                            isConnecting = false, isConnectedSuccessfully = false, isError = true
                        )
                        pushUIEvent(UIEvent.Type.ShowSnackbar(failureMessage))
                    }.onLoading {
                        serverSetupState.value = ServerSetupState(
                            isConnecting = true, isConnectedSuccessfully = false, isError = false
                        )
                    }
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
            preferencesRepository.changePreferenceValue(
                preferenceKey = stringPreferencesKey(
                    AppPreferences.SERVER_URL.key
                ), newValue = ""
            )
            preferencesRepository.changePreferenceValue(
                preferenceKey = stringPreferencesKey(
                    AppPreferences.SERVER_AUTH_TOKEN.key
                ), newValue = ""
            )
        }
    }

    private var syncServerCertificate: ByteArray? = null
    fun saveServerConnectionAndSync(
        serverConnection: ServerConnection,
        timeStampAfter: suspend () -> Long = { 0 },
        onSyncStart: () -> Unit,
        onCompletion: () -> Unit
    ) {
        AppVM.pauseSnapshots = true
        saveServerConnectionAndSyncJob?.cancel()
        dataSyncLogs.clear()
        saveServerConnectionAndSyncJob = viewModelScope.launch {
            preferencesRepository.changePreferenceValue(
                preferenceKey = stringPreferencesKey(
                    AppPreferences.SERVER_URL.key
                ), newValue = serverConnection.serverUrl
            )


            preferencesRepository.changePreferenceValue(
                preferenceKey = stringPreferencesKey(AppPreferences.SERVER_AUTH_TOKEN.key),
                newValue = serverConnection.authToken
            )

            preferencesRepository.changePreferenceValue(
                preferenceKey = stringPreferencesKey(
                    AppPreferences.SERVER_SYNC_TYPE.key
                ), newValue = serverConnection.syncType.name
            )
            onSyncStart()
            val preferences = preferencesRepository.getPreferences()
            if (preferences.canReadFromServer()) {
                remoteSyncRepo.applyUpdatesFromRemote(timeStampAfter()).collectLatest {
                    it.onLoading {
                        dataSyncLogs.add(it)
                    }.onSuccess {
                        AppVM.readSocketEvents(remoteSyncRepo, preferences)
                    }.onFailure { _ ->
                        it.pushSnackbarOnFailure()
                        cancel()
                    }
                }
            }
            if (preferences.canPushToServer()) {
                with(remoteSyncRepo) {
                    channelFlow {
                        this.pushNonSyncedDataToServer<Unit>()
                    }.collectLatest {
                        it.onLoading {
                            dataSyncLogs.add(it)
                        }
                        it.onSuccess {
                            onCompletion()
                        }
                        it.onFailure {
                            cancel()
                        }.pushSnackbarOnFailure()
                    }
                }
            }
        }
        saveServerConnectionAndSyncJob?.invokeOnCompletion {
            if (it == null) {
                viewModelScope.launch {
                    pushUIEvent(UIEvent.Type.ShowSnackbar(Localization.Key.DataSynchronizationCompletedSuccessfully.getLocalizedString()))
                }
            }
            AppVM.pauseSnapshots = false
            AppVM.forceSnapshot()
            onCompletion()
            dataSyncLogs.clear()
        }
    }

    val existingCertificateInfo = mutableStateOf("")

    fun deleteTheConnection(onDeleted: () -> Unit) {
        viewModelScope.launch {
            preferencesRepository.changePreferenceValue(
                preferenceKey = stringPreferencesKey(
                    AppPreferences.SERVER_URL.key
                ), newValue = ""
            )

            preferencesRepository.changePreferenceValue(
                preferenceKey = stringPreferencesKey(AppPreferences.SERVER_AUTH_TOKEN.key),
                newValue = ""
            )

            preferencesRepository.changePreferenceValue(
                preferenceKey = stringPreferencesKey(
                    AppPreferences.SERVER_SYNC_TYPE.key
                ), newValue = ""
            )

            AppVM.shutdownSocketConnection()
            network.closeSyncServerClient()

            pushUIEvent(UIEvent.Type.ShowSnackbar(Localization.getLocalizedString(Localization.Key.DeletedTheServerConnectionSuccessfully)))
        }.invokeOnCompletion {
            onDeleted()
        }
    }

    private var certImportJob: Job? = null

    fun importSignedCertificate(onStart: () -> Unit, onCompletion: (filename: String) -> Unit) {
        syncServerCertificate = null
        onStart()
        certImportJob?.cancel()
        var certInfo = ""
        certImportJob = viewModelScope.launch {
            syncServerCertificate = fileManager.getSyncServerCertificate(
                onCompletion = { info ->
                    certInfo = info
                })
            syncServerCertificate?.let { syncServerCertificate ->
                fileManager.saveSyncServerCertificateInternally(
                    certificate = syncServerCertificate, onCompletion = {
                        onCompletion(certInfo)
                        pushUIEvent(
                            UIEvent.Type.ShowSnackbar(
                                Localization.getLocalizedString(
                                    Localization.Key.ServerCertificateSavedSuccessfully
                                )
                            )
                        )
                    })
            }
        }
    }

    fun updateCertificateBypassRule(bypass: Boolean) {
        viewModelScope.launch {
            preferencesRepository.changePreferenceValue(
                preferenceKey = booleanPreferencesKey(AppPreferences.SKIP_CERT_CHECK_FOR_SYNC_SERVER.key),
                newValue = bypass
            )
        }
    }
}