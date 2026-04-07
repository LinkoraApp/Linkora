import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import com.fleeksoft.io.ByteArrayInputStream
import com.sakethh.linkora.Localization
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.PreferenceKey
import com.sakethh.linkora.domain.RefreshLinkType
import com.sakethh.linkora.domain.SnapshotFormat
import com.sakethh.linkora.domain.SyncType
import com.sakethh.linkora.domain.dto.server.Correlation
import com.sakethh.linkora.platform.PlatformPreference
import com.sakethh.linkora.platform.defaultExportLocation
import com.sakethh.linkora.platform.defaultSnapshotLocation
import com.sakethh.linkora.platform.platform
import com.sakethh.linkora.ui.domain.AppIconCode
import com.sakethh.linkora.ui.domain.Font
import com.sakethh.linkora.ui.domain.Layout
import com.sakethh.linkora.ui.domain.SortingType
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.stringPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.serialization.json.Json
import java.security.cert.CertificateFactory
import java.security.cert.X509Certificate
import java.text.SimpleDateFormat
import java.util.Locale
import androidx.datastore.preferences.core.booleanPreferencesKey as dsBooleanKey
import androidx.datastore.preferences.core.intPreferencesKey as dsIntKey
import androidx.datastore.preferences.core.longPreferencesKey as dsLongKey
import androidx.datastore.preferences.core.stringPreferencesKey as dsStringKey

fun getCertificateInfo(factory: CertificateFactory, inputStream: ByteArrayInputStream): String {
    val certificate = factory.generateCertificate(inputStream) as X509Certificate
    val rawSubject = certificate.subjectX500Principal.name
    val issuedTo = rawSubject.split(",").firstOrNull { it.trim().startsWith("CN=") }
        ?.substringAfter("CN=") ?: rawSubject

    val rawIssuer = certificate.issuerX500Principal.name
    val issuedBy = rawIssuer.split(",").firstOrNull { it.trim().startsWith("CN=") }
        ?.substringAfter("CN=") ?: rawIssuer

    val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    val expiresOn = dateFormat.format(certificate.notAfter)
    return "Issued To: $issuedTo\nIssued By: $issuedBy\nExpires On: $expiresOn"
}

