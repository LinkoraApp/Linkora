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
import com.sakethh.linkora.di.linkoraViewModel
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.model.settings.SettingComponentParam
import com.sakethh.linkora.platform.showDynamicThemingOption
import com.sakethh.linkora.platform.showFollowSystemThemeOption
import com.sakethh.linkora.ui.LocalPlatform
import com.sakethh.linkora.ui.LocalizedStrings
import com.sakethh.linkora.ui.screens.settings.SettingsScreenViewModel
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingComponent
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingsSectionScaffold
import com.sakethh.linkora.utils.addEdgeToEdgeScaffoldPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemeSettingsScreen() {
    val localizedStrings = LocalizedStrings.current
    val settingsScreenViewModel: SettingsScreenViewModel = linkoraViewModel()
    val preferences by settingsScreenViewModel.preferencesAsFlow.collectAsStateWithLifecycle()
    val platform = LocalPlatform.current
    val isSystemInDarkTheme = isSystemInDarkTheme()
    SettingsSectionScaffold(
        topAppBarText = localizedStrings.Theme,
    ) { paddingValues, topAppBarScrollBehaviour ->
        LazyColumn(
            modifier =
                Modifier.fillMaxSize()
                    .addEdgeToEdgeScaffoldPadding(paddingValues)
                    .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(30.dp),
        ) {
            if (
                platform is Platform.Android && showFollowSystemThemeOption && !preferences.useDarkTheme
            ) {
                item(key = LocalizationKey.FollowSystemTheme.name) {
                    SettingComponent(
                        SettingComponentParam(
                            title = localizedStrings.FollowSystemTheme,
                            doesDescriptionExists = false,
                            isSwitchNeeded = true,
                            description = null,
                            isSwitchEnabled = preferences.useSystemTheme,
                            onSwitchStateChange = {
                                settingsScreenViewModel.changeSettingPreferenceValue(
                                    AppPreferences.FOLLOW_SYSTEM_THEME,
                                    it,
                                )
                            },
                            isIconNeeded = false,
                        ),
                    )
                }
            }
            if (!preferences.useSystemTheme || platform is Platform.Desktop || platform is Platform.Web) {
                item(key = LocalizationKey.UseDarkMode.name) {
                    SettingComponent(
                        SettingComponentParam(
                            title = localizedStrings.UseDarkMode,
                            doesDescriptionExists = false,
                            description = null,
                            isSwitchNeeded = true,
                            isSwitchEnabled = preferences.useDarkTheme,
                            onSwitchStateChange = {
                                settingsScreenViewModel.changeSettingPreferenceValue(
                                    AppPreferences.DARK_THEME,
                                    it,
                                )
                            },
                            isIconNeeded = false,
                        ),
                    )
                }
            }
            if (
                platform is Platform.Android &&
                (preferences.useDarkTheme || (isSystemInDarkTheme && preferences.useSystemTheme))
            ) {
                item {
                    SettingComponent(
                        SettingComponentParam(
                            title = localizedStrings.UseAmoledTheme,
                            doesDescriptionExists = false,
                            description = "",
                            isSwitchNeeded = true,
                            isSwitchEnabled = preferences.useAmoledTheme,
                            onSwitchStateChange = {
                                settingsScreenViewModel.changeSettingPreferenceValue(
                                    AppPreferences.AMOLED_THEME_STATE,
                                    it,
                                )
                            },
                            isIconNeeded = false,
                        ),
                    )
                }
            }
            if (platform is Platform.Android && showDynamicThemingOption) {
                item(key = LocalizationKey.UseDynamicTheming.name) {
                    SettingComponent(
                        SettingComponentParam(
                            title = localizedStrings.UseDynamicTheming,
                            doesDescriptionExists = true,
                            description = localizedStrings.UseDynamicThemingDesc,
                            isSwitchNeeded = true,
                            isSwitchEnabled = preferences.useDynamicTheming,
                            onSwitchStateChange = {
                                settingsScreenViewModel.changeSettingPreferenceValue(
                                    AppPreferences.DYNAMIC_THEMING,
                                    it,
                                )
                            },
                            isIconNeeded = false,
                        ),
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
}
