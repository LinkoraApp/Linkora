package com.sakethh.linkora.screens.settings

import android.os.Build
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.datastore.DataStore
import androidx.datastore.preferences.edit
import androidx.datastore.preferences.preferencesKey
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sakethh.linkora.screens.settings.appInfo.dto.AppInfoDTO
import com.sakethh.linkora.screens.settings.appInfo.dto.MutableStateAppInfoDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import org.jsoup.Jsoup

data class SettingsUIElement(
    val title: String,
    val doesDescriptionExists: Boolean,
    val description: String?,
    val isSwitchNeeded: Boolean,
    val isSwitchEnabled: MutableState<Boolean>,
    val onSwitchStateChange: () -> Unit,
)

class SettingsScreenVM : ViewModel() {

    val shouldDeleteDialogBoxAppear = mutableStateOf(false)

    companion object {
        const val currentAppVersion = "0.1.0"
        val latestAppInfoFromServer = MutableStateAppInfoDTO(
            mutableStateOf(""),
            mutableStateOf(""),
            mutableStateOf(""),
            mutableStateOf(""),
            mutableStateOf(""),
            mutableStateOf(""),
            mutableStateOf("")
        )
    }

    val themeSection = listOf(
        SettingsUIElement(title = "Follow System Theme",
            doesDescriptionExists = false,
            description = null,
            isSwitchNeeded = true,
            isSwitchEnabled = Settings.shouldFollowSystemTheme,
            onSwitchStateChange = {
                viewModelScope.launch {
                    Settings.changeSettingPreferenceValue(
                        preferenceKey = preferencesKey(
                            SettingsPreferences.FOLLOW_SYSTEM_THEME.name
                        ),
                        dataStore = Settings.dataStore,
                        newValue = !Settings.shouldFollowSystemTheme.value
                    )
                    Settings.shouldFollowSystemTheme.value = Settings.readSettingPreferenceValue(
                        preferenceKey = preferencesKey(SettingsPreferences.FOLLOW_SYSTEM_THEME.name),
                        dataStore = Settings.dataStore
                    ) == true
                }
            }),
        SettingsUIElement(title = "Use dynamic theming",
            doesDescriptionExists = true,
            description = "Change colour themes within the app based on your wallpaper.",
            isSwitchNeeded = true,
            isSwitchEnabled = Settings.shouldFollowDynamicTheming,
            onSwitchStateChange = {
                viewModelScope.launch {
                    Settings.changeSettingPreferenceValue(
                        preferenceKey = preferencesKey(
                            SettingsPreferences.DYNAMIC_THEMING.name
                        ),
                        dataStore = Settings.dataStore,
                        newValue = !Settings.shouldFollowDynamicTheming.value
                    )
                    Settings.shouldFollowDynamicTheming.value = Settings.readSettingPreferenceValue(
                        preferenceKey = preferencesKey(SettingsPreferences.DYNAMIC_THEMING.name),
                        dataStore = Settings.dataStore
                    ) == true
                }
            }),
        SettingsUIElement(title = "Use Dark Mode",
            doesDescriptionExists = false,
            description = null,
            isSwitchNeeded = true,
            isSwitchEnabled = Settings.shouldDarkThemeBeEnabled,
            onSwitchStateChange = {
                viewModelScope.launch {
                    Settings.changeSettingPreferenceValue(
                        preferenceKey = preferencesKey(
                            SettingsPreferences.DARK_THEME.name
                        ),
                        dataStore = Settings.dataStore,
                        newValue = !Settings.shouldDarkThemeBeEnabled.value
                    )
                    Settings.shouldDarkThemeBeEnabled.value = Settings.readSettingPreferenceValue(
                        preferenceKey = preferencesKey(SettingsPreferences.DARK_THEME.name),
                        dataStore = Settings.dataStore
                    ) == true
                }
            }),
    )