suspend fun readAllPreferences(
    prefs: Preferences, externalAction: suspend (suspend (PlatformPreference) -> Unit) -> Unit
): AppPreferences {
    return AppPreferences(
        correlation = prefs[dsStringKey(AppPreferences.SERVER_CORRELATION.key)].let {
            if (it != null) {
                Json.decodeFromString<Correlation>(it)
            } else {
                val randomCorrelation = Correlation.generateRandomCorrelation()
                externalAction { platformPreference ->
                    platformPreference.writePreferenceValue(
                        preferenceKey = stringPreferencesKey(AppPreferences.SERVER_CORRELATION.key),
                        newValue = Json.encodeToString(randomCorrelation)
                    )
                }
                randomCorrelation
            }
        },
        useDarkTheme = prefs[dsBooleanKey(AppPreferences.DARK_THEME.key)]
            ?: (platform == Platform.Desktop),
        useSystemTheme = prefs[dsBooleanKey(AppPreferences.FOLLOW_SYSTEM_THEME.key)]
            ?: (platform == Platform.Android),
        useAmoledTheme = prefs[dsBooleanKey(AppPreferences.AMOLED_THEME_STATE.key)] ?: false,
        useDynamicTheming = prefs[dsBooleanKey(AppPreferences.DYNAMIC_THEMING.key)] ?: false,
        isAutoDetectTitleForLinksEnabled = prefs[dsBooleanKey(AppPreferences.AUTO_DETECT_TITLE_FOR_LINK.key)]
            ?: false,
        showAssociatedImageInLinkMenu = prefs[dsBooleanKey(AppPreferences.ASSOCIATED_IMAGES_IN_LINK_MENU_VISIBILITY.key)]
            ?: true,
        isHomeScreenEnabled = prefs[dsBooleanKey(AppPreferences.HOME_SCREEN_VISIBILITY.key)]
            ?: true,
        useRemoteStrings = prefs[dsBooleanKey(AppPreferences.USE_REMOTE_LANGUAGE_STRINGS.key)]
            ?: false,
        selectedSortingType = prefs[dsStringKey(AppPreferences.SORTING_PREFERENCE.key)]
            ?: SortingType.NEW_TO_OLD.name,
        primaryJsoupUserAgent = prefs[dsStringKey(AppPreferences.JSOUP_USER_AGENT.key)]
            ?: Constants.DEFAULT_USER_AGENT,
        localizationServerURL = prefs[dsStringKey(AppPreferences.LOCALIZATION_SERVER_URL.key)]
            ?: Constants.LOCALIZATION_SERVER_URL,
        preferredAppLanguageName = prefs[dsStringKey(AppPreferences.APP_LANGUAGE_NAME.key)]
            ?: "English",
        preferredAppLanguageCode = prefs[dsStringKey(AppPreferences.APP_LANGUAGE_CODE.key)] ?: "en",
        selectedLinkLayout = prefs[dsStringKey(AppPreferences.CURRENTLY_SELECTED_LINK_VIEW.key)]
            ?: Layout.REGULAR_LIST_VIEW.name,
        showTitleInLinkGridView = prefs[dsBooleanKey(AppPreferences.TITLE_VISIBILITY_FOR_NON_LIST_VIEWS.key)]
            ?: true,
        showHostInLinkListView = prefs[dsBooleanKey(AppPreferences.BASE_URL_VISIBILITY_FOR_NON_LIST_VIEWS.key)]
            ?: true,
        enableFadedEdgeForNonListViews = prefs[dsBooleanKey(AppPreferences.FADED_EDGE_VISIBILITY_FOR_NON_LIST_VIEWS.key)]
            ?: true,
        forceSaveWithoutFetchingAnyMetaData = prefs[dsBooleanKey(AppPreferences.FORCE_SAVE_WITHOUT_FETCHING_META_DATA.key)]
            ?: false,
        skipSavingExistingLink = prefs[dsBooleanKey(AppPreferences.SKIP_SAVING_EXISTING_LINK.key)]
            ?: true,
        useProxy = prefs[dsBooleanKey(AppPreferences.USE_PROXY.key)] ?: false,
        proxyUrl = prefs[dsStringKey(AppPreferences.PROXY_URL.key)] ?: Constants.PROXY_SERVER_URL,
        startDestination = prefs[dsStringKey(AppPreferences.INITIAL_ROUTE.key)]
            ?: Navigation.Root.HomeScreen.toString(),
        serverBaseUrl = prefs[dsStringKey(AppPreferences.SERVER_URL.key)] ?: "",
        serverSecurityToken = prefs[dsStringKey(AppPreferences.SERVER_AUTH_TOKEN.key)] ?: "",
        serverSyncType = prefs[dsStringKey(AppPreferences.SERVER_SYNC_TYPE.key)]?.let {
            SyncType.valueOf(
                it
            )
        } ?: SyncType.TwoWay,
        useLinkoraTopDecoratorOnDesktop = prefs[dsBooleanKey(AppPreferences.DESKTOP_TOP_DECORATOR.key)]
            ?: true,
        refreshLinksWorkerTag = prefs[dsStringKey(AppPreferences.CURRENT_WORK_MANAGER_WORK_UUID.key)]
            ?: "52ae3f4a-d37f-4fdb-a6b6-4397b99ef1bd",
        showVideoTagOnUIIfApplicable = prefs[dsBooleanKey(AppPreferences.SHOW_VIDEO_TAG_IF_APPLICABLE.key)]
            ?: false,
        forceShuffleLinks = prefs[dsBooleanKey(AppPreferences.FORCE_SHUFFLE_LINKS.key)] ?: false,
        showNoteInLinkView = prefs[dsBooleanKey(AppPreferences.NOTE_VISIBILITY_IN_LIST_VIEWS.key)]
            ?: true,
        showDateInLinkView = prefs[dsBooleanKey(AppPreferences.SHOW_DATE_IN_LINK_VIEW.key)] ?: true,
        showTagsInLinkView = prefs[dsBooleanKey(AppPreferences.SHOW_TAGS_IN_LINK_VIEW.key)] ?: true,
        areSnapshotsEnabled = prefs[dsBooleanKey(AppPreferences.USE_SNAPSHOTS.key)] ?: false,
        snapshotExportFormatID = prefs[dsStringKey(AppPreferences.SNAPSHOTS_EXPORT_TYPE.key)]
            ?: SnapshotFormat.JSON.id.toString(),
        skipCertCheckForSync = prefs[dsBooleanKey(AppPreferences.SKIP_CERT_CHECK_FOR_SYNC_SERVER.key)]
            ?: false,
        currentExportLocation = (prefs[dsStringKey(AppPreferences.EXPORT_LOCATION.key)]
            ?: defaultExportLocation())
            ?: Localization.Key.ExportRequiresDirectory.getLocalizedString(),
        currentBackupLocation = (prefs[dsStringKey(AppPreferences.BACKUP_LOCATION.key)]
            ?: defaultSnapshotLocation())
            ?: Localization.Key.BackupsWorkOnlyWithDirectory.getLocalizedString(),
        backupAutoDeleteThreshold = prefs[dsIntKey(AppPreferences.BACKUP_AUTO_DELETION_THRESHOLD.key)]
            ?: 25,
        backupAutoDeletionEnabled = prefs[dsBooleanKey(AppPreferences.BACKUP_AUTO_DELETION_ENABLED.key)]
            ?: false,
        selectedCollectionSourceId = prefs[dsIntKey(AppPreferences.COLLECTION_SOURCE_ID.key)] ?: 0,
        selectedAppIcon = prefs[dsStringKey(AppPreferences.SELECTED_APP_ICON.key)]
            ?: AppIconCode.new_logo.name,
        showTagsInAddNewLinkDialogBox = prefs[dsBooleanKey(AppPreferences.SHOW_TAGS_BY_DEFAULT_IN_ADD_LINK.key)]
            ?: false,
        showMenuOnGridLinkClick = prefs[dsBooleanKey(AppPreferences.SHOW_MENU_ON_GRID_LINK_CLICK.key)]
            ?: true,
        autoSaveOnShareIntent = prefs[dsBooleanKey(AppPreferences.AUTO_SAVE_ON_SHARE_INTENT.key)]
            ?: false,
        forceSaveIfRetrievalFails = prefs[dsBooleanKey(AppPreferences.FORCE_SAVE_LINKS.key)]
            ?: true,
        selectedFont = prefs[dsStringKey(AppPreferences.FONT_TYPE.key)]?.let { Font.valueOf(it) }
            ?: Font.POPPINS,
        selectedLinkRefreshType = prefs[dsStringKey(AppPreferences.REFRESH_LINK_TYPE.key)]?.let {
            RefreshLinkType.valueOf(
                it
            )
        } ?: RefreshLinkType.Both,
        maxConcurrentRefreshCount = prefs[dsIntKey(AppPreferences.MAX_CONCURRENT_REFRESH_COUNT.key)]
            ?: 15,
        showSyncServerSurveyNotice = prefs[dsBooleanKey(AppPreferences.SHOW_SYNC_SERVER_SURVEY_NOTICE.key)]
            ?: true)
}

