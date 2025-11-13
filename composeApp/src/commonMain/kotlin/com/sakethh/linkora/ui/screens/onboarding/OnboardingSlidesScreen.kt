package com.sakethh.linkora.ui.screens.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.primaryContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import com.sakethh.linkora.Localization
import com.sakethh.linkora.di.LinkoraSDK
import com.sakethh.linkora.di.linkoraViewModel
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.model.link.Link
import com.sakethh.linkora.domain.model.tag.Tag
import com.sakethh.linkora.platform.platform
import com.sakethh.linkora.preferences.AppPreferences
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.components.folder.FolderComponent
import com.sakethh.linkora.ui.components.link.ListViewLinkComponent
import com.sakethh.linkora.ui.domain.CurrentFABContext
import com.sakethh.linkora.ui.domain.FABContext
import com.sakethh.linkora.ui.domain.model.FolderComponentParam
import com.sakethh.linkora.ui.domain.model.LinkComponentParam
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.collections.components.ItemDivider
import com.sakethh.linkora.ui.screens.settings.SettingsScreenViewModel
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.rememberLocalizedString
import kotlinx.coroutines.launch
import linkora.composeapp.generated.resources.LOLCATpl_logo
import linkora.composeapp.generated.resources.Res
import linkora.composeapp.generated.resources.mondstern_logo
import linkora.composeapp.generated.resources.new_logo
import org.jetbrains.compose.resources.painterResource

data class OnboardingSlide(val screen: @Composable () -> Unit)

