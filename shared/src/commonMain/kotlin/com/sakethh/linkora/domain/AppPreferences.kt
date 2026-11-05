package com.sakethh.linkora.domain

import androidx.compose.runtime.Immutable
import com.sakethh.linkora.domain.dto.server.Correlation
import com.sakethh.linkora.platform.hostOS
import com.sakethh.linkora.ui.domain.AppIconCode
import com.sakethh.linkora.ui.domain.Font
import com.sakethh.linkora.ui.domain.Layout
import com.sakethh.linkora.ui.domain.SortingType
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.booleanPreference
import com.sakethh.linkora.utils.intPreference
import com.sakethh.linkora.utils.longPreference
import com.sakethh.linkora.utils.stringPreference
import kotlinx.serialization.json.Json

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
    val alwaysShowOpenWebCaptureFolderInMenu: Boolean = true,
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
    val useProxy: Boolean = hostOS == HostOS.Web,
    val proxyUrl: String = Constants.PROXY_SERVER_URL,
    val initialRouteId: Int = Constants.HOME_SCREEN_ROUTE_ID,
    val serverBaseUrl: String = "",
    val serverSecurityToken: String = "",
    val serverSyncType: SyncType = SyncType.TwoWay,
    val useLinkoraTopDecoratorOnDesktop: Boolean = true,
    val refreshLinksWorkerTag: String = "52ae3f4a-d37f-4fdb-a6b6-4397b99ef1bd",
    val allLinksWebCaptureWorkerTag: String = "4397b99ef1bd-d37f-52ae3f4a-a6b6-4fdb",
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
    val snapshotExportFormatID: String = Constants.SNAPSHOT_JSON_FORMAT_ID.toString(),
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
    val maxConcurrentRefreshCount: Int = 15,
    val showSyncServerSurveyNotice: Boolean = false,
    val useWebCaptures: Boolean = false,
    val webCapturesLocation: String = "",
    val webCaptureSaveImages: Boolean = true,
    val webCaptureSaveFonts: Boolean = true,
    val webCaptureSaveCss: Boolean = true,
    val webCaptureExecuteJs: Boolean = false,
    val webCaptureSaveAudio: Boolean = true,
    val webCaptureSaveVideo: Boolean = true,
    val webCaptureSaveMetadata: Boolean = true,
    val webCaptureWhitelistDomains: String = "",
    val webCaptureBlacklistDomains: String = "",
    val webCaptureSaveAsVersions: Boolean = false,
    val webCaptureRetainAllVersions: Boolean = false,
    val webCaptureMaxVersions: Int = 3,
    val webCaptureMaxConcurrency: Int = 15,
    val captureWhenRefreshAllLink: Boolean = false,
    val suggestLastSavedFolderWhenSavingLink: Boolean = false,
) {
    companion object {

        // added in v0.23.0
        val DID_AUTO_FORCE_STRICT_FOLDER_IDS_ON_DEFAULT_FOLDERS =
            booleanPreference(
                key = "DID_AUTO_FORCE_STRICT_FOLDER_IDS_ON_DEFAULT_FOLDERS",
                readFromCurrentState = {
                    Preference.requiresDiskRead(
                        "DID_AUTO_FORCE_STRICT_FOLDER_IDS_ON_DEFAULT_FOLDERS",
                    )
                },
                writeToCurrentState = { _, _ ->
                    Preference.requiresDiskWrite(
                        "DID_AUTO_FORCE_STRICT_FOLDER_IDS_ON_DEFAULT_FOLDERS",
                    )
                },
            )

        val LAST_SAVED_FOLDER_WHEN_SAVING_LINK_ID =
            longPreference(
                key = "LAST_SAVED_FOLDER_WHEN_SAVING_LINK_ID",
                readFromCurrentState = {
                    Preference.requiresDiskRead("LAST_SAVED_FOLDER_WHEN_SAVING_LINK_ID")
                },
                writeToCurrentState = { _, _ ->
                    Preference.requiresDiskWrite("LAST_SAVED_FOLDER_WHEN_SAVING_LINK_ID")
                },
            )

        val SUGGEST_LAST_SAVED_FOLDER_WHEN_SAVING_LINK =
            booleanPreference(
                key = "SUGGEST_LAST_SAVED_FOLDER_WHEN_SAVING_LINK",
                readFromCurrentState = AppPreferences::suggestLastSavedFolderWhenSavingLink,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(suggestLastSavedFolderWhenSavingLink = newBool)
                },
            )

        val DARK_THEME =
            booleanPreference(
                key = "DARK_THEME",
                readFromCurrentState = AppPreferences::useDarkTheme,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(useDarkTheme = newBool)
                },
            )

        val AMOLED_THEME_STATE =
            booleanPreference(
                key = "AMOLED_THEME_STATE",
                readFromCurrentState = AppPreferences::useAmoledTheme,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(useAmoledTheme = newBool)
                },
            )

        val DYNAMIC_THEMING =
            booleanPreference(
                key = "DYNAMIC_THEMING",
                readFromCurrentState = AppPreferences::useDynamicTheming,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(useDynamicTheming = newBool)
                },
            )

        val JSOUP_USER_AGENT =
            stringPreference(
                key = "JSOUP_USER_AGENT",
                readFromCurrentState = AppPreferences::primaryJsoupUserAgent,
                writeToCurrentState = { currentPreferences, newString ->
                    currentPreferences.copy(primaryJsoupUserAgent = newString)
                },
            )

        val FOLLOW_SYSTEM_THEME =
            booleanPreference(
                key = "FOLLOW_SYSTEM_THEME",
                readFromCurrentState = AppPreferences::useSystemTheme,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(useSystemTheme = newBool)
                },
            )

        val AUTO_DETECT_TITLE_FOR_LINK =
            booleanPreference(
                key = "AUTO_DETECT_TITLE_FOR_LINK",
                readFromCurrentState = AppPreferences::isAutoDetectTitleForLinksEnabled,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(isAutoDetectTitleForLinksEnabled = newBool)
                },
            )

        val HOME_SCREEN_VISIBILITY =
            booleanPreference(
                key = "HOME_SCREEN_VISIBILITY",
                readFromCurrentState = AppPreferences::isHomeScreenEnabled,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(isHomeScreenEnabled = newBool)
                },
            )

        val SORTING_PREFERENCE =
            stringPreference(
                key = "SORTING_PREFERENCE",
                readFromCurrentState = AppPreferences::selectedSortingType,
                writeToCurrentState = { currentPreferences, newString ->
                    currentPreferences.copy(selectedSortingType = newString)
                },
            )

        val REFRESH_ALL_LINKS_WORKER_UUID =
            stringPreference(
                // DO NOT CHANGE THE KEY NAME
                // bad key name, but changing this will break any ongoing refreshes
                // and would require a couple more checks if we rename this key or add a totally
                // different key for the same purpose
                key = "CURRENT_WORK_MANAGER_WORK_UUID",
                readFromCurrentState = AppPreferences::refreshLinksWorkerTag,
                writeToCurrentState = { currentPreferences, newString ->
                    currentPreferences.copy(refreshLinksWorkerTag = newString)
                },
            )

        val WEB_CAPTURE_ALL_LINKS_WORKER_UUID =
            stringPreference(
                key = "WEB_CAPTURE_ALL_LINKS_WORKER_UUID",
                readFromCurrentState = AppPreferences::allLinksWebCaptureWorkerTag,
                writeToCurrentState = { currentPreferences, newString ->
                    currentPreferences.copy(allLinksWebCaptureWorkerTag = newString)
                },
            )

        val APP_LANGUAGE_NAME =
            stringPreference(
                key = "APP_LANGUAGE_NAME",
                readFromCurrentState = AppPreferences::preferredAppLanguageName,
                writeToCurrentState = { currentPreferences, newString ->
                    currentPreferences.copy(preferredAppLanguageName = newString)
                },
            )

        val APP_LANGUAGE_CODE =
            stringPreference(
                key = "APP_LANGUAGE_CODE",
                readFromCurrentState = AppPreferences::preferredAppLanguageCode,
                writeToCurrentState = { currentPreferences, newString ->
                    currentPreferences.copy(preferredAppLanguageCode = newString)
                },
            )

        val USE_REMOTE_LANGUAGE_STRINGS =
            booleanPreference(
                key = "USE_REMOTE_LANGUAGE_STRINGS",
                readFromCurrentState = AppPreferences::useRemoteStrings,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(useRemoteStrings = newBool)
                },
            )

        val ALWAYS_SHOW_OPEN_WEB_CAPTURE_FOLDER_IN_MENU =
            booleanPreference(
                key = "ALWAYS_SHOW_OPEN_WEB_CAPTURE_FOLDER_IN_MENU",
                readFromCurrentState = AppPreferences::alwaysShowOpenWebCaptureFolderInMenu,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(alwaysShowOpenWebCaptureFolderInMenu = newBool)
                },
            )

        val LOCALIZATION_SERVER_URL =
            stringPreference(
                key = "LOCALIZATION_SERVER_URL",
                readFromCurrentState = AppPreferences::localizationServerURL,
                writeToCurrentState = { currentPreferences, newString ->
                    currentPreferences.copy(localizationServerURL = newString)
                },
            )

        val ASSOCIATED_IMAGES_IN_LINK_MENU_VISIBILITY =
            booleanPreference(
                key = "ASSOCIATED_IMAGES_IN_LINK_MENU_VISIBILITY",
                readFromCurrentState = AppPreferences::showAssociatedImageInLinkMenu,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(showAssociatedImageInLinkMenu = newBool)
                },
            )

        val CURRENTLY_SELECTED_LINK_VIEW =
            stringPreference(
                key = "CURRENTLY_SELECTED_LINK_VIEW",
                readFromCurrentState = AppPreferences::selectedLinkLayout,
                writeToCurrentState = { currentPreferences, newString ->
                    currentPreferences.copy(selectedLinkLayout = newString)
                },
            )

        val TITLE_VISIBILITY_FOR_NON_LIST_VIEWS =
            booleanPreference(
                key = "TITLE_VISIBILITY_FOR_NON_LIST_VIEWS",
                readFromCurrentState = AppPreferences::showTitleInLinkGridView,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(showTitleInLinkGridView = newBool)
                },
            )

        val BASE_URL_VISIBILITY_FOR_NON_LIST_VIEWS =
            booleanPreference(
                key = "BASE_URL_VISIBILITY_FOR_NON_LIST_VIEWS",
                readFromCurrentState = AppPreferences::showHostInLinkListView,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(showHostInLinkListView = newBool)
                },
            )

        val FADED_EDGE_VISIBILITY_FOR_NON_LIST_VIEWS =
            booleanPreference(
                key = "FADED_EDGE_VISIBILITY_FOR_NON_LIST_VIEWS",
                readFromCurrentState = AppPreferences::enableFadedEdgeForNonListViews,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(enableFadedEdgeForNonListViews = newBool)
                },
            )

        val FORCE_SAVE_WITHOUT_FETCHING_META_DATA =
            booleanPreference(
                key = "FORCE_SAVE_WITHOUT_FETCHING_META_DATA",
                readFromCurrentState = AppPreferences::forceSaveWithoutFetchingAnyMetaData,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(forceSaveWithoutFetchingAnyMetaData = newBool)
                },
            )

        val INITIAL_ROUTE_ID =
            intPreference(
                key = "INITIAL_ROUTE_ID",
                readFromCurrentState = AppPreferences::initialRouteId,
                writeToCurrentState = { currentPreferences, newInt ->
                    currentPreferences.copy(initialRouteId = newInt)
                },
            )

        val SERVER_URL =
            stringPreference(
                key = "SERVER_URL",
                readFromCurrentState = AppPreferences::serverBaseUrl,
                writeToCurrentState = { currentPreferences, newString ->
                    currentPreferences.copy(serverBaseUrl = newString)
                },
            )

        val SERVER_SYNC_TYPE =
            stringPreference(
                key = "SERVER_SYNC_TYPE",
                readFromCurrentState = { currentPreferences ->
                    currentPreferences.serverSyncType.name
                },
                writeToCurrentState = { currentPreferences, newString ->
                    currentPreferences.copy(serverSyncType = SyncType.valueOf(newString))
                },
            )

        val SERVER_AUTH_TOKEN =
            stringPreference(
                key = "SERVER_AUTH_TOKEN",
                readFromCurrentState = AppPreferences::serverSecurityToken,
                writeToCurrentState = { currentPreferences, newString ->
                    currentPreferences.copy(serverSecurityToken = newString)
                },
            )

        val DESKTOP_TOP_DECORATOR =
            booleanPreference(
                key = "DESKTOP_TOP_DECORATOR",
                readFromCurrentState = AppPreferences::useLinkoraTopDecoratorOnDesktop,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(useLinkoraTopDecoratorOnDesktop = newBool)
                },
            )

        val SERVER_CORRELATION =
            stringPreference(
                key = "SERVER_CORRELATION",
                readFromCurrentState = { currentPreferences ->
                    Json.encodeToString(currentPreferences.correlation)
                },
                writeToCurrentState = { currentPreferences, newString ->
                    currentPreferences.copy(correlation = Json.decodeFromString(newString))
                },
            )

        val SHOW_VIDEO_TAG_IF_APPLICABLE =
            booleanPreference(
                key = "SHOW_VIDEO_TAG_IF_APPLICABLE",
                readFromCurrentState = AppPreferences::showVideoTagOnUIIfApplicable,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(showVideoTagOnUIIfApplicable = newBool)
                },
            )

        val FORCE_SHUFFLE_LINKS =
            booleanPreference(
                key = "FORCE_SHUFFLE_LINKS",
                readFromCurrentState = AppPreferences::forceShuffleLinks,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(forceShuffleLinks = newBool)
                },
            )

        val NOTE_VISIBILITY_IN_LIST_VIEWS =
            booleanPreference(
                key = "NOTE_VISIBILITY_IN_LIST_VIEWS",
                readFromCurrentState = AppPreferences::showNoteInLinkView,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(showNoteInLinkView = newBool)
                },
            )

        val USE_SNAPSHOTS =
            booleanPreference(
                key = "USE_SNAPSHOTS",
                readFromCurrentState = AppPreferences::areSnapshotsEnabled,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(areSnapshotsEnabled = newBool)
                },
            )

        val SNAPSHOTS_EXPORT_TYPE =
            stringPreference(
                key = "SNAPSHOTS_EXPORT_TYPE",
                readFromCurrentState = AppPreferences::snapshotExportFormatID,
                writeToCurrentState = { currentPreferences, newString ->
                    currentPreferences.copy(snapshotExportFormatID = newString)
                },
            )

        val SKIP_SAVING_EXISTING_LINK =
            booleanPreference(
                key = "SKIP_SAVING_EXISTING_LINK",
                readFromCurrentState = AppPreferences::skipSavingExistingLink,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(skipSavingExistingLink = newBool)
                },
            )

        val SKIP_CERT_CHECK_FOR_SYNC_SERVER =
            booleanPreference(
                key = "SKIP_CERT_CHECK_FOR_SYNC_SERVER",
                readFromCurrentState = AppPreferences::skipCertCheckForSync,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(skipCertCheckForSync = newBool)
                },
            )

        val EXPORT_LOCATION =
            stringPreference(
                key = "EXPORT_LOCATION",
                readFromCurrentState = AppPreferences::currentExportLocation,
                writeToCurrentState = { currentPreferences, newString ->
                    currentPreferences.copy(currentExportLocation = newString)
                },
            )

        val BACKUP_LOCATION =
            stringPreference(
                key = "BACKUP_LOCATION",
                readFromCurrentState = AppPreferences::currentBackupLocation,
                writeToCurrentState = { currentPreferences, newString ->
                    currentPreferences.copy(currentBackupLocation = newString)
                },
            )

        val BACKUP_AUTO_DELETION_ENABLED =
            booleanPreference(
                key = "BACKUP_AUTO_DELETION_ENABLED",
                readFromCurrentState = AppPreferences::backupAutoDeletionEnabled,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(backupAutoDeletionEnabled = newBool)
                },
            )

        val BACKUP_AUTO_DELETION_THRESHOLD =
            intPreference(
                key = "BACKUP_AUTO_DELETION_THRESHOLD",
                readFromCurrentState = AppPreferences::backupAutoDeleteThreshold,
                writeToCurrentState = { currentPreferences, newInt ->
                    currentPreferences.copy(backupAutoDeleteThreshold = newInt)
                },
            )

        val COLLECTION_SOURCE_ID =
            intPreference(
                key = "COLLECTION_SOURCE_ID",
                readFromCurrentState = AppPreferences::selectedCollectionSourceId,
                writeToCurrentState = { currentPreferences, newInt ->
                    currentPreferences.copy(selectedCollectionSourceId = newInt)
                },
            )

        val SELECTED_APP_ICON =
            stringPreference(
                key = "SELECTED_APP_ICON",
                readFromCurrentState = AppPreferences::selectedAppIcon,
                writeToCurrentState = { currentPreferences, newString ->
                    currentPreferences.copy(selectedAppIcon = newString)
                },
            )

        val SHOW_TAGS_BY_DEFAULT_IN_ADD_LINK =
            booleanPreference(
                key = "SHOW_TAGS_BY_DEFAULT_IN_ADD_LINK",
                readFromCurrentState = AppPreferences::showTagsInAddNewLinkDialogBox,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(showTagsInAddNewLinkDialogBox = newBool)
                },
            )

        val SHOW_MENU_ON_GRID_LINK_CLICK =
            booleanPreference(
                key = "SHOW_MENU_ON_GRID_LINK_CLICK",
                readFromCurrentState = AppPreferences::showMenuOnGridLinkClick,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(showMenuOnGridLinkClick = newBool)
                },
            )

        val AUTO_SAVE_ON_SHARE_INTENT =
            booleanPreference(
                key = "AUTO_SAVE_ON_SHARE_INTENT",
                readFromCurrentState = AppPreferences::autoSaveOnShareIntent,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(autoSaveOnShareIntent = newBool)
                },
            )

        val FONT_TYPE =
            stringPreference(
                key = "FONT_TYPE",
                readFromCurrentState = { currentPreferences ->
                    currentPreferences.selectedFont.name
                },
                writeToCurrentState = { currentPreferences, newString ->
                    currentPreferences.copy(selectedFont = Font.valueOf(newString))
                },
            )

        val FORCE_SAVE_LINKS =
            booleanPreference(
                key = "FORCE_SAVE_LINKS",
                readFromCurrentState = AppPreferences::forceSaveIfRetrievalFails,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(forceSaveIfRetrievalFails = newBool)
                },
            )

        val REFRESH_LINK_TYPE =
            stringPreference(
                key = "REFRESH_LINK_TYPE",
                readFromCurrentState = { currentPreferences ->
                    currentPreferences.selectedLinkRefreshType.name
                },
                writeToCurrentState = { currentPreferences, newString ->
                    currentPreferences.copy(
                        selectedLinkRefreshType = RefreshLinkType.valueOf(newString),
                    )
                },
            )

        val CAPTURE_WHEN_REFRESH_ALL_LINK =
            booleanPreference(
                key = "CAPTURE_WHEN_REFRESH_ALL_LINK",
                readFromCurrentState = AppPreferences::captureWhenRefreshAllLink,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(captureWhenRefreshAllLink = newBool)
                },
            )

        val MAX_CONCURRENT_REFRESH_COUNT =
            intPreference(
                key = "MAX_CONCURRENT_REFRESH_COUNT",
                readFromCurrentState = AppPreferences::maxConcurrentRefreshCount,
                writeToCurrentState = { currentPreferences, newInt ->
                    currentPreferences.copy(maxConcurrentRefreshCount = newInt)
                },
            )

        val SHOW_TAGS_IN_LINK_VIEW =
            booleanPreference(
                key = "SHOW_TAGS_IN_LINK_VIEW",
                readFromCurrentState = AppPreferences::showTagsInLinkView,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(showTagsInLinkView = newBool)
                },
            )

        val SHOW_DATE_IN_LINK_VIEW =
            booleanPreference(
                key = "SHOW_DATE_IN_LINK_VIEW",
                readFromCurrentState = AppPreferences::showDateInLinkView,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(showDateInLinkView = newBool)
                },
            )

        val PROXY_URL =
            stringPreference(
                key = "PROXY_URL",
                readFromCurrentState = AppPreferences::proxyUrl,
                writeToCurrentState = { currentPreferences, newString ->
                    currentPreferences.copy(proxyUrl = newString)
                },
            )

        val USE_PROXY =
            booleanPreference(
                key = "USE_PROXY",
                readFromCurrentState = AppPreferences::useProxy,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(useProxy = newBool)
                },
            )

        val SHOW_SYNC_SERVER_SURVEY_NOTICE =
            booleanPreference(
                key = "SHOW_SYNC_SERVER_SURVEY_NOTICE",
                readFromCurrentState = AppPreferences::showSyncServerSurveyNotice,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(showSyncServerSurveyNotice = newBool)
                },
            )

        val USE_WEB_CAPTURES =
            booleanPreference(
                key = "USE_WEB_CAPTURES",
                readFromCurrentState = AppPreferences::useWebCaptures,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(useWebCaptures = newBool)
                },
            )

        val WEB_CAPTURES_LOCATION =
            stringPreference(
                key = "WEB_CAPTURES_LOCATION",
                readFromCurrentState = AppPreferences::webCapturesLocation,
                writeToCurrentState = { currentPreferences, newString ->
                    currentPreferences.copy(webCapturesLocation = newString)
                },
            )

        val WEB_CAPTURE_SAVE_IMAGES =
            booleanPreference(
                key = "WEB_CAPTURE_SAVE_IMAGES",
                readFromCurrentState = AppPreferences::webCaptureSaveImages,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(webCaptureSaveImages = newBool)
                },
            )

        val WEB_CAPTURE_SAVE_FONTS =
            booleanPreference(
                key = "WEB_CAPTURE_SAVE_FONTS",
                readFromCurrentState = AppPreferences::webCaptureSaveFonts,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(webCaptureSaveFonts = newBool)
                },
            )

        val WEB_CAPTURE_SAVE_CSS =
            booleanPreference(
                key = "WEB_CAPTURE_SAVE_CSS",
                readFromCurrentState = AppPreferences::webCaptureSaveCss,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(webCaptureSaveCss = newBool)
                },
            )

        val WEB_CAPTURE_EXECUTE_JS =
            booleanPreference(
                key = "WEB_CAPTURE_EXECUTE_JS",
                readFromCurrentState = AppPreferences::webCaptureExecuteJs,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(webCaptureExecuteJs = newBool)
                },
            )

        val WEB_CAPTURE_SAVE_AUDIO =
            booleanPreference(
                key = "WEB_CAPTURE_SAVE_AUDIO",
                readFromCurrentState = AppPreferences::webCaptureSaveAudio,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(webCaptureSaveAudio = newBool)
                },
            )

        val WEB_CAPTURE_SAVE_VIDEO =
            booleanPreference(
                key = "WEB_CAPTURE_SAVE_VIDEO",
                readFromCurrentState = AppPreferences::webCaptureSaveVideo,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(webCaptureSaveVideo = newBool)
                },
            )

        val WEB_CAPTURE_SAVE_METADATA =
            booleanPreference(
                key = "WEB_CAPTURE_SAVE_METADATA",
                readFromCurrentState = AppPreferences::webCaptureSaveMetadata,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(webCaptureSaveMetadata = newBool)
                },
            )

        val WEB_CAPTURE_WHITELIST_DOMAINS =
            stringPreference(
                key = "WEB_CAPTURE_WHITELIST_DOMAINS",
                readFromCurrentState = AppPreferences::webCaptureWhitelistDomains,
                writeToCurrentState = { currentPreferences, newString ->
                    currentPreferences.copy(webCaptureWhitelistDomains = newString)
                },
            )

        val WEB_CAPTURE_BLACKLIST_DOMAINS =
            stringPreference(
                key = "WEB_CAPTURE_BLACKLIST_DOMAINS",
                readFromCurrentState = AppPreferences::webCaptureBlacklistDomains,
                writeToCurrentState = { currentPreferences, newString ->
                    currentPreferences.copy(webCaptureBlacklistDomains = newString)
                },
            )

        val WEB_CAPTURE_SAVE_AS_VERSIONS =
            booleanPreference(
                key = "WEB_CAPTURE_SAVE_AS_VERSIONS",
                readFromCurrentState = AppPreferences::webCaptureSaveAsVersions,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(webCaptureSaveAsVersions = newBool)
                },
            )

        val WEB_CAPTURE_RETAIN_ALL_VERSIONS =
            booleanPreference(
                key = "WEB_CAPTURE_RETAIN_ALL_VERSIONS",
                readFromCurrentState = AppPreferences::webCaptureRetainAllVersions,
                writeToCurrentState = { currentPreferences, newBool ->
                    currentPreferences.copy(webCaptureRetainAllVersions = newBool)
                },
            )

        val WEB_CAPTURE_MAX_VERSIONS =
            intPreference(
                key = "WEB_CAPTURE_MAX_VERSIONS",
                readFromCurrentState = AppPreferences::webCaptureMaxVersions,
                writeToCurrentState = { currentPreferences, newInt ->
                    currentPreferences.copy(webCaptureMaxVersions = newInt)
                },
            )

        val WEB_CAPTURE_MAX_CONCURRENCY =
            intPreference(
                key = "WEB_CAPTURE_MAX_CONCURRENCY",
                readFromCurrentState = AppPreferences::webCaptureMaxConcurrency,
                writeToCurrentState = { currentPreferences, newInt ->
                    currentPreferences.copy(webCaptureMaxConcurrency = newInt)
                },
            )

        val LAST_TIME_SYNCED_WITH_SERVER =
            longPreference(
                key = "LAST_TIME_SYNCED_WITH_SERVER",
                readFromCurrentState = {
                    Preference.requiresDiskRead("LAST_TIME_SYNCED_WITH_SERVER")
                },
                writeToCurrentState = { _, _ ->
                    Preference.requiresDiskWrite("LAST_TIME_SYNCED_WITH_SERVER")
                },
            )

        val REFRESHED_LINKS_COUNT =
            longPreference(
                key = "REFRESHED_LINKS_COUNT",
                readFromCurrentState = {
                    Preference.requiresDiskRead("REFRESHED_LINKS_COUNT")
                },
                writeToCurrentState = { _, _ ->
                    Preference.requiresDiskWrite("REFRESHED_LINKS_COUNT")
                },
            )

        val LAST_SELECTED_PANEL_ID =
            longPreference(
                key = "LAST_SELECTED_PANEL_ID",
                readFromCurrentState = {
                    Preference.requiresDiskRead("LAST_SELECTED_PANEL_ID")
                },
                writeToCurrentState = { _, _ ->
                    Preference.requiresDiskWrite("LAST_SELECTED_PANEL_ID")
                },
            )

        val SHOULD_SHOW_ONBOARDING =
            booleanPreference(
                key = "SHOULD_SHOW_ONBOARDING",
                readFromCurrentState = {
                    Preference.requiresDiskRead("SHOULD_SHOW_ONBOARDING")
                },
                writeToCurrentState = { _, _ ->
                    Preference.requiresDiskWrite("SHOULD_SHOW_ONBOARDING")
                },
            )
    }
}
