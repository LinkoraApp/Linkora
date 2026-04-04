package com.sakethh.linkora.ui.screens.settings.section

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sakethh.linkora.Localization
import com.sakethh.linkora.di.linkoraViewModel
import com.sakethh.linkora.domain.LinkoraPlaceHolder
import com.sakethh.linkora.domain.model.localization.LocalizedLanguage
import com.sakethh.linkora.ui.components.LoadingDialog
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.DataEmptyScreen
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingsSectionScaffold
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.ui.utils.rememberDeserializableMutableObject
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.addEdgeToEdgeScaffoldPadding
import com.sakethh.linkora.utils.inDoubleQuotes
import com.sakethh.linkora.utils.rememberLocalizedString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguageSettingsScreen() {
    val languageSettingsScreenVM: LanguageSettingsScreenVM = linkoraViewModel()
    val preferences by languageSettingsScreenVM.preferencesAsFlow.collectAsStateWithLifecycle()
    val availableLanguages =
        languageSettingsScreenVM.availableLanguages.collectAsStateWithLifecycle()
    val isLanguageSelectionBtmSheetVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val doesRemoteLanguagePackExistsLocallyForTheSelectedLanguage = rememberSaveable {
        mutableStateOf(false)
    }
    val selectedLanguage = rememberDeserializableMutableObject<LocalizedLanguage> {
        mutableStateOf(
            LocalizedLanguage(
                languageCode = "",
                languageName = "",
                localizedStringsCount = 0,
                contributionLink = ""
            )
        )
    }
    SettingsSectionScaffold(
        topAppBarText = Navigation.Settings.LanguageSettingsScreen.toString(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                    .padding(start = 15.dp, end = 15.dp), onClick = {
                    languageSettingsScreenVM.fetchRemoteLanguages()
                }) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "")
                Spacer(modifier = Modifier.width(15.dp))
                Text(
                    text = Localization.Key.RetrieveLanguageInfoFromServer.rememberLocalizedString(),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(end = 5.dp)
                )
            }
        }) { paddingValues, topAppBarScrollBehaviour ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().addEdgeToEdgeScaffoldPadding(paddingValues)
                .padding(start = 15.dp, end = 15.dp)
                .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            item {
                Spacer(modifier = Modifier)
            }
            item {
                Text(
                    text = Localization.Key.AppLanguage.rememberLocalizedString(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                Text(
                    text = preferences.preferredAppLanguageName,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp
                )
            }
            item {
                HorizontalDivider(
                    modifier = Modifier.fillMaxWidth().padding(top = 15.dp)
                )
            }
            item {
                Box(
                    modifier = Modifier.fillMaxWidth().animateContentSize()
                ) {
                    if (preferences.preferredAppLanguageCode != Constants.DEFAULT_APP_LANGUAGE_CODE) {
                        FilledTonalButton(
                            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                .fillMaxWidth().padding(top = 15.dp, bottom = 15.dp)
                                .pressScaleEffect(), onClick = {
                                isLanguageSelectionBtmSheetVisible.value = false
                                Localization.loadDefaultValues(preferences)
                            }) {
                            Text(
                                text = Localization.Key.ResetAppLanguage.rememberLocalizedString(),
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }
            }
            item {
                Column(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = Localization.Key.AvailableLanguages.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            if (availableLanguages.value.isEmpty()) {
                item {
                    DataEmptyScreen(
                        text = Localization.Key.NoRemoteLangPacks.rememberLocalizedString(),
                        paddingValues = PaddingValues(top = 30.dp)
                    )
                }
            }

            items(availableLanguages.value) {
                LanguageUIComponent(
                    onClick = {
                        languageSettingsScreenVM.doesLanguagePackExists(
                            doesRemoteLanguagePackExistsLocallyForTheSelectedLanguage,
                            it.languageCode
                        )
                        selectedLanguage.value = LocalizedLanguage(
                            languageCode = it.languageCode,
                            languageName = it.languageName,
                            localizedStringsCount = it.localizedStringsCount,
                            contributionLink = it.contributionLink
                        )
                        isLanguageSelectionBtmSheetVisible.value =
                            !isLanguageSelectionBtmSheetVisible.value
                    },
                    text = it.languageName,
                    isRemoteLanguage = true,
                    localizationStatus = Localization.Key.StringsLocalizedStatus.rememberLocalizedString()
                        .replace(
                            LinkoraPlaceHolder.First.value, it.localizedStringsCount.toString()
                        ).replace(
                            LinkoraPlaceHolder.Second.value,
                            Localization.Key.entries.size.toString()
                        ),
                    localizationStatusFraction = it.localizedStringsCount.toFloat() / Localization.Key.entries.size.toFloat()
                )
                Spacer(modifier = Modifier.height(15.dp))
            }
            item {
                Spacer(modifier = Modifier.height(200.dp))
            }
        }
    }
    if (isLanguageSelectionBtmSheetVisible.value) {
        ModalBottomSheet(onDismissRequest = {
            isLanguageSelectionBtmSheetVisible.value = !isLanguageSelectionBtmSheetVisible.value
        }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = selectedLanguage.value.languageName,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 15.dp, bottom = 7.5.dp),
                    color = MaterialTheme.colorScheme.primary
                )
                if (doesRemoteLanguagePackExistsLocallyForTheSelectedLanguage.value) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                            .clickable(onClick = {
                                isLanguageSelectionBtmSheetVisible.value = false
                                Localization.loadLocalizedStrings(
                                    languageCode = selectedLanguage.value.languageCode,
                                    languageName = selectedLanguage.value.languageName,
                                    preferences = preferences
                                )
                            }, indication = null, interactionSource = remember {
                                MutableInteractionSource()
                            }).pressScaleEffect().fillMaxWidth()
                            .padding(top = 7.5.dp, bottom = 7.5.dp, start = 10.dp, end = 15.dp)
                    ) {
                        FilledTonalIconButton(
                            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                .pressScaleEffect(), onClick = {
                                isLanguageSelectionBtmSheetVisible.value = false
                                Localization.loadLocalizedStrings(
                                    languageCode = selectedLanguage.value.languageCode,
                                    languageName = selectedLanguage.value.languageName,
                                    preferences = preferences
                                )
                            }) {
                            Icon(imageVector = Icons.Default.Cloud, contentDescription = "")
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = Localization.Key.LoadServerStrings.rememberLocalizedString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 16.sp
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                        .clickable(onClick = {
                            languageSettingsScreenVM.downloadALanguageStringsPack(selectedLanguage.value)
                            isLanguageSelectionBtmSheetVisible.value = false
                        }, indication = null, interactionSource = remember {
                            MutableInteractionSource()
                        }).pressScaleEffect().fillMaxWidth()
                        .padding(top = 7.5.dp, bottom = 7.5.dp, start = 10.dp, end = 15.dp)
                ) {
                    FilledTonalIconButton(
                        modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                            .pressScaleEffect(), onClick = {
                            languageSettingsScreenVM.downloadALanguageStringsPack(selectedLanguage.value)
                            isLanguageSelectionBtmSheetVisible.value = false
                        }) {
                        Icon(
                            imageVector = Icons.Default.DownloadForOffline, contentDescription = ""
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = if (doesRemoteLanguagePackExistsLocallyForTheSelectedLanguage.value) {
                            Localization.Key.UpdateLanguageStrings
                        } else {
                            Localization.Key.DownloadLanguageStrings
                        }.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 16.sp
                    )
                }
                if (doesRemoteLanguagePackExistsLocallyForTheSelectedLanguage.value) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                            .clickable(onClick = {
                                languageSettingsScreenVM.deleteALanguagePack(selectedLanguage.value)
                                isLanguageSelectionBtmSheetVisible.value = false
                            }, indication = null, interactionSource = remember {
                                MutableInteractionSource()
                            }).pressScaleEffect().fillMaxWidth()
                            .padding(top = 7.5.dp, bottom = 7.5.dp, start = 10.dp, end = 15.dp)
                    ) {
                        FilledTonalIconButton(
                            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                                .pressScaleEffect(), onClick = {
                                languageSettingsScreenVM.deleteALanguagePack(selectedLanguage.value)
                                isLanguageSelectionBtmSheetVisible.value = false
                            }) {
                            Icon(
                                imageVector = Icons.Default.DeleteForever, contentDescription = ""
                            )
                        }
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = Localization.Key.RemoveLanguageStrings.rememberLocalizedString(),
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }
    LoadingDialog(
        shouldDialogBoxAppear = languageSettingsScreenVM.languageSettingsState.value.fetchingLanguageInfo || languageSettingsScreenVM.languageSettingsState.value.fetchingStrings,
        text = if (languageSettingsScreenVM.languageSettingsState.value.fetchingLanguageInfo) Localization.Key.FetchingAvailableLanguages.rememberLocalizedString() else Localization.Key.DownloadingStrings.rememberLocalizedString()
            .replace(
                LinkoraPlaceHolder.First.value, selectedLanguage.value.languageName.inDoubleQuotes()
            )
    )
}

@Composable
private fun LanguageUIComponent(
    onClick: () -> Unit,
    text: String,
    isRemoteLanguage: Boolean,
    localizationStatus: String,
    localizationStatusFraction: Float
) {
    Row(
        Modifier.pointerHoverIcon(icon = PointerIcon.Hand).fillMaxWidth()
            .clickable(interactionSource = remember {
                MutableInteractionSource()
            }, indication = null, onClick = {
                onClick()
            }).pressScaleEffect(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(Modifier.fillMaxWidth(0.8f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isRemoteLanguage) Icons.Default.Cloud else Icons.Default.Code,
                    contentDescription = "",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(5.dp))
                Text(
                    text = text, style = MaterialTheme.typography.titleSmall, fontSize = 16.sp
                )
            }
            LinearProgressIndicator(
                progress = { localizationStatusFraction },
                modifier = Modifier.fillMaxWidth().padding(top = 10.dp, bottom = 10.dp)
            )
            Text(
                text = localizationStatus, style = MaterialTheme.typography.titleSmall
            )
        }
        Box(
            modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd
        ) {
            IconButton(
                modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).pressScaleEffect(),
                onClick = {
                    onClick()
                }) {
                Icon(
                    imageVector = Icons.Default.MoreVert, contentDescription = ""
                )
            }
        }
    }
}