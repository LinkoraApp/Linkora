package com.sakethh.linkora.ui.screens.settings.specific.language

import android.widget.Toast
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DownloadForOffline
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.sakethh.linkora.LocalizedStrings.appLanguage
import com.sakethh.linkora.LocalizedStrings.availableLanguages
import com.sakethh.linkora.LocalizedStrings.language
import com.sakethh.linkora.LocalizedStrings.resetAppLanguage
import com.sakethh.linkora.ui.CommonUiEvent
import com.sakethh.linkora.ui.commonComposables.pulsateEffect
import com.sakethh.linkora.ui.screens.CustomWebTab
import com.sakethh.linkora.ui.screens.settings.SettingsPreference
import com.sakethh.linkora.ui.screens.settings.SettingsPreference.dataStore
import com.sakethh.linkora.ui.screens.settings.SettingsPreference.preferredAppLanguageName
import com.sakethh.linkora.ui.screens.settings.SettingsPreference.readSettingPreferenceValue
import com.sakethh.linkora.ui.screens.settings.SettingsPreferences
import com.sakethh.linkora.ui.screens.settings.composables.SpecificSettingsScreenScaffold
import kotlinx.coroutines.flow.collectLatest

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LanguageSettingsScreen(
    navController: NavController,
    customWebTab: CustomWebTab
) {
    val context = LocalContext.current
    val languageSettingsScreenVM: LanguageSettingsScreenVM = hiltViewModel()
    LaunchedEffect(key1 = Unit) {
        languageSettingsScreenVM.eventChannel.collectLatest {
            when (it) {
                is CommonUiEvent.ShowToast -> {
                    Toast.makeText(context, it.msg, Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
    LaunchedEffect(key1 = Unit) {
        preferredAppLanguageName.value = readSettingPreferenceValue(
            stringPreferencesKey(SettingsPreferences.APP_LANGUAGE_NAME.name),
            context.dataStore
        ) ?: "English"

        SettingsPreference.preferredAppLanguageCode.value = readSettingPreferenceValue(
            stringPreferencesKey(SettingsPreferences.APP_LANGUAGE_CODE.name),
            context.dataStore
        ) ?: "en"
    }
    val isLanguageSelectionBtmSheetVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val currentlySelectedLanguageCode = rememberSaveable {
        mutableStateOf("")
    }
    val currentlySelectedLanguageName = rememberSaveable {
        mutableStateOf("")
    }
    val currentlySelectedLanguageContributionLink = rememberSaveable {
        mutableStateOf("")
    }
    val languageSelectionBtmSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    SpecificSettingsScreenScaffold(
        topAppBarText = language.value,
        navController = navController
    ) { paddingValues, topAppBarScrollBehaviour ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection)
                .navigationBarsPadding()
                .padding(start = 15.dp, end = 15.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(30.dp))
            }
            item {
                Text(
                    text = appLanguage.value,
                    style = MaterialTheme.typography.titleSmall
                )
                Spacer(modifier = Modifier.height(15.dp))
            }
            item {
                Text(
                    text = preferredAppLanguageName.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 18.sp
                )
            }
            item {
                Card(
                    border = BorderStroke(
                        1.dp,
                        contentColorFor(MaterialTheme.colorScheme.surface)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 15.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .wrapContentHeight()
                            .padding(
                                top = 10.dp, bottom = 10.dp
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Info,
                                contentDescription = null,
                                modifier = Modifier
                                    .padding(
                                        start = 10.dp, end = 10.dp
                                    )
                            )
                        }
                        Text(
                            text = if (SettingsPreference.useLanguageStringsBasedOnFetchedValuesFromServer.value) "" else "Using compiled strings",
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 14.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Start,
                            modifier = Modifier
                                .padding(end = 10.dp)
                        )
                    }
                }
                HorizontalDivider(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 15.dp)
                )
            }
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateContentSize()
                ) {
                    if (preferredAppLanguageName.value != "English") {
                        FilledTonalButton(modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 15.dp, bottom = 15.dp, start = 10.dp, end = 10.dp)
                            .pulsateEffect(), onClick = {
                            languageSettingsScreenVM.onClick(
                                LanguageSettingsScreenUIEvent.UpdatePreferredLocalLanguage(
                                    context,
                                    languageCode = "en",
                                    languageName = "English"
                                )
                            )
                        }) {
                            Text(
                                text = resetAppLanguage.value,
                                style = MaterialTheme.typography.titleSmall
                            )
                        }
                    }
                }

            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Spacer(modifier = Modifier.height(15.dp))
                    Text(
                        text = availableLanguages.value,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(15.dp))
                }
            }
            items(languageSettingsScreenVM.availableLanguages) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(interactionSource = remember {
                            MutableInteractionSource()
                        }, indication = null, onClick = {
                            isLanguageSelectionBtmSheetVisible.value =
                                !isLanguageSelectionBtmSheetVisible.value
                        })
                        .pulsateEffect(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(Modifier.fillMaxWidth(0.8f)) {
                        Text(
                            text = it.languageName,
                            style = MaterialTheme.typography.titleSmall,
                            fontSize = 16.sp
                        )
                        LinearProgressIndicator(
                            progress = { 0.75f }, modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp, bottom = 10.dp)
                        )
                        Text(
                            text = "9 of 12 strings localized",
                            style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Box(
                        modifier = Modifier.fillMaxWidth(),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        IconButton(
                            modifier = Modifier.pulsateEffect(),
                            onClick = {
                                currentlySelectedLanguageCode.value = it.languageCode
                                currentlySelectedLanguageName.value = it.languageName
                                currentlySelectedLanguageContributionLink.value =
                                    it.languageContributionLink
                                isLanguageSelectionBtmSheetVisible.value =
                                    !isLanguageSelectionBtmSheetVisible.value
                            }) {
                            Icon(
                                imageVector = Icons.Default.MoreVert,
                                contentDescription = ""
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(15.dp))
            }
        }
    }
    if (isLanguageSelectionBtmSheetVisible.value) {
        ModalBottomSheet(onDismissRequest = {
            isLanguageSelectionBtmSheetVisible.value =
                !isLanguageSelectionBtmSheetVisible.value
        }) {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = currentlySelectedLanguageName.value,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 15.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable {
                            languageSettingsScreenVM.onClick(
                                LanguageSettingsScreenUIEvent.UseStringsFetchedFromTheServer(
                                    context = context,
                                    languageCode = currentlySelectedLanguageCode.value,
                                    languageName = currentlySelectedLanguageName.value
                                )
                            )
                        }
                        .fillMaxWidth()
                        .padding(top = 15.dp, start = 10.dp, end = 15.dp)
                ) {
                    FilledTonalIconButton(
                        modifier = Modifier.pulsateEffect(),
                        onClick = {
                            languageSettingsScreenVM.onClick(
                                LanguageSettingsScreenUIEvent.UseStringsFetchedFromTheServer(
                                    context = context,
                                    languageCode = currentlySelectedLanguageCode.value,
                                    languageName = currentlySelectedLanguageName.value
                                )
                            )
                        }) {
                        Icon(imageVector = Icons.Default.Cloud, contentDescription = "")
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Use strings fetched from the server",
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 16.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable {
                            languageSettingsScreenVM.onClick(
                                LanguageSettingsScreenUIEvent.UseCompiledStrings(
                                    context = context,
                                    languageCode = currentlySelectedLanguageCode.value,
                                    languageName = currentlySelectedLanguageName.value
                                )
                            )
                        }
                        .fillMaxWidth()
                        .padding(top = 15.dp, start = 10.dp, end = 15.dp)
                ) {
                    FilledTonalIconButton(
                        modifier = Modifier.pulsateEffect(),
                        onClick = {
                            languageSettingsScreenVM.onClick(
                                LanguageSettingsScreenUIEvent.UseCompiledStrings(
                                    context = context,
                                    languageCode = currentlySelectedLanguageCode.value,
                                    languageName = currentlySelectedLanguageName.value
                                )
                            )
                        }) {
                        Icon(imageVector = Icons.Default.Code, contentDescription = "")
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Use compiled strings",
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 16.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable {
                            languageSettingsScreenVM.onClick(
                                LanguageSettingsScreenUIEvent.DownloadLatestLanguageStrings(
                                    languageCode = currentlySelectedLanguageCode.value,
                                    languageName = currentlySelectedLanguageName.value
                                )
                            )
                        }
                        .fillMaxWidth()
                        .padding(top = 15.dp, start = 10.dp, end = 15.dp)
                ) {
                    FilledTonalIconButton(
                        modifier = Modifier.pulsateEffect(),
                        onClick = {
                            languageSettingsScreenVM.onClick(
                                LanguageSettingsScreenUIEvent.DownloadLatestLanguageStrings(
                                    languageCode = currentlySelectedLanguageCode.value,
                                    languageName = currentlySelectedLanguageName.value
                                )
                            )
                        }) {
                        Icon(
                            imageVector = Icons.Default.DownloadForOffline,
                            contentDescription = ""
                        )
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Download latest language strings",
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 16.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 10.dp, top = 15.dp, bottom = 30.dp, end = 15.dp)
                ) {
                    FilledTonalIconButton(
                        modifier = Modifier.pulsateEffect(),
                        onClick = {

                        }) {
                        Icon(imageVector = Icons.Default.Translate, contentDescription = "")
                    }
                    Spacer(modifier = Modifier.width(5.dp))
                    Text(
                        text = "Contribute",
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}