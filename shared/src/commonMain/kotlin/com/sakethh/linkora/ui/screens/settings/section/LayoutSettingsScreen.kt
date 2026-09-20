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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sakethh.linkora.di.LinkoraSDK
import com.sakethh.linkora.di.linkoraViewModel
import com.sakethh.linkora.domain.AppPreferences
import com.sakethh.linkora.domain.PreferenceKey
import com.sakethh.linkora.ui.LocalizedStrings
import com.sakethh.linkora.ui.components.link.GridViewLinkComponent
import com.sakethh.linkora.ui.components.link.ListViewLinkComponent
import com.sakethh.linkora.ui.domain.Layout
import com.sakethh.linkora.ui.screens.settings.SettingsScreenViewModel
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingsSectionScaffold
import com.sakethh.linkora.utils.addEdgeToEdgeScaffoldPadding
import com.sakethh.linkora.utils.highlightOnFocused

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutSettingsScreen() {
    val localizedStrings = LocalizedStrings.current
    val settingsScreenViewModel: SettingsScreenViewModel = linkoraViewModel()
    val preferences by settingsScreenViewModel.preferencesAsFlow.collectAsStateWithLifecycle()
    val localUriHandler = LocalUriHandler.current
    val sampleLinksList =
        retain(preferences) {
            settingsScreenViewModel.sampleLinks(localUriHandler, preferences)
        }
    SettingsSectionScaffold(
        topAppBarText = localizedStrings.LinkLayoutSettings,
    ) { paddingValues, topAppBarScrollBehaviour ->
        when (preferences.selectedLinkLayout) {
            Layout.REGULAR_LIST_VIEW.name,
            Layout.TITLE_ONLY_LIST_VIEW.name,
                -> {
                LazyColumn(
                    modifier =
                        Modifier.fillMaxSize()
                            .addEdgeToEdgeScaffoldPadding(paddingValues)
                            .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection),
                ) {
                    item {
                        Text(
                            text = localizedStrings.ChooseTheLayoutYouLikeBest,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(15.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    items(Layout.entries) {
                        LinkViewRadioButtonComponent(
                            selectedLinkLayout = Layout.valueOf(preferences.selectedLinkLayout),
                            linkLayout = it,
                            changePreferenceValue = { preferenceKey: PreferenceKey<String>, newValue: String ->
                                settingsScreenViewModel.changeSettingPreferenceValue(
                                    preferenceKey = preferenceKey,
                                    newValue = newValue,
                                )
                            },
                            paddingValues = PaddingValues(start = 10.dp),
                        )
                    }

                    item {
                        Box(modifier = Modifier.padding(start = 10.dp, end = 10.dp)) {
                            LinkViewPreferenceSwitch(
                                onClick = {
                                    settingsScreenViewModel.changeSettingPreferenceValue(
                                        preferenceKey =
                                            AppPreferences.BASE_URL_VISIBILITY_FOR_NON_LIST_VIEWS,
                                        newValue = !preferences.showHostInLinkListView,
                                    )
                                },
                                title = localizedStrings.ShowHostAddress,
                                isSwitchChecked = preferences.showHostInLinkListView,
                            )
                        }
                    }

                    item {
                        Box(modifier = Modifier.padding(start = 10.dp, end = 10.dp)) {
                            LinkViewPreferenceSwitch(
                                onClick = {
                                    settingsScreenViewModel.changeSettingPreferenceValue(
                                        preferenceKey =
                                            AppPreferences.NOTE_VISIBILITY_IN_LIST_VIEWS,
                                        newValue = !preferences.showNoteInLinkView,
                                    )
                                },
                                title = localizedStrings.ShowNote,
                                isSwitchChecked = preferences.showNoteInLinkView,
                            )
                        }
                    }
                    item {
                        Box(modifier = Modifier.padding(start = 10.dp, end = 10.dp)) {
                            LinkViewPreferenceSwitch(
                                onClick = {
                                    settingsScreenViewModel.changeSettingPreferenceValue(
                                        preferenceKey =
                                            AppPreferences.SHOW_TAGS_IN_LINK_VIEW,
                                        newValue = !preferences.showTagsInLinkView,
                                    )
                                },
                                title = localizedStrings.ShowTagsLabel,
                                isSwitchChecked = preferences.showTagsInLinkView,
                            )
                        }
                    }
                    item {
                        Box(modifier = Modifier.padding(start = 10.dp, end = 10.dp)) {
                            LinkViewPreferenceSwitch(
                                onClick = {
                                    settingsScreenViewModel.changeSettingPreferenceValue(
                                        preferenceKey =
                                            AppPreferences.SHOW_DATE_IN_LINK_VIEW,
                                        newValue = !preferences.showDateInLinkView,
                                    )
                                },
                                title = localizedStrings.ShowDateLabel,
                                isSwitchChecked = preferences.showDateInLinkView,
                            )
                        }
                    }
                    item {
                        HorizontalDivider(
                            Modifier.padding(
                                start = 15.dp,
                                end = 15.dp,
                                top = 15.dp,
                                bottom = 5.dp,
                            ),
                        )
                    }

                    item {
                        Text(
                            text = localizedStrings.FeedPreview,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(15.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    items(sampleLinksList) {
                        ListViewLinkComponent(
                            linkComponentParam = it,
                            onShare = {
                                LinkoraSDK.getInstance().nativeUtils.onShare(it)
                            },
                            titleOnlyView = preferences.selectedLinkLayout == Layout.TITLE_ONLY_LIST_VIEW.name,
                            preferences = preferences,
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
                    modifier =
                        Modifier.padding(start = 10.dp, end = 10.dp)
                            .addEdgeToEdgeScaffoldPadding(paddingValues)
                            .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection)
                            .navigationBarsPadding(),
                ) {
                    item(
                        span = {
                            GridItemSpan(maxLineSpan)
                        },
                    ) {
                        Text(
                            text = localizedStrings.ChooseTheLayoutYouLikeBest,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 15.dp, bottom = 15.dp, start = 5.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    items(
                        Layout.entries,
                        span = {
                            GridItemSpan(maxLineSpan)
                        },
                    ) {
                        LinkViewRadioButtonComponent(
                            selectedLinkLayout = Layout.valueOf(preferences.selectedLinkLayout),
                            linkLayout = it,
                            changePreferenceValue = { preferenceKey: PreferenceKey<String>, newValue: String ->
                                settingsScreenViewModel.changeSettingPreferenceValue(
                                    preferenceKey = preferenceKey,
                                    newValue = newValue,
                                )
                            },
                        )
                    }

                    items(
                        settingsScreenViewModel.gridViewPref(preferences = preferences),
                        span = {
                            GridItemSpan(maxLineSpan)
                        },
                    ) {
                        LinkViewPreferenceSwitch(
                            onClick = it.onClick,
                            title = it.title,
                            isSwitchChecked = it.isSwitchChecked(),
                        )
                    }

                    item(
                        span = {
                            GridItemSpan(maxLineSpan)
                        },
                    ) {
                        HorizontalDivider(
                            modifier =
                                Modifier.padding(
                                    top = 15.dp,
                                    bottom = 5.dp,
                                    start = 5.dp,
                                    end = 5.dp,
                                ),
                        )
                    }

                    item(
                        span = {
                            GridItemSpan(maxLineSpan)
                        },
                    ) {
                        Text(
                            text = localizedStrings.FeedPreview,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 10.dp, bottom = 15.dp, start = 5.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    items(sampleLinksList) {
                        GridViewLinkComponent(
                            preferences = preferences,
                            linkComponentParam = it,
                            forStaggeredView = false,
                        )
                    }
                    item(
                        span = {
                            GridItemSpan(maxLineSpan)
                        },
                    ) {
                        Spacer(Modifier.height(100.dp))
                    }
                }
            }

            else -> {
                LazyVerticalStaggeredGrid(
                    columns = StaggeredGridCells.Adaptive(150.dp),
                    modifier =
                        Modifier.padding(start = 10.dp, end = 10.dp)
                            .addEdgeToEdgeScaffoldPadding(paddingValues)
                            .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection),
                ) {
                    item(span = StaggeredGridItemSpan.FullLine) {
                        Text(
                            text = localizedStrings.ChooseTheLayoutYouLikeBest,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 15.dp, bottom = 15.dp, start = 5.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }

                    items(
                        items = Layout.entries,
                        span = {
                            StaggeredGridItemSpan.FullLine
                        },
                    ) {
                        LinkViewRadioButtonComponent(
                            selectedLinkLayout = Layout.valueOf(preferences.selectedLinkLayout),
                            linkLayout = it,
                            changePreferenceValue = { preferenceKey: PreferenceKey<String>, newValue: String ->
                                settingsScreenViewModel.changeSettingPreferenceValue(
                                    preferenceKey = preferenceKey,
                                    newValue = newValue,
                                )
                            },
                        )
                    }

                    items(
                        items = settingsScreenViewModel.gridViewPref(preferences = preferences),
                        span = { StaggeredGridItemSpan.FullLine },
                    ) {
                        LinkViewPreferenceSwitch(
                            onClick = it.onClick,
                            title = it.title,
                            isSwitchChecked = it.isSwitchChecked(),
                        )
                    }

                    item(span = StaggeredGridItemSpan.FullLine) {
                        HorizontalDivider(
                            modifier =
                                Modifier.padding(
                                    top = 15.dp,
                                    bottom = 5.dp,
                                    start = 5.dp,
                                    end = 5.dp,
                                ),
                        )
                    }

                    item(span = StaggeredGridItemSpan.FullLine) {
                        Text(
                            text = localizedStrings.FeedPreview,
                            style = MaterialTheme.typography.titleSmall,
                            modifier = Modifier.padding(top = 10.dp, bottom = 15.dp, start = 5.dp),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    items(sampleLinksList) {
                        GridViewLinkComponent(
                            preferences = preferences,
                            linkComponentParam = it,
                            forStaggeredView = true,
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
    onClick: () -> Unit,
    title: String,
    isSwitchChecked: Boolean,
) {
    Row(
        modifier =
            Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                .highlightOnFocused()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        onClick()
                    },
                )
                .fillMaxWidth()
                .padding(start = 15.dp, end = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.fillMaxWidth(0.75f),
        )
        Switch(
            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand),
            checked = isSwitchChecked,
            onCheckedChange = {
                onClick()
            },
        )
    }
}

@Composable
private fun LinkViewRadioButtonComponent(
    selectedLinkLayout: Layout,
    linkLayout: Layout,
    paddingValues: PaddingValues = PaddingValues(0.dp),
    changePreferenceValue: (preferenceKey: PreferenceKey<String>, newValue: String) -> Unit,
) {
    val localizedStrings = LocalizedStrings.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier =
            Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                .highlightOnFocused()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = {
                        changePreferenceValue(
                            AppPreferences.CURRENTLY_SELECTED_LINK_VIEW,
                            linkLayout.name,
                        )
                    },
                )
                .fillMaxWidth()
                .padding(paddingValues),
    ) {
        RadioButton(
            selected = selectedLinkLayout == linkLayout,
            onClick = {
                changePreferenceValue(
                    AppPreferences.CURRENTLY_SELECTED_LINK_VIEW,
                    linkLayout.name,
                )
            },
        )
        Text(
            text =
                when (linkLayout) {
                    Layout.REGULAR_LIST_VIEW -> localizedStrings.RegularListView

                    Layout.TITLE_ONLY_LIST_VIEW ->
                        localizedStrings.TitleOnlyListView

                    Layout.GRID_VIEW -> localizedStrings.GridView

                    Layout.STAGGERED_VIEW -> localizedStrings.StaggeredView
                },
            style = MaterialTheme.typography.titleSmall,
        )
    }
}
