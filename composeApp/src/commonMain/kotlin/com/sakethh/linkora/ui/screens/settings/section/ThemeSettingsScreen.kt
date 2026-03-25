package com.sakethh.linkora.ui.screens.settings.section

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.sakethh.linkora.Localization
import com.sakethh.linkora.di.linkoraViewModel
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.model.settings.SettingComponentParam
import com.sakethh.linkora.platform.platform
import com.sakethh.linkora.platform.showDynamicThemingOption
import com.sakethh.linkora.platform.showFollowSystemThemeOption
import com.sakethh.linkora.preferences.AppPreferenceType
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.screens.settings.SettingsScreenViewModel
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingComponent
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingsSectionScaffold
import com.sakethh.linkora.utils.addEdgeToEdgeScaffoldPadding
import com.sakethh.linkora.utils.booleanPreferencesKey
import com.sakethh.linkora.utils.rememberLocalizedString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen() {
    val settingsScreenViewModel: SettingsScreenViewModel = linkoraViewModel()
    val platform = platform
    val isSystemInDarkTheme = isSystemInDarkTheme()
    SettingsSectionScaffold(
        topAppBarText = Localization.Key.Theme.rememberLocalizedString(),
    ) { paddingValues, topAppBarScrollBehaviour ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().addEdgeToEdgeScaffoldPadding(paddingValues)
                .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            if (platform is Platform.Android && showFollowSystemThemeOption && !AppPreferences.useDarkTheme.value) {
                item(key = Localization.Key.FollowSystemTheme.defaultValue) {
                    SettingComponent(
                        SettingComponentParam(
                            title = Localization.Key.FollowSystemTheme.rememberLocalizedString(),
                            doesDescriptionExists = false,
                            isSwitchNeeded = true,
                            description = null,
                            isSwitchEnabled = AppPreferences.useSystemTheme,
                            onSwitchStateChange = {
                                AppPreferences.useSystemTheme.value =
                                    !AppPreferences.useSystemTheme.value
                                settingsScreenViewModel.changeSettingPreferenceValue(
                                    booleanPreferencesKey(AppPreferenceType.FOLLOW_SYSTEM_THEME.name),
                                    it
                                )
                            },
                            isIconNeeded = remember {
                                mutableStateOf(false)
                            })
                    )
                }
            }
            if (!AppPreferences.useSystemTheme.value || platform is Platform.Desktop || platform is Platform.Web) {
                item(key = Localization.Key.UseDarkMode.defaultValue) {
                    SettingComponent(
                        SettingComponentParam(
                            title = Localization.Key.UseDarkMode.rememberLocalizedString(),
                            doesDescriptionExists = false,
                            description = null,
                            isSwitchNeeded = true,
                            isSwitchEnabled = AppPreferences.useDarkTheme,
                            onSwitchStateChange = {
                                AppPreferences.useDarkTheme.value =
                                    !AppPreferences.useDarkTheme.value
                                settingsScreenViewModel.changeSettingPreferenceValue(
                                    booleanPreferencesKey(AppPreferenceType.DARK_THEME.name), it
                                )
                            },
                            isIconNeeded = remember {
                                mutableStateOf(false)
                            })
                    )
                }
            }
            if (platform is Platform.Android && (AppPreferences.useDarkTheme.value || (isSystemInDarkTheme && AppPreferences.useSystemTheme.value))) {
                item {
                    SettingComponent(
                        SettingComponentParam(
                            title = Localization.Key.UseAmoledTheme.rememberLocalizedString(),
                            doesDescriptionExists = false,
                            description = "",
                            isSwitchNeeded = true,
                            isSwitchEnabled = AppPreferences.useAmoledTheme,
                            onSwitchStateChange = {
                                AppPreferences.useAmoledTheme.value =
                                    !AppPreferences.useAmoledTheme.value
                                settingsScreenViewModel.changeSettingPreferenceValue(
                                    booleanPreferencesKey(AppPreferenceType.AMOLED_THEME_STATE.name),
                                    it
                                )
                            },
                            isIconNeeded = remember {
                                mutableStateOf(false)
                            })
                    )
                }
            }
            if (platform is Platform.Android && showDynamicThemingOption) {
                item(key = Localization.Key.UseDynamicTheming.defaultValue) {
                    SettingComponent(
                        SettingComponentParam(
                            title = Localization.Key.UseDynamicTheming.rememberLocalizedString(),
                            doesDescriptionExists = true,
                            description = Localization.Key.UseDynamicThemingDesc.rememberLocalizedString(),
                            isSwitchNeeded = true,
                            isSwitchEnabled = AppPreferences.useDynamicTheming,
                            onSwitchStateChange = {
                                AppPreferences.useDynamicTheming.value =
                                    !AppPreferences.useDynamicTheming.value
                                settingsScreenViewModel.changeSettingPreferenceValue(
                                    booleanPreferencesKey(AppPreferenceType.DYNAMIC_THEMING.name),
                                    it
                                )
                            },
                            isIconNeeded = remember {
                                mutableStateOf(false)
                            })
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}