package com.sakethh.linkora.ui.screens.settings.section

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sakethh.linkora.Localization
import com.sakethh.linkora.di.linkoraViewModel
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.model.settings.SettingComponentParam
import com.sakethh.linkora.platform.platform
import com.sakethh.linkora.platform.showDynamicThemingOption
import com.sakethh.linkora.platform.showFollowSystemThemeOption
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
    val preferences by settingsScreenViewModel.preferencesAsFlow.collectAsStateWithLifecycle()
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
            if (platform is Platform.Android && showFollowSystemThemeOption && !preferences.useDarkTheme) {
                item(key = Localization.Key.FollowSystemTheme.defaultValue) {
                    SettingComponent(
                        SettingComponentParam(
                            title = Localization.Key.FollowSystemTheme.rememberLocalizedString(),
                            doesDescriptionExists = false,
                            isSwitchNeeded = true,
                            description = null,
                            isSwitchEnabled = preferences.useSystemTheme,
                            onSwitchStateChange = {
                                settingsScreenViewModel.changeSettingPreferenceValue(
                                    booleanPreferencesKey(AppPreferences.FOLLOW_SYSTEM_THEME.key),
                                    it
                                )
                            },
                            isIconNeeded = false
                        )
                    )
                }
            }
            if (!preferences.useSystemTheme || platform is Platform.Desktop || platform is Platform.Web) {
                item(key = Localization.Key.UseDarkMode.defaultValue) {
                    SettingComponent(
                        SettingComponentParam(
                            title = Localization.Key.UseDarkMode.rememberLocalizedString(),
                            doesDescriptionExists = false,
                            description = null,
                            isSwitchNeeded = true,
                            isSwitchEnabled = preferences.useDarkTheme,
                            onSwitchStateChange = {
                                settingsScreenViewModel.changeSettingPreferenceValue(
                                    booleanPreferencesKey(AppPreferences.DARK_THEME.key), it
                                )
                            },
                            isIconNeeded = false
                        )
                    )
                }
            }
            if (platform is Platform.Android && (preferences.useDarkTheme || (isSystemInDarkTheme && preferences.useSystemTheme))) {
                item {
                    SettingComponent(
                        SettingComponentParam(
                            title = Localization.Key.UseAmoledTheme.rememberLocalizedString(),
                            doesDescriptionExists = false,
                            description = "",
                            isSwitchNeeded = true,
                            isSwitchEnabled = preferences.useAmoledTheme,
                            onSwitchStateChange = {
                                settingsScreenViewModel.changeSettingPreferenceValue(
                                    booleanPreferencesKey(AppPreferences.AMOLED_THEME_STATE.key),
                                    it
                                )
                            },
                            isIconNeeded = false
                        )
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
                            isSwitchEnabled = preferences.useDynamicTheming,
                            onSwitchStateChange = {
                                settingsScreenViewModel.changeSettingPreferenceValue(
                                    booleanPreferencesKey(AppPreferences.DYNAMIC_THEMING.key),
                                    it
                                )
                            },
                            isIconNeeded = false
                        )
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}