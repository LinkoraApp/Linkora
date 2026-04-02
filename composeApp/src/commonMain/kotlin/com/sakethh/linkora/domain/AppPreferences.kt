package com.sakethh.linkora.domain

import androidx.compose.runtime.Immutable
import com.sakethh.linkora.domain.dto.server.Correlation
import com.sakethh.linkora.platform.platform
import com.sakethh.linkora.ui.domain.AppIconCode
import com.sakethh.linkora.ui.domain.Font
import com.sakethh.linkora.ui.domain.Layout
import com.sakethh.linkora.ui.domain.SortingType
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.booleanPreferencesKey
import com.sakethh.linkora.utils.intPreferencesKey
import com.sakethh.linkora.utils.longPreferencesKey
import com.sakethh.linkora.utils.stringPreferencesKey

@Immutable
data class AppPreferences(
    val correlation: Correlation = Correlation.generateRandomCorrelation(),
    val useDarkTheme: Boolean = true,
    val useSystemTheme: Boolean = false,
    val useAmoledTheme: Boolean = false,
    val useDynamicTheming: Boolean = false,
    val isAutoDetectTitleForLinksEnabled: Boolean = false,
    val showAssociatedImageInLinkMenu: Boolean = false,
    val isHomeScreenEnabled: Boolean = true,
    val useRemoteStrings: Boolean = false,
    val selectedSortingType: String = SortingType.NEW_TO_OLD.name,
    val primaryJsoupUserAgent: String = Constants.DEFAULT_USER_AGENT,
    val localizationServerURL: String = Constants.LOCALIZATION_SERVER_URL,
    val preferredAppLanguageName: String = "English",
    val preferredAppLanguageCode: String = "en",
    val selectedLinkLayout: String = Layout.REGULAR_LIST_VIEW.name,
    val showTitleInLinkGridView: Boolean = true,
    val showHostInLinkListView: Boolean = true,
    val enableFadedEdgeForNonListViews: Boolean = true,
    val forceSaveWithoutFetchingAnyMetaData: Boolean = false,
    val skipSavingExistingLink: Boolean = true,
    val useProxy: Boolean = platform == Platform.Web,
    val proxyUrl: String = Constants.PROXY_SERVER_URL,
    val startDestination: String = Navigation.Root.HomeScreen.toString(),
    val serverBaseUrl: String = "",
    val serverSecurityToken: String = "",
    val serverSyncType: SyncType = SyncType.TwoWay,
    val useLinkoraTopDecoratorOnDesktop: Boolean = true,
    val refreshLinksWorkerTag: String = "52ae3f4a-d37f-4fdb-a6b6-4397b99ef1bd",
    val showVideoTagOnUIIfApplicable: Boolean = false,
    val forceShuffleLinks: Boolean = false,
    val showNoteInLinkView: Boolean = true,
    val showDateInLinkView: Boolean = true,
    val showTagsInLinkView: Boolean = true,
    val areSnapshotsEnabled: Boolean = false,
    // THIS MUST BE STRING, DO NOT CHANGE TO INT/LONG
    // OLDER LINKORA VERSIONS STORED DATA AS STRINGS (JSON/HTML/BOTH instead of 0/1/1)
    // CHANGING THIS TO INT/LONG WILL BREAK READING OF EXISTING PREFERENCES
    // SAVED BY PREVIOUS VERSIONS
    val snapshotExportFormatID: String = SnapshotFormat.JSON.id.toString(),
    val skipCertCheckForSync: Boolean = false,
    val currentExportLocation: String = "",
    val currentBackupLocation: String = "",
    val backupAutoDeleteThreshold: Int = 10,
    val backupAutoDeletionEnabled: Boolean = false,
    val selectedCollectionSourceId: Int = 0,
    val selectedAppIcon: String = AppIconCode.new_logo.name,
    val showTagsInAddNewLinkDialogBox: Boolean = false,
    val showMenuOnGridLinkClick: Boolean = true,
    val autoSaveOnShareIntent: Boolean = false,
    val forceSaveIfRetrievalFails: Boolean = true,
    val selectedFont: Font = Font.POPPINS,
    val selectedLinkRefreshType: RefreshLinkType = RefreshLinkType.Both,
    val maxConcurrentRefreshCount: Int = 15
) {
    companion object {
        val DARK_THEME = booleanPreferencesKey("DARK_THEME")
        val AMOLED_THEME_STATE = booleanPreferencesKey("AMOLED_THEME_STATE")
        val DYNAMIC_THEMING = booleanPreferencesKey("DYNAMIC_THEMING")
        val JSOUP_USER_AGENT = stringPreferencesKey("JSOUP_USER_AGENT")
        val SECONDARY_JSOUP_USER_AGENT = stringPreferencesKey("SECONDARY_JSOUP_USER_AGENT")
        val FOLLOW_SYSTEM_THEME = booleanPreferencesKey("FOLLOW_SYSTEM_THEME")
        val SETTING_COMPONENT_DESCRIPTION_STATE =
            booleanPreferencesKey("SETTING_COMPONENT_DESCRIPTION_STATE")
        val CUSTOM_TABS = booleanPreferencesKey("CUSTOM_TABS")
        val AUTO_DETECT_TITLE_FOR_LINK = booleanPreferencesKey("AUTO_DETECT_TITLE_FOR_LINK")
        val AUTO_CHECK_UPDATES = booleanPreferencesKey("AUTO_CHECK_UPDATES")
        val HOME_SCREEN_VISIBILITY = booleanPreferencesKey("HOME_SCREEN_VISIBILITY")
        val SORTING_PREFERENCE = stringPreferencesKey("SORTING_PREFERENCE")
        val SEND_CRASH_REPORTS = booleanPreferencesKey("SEND_CRASH_REPORTS")
        val IS_DATA_MIGRATION_COMPLETED_FROM_V9 =
            booleanPreferencesKey("IS_DATA_MIGRATION_COMPLETED_FROM_V9")
        val CURRENT_WORK_MANAGER_WORK_UUID = stringPreferencesKey("CURRENT_WORK_MANAGER_WORK_UUID")
        val SHELF_VISIBLE_STATE = booleanPreferencesKey("SHELF_VISIBLE_STATE")
        val LAST_SELECTED_PANEL_ID = intPreferencesKey("LAST_SELECTED_PANEL_ID")
        val APP_LANGUAGE_NAME = stringPreferencesKey("APP_LANGUAGE_NAME")
        val APP_LANGUAGE_CODE = stringPreferencesKey("APP_LANGUAGE_CODE")
        val USE_REMOTE_LANGUAGE_STRINGS = booleanPreferencesKey("USE_REMOTE_LANGUAGE_STRINGS")
        val TOTAL_REMOTE_STRINGS = intPreferencesKey("TOTAL_REMOTE_STRINGS")
        val REMOTE_STRINGS_LAST_UPDATED_ON = longPreferencesKey("REMOTE_STRINGS_LAST_UPDATED_ON")
        val LOCALIZATION_SERVER_URL = stringPreferencesKey("LOCALIZATION_SERVER_URL")
        val ASSOCIATED_IMAGES_IN_LINK_MENU_VISIBILITY =
            booleanPreferencesKey("ASSOCIATED_IMAGES_IN_LINK_MENU_VISIBILITY")
        val CURRENTLY_SELECTED_LINK_VIEW = stringPreferencesKey("CURRENTLY_SELECTED_LINK_VIEW")
        val BORDER_VISIBILITY_FOR_NON_LIST_VIEWS =
            booleanPreferencesKey("BORDER_VISIBILITY_FOR_NON_LIST_VIEWS")
        val TITLE_VISIBILITY_FOR_NON_LIST_VIEWS =
            booleanPreferencesKey("TITLE_VISIBILITY_FOR_NON_LIST_VIEWS")
        val BASE_URL_VISIBILITY_FOR_NON_LIST_VIEWS =
            booleanPreferencesKey("BASE_URL_VISIBILITY_FOR_NON_LIST_VIEWS")
        val FADED_EDGE_VISIBILITY_FOR_NON_LIST_VIEWS =
            booleanPreferencesKey("FADED_EDGE_VISIBILITY_FOR_NON_LIST_VIEWS")
        val FORCE_SAVE_WITHOUT_FETCHING_META_DATA =
            booleanPreferencesKey("FORCE_SAVE_WITHOUT_FETCHING_META_DATA")
        val INITIAL_ROUTE = stringPreferencesKey("INITIAL_ROUTE")
        val SERVER_URL = stringPreferencesKey("SERVER_URL")
        val SERVER_SYNC_TYPE = stringPreferencesKey("SERVER_SYNC_TYPE")
        val SERVER_AUTH_TOKEN = stringPreferencesKey("SERVER_AUTH_TOKEN")
        val DESKTOP_TOP_DECORATOR = booleanPreferencesKey("DESKTOP_TOP_DECORATOR")
        val REFRESHED_LINKS_COUNT = intPreferencesKey("REFRESHED_LINKS_COUNT")
        val SERVER_CORRELATION = stringPreferencesKey("SERVER_CORRELATION")
        val LAST_TIME_SYNCED_WITH_SERVER = longPreferencesKey("LAST_TIME_SYNCED_WITH_SERVER")
        val SHOW_VIDEO_TAG_IF_APPLICABLE = booleanPreferencesKey("SHOW_VIDEO_TAG_IF_APPLICABLE")
        val SHOULD_SHOW_ONBOARDING = booleanPreferencesKey("SHOULD_SHOW_ONBOARDING")
        val FORCE_SHUFFLE_LINKS = booleanPreferencesKey("FORCE_SHUFFLE_LINKS")
        val NOTE_VISIBILITY_IN_LIST_VIEWS = booleanPreferencesKey("NOTE_VISIBILITY_IN_LIST_VIEWS")
        val USE_SNAPSHOTS = booleanPreferencesKey("USE_SNAPSHOTS")
        val SNAPSHOTS_EXPORT_TYPE = stringPreferencesKey("SNAPSHOTS_EXPORT_TYPE")
        val SKIP_SAVING_EXISTING_LINK = booleanPreferencesKey("SKIP_SAVING_EXISTING_LINK")
        val SKIP_CERT_CHECK_FOR_SYNC_SERVER =
            booleanPreferencesKey("SKIP_CERT_CHECK_FOR_SYNC_SERVER")
        val EXPORT_LOCATION = stringPreferencesKey("EXPORT_LOCATION")
        val BACKUP_LOCATION = stringPreferencesKey("BACKUP_LOCATION")
        val BACKUP_AUTO_DELETION_ENABLED = booleanPreferencesKey("BACKUP_AUTO_DELETION_ENABLED")
        val BACKUP_AUTO_DELETION_THRESHOLD = intPreferencesKey("BACKUP_AUTO_DELETION_THRESHOLD")
        val COLLECTION_SOURCE_ID = intPreferencesKey("COLLECTION_SOURCE_ID")
        val SELECTED_APP_ICON = stringPreferencesKey("SELECTED_APP_ICON")
        val CUSTOM_VERSION_APP_LABEL = stringPreferencesKey("CUSTOM_VERSION_APP_LABEL")
        val SHOW_TAGS_BY_DEFAULT_IN_ADD_LINK =
            booleanPreferencesKey("SHOW_TAGS_BY_DEFAULT_IN_ADD_LINK")
        val SHOW_MENU_ON_GRID_LINK_CLICK = booleanPreferencesKey("SHOW_MENU_ON_GRID_LINK_CLICK")
        val AUTO_SAVE_ON_SHARE_INTENT = booleanPreferencesKey("AUTO_SAVE_ON_SHARE_INTENT")
        val FONT_TYPE = stringPreferencesKey("FONT_TYPE")
        val FORCE_SAVE_LINKS = booleanPreferencesKey("FORCE_SAVE_LINKS")
        val REFRESH_LINK_TYPE = stringPreferencesKey("REFRESH_LINK_TYPE")
        val MAX_CONCURRENT_REFRESH_COUNT = intPreferencesKey("MAX_CONCURRENT_REFRESH_COUNT")
        val SHOW_TAGS_IN_LINK_VIEW = booleanPreferencesKey("SHOW_TAGS_IN_LINK_VIEW")
        val SHOW_DATE_IN_LINK_VIEW = booleanPreferencesKey("SHOW_DATE_IN_LINK_VIEW")
        val PROXY_URL = stringPreferencesKey("PROXY_URL")
        val USE_PROXY = booleanPreferencesKey("USE_PROXY")
    }
}