@Composable
fun OnboardingSlidesScreen(
    onOnboardingComplete: () -> Unit, currentFABContext: (CurrentFABContext) -> Unit
) {
    LaunchedEffect(Unit) {
        currentFABContext(CurrentFABContext(fabContext = FABContext.HIDE))
    }
    val pagerState = rememberPagerState { 4 }
    val coroutineScope = rememberCoroutineScope()
    val navController = LocalNavController.current
    val settingsScreenViewModel: SettingsScreenViewModel = linkoraViewModel()
    val slides = settingsScreenViewModel.onboardingSlides
    Box(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState, modifier = Modifier.fillMaxSize()
        ) {
            Box(
                modifier = Modifier.background(MaterialTheme.colorScheme.surface).fillMaxSize()
                    .navigationBarsPadding().graphicsLayer {
                        val pageOffset =
                            (pagerState.currentPage - it) + pagerState.currentPageOffsetFraction
                        val scale = lerp(1f, 2f, pageOffset)
                        scaleX = scale
                        scaleY = scale
                    }) {
                slides[it].screen()
            }
        }
        Box(
            modifier = Modifier.fillMaxWidth().navigationBarsPadding().align(Alignment.BottomEnd),
            contentAlignment = Alignment.CenterEnd
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically, modifier = Modifier.then(
                    if (platform() is Platform.Desktop) Modifier.padding(15.dp) else Modifier.padding(
                        end = 15.dp
                    )
                )
            ) {
                AnimatedVisibility(
                    visible = pagerState.currentPage != 0, enter = fadeIn(), exit = fadeOut()
                ) {
                    FilledTonalButton(
                        modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                            .pressScaleEffect(), onClick = {
                            if (pagerState.isScrollInProgress) return@FilledTonalButton
                            coroutineScope.launch {
                                try {
                                    pagerState.animateScrollToPage(
                                        pagerState.currentPage - 1, animationSpec = tween(750)
                                    )
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            }
                        }) {
                        Text(
                            text = Localization.Key.PreviousPage.rememberLocalizedString(),
                            style = MaterialTheme.typography.titleMedium
                        )
                    }
                }
                Spacer(Modifier.width(15.dp))
                Button(
                    modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                        .pressScaleEffect(), onClick = {
                        if (pagerState.currentPage == pagerState.pageCount - 1) {
                            onOnboardingComplete()
                            navController.navigate(Navigation.Root.HomeScreen) {
                                popUpTo(0)
                            }
                            return@Button
                        }
                        if (pagerState.isScrollInProgress) return@Button
                        coroutineScope.launch {
                            try {
                                pagerState.animateScrollToPage(
                                    pagerState.currentPage + 1, animationSpec = tween(750)
                                )
                            } catch (e: Exception) {
                                e.printStackTrace()
                            }
                        }
                    }) {
                    val text = rememberSaveable(pagerState.currentPage) {
                        if (pagerState.currentPage == pagerState.pageCount - 1) {
                            Localization.Key.Done.getLocalizedString()
                        } else {
                            Localization.Key.NextPage.getLocalizedString()
                        }
                    }
                    AnimatedContent(text, transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    }) {
                        Text(text = it, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun Slide1() {
    Column(
        modifier = Modifier.padding(15.dp).fillMaxSize(),
        verticalArrangement = Arrangement.Bottom,
        horizontalAlignment = Alignment.Start
    ) {
        Image(
            painter = painterResource(
                listOf(
                    Res.drawable.mondstern_logo, Res.drawable.new_logo, Res.drawable.LOLCATpl_logo
                ).random()
            ),
            contentDescription = null,
            modifier = Modifier.clip(RoundedCornerShape(15.dp))
                .sizeIn(maxWidth = 250.dp, maxHeight = 250.dp).wrapContentSize().border(
                    shape = RoundedCornerShape(15.dp),
                    width = 2.dp,
                    brush = Brush.horizontalGradient(
                        listOf(
                            MaterialTheme.colorScheme.primary,
                            MaterialTheme.colorScheme.primaryContainer,
                        )
                    )
                )
        )
        Spacer(modifier = Modifier.height(15.dp))
        SlideTitle(Localization.Key.Linkora.rememberLocalizedString())
        Text(
            text = Localization.Key.AppIntroSlide1Label.rememberLocalizedString(),
            style = MaterialTheme.typography.titleSmall,
            fontSize = 18.sp,
        )
        Spacer(modifier = Modifier.height(15.dp))
        SlideDesc(Localization.Key.AppIntroSlide1SwipeLabel.rememberLocalizedString())
        Spacer(modifier = Modifier.height(75.dp))
    }
}

@Composable
private fun SlideTitle(string: String, modifier: Modifier = Modifier, fontSize: TextUnit = 24.sp) {
    Text(
        text = string,
        style = MaterialTheme.typography.titleLarge,
        fontSize = fontSize,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
    )
}

@Composable
private fun SlideDesc(string: String, modifier: Modifier = Modifier) {
    Text(
        text = string,
        style = MaterialTheme.typography.titleMedium,
        fontSize = 16.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(0.85f),
        modifier = modifier
    )
}

@Composable
fun Slide2() {
    val localUriHandler = LocalUriHandler.current
    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.verticalScroll(rememberScrollState(1)).fillMaxWidth()
                .align(Alignment.BottomCenter)
        ) {
            FolderComponent(
                FolderComponentParam(
                    name = Localization.Key.AppIntroSlide2Folder2Name.rememberLocalizedString(),
                    note = Localization.Key.AppIntroSlide2Folder2Note.rememberLocalizedString(),
                    onClick = {},
                    onLongClick = {},
                    onMoreIconClick = {},
                    isCurrentlyInDetailsView = rememberSaveable {
                        mutableStateOf(false)
                    },
                    showMoreIcon = rememberSaveable {
                        mutableStateOf(true)
                    },
                    isSelectedForSelection = rememberSaveable {
                        mutableStateOf(false)
                    },
                    showCheckBox = rememberSaveable {
                        mutableStateOf(false)
                    },
                    onCheckBoxChanged = {})
            )
            ListViewLinkComponent(
                linkComponentParam = LinkComponentParam(
                    link = Link(
                title = "Red Dead Redemption 2 - Rockstar Games",
                baseURL = "rockstargames.com",
                imgURL = "https://media-rockstargames-com.akamaized.net/rockstargames-newsite/img/global/downloads/buddyiconsconavatars/rdr2_officialart1_256x256.jpg",
                url = "https://www.rockstargames.com/reddeadredemption2",
                userAgent = AppPreferences.primaryJsoupUserAgent.value,
                linkType = LinkType.SAVED_LINK,
                localId = 0L,
                note = "",
                idOfLinkedFolder = null
            ),
                onMoreIconClick = { -> },
                onLinkClick = { ->
                    localUriHandler.openUri("https://www.rockstargames.com/reddeadredemption2")
                },
                onForceOpenInExternalBrowserClicked = { -> },
                isSelectionModeEnabled = rememberSaveable {
                    mutableStateOf(false)
                },
                isItemSelected = rememberSaveable {
                    mutableStateOf(false)
                },
                onLongClick = { -> },
                tags = listOf(
                    Tag(name = "Tahiti"), Tag(name = "AndaquarterDONTFORGETTHEQUARTRR")
                ),
                onTagClick = {}), titleOnlyView = false, onShare = {
                LinkoraSDK.getInstance().nativeUtils.onShare(it)
            })
            ListViewLinkComponent(
                linkComponentParam = LinkComponentParam(
                link = Link(
                title = "Nas | Spotify",
                baseURL = "open.spotify.com",
                imgURL = "https://ucarecdn.com/9b4d5145-a417-4ff9-a7e5-93a452a443c8/-/crop/974x818/26,197/-/preview/",
                url = "https://open.spotify.com/artist/20qISvAhX20dpIbOOzGK3q",
                userAgent = AppPreferences.primaryJsoupUserAgent.value,
                linkType = LinkType.SAVED_LINK,
                localId = 0L,
                note = "",
                idOfLinkedFolder = null
            ),
                onForceOpenInExternalBrowserClicked = { -> },
                isSelectionModeEnabled = rememberSaveable {
                    mutableStateOf(false)
                },
                isItemSelected = rememberSaveable {
                    mutableStateOf(false)
                },
                onLongClick = { -> },
                onMoreIconClick = { -> },
                onLinkClick = { ->
                    localUriHandler.openUri("https://open.spotify.com/artist/20qISvAhX20dpIbOOzGK3q")
                },
                tags = listOf(Tag(name = "half man half amazing")),
                onTagClick = {}),
                titleOnlyView = false,
                imageAlignment = Alignment.TopCenter,
                onShare = {
                    LinkoraSDK.getInstance().nativeUtils.onShare(it)
                })
            Spacer(modifier = Modifier.height(5.dp))
            SlideTitle(
                string = Localization.Key.AppIntroSlide2MainLabel.rememberLocalizedString(),
                modifier = Modifier.padding(start = 15.dp)
            )
            Spacer(modifier = Modifier.height(5.dp))
            SlideDesc(
                modifier = Modifier.padding(start = 15.dp, bottom = 75.dp),
                string = Localization.Key.AppIntroSlide2MainLabelDesc.rememberLocalizedString()
            )
        }
    }
}

@Composable
fun Slide3() {
    val pagerState = rememberPagerState { 2 }
    val localUriHandler = LocalUriHandler.current
    val coroutineScope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxSize(), verticalArrangement = Arrangement.Bottom) {
        Text(
            text = Localization.Key.SelectedPanel.rememberLocalizedString(),
            color = MaterialTheme.colorScheme.primary.copy(0.9f),
            style = MaterialTheme.typography.titleSmall,
            modifier = Modifier.padding(start = 10.dp, bottom = 5.dp)
        )

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.pressScaleEffect().fillMaxWidth().padding(start = 5.dp, end = 5.dp),
        ) {
            Spacer(Modifier.width(5.dp))
            FilledTonalIconButton(onClick = {

            }, modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).size(22.dp)) {
                Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null)
            }
            Spacer(Modifier.width(10.dp))
            Text(
                text = Localization.Key.AppIntroSlide3PanelName.rememberLocalizedString(),
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.titleLarge,
                fontSize = 20.sp,
                modifier = Modifier.fillMaxWidth(0.8f)
            )
        }
        ScrollableTabRow(
            modifier = Modifier.fillMaxWidth(),
            selectedTabIndex = pagerState.currentPage,
            divider = {}) {
            (0..1).forEach {
                key(it) {
                    Tab(
                        modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand),
                        selected = pagerState.currentPage == it,
                        onClick = {
                            coroutineScope.launch {
                                pagerState.animateScrollToPage(it)
                            }.start()
                        }) {
                        Text(
                            text = if (it == 0) Localization.Key.AppIntroSlide2Folder1Name.rememberLocalizedString() else Localization.Key.AppIntroSlide3Folder2Name.rememberLocalizedString(),
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(15.dp),
                            color = if (pagerState.currentPage == it) primaryContentColor else MaterialTheme.colorScheme.onSurface.copy(
                                0.70f
                            )
                        )
                    }
                }
            }
        }
        HorizontalPager(
            state = pagerState, modifier = Modifier.fillMaxWidth().animateContentSize()
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                when (it) {
                    0 -> {
                        FolderComponent(
                            FolderComponentParam(
                                name = Localization.Key.AppIntroSlide3Folder2_1Name.rememberLocalizedString(),
                                note = Localization.Key.AppIntroSlide3Folder2_1Note.rememberLocalizedString(),
                                onClick = {},
                                onLongClick = {},
                                onMoreIconClick = {},
                                isCurrentlyInDetailsView = rememberSaveable {
                                    mutableStateOf(false)
                                },
                                showMoreIcon = rememberSaveable {
                                    mutableStateOf(true)
                                },
                                isSelectedForSelection = rememberSaveable {
                                    mutableStateOf(false)
                                },
                                showCheckBox = rememberSaveable {
                                    mutableStateOf(false)
                                },
                                onCheckBoxChanged = {})
                        )
                        ListViewLinkComponent(
                            linkComponentParam = LinkComponentParam(
                                link = Link(
                            title = "Synchronization in Linkora • Saketh Pathike",
                            baseURL = "sakethpathike.github.io",
                            imgURL = "https://sakethpathike.github.io/images/ogImage-synchronization-in-linkora.png",
                            url = "https://sakethpathike.github.io/blog/synchronization-in-linkora",
                            userAgent = AppPreferences.primaryJsoupUserAgent.value,
                            linkType = LinkType.SAVED_LINK,
                            localId = 0L,
                            note = "",
                            idOfLinkedFolder = null
                        ),
                            onMoreIconClick = { -> },
                            onLinkClick = { ->
                                localUriHandler.openUri("https://sakethpathike.github.io/blog/synchronization-in-linkora")
                            },
                            onForceOpenInExternalBrowserClicked = { -> },
                            isSelectionModeEnabled = rememberSaveable { mutableStateOf(false) },
                            isItemSelected = rememberSaveable { mutableStateOf(false) },
                            onLongClick = { -> },
                            tags = listOf(Tag(name = "Linkora")),
                            onTagClick = {}), titleOnlyView = false, onShare = {
                            LinkoraSDK.getInstance().nativeUtils.onShare(it)
                        })
                    }

                    1 -> {
                        FolderComponent(
                            FolderComponentParam(
                                name = Localization.Key.AppIntroSlide3Folder3_1Name.rememberLocalizedString(),
                                note = Localization.Key.AppIntroSlide3Folder3_1Note.rememberLocalizedString(),
                                onClick = {},
                                onLongClick = {},
                                onMoreIconClick = {},
                                isCurrentlyInDetailsView = rememberSaveable {
                                    mutableStateOf(false)
                                },
                                showMoreIcon = rememberSaveable {
                                    mutableStateOf(true)
                                },
                                isSelectedForSelection = rememberSaveable {
                                    mutableStateOf(false)
                                },
                                showCheckBox = rememberSaveable {
                                    mutableStateOf(false)
                                },
                                onCheckBoxChanged = {})
                        )
                        ListViewLinkComponent(
                            linkComponentParam = LinkComponentParam(
                                link = Link(
                                title = "LinkoraApp/sync-server: self-hostable sync-server for Linkora with browser extension support.",
                                baseURL = "github.com",
                                imgURL = "https://opengraph.githubassets.com/45fc9e2969396c9f27f7af994014d3a75ff93899d98ef2f6c5504fef71edd9cf/LinkoraApp/sync-server",
                                url = "https://github.com/LinkoraApp/sync-server",
                                userAgent = AppPreferences.primaryJsoupUserAgent.value,
                                linkType = LinkType.SAVED_LINK,
                                localId = 0L,
                                note = "",
                                idOfLinkedFolder = null
                            ),
                                onMoreIconClick = { -> },
                                onLinkClick = { ->
                                    localUriHandler.openUri("https://github.com/LinkoraApp/sync-server")
                                },
                                onForceOpenInExternalBrowserClicked = { -> },
                                isSelectionModeEnabled = rememberSaveable { mutableStateOf(false) },
                                isItemSelected = rememberSaveable {
                                    mutableStateOf(false)
                                },
                                onLongClick = { -> },
                                tags = listOf(Tag(name = "Linkora")),
                                onTagClick = {}),
                            titleOnlyView = false,
                            imageAlignment = Alignment.TopCenter,
                            onShare = {
                                LinkoraSDK.getInstance().nativeUtils.onShare(it)
                            })
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        SlideTitle(
            string = Localization.Key.AppIntroSlide3MainLabel.rememberLocalizedString(),
            modifier = Modifier.fillMaxWidth().padding(start = 15.dp)
        )
        Spacer(modifier = Modifier.height(5.dp))
        SlideDesc(
            modifier = Modifier.fillMaxWidth().padding(start = 15.dp, end = 15.dp),
            string = Localization.Key.AppIntroSlide3MainLabelDesc.rememberLocalizedString()
        )
        Spacer(modifier = Modifier.height(75.dp))
    }
}

@Composable
fun Slide4() {
    Column(
        modifier = Modifier.padding(start = 15.dp, end = 15.dp, bottom = 75.dp).fillMaxSize(),
        verticalArrangement = Arrangement.Bottom
    ) {
        SlideTitle(string = Localization.Key.AppIntroSlide4Label1.rememberLocalizedString())
        Spacer(Modifier.height(5.dp))
        listOf(
            Localization.Key.AppIntroSlide4Label1Desc1.getLocalizedString(),
            Localization.Key.AppIntroSlide4Label1Desc2.getLocalizedString(),
            Localization.Key.AppIntroSlide4Label1Desc3.getLocalizedString(),
            Localization.Key.AppIntroSlide4Label1Desc4.getLocalizedString(),
            Localization.Key.AppIntroSlide4Label1Desc5.getLocalizedString(),
            Localization.Key.AppIntroSlide4Label1Desc6.getLocalizedString(),
            Localization.Key.AppIntroSlide4Label1Desc7.getLocalizedString(),
            Localization.Key.AppIntroSlide4Label1Desc8.getLocalizedString(),
            Localization.Key.AppIntroSlide4Label1Desc9.getLocalizedString(),
            Localization.Key.AppIntroSlide4Label1Desc10.getLocalizedString()
        ).forEach {
            key(it) {
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(text = Typography.bullet.toString())
                    Spacer(modifier = Modifier.width(5.dp))
                    SlideDesc(
                        string = it
                    )
                }
            }
        }
        ItemDivider(
            paddingValues = PaddingValues(top = 15.dp, bottom = 15.dp),
            colorOpacity = 0.95f,
            thickness = 2.dp
        )
        SlideTitle(string = Localization.Key.AppIntroSlide4Label2.rememberLocalizedString())
        Spacer(Modifier.height(5.dp))
        SlideDesc(string = Localization.Key.AppIntroSlide4Label2Desc.rememberLocalizedString())
    }
}