package com.sakethh.linkora.ui.screens.settings.section

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Start
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material.icons.filled.VideoLabel
import androidx.compose.material.icons.outlined.PresentToAll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sakethh.linkora.Localization
import com.sakethh.linkora.di.linkoraViewModel
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.model.settings.SettingComponentParam
import com.sakethh.linkora.platform.platform
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.components.InfoCard
import com.sakethh.linkora.ui.domain.AppIconCode
import com.sakethh.linkora.ui.domain.Font
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.settings.SettingsScreenViewModel
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingComponent
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingsSectionScaffold
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.addEdgeToEdgeScaffoldPadding
import com.sakethh.linkora.utils.booleanPreferencesKey
import com.sakethh.linkora.utils.rememberLocalizedString
import com.sakethh.linkora.utils.stringPreferencesKey
import kotlinx.collections.immutable.PersistentList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toPersistentList
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GeneralSettingsScreen() {
    val navController = LocalNavController.current
    val settingsScreenViewModel: SettingsScreenViewModel = linkoraViewModel()
    val preferences by settingsScreenViewModel.preferencesAsFlow.collectAsStateWithLifecycle()
    var showInitialNavigationChangerDialogBox by rememberSaveable {
        mutableStateOf(false)
    }
    var showFontFamilySwitcherDialogBox by rememberSaveable {
        mutableStateOf(false)
    }
    val platform = platform
    val onAndroidMobile = Platform.Android.onMobile()
    val generalSectionData = retain(preferences) {
        settingsScreenViewModel.generalSection(
            onAndroidMobile,
            preferences
        )
    }
    val isLinkoraTopAppBarEnabled by rememberSaveable(preferences.useLinkoraTopDecoratorOnDesktop) {
        mutableStateOf(preferences.useLinkoraTopDecoratorOnDesktop)
    }
    var tempSelectedAppIcon by rememberSaveable {
        mutableStateOf(preferences.selectedAppIcon)
    }
    var showIconSwitchDialogBox by rememberSaveable {
        mutableStateOf(false)
    }
    val fontsEntries = retain {
        Font.entries.toPersistentList()
    }
    val initialRouteEntries = retain {
        persistentListOf(
            Navigation.Root.HomeScreen,
            Navigation.Root.SearchScreen,
            Navigation.Root.CollectionsScreen
        )
    }
    SettingsSectionScaffold(
        topAppBarText = Navigation.Settings.GeneralSettingsScreen.toString(),
    ) { paddingValues, topAppBarScrollBehaviour ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().addEdgeToEdgeScaffoldPadding(paddingValues)
                .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(30.dp)
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
                                settingsScreenViewModel.changeSettingPreferenceValue(
                                    preferenceKey = booleanPreferencesKey(
                                        AppPreferences.DESKTOP_TOP_DECORATOR.key
                                    ), newValue = it
                                )
                            },
                            isIconNeeded = true,
                            icon = Icons.Default.VideoLabel
                        )
                    )
                }
            }
            items(generalSectionData, key = {
                it.title
            }) { setting ->
                SettingComponent(setting)
            }

            item {
                SettingComponent(
                    SettingComponentParam(
                        title = Localization.Key.ChangeInitialRoute.rememberLocalizedString(),
                        doesDescriptionExists = true,
                        description = Localization.Key.ChangeInitialRouteDesc.rememberLocalizedString(),
                        isSwitchNeeded = false,
                        isSwitchEnabled = true,
                        onSwitchStateChange = {
                            showInitialNavigationChangerDialogBox = true
                        },
                        isIconNeeded = true,
                        icon = Icons.Default.Start
                    )
                )
            }

            item {
                SettingComponent(
                    SettingComponentParam(
                        title = Localization.Key.ChangeFontLabel.rememberLocalizedString(),
                        doesDescriptionExists = false,
                        description = "",
                        isSwitchNeeded = false,
                        isSwitchEnabled = true,
                        onSwitchStateChange = {
                            showFontFamilySwitcherDialogBox = true
                        },
                        isIconNeeded = true,
                        icon = Icons.Default.TextFields
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
                        isSwitchEnabled = true,
                        onSwitchStateChange = {
                            navController.navigate(Navigation.Root.OnboardingSlidesScreen)
                        },
                        icon = Icons.Outlined.PresentToAll,
                        isIconNeeded = true,
                    )
                )
            }
            if (platform is Platform.Android) {
                item {
                    HorizontalDivider(
                        modifier = Modifier.fillMaxWidth().padding(start = 15.dp, end = 15.dp)
                    )
                }
                item {
                    Text(
                        text = Localization.Key.SelectAnAppIcon.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(start = 15.dp, end = 15.dp, bottom = 15.dp),
                        fontSize = 16.sp
                    )
                    FlowRow(
                        modifier = Modifier.padding(start = 15.dp, end = 15.dp),
                        horizontalArrangement = Arrangement.spacedBy(15.dp)
                    ) {
                        AppIconCode.entries.forEach {
                            key(it.name) {
                                Box(
                                    Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                        .pressScaleEffect().clickable(onClick = {
                                            showIconSwitchDialogBox = true
                                            tempSelectedAppIcon = it.name
                                        }, indication = null, interactionSource = remember {
                                            MutableInteractionSource()
                                        }).size(65.dp), contentAlignment = Alignment.Center
                                ) {
                                    with(this@FlowRow) {
                                        AnimatedVisibility(
                                            preferences.selectedAppIcon == it.name,
                                            enter = fadeIn(),
                                            exit = fadeOut()
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxSize().border(
                                                    width = 5.dp,
                                                    color = MaterialTheme.colorScheme.primary,
                                                    shape = CircleShape
                                                )
                                            )
                                        }
                                    }
                                    Image(
                                        painter = painterResource(it.icon),
                                        contentDescription = null,
                                        modifier = Modifier.clip(CircleShape).size(50.dp)
                                    )
                                }
                            }
                        }
                    }
                    Text(
                        text = Localization.Key.AppIconCurrentlyInUse.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(
                            start = 15.dp, end = 15.dp, top = 15.dp, bottom = 5.dp
                        ),
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Text(
                        text = preferences.selectedAppIcon,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(
                            start = 15.dp, end = 15.dp, bottom = 15.dp
                        ),
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            item {
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
    }
    var showIconSwitchingProgressbar by rememberSaveable {
        mutableStateOf(false)
    }
    if (showIconSwitchDialogBox) {
        AlertDialog(modifier = Modifier.animateContentSize(), onDismissRequest = {
            if (!showIconSwitchingProgressbar) {
                showIconSwitchDialogBox = false
            }
        }, confirmButton = {
            if (!showIconSwitchingProgressbar) {
                Button(
                    onClick = {
                        showIconSwitchingProgressbar = true
                        settingsScreenViewModel.onIconChange(
                            newIconCode = tempSelectedAppIcon, onCompletion = {
                                showIconSwitchDialogBox = false
                            })
                    },
                    modifier = Modifier.pressScaleEffect().pointerHoverIcon(icon = PointerIcon.Hand)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = Localization.Key.Confirm.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            } else {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }, dismissButton = {
            if (!showIconSwitchingProgressbar) {
                OutlinedButton(
                    onClick = {
                        showIconSwitchDialogBox = false
                    },
                    modifier = Modifier.pressScaleEffect().pointerHoverIcon(icon = PointerIcon.Hand)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = Localization.Key.Cancel.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleSmall
                    )
                }
            }
        }, title = {
            Column {
                AppIconCode.entries.find {
                    it.name == tempSelectedAppIcon
                }?.let {
                    Image(
                        painter = painterResource(it.icon),
                        modifier = Modifier.clip(RoundedCornerShape(15.dp)).border(
                            width = 1.5.dp,
                            color = MaterialTheme.colorScheme.primary.copy(0.5f),
                            shape = RoundedCornerShape(15.dp)
                        ).size(75.dp),
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                }
                Text(
                    text = Localization.Key.ChangeAppIcon.rememberLocalizedString(),
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 22.sp
                )
            }
        }, text = {
            Column {
                Text(
                    text = Localization.Key.ChangeAppIconDesc.rememberLocalizedString(),
                    style = MaterialTheme.typography.titleSmall,
                    fontSize = 18.sp
                )
                if (tempSelectedAppIcon == AppIconCode.must_be_weather.name) {
                    InfoCard(
                        paddingValues = PaddingValues(top = 10.dp),
                        info = Localization.Key.ChangeInAppNameDesc.rememberLocalizedString()
                    )
                }
            }
        })
    }
    if (showInitialNavigationChangerDialogBox) {
        val currentlySelectedRoute = rememberSaveable {
            mutableStateOf(preferences.startDestination)
        }
        LaunchedEffect(Unit) {
            settingsScreenViewModel.currInitialRoute {
                currentlySelectedRoute.value = it
            }
        }
        SwitchDialogBox(
            title = Localization.Key.SelectTheInitialScreen.rememberLocalizedString(),
            entries = initialRouteEntries,
            selected = {
                currentlySelectedRoute.value == it.toString()
            },
            onEntryClick = {
                currentlySelectedRoute.value = it.toString()
            },
            onDismissRequest = {
                showInitialNavigationChangerDialogBox = false
            },
            onConfirm = {
                settingsScreenViewModel.changeSettingPreferenceValue(
                    stringPreferencesKey(
                        AppPreferences.INITIAL_ROUTE.key
                    ), currentlySelectedRoute.value
                )
                showInitialNavigationChangerDialogBox = false
            })
    }
    if (showFontFamilySwitcherDialogBox) {
        var tempSelectedFont by rememberSaveable {
            mutableStateOf(preferences.selectedFont.name)
        }
        SwitchDialogBox(
            title = Localization.Key.SelectAFontLabel.rememberLocalizedString(),
            entries = fontsEntries,
            selected = {
                tempSelectedFont == it.name
            },
            onEntryClick = {
                tempSelectedFont = it.name
            },
            onDismissRequest = {
                showFontFamilySwitcherDialogBox = false
            },
            onConfirm = {
                settingsScreenViewModel.changeSettingPreferenceValue(
                    stringPreferencesKey(
                        AppPreferences.FONT_TYPE.key
                    ), tempSelectedFont
                )
                showFontFamilySwitcherDialogBox = false
            })
    }
}

@Composable
private fun <T> SwitchDialogBox(
    title: String,
    entries: PersistentList<T>,
    entryLabel: (T) -> String = { it.toString() },
    selected: (T) -> Boolean,
    onEntryClick: (T) -> Unit,
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(onDismissRequest = onDismissRequest, confirmButton = {
        Button(
            onClick = onConfirm,
            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).fillMaxWidth()
        ) {
            Text(
                text = Localization.Key.Confirm.rememberLocalizedString(),
                style = MaterialTheme.typography.titleMedium
            )
        }
    }, dismissButton = {
        OutlinedButton(
            onClick = onDismissRequest,
            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).fillMaxWidth()
        ) {
            Text(
                text = Localization.Key.Cancel.rememberLocalizedString(),
                style = MaterialTheme.typography.titleSmall
            )
        }
    }, text = {
        Column(verticalArrangement = Arrangement.spacedBy(15.dp)) {
            entries.forEach {
                Row(
                    modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).fillMaxWidth()
                        .clickable(onClick = {
                            onEntryClick(it)
                        }, indication = null, interactionSource = remember {
                            MutableInteractionSource()
                        }).pressScaleEffect(), verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand),
                        selected = selected(it),
                        onClick = {
                            onEntryClick(it)
                        })
                    Text(
                        style = if (selected(it)) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
                        text = entryLabel(it),
                        color = if (selected(it)) MaterialTheme.colorScheme.primary else LocalContentColor.current,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }, title = {
        Text(
            text = title, style = MaterialTheme.typography.titleLarge, fontSize = 24.sp
        )
    })
}