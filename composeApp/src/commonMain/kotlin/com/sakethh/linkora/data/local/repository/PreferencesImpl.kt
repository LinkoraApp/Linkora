package com.sakethh.linkora.data.local.repository

import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.PreferenceKey
import com.sakethh.linkora.domain.RefreshLinkType
import com.sakethh.linkora.domain.SyncType
import com.sakethh.linkora.domain.repository.local.PreferencesRepository
import com.sakethh.linkora.platform.PlatformPreference
import com.sakethh.linkora.ui.domain.Font
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class PreferencesImpl(
    private val platformPreference: PlatformPreference
) : PreferencesRepository {

    private val _preferences = MutableStateFlow(AppPreferences())
    override val preferencesAsFlow: StateFlow<AppPreferences> = _preferences.asStateFlow()

    override fun getPreferences(): AppPreferences {
        return _preferences.value
    }

    override suspend fun loadPersistedPreferences() {
        _preferences.value = platformPreference.readAllPreferences()
    }

    override suspend fun <T> changePreferenceValue(
        preferenceKey: PreferenceKey<T>, newValue: T
    ) {
        platformPreference.writePreferenceValue(
            preferenceKey = preferenceKey, newValue = newValue
        )
        updateStateMemory(preferenceKey, newValue)
    }

    override suspend fun <T> readPreferenceValue(
        preferenceKey: PreferenceKey<T>,
    ): T? {
        return platformPreference.readPreferenceValue(
            preferenceKey = preferenceKey
        )
    }

    @Suppress("UNCHECKED_CAST")
    private fun <T> updateStateMemory(key: PreferenceKey<T>, value: T) {
        _preferences.update { currentPref ->
            when (key) {
                AppPreferences.DARK_THEME -> currentPref.copy(useDarkTheme = value as Boolean)
                AppPreferences.FOLLOW_SYSTEM_THEME -> currentPref.copy(useSystemTheme = value as Boolean)
                AppPreferences.AMOLED_THEME_STATE -> currentPref.copy(useAmoledTheme = value as Boolean)
                AppPreferences.DYNAMIC_THEMING -> currentPref.copy(useDynamicTheming = value as Boolean)
                AppPreferences.AUTO_DETECT_TITLE_FOR_LINK -> currentPref.copy(
                    isAutoDetectTitleForLinksEnabled = value as Boolean
                )

                AppPreferences.ASSOCIATED_IMAGES_IN_LINK_MENU_VISIBILITY -> currentPref.copy(
                    showAssociatedImageInLinkMenu = value as Boolean
                )

                AppPreferences.HOME_SCREEN_VISIBILITY -> currentPref.copy(isHomeScreenEnabled = value as Boolean)
                AppPreferences.USE_REMOTE_LANGUAGE_STRINGS -> currentPref.copy(useRemoteStrings = value as Boolean)
                AppPreferences.SORTING_PREFERENCE -> currentPref.copy(selectedSortingType = value as String)
                AppPreferences.JSOUP_USER_AGENT -> currentPref.copy(primaryJsoupUserAgent = value as String)
                AppPreferences.LOCALIZATION_SERVER_URL -> currentPref.copy(localizationServerURL = value as String)
                AppPreferences.APP_LANGUAGE_NAME -> currentPref.copy(preferredAppLanguageName = value as String)
                AppPreferences.APP_LANGUAGE_CODE -> currentPref.copy(preferredAppLanguageCode = value as String)
                AppPreferences.CURRENTLY_SELECTED_LINK_VIEW -> currentPref.copy(selectedLinkLayout = value as String)
                AppPreferences.TITLE_VISIBILITY_FOR_NON_LIST_VIEWS -> currentPref.copy(
                    showTitleInLinkGridView = value as Boolean
                )

                AppPreferences.BASE_URL_VISIBILITY_FOR_NON_LIST_VIEWS -> currentPref.copy(
                    showHostInLinkListView = value as Boolean
                )

                AppPreferences.FADED_EDGE_VISIBILITY_FOR_NON_LIST_VIEWS -> currentPref.copy(
                    enableFadedEdgeForNonListViews = value as Boolean
                )

                AppPreferences.FORCE_SAVE_WITHOUT_FETCHING_META_DATA -> currentPref.copy(
                    forceSaveWithoutFetchingAnyMetaData = value as Boolean
                )

                AppPreferences.SKIP_SAVING_EXISTING_LINK -> currentPref.copy(skipSavingExistingLink = value as Boolean)
                AppPreferences.USE_PROXY -> currentPref.copy(useProxy = value as Boolean)
                AppPreferences.PROXY_URL -> currentPref.copy(proxyUrl = value as String)
                AppPreferences.INITIAL_ROUTE -> currentPref.copy(startDestination = value as String)
                AppPreferences.SERVER_URL -> currentPref.copy(serverBaseUrl = value as String)
                AppPreferences.SERVER_AUTH_TOKEN -> currentPref.copy(serverSecurityToken = value as String)
                AppPreferences.SERVER_SYNC_TYPE -> currentPref.copy(
                    serverSyncType = SyncType.valueOf(
                        value as String
                    )
                )

                AppPreferences.DESKTOP_TOP_DECORATOR -> currentPref.copy(
                    useLinkoraTopDecoratorOnDesktop = value as Boolean
                )

                AppPreferences.CURRENT_WORK_MANAGER_WORK_UUID -> currentPref.copy(
                    refreshLinksWorkerTag = value as String
                )

                AppPreferences.SHOW_VIDEO_TAG_IF_APPLICABLE -> currentPref.copy(
                    showVideoTagOnUIIfApplicable = value as Boolean
                )

                AppPreferences.FORCE_SHUFFLE_LINKS -> currentPref.copy(forceShuffleLinks = value as Boolean)
                AppPreferences.NOTE_VISIBILITY_IN_LIST_VIEWS -> currentPref.copy(showNoteInLinkView = value as Boolean)
                AppPreferences.SHOW_DATE_IN_LINK_VIEW -> currentPref.copy(showDateInLinkView = value as Boolean)
                AppPreferences.SHOW_TAGS_IN_LINK_VIEW -> currentPref.copy(showTagsInLinkView = value as Boolean)
                AppPreferences.USE_SNAPSHOTS -> currentPref.copy(areSnapshotsEnabled = value as Boolean)
                AppPreferences.SNAPSHOTS_EXPORT_TYPE -> currentPref.copy(snapshotExportFormatID = value as String)
                AppPreferences.SKIP_CERT_CHECK_FOR_SYNC_SERVER -> currentPref.copy(
                    skipCertCheckForSync = value as Boolean
                )

                AppPreferences.EXPORT_LOCATION -> currentPref.copy(currentExportLocation = value as String)
                AppPreferences.BACKUP_LOCATION -> currentPref.copy(currentBackupLocation = value as String)
                AppPreferences.BACKUP_AUTO_DELETION_THRESHOLD -> currentPref.copy(
                    backupAutoDeleteThreshold = value as Int
                )

                AppPreferences.BACKUP_AUTO_DELETION_ENABLED -> currentPref.copy(
                    backupAutoDeletionEnabled = value as Boolean
                )

                AppPreferences.COLLECTION_SOURCE_ID -> currentPref.copy(selectedCollectionSourceId = value as Int)
                AppPreferences.SELECTED_APP_ICON -> currentPref.copy(selectedAppIcon = value as String)
                AppPreferences.SHOW_TAGS_BY_DEFAULT_IN_ADD_LINK -> currentPref.copy(
                    showTagsInAddNewLinkDialogBox = value as Boolean
                )

                AppPreferences.SHOW_MENU_ON_GRID_LINK_CLICK -> currentPref.copy(
                    showMenuOnGridLinkClick = value as Boolean
                )

                AppPreferences.AUTO_SAVE_ON_SHARE_INTENT -> currentPref.copy(autoSaveOnShareIntent = value as Boolean)
                AppPreferences.FORCE_SAVE_LINKS -> currentPref.copy(forceSaveIfRetrievalFails = value as Boolean)
                AppPreferences.FONT_TYPE -> currentPref.copy(selectedFont = Font.valueOf(value as String))
                AppPreferences.REFRESH_LINK_TYPE -> currentPref.copy(
                    selectedLinkRefreshType = RefreshLinkType.valueOf(
                        value as String
                    )
                )

                AppPreferences.MAX_CONCURRENT_REFRESH_COUNT -> currentPref.copy(
                    maxConcurrentRefreshCount = value as Int
                )

                else -> currentPref
            }
        }
    }
}