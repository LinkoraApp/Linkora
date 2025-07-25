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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sakethh.linkora.common.Localization
import com.sakethh.linkora.common.preferences.AppPreferenceType
import com.sakethh.linkora.common.preferences.AppPreferences
import com.sakethh.linkora.common.utils.addEdgeToEdgeScaffoldPadding
import com.sakethh.linkora.common.utils.getLocalizedString
import com.sakethh.linkora.common.utils.rememberLocalizedString
import com.sakethh.linkora.di.linkoraViewModel
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.MediaType
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.components.link.GridViewLinkUIComponent
import com.sakethh.linkora.ui.components.link.LinkListItemComposable
import com.sakethh.linkora.ui.domain.Layout
import com.sakethh.linkora.ui.domain.model.LinkPref
import com.sakethh.linkora.ui.domain.model.LinkUIComponentParam
import com.sakethh.linkora.ui.screens.settings.SettingsScreenViewModel
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingsSectionScaffold

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LayoutSettingsScreen() {
    val navController = LocalNavController.current
    val settingsScreenViewModel: SettingsScreenViewModel = linkoraViewModel()
    val localUriHandler = LocalUriHandler.current
    val sampleList = remember {
        listOf(
            LinkUIComponentParam(
                link = Link(
                    title = "Red Dead Redemption 2 - Rockstar Games",
                    baseURL = "rockstargames.com",
                    imgURL = "https://media-rockstargames-com.akamaized.net/rockstargames-newsite/img/global/games/fob/640/reddeadredemption2.jpg",
                    url = "https://www.rockstargames.com/reddeadredemption2",
                    userAgent = AppPreferences.primaryJsoupUserAgent.value,
                    linkType = LinkType.SAVED_LINK,
                    localId = 0L,
                    note = "RDR2 is the epic tale of outlaw Arthur Morgan and the infamous Van der Linde gang, on the run across America at the dawn of the modern age.",
                    idOfLinkedFolder = null
                ),
                onMoreIconClick = { -> },
                onLinkClick = { ->
                    localUriHandler.openUri("https://www.rockstargames.com/reddeadredemption2")
                },
                onForceOpenInExternalBrowserClicked = { -> },
                isSelectionModeEnabled = mutableStateOf(false),
                isItemSelected = mutableStateOf(false),
                onLongClick = { -> },
            ),
            LinkUIComponentParam(
                link = Link(
                    title = "A Plague Tale: Requiem | Download and Buy Today - Epic Games Store",
                    baseURL = "store.epicgames.com",
                    imgURL = listOf(
                        "https://pbs.twimg.com/media/FUPM2TrWYAAQsXm?format=jpg",
                        "https://pbs.twimg.com/media/FLJx9epWYAADM0O?format=jpg",
                        "https://pbs.twimg.com/media/FAdLIY8WUAEgLRM?format=jpg",
                        "https://pbs.twimg.com/media/ETUI-RDWsAE2UYR?format=jpg",
                        "https://pbs.twimg.com/media/ET9J7vTWsAYVtvG?format=jpg",
                        "https://pbs.twimg.com/media/GRo2CKkWUAEsdEl?format=jpg",
                        "https://pbs.twimg.com/media/FezZxQYWQAQ4K3f?format=jpg",
                        "https://pbs.twimg.com/media/FezaHWkX0AIWvvU?format=jpg",
                        "https://i.redd.it/qoa6gk4ii8571.jpg",
                        "https://i.redd.it/8psapajhi8571.jpg"
                    ).random(),
                    url = "https://store.epicgames.com/en-US/p/a-plague-tale-requiem",
                    userAgent = AppPreferences.primaryJsoupUserAgent.value,
                    linkType = LinkType.SAVED_LINK,
                    localId = 0L,
                    note = "The plague ravages the Kingdom of France. Amicia and her younger brother Hugo are pursued by the Inquisition through villages devastated by the disease.",
                    idOfLinkedFolder = null
                ),
                onMoreIconClick = { -> },
                onLinkClick = { ->
                    localUriHandler.openUri("https://store.epicgames.com/en-US/p/a-plague-tale-requiem")
                },
                onForceOpenInExternalBrowserClicked = { -> },
                isSelectionModeEnabled = mutableStateOf(false),
                isItemSelected = mutableStateOf(false),
                onLongClick = { -> },
            ),
            LinkUIComponentParam(
                link = Link(
                    title = "Shadow of the Tomb Raider",
                    imgURL = "https://images.ctfassets.net/x77ixfmkpoiv/4UnPNfdN8Yq2aZvOhIdBx9/1b641d296ebb37bfa3eca8873c25a321/SOTTR_Product_Image.jpg",
                    url = "https://www.tombraider.com/products/games/shadow-of-the-tomb-raider",
                    userAgent = AppPreferences.primaryJsoupUserAgent.value,
                    linkType = LinkType.SAVED_LINK,
                    localId = 0L,
                    note = "As Lara Croft races to save the world from a Maya apocalypse, she must become the Tomb Raider she is destined to be.",
                    idOfLinkedFolder = null
                ),
                onMoreIconClick = { -> },
                onLinkClick = { ->
                    localUriHandler.openUri("https://www.tombraider.com/products/games/shadow-of-the-tomb-raider")
                },
                onForceOpenInExternalBrowserClicked = { -> },
                isSelectionModeEnabled = mutableStateOf(false),
                isItemSelected = mutableStateOf(false),
                onLongClick = { -> },
            ),
            LinkUIComponentParam(
                link = Link(
                    title = "Nas | Spotify",
                    baseURL = "open.spotify.com",
                    imgURL = "https://i.scdn.co/image/ab6761610000e5eb153198caeef9e3bda92f9285",
                    url = "https://open.spotify.com/artist/20qISvAhX20dpIbOOzGK3q",
                    userAgent = AppPreferences.primaryJsoupUserAgent.value,
                    linkType = LinkType.SAVED_LINK,
                    localId = 0L,
                    note = "half man, half amazing.",
                    idOfLinkedFolder = null
                ),
                onForceOpenInExternalBrowserClicked = { -> },
                isSelectionModeEnabled = mutableStateOf(false),
                isItemSelected = mutableStateOf(false),
                onLongClick = { -> }, onMoreIconClick = { -> },
                onLinkClick = { ->
                    localUriHandler.openUri("https://open.spotify.com/artist/20qISvAhX20dpIbOOzGK3q")
                },
            ),
            LinkUIComponentParam(
                link = Link(
                    title = "Photos From 2024",
                    baseURL = "reddit.com",
                    imgURL = "https://i.redd.it/j14an1zv6aae1.jpg",
                    url = "https://www.reddit.com/r/nas/comments/1hqsamj/photos_from_2024/",
                    userAgent = AppPreferences.primaryJsoupUserAgent.value,
                    linkType = LinkType.SAVED_LINK,
                    localId = 0L,
                    note = "",
                    idOfLinkedFolder = null
                ),
                onForceOpenInExternalBrowserClicked = { -> },
                isSelectionModeEnabled = mutableStateOf(false),
                isItemSelected = mutableStateOf(false),
                onLongClick = { -> }, onMoreIconClick = { -> },
                onLinkClick = { ->
                    localUriHandler.openUri("https://www.reddit.com/r/nas/comments/1hqsamj/photos_from_2024/")
                },
            ),
            LinkUIComponentParam(
                link = Link(
                    title = "Hacker (small type)",
                    baseURL = "twitter.com",
                    imgURL = "https://pbs.twimg.com/media/GT7RIrWWwAAjZzg.jpg",
                    url = "https://twitter.com/CatWorkers/status/1819121250226127061",
                    userAgent = AppPreferences.primaryJsoupUserAgent.value,
                    linkType = LinkType.SAVED_LINK,
                    localId = 0L,
                    note = "",
                    idOfLinkedFolder = null
                ),
                onForceOpenInExternalBrowserClicked = { -> },
                isSelectionModeEnabled = mutableStateOf(false),
                isItemSelected = mutableStateOf(false),
                onLongClick = { -> }, onMoreIconClick = { -> },
                onLinkClick = { ->
                    localUriHandler.openUri("https://twitter.com/CatWorkers/status/1819121250226127061")
                },
            ),
            LinkUIComponentParam(
                link = Link(
                    title = "Nas - You're da Man (from Made You Look: God's Son Live)",
                    baseURL = "youtube.com",
                    imgURL = "https://i.ytimg.com/vi/3vlqI5TPVjQ/maxresdefault.jpg",
                    url = "https://www.youtube.com/watch?v=3vlqI5TPVjQ",
                    userAgent = AppPreferences.primaryJsoupUserAgent.value,
                    linkType = LinkType.SAVED_LINK,
                    localId = 0L,
                    note = "",
                    idOfLinkedFolder = null,
                    mediaType = MediaType.VIDEO
                ),
                onForceOpenInExternalBrowserClicked = { -> },
                isSelectionModeEnabled = mutableStateOf(false),
                isItemSelected = mutableStateOf(false),
                onLongClick = { -> }, onMoreIconClick = { -> },
                onLinkClick = { ->
                    localUriHandler.openUri("https://www.youtube.com/watch?v=3vlqI5TPVjQ")
                },
            ),
            LinkUIComponentParam(
                link = Link(
                    title = "Nas - Mastermind (from Made You Look: God's Son Live)",
                    baseURL = "youtube.com",
                    imgURL = "https://i.ytimg.com/vi/scCey_wI46w/maxresdefault.jpg",
                    url = "https://www.youtube.com/watch?v=scCey_wI46w",
                    userAgent = AppPreferences.primaryJsoupUserAgent.value,
                    linkType = LinkType.SAVED_LINK,
                    localId = 0L,
                    note = "",
                    idOfLinkedFolder = null,
                    mediaType = MediaType.VIDEO
                ),
                onForceOpenInExternalBrowserClicked = { -> },
                isSelectionModeEnabled = mutableStateOf(false),
                isItemSelected = mutableStateOf(false),
                onLongClick = { -> }, onMoreIconClick = { -> },
                onLinkClick = { ->
                    localUriHandler.openUri("https://www.youtube.com/watch?v=scCey_wI46w")
                },
            ),
            LinkUIComponentParam(
                link = Link(
                    title = "Nas - Rare (Official Video)",
                    baseURL = "youtube.com",
                    imgURL = "https://i.ytimg.com/vi/66OFYWBrg3o/maxresdefault.jpg",
                    url = "https://www.youtube.com/watch?v=66OFYWBrg3o",
                    userAgent = AppPreferences.primaryJsoupUserAgent.value,
                    linkType = LinkType.SAVED_LINK,
                    localId = 0L,
                    note = "",
                    idOfLinkedFolder = null,
                    mediaType = MediaType.VIDEO
                ),
                onForceOpenInExternalBrowserClicked = { -> },
                isSelectionModeEnabled = mutableStateOf(false),
                isItemSelected = mutableStateOf(false),
                onLongClick = { -> }, onMoreIconClick = { -> },
                onLinkClick = { ->
                    localUriHandler.openUri("https://www.youtube.com/watch?v=66OFYWBrg3o")
                },
            ),
            LinkUIComponentParam(
                link = Link(
                    title = "Resonance: A Plague Tale Legacy on Steam",
                    baseURL = "store.steampowered.com",
                    imgURL = "https://shared.fastly.steamstatic.com/store_item_assets/steam/apps/2713000/56f8a90aaf16302209dd98ff4b67f11c839582ad/capsule_616x353.jpg",
                    url = "https://store.steampowered.com/app/2713000/Resonance_A_Plague_Tale_Legacy/",
                    userAgent = AppPreferences.primaryJsoupUserAgent.value,
                    linkType = LinkType.SAVED_LINK,
                    localId = 0L,
                    note = "Embark on an original story, prequel to the award-winning games: A Plague Tale. As Sophia, journey to the Minotaur’s Island, outsmart deadly foes, unravel ancient secrets, and confront a mythical creature at the heart of a devastating curse.",
                    idOfLinkedFolder = null,
                    mediaType = MediaType.IMAGE
                ),
                onForceOpenInExternalBrowserClicked = { -> },
                isSelectionModeEnabled = mutableStateOf(false),
                isItemSelected = mutableStateOf(false),
                onLongClick = { -> }, onMoreIconClick = { -> },
                onLinkClick = { ->
                    localUriHandler.openUri("https://store.steampowered.com/app/2713000/Resonance_A_Plague_Tale_Legacy/")
                },
            ),
        ).sortedBy {
            it.link.title
        }
    }

    val nonListViewPref = remember {
        listOf(
            LinkPref(
                onClick = {
                    AppPreferences.enableBorderForNonListViews.value =
                        !AppPreferences.enableBorderForNonListViews.value
                    settingsScreenViewModel.changeSettingPreferenceValue(
                        preferenceKey = booleanPreferencesKey(AppPreferenceType.BORDER_VISIBILITY_FOR_NON_LIST_VIEWS.name),
                        newValue = AppPreferences.enableBorderForNonListViews.value
                    )
                },
                title = Localization.Key.ShowBorderAroundLinks.getLocalizedString(),
                isSwitchChecked = AppPreferences.enableBorderForNonListViews
            ),
            LinkPref(
                onClick = {
                    AppPreferences.enableTitleForNonListViews.value =
                        !AppPreferences.enableTitleForNonListViews.value
                    settingsScreenViewModel.changeSettingPreferenceValue(
                        preferenceKey = booleanPreferencesKey(AppPreferenceType.TITLE_VISIBILITY_FOR_NON_LIST_VIEWS.name),
                        newValue = AppPreferences.enableTitleForNonListViews.value
                    )
                },
                title = Localization.Key.ShowTitle.getLocalizedString(),
                isSwitchChecked = AppPreferences.enableTitleForNonListViews
            ),
            LinkPref(
                onClick = {
                    AppPreferences.enableBaseURLForLinkViews.value =
                        !AppPreferences.enableBaseURLForLinkViews.value
                    settingsScreenViewModel.changeSettingPreferenceValue(
                        preferenceKey = booleanPreferencesKey(AppPreferenceType.BASE_URL_VISIBILITY_FOR_NON_LIST_VIEWS.name),
                        newValue = AppPreferences.enableBaseURLForLinkViews.value
                    )
                },
                title = Localization.Key.ShowHostAddress.getLocalizedString(),
                isSwitchChecked = AppPreferences.enableBaseURLForLinkViews
            ),
            LinkPref(
                onClick = {
                    AppPreferences.enableFadedEdgeForNonListViews.value =
                        !AppPreferences.enableFadedEdgeForNonListViews.value
                    settingsScreenViewModel.changeSettingPreferenceValue(
                        preferenceKey = booleanPreferencesKey(AppPreferenceType.FADED_EDGE_VISIBILITY_FOR_NON_LIST_VIEWS.name),
                        newValue = AppPreferences.enableFadedEdgeForNonListViews.value
                    )
                },
                title = Localization.Key.ShowBottomFadedEdge.getLocalizedString(),
                isSwitchChecked = AppPreferences.enableFadedEdgeForNonListViews
            ),
            LinkPref(
                onClick = {
                    AppPreferences.showVideoTagOnUIIfApplicable.value =
                        !AppPreferences.showVideoTagOnUIIfApplicable.value
                    settingsScreenViewModel.changeSettingPreferenceValue(
                        preferenceKey = booleanPreferencesKey(AppPreferenceType.SHOW_VIDEO_TAG_IF_APPLICABLE.name),
                        newValue = AppPreferences.showVideoTagOnUIIfApplicable.value
                    )
                },
                title = Localization.Key.ShowVideoTagOnUIIfApplicable.getLocalizedString(),
                isSwitchChecked = AppPreferences.showVideoTagOnUIIfApplicable
            ),
        )
    }
    SettingsSectionScaffold(
        topAppBarText = Localization.Key.LinkLayoutSettings.rememberLocalizedString(),
        navController = navController
    ) { paddingValues, topAppBarScrollBehaviour ->
        when (AppPreferences.currentlySelectedLinkLayout.value) {
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
                                    AppPreferences.enableBaseURLForLinkViews.value =
                                        !AppPreferences.enableBaseURLForLinkViews.value
                                    settingsScreenViewModel.changeSettingPreferenceValue(
                                        preferenceKey = booleanPreferencesKey(AppPreferenceType.BASE_URL_VISIBILITY_FOR_NON_LIST_VIEWS.name),
                                        newValue = AppPreferences.enableBaseURLForLinkViews.value
                                    )
                                },
                                title = Localization.Key.ShowHostAddress.getLocalizedString(),
                                isSwitchChecked = AppPreferences.enableBaseURLForLinkViews.value
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
                        LinkListItemComposable(
                            linkUIComponentParam = it,
                            forTitleOnlyView = AppPreferences.currentlySelectedLinkLayout.value == Layout.TITLE_ONLY_LIST_VIEW.name
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
                    modifier = Modifier.padding(start = 10.dp, end = 10.dp)
                        .addEdgeToEdgeScaffoldPadding(paddingValues)
                        .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection)
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

                    items(nonListViewPref, span = {
                        GridItemSpan(maxLineSpan)
                    }) {
                        LinkViewPreferenceSwitch(
                            onClick = it.onClick,
                            title = it.title,
                            isSwitchChecked = it.isSwitchChecked.value
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
                        GridViewLinkUIComponent(it, forStaggeredView = false)
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

                    items(items = nonListViewPref, span = { StaggeredGridItemSpan.FullLine }) {
                        LinkViewPreferenceSwitch(
                            onClick = it.onClick,
                            title = it.title,
                            isSwitchChecked = it.isSwitchChecked.value
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
                        GridViewLinkUIComponent(
                            linkUIComponentParam = it, forStaggeredView = true
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
        modifier = Modifier.fillMaxWidth().clickable(onClick = {
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
            checked = isSwitchChecked, onCheckedChange = {
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
        modifier = Modifier.fillMaxWidth().clickable {
            AppPreferences.currentlySelectedLinkLayout.value = linkLayout.name
            settingsScreenViewModel.changeSettingPreferenceValue(
                preferenceKey = stringPreferencesKey(AppPreferenceType.CURRENTLY_SELECTED_LINK_VIEW.name),
                newValue = linkLayout.name
            )
        }.padding(paddingValues)
    ) {
        RadioButton(
            selected = AppPreferences.currentlySelectedLinkLayout.value == linkLayout.name,
            onClick = {
                AppPreferences.currentlySelectedLinkLayout.value = linkLayout.name
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