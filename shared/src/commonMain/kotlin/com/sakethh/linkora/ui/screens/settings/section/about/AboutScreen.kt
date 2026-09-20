package com.sakethh.linkora.ui.screens.settings.section.about

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sakethh.linkora.di.linkoraViewModel
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.dto.github.GitHubReleaseDTOItem
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.ui.LocalizedStrings
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.settings.AppVersionLabel
import com.sakethh.linkora.ui.screens.settings.SettingsScreenViewModel
import com.sakethh.linkora.ui.screens.settings.common.composables.SettingsSectionScaffold
import com.sakethh.linkora.ui.utils.rememberDeserializableMutableObject
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.addEdgeToEdgeScaffoldPadding
import com.sakethh.linkora.utils.openUriOrNotify
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen() {
    val localizedStrings = LocalizedStrings.current
    val coroutineScope = rememberCoroutineScope()
    val settingsScreenVM: SettingsScreenViewModel = linkoraViewModel()
    val preferences by settingsScreenVM.preferencesAsFlow.collectAsStateWithLifecycle()
    val btmModalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val shouldVersionCheckerDialogAppear = rememberSaveable {
        mutableStateOf(false)
    }
    val uriHandler = LocalUriHandler.current
    val shouldBtmModalSheetBeVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val aboutSettingsScreenVM: AboutSettingsScreenVM = linkoraViewModel()
    val retrievedAppVersionData = rememberDeserializableMutableObject {
        mutableStateOf(
            GitHubReleaseDTOItem(
                assets = listOf(),
                body = "",
                createdAt = "",
                releasePageURL = "",
                releaseName = "",
                tagName = "",
            ),
        )
    }
    var isOnLatestUpdate by rememberSaveable {
        mutableStateOf(false)
    }
    SettingsSectionScaffold(topAppBarText = localizedStrings.About) {
            paddingValues,
            topAppBarScrollBehaviour,
        ->
        LazyColumn(
            modifier =
                Modifier.fillMaxSize()
                    .addEdgeToEdgeScaffoldPadding(paddingValues)
                    .nestedScroll(topAppBarScrollBehaviour.nestedScrollConnection),
        ) {
            item {
                Spacer(Modifier.height(30.dp))
            }
            item {
                AppVersionLabel()
            }
            item {
                if (!isOnLatestUpdate) {
                    SettingsAppInfoComponent(
                        hasDescription = false,
                        description = "",
                        icon = Icons.Default.Refresh,
                        title = localizedStrings.CheckForLatestVersion,
                        onClick = {
                            aboutSettingsScreenVM.retrieveLatestVersionData(
                                onLoading = {
                                    shouldVersionCheckerDialogAppear.value = true
                                },
                                onCompletion = { githubReleaseDTO ->
                                    if (githubReleaseDTO == null) {
                                        retrievedAppVersionData.value =
                                            GitHubReleaseDTOItem(
                                                assets = listOf(),
                                                body = "",
                                                createdAt = "",
                                                releasePageURL = "",
                                                releaseName = "",
                                                tagName = "",
                                            )
                                        isOnLatestUpdate = false
                                    } else {
                                        retrievedAppVersionData.value = githubReleaseDTO
                                        isOnLatestUpdate =
                                            githubReleaseDTO.releaseName != Constants.APP_VERSION_NAME
                                        isOnLatestUpdate =
                                            Constants.APP_VERSION_NAME == githubReleaseDTO.releaseName
                                    }
                                    shouldVersionCheckerDialogAppear.value = false
                                    shouldBtmModalSheetBeVisible.value =
                                        isOnLatestUpdate.not() &&
                                                githubReleaseDTO != null
                                },
                            )
                        },
                    )
                } else if (isOnLatestUpdate) {
                    Card(
                        border =
                            BorderStroke(
                                1.dp,
                                contentColorFor(MaterialTheme.colorScheme.surface),
                            ),
                        colors = CardDefaults.cardColors(containerColor = AlertDialogDefaults.containerColor),
                        modifier = Modifier.fillMaxWidth()
                            .padding(start = 15.dp, end = 15.dp, top = 15.dp),
                    ) {
                        Row(
                            modifier =
                                Modifier.fillMaxWidth()
                                    .wrapContentHeight()
                                    .padding(
                                        top = 10.dp,
                                        bottom = 10.dp,
                                    ),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(contentAlignment = Alignment.CenterStart) {
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    modifier =
                                        Modifier.padding(
                                            start = 10.dp,
                                            end = 10.dp,
                                        ),
                                )
                            }
                            Text(
                                text =
                                    localizedStrings.YouAreUsingLatestVersionOfLinkora,
                                style = MaterialTheme.typography.titleSmall,
                                fontSize = 14.sp,
                                lineHeight = 18.sp,
                                textAlign = TextAlign.Start,
                                modifier = Modifier.padding(end = 15.dp),
                            )
                        }
                    }
                }
            }
            item {
                HorizontalDivider(
                    modifier = Modifier.padding(20.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            item {
                Text(
                    text = localizedStrings.Socials,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 15.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            item {
                SettingsAppInfoComponent(
                    hasDescription = false,
                    description = "",
                    icon = Icons.Default.Web,
                    title = localizedStrings.Twitter,
                    onClick = {
                        val url = "https://www.twitter.com/LinkoraApp"
                        aboutSettingsScreenVM.addANewLinkToHistory(
                            link =
                                Link(
                                    linkType = LinkType.HISTORY_LINK,
                                    title = localizedStrings.LinkoraOnTwitter,
                                    url = url,
                                    imgURL = "",
                                    note = localizedStrings.LinkoraOnTwitter,
                                    idOfLinkedFolder = null,
                                    userAgent = preferences.primaryJsoupUserAgent,
                                ),
                            tagIds = emptyList(),
                            useProxy = preferences.useProxy,
                            skipSavingIfExists = preferences.skipSavingExistingLink,
                            forceSaveIfRetrievalFails = preferences.forceSaveIfRetrievalFails,
                        )
                        coroutineScope.launch {
                            uriHandler.openUriOrNotify(url, localizedStrings)
                        }
                    },
                )
            }
            item {
                SettingsAppInfoComponent(
                    hasDescription = false,
                    description = "",
                    icon = Icons.Default.Construction,
                    title = localizedStrings.Discord,
                    onClick = {
                        val url = "https://discord.gg/ZDBXNtv8MD"
                        aboutSettingsScreenVM.addANewLinkToHistory(
                            link =
                                Link(
                                    linkType = LinkType.HISTORY_LINK,
                                    title = localizedStrings.LinkoraOnDiscord,
                                    url = url,
                                    imgURL = "https://cdn.discordapp.com/assets/og_img_discord_home.png",
                                    note = localizedStrings.LinkoraOnDiscord,
                                    idOfLinkedFolder = null,
                                    userAgent = preferences.primaryJsoupUserAgent,
                                ),
                            tagIds = emptyList(),
                            useProxy = preferences.useProxy,
                            skipSavingIfExists = preferences.skipSavingExistingLink,
                            forceSaveIfRetrievalFails = preferences.forceSaveIfRetrievalFails,
                        )
                        coroutineScope.launch {
                            uriHandler.openUriOrNotify(url, localizedStrings)
                        }
                    },
                )
            }
            item {
                HorizontalDivider(
                    modifier =
                        Modifier.padding(
                            start = 20.dp,
                            top = 20.dp,
                            end = 20.dp,
                        ),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline,
                )
            }
            item {
                Text(
                    text = localizedStrings.Development,
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 15.dp, bottom = 15.dp, top = 20.dp),
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            item {
                SettingsAppInfoComponent(
                    hasDescription = true,
                    description = localizedStrings.GithubDesc,
                    icon = Icons.Default.Construction,
                    title = localizedStrings.Github,
                    onClick = {
                        val url = "https://www.github.com/LinkoraApp"
                        aboutSettingsScreenVM.addANewLinkToHistory(
                            link =
                                Link(
                                    linkType = LinkType.HISTORY_LINK,
                                    title = localizedStrings.LinkoraOnGithub,
                                    url = url,
                                    imgURL = "https://avatars.githubusercontent.com/u/183308434?s=280&v=4",
                                    note = localizedStrings.LinkoraOnGithub,
                                    idOfLinkedFolder = null,
                                    userAgent = preferences.primaryJsoupUserAgent,
                                ),
                            tagIds = emptyList(),
                            useProxy = preferences.useProxy,
                            skipSavingIfExists = preferences.skipSavingExistingLink,
                            forceSaveIfRetrievalFails = preferences.forceSaveIfRetrievalFails,
                        )
                        coroutineScope.launch {
                            uriHandler.openUriOrNotify(url, localizedStrings)
                        }
                    },
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(20.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline,
                )
                SettingsAppInfoComponent(
                    hasDescription = true,
                    description =
                        localizedStrings.HaveASuggestionCreateAnIssueOnGithubToImproveLinkora,
                    icon = Icons.Default.Construction,
                    title = localizedStrings.OpenAGithubIssue,
                    onClick = {
                        val url = "https://github.com/sakethpathike/Linkora/issues/new"
                        aboutSettingsScreenVM.addANewLinkToHistory(
                            link =
                                Link(
                                    linkType = LinkType.HISTORY_LINK,
                                    title = localizedStrings.LinkoraIssues,
                                    url = url,
                                    imgURL =
                                        "https://repository-images.githubusercontent.com/648784316/df5ac80f-8d5a-4d8d-b7b5-6068ee49eb4b",
                                    note = localizedStrings.LinkoraIssuesOnGithub,
                                    idOfLinkedFolder = null,
                                    userAgent = preferences.primaryJsoupUserAgent,
                                ),
                            tagIds = emptyList(),
                            useProxy = preferences.useProxy,
                            skipSavingIfExists = preferences.skipSavingExistingLink,
                            forceSaveIfRetrievalFails = preferences.forceSaveIfRetrievalFails,
                        )
                        coroutineScope.launch {
                            uriHandler.openUriOrNotify(url, localizedStrings)
                        }
                    },
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(20.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline,
                )

                SettingsAppInfoComponent(
                    hasDescription = true,
                    description =
                        localizedStrings.TrackRecentChangesAndUpdatesToLinkora,
                    icon = Icons.Default.TrackChanges,
                    title = localizedStrings.Changelog,
                    onClick = {
                        val url = "https://github.com/sakethpathike/Linkora/releases"
                        aboutSettingsScreenVM.addANewLinkToHistory(
                            link =
                                Link(
                                    linkType = LinkType.HISTORY_LINK,
                                    title = localizedStrings.LinokraReleases,
                                    url = url,
                                    imgURL =
                                        "https://repository-images.githubusercontent.com/648784316/df5ac80f-8d5a-4d8d-b7b5-6068ee49eb4b",
                                    note = localizedStrings.LinokraReleasesOnGitHub,
                                    idOfLinkedFolder = null,
                                    userAgent = preferences.primaryJsoupUserAgent,
                                ),
                            tagIds = emptyList(),
                            useProxy = preferences.useProxy,
                            skipSavingIfExists = preferences.skipSavingExistingLink,
                            forceSaveIfRetrievalFails = preferences.forceSaveIfRetrievalFails,
                        )
                        coroutineScope.launch {
                            uriHandler.openUriOrNotify(url, localizedStrings)
                        }
                    },
                )
            }

            item {
                HorizontalDivider(
                    modifier = Modifier.padding(20.dp),
                    thickness = 0.5.dp,
                    color = MaterialTheme.colorScheme.outline,
                )
                Spacer(modifier = Modifier.height(100.dp))
            }
        }
        LodingDialogBox(
            shouldDialogBoxAppear = shouldVersionCheckerDialogAppear,
            text = localizedStrings.RetrievingLatestInformation,
        )
        if (shouldBtmModalSheetBeVisible.value) {
            ModalBottomSheet(
                sheetState = btmModalSheetState,
                onDismissRequest = {
                    coroutineScope
                        .launch {
                            if (btmModalSheetState.isVisible) {
                                btmModalSheetState.hide()
                            }
                        }
                        .invokeOnCompletion {
                            shouldBtmModalSheetBeVisible.value = false
                        }
                },
            ) {
                NewVersionUpdateBtmContent(
                    shouldBtmModalSheetBeVisible = shouldBtmModalSheetBeVisible,
                    modalBtmSheetState = btmModalSheetState,
                    latestVersion = retrievedAppVersionData.value.releaseName,
                    urlOfLatestReleasePage = retrievedAppVersionData.value.releasePageURL,
                    tagName = retrievedAppVersionData.value.tagName,
                )
            }
        }
    }
}