suspend fun <T> writePreferenceValue(
    dataStore: DataStore<Preferences>, preferenceKey: PreferenceKey<T>, newValue: T
) {
    dataStore.edit {
        when (preferenceKey) {
            is PreferenceKey.BooleanPreferencesKey -> {
                it[booleanPreferencesKey(preferenceKey.key)] = newValue as Boolean
            }

            is PreferenceKey.LongPreferencesKey -> {
                it[longPreferencesKey(preferenceKey.key)] = newValue as Long
            }

            is PreferenceKey.StringPreferencesKey -> {
                it[dsStringKey(preferenceKey.key)] = newValue as String
            }

            is PreferenceKey.IntPreferencesKey -> {
                it[intPreferencesKey(preferenceKey.key)] = newValue as Int
            }
        }
    }
}

suspend fun <T> readPreferenceValue(
    dataStore: DataStore<Preferences>, preferenceKey: PreferenceKey<T>
): T? {
    val preferences = dataStore.data.first()

    return when (preferenceKey) {
        is PreferenceKey.BooleanPreferencesKey -> preferences[dsBooleanKey(preferenceKey.key)]
        is PreferenceKey.LongPreferencesKey -> preferences[dsLongKey(preferenceKey.key)]
        is PreferenceKey.StringPreferencesKey -> preferences[dsStringKey(preferenceKey.key)]
        is PreferenceKey.IntPreferencesKey -> preferences[dsIntKey(preferenceKey.key)]
    } as T?
}