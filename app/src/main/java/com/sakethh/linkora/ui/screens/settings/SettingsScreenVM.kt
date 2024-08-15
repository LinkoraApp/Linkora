package com.sakethh.linkora.ui.screens.settings

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShortText
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SystemUpdateAlt
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.UriHandler
import androidx.core.content.ContextCompat
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.lifecycle.viewModelScope
import androidx.work.WorkInfo
import androidx.work.WorkManager
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.sakethh.linkora.R
import com.sakethh.linkora.data.local.LocalDatabase
import com.sakethh.linkora.data.local.RecentlyVisited
import com.sakethh.linkora.data.local.backup.ExportRepo
import com.sakethh.linkora.data.local.links.LinksRepo
import com.sakethh.linkora.data.local.restore.ImportRepo
import com.sakethh.linkora.data.remote.releases.GitHubReleasesRepo
import com.sakethh.linkora.data.remote.releases.GitHubReleasesResult
import com.sakethh.linkora.data.remote.releases.model.GitHubReleaseDTOItem
import com.sakethh.linkora.ui.CommonUiEvent
import com.sakethh.linkora.ui.screens.CustomWebTab
import com.sakethh.linkora.ui.screens.settings.SettingsPreference.dataStore
import com.sakethh.linkora.ui.screens.settings.SettingsPreference.isSendCrashReportsEnabled
import com.sakethh.linkora.worker.RefreshLinksWorker
import com.sakethh.linkora.worker.RefreshLinksWorkerRequestBuilder
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class SettingsUIElement(
    val title: String,
    val doesDescriptionExists: Boolean,
    val description: String?,
    val isSwitchNeeded: Boolean,
    val isSwitchEnabled: MutableState<Boolean>,
    val onSwitchStateChange: (newValue: Boolean) -> Unit,
    val onAcknowledgmentClick: (uriHandler: UriHandler, context: Context) -> Unit = { _, _ -> },
    val icon: ImageVector? = null,
    val isIconNeeded: MutableState<Boolean>,
    val shouldFilledIconBeUsed: MutableState<Boolean> = mutableStateOf(false),
    val shouldArrowIconBeAppear: MutableState<Boolean> = mutableStateOf(false)
)

