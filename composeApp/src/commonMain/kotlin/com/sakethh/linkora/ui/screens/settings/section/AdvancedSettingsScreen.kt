package com.sakethh.linkora.ui.screens.settings.section

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sakethh.linkora.Localization
import com.sakethh.linkora.di.linkoraViewModel
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.model.settings.SettingComponentParam
import com.sakethh.linkora.platform.platform
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.settings.SettingsScreenViewModel
import com.sakethh.linkora.ui.screens.settings.common.composables.PreferenceTextField
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingComponent
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingsSectionScaffold
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.addEdgeToEdgeScaffoldPadding
import com.sakethh.linkora.utils.booleanPreferencesKey
import com.sakethh.linkora.utils.rememberLocalizedString
import com.sakethh.linkora.utils.stringPreferencesKey

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdvancedSettingsScreen() {
    val settingsScreenVM: SettingsScreenViewModel = linkoraViewModel()
    val preferences by settingsScreenVM.preferencesAsFlow.collectAsStateWithLifecycle()
    val primaryJsoupStringAgent = rememberSaveable(preferences.primaryJsoupUserAgent) {
        mutableStateOf(preferences.primaryJsoupUserAgent)
    }
    val localizationServerURL = rememberSaveable(preferences.localizationServerURL) {
        mutableStateOf(preferences.localizationServerURL)
    }
    var proxyServerUrl by rememberSaveable(preferences.proxyUrl) {
        mutableStateOf(preferences.proxyUrl)
    }
    var isReadOnlyTextFieldForProxyServer by rememberSaveable {
        mutableStateOf(true)
    }
    val isReadOnlyTextFieldForPrimaryUserAgent = rememberSaveable {
        mutableStateOf(true)
    }
    val isReadOnlyTextFieldForLocalizationServer = rememberSaveable {
        mutableStateOf(true)
    }
    val useProxy by rememberSaveable(preferences.useProxy) {
        mutableStateOf(preferences.useProxy)
    }
    val proxyUrlFocusRequester = remember { FocusRequester() }
    val primaryJsoupUserAgentFocusRequester = remember { FocusRequester() }
    val localizationServerTextFieldFocusRequester = remember { FocusRequester() }
    SettingsSectionScaffold(
        topAppBarText = Navigation.Settings.AdvancedSettingsScreen.toString(),
    ) { paddingValues, topAppBarScrollBehaviour ->
        LazyColumn(
            modifier = Modifier.animateContentSize().fillMaxSize()
                .addEdgeToEdgeScaffoldPadding(paddingValues)
                .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            item {
                Spacer(Modifier)
            }

            if (platform != Platform.Web) {
                item {
                    SettingComponent(
                        settingComponentParam = SettingComponentParam(
                            title = Localization.Key.UseAProxyServer.rememberLocalizedString(),
                            doesDescriptionExists = true,
                            description = Localization.Key.UseAProxyServerDesc.rememberLocalizedString(),
                            isSwitchNeeded = true,
                            isSwitchEnabled = useProxy,
                            onSwitchStateChange = {
                                settingsScreenVM.changeSettingPreferenceValue(
                                    preferenceKey = booleanPreferencesKey(
                                        AppPreferences.USE_PROXY.key
                                    ), newValue = it
                                )
                            },
                            isIconNeeded = true,
                            icon = Icons.Default.Route
                        )
                    )
                }
            }

            if (useProxy || platform == Platform.Web) {
                item {
                    PreferenceTextField(
                        textFieldDescText = Localization.Key.UseAProxyServerLongDesc.rememberLocalizedString(),
                        textFieldLabel = Localization.Key.Proxy.rememberLocalizedString(),
                        textFieldValue = proxyServerUrl,
                        onResetButtonClick = {
                            settingsScreenVM.changeSettingPreferenceValue(
                                stringPreferencesKey(AppPreferences.PROXY_URL.key),
                                Constants.PROXY_SERVER_URL
                            )
                        },
                        onTextFieldValueChange = {
                            proxyServerUrl = it
                        },
                        onConfirmButtonClick = {
                            isReadOnlyTextFieldForProxyServer = !isReadOnlyTextFieldForProxyServer
                            if (!isReadOnlyTextFieldForProxyServer) {
                                proxyUrlFocusRequester.requestFocus()
                            } else {
                                proxyUrlFocusRequester.freeFocus()
                            }
                            if (isReadOnlyTextFieldForProxyServer) {
                                settingsScreenVM.changeSettingPreferenceValue(
                                    stringPreferencesKey(AppPreferences.PROXY_URL.key),
                                    proxyServerUrl
                                )
                            }
                        },
                        focusRequester = proxyUrlFocusRequester,
                        readonly = isReadOnlyTextFieldForProxyServer
                    )
                }
            }

            item {
                PreferenceTextField(
                    textFieldDescText = Localization.Key.UserAgentDesc.rememberLocalizedString(),
                    textFieldLabel = Localization.Key.UserAgent.rememberLocalizedString(),
                    textFieldValue = primaryJsoupStringAgent.value,
                    onResetButtonClick = {
                        settingsScreenVM.changeSettingPreferenceValue(
                            stringPreferencesKey(AppPreferences.JSOUP_USER_AGENT.key),
                            Constants.DEFAULT_USER_AGENT
                        )
                    },
                    onTextFieldValueChange = {
                        primaryJsoupStringAgent.value = it
                    },
                    onConfirmButtonClick = {
                        isReadOnlyTextFieldForPrimaryUserAgent.value =
                            !isReadOnlyTextFieldForPrimaryUserAgent.value
                        if (!isReadOnlyTextFieldForPrimaryUserAgent.value) {
                            primaryJsoupUserAgentFocusRequester.requestFocus()
                        } else {
                            primaryJsoupUserAgentFocusRequester.freeFocus()
                        }
                        if (isReadOnlyTextFieldForPrimaryUserAgent.value) {
                            settingsScreenVM.changeSettingPreferenceValue(
                                stringPreferencesKey(AppPreferences.JSOUP_USER_AGENT.key),
                                primaryJsoupStringAgent.value
                            )
                        }
                    },
                    focusRequester = primaryJsoupUserAgentFocusRequester,
                    readonly = isReadOnlyTextFieldForPrimaryUserAgent.value
                )
            }

            item {
                PreferenceTextField(
                    textFieldDescText = Localization.Key.LocalizationServerDesc.rememberLocalizedString(),
                    textFieldLabel = Localization.Key.LocalizationServer.rememberLocalizedString(),
                    textFieldValue = localizationServerURL.value,
                    onResetButtonClick = {
                        settingsScreenVM.changeSettingPreferenceValue(
                            stringPreferencesKey(AppPreferences.LOCALIZATION_SERVER_URL.key),
                            Constants.LOCALIZATION_SERVER_URL
                        )
                    },
                    onTextFieldValueChange = {
                        localizationServerURL.value = it
                    },
                    onConfirmButtonClick = {
                        isReadOnlyTextFieldForLocalizationServer.value =
                            !isReadOnlyTextFieldForLocalizationServer.value
                        if (!isReadOnlyTextFieldForLocalizationServer.value) {
                            localizationServerTextFieldFocusRequester.requestFocus()
                        } else {
                            localizationServerTextFieldFocusRequester.freeFocus()
                        }
                        if (isReadOnlyTextFieldForLocalizationServer.value) {
                            settingsScreenVM.changeSettingPreferenceValue(
                                stringPreferencesKey(AppPreferences.LOCALIZATION_SERVER_URL.key),
                                localizationServerURL.value
                            )
                        }
                    },
                    focusRequester = localizationServerTextFieldFocusRequester,
                    readonly = isReadOnlyTextFieldForLocalizationServer.value
                )
            }
            item {
                Spacer(Modifier.height(100.dp))
            }
        }
    }
}