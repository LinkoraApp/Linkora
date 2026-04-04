package com.sakethh.linkora.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.Localization
import com.sakethh.linkora.data.local.repository.SnapshotRepoImpl
import com.sakethh.linkora.di.LinkoraSDK
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.PreferenceKey
import com.sakethh.linkora.domain.SyncServerRoute
import com.sakethh.linkora.domain.asLinkType
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.domain.onFailure
import com.sakethh.linkora.domain.onLoading
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.domain.repository.NetworkRepo
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalMultiActionRepo
import com.sakethh.linkora.domain.repository.local.LocalPanelsRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.local.SnapshotRepo
import com.sakethh.linkora.domain.repository.remote.RemoteSyncRepo
import com.sakethh.linkora.platform.FileManager
import com.sakethh.linkora.platform.NativeUtils
import com.sakethh.linkora.platform.PermissionManager
import com.sakethh.linkora.ui.components.menu.MenuBtmSheetType
import com.sakethh.linkora.ui.domain.AppAction
import com.sakethh.linkora.ui.domain.TransferActionType
import com.sakethh.linkora.ui.domain.model.LinkTagsPair
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM.Companion.clearAllSelections
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM.Companion.selectedFoldersViaLongClick
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM.Companion.selectedLinkTagPairsViaLongClick
import com.sakethh.linkora.ui.screens.settings.section.data.sync.ServerManagementViewModel
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.ui.utils.linkoraLog
import com.sakethh.linkora.utils.booleanPreferencesKey
import com.sakethh.linkora.utils.canPushToServer
import com.sakethh.linkora.utils.canReadFromServer
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.getRemoteOnlyFailureMsg
import com.sakethh.linkora.utils.isServerConfigured
import com.sakethh.linkora.utils.lastSyncedLocally
import com.sakethh.linkora.utils.longPreferencesKey
import com.sakethh.linkora.utils.pushSnackbar
import com.sakethh.linkora.utils.pushSnackbarOnFailure
import com.sakethh.linkora.utils.stringPreferencesKey
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch

@OptIn(FlowPreview::class, ExperimentalMaterial3Api::class)
class AppVM(
    private val density: Density,
    private val remoteSyncRepo: RemoteSyncRepo,
    preferencesRepository: PreferencesRepository,
    private val networkRepo: NetworkRepo,
    private val linksRepo: LocalLinksRepo,
    private val foldersRepo: LocalFoldersRepo,
    private val localMultiActionRepo: LocalMultiActionRepo,
    private val localPanelsRepo: LocalPanelsRepo,
    permissionManager: PermissionManager,
    fileManager: FileManager,
    private val dataSyncingNotificationService: NativeUtils.DataSyncingNotificationService,
    private val snapshotRepo: SnapshotRepo,
    nativeUtils: NativeUtils
) : ServerManagementViewModel(
    networkRepo = networkRepo,
    preferencesRepository = preferencesRepository,
    remoteSyncRepo = remoteSyncRepo,
    permissionManager = permissionManager,
    fileManager = fileManager,
    network = LinkoraSDK.getInstance().network
) {

    var isPerformingStartupSync by mutableStateOf(false)

    val transferActionType = mutableStateOf(TransferActionType.NONE)

    val onBoardingCompleted = mutableStateOf(false)

    fun <T> updatePreference(key: PreferenceKey<T>, newValue:T, onCompletion: () -> Unit){
        viewModelScope.launch {
            preferencesRepository.changePreferenceValue(
                preferenceKey = key,
                newValue = newValue
            )
        }.invokeOnCompletion {
            onCompletion()
        }
    }

    val startDestination: Navigation.Root = nativeUtils.platformRunBlocking {
        val showOnboarding = preferencesRepository.readPreferenceValue(
            booleanPreferencesKey(
                AppPreferences.SHOULD_SHOW_ONBOARDING.key
            )
        ) != false && linksRepo.isLinksTableEmpty() && foldersRepo.isFoldersTableEmpty() && localPanelsRepo.isPanelsTableEmpty()

        if (showOnboarding) {
            Navigation.Root.OnboardingSlidesScreen
        } else {
            onBoardingCompleted.value = true
            when (preferencesRepository.readPreferenceValue(stringPreferencesKey(AppPreferences.INITIAL_ROUTE.key))) {
                Navigation.Root.HomeScreen.toString() -> if (preferencesRepository.readPreferenceValue(
                        booleanPreferencesKey(
                            AppPreferences.HOME_SCREEN_VISIBILITY.key
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
    } ?: Navigation.Root.CollectionsScreen


    fun performAppAction(appAction: AppAction) {
        when (appAction) {
            is AppAction.ArchiveSelectedItems -> archiveSelectedItems(
                onStart = appAction.onStart,
                onCompletion = appAction.onCompletion
            )

            is AppAction.CopySelectedItems -> copySelectedItems(
                folderId = appAction.folderId,
                onStart = appAction.onStart,
                onCompletion = appAction.onCompletion
            )

            is AppAction.MarkSelectedFoldersAsRoot -> markSelectedFoldersAsRoot(
                onStart = appAction.onStart,
                onCompletion = appAction.onCompletion
            )

            is AppAction.MarkSelectedItemsAsRegular -> markSelectedItemsAsRegular(
                onStart = appAction.onStart,
                onCompletion = appAction.onCompletion
            )

            is AppAction.MoveSelectedItems -> moveSelectedItems(
                folderId = appAction.folderId,
                onStart = appAction.onStart,
                onCompletion = appAction.onCompletion
            )

            is AppAction.SaveServerConnectionAndSync -> saveServerConnectionAndSync(
                serverConnection = appAction.serverConnection,
                timeStampAfter = appAction.timeStampAfter,
                onSyncStart = appAction.onSyncStart,
                onCompletion = appAction.onCompletion
            )
        }
    }

    suspend fun getLastSyncedTime(): Long {
        return preferencesRepository.readPreferenceValue(
            preferenceKey = longPreferencesKey(AppPreferences.LAST_TIME_SYNCED_WITH_SERVER.key)
        ) ?: 0
    }

    var isAnySnapshotOngoing by mutableStateOf(false)

    init {
        with(viewModelScope) {
            snapshotRepo.collectLatestAndExport()
        }

        viewModelScope.launch {
            snapshotFlow {
                snapshotRepo.isAnySnapshotOngoing.value
            }.collectLatest {
                if (it) {
                    linkoraLog("Snapshot in progress")
                } else {
                    linkoraLog("No snapshot in progress")
                }
                isAnySnapshotOngoing = it
            }
        }

        viewModelScope.launch {
            snapshotFlow {
                CollectionsScreenVM.isSelectionEnabled.value
            }.collectLatest {
                if (!it) {
                    transferActionType.value = TransferActionType.NONE
                }
            }
        }


        viewModelScope.launch {
            val preferences = preferencesRepository.getPreferences()
            readSocketEvents(remoteSyncRepo, preferences)

            if (preferences.isServerConfigured()) {
                try {
                    LinkoraSDK.getInstance().network.configureSyncServerClient(
                        bypassCertCheck = preferences.skipCertCheckForSync
                    )
                } catch (e: Exception) {
                    pushUIEvent(UIEvent.Type.ShowSnackbar(e.message.toString()))
                }
                isPerformingStartupSync = true
                // TODO: NESTED collectLatest
                networkRepo.testServerConnection(
                    serverUrl = preferences.serverBaseUrl + SyncServerRoute.TEST_BEARER.name,
                    token = preferences.serverSecurityToken
                ).collectLatest {
                    it.onSuccess {
                        pushUIEvent(UIEvent.Type.ShowSnackbar(Localization.Key.SuccessfullyConnectedToTheServer.getLocalizedString()))
                        dataSyncingNotificationService.showNotification()
                        launch {
                            if (preferences.canPushToServer()) {
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
                            if (preferences.canReadFromServer()) {
                                remoteSyncRepo.applyUpdatesBasedOnRemoteTombstones(
                                    preferences.lastSyncedLocally(
                                        preferencesRepository
                                    )
                                ).collectLatest {
                                    it.pushSnackbarOnFailure()
                                }
                            }
                        }, launch {
                            if (preferences.canReadFromServer()) {
                                remoteSyncRepo.applyUpdatesFromRemote(
                                    preferences.lastSyncedLocally(
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
            isPerformingStartupSync = false
            dataSyncingNotificationService.clearNotification()
        }
    }

    fun markOnboardingComplete() {
        viewModelScope.launch {
            preferencesRepository.changePreferenceValue(
                preferenceKey = booleanPreferencesKey(
                    AppPreferences.SHOULD_SHOW_ONBOARDING.key
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


        fun forceSnapshot() {
            SnapshotRepoImpl.forceTriggerASnapshot()
        }

        fun shutdownSocketConnection() {
            socketEventJob?.cancel()
        }

        fun readSocketEvents(remoteSyncRepo: RemoteSyncRepo, preferences: AppPreferences) {
            if (preferences.canReadFromServer().not()) return

            socketEventJob?.cancel()
            socketEventJob = coroutineScope.launch(CoroutineExceptionHandler { _, throwable ->
                throwable.printStackTrace()
                throwable.pushSnackbar(coroutineScope)
            }) {
                remoteSyncRepo.readSocketEvents(preferences.correlation).collectLatest {
                    it.pushSnackbarOnFailure()
                }
            }
        }

        val isMainFabRotated = mutableStateOf(false)
    }

    fun moveSelectedItems(folderId: Long, onStart: () -> Unit, onCompletion: () -> Unit) {
        viewModelScope.launch {
            localMultiActionRepo.moveMultipleItems(linkIds = selectedLinkTagPairsViaLongClick.map {
                it.link.localId
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
                linkTagsPairs = selectedLinkTagPairsViaLongClick.toList(),
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
                linkIds = selectedLinkTagPairsViaLongClick.filter { it.link.linkType != LinkType.ARCHIVE_LINK }
                    .map { it.link.localId },
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
                linkIds = selectedLinkTagPairsViaLongClick.toList().map { it.link.localId },
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
                linkIds = selectedLinkTagPairsViaLongClick.filter { it.link.linkType == LinkType.ARCHIVE_LINK }
                    .map { it.link.localId }).collect()
        }.invokeOnCompletion {
            clearAllSelections()
            onCompletion()
        }
    }


    val snackbarHostState = SnackbarHostState()
    var showRenameDialogBox by mutableStateOf(false)
    var showDeleteDialogBox by mutableStateOf(false)

    val menuBtmSheetState = SheetState(
        skipPartiallyExpanded = true,
        positionalThreshold = {
            with(density) {
                56.dp.toPx()
            }
        },
        velocityThreshold = {
            with(density) {
                125.dp.toPx()
            }
        },
    )
    var selectedFolderForMenuBtmSheet by mutableStateOf(
        Folder(
            name = "", note = "", parentFolderId = null, localId = 0L, isArchived = false
        )
    )
    var selectedLinkTagsForMenuBtmSheet by mutableStateOf(
        LinkTagsPair(
            link = Link(
                linkType = LinkType.SAVED_LINK,
                localId = 0L,
                title = "",
                url = "",
                host = "",
                imgURL = "",
                note = "",
                idOfLinkedFolder = null,
                userAgent = null
            ), tags = emptyList()
        )
    )
    var showMenuSheet by mutableStateOf(false)
    var showAddLinkDialog by mutableStateOf(false)
    var showNewFolderDialog by mutableStateOf(false)
    var showSortingBtmSheet by mutableStateOf(false)
    val sortingBtmSheetState = SheetState(
        skipPartiallyExpanded = true,
        positionalThreshold = {
            with(density) {
                56.dp.toPx()
            }
        },
        velocityThreshold = {
            with(density) {
                125.dp.toPx()
            }
        },
    )

    var menuBtmSheetFor: MenuBtmSheetType by mutableStateOf(MenuBtmSheetType.Folder.RegularFolder)

    var selectedTagForBtmTagSheet by mutableStateOf(Tag(localId = 0, name = ""))

    var showMenuForTag by mutableStateOf(false)

    var showBtmSheetForNewTagAddition by mutableStateOf(false)

    init {
        viewModelScope.launch(Dispatchers.Default) {
            UIEvent.uiEvents.collectLatest { eventType ->
                when (eventType) {
                    is UIEvent.Type.ShowSnackbar -> {
                        snackbarHostState.showSnackbar(message = eventType.message)
                    }

                    is UIEvent.Type.ShowAddANewFolderDialogBox -> showNewFolderDialog = true
                    is UIEvent.Type.ShowAddANewLinkDialogBox -> showAddLinkDialog = true
                    is UIEvent.Type.ShowDeleteDialogBox -> showDeleteDialogBox = true

                    is UIEvent.Type.ShowMenuBtmSheet -> {
                        menuBtmSheetFor = eventType.menuBtmSheetFor
                        if (eventType.selectedFolderForMenuBtmSheet != null) {
                            selectedFolderForMenuBtmSheet = eventType.selectedFolderForMenuBtmSheet
                        }
                        if (eventType.selectedLinkForMenuBtmSheet != null) {
                            selectedLinkTagsForMenuBtmSheet = eventType.selectedLinkForMenuBtmSheet
                        }
                        showMenuSheet = true
                    }

                    is UIEvent.Type.ShowRenameDialogBox -> showRenameDialogBox = true

                    is UIEvent.Type.ShowSortingBtmSheet -> {
                        showSortingBtmSheet = true
                    }

                    is UIEvent.Type.ShowTagMenuBtmSheet -> {
                        selectedTagForBtmTagSheet = eventType.selectedTag
                        showMenuForTag = true
                    }

                    is UIEvent.Type.ShowCreateTagBtmSheet -> showBtmSheetForNewTagAddition = true
                    else -> Unit
                }
            }
        }
    }
}