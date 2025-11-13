package com.sakethh.linkora.ui.screens.settings.section

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sakethh.linkora.Localization
import com.sakethh.linkora.di.LinkoraSDK
import com.sakethh.linkora.di.linkoraViewModel
import com.sakethh.linkora.preferences.AppPreferenceType
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.components.link.GridViewLinkComponent
import com.sakethh.linkora.ui.components.link.ListViewLinkComponent
import com.sakethh.linkora.ui.domain.Layout
import com.sakethh.linkora.ui.screens.settings.SettingsScreenViewModel
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingsSectionScaffold
import com.sakethh.linkora.utils.addEdgeToEdgeScaffoldPadding
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.rememberLocalizedString

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutSettingsScreen() {
    val navController = LocalNavController.current
    val settingsScreenViewModel: SettingsScreenViewModel = linkoraViewModel()
    val localUriHandler = LocalUriHandler.current
    val sampleList = remember {
        settingsScreenViewModel.sampleLinks(localUriHandler)
    }
    SettingsSectionScaffold(
        topAppBarText = Localization.Key.LinkLayoutSettings.rememberLocalizedString(),
        navController = navController
    ) { paddingValues, topAppBarScrollBehaviour ->
        when (AppPreferences.selectedLinkLayout.value) {
            Layout.REGULAR_LIST_VIEW.name, Layout.TITLE_ONLY_LIST_VIEW.name -> {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().addEdgeToEdgeScaffoldPadding(paddingValues)
                        .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection)
                ) {
                    item {
                        Text(
                            text = Localization.Key.ChooseTheLayoutYouLikeBest.rememberLocalizedString(),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(15.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(Layout.entries) {
                        LinkViewRadioButtonComponent(
                            it, settingsScreenViewModel, PaddingValues(start = 10.dp)
                        )
                    }

                    item {
                        Box(
                            modifier = Modifier.padding(start = 10.dp, end = 10.dp)
                        ) {
                            LinkViewPreferenceSwitch(
                                onClick = {
                                    AppPreferences.showHostInLinkListView.value =
                                        !AppPreferences.showHostInLinkListView.value
                                    settingsScreenViewModel.changeSettingPreferenceValue(
                                        preferenceKey = booleanPreferencesKey(AppPreferenceType.BASE_URL_VISIBILITY_FOR_NON_LIST_VIEWS.name),
                                        newValue = AppPreferences.showHostInLinkListView.value
                                    )
                                },
                                title = Localization.Key.ShowHostAddress.getLocalizedString(),
                                isSwitchChecked = AppPreferences.showHostInLinkListView.value
                            )
                        }
                    }

                    item {
                        Box(
                            modifier = Modifier.padding(start = 10.dp, end = 10.dp)
                        ) {
                            LinkViewPreferenceSwitch(
                                onClick = {
                                    AppPreferences.showNoteInListViewLayout.value =
                                        !AppPreferences.showNoteInListViewLayout.value
                                    settingsScreenViewModel.changeSettingPreferenceValue(
                                        preferenceKey = booleanPreferencesKey(AppPreferenceType.NOTE_VISIBILITY_IN_LIST_VIEWS.name),
                                        newValue = AppPreferences.showNoteInListViewLayout.value
                                    )
                                },
                                title = Localization.Key.ShowNote.getLocalizedString(),
                                isSwitchChecked = AppPreferences.showNoteInListViewLayout.value
                            )
                        }
                    }

                    item {
                        HorizontalDivider(
                            Modifier.padding(
                                start = 15.dp, end = 15.dp, top = 15.dp, bottom = 5.dp
                            )
                        )
                    }

                    item {
                        Text(
                            text = Localization.Key.FeedPreview.rememberLocalizedString(),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(15.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(sampleList) {
                        ListViewLinkComponent(
                            linkComponentParam = it,
                            onShare = {
                                LinkoraSDK.getInstance().nativeUtils.onShare(it)
                            },
                            titleOnlyView = AppPreferences.selectedLinkLayout.value == Layout.TITLE_ONLY_LIST_VIEW.name,
                        )
                    }
                    item {
                        Spacer(Modifier.height(100.dp))
                    }
                }
            }

            Layout.GRID_VIEW.name -> {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(150.dp),
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp).addEdgeToEdgeScaffoldPadding(paddingValues)
                        .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection)
                        .navigationBarsPadding()
                ) {
                    item(span = {
                        GridItemSpan(maxLineSpan)
                    }) {
                        Text(
                            text = Localization.Key.ChooseTheLayoutYouLikeBest.getLocalizedString(),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 15.dp, bottom = 15.dp, start = 5.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(Layout.entries, span = {
                        GridItemSpan(maxLineSpan)
                    }) {
                        LinkViewRadioButtonComponent(
                            it, settingsScreenViewModel
                        )
                    }

                    items(settingsScreenViewModel.gridViewPref, span = {
                        GridItemSpan(maxLineSpan)
                    }) {
                        LinkViewPreferenceSwitch(
                            onClick = it.onClick,
                            title = it.title,
                            isSwitchChecked = it.isSwitchChecked()
                        )
                    }

                    item(span = {
                        GridItemSpan(maxLineSpan)
                    }) {
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                top = 15.dp, bottom = 5.dp, start = 5.dp, end = 5.dp
                            ),
                        )
                    }

                    item(span = {
                        GridItemSpan(maxLineSpan)
                    }) {
                        Text(
                            text = Localization.Key.FeedPreview.getLocalizedString(),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 10.dp, bottom = 15.dp, start = 5.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(sampleList) {
                        GridViewLinkComponent(it, forStaggeredView = false)
                    }
                    item(span = {
                        GridItemSpan(maxLineSpan)
                    }) {
                        Spacer(Modifier.height(100.dp))
                    }
                }
            }

            else -> {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(150.dp),
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp)
                        .addEdgeToEdgeScaffoldPadding(paddingValues)
                        .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection)
                ) {
                    item(
                        span = StaggeredGridItemSpan.FullLine
                    ) {
                        Text(
                            text = Localization.Key.ChooseTheLayoutYouLikeBest.getLocalizedString(),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 15.dp, bottom = 15.dp, start = 5.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    items(
                        items = Layout.entries, span = {
                            StaggeredGridItemSpan.FullLine
                        }) {
                        LinkViewRadioButtonComponent(
                            it, settingsScreenViewModel
                        )
                    }

                    items(
                        items = settingsScreenViewModel.gridViewPref,
                        span = { StaggeredGridItemSpan.FullLine }) {
                        LinkViewPreferenceSwitch(
                            onClick = it.onClick,
                            title = it.title,
                            isSwitchChecked = it.isSwitchChecked()
                        )
                    }

                    item(span = StaggeredGridItemSpan.FullLine) {
                        HorizontalDivider(
                            modifier = Modifier.padding(
                                top = 15.dp, bottom = 5.dp, start = 5.dp, end = 5.dp
                            ),
                        )
                    }

                    item(span = StaggeredGridItemSpan.FullLine) {
                        Text(
                            text = Localization.Key.FeedPreview.getLocalizedString(),
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 10.dp, bottom = 15.dp, start = 5.dp),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    items(sampleList) {
                        GridViewLinkComponent(
                            linkComponentParam = it, forStaggeredView = true
                        )
                    }
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Spacer(Modifier.height(100.dp))
                    }
                }
            }
        }
    }
}


@Composable
private fun LinkViewPreferenceSwitch(
    onClick: () -> Unit, title: String, isSwitchChecked: Boolean
) {
    Row(
        modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).fillMaxWidth()
            .clickable(onClick = {
                onClick()
            }, interactionSource = remember {
                MutableInteractionSource()
            }, indication = null).padding(start = 15.dp, end = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.fillMaxWidth(0.75f)
        )
        Switch(
            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand),
            checked = isSwitchChecked,
            onCheckedChange = {
                onClick()
            })
    }
}

