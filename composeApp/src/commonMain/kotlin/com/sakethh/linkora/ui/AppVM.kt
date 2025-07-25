package com.sakethh.linkora.ui

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.lifecycle.viewModelScope
import com.sakethh.DataSyncingNotificationService
import com.sakethh.deleteAutoBackups
import com.sakethh.linkora.common.Localization
import com.sakethh.linkora.common.network.Network
import com.sakethh.linkora.common.preferences.AppPreferenceType
import com.sakethh.linkora.common.preferences.AppPreferences
import com.sakethh.linkora.common.utils.Constants
import com.sakethh.linkora.common.utils.getLocalizedString
import com.sakethh.linkora.common.utils.getRemoteOnlyFailureMsg
import com.sakethh.linkora.common.utils.pushSnackbar
import com.sakethh.linkora.common.utils.pushSnackbarOnFailure
import com.sakethh.linkora.domain.SnapshotFormat
import com.sakethh.linkora.domain.ExportFileType
import com.sakethh.linkora.domain.FileType
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.RemoteRoute
import com.sakethh.linkora.domain.asLinkType
import com.sakethh.linkora.domain.dto.server.AllTablesDTO
import com.sakethh.linkora.domain.model.JSONExportSchema
import com.sakethh.linkora.domain.model.PanelForJSONExportSchema
import com.sakethh.linkora.domain.onFailure
import com.sakethh.linkora.domain.onLoading
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.domain.repository.ExportDataRepo
import com.sakethh.linkora.domain.repository.NetworkRepo
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalMultiActionRepo
import com.sakethh.linkora.domain.repository.local.LocalPanelsRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.remote.RemoteSyncRepo
import com.sakethh.linkora.ui.domain.TransferActionType
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM.Companion.clearAllSelections
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM.Companion.selectedFoldersViaLongClick
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM.Companion.selectedLinksViaLongClick
import com.sakethh.linkora.ui.screens.settings.section.data.sync.ServerManagementViewModel
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.ui.utils.linkoraLog
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.cancellable
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@OptIn(FlowPreview::class)
class AppVM(
    private val remoteSyncRepo: RemoteSyncRepo,
    preferencesRepository: PreferencesRepository,
    private val networkRepo: NetworkRepo,
    private val linksRepo: LocalLinksRepo,
    private val foldersRepo: LocalFoldersRepo,
    private val localMultiActionRepo: LocalMultiActionRepo,
    private val localPanelsRepo: LocalPanelsRepo,
    private val exportDataRepo: ExportDataRepo
) : ServerManagementViewModel(
    networkRepo = networkRepo,
    preferencesRepository = preferencesRepository,
    remoteSyncRepo = remoteSyncRepo,
    loadExistingCertificateInfo = false
) {

    private val dataSyncingNotificationService = DataSyncingNotificationService()
    val isPerformingStartupSync = mutableStateOf(false)

    val transferActionType = mutableStateOf(TransferActionType.NONE)
    val startDestination: MutableState<Navigation.Root> = mutableStateOf(Navigation.Root.HomeScreen)
    val onBoardingCompleted = mutableStateOf(false)

    private var snapshotsJob: Job? = null
    val isAnySnapshotOngoing = mutableStateOf(false)

    suspend fun getLastSyncedTime(): Long {
        return preferencesRepository.readPreferenceValue(
            preferenceKey = longPreferencesKey(AppPreferenceType.LAST_TIME_SYNCED_WITH_SERVER.name)
        ) ?: 0
    }

    init {

        runBlocking {
            startDestination.value = if (preferencesRepository.readPreferenceValue(
                    booleanPreferencesKey(
                        AppPreferenceType.SHOULD_SHOW_ONBOARDING.name
                    )
                ) != false && (linksRepo.getAllLinks().size + foldersRepo.getAllFoldersAsList().size + localPanelsRepo.getAllThePanelsAsAList().size) == 0
            ) {
                Navigation.Root.OnboardingSlidesScreen
            } else {
                onBoardingCompleted.value = true
                when (AppPreferences.startDestination.value) {
                    Navigation.Root.HomeScreen.toString() -> if (preferencesRepository.readPreferenceValue(
                            booleanPreferencesKey(
                                AppPreferenceType.HOME_SCREEN_VISIBILITY.name
                            )
                        ) == false
                    ) {
                        Navigation.Root.CollectionsScreen
                    } else {
                        Navigation.Root.HomeScreen
                    }

                    Navigation.Root.SearchScreen.toString() -> Navigation.Root.SearchScreen
                    else -> Navigation.Root.CollectionsScreen
                }
            }
        }

        viewModelScope.launch {
            snapshotFlow {
                AppPreferences.areSnapshotsEnabled.value
            }.debounce(1000).collectLatest {
                if (it) {
                    snapshotsJob = this.launch {
                        linkoraLog("data checks for snapshots are now live")
                        combine(
                            linksRepo.getAllLinksAsFlow(),
                            foldersRepo.getAllFoldersAsFlow(),
                            localPanelsRepo.getAllThePanels(),
                            localPanelsRepo.getAllThePanelFoldersAsAFlow(),
                            snapshotFlow {
                                forceSnapshot.value
                            }) { links, folders, panels, panelFolders, _ ->
                            AllTablesDTO(
                                links = links,
                                folders = folders,
                                panels = panels,
                                panelFolders = panelFolders
                            )
                        }.cancellable()
                            .drop(1) // ignore the first emission which gets fired when the app launches
                            .debounce(1000).flowOn(Dispatchers.Default).collectLatest {
                                if (pauseSnapshots || (it.links + it.folders + it.panelFolders + it.panels).isEmpty()) return@collectLatest
                                try {
                                    isAnySnapshotOngoing.value = true
                                    if (AppPreferences.isBackupAutoDeletionEnabled.value) {
                                        deleteAutoBackups(
                                            backupLocation = AppPreferences.currentBackupLocation.value,
                                            threshold = AppPreferences.backupAutoDeleteThreshold.intValue,
                                            onCompletion = {
                                                linkoraLog(
                                                    "Deleted $it snapshot files as the threshold was ${AppPreferences.backupAutoDeleteThreshold.intValue}"
                                                )
                                            })
                                    }

                                    if (AppPreferences.snapshotExportFormatID.value == SnapshotFormat.JSON.id.toString() || AppPreferences.snapshotExportFormatID.value == SnapshotFormat.BOTH.id.toString()) {

                                        val serializedJsonExportString = JSONExportSchema(
                                            schemaVersion = Constants.EXPORT_SCHEMA_VERSION,
                                            links = it.links.map {
                                                it.copy(
                                                    remoteId = null, lastModified = 0
                                                )
                                            },
                                            folders = it.folders.map {
                                                it.copy(
                                                    remoteId = null, lastModified = 0
                                                )
                                            },
                                            panels = PanelForJSONExportSchema(panels = it.panels.map {
                                                it.copy(
                                                    remoteId = null, lastModified = 0
                                                )
                                            }, panelFolders = it.panelFolders.map {
                                                it.copy(
                                                    remoteId = null, lastModified = 0
                                                )
                                            }),
                                        ).run {
                                            Json.encodeToString(this)
                                        }

                                        com.sakethh.exportSnapshotData(
                                            rawExportString = serializedJsonExportString,
                                            fileType = FileType.JSON,
                                            exportLocation = AppPreferences.currentBackupLocation.value
                                        )
                                    }

                                    if (AppPreferences.snapshotExportFormatID.value == SnapshotFormat.HTML.id.toString() || AppPreferences.snapshotExportFormatID.value == SnapshotFormat.BOTH.id.toString()) {
                                        com.sakethh.exportSnapshotData(
                                            rawExportString = exportDataRepo.rawExportDataAsHTML(
                                                links = it.links, folders = it.folders
                                            ),
                                            fileType = ExportFileType.HTML,
                                            exportLocation = AppPreferences.currentBackupLocation.value
                                        )
                                    }
                                } catch (e: Exception) {
                                    e.pushSnackbar()
                                } finally {
                                    isAnySnapshotOngoing.value = false
                                }
                            }
                    }
                } else {
                    linkoraLog("cancelled data checks for snapshots")
                    snapshotsJob?.cancel()
                }
            }
        }

        viewModelScope.launch {
            snapshotFlow {
                CollectionsScreenVM.isSelectionEnabled.value
            }.collectLatest {
                if (it.not()) {
                    transferActionType.value = TransferActionType.NONE
                }
            }
        }

        readSocketEvents(remoteSyncRepo)

        viewModelScope.launch {
            if (AppPreferences.isServerConfigured()) {
                try {
                    Network.configureSyncServerClient(
                        signedCertificate = getExistingSyncServerCertificate(),
                        bypassCertCheck = AppPreferences.skipCertCheckForSync.value
                    )
                } catch (e: Exception) {
                    pushUIEvent(UIEvent.Type.ShowSnackbar(e.message.toString()))
                }
                isPerformingStartupSync.value = true
                networkRepo.testServerConnection(
                    serverUrl = AppPreferences.serverBaseUrl.value + RemoteRoute.SyncInLocalRoute.TEST_BEARER.name,
                    token = AppPreferences.serverSecurityToken.value
                ).collectLatest {
                    it.onSuccess {
                        pushUIEvent(UIEvent.Type.ShowSnackbar(Localization.Key.SuccessfullyConnectedToTheServer.getLocalizedString()))
                        dataSyncingNotificationService.showNotification()
                        launch {
                            if (AppPreferences.canPushToServer()) {
                                with(remoteSyncRepo) {
                                    channelFlow {
                                        pushPendingSyncQueueToServer<Unit>().collectLatest {
                                            it.pushSnackbarOnFailure()
                                        }
                                    }.collect()
                                }
                            }
                        }

                        listOf(launch {
                            if (AppPreferences.canReadFromServer()) {
                                remoteSyncRepo.applyUpdatesBasedOnRemoteTombstones(
                                    AppPreferences.lastSyncedLocally(
                                        preferencesRepository
                                    )
                                ).collectLatest {
                                    it.pushSnackbarOnFailure()
                                }
                            }
                        }, launch {
                            if (AppPreferences.canReadFromServer()) {
                                remoteSyncRepo.applyUpdatesFromRemote(
                                    AppPreferences.lastSyncedLocally(
                                        preferencesRepository
                                    )
                                ).collectLatest {
                                    it.pushSnackbarOnFailure()
                                }
                            }
                        }).joinAll()
                    }.onFailure {
                        pushUIEvent(UIEvent.Type.ShowSnackbar(Localization.Key.ConnectionToServerFailed.getLocalizedString() + "\n" + it))
                    }
                }
            }
        }.invokeOnCompletion {
            isPerformingStartupSync.value = false
            dataSyncingNotificationService.clearNotification()
        }
    }

    fun markOnboardingComplete() {
        viewModelScope.launch {
            preferencesRepository.changePreferenceValue(
                preferenceKey = booleanPreferencesKey(
                    AppPreferenceType.SHOULD_SHOW_ONBOARDING.name
                ), newValue = false
            )
        }.invokeOnCompletion {
            onBoardingCompleted.value = true
        }
    }

    companion object {
        private var socketEventJob: Job? = null
        private val coroutineScope = CoroutineScope(Dispatchers.Default)

        var pauseSnapshots = false

        private val forceSnapshot = mutableStateOf(false)

        fun forceSnapshot() {
            forceSnapshot.value = !forceSnapshot.value
        }

        fun shutdownSocketConnection() {
            socketEventJob?.cancel()
        }

        fun readSocketEvents(remoteSyncRepo: RemoteSyncRepo) {
            if (AppPreferences.canReadFromServer().not()) return

            socketEventJob?.cancel()
            socketEventJob = coroutineScope.launch(CoroutineExceptionHandler { _, throwable ->
                throwable.printStackTrace()
                throwable.pushSnackbar(coroutineScope)
            }) {
                remoteSyncRepo.readSocketEvents(AppPreferences.getCorrelation()).collectLatest {
                    it.pushSnackbarOnFailure()
                }
            }
        }

        val isMainFabRotated = mutableStateOf(false)
    }

    fun moveSelectedItems(folderId: Long, onStart: () -> Unit, onCompletion: () -> Unit) {
        viewModelScope.launch {
            localMultiActionRepo.moveMultipleItems(linkIds = selectedLinksViaLongClick.map {
                it.localId
            }, folderIds = selectedFoldersViaLongClick.map {
                it.localId
            }, linkType = folderId.asLinkType(), newParentFolderId = folderId).collectLatest {
                it.onLoading {
                    onStart()
                }
                it.pushSnackbarOnFailure()
            }
        }.invokeOnCompletion {
            onCompletion()
            clearAllSelections()
        }
    }

    fun copySelectedItems(folderId: Long, onStart: () -> Unit, onCompletion: () -> Unit) {
        onStart()
        viewModelScope.launch {
            localMultiActionRepo.copyMultipleItems(
                links = selectedLinksViaLongClick.toList(),
                folders = selectedFoldersViaLongClick.toList(),
                linkType = folderId.asLinkType(),
                newParentFolderId = folderId
            ).collectLatest {
                it.onLoading {
                    onStart()
                }
                it.pushSnackbarOnFailure()
            }
        }.invokeOnCompletion {
            clearAllSelections()
            onCompletion()
        }
    }

    fun archiveSelectedItems(onStart: () -> Unit, onCompletion: () -> Unit) {
        onStart()
        viewModelScope.launch {
            localMultiActionRepo.archiveMultipleItems(
                linkIds = selectedLinksViaLongClick.filter { it.linkType != LinkType.ARCHIVE_LINK }
                .map { it.localId },
                folderIds = selectedFoldersViaLongClick.filter { it.isArchived.not() }
                    .map { it.localId }).collectLatest {
                it.onSuccess {
                    pushUIEvent(
                        UIEvent.Type.ShowSnackbar(
                            Localization.getLocalizedString(
                                Localization.Key.ArchivedSuccessfully
                            ) + it.getRemoteOnlyFailureMsg()
                        )
                    )
                }
                it.pushSnackbarOnFailure()
            }
        }.invokeOnCompletion {
            onCompletion()
            clearAllSelections()
        }
    }

    fun deleteSelectedItems(onStart: () -> Unit, onCompletion: () -> Unit) {
        onStart()
        viewModelScope.launch {
            localMultiActionRepo.deleteMultipleItems(
                linkIds = selectedLinksViaLongClick.toList()
                .map { it.localId },
                folderIds = selectedFoldersViaLongClick.toList().map { it.localId }).collectLatest {
                it.onSuccess {
                    pushUIEvent(
                        UIEvent.Type.ShowSnackbar(
                            Localization.getLocalizedString(
                                Localization.Key.DeletedSuccessfully
                            ) + it.getRemoteOnlyFailureMsg()
                        )
                    )
                }
                it.pushSnackbarOnFailure()
            }
        }.invokeOnCompletion {
            clearAllSelections()
            onCompletion()
        }
    }

    fun markSelectedFoldersAsRoot(onStart: () -> Unit, onCompletion: () -> Unit) {
        onStart()
        viewModelScope.launch {
            foldersRepo.markFoldersAsRoot(selectedFoldersViaLongClick.toList().map { it.localId })
                .collect()
        }.invokeOnCompletion {
            clearAllSelections()
            onCompletion()
        }
    }

    fun markSelectedItemsAsRegular(onStart: () -> Unit, onCompletion: () -> Unit) {
        onStart()
        viewModelScope.launch {
            localMultiActionRepo.unArchiveMultipleItems(
                folderIds = selectedFoldersViaLongClick.filter { it.isArchived }
                .map { it.localId },
                linkIds = selectedLinksViaLongClick.filter { it.linkType == LinkType.ARCHIVE_LINK }
                    .map { it.localId }).collect()
        }.invokeOnCompletion {
            clearAllSelections()
            onCompletion()
        }
    }
}