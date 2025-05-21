package com.sakethh.linkora.ui.screens.settings.section.about

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Construction
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Web
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialogDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.contentColorFor
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sakethh.linkora.common.DependencyContainer
import com.sakethh.linkora.common.Localization
import com.sakethh.linkora.common.preferences.AppPreferences
import com.sakethh.linkora.common.utils.Constants
import com.sakethh.linkora.common.utils.isNull
import com.sakethh.linkora.common.utils.rememberLocalizedString
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.dto.github.GitHubReleaseDTOItem
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingsSectionScaffold
import com.sakethh.linkora.ui.utils.genericViewModelFactory
import com.sakethh.linkora.ui.utils.rememberDeserializableMutableObject
import kotlinx.coroutines.launch
import linkora.composeapp.generated.resources.Res
import linkora.composeapp.generated.resources.made_with_img
import org.jetbrains.compose.resources.painterResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutSettingsScreen() {
    val navController = LocalNavController.current
    val coroutineScope = rememberCoroutineScope()
    val btmModalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val shouldVersionCheckerDialogAppear = rememberSaveable {
        mutableStateOf(false)
    }
    val uriHandler = LocalUriHandler.current
    val shouldBtmModalSheetBeVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val aboutSettingsScreenVM: AboutSettingsScreenVM = viewModel(factory = genericViewModelFactory {
        AboutSettingsScreenVM(
            localLinksRepo = DependencyContainer.localLinksRepo.value,
            gitHubReleasesRepo = DependencyContainer.gitHubReleasesRepo.value
        )
    })
    val retrievedAppVersionData = rememberDeserializableMutableObject {
        mutableStateOf(
            GitHubReleaseDTOItem(
                assets = listOf(),
                body = "",
                createdAt = "",
                releasePageURL = "",
                releaseName = "",
                tagName = ""
            )
        )
    }
    SettingsSectionScaffold(
        topAppBarText = Navigation.Settings.AboutSettingsScreen.toString(),
        navController = navController
    ) { paddingValues, topAppBarScrollBehaviour ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues)
                .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection)
                .navigationBarsPadding()
        ) {
            item {
                Spacer(Modifier.height(30.dp))
            }
            item {
                Row {
                    Text(
                        text = Localization.Key.Linkora.rememberLocalizedString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontSize = 18.sp,
                        modifier = Modifier.padding(start = 15.dp).alignByBaseline()
                    )
                    Text(
                        text = Constants.APP_VERSION_NAME,
                        style = MaterialTheme.typography.titleSmall,
                        fontSize = 12.sp,
                        modifier = Modifier.alignByBaseline()
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 10.dp, start = 15.dp)
                ) {
                    Text(
                        text = "built with",
                        fontSize = 12.sp,
                        style = MaterialTheme.typography.labelSmall
                    )
                    Spacer(modifier = Modifier.width(5.dp))
                    Image(
                        painter = painterResource(Res.drawable.made_with_img),
                        contentDescription = null,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            item {
                if (!AppPreferences.isOnLatestUpdate.value) {
                    SettingsAppInfoComponent(
                        hasDescription = false,
                        description = "",
                        icon = Icons.Default.Refresh,
                        title = Localization.Key.CheckForLatestVersion.rememberLocalizedString(),
                        onClick = {
                            aboutSettingsScreenVM.retrieveLatestVersionData(onLoading = {
                                shouldVersionCheckerDialogAppear.value = true
                            }, onCompletion = { githubReleaseDTO ->
                                if (githubReleaseDTO.isNull()) {
                                    retrievedAppVersionData.value = GitHubReleaseDTOItem(
                                        assets = listOf(),
                                        body = "",
                                        createdAt = "",
                                        releasePageURL = "",
                                        releaseName = "",
                                        tagName = ""
                                    )
                                    AppPreferences.isOnLatestUpdate.value = false
                                } else {
                                    retrievedAppVersionData.value = githubReleaseDTO!!
                                    AppPreferences.isOnLatestUpdate.value =
                                        githubReleaseDTO.releaseName != Constants.APP_VERSION_NAME
                                    AppPreferences.isOnLatestUpdate.value =
                                        Constants.APP_VERSION_NAME == githubReleaseDTO.releaseName
                                }
                                shouldVersionCheckerDialogAppear.value = false
                                shouldBtmModalSheetBeVisible.value =
                                    AppPreferences.isOnLatestUpdate.value.not() && githubReleaseDTO != null
                            })
                        },
                    )
                } else if (AppPreferences.isOnLatestUpdate.value) {
                    Card(
                        border = BorderStroke(
                            1.dp, contentColorFor(MaterialTheme.colorScheme.surface)
                        ),
                        colors = CardDefaults.cardColors(containerColor = AlertDialogDefaults.containerColor),
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 15.dp, end = 15.dp, top = 15.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().wrapContentHeight().padding(
                                top = 10.dp, bottom = 10.dp
                            ), verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    modifier = Modifier.padding(
                                        start = 10.dp, end = 10.dp
                                    )
                                )
                            }
                            Text(
                                text = Localization.Key.YouAreUsingLatestVersionOfLinkora.rememberLocalizedString(),
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 14.sp,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.padding(end = 15.dp)
                            )
                        }
                    }
                }
            }
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(20.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline
                )
            }
            item {
                Text(
                    text = Localization.Key.Socials.rememberLocalizedString(),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 15.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }
            item {
                SettingsAppInfoComponent(
                    hasDescription = false,
                    description = "",
                    icon = Icons.Default.Web,
                    title = Localization.Key.Twitter.rememberLocalizedString(),
                    onClick = {
                        val url = "https://www.twitter.com/LinkoraApp"
                        aboutSettingsScreenVM.addANewLinkToHistory(
                            Link(
                                linkType = LinkType.HISTORY_LINK,
                                title = "Linkora on Twitter",
                                url = url,
                                imgURL = "",
                                note = "Linkora on Twitter",
                                idOfLinkedFolder = null,
                                userAgent = AppPreferences.primaryJsoupUserAgent.value
                            )
                        )
                        uriHandler.openUri(url)
                    },
                )
            }/*HorizontalDivider(
                modifier = Modifier.padding(
                    start = 20.dp,
                    top = 20.dp,
                    end = 20.dp
                ),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outline
            )*/
            item {
                SettingsAppInfoComponent(
                    hasDescription = false,
                    description = "",
                    icon = Icons.Default.Construction,
                    title = Localization.Key.Discord.rememberLocalizedString(),
                    onClick = {
                        val url = "https://discord.gg/ZDBXNtv8MD"
                        aboutSettingsScreenVM.addANewLinkToHistory(
                            Link(
                                linkType = LinkType.HISTORY_LINK,
                                title = "Linkora on Discord",
                                url = url,
                                imgURL = "https://cdn.discordapp.com/assets/og_img_discord_home.png",
                                note = "Linkora on Discord",
                                idOfLinkedFolder = null,
                                userAgent = AppPreferences.primaryJsoupUserAgent.value
                            )
                        )
                        uriHandler.openUri(url)
                    },
                )
            }
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(
                        start = 20.dp, top = 20.dp, end = 20.dp
                    ), thickness = 0.5.dp, color = MaterialTheme.colorScheme.outline
                )
            }
            item {
                Text(
                    text = Localization.Key.Development.rememberLocalizedString(),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 15.dp, bottom = 15.dp, top = 20.dp),
                    color = MaterialTheme.colorScheme.primary
                )
            }

            item {
                SettingsAppInfoComponent(
                    hasDescription = true,
                    description = Localization.Key.GithubDesc.rememberLocalizedString(),
                    icon = Icons.Default.Construction,
                    title = Localization.Key.Github.rememberLocalizedString(),
                    onClick = {
                        val url = "https://www.github.com/LinkoraApp"
                        aboutSettingsScreenVM.addANewLinkToHistory(
                            Link(
                                linkType = LinkType.HISTORY_LINK,
                                title = "Linkora on Github",
                                url = url,
                                imgURL = "https://avatars.githubusercontent.com/u/183308434?s=280&v=4",
                                note = "Linkora on Github",
                                idOfLinkedFolder = null,
                                userAgent = AppPreferences.primaryJsoupUserAgent.value
                            )
                        )
                        uriHandler.openUri(url)
                    },
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(20.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline
                )
                SettingsAppInfoComponent(
                    hasDescription = true,
                    description = Localization.Key.HaveASuggestionCreateAnIssueOnGithubToImproveLinkora.rememberLocalizedString(),
                    icon = Icons.Default.Construction,
                    title = Localization.Key.OpenAGithubIssue.rememberLocalizedString(),
                    onClick = {
                        val url = "https://github.com/sakethpathike/Linkora/issues/new"
                        aboutSettingsScreenVM.addANewLinkToHistory(
                            Link(
                                linkType = LinkType.HISTORY_LINK,
                                title = "Issues · sakethpathike/Linkora",
                                url = url,
                                imgURL = "https://repository-images.githubusercontent.com/648784316/df5ac80f-8d5a-4d8d-b7b5-6068ee49eb4b",
                                note = "Issues · sakethpathike/Linkora on Github",
                                idOfLinkedFolder = null,
                                userAgent = AppPreferences.primaryJsoupUserAgent.value
                            )
                        )
                        uriHandler.openUri(url)
                    },
                )
            }

            item {

                HorizontalDivider(
                    modifier = Modifier.padding(20.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline
                )

                SettingsAppInfoComponent(
                    hasDescription = true,
                    description = Localization.Key.TrackRecentChangesAndUpdatesToLinkora.rememberLocalizedString(),
                    icon = Icons.Default.TrackChanges,
                    title = Localization.Key.Changelog.rememberLocalizedString(),
                    onClick = {
                        val url = "https://github.com/sakethpathike/Linkora/releases"
                        aboutSettingsScreenVM.addANewLinkToHistory(
                            Link(
                                linkType = LinkType.HISTORY_LINK,
                                title = "Releases · sakethpathike/Linkora",
                                url = url,
                                imgURL = "https://repository-images.githubusercontent.com/648784316/df5ac80f-8d5a-4d8d-b7b5-6068ee49eb4b",
                                note = "Releases · sakethpathike/Linkora on Github",
                                idOfLinkedFolder = null,
                                userAgent = AppPreferences.primaryJsoupUserAgent.value
                            )
                        )
                        uriHandler.openUri(url)
                    },
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(20.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline
                )
                Spacer(modifier = Modifier.height(100.dp))
            }

        }
        LodingDialogBox(
            shouldDialogBoxAppear = shouldVersionCheckerDialogAppear,
            text = Localization.Key.RetrievingLatestInformation.rememberLocalizedString()
        )
        if (shouldBtmModalSheetBeVisible.value) {
            ModalBottomSheet(sheetState = btmModalSheetState, onDismissRequest = {
                coroutineScope.launch {
                    if (btmModalSheetState.isVisible) {
                        btmModalSheetState.hide()
                    }
                }.invokeOnCompletion {
                    shouldBtmModalSheetBeVisible.value = false
                }
            }) {
                NewVersionUpdateBtmContent(
                    shouldBtmModalSheetBeVisible = shouldBtmModalSheetBeVisible,
                    modalBtmSheetState = btmModalSheetState,
                    latestVersion = retrievedAppVersionData.value.releaseName,
                    urlOfLatestReleasePage = retrievedAppVersionData.value.releasePageURL,
                    tagName = retrievedAppVersionData.value.tagName
                )
            }
        }
    }
}