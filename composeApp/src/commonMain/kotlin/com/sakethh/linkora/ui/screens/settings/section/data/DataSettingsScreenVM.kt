package com.sakethh.linkora.ui.screens.settings.section.data

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.ExportFileType
import com.sakethh.linkora.domain.ImportFileType
import com.sakethh.linkora.domain.LinkoraPlaceHolder
import com.sakethh.linkora.domain.PermissionStatus
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.onFailure
import com.sakethh.linkora.domain.onLoading
import com.sakethh.linkora.domain.onSuccess
import com.sakethh.linkora.domain.repository.ExportDataRepo
import com.sakethh.linkora.domain.repository.ImportDataRepo
import com.sakethh.linkora.domain.repository.local.LocalLinksRepo
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.domain.repository.remote.RemoteSyncRepo
import com.sakethh.linkora.platform.FileManager
import com.sakethh.linkora.platform.NativeUtils
import com.sakethh.linkora.platform.PermissionManager
import com.sakethh.linkora.preferences.AppPreferenceType
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.AppVM
import com.sakethh.linkora.ui.domain.ImportFileSelectionMethod
import com.sakethh.linkora.ui.screens.settings.SettingsScreenViewModel
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.utils.duplicate
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.getRemoteOnlyFailureMsg
import com.sakethh.linkora.utils.isNull
import com.sakethh.linkora.utils.pushSnackbarOnFailure
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class DataSettingsScreenVM(
    private val exportDataRepo: ExportDataRepo,
    private val importDataRepo: ImportDataRepo,
    private val linksRepo: LocalLinksRepo,
    private val preferencesRepository: PreferencesRepository,
    private val remoteSyncRepo: RemoteSyncRepo,
    private val nativeUtils: NativeUtils,
    private val fileManager: FileManager,
    private val permissionManager: PermissionManager
) : SettingsScreenViewModel(preferencesRepository, nativeUtils, permissionManager) {
    val importExportProgressLogs = mutableStateListOf<String>()

    private var importExportJob: Job? = null

    val isAnyRefreshingScheduledOnAndroid = mutableStateOf(false)

    init {
        viewModelScope.launch {
            nativeUtils.isAnyRefreshingScheduled().collectLatest {
                isAnyRefreshingScheduledOnAndroid.value = it == true
            }
        }
    }

    fun importDataFromAFile(
        importFileType: ImportFileType,
        onStart: () -> Unit,
        onCompletion: () -> Unit,
        importFileSelectionMethod: Pair<ImportFileSelectionMethod, String>
    ) {
        AppVM.pauseSnapshots = true
        importExportJob?.cancel()
        importExportJob = viewModelScope.launch(Dispatchers.Default) {
            val file =
                if (importFileSelectionMethod.first == ImportFileSelectionMethod.FileLocationString) {
                    File(importFileSelectionMethod.second).let {
                        if (it.exists() && it.extension.lowercase() == importFileType.name.lowercase()) {
                            onStart()
                            it
                        } else if (it.exists() && it.extension.lowercase() != importFileType.name.lowercase()) {
                            UIEvent.pushUIEvent(
                                UIEvent.Type.ShowSnackbar(
                                    Localization.Key.FileTypeNotSupportedOnDesktopImport.getLocalizedString()
                                        .replace(LinkoraPlaceHolder.First.value, it.extension)
                                        .replace(
                                            LinkoraPlaceHolder.Second.value, importFileType.name
                                        )
                                )
                            )
                            return@launch
                        } else {
                            null
                        }
                    }?.duplicate()
                } else {
                    fileManager.pickAValidFileForImporting(importFileType, onStart = {
                        onStart()
                        importExportProgressLogs.add(Localization.Key.ReadingFile.getLocalizedString())
                    })
                }
            if (file.isNull()) return@launch
            file as File
            if (importFileType == ImportFileType.JSON) {
                importDataRepo.importDataFromAJSONFile(file)
            } else {
                importDataRepo.importDataFromAHTMLFile(file)
            }.collectLatest {
                it.onLoading { importLogItem ->
                    importExportProgressLogs.add(importLogItem)
                }.onSuccess {
                    pushUIEvent(UIEvent.Type.ShowSnackbar(Localization.Key.SuccessfullyImportedTheData.getLocalizedString()))
                    file.delete()
                }.onFailure {
                    file.delete()
                }.pushSnackbarOnFailure()
            }
        }
        importExportJob?.invokeOnCompletion { cause ->
            AppVM.pauseSnapshots = false
            AppVM.forceSnapshot()
            onCompletion()
            cause?.printStackTrace()
            importExportProgressLogs.clear()
        }
    }

    suspend fun isStoragePermissionGranted(): Boolean {
        return permissionManager.isStorageAccessPermitted() is PermissionStatus.Granted
    }

    fun exportDataToAFile(
        platform: Platform,
        exportFileType: ExportFileType,
        onStart: () -> Unit,
        onCompletion: () -> Unit
    ) {
        importExportJob?.cancel()
        importExportJob = viewModelScope.launch {
            withContext(Dispatchers.Main) {
                if (platform is Platform.Android) {
                    when (permissionManager.isStorageAccessPermitted()) {
                        PermissionStatus.Granted -> onStart()
                        PermissionStatus.NeedsRequest -> importExportJob?.cancel()
                    }
                } else {
                    onStart()
                }
            }
            if (exportFileType == ExportFileType.JSON) {
                exportDataRepo.rawExportDataAsJSON()
            } else {
                exportDataRepo.rawExportDataAsHTML()
            }.collectLatest {
                it.onLoading { exportLogItem ->
                    importExportProgressLogs.add(exportLogItem)
                }.onSuccess {
                    try {
                        fileManager.writeRawExportStringToFile(
                            exportLocation = AppPreferences.currentExportLocation.value,
                            exportFileType = exportFileType,
                            rawExportString = it.data,
                            onCompletion = {
                                pushUIEvent(UIEvent.Type.ShowSnackbar(Localization.Key.ExportedSuccessfully.getLocalizedString()))
                            },
                            exportLocationType = ExportLocationType.EXPORT
                        )
                    } catch (e: Exception) {
                        e.printStackTrace()
                        pushUIEvent(UIEvent.Type.ShowSnackbar(message = e.message.toString()))
                    }
                }.pushSnackbarOnFailure()
            }
        }
        importExportJob?.invokeOnCompletion { cause ->
            onCompletion()
            cause?.printStackTrace()
            importExportProgressLogs.clear()
        }
    }

    fun cancelImportExportJob() {
        importExportJob?.cancel()
    }

    fun deleteEntireDatabase(deleteEverythingFromRemote: Boolean, onCompletion: () -> Unit) {
        AppVM.pauseSnapshots = true
        var remoteOperationFailed: Boolean? = null
        viewModelScope.launch {
            remoteSyncRepo.deleteEverything(deleteOnRemote = deleteEverythingFromRemote)
                .collectLatest {
                    it.onFailure {
                        remoteOperationFailed = true
                    }
                    it.onSuccess {
                        remoteOperationFailed = it.isRemoteExecutionSuccessful == false
                    }
                }
        }.invokeOnCompletion {
            viewModelScope.launch {
                pushUIEvent(
                    UIEvent.Type.ShowSnackbar(
                        if (remoteOperationFailed == null || !remoteOperationFailed) Localization.Key.DeletedEntireDataPermanently.getLocalizedString()
                        else Localization.Key.RemoteDataDeletionFailure.getLocalizedString()
                    )
                )
            }
            AppVM.pauseSnapshots = false
            onCompletion()
        }
    }

    companion object {
        val refreshLinksState = mutableStateOf(
            RefreshLinksState(
                isInRefreshingState = false, currentIteration = 0
            )
        )
        val totalLinksForRefresh = mutableStateOf(0)
    }

    fun refreshAllLinks() {
        AppVM.pauseSnapshots = true
        viewModelScope.launch {
            launch {
                permissionManager.permittedToShowNotification()
            }
            launch {
                nativeUtils.onRefreshAllLinks(
                    localLinksRepo = linksRepo, preferencesRepository = preferencesRepository
                )
            }
        }.invokeOnCompletion {
            AppVM.pauseSnapshots = false
        }
    }

    fun cancelRefreshingAllLinks() {
        nativeUtils.cancelRefreshingLinks()
    }

    fun deleteDuplicates(onStart: () -> Unit, onCompletion: () -> Unit) {
        AppVM.pauseSnapshots = true
        viewModelScope.launch {
            linksRepo.deleteDuplicateLinks().collectLatest {
                it.onSuccess {
                    onCompletion()
                    pushUIEvent(UIEvent.Type.ShowSnackbar(Localization.Key.DeletedDuplicatedLinksSuccessfully.getLocalizedString() + it.getRemoteOnlyFailureMsg()))
                }
                it.onLoading {
                    onStart()
                }
                it.onFailure {
                    onCompletion()
                    pushUIEvent(UIEvent.Type.ShowSnackbar(it))
                }
            }
        }.invokeOnCompletion {
            AppVM.pauseSnapshots = false
            AppVM.forceSnapshot()
        }
    }

    fun changeExportLocation(
        platform: Platform,
        // on desktop, exportLocation can be taken as direct string input, so this is fine
        exportLocation: String, exportLocationType: ExportLocationType
    ) {
        viewModelScope.launch {

            try {

                val newExportLocation =
                    if (platform == Platform.Desktop) exportLocation else fileManager.pickADirectory()
                        ?: throw NullPointerException("Looks like you skipped picking an export location.")

                preferencesRepository.changePreferenceValue(
                    preferenceKey = stringPreferencesKey(
                        if (exportLocationType == ExportLocationType.EXPORT) {
                            AppPreferenceType.EXPORT_LOCATION.name
                        } else {
                            AppPreferenceType.BACKUP_LOCATION.name
                        }
                    ), newValue = newExportLocation
                )

                if (exportLocationType == ExportLocationType.EXPORT) {
                    AppPreferences.currentExportLocation.value = newExportLocation
                } else {
                    AppPreferences.currentBackupLocation.value = newExportLocation
                }
            } catch (e: Exception) {
                e.printStackTrace()
                pushUIEvent(UIEvent.Type.ShowSnackbar(e.message.toString()))
            }
        }
    }

    fun updateAutoDeletionBackupsState(isEnabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.changePreferenceValue(
                preferenceKey = booleanPreferencesKey(
                    AppPreferenceType.BACKUP_AUTO_DELETION_ENABLED.name
                ), newValue = isEnabled
            )
            AppPreferences.backupAutoDeletionEnabled.value = isEnabled
        }
    }

    fun updateAutoDeletionBackupsThreshold(count: Int) {
        viewModelScope.launch {
            preferencesRepository.changePreferenceValue(
                preferenceKey = intPreferencesKey(
                    AppPreferenceType.BACKUP_AUTO_DELETION_THRESHOLD.name
                ), newValue = count
            )
            AppPreferences.backupAutoDeleteThreshold.intValue = count
        }
    }
}