@Composable
private fun LinkViewRadioButtonComponent(
    linkLayout: Layout,
    settingsScreenViewModel: SettingsScreenViewModel,
    paddingValues: PaddingValues = PaddingValues(0.dp)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).fillMaxWidth()
            .clickable(interactionSource = null, indication = null) {
                AppPreferences.selectedLinkLayout.value = linkLayout.name
                settingsScreenViewModel.changeSettingPreferenceValue(
                    preferenceKey = stringPreferencesKey(AppPreferenceType.CURRENTLY_SELECTED_LINK_VIEW.name),
                    newValue = linkLayout.name
                )
            }.padding(paddingValues)
    ) {
        RadioButton(
            selected = AppPreferences.selectedLinkLayout.value == linkLayout.name, onClick = {
                AppPreferences.selectedLinkLayout.value = linkLayout.name
                settingsScreenViewModel.changeSettingPreferenceValue(
                    preferenceKey = stringPreferencesKey(AppPreferenceType.CURRENTLY_SELECTED_LINK_VIEW.name),
                    newValue = linkLayout.name
                )
            })
        Text(
            text = when (linkLayout) {
                Layout.REGULAR_LIST_VIEW -> Localization.Key.RegularListView.rememberLocalizedString()
                Layout.TITLE_ONLY_LIST_VIEW -> Localization.Key.TitleOnlyListView.rememberLocalizedString()
                Layout.GRID_VIEW -> Localization.Key.GridView.rememberLocalizedString()
                Layout.STAGGERED_VIEW -> Localization.Key.StaggeredView.rememberLocalizedString()
            }, style = MaterialTheme.typography.titleSmall
        )
    }
}