    val generalSection = listOf(
        SettingsUIElement(title = "Use in-app browser",
            doesDescriptionExists = false,
            description = "If this is enabled, links will be opened within the app; if this setting is not enabled, your default browser will open every time you click on a link when using this app.",
            isSwitchNeeded = true,
            isSwitchEnabled = Settings.isInAppWebTabEnabled,
            onSwitchStateChange = {
                viewModelScope.launch {
                    Settings.changeSettingPreferenceValue(
                        preferenceKey = preferencesKey(
                            SettingsPreferences.CUSTOM_TABS.name
                        ),
                        dataStore = Settings.dataStore,
                        newValue = !Settings.isInAppWebTabEnabled.value
                    )
                    Settings.isInAppWebTabEnabled.value = Settings.readSettingPreferenceValue(
                        preferenceKey = preferencesKey(SettingsPreferences.CUSTOM_TABS.name),
                        dataStore = Settings.dataStore
                    ) == true
                }
            }),
        SettingsUIElement(title = "Enable Home Screen",
            doesDescriptionExists = false,
            description = "If this is enabled, Home Screen option will be shown in Bottom Navigation Bar; if this setting is not enabled, Home screen option will NOT be shown.",
            isSwitchNeeded = true,
            isSwitchEnabled = Settings.isHomeScreenEnabled,
            onSwitchStateChange = {
                viewModelScope.launch {
                    Settings.changeSettingPreferenceValue(
                        preferenceKey = preferencesKey(
                            SettingsPreferences.HOME_SCREEN_VISIBILITY.name
                        ),
                        dataStore = Settings.dataStore,
                        newValue = !Settings.isHomeScreenEnabled.value
                    )
                    Settings.isHomeScreenEnabled.value = Settings.readSettingPreferenceValue(
                        preferenceKey = preferencesKey(SettingsPreferences.HOME_SCREEN_VISIBILITY.name),
                        dataStore = Settings.dataStore
                    ) == true
                }
            }),
        SettingsUIElement(title = "Use Bottom Sheet UI for saving links",
            doesDescriptionExists = false,
            description = "If this is enabled, Bottom sheet will pop-up while saving a link; if this setting is not enabled, a dialog box will be shown instead of bottom sheet.",
            isSwitchNeeded = true,
            isSwitchEnabled = Settings.isBtmSheetEnabledForSavingLinks,
            onSwitchStateChange = {
                viewModelScope.launch {
                    Settings.changeSettingPreferenceValue(
                        preferenceKey = preferencesKey(
                            SettingsPreferences.BTM_SHEET_FOR_SAVING_LINKS.name
                        ),
                        dataStore = Settings.dataStore,
                        newValue = !Settings.isBtmSheetEnabledForSavingLinks.value
                    )
                    Settings.isBtmSheetEnabledForSavingLinks.value =
                        Settings.readSettingPreferenceValue(
                            preferenceKey = preferencesKey(SettingsPreferences.BTM_SHEET_FOR_SAVING_LINKS.name),
                            dataStore = Settings.dataStore
                        ) == true
                }
            }),
        SettingsUIElement(title = "Auto-Detect Title",
            doesDescriptionExists = true,
            description = "Note: This may not detect every website.",
            isSwitchNeeded = true,
            isSwitchEnabled = Settings.isAutoDetectTitleForLinksEnabled,
            onSwitchStateChange = {
                viewModelScope.launch {
                    Settings.changeSettingPreferenceValue(
                        preferenceKey = preferencesKey(
                            SettingsPreferences.AUTO_DETECT_TITLE_FOR_LINK.name
                        ),
                        dataStore = Settings.dataStore,
                        newValue = !Settings.isAutoDetectTitleForLinksEnabled.value
                    )
                    Settings.isAutoDetectTitleForLinksEnabled.value =
                        Settings.readSettingPreferenceValue(
                            preferenceKey = preferencesKey(SettingsPreferences.AUTO_DETECT_TITLE_FOR_LINK.name),
                            dataStore = Settings.dataStore
                        ) == true
                }
            }), SettingsUIElement(title = "Delete entire data permanently",
            doesDescriptionExists = true,
            description = "Delete all links and folders permanently including archive(s).",
            isSwitchNeeded = false,
            isSwitchEnabled = Settings.shouldFollowDynamicTheming,
            onSwitchStateChange = {
                shouldDeleteDialogBoxAppear.value = true
            })
    )

    enum class SettingsPreferences {
        DYNAMIC_THEMING, DARK_THEME, FOLLOW_SYSTEM_THEME, CUSTOM_TABS,
        AUTO_DETECT_TITLE_FOR_LINK, BTM_SHEET_FOR_SAVING_LINKS, HOME_SCREEN_VISIBILITY, SORTING_PREFERENCE
    }

    enum class SortingPreferences {
        A_TO_Z, Z_TO_A, NEW_TO_OLD, OLD_TO_NEW
    }

    object Settings {

        lateinit var dataStore: DataStore<androidx.datastore.preferences.Preferences>

        val shouldFollowDynamicTheming = mutableStateOf(false)
        val shouldFollowSystemTheme = mutableStateOf(true)
        val shouldDarkThemeBeEnabled = mutableStateOf(false)
        val isInAppWebTabEnabled = mutableStateOf(true)
        val isAutoDetectTitleForLinksEnabled = mutableStateOf(false)
        val isBtmSheetEnabledForSavingLinks = mutableStateOf(true)
        val isHomeScreenEnabled = mutableStateOf(true)
        val selectedSortingType = mutableStateOf("")

