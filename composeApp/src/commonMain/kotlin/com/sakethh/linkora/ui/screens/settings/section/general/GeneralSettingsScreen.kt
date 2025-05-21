package com.sakethh.linkora.ui.screens.settings.section.general

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Start
import androidx.compose.material.icons.filled.VideoLabel
import androidx.compose.material.icons.outlined.EditNotifications
import androidx.compose.material.icons.outlined.PresentToAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sakethh.linkora.common.DependencyContainer
import com.sakethh.linkora.common.Localization
import com.sakethh.linkora.common.preferences.AppPreferenceType
import com.sakethh.linkora.common.preferences.AppPreferences
import com.sakethh.linkora.common.utils.rememberLocalizedString
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.model.settings.SettingComponentParam
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.settings.SettingsScreenViewModel
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingComponent
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingsSectionScaffold
import com.sakethh.linkora.ui.utils.genericViewModelFactory
import com.sakethh.linkora.ui.utils.pulsateEffect
import com.sakethh.platform

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen() {
    val navController = LocalNavController.current
    val settingsScreenViewModel: SettingsScreenViewModel =
        viewModel(factory = genericViewModelFactory {
            SettingsScreenViewModel(DependencyContainer.preferencesRepo.value)
        })
    val showInitialNavigationChangerDialogBox = rememberSaveable {
        mutableStateOf(false)
    }
    val platform = platform()
    val generalSectionData = settingsScreenViewModel.generalSection(platform)
    val isLinkoraTopAppBarEnabled = rememberSaveable {
        mutableStateOf(AppPreferences.useLinkoraTopDecoratorOnDesktop.value)
    }
    SettingsSectionScaffold(
        topAppBarText = Navigation.Settings.GeneralSettingsScreen.toString(),
        navController = navController
    ) { paddingValues, topAppBarScrollBehaviour ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
                .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection)
                .navigationBarsPadding(), verticalArrangement = Arrangement.spacedBy(30.dp)
        ) {
            item {
                Spacer(Modifier)
            }
            if (platform == Platform.Desktop) {
                item {
                    SettingComponent(
                        SettingComponentParam(
                            title = Localization.Key.TopDecoratorSetting.rememberLocalizedString(),
                            doesDescriptionExists = true,
                            description = Localization.Key.TopDecoratorSettingDesc.rememberLocalizedString(),
                            isSwitchNeeded = true,
                            isSwitchEnabled = isLinkoraTopAppBarEnabled,
                            onSwitchStateChange = {
                                isLinkoraTopAppBarEnabled.value = it
                                settingsScreenViewModel.changeSettingPreferenceValue(
                                    preferenceKey = booleanPreferencesKey(
                                        AppPreferenceType.DESKTOP_TOP_DECORATOR.name
                                    ), newValue = it
                                )
                            },
                            isIconNeeded = mutableStateOf(true),
                            icon = Icons.Default.VideoLabel
                        )
                    )
                }
            }
            items(generalSectionData) { setting ->
                SettingComponent(setting)
            }

            item {
                SettingComponent(
                    SettingComponentParam(
                        title = Localization.Key.ChangeInitialRoute.rememberLocalizedString(),
                        doesDescriptionExists = true,
                        description = Localization.Key.ChangeInitialRouteDesc.rememberLocalizedString(),
                        isSwitchNeeded = false,
                        isSwitchEnabled = mutableStateOf(false),
                        onSwitchStateChange = {
                            showInitialNavigationChangerDialogBox.value = true
                        },
                        isIconNeeded = mutableStateOf(true),
                        icon = Icons.Default.Start
                    )
                )
            }

            item {
                SettingComponent(
                    SettingComponentParam(
                        title = "Manage Reminders",
                        doesDescriptionExists = false,
                        description = Localization.Key.ChangeInitialRouteDesc.rememberLocalizedString(),
                        isSwitchNeeded = false,
                        isSwitchEnabled = mutableStateOf(false),
                        onSwitchStateChange = {
                            navController.navigate(Navigation.Settings.General.RemindersSettingsScreen)
                        },
                        isIconNeeded = mutableStateOf(true),
                        icon = Icons.Outlined.EditNotifications
                    )
                )
            }
            item {
                SettingComponent(
                    SettingComponentParam(
                        title = Localization.getLocalizedString(Localization.Key.ShowOnboardingSlides),
                        doesDescriptionExists = false,
                        description = "",
                        isSwitchNeeded = false,
                        isSwitchEnabled = mutableStateOf(false),
                        onSwitchStateChange = {
                            navController.navigate(Navigation.Root.OnboardingSlidesScreen)
                        },
                        icon = Icons.Outlined.PresentToAll,
                        isIconNeeded = mutableStateOf(true),
                    )
                )
            }
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
    if (showInitialNavigationChangerDialogBox.value) {
        val currentlySelectedRoute = rememberSaveable {
            mutableStateOf(AppPreferences.startDestination.value)
        }
        LaunchedEffect(Unit) {
            settingsScreenViewModel.currInitialRoute {
                currentlySelectedRoute.value = it
            }
        }
        AlertDialog(onDismissRequest = {
            showInitialNavigationChangerDialogBox.value = false
        }, confirmButton = {
            Button(onClick = {
                settingsScreenViewModel.changeSettingPreferenceValue(
                    stringPreferencesKey(
                        AppPreferenceType.INITIAL_ROUTE.name
                    ), currentlySelectedRoute.value.toString()
                )
                showInitialNavigationChangerDialogBox.value = false
            }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = Localization.Key.Confirm.rememberLocalizedString(),
                    style = MaterialTheme.typography.titleMedium
                )
            }
        }, dismissButton = {
            OutlinedButton(onClick = {
                showInitialNavigationChangerDialogBox.value = false
            }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = Localization.Key.Cancel.rememberLocalizedString(),
                    style = MaterialTheme.typography.titleSmall
                )
            }
        }, text = {
            Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
                listOf(
                    Navigation.Root.HomeScreen,
                    Navigation.Root.SearchScreen,
                    Navigation.Root.CollectionsScreen
                ).forEach {
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = {
                            currentlySelectedRoute.value = it.toString()
                        }, indication = null, interactionSource = remember {
                            MutableInteractionSource()
                        }).pulsateEffect(), verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = currentlySelectedRoute.value == it.toString(), onClick = {
                                currentlySelectedRoute.value = it.toString()
                            })
                        Text(
                            style = if (currentlySelectedRoute.value == it.toString()) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
                            text = it.toString(),
                            color = if (currentlySelectedRoute.value == it.toString()) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }, title = {
            Text(
                text = Localization.Key.SelectTheInitialScreen.rememberLocalizedString(),
                style = MaterialTheme.typography.titleLarge,
                fontSize = 24.sp
            )
        })
    }
}