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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.KaptureOptions
import com.sakethh.linkora.data.local.repository.SnapshotRepoImpl
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.HostOS
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.PermissionStatus
import com.sakethh.linkora.domain.Preference
import com.sakethh.linkora.domain.asLinkType
import com.sakethh.linkora.domain.model.Folder
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.domain.onLoading
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.domain.repository.LocalizationRepo
import com.sakethh.linkora.domain.repository.local.LocalFoldersRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.LocalMultiActionRepo
import com.sakethh.linkora.domain.repository.local.LocalPanelsRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.local.SnapshotRepo
import com.sakethh.linkora.domain.repository.remote.SyncSessionService
import com.sakethh.linkora.platform.FileManager
import com.sakethh.linkora.platform.NativeUtils
import com.sakethh.linkora.platform.PermissionManager
import com.sakethh.linkora.platform.hostOS
import com.sakethh.linkora.ui.domain.AppAction
import com.sakethh.linkora.ui.domain.TransferActionType
import com.sakethh.linkora.ui.domain.model.LinkTagsPair
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM.Companion.clearAllSelections
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM.Companion.selectedFoldersViaLongClick
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM.Companion.selectedLinkTagPairsViaLongClick
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.collectIndefinitelyAndReportToUI
import com.sakethh.linkora.utils.currentSavedServerConfig
import com.sakethh.linkora.utils.getRemoteOnlyFailureMsg
import com.sakethh.linkora.utils.lastSyncedLocally
import com.sakethh.linkora.utils.pushSnackbar
import com.sakethh.linkora.utils.pushSnackbarOnFailure
import com.sakethh.linkora.utils.runSuspendCatching
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
class AppVM(
    private val density: Density,
    private val preferencesRepository: PreferencesRepository,
    private val linksRepo: LocalLinksRepo,
    private val foldersRepo: LocalFoldersRepo,
    private val localMultiActionRepo: LocalMultiActionRepo,
    private val localPanelsRepo: LocalPanelsRepo,
    private val permissionManager: PermissionManager,
    private val fileManager: FileManager,
    private val snapshotRepo: SnapshotRepo,
    private val localizationRepo: LocalizationRepo.Local,
    nativeUtils: NativeUtils,
    private val webCapture: NativeUtils.WebCapture,
    private val syncSessionService: SyncSessionService,
) : ViewModel() {
    val preferencesAsFlow = preferencesRepository.currentPreferencesStateAsFlow

    val localizedStrings
        get() = localizationRepo.localizedStrings.value

    var isPerformingDataSync = syncSessionService.isBatchSyncRunning

    val transferActionType = mutableStateOf(TransferActionType.NONE)

    val onBoardingCompleted = mutableStateOf(false)

    fun <T> updatePreference(
        key: Preference<T>,
        newValue: T,
        onCompletion: () -> Unit,
    ) {
        viewModelScope
            .launch {
                preferencesRepository.writePreferenceToDisk(
                    preference = key,
                    newValue = newValue,
                )
            }
            .invokeOnCompletion {
                onCompletion()
            }
    }

    val startDestination: Navigation.Root =
        nativeUtils.platformRunBlocking {
            val showOnboarding =
                preferencesRepository.readPreferenceFromDisk(
                    AppPreferences.SHOULD_SHOW_ONBOARDING,
                ) != false &&
                    linksRepo.isLinksTableEmpty() &&
                    foldersRepo.isFoldersTableEmpty() &&
                    localPanelsRepo.isPanelsTableEmpty()

            if (showOnboarding) {
                Navigation.Root.OnboardingSlidesScreen
            } else {
                onBoardingCompleted.value = true
                when (preferencesAsFlow.value.initialRouteId) {
                    Constants.HOME_SCREEN_ROUTE_ID ->
                        if (!preferencesAsFlow.value.isHomeScreenEnabled) {
                            Navigation.Root.CollectionsScreen
                        } else {
                            Navigation.Root.HomeScreen
                        }

                    Constants.SEARCH_SCREEN_ROUTE_ID -> Navigation.Root.SearchScreen

                    else -> Navigation.Root.CollectionsScreen
                }
            }
        } ?: Navigation.Root.HomeScreen

    fun performAppAction(appAction: AppAction) {
        when (appAction) {
            is AppAction.ArchiveSelectedItems ->
                archiveSelectedItems(
                    onStart = appAction.onStart,
                    onCompletion = appAction.onCompletion,
                )

            is AppAction.CopySelectedItems ->
                copySelectedItems(
                    folderId = appAction.folderId,
                    onStart = appAction.onStart,
                    onCompletion = appAction.onCompletion,
                )

            is AppAction.MarkSelectedFoldersAsRoot ->
                markSelectedFoldersAsRoot(
                    onStart = appAction.onStart,
                    onCompletion = appAction.onCompletion,
                )

            is AppAction.MarkSelectedItemsAsRegular ->
                markSelectedItemsAsRegular(
                    onStart = appAction.onStart,
                    onCompletion = appAction.onCompletion,
                )

            is AppAction.MoveSelectedItems ->
                moveSelectedItems(
                    folderId = appAction.folderId,
                    onStart = appAction.onStart,
                    onCompletion = appAction.onCompletion,
                )

            is AppAction.ForceDataSync ->
                forceSync(
                    onStart = appAction.onStart,
                    onCompletion = appAction.onCompletion,
                )
        }
    }

    var isAnySnapshotOngoing = snapshotFlow {
        snapshotRepo.isAnySnapshotOngoing.value
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), false)

    val isWebCaptureWorkerEnqueued =
        webCapture
            .isWebCaptureWorkerEnqueued()
            .stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5000L),
                false,
            )

    init {
        with(viewModelScope) {
            snapshotRepo.collectLatestAndExport()
        }
        viewModelScope.launch {
            snapshotFlow {
                CollectionsScreenVM.isSelectionEnabled.value
            }
                .collectLatest {
                    if (!it) {
                        transferActionType.value = TransferActionType.NONE
                    }
                }
        }

        viewModelScope.launch(Dispatchers.Default) {
            val preferences = preferencesAsFlow.value
            if (preferences.useWebCaptures) {
                webCapture.init(
                    options =
                        KaptureOptions(
                            userAgent = preferences.primaryJsoupUserAgent,
                            includeCss = preferences.webCaptureSaveCss,
                            includeImages = preferences.webCaptureSaveImages,
                            includeJs = preferences.webCaptureExecuteJs,
                            includeAudio = preferences.webCaptureSaveAudio,
                            includeVideo = preferences.webCaptureSaveVideo,
                            includeFonts = preferences.webCaptureSaveFonts,
                            includeMetadata = preferences.webCaptureSaveMetadata,
                        ),
                )
            }
        }
    }

    fun markOnboardingComplete() {
        viewModelScope
            .launch {
                preferencesRepository.writePreferenceToDisk(
                    preference = AppPreferences.SHOULD_SHOW_ONBOARDING,
                    newValue = false,
                )
            }
            .invokeOnCompletion {
                onBoardingCompleted.value = true
            }
    }

    fun openWebCaptureFolder(link: String) {
        viewModelScope.launch {
            fileManager.openWebCaptureFolder(link)
        }
    }

    fun downloadWebpage(link: String) {
        viewModelScope.launch {
            if (
                hostOS == HostOS.Android &&
                    permissionManager.permittedToShowNotification() == PermissionStatus.NeedsRequest
            )
                return@launch

            webCapture.saveHTMLPage(preferencesAsFlow.value.webCapturesLocation, link)
        }
    }

    companion object {
        var pauseSnapshots = false

        fun forceSnapshot() {
            SnapshotRepoImpl.forceTriggerASnapshot()
        }

        val isMainFabRotated = mutableStateOf(false)
    }

    fun moveSelectedItems(
        folderId: Long,
        onStart: () -> Unit,
        onCompletion: () -> Unit,
    ) {
        viewModelScope
            .launch {
                localMultiActionRepo
                    .moveMultipleItems(
                        linkIds =
                            selectedLinkTagPairsViaLongClick.map {
                                it.link.localId
                            },
                        folderIds =
                            selectedFoldersViaLongClick.map {
                                it.localId
                            },
                        linkType = folderId.asLinkType(),
                        newParentFolderId = folderId,
                    )
                    .collectLatest {
                        it.onLoading {
                            onStart()
                        }
                        it.pushSnackbarOnFailure()
                    }
            }
            .invokeOnCompletion {
                onCompletion()
                clearAllSelections()
            }
    }

    fun copySelectedItems(
        folderId: Long,
        onStart: () -> Unit,
        onCompletion: () -> Unit,
    ) {
        onStart()
        viewModelScope
            .launch {
                localMultiActionRepo
                    .copyMultipleItems(
                        linkTagsPairs = selectedLinkTagPairsViaLongClick.toList(),
                        folders = selectedFoldersViaLongClick.toList(),
                        linkType = folderId.asLinkType(),
                        newParentFolderId = folderId,
                    )
                    .collectLatest {
                        it.onLoading {
                            onStart()
                        }
                        it.pushSnackbarOnFailure()
                    }
            }
            .invokeOnCompletion {
                clearAllSelections()
                onCompletion()
            }
    }

    fun archiveSelectedItems(
        onStart: () -> Unit,
        onCompletion: () -> Unit,
    ) {
        onStart()
        viewModelScope
            .launch {
                localMultiActionRepo
                    .archiveMultipleItems(
                        linkIds =
                            selectedLinkTagPairsViaLongClick
                                .filter { it.link.linkType != LinkType.ARCHIVE_LINK }
                                .map { it.link.localId },
                        folderIds =
                            selectedFoldersViaLongClick
                                .filter { !it.isArchived }
                                .map { it.localId },
                    )
                    .collectLatest {
                        it.onSuccess {
                            this@launch.pushUIEvent(
                                UIEvent.Type.ShowSnackbar(
                                    localizedStrings.ArchivedSuccessfully +
                                        it.getRemoteOnlyFailureMsg(
                                            localizedStrings.RemoteExecutionFailed,
                                        ),
                                ),
                            )
                        }
                        it.pushSnackbarOnFailure()
                    }
            }
            .invokeOnCompletion {
                onCompletion()
                clearAllSelections()
            }
    }

    fun deleteSelectedItems(
        onStart: () -> Unit,
        onCompletion: () -> Unit,
    ) {
        onStart()
        viewModelScope
            .launch {
                localMultiActionRepo
                    .deleteMultipleItems(
                        linkIds = selectedLinkTagPairsViaLongClick.toList().map { it.link.localId },
                        folderIds = selectedFoldersViaLongClick.toList().map { it.localId },
                    )
                    .collectLatest {
                        it.onSuccess {
                            this@launch.pushUIEvent(
                                UIEvent.Type.ShowSnackbar(
                                    localizedStrings.DeletedSuccessfully +
                                        it.getRemoteOnlyFailureMsg(
                                            localizedStrings.RemoteExecutionFailed,
                                        ),
                                ),
                            )
                        }
                        it.pushSnackbarOnFailure()
                    }
            }
            .invokeOnCompletion {
                clearAllSelections()
                onCompletion()
            }
    }

    fun markSelectedFoldersAsRoot(
        onStart: () -> Unit,
        onCompletion: () -> Unit,
    ) {
        onStart()
        viewModelScope
            .launch {
                foldersRepo
                    .markFoldersAsRoot(
                        selectedFoldersViaLongClick.toList().map { it.localId },
                    )
                    .collect()
            }
            .invokeOnCompletion {
                selectedFoldersViaLongClick.clear()
                if (selectedLinkTagPairsViaLongClick.isEmpty()) {
                    clearAllSelections()
                }
                onCompletion()
            }
    }

    fun markSelectedItemsAsRegular(
        onStart: () -> Unit,
        onCompletion: () -> Unit,
    ) {
        onStart()
        viewModelScope
            .launch {
                localMultiActionRepo
                    .unArchiveMultipleItems(
                        folderIds =
                            selectedFoldersViaLongClick.filter { it.isArchived }.map { it.localId },
                        linkIds =
                            selectedLinkTagPairsViaLongClick
                                .filter { it.link.linkType == LinkType.ARCHIVE_LINK }
                                .map { it.link.localId },
                    )
                    .collect()
            }
            .invokeOnCompletion {
                clearAllSelections()
                onCompletion()
            }
    }

    val snackbarHostState = SnackbarHostState()
    var showLinkRenameDialogBox by mutableStateOf(false)
    var showFolderRenameDialogBox by mutableStateOf(false)

    var showLinkDeleteDialogBox by mutableStateOf(false)
    var showFolderDeleteDialogBox by mutableStateOf(false)
    var showDeleteAllSelectedItemsDialogBox by mutableStateOf(false)

    val menuBtmSheetState =
        SheetState(
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
    var selectedFolderForMenuBtmSheet by
        mutableStateOf(
            Folder(
                name = "",
                note = "",
                parentFolderId = null,
                localId = 0L,
                isArchived = false,
            ),
        )
    var selectedLinkTagsForMenuBtmSheet by
        mutableStateOf(
            LinkTagsPair(
                link =
                    Link(
                        linkType = LinkType.SAVED_LINK,
                        localId = 0L,
                        title = "",
                        url = "",
                        host = "",
                        imgURL = "",
                        note = "",
                        idOfLinkedFolder = null,
                        userAgent = null,
                    ),
                tags = emptyList(),
            ),
        )
    var showAddLinkDialog by mutableStateOf(false)
    var showNewFolderDialog by mutableStateOf(false)
    var showSortingBtmSheet by mutableStateOf(false)
    val sortingBtmSheetState =
        SheetState(
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

    var selectedTagForBtmTagSheet by mutableStateOf(Tag(localId = 0, name = ""))

    var showMenuForTag by mutableStateOf(false)
    var showMenuForFolder by mutableStateOf(false)
    var showMenuForLink by mutableStateOf(false)

    var showBtmSheetForNewTagAddition by mutableStateOf(false)

    init {
        viewModelScope.launch(Dispatchers.Default) {
            UIEvent.uiEvents.collectLatest { eventType ->
                when (eventType) {
                    is UIEvent.Type.ShowSnackbar -> {
                        withContext(Dispatchers.Main.immediate) {
                            snackbarHostState.showSnackbar(message = eventType.message)
                        }
                    }

                    is UIEvent.Type.ShowAddANewFolderDialogBox -> showNewFolderDialog = true

                    is UIEvent.Type.ShowAddANewLinkDialogBox -> {
                        showAddLinkDialog = true
                    }

                    is UIEvent.Type.ShowDeleteLinkDialogBox -> {
                        showLinkDeleteDialogBox = true
                    }

                    is UIEvent.Type.ShowDeleteFolderDialogBox -> {
                        showFolderDeleteDialogBox = true
                    }

                    is UIEvent.Type.ShowDeleteAllSelectedDialogBox -> {
                        showDeleteAllSelectedItemsDialogBox = true
                    }

                    is UIEvent.Type.ShowLinkMenuBtmSheet -> {
                        selectedLinkTagsForMenuBtmSheet = eventType.selectedLinkTagsPair
                        showMenuForLink = true
                    }

                    is UIEvent.Type.ShowFolderMenuBtmSheet -> {
                        selectedFolderForMenuBtmSheet = eventType.selectedFolder
                        showMenuForFolder = true
                    }

                    is UIEvent.Type.ShowRenameLinkDialogBox -> showLinkRenameDialogBox = true

                    is UIEvent.Type.ShowRenameFolderDialogBox -> showFolderRenameDialogBox = true

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

    fun forceSync(onStart: () -> Unit, onCompletion: () -> Unit) {
        onStart()
        viewModelScope.launch {
            runSuspendCatching {
                syncSessionService.stop()
                with({ localizedStrings }) {
                    syncSessionService
                        .start(
                            lastSynced = preferencesRepository.lastSyncedLocally(),
                            serverConnection = preferencesAsFlow.value.currentSavedServerConfig(),
                            correlation = preferencesAsFlow.value.correlation,
                            bypassCertCheck = preferencesAsFlow.value.skipCertCheckForSync,
                        )
                        .collectIndefinitelyAndReportToUI(
                            onConnectionFailure = onCompletion,
                            onNewLog = {},
                            onSyncBatchSuccessful = onCompletion,
                            onSyncBatchHadFailure = onCompletion,
                        )
                }
            }
                .onFailure {
                    it.printStackTrace()
                    it.pushSnackbar(this)
                }
        }
    }

    fun getAllLinksFromFolder(folderId: Long, onLinksFetched: (String) -> Unit) {
        viewModelScope.launch {
            val joinedLinks =
                linksRepo.getLinksOfThisFolderAsList(folderId).joinToString(separator = "\n") {
                    it.url
                }
            onLinksFetched(joinedLinks)
        }
    }
}