@HiltViewModel
open class SettingsScreenVM @Inject constructor(
    private val linksRepo: LinksRepo,
    private val importRepo: ImportRepo,
    private val localDatabase: LocalDatabase,
    private val exportRepo: ExportRepo,
    private val gitHubReleasesRepo: GitHubReleasesRepo,
    private val refreshLinksWorkerRequestBuilder: RefreshLinksWorkerRequestBuilder,
    private val workManager: WorkManager
) : CustomWebTab(linksRepo) {

    val shouldDeleteDialogBoxAppear = mutableStateOf(false)
    val exceptionType: MutableState<String?> = mutableStateOf(null)

    init {
        viewModelScope.launch {
            RefreshLinksWorkerRequestBuilder.REFRESH_LINKS_WORKER_TAG.collectLatest { uuid ->
                workManager.getWorkInfoByIdFlow(uuid).collectLatest {
                    if (it != null) {
                        isAnyRefreshingTaskGoingOn.value =
                            it.state == WorkInfo.State.RUNNING || it.state == WorkInfo.State.ENQUEUED
                    }
                }
            }
        }
    }

    fun refreshAllLinksImagesAndTitles() {
        viewModelScope.launch {
            refreshLinksWorkerRequestBuilder.request()
        }
    }

    fun cancelRefreshAllLinksImagesAndTitlesWork() {
        RefreshLinksWorker.superVisorJob?.cancel()
        workManager.cancelAllWork()
    }

    companion object {
        val isAnyRefreshingTaskGoingOn = mutableStateOf(false)
        val currentSelectedSettingSection = mutableStateOf(SettingsSections.THEME)
        const val APP_VERSION_NAME = "v0.6.0"
        const val APP_VERSION_CODE = 24
        private val _latestReleaseInfoFromGitHubReleases = MutableStateFlow(
            GitHubReleaseDTOItem(
                assets = listOf(),
                body = "",
                createdAt = "",
                releasePageURL = "",
                releaseName = ""
            )
        )
        val latestReleaseInfoFromGitHubReleases = _latestReleaseInfoFromGitHubReleases.asStateFlow()
    }

    val acknowledgmentsSection: (context: Context) -> List<SettingsUIElement> = {
        listOf(
            SettingsUIElement(
                title = it.getString(R.string.kotlin),
                doesDescriptionExists = true,
                description = it.getString(R.string.apache_license),
                isSwitchNeeded = false,
                isSwitchEnabled = mutableStateOf(false),
                onSwitchStateChange = {},
                onAcknowledgmentClick = { uriHandler, context ->
                    viewModelScope.launch {
                        openInWeb(
                            recentlyVisitedData = RecentlyVisited(
                                title = "Kotlin - GitHub",
                                webURL = "https://github.com/JetBrains/kotlin",
                                baseURL = "github.com",
                                imgURL = "https://avatars.githubusercontent.com/u/1446536",
                                infoForSaving = "",
                            ),
                            uriHandler = uriHandler,
                            context = context,
                            forceOpenInExternalBrowser = false
                        )
                    }
                },
                isIconNeeded = mutableStateOf(false),
                shouldArrowIconBeAppear = mutableStateOf(true)
            ),
            SettingsUIElement(
                title = it.getString(R.string.android_Jetpack),
                doesDescriptionExists = true,
                description = it.getString(R.string.apache_license),
                isSwitchNeeded = false,
                isSwitchEnabled = mutableStateOf(false),
                onSwitchStateChange = {},
                onAcknowledgmentClick = { uriHandler, context ->
                    viewModelScope.launch {
                        openInWeb(
                            recentlyVisitedData = RecentlyVisited(
                                title = "androidx - GitHub",
                                webURL = "https://github.com/androidx/androidx",
                                baseURL = "github.com",
                                imgURL = "https://play-lh.googleusercontent.com/PCpXdqvUWfCW1mXhH1Y_98yBpgsWxuTSTofy3NGMo9yBTATDyzVkqU580bfSln50bFU",
                                infoForSaving = "",
                            ),
                            uriHandler = uriHandler,
                            context = context,
                            forceOpenInExternalBrowser = false
                        )
                    }
                },
                shouldArrowIconBeAppear = mutableStateOf(true),
                isIconNeeded = mutableStateOf(false)
            ),
            SettingsUIElement(
                title = it.getString(R.string.coil),
                doesDescriptionExists = true,
                description = it.getString(R.string.apache_license),
                isSwitchNeeded = false,
                isSwitchEnabled = mutableStateOf(false),
                onSwitchStateChange = {

                },
                onAcknowledgmentClick = { uriHandler, context ->
                    viewModelScope.launch {
                        openInWeb(
                            recentlyVisitedData = RecentlyVisited(
                                title = "Coil - GitHub",
                                webURL = "https://github.com/coil-kt/coil",
                                baseURL = "github.com",
                                imgURL = "https://avatars.githubusercontent.com/u/52722434",
                                infoForSaving = "",
                            ),
                            uriHandler = uriHandler,
                            context = context,
                            forceOpenInExternalBrowser = false
                        )
                    }
                },
                shouldArrowIconBeAppear = mutableStateOf(true),
                isIconNeeded = mutableStateOf(false)
            ),
            SettingsUIElement(
                title = "jsoup",
                doesDescriptionExists = true,
                description = "MIT License",
                isSwitchNeeded = false,
                isSwitchEnabled = mutableStateOf(false),
                onSwitchStateChange = {

                },
                onAcknowledgmentClick = { uriHandler, context ->
                    viewModelScope.launch {
                        openInWeb(
                            recentlyVisitedData = RecentlyVisited(
                                title = "jsoup - GitHub",
                                webURL = "https://github.com/jhy/jsoup",
                                baseURL = "github.com",
                                imgURL = "https://jsoup.org/rez/jsoup%20logo%20twitter.png",
                                infoForSaving = "jsoup on GitHub",
                            ),
                            uriHandler = uriHandler,
                            context = context,
                            forceOpenInExternalBrowser = false
                        )
                    }
                },
                shouldArrowIconBeAppear = mutableStateOf(true),
                isIconNeeded = mutableStateOf(false)
            ),
            SettingsUIElement(
                title = it.getString(R.string.material_design_3),
                doesDescriptionExists = true,
                description = it.getString(R.string.apache_license),
                isSwitchNeeded = false,
                isSwitchEnabled = mutableStateOf(false),
                onSwitchStateChange = {

                },
                onAcknowledgmentClick = { uriHandler, context ->
                    viewModelScope.launch {
                        openInWeb(
                            recentlyVisitedData = RecentlyVisited(
                                title = "Material 3",
                                webURL = "https://m3.material.io/",
                                baseURL = "material.io",
                                imgURL = "https://upload.wikimedia.org/wikipedia/commons/thumb/c/c7/Google_Material_Design_Logo.svg/512px-Google_Material_Design_Logo.svg.png",
                                infoForSaving = "",
                            ),
                            uriHandler = uriHandler,
                            context = context,
                            forceOpenInExternalBrowser = false
                        )
                    }
                },
                shouldArrowIconBeAppear = mutableStateOf(true),
                isIconNeeded = mutableStateOf(false)
            ),
            SettingsUIElement(
                title = "Accompanist",
                doesDescriptionExists = true,
                description = it.getString(R.string.apache_license),
                isSwitchNeeded = false,
                isSwitchEnabled = mutableStateOf(false),
                onSwitchStateChange = {

                },
                onAcknowledgmentClick = { uriHandler, context ->
                    viewModelScope.launch {
                        openInWeb(
                            recentlyVisitedData = RecentlyVisited(
                                title = "Accompanist",
                                webURL = "https://google.github.io/accompanist/",
                                baseURL = "github.io",
                                imgURL = "https://google.github.io/accompanist/header.png",
                                infoForSaving = "",
                            ),
                            uriHandler = uriHandler,
                            context = context,
                            forceOpenInExternalBrowser = false
                        )
                    }
                },
                shouldArrowIconBeAppear = mutableStateOf(true),
                isIconNeeded = mutableStateOf(false)
            ),
            SettingsUIElement(
                title = "kotlinx.serialization",
                doesDescriptionExists = true,
                description = it.getString(R.string.apache_license),
                isSwitchNeeded = false,
                isSwitchEnabled = mutableStateOf(false),
                onSwitchStateChange = {

                },
                onAcknowledgmentClick = { uriHandler, context ->
                    viewModelScope.launch {
                        openInWeb(
                            recentlyVisitedData = RecentlyVisited(
                                title = "kotlinx.serialization - GitHub",
                                webURL = "https://github.com/Kotlin/kotlinx.serialization",
                                baseURL = "github.com",
                                imgURL = "https://avatars.githubusercontent.com/u/1446536?v=4",
                                infoForSaving = "kotlinx.serialization on GitHub",
                            ),
                            uriHandler = uriHandler,
                            context = context,
                            forceOpenInExternalBrowser = false
                        )
                    }
                },
                shouldArrowIconBeAppear = mutableStateOf(true),
                isIconNeeded = mutableStateOf(false)
            ),
            SettingsUIElement(
                title = it.getString(R.string.material_icons),
                doesDescriptionExists = true,
                description = it.getString(R.string.apache_license),
                isSwitchNeeded = false,
                isSwitchEnabled = mutableStateOf(false),
                onSwitchStateChange = {

                },
                onAcknowledgmentClick = { uriHandler, context ->
                    viewModelScope.launch {
                        openInWeb(
                            recentlyVisitedData = RecentlyVisited(
                                title = "Material Icons - GitHub",
                                webURL = "https://github.com/google/material-design-icons",
                                baseURL = "google.com",
                                imgURL = "",
                                infoForSaving = "Material Icons on GitHub",
                            ),
                            uriHandler = uriHandler,
                            context = context,
                            forceOpenInExternalBrowser = false
                        )
                    }
                },
                isIconNeeded = mutableStateOf(false),
                shouldArrowIconBeAppear = mutableStateOf(true)
            ),
        )
    }


    val privacySection: (context: Context) -> SettingsUIElement = { context ->
        SettingsUIElement(
            title = context.getString(R.string.send_crash_reports),
            doesDescriptionExists = true,
            description = if (!isSendCrashReportsEnabled.value) mutableStateOf(context.getString(R.string.every_single_bit_of_data_is_stored_locally_on_your_device)).value else mutableStateOf(
                context.getString(R.string.linkora_collects_data_related_to_app_crashes)
            ).value,
            isSwitchNeeded = true,
            isSwitchEnabled = isSendCrashReportsEnabled,
            isIconNeeded = mutableStateOf(true),
            icon = Icons.Default.BugReport,
            onSwitchStateChange = {
                viewModelScope.launch {
                    SettingsPreference.changeSettingPreferenceValue(
                        preferenceKey = booleanPreferencesKey(
                            SettingsPreferences.SEND_CRASH_REPORTS.name
                        ),
                        dataStore = context.dataStore,
                        newValue = !isSendCrashReportsEnabled.value
                    )
                    isSendCrashReportsEnabled.value = !isSendCrashReportsEnabled.value
                }.invokeOnCompletion {
                    val firebaseCrashlytics = FirebaseCrashlytics.getInstance()
                    firebaseCrashlytics.setCrashlyticsCollectionEnabled(isSendCrashReportsEnabled.value)
                }
            })
    }
    val generalSection: (context: Context) -> List<SettingsUIElement> = { context ->
        listOf(
            SettingsUIElement(title = context.getString(R.string.use_in_app_browser),
                doesDescriptionExists = SettingsPreference.showDescriptionForSettingsState.value,
                description = context.getString(R.string.use_in_app_browser_desc),
                isSwitchNeeded = true,
                isSwitchEnabled = SettingsPreference.isInAppWebTabEnabled,
                isIconNeeded = mutableStateOf(true),
                icon = Icons.Default.OpenInBrowser,
                onSwitchStateChange = {
                    viewModelScope.launch {
                        SettingsPreference.changeSettingPreferenceValue(
                            preferenceKey = booleanPreferencesKey(
                                SettingsPreferences.CUSTOM_TABS.name
                            ), dataStore = context.dataStore, newValue = it
                        )
                        SettingsPreference.isInAppWebTabEnabled.value = it
                    }
                }), SettingsUIElement(
                title = context.getString(R.string.enable_home_screen),
                doesDescriptionExists = SettingsPreference.showDescriptionForSettingsState.value,
                description = context.getString(R.string.enable_home_screen_desc),
                isSwitchNeeded = true,
                isIconNeeded = mutableStateOf(true),
                icon = Icons.Default.Home,
                isSwitchEnabled = SettingsPreference.isHomeScreenEnabled,
                onSwitchStateChange = {
                    viewModelScope.launch {
                        SettingsPreference.changeSettingPreferenceValue(
                            preferenceKey = booleanPreferencesKey(
                                SettingsPreferences.HOME_SCREEN_VISIBILITY.name
                            ), dataStore = context.dataStore, newValue = it
                        )
                        SettingsPreference.isHomeScreenEnabled.value = it
                    }
                }), SettingsUIElement(
                title = context.getString(R.string.auto_detect_title),
                doesDescriptionExists = true,
                description = context.getString(R.string.auto_detect_title_desc),
                isSwitchNeeded = true,
                isSwitchEnabled = SettingsPreference.isAutoDetectTitleForLinksEnabled,
                isIconNeeded = mutableStateOf(true),
                icon = Icons.Default.Search,
                onSwitchStateChange = {
                    viewModelScope.launch {
                        SettingsPreference.changeSettingPreferenceValue(
                            preferenceKey = booleanPreferencesKey(
                                SettingsPreferences.AUTO_DETECT_TITLE_FOR_LINK.name
                            ), dataStore = context.dataStore, newValue = it
                        )
                        SettingsPreference.isAutoDetectTitleForLinksEnabled.value = it
                    }
                }), SettingsUIElement(title = context.getString(R.string.auto_check_for_updates),
                doesDescriptionExists = SettingsPreference.showDescriptionForSettingsState.value,
                description = context.getString(R.string.auto_check_for_updates_desc),
                isIconNeeded = mutableStateOf(true),
                icon = Icons.Default.SystemUpdateAlt,
                isSwitchNeeded = true,
                isSwitchEnabled = SettingsPreference.isAutoCheckUpdatesEnabled,
                onSwitchStateChange = {
                    viewModelScope.launch {
                        SettingsPreference.changeSettingPreferenceValue(
                            preferenceKey = booleanPreferencesKey(
                                SettingsPreferences.AUTO_CHECK_UPDATES.name
                            ), dataStore = context.dataStore, newValue = it
                        )
                        SettingsPreference.isAutoCheckUpdatesEnabled.value = it
                    }
                }), SettingsUIElement(
                title = context.getString(R.string.show_description_for_settings),
                doesDescriptionExists = true,
                description = context.getString(R.string.show_description_for_settings_desc),
                isSwitchNeeded = true,
                isIconNeeded = mutableStateOf(true),
                icon = Icons.AutoMirrored.Default.ShortText,
                isSwitchEnabled = SettingsPreference.showDescriptionForSettingsState,
                onSwitchStateChange = {
                    viewModelScope.launch {
                        SettingsPreference.changeSettingPreferenceValue(
                            preferenceKey = booleanPreferencesKey(
                                SettingsPreferences.SETTING_COMPONENT_DESCRIPTION_STATE.name
                            ), dataStore = context.dataStore, newValue = it
                        )
                        SettingsPreference.showDescriptionForSettingsState.value = it
                    }
                })
        )
    }

    fun importData(
        exceptionType: MutableState<String?>,
        json: String,
        shouldErrorDialogBeVisible: MutableState<Boolean>
    ) {
        viewModelScope.launch {
            importRepo.importToLocalDB(
                exceptionType = exceptionType,
                jsonString = json,
                shouldErrorDialogBeVisible = shouldErrorDialogBeVisible
            )
        }
    }

    private val _eventChannel = Channel<CommonUiEvent>()
    val eventChannel = _eventChannel.receiveAsFlow()

    private suspend fun pushUiEvent(commonUiEvent: CommonUiEvent) {
        _eventChannel.send(commonUiEvent)
    }

    fun exportDataToAFile(
        context: Context,
        isDialogBoxVisible: MutableState<Boolean>,
        runtimePermission: ManagedActivityResultLauncher<String, Boolean>
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            viewModelScope.launch {
                exportRepo.exportToAFile()
                pushUiEvent(CommonUiEvent.ShowToast(R.string.successfully_exported))
            }
        } else {
            when (ContextCompat.checkSelfPermission(
                context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )) {
                PackageManager.PERMISSION_GRANTED -> {
                    viewModelScope.launch {
                        exportRepo.exportToAFile()
                        pushUiEvent(CommonUiEvent.ShowToast(R.string.successfully_exported))
                    }
                    isDialogBoxVisible.value = false
                }

                else -> {
                    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                        runtimePermission.launch(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        viewModelScope.launch {
                            pushUiEvent(CommonUiEvent.ShowToast(R.string.permission_denied_title))
                        }
                    }
                }
            }
        }
    }

    fun dataSection(
        runtimePermission: ManagedActivityResultLauncher<String, Boolean>,
        context: Context,
        isDialogBoxVisible: MutableState<Boolean>,
        activityResultLauncher: ManagedActivityResultLauncher<String, Uri?>,
        importModalBtmSheetState: MutableState<Boolean>
    ): List<SettingsUIElement> {
        return listOf(
            SettingsUIElement(
                isIconNeeded = mutableStateOf(true),
                title = context.getString(R.string.import_data),
                doesDescriptionExists = true,
                description = context.getString(R.string.import_data_from_external_JSON_file),
                isSwitchNeeded = false,
                isSwitchEnabled = SettingsPreference.shouldFollowDynamicTheming,
                onSwitchStateChange = {
                    viewModelScope.launch {
                        if (linksRepo
                                .isHistoryLinksTableEmpty() && linksRepo
                                .isImpLinksTableEmpty() && linksRepo
                                .isLinksTableEmpty() && linksRepo
                                .isArchivedFoldersTableEmpty() && linksRepo
                                .isFoldersTableEmpty() && linksRepo
                                .isArchivedLinksTableEmpty()
                        ) {
                            activityResultLauncher.launch("text/*")
                        } else {
                            importModalBtmSheetState.value = true
                        }
                    }
                },
                icon = Icons.Default.FileDownload,
                shouldFilledIconBeUsed = mutableStateOf(true)
            ),
            SettingsUIElement(
                isIconNeeded = mutableStateOf(true),
                title = context.getString(R.string.export_data),
                doesDescriptionExists = true,
                description = context.getString(R.string.export_data_desc),
                isSwitchNeeded = false,
                isSwitchEnabled = SettingsPreference.shouldFollowDynamicTheming,
                onSwitchStateChange = {
                    exportDataToAFile(context, isDialogBoxVisible, runtimePermission)
                },
                icon = Icons.Default.FileUpload,
                shouldFilledIconBeUsed = mutableStateOf(true)
            ),
            SettingsUIElement(
                isIconNeeded = mutableStateOf(true),
                title = context.getString(R.string.delete_entire_data_permanently),
                doesDescriptionExists = true,
                description = context.getString(R.string.delete_entire_data_permanently_desc),
                isSwitchNeeded = false,
                isSwitchEnabled = SettingsPreference.shouldFollowDynamicTheming,
                onSwitchStateChange = {
                    shouldDeleteDialogBoxAppear.value = true
                },
                icon = Icons.Default.DeleteForever,
                shouldFilledIconBeUsed = mutableStateOf(true)
            ),
        )
    }

    fun latestAppVersionRetriever(onTaskCompleted: () -> Unit) {
        viewModelScope.launch {
            when (val latestReleaseData = gitHubReleasesRepo.getLatestVersionData()) {
                is GitHubReleasesResult.Failure -> {

                }

                is GitHubReleasesResult.Success -> {
                    _latestReleaseInfoFromGitHubReleases.emit(latestReleaseData.data)
                }
            }
        }.invokeOnCompletion {
            onTaskCompleted()
        }
    }

    fun deleteEntireLinksAndFoldersData(onTaskCompleted: () -> Unit = {}) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                localDatabase.clearAllTables()
            }
        }.invokeOnCompletion {
            onTaskCompleted()
        }
    }
}
