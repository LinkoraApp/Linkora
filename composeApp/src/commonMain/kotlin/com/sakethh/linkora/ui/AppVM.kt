package com.sakethh.linkora.ui

import androidx.compose.material3.DatePickerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TimePickerState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asSkiaBitmap
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakethh.DataSyncingNotificationService
import com.sakethh.exportSnapshotData
import com.sakethh.linkora.common.Localization
import com.sakethh.linkora.common.preferences.AppPreferenceType
import com.sakethh.linkora.common.preferences.AppPreferences
import com.sakethh.linkora.common.utils.Constants
import com.sakethh.linkora.common.utils.getLocalizedString
import com.sakethh.linkora.common.utils.getRemoteOnlyFailureMsg
import com.sakethh.linkora.common.utils.pushSnackbar
import com.sakethh.linkora.common.utils.pushSnackbarOnFailure
import com.sakethh.linkora.domain.ExportFileType
import com.sakethh.linkora.domain.FileType
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.RemoteRoute
import com.sakethh.linkora.domain.asLinkType
import com.sakethh.linkora.domain.dto.server.AllTablesDTO
import com.sakethh.linkora.domain.model.JSONExportSchema
import com.sakethh.linkora.domain.model.PanelForJSONExportSchema
import com.sakethh.linkora.domain.model.Reminder
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
import com.sakethh.linkora.domain.repository.local.ReminderRepo
import com.sakethh.linkora.domain.repository.remote.RemoteSyncRepo
import com.sakethh.linkora.ui.domain.ReminderMode
import com.sakethh.linkora.ui.domain.TransferActionType
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM.Companion.clearAllSelections
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM.Companion.selectedFoldersViaLongClick
import com.sakethh.linkora.ui.screens.collections.CollectionsScreenVM.Companion.selectedLinksViaLongClick
import com.sakethh.linkora.ui.screens.settings.section.general.reminders.ReminderType
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.ui.utils.linkoraLog
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
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
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Base64
import java.util.Date

@OptIn(FlowPreview::class)
class AppVM(
    private val remoteSyncRepo: RemoteSyncRepo,
    private val preferencesRepository: PreferencesRepository,
    private val networkRepo: NetworkRepo,
    private val linksRepo: LocalLinksRepo,
    private val foldersRepo: LocalFoldersRepo,
    private val localMultiActionRepo: LocalMultiActionRepo,
    private val localPanelsRepo: LocalPanelsRepo,
    private val exportDataRepo: ExportDataRepo,
    private val reminderRepo: ReminderRepo
) : ViewModel() {

    private val dataSyncingNotificationService = DataSyncingNotificationService()
    val isPerformingStartupSync = mutableStateOf(false)

    val transferActionType = mutableStateOf(TransferActionType.NONE)
    val startDestination: MutableState<Navigation.Root> = mutableStateOf(Navigation.Root.HomeScreen)
    val onBoardingCompleted = mutableStateOf(false)

    private var snapshotsJob: Job? = null
    val isAnySnapshotOngoing = mutableStateOf(false)

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
                            localPanelsRepo.getAllThePanelFoldersAsAFlow()
                        ) { links, folders, panels, panelFolders ->
                            AllTablesDTO(
                                links = links,
                                folders = folders,
                                panels = panels,
                                panelFolders = panelFolders
                            )
                        }.cancellable()
                            .drop(1) // ignore the first emission which gets fired when the app launches
                            .debounce(1000).flowOn(Dispatchers.Default).collectLatest {
                                try {
                                    isAnySnapshotOngoing.value = true
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

                                    if (AppPreferences.snapshotsExportType.value.lowercase() == "both") {
                                        awaitAll(async {
                                            exportSnapshotData(
                                                rawExportString = serializedJsonExportString,
                                                fileType = FileType.JSON
                                            )
                                        }, async {
                                            exportSnapshotData(
                                                rawExportString = exportDataRepo.rawExportDataAsHTML(
                                                    links = it.links, folders = it.folders
                                                ), fileType = ExportFileType.HTML
                                            )
                                        })
                                    }

                                    if (AppPreferences.snapshotsExportType.value == ExportFileType.JSON.name) {
                                        exportSnapshotData(
                                            rawExportString = serializedJsonExportString,
                                            fileType = FileType.JSON
                                        )
                                    }

                                    if (AppPreferences.snapshotsExportType.value == ExportFileType.HTML.name) {
                                        exportSnapshotData(
                                            rawExportString = exportDataRepo.rawExportDataAsHTML(
                                                links = it.links, folders = it.folders
                                            ), fileType = ExportFileType.HTML
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

        readSocketEvents(viewModelScope, remoteSyncRepo)

        viewModelScope.launch {
            if (AppPreferences.isServerConfigured()) {
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

        fun shutdownSocketConnection() {
            socketEventJob?.cancel()
        }

        fun readSocketEvents(coroutineScope: CoroutineScope, remoteSyncRepo: RemoteSyncRepo) {
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
                    pushUIEvent(UIEvent.Type.ShowSnackbar("Archived successfully." + it.getRemoteOnlyFailureMsg()))
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
                    pushUIEvent(UIEvent.Type.ShowSnackbar("Deleted successfully." + it.getRemoteOnlyFailureMsg()))
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

    @OptIn(ExperimentalMaterial3Api::class)
    fun scheduleAReminder(
        linkId: Long,
        title: String,
        description: String,
        timePickerState: TimePickerState,
        datePickerState: DatePickerState,
        reminderType: ReminderType,
        reminderMode: ReminderMode,
        linkView: ImageBitmap
    ) {
        if (datePickerState.selectedDateMillis == null) return

        viewModelScope.launch {
            val selectedDate =
                SimpleDateFormat("yyyy\nMM\ndd").format(Date(datePickerState.selectedDateMillis!!))
                    .split("\n").map { it.toInt() }

            val localDate = Reminder.Date(
                selectedDate[0], selectedDate[1], selectedDate[2]
            )
            val localTime = Reminder.Time(
                timePickerState.hour, timePickerState.minute
            )
            val linkViewInBase64 = ByteArrayOutputStream().use {
                Image.makeFromBitmap(linkView.asSkiaBitmap())
                    .encodeToData(EncodedImageFormat.PNG)?.bytes
            }?.run {
                Base64.getEncoder().encodeToString(this)
            }
            val reminder = reminderRepo.getAReminder(
                reminderRepo.createAReminder(
                    Reminder(
                        linkId = linkId,
                        title = title,
                        description = description,
                        reminderType = reminderType,
                        reminderMode = reminderMode,
                        date = localDate,
                        time = localTime,
                        linkView = linkViewInBase64 ?: ""
                    )
                )
            )
            com.sakethh.scheduleAReminder(reminder = reminder, onCompletion = {
                linkoraLog("Scheduled $reminder")
            })
        }
    }
}