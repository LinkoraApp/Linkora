package com.sakethh.linkora.ui.screens.home

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults.primaryContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sakethh.linkora.Localization
import com.sakethh.linkora.di.HomeScreenVMAssistedFactory
import com.sakethh.linkora.di.linkoraViewModel
import com.sakethh.linkora.domain.asMenuBtmSheetType
import com.sakethh.linkora.platform.PlatformSpecificBackHandler
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.components.CollectionLayoutManager
import com.sakethh.linkora.ui.components.SortingIconButton
import com.sakethh.linkora.ui.components.menu.MenuBtmSheetType
import com.sakethh.linkora.ui.domain.CurrentFABContext
import com.sakethh.linkora.ui.domain.ScreenType
import com.sakethh.linkora.ui.domain.model.CollectionDetailPaneInfo
import com.sakethh.linkora.ui.domain.model.CollectionType
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.DataEmptyScreen
import com.sakethh.linkora.ui.screens.LoadingScreen
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.ui.utils.pressScaleEffect
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.rememberLocalizedString
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(currentFABContext: (CurrentFABContext) -> Unit) {
    LaunchedEffect(Unit) {
        currentFABContext(CurrentFABContext.ROOT)
    }
    val navController = LocalNavController.current
    val homeScreenVM: HomeScreenVM =
        linkoraViewModel(factory = HomeScreenVMAssistedFactory.createForHomeScreen())
    val shouldPanelsBtmSheetBeVisible = rememberSaveable {
        mutableStateOf(false)
    }
    val panelsBtmSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val panels = homeScreenVM.existingPanels.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val activePanelAssociatedPanelFolders =
        homeScreenVM.activePanelAssociatedPanelFolders.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = {
        activePanelAssociatedPanelFolders.value.size
    })
    val localUriHandler = LocalUriHandler.current
    val panelFoldersDataFlat by homeScreenVM.panelFoldersDataFlat.collectAsStateWithLifecycle()
    Scaffold(topBar = {
        Column(
            modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).animateContentSize()
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(5.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = homeScreenVM.currentPhaseOfTheDay.value,
                    color = MaterialTheme.colorScheme.secondary,
                    style = MaterialTheme.typography.titleMedium,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 5.dp)
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        navController.navigate(Navigation.Home.PanelsManagerScreen)
                    }, modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)) {
                        Icon(imageVector = Icons.Default.Tune, contentDescription = null)
                    }
                    SortingIconButton()
                }
            }
            if (homeScreenVM.selectedPanelData != null) {
                Text(
                    text = Localization.Key.SelectedPanel.rememberLocalizedString(),
                    color = MaterialTheme.colorScheme.primary.copy(0.9f),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(start = 10.dp, bottom = 5.dp)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                        .clickable(onClick = {
                            shouldPanelsBtmSheetBeVisible.value = true
                            coroutineScope.launch {
                                panelsBtmSheetState.show()
                            }
                        }, indication = null, interactionSource = remember {
                            MutableInteractionSource()
                        }).pressScaleEffect().pointerHoverIcon(icon = PointerIcon.Hand)
                        .fillMaxWidth()
                        .padding(start = 5.dp, end = 5.dp),
                ) {
                    Spacer(Modifier.width(5.dp))
                    FilledTonalIconButton(onClick = {
                        shouldPanelsBtmSheetBeVisible.value = true
                        coroutineScope.launch {
                            panelsBtmSheetState.show()
                        }
                    }, modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand).size(22.dp)) {
                        Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null)
                    }
                    Spacer(Modifier.width(10.dp))
                    Text(
                        text = homeScreenVM.selectedPanelData!!.panelName,
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.titleLarge,
                        fontSize = 20.sp,
                        modifier = Modifier.fillMaxWidth(0.8f)
                    )
                }
            }
        }
    }) { paddingValues ->
        if (activePanelAssociatedPanelFolders.value.isEmpty() && homeScreenVM.selectedPanelData == null) {
            LoadingScreen(paddingValues = PaddingValues(25.dp))
            return@Scaffold
        }
        Column(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            if (activePanelAssociatedPanelFolders.value.isEmpty() && homeScreenVM.selectedPanelData != null) {
                DataEmptyScreen(text = Localization.Key.NoFoldersInThePanel.rememberLocalizedString())
                return@Scaffold
            }
            ScrollableTabRow(
                modifier = Modifier.fillMaxWidth(),
                selectedTabIndex = pagerState.currentPage,
                divider = {}) {
                activePanelAssociatedPanelFolders.value.forEachIndexed { index, panelFolder ->
                    Tab(selected = pagerState.currentPage == index, onClick = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(index)
                        }.start()
                    }, modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)) {
                        Text(
                            text = panelFolder.folderName,
                            style = MaterialTheme.typography.titleLarge,
                            fontSize = 18.sp,
                            modifier = Modifier.padding(15.dp),
                            color = if (pagerState.currentPage == index) primaryContentColor else MaterialTheme.colorScheme.onSurface.copy(
                                0.70f
                            )
                        )
                    }
                }
            }
            HorizontalDivider()
            HorizontalPager(state = pagerState) { pageIndex ->
                val panelFolderId =
                    retain(activePanelAssociatedPanelFolders.value[pageIndex].folderId) {
                        activePanelAssociatedPanelFolders.value[pageIndex].folderId
                    }
                CollectionLayoutManager(
                    screenType = ScreenType.FOLDERS_AND_LINKS,
                    flatChildFolderDataState = panelFoldersDataFlat[panelFolderId],
                    linksTagsPairsState = null,
                    paddingValues = PaddingValues(0.dp),
                    folderMoreIconClick = {
                        coroutineScope.pushUIEvent(
                            UIEvent.Type.ShowMenuBtmSheet(
                                menuBtmSheetFor = MenuBtmSheetType.Folder.RegularFolder,
                                selectedLinkForMenuBtmSheet = null,
                                selectedFolderForMenuBtmSheet = it
                            )
                        )
                    },
                    onFolderClick = { folder ->
                        val collectionDetailPaneInfo = CollectionDetailPaneInfo(
                            currentFolder = folder,
                            currentTag = null,
                            collectionType = CollectionType.FOLDER,
                        )
                        navController.currentBackStackEntry?.savedStateHandle?.set(
                            key = Constants.COLLECTION_INFO_SAVED_STATE_HANDLE_KEY,
                            value = Json.encodeToString(collectionDetailPaneInfo)
                        )
                        navController.navigate(Navigation.Collection.MobileCollectionDetailScreen)
                    },
                    linkMoreIconClick = {
                        coroutineScope.pushUIEvent(
                            UIEvent.Type.ShowMenuBtmSheet(
                                menuBtmSheetFor = it.link.linkType.asMenuBtmSheetType(),
                                selectedLinkForMenuBtmSheet = it,
                                selectedFolderForMenuBtmSheet = null
                            )
                        )
                    },
                    onLinkClick = {
                        homeScreenVM.addLinkToHistory(
                            link = it.link,
                            selectedTags = it.tags
                        )
                        localUriHandler.openUri(it.link.url)
                    },
                    isCurrentlyInDetailsView = {
                        false
                    },
                    nestedScrollConnection = null,
                    emptyDataText = if (activePanelAssociatedPanelFolders.value.map { it.folderId }
                            .contains(Constants.SAVED_LINKS_ID)) Localization.Key.NoLinksFound.rememberLocalizedString() else "",
                    onAttachedTagClick = {
                        val collectionDetailPaneInfo = CollectionDetailPaneInfo(
                            currentFolder = null,
                            currentTag = it,
                            collectionType = CollectionType.TAG,
                        )
                        navController.currentBackStackEntry?.savedStateHandle?.set(
                            key = Constants.COLLECTION_INFO_SAVED_STATE_HANDLE_KEY,
                            value = Json.encodeToString(
                                collectionDetailPaneInfo
                            )
                        )
                        navController.navigate(
                            Navigation.Collection.MobileCollectionDetailScreen
                        )
                    },
                    onTagClick = {},
                    tagMoreIconClick = {},
                    onRetrieveNextPage = {
                        homeScreenVM.retrieveNextPage(folderId = panelFolderId)
                    },
                    onFirstVisibleItemIndexChange = {
                        homeScreenVM.onFirstVisibleItemIndexChange(
                            folderId = panelFolderId,
                            itemIndex = it
                        )
                    },
                    flatSearchResultState = null
                )
            }
        }
    }
    if (shouldPanelsBtmSheetBeVisible.value) {
        ModalBottomSheet(onDismissRequest = {
            shouldPanelsBtmSheetBeVisible.value = false
            coroutineScope.launch {
                panelsBtmSheetState.hide()
            }
        }, sheetState = panelsBtmSheetState) {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                item {
                    Text(
                        text = Localization.Key.SelectAPanel.rememberLocalizedString(),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(15.dp)
                    )
                }
                items(panels.value) { panel ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable {
                            homeScreenVM.selectedPanelData = panel
                            homeScreenVM.updatePanelFolders(homeScreenVM.selectedPanelData!!)
                            coroutineScope.launch {
                                panelsBtmSheetState.hide()
                            }.invokeOnCompletion {
                                shouldPanelsBtmSheetBeVisible.value = false
                            }
                        }.pointerHoverIcon(icon = PointerIcon.Hand).padding(5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = homeScreenVM.selectedPanelData!!.localId == panel.localId,
                            onClick = {
                                homeScreenVM.selectedPanelData = panel
                                homeScreenVM.updatePanelFolders(homeScreenVM.selectedPanelData!!)
                                coroutineScope.launch {
                                    panelsBtmSheetState.hide()
                                }.invokeOnCompletion {
                                    shouldPanelsBtmSheetBeVisible.value = false
                                }
                            },
                            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand)
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = panel.panelName,
                            style = if (homeScreenVM.selectedPanelData!!.localId == panel.localId) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleSmall,
                            color = if (homeScreenVM.selectedPanelData!!.localId == panel.localId) LocalContentColor.current else LocalContentColor.current.copy(
                                0.85f
                            )
                        )
                    }
                }
            }
        }
    }
    PlatformSpecificBackHandler {
        navController.navigateUp()
    }
}