        suspend fun readSettingPreferenceValue(
            preferenceKey: androidx.datastore.preferences.Preferences.Key<Boolean>,
            dataStore: DataStore<androidx.datastore.preferences.Preferences>,
        ): Boolean? {
            return dataStore.data.first()[preferenceKey]
        }

        private suspend fun readSortingPreferenceValue(
            preferenceKey: androidx.datastore.preferences.Preferences.Key<String>,
            dataStore: DataStore<androidx.datastore.preferences.Preferences>,
        ): String? {
            return dataStore.data.first()[preferenceKey]
        }

        suspend fun changeSettingPreferenceValue(
            preferenceKey: androidx.datastore.preferences.Preferences.Key<Boolean>,
            dataStore: DataStore<androidx.datastore.preferences.Preferences>, newValue: Boolean,
        ) {
            dataStore.edit {
                it[preferenceKey] = newValue
            }
        }

        suspend fun changeSortingPreferenceValue(
            preferenceKey: androidx.datastore.preferences.Preferences.Key<String>,
            dataStore: DataStore<androidx.datastore.preferences.Preferences>,
            newValue: SortingPreferences,
        ) {
            dataStore.edit {
                it[preferenceKey] = newValue.name
            }
        }

        suspend fun readAllPreferencesValues() {
            coroutineScope {
                kotlinx.coroutines.awaitAll(
                    async {
                        shouldFollowSystemTheme.value =
                            readSettingPreferenceValue(
                                preferenceKey = preferencesKey(SettingsPreferences.FOLLOW_SYSTEM_THEME.name),
                                dataStore = dataStore
                            ) ?: (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q)
                    },
                    async {
                        shouldDarkThemeBeEnabled.value =
                            readSettingPreferenceValue(
                                preferenceKey = preferencesKey(SettingsPreferences.DARK_THEME.name),
                                dataStore = dataStore
                            ) == true
                    },
                    async {
                        shouldFollowDynamicTheming.value =
                            readSettingPreferenceValue(
                                preferenceKey = preferencesKey(SettingsPreferences.DYNAMIC_THEMING.name),
                                dataStore = dataStore
                            ) ?: false
                    },
                    async {
                        isInAppWebTabEnabled.value =
                            readSettingPreferenceValue(
                                preferenceKey = preferencesKey(SettingsPreferences.CUSTOM_TABS.name),
                                dataStore = dataStore
                            ) ?: true
                    },
                    async {
                        isAutoDetectTitleForLinksEnabled.value =
                            readSettingPreferenceValue(
                                preferenceKey = preferencesKey(SettingsPreferences.AUTO_DETECT_TITLE_FOR_LINK.name),
                                dataStore = dataStore
                            ) ?: true
                    },
                    async {
                        isHomeScreenEnabled.value =
                            if (readSettingPreferenceValue(
                                    preferenceKey = preferencesKey(SettingsPreferences.HOME_SCREEN_VISIBILITY.name),
                                    dataStore = dataStore
                                ) == null
                            ) {
                                true
                            } else {
                                readSettingPreferenceValue(
                                    preferenceKey = preferencesKey(SettingsPreferences.HOME_SCREEN_VISIBILITY.name),
                                    dataStore = dataStore
                                ) == true
                            }
                    },
                    async {
                        isBtmSheetEnabledForSavingLinks.value =
                            readSettingPreferenceValue(
                                preferenceKey = preferencesKey(SettingsPreferences.BTM_SHEET_FOR_SAVING_LINKS.name),
                                dataStore = dataStore
                            ) ?: true
                    },
                    async {
                        selectedSortingType.value =
                            readSortingPreferenceValue(
                                preferenceKey = preferencesKey(SettingsPreferences.SORTING_PREFERENCE.name),
                                dataStore = dataStore
                            ) ?: SortingPreferences.NEW_TO_OLD.name
                    }
                )
            }
        }

        suspend fun latestAppVersionRetriever() {
            val rawData = withContext(Dispatchers.Default) {
                Jsoup.connect(appInfoURL).get().body().ownText()
            }
            val retrievedData = Json.decodeFromString<AppInfoDTO>(rawData)
            latestAppInfoFromServer.apply {
                this.latestVersion.value = retrievedData.latestVersion
                this.latestStableVersion.value = retrievedData.latestStableVersion
                this.latestStableVersionReleaseURL.value =
                    retrievedData.latestStableVersionReleaseURL
                this.latestVersionReleaseURL.value = retrievedData.latestVersionReleaseURL
                this.changeLogForLatestVersion.value = retrievedData.changeLogForLatestVersion
                this.changeLogForLatestStableVersion.value =
                    retrievedData.changeLogForLatestStableVersion
                this.httpStatusCodeFromServer.value = ""
            }
        }
    }
}