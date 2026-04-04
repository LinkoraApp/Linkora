package com.sakethh.linkora.ui.screens.collections

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.ContainedLoadingIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MediumTopAppBar
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults.primaryContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerIcon
import androidx.compose.ui.input.pointer.pointerHoverIcon
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hasRoute
import com.composables.core.ScrollArea
import com.composables.core.rememberScrollAreaState
import com.sakethh.linkora.Localization
import com.sakethh.linkora.di.CollectionDetailPaneVMFactory
import com.sakethh.linkora.domain.LinkSaveConfig
import com.sakethh.linkora.domain.LinkType
import com.sakethh.linkora.domain.Platform
import com.sakethh.linkora.domain.asLocalizedString
import com.sakethh.linkora.domain.asMenuBtmSheetType
import com.sakethh.linkora.domain.asUnifiedLazyState
import com.sakethh.linkora.ui.LocalNavController
import com.sakethh.linkora.ui.components.CollectionLayoutManager
import com.sakethh.linkora.ui.components.PerformAtTheEndOfTheList
import com.sakethh.linkora.ui.components.SortingIconButton
import com.sakethh.linkora.ui.components.folder.FolderComponent
import com.sakethh.linkora.ui.components.menu.MenuBtmSheetType
import com.sakethh.linkora.ui.domain.CurrentFABContext
import com.sakethh.linkora.ui.domain.FABContext
import com.sakethh.linkora.ui.domain.ScreenType
import com.sakethh.linkora.ui.domain.model.CollectionDetailPaneInfo
import com.sakethh.linkora.ui.domain.model.CollectionType
import com.sakethh.linkora.ui.domain.model.FolderComponentParam
import com.sakethh.linkora.ui.navigation.Navigation
import com.sakethh.linkora.ui.screens.DataEmptyScreen
import com.sakethh.linkora.ui.screens.search.FilterChip
import com.sakethh.linkora.ui.utils.UIEvent
import com.sakethh.linkora.ui.utils.UIEvent.pushUIEvent
import com.sakethh.linkora.utils.Constants
import com.sakethh.linkora.utils.Utils
import com.sakethh.linkora.utils.VerticalScrollbar
import com.sakethh.linkora.utils.addEdgeToEdgeScaffoldPadding
import com.sakethh.linkora.utils.getLocalizedString
import com.sakethh.linkora.utils.rememberLocalizedString
import com.sakethh.linkora.utils.supportsWideDisplay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

@Composable
fun CollectionDetailScreen(
    collectionDetailPaneInfo: CollectionDetailPaneInfo,
    currentFABContext: (CurrentFABContext) -> Unit
) {
    val navController = LocalNavController.current
    CollectionDetailPane(
        currentFABContext = currentFABContext,
        onNavigate = { collectionDetailPaneInfo ->
            navController.navigate(
                Navigation.Collection.CollectionDetailScreen(
                    Utils.json.encodeToString(collectionDetailPaneInfo)
                )
            )
        },
        collectionDetailPaneInfo = collectionDetailPaneInfo,
        navigateUp = navController::navigateUp,
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun CollectionDetailPane(
    currentFABContext: (CurrentFABContext) -> Unit,
    onNavigate: (CollectionDetailPaneInfo) -> Unit,
    collectionDetailPaneInfo: CollectionDetailPaneInfo,
    navigateUp: () -> Unit
) {
    val collectionDetailPaneVM: CollectionDetailPaneVM =
        viewModel(factory = CollectionDetailPaneVMFactory.create(collectionDetailPaneInfo))
    val preferences by collectionDetailPaneVM.preferencesAsFlow.collectAsStateWithLifecycle()
    val linkTagsPairs by collectionDetailPaneVM.linkTagsPairsState.collectAsStateWithLifecycle()
    val coroutineScope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val rootArchiveFoldersState by collectionDetailPaneVM.rootArchiveFolders.collectAsStateWithLifecycle()
    val rootArchiveFoldersListState = rememberLazyListState()
    val rootArchiveFoldersUnifiedListState = retain {
        rootArchiveFoldersListState.asUnifiedLazyState()
    }
    val localUriHandler = LocalUriHandler.current
    val topAppBarScrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val flatChildFolderDataState =
        collectionDetailPaneVM.childFoldersFlat.collectAsStateWithLifecycle().value
    PerformAtTheEndOfTheList(
        unifiedLazyState = rootArchiveFoldersUnifiedListState, actionOnReachingEnd = {
            collectionDetailPaneVM.performAction(CollectionPaneAction.RetrieveNextRootArchivedFolderPage)
        })
    val collectionDetailPaneInfo = collectionDetailPaneVM.collectionDetailPaneInfo
    val onAndroidMobile = Platform.Android.onMobile() || !supportsWideDisplay()
    val navController = LocalNavController.current
    DisposableEffect(Unit) {
        onDispose {
            if (onAndroidMobile && navController.currentBackStackEntry?.destination?.hasRoute<Navigation.Root.CollectionsScreen>() == true) {
                currentFABContext(CurrentFABContext.ROOT)
            }
        }
    }

    LaunchedEffect(collectionDetailPaneInfo.currentFolder) {
        if (collectionDetailPaneInfo.currentTag != null || (collectionDetailPaneInfo.currentFolder != null && (collectionDetailPaneInfo.currentFolder.localId == Constants.ALL_LINKS_ID || collectionDetailPaneInfo.currentFolder.localId >= 0))) {
            currentFABContext(
                CurrentFABContext(
                    fabContext = FABContext.REGULAR,
                    currentFolder = collectionDetailPaneInfo.currentFolder
                )
            )
            return@LaunchedEffect
        }
        if (collectionDetailPaneInfo.currentFolder != null && (collectionDetailPaneInfo.currentFolder.localId == Constants.SAVED_LINKS_ID || collectionDetailPaneInfo.currentFolder.localId == Constants.IMPORTANT_LINKS_ID)) {
            currentFABContext(
                CurrentFABContext(
                    fabContext = FABContext.ADD_LINK_IN_FOLDER,
                    currentFolder = collectionDetailPaneInfo.currentFolder
                )
            )
            return@LaunchedEffect
        }

        // for archive:
        currentFABContext(CurrentFABContext(FABContext.HIDE))
    }

    Scaffold(modifier = Modifier.fillMaxSize(), topBar = {
        Column {
            MediumTopAppBar(scrollBehavior = topAppBarScrollBehavior, actions = {
                SortingIconButton()
            }, navigationIcon = {
                IconButton(
                    modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand), onClick = {
                        navigateUp()
                    }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null
                    )
                }
            }, title = {
                val isTag = collectionDetailPaneInfo.collectionType == CollectionType.TAG
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (isTag) {
                        Icon(imageVector = Icons.Default.Tag, contentDescription = null)
                        Spacer(modifier = Modifier.width(5.dp))
                    }
                    Text(
                        text = if (isTag) collectionDetailPaneInfo.currentTag?.name
                            ?: "" else collectionDetailPaneInfo.currentFolder?.name ?: "",
                        color = MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.titleMedium,
                        fontSize = 18.sp
                    )
                }
            })
            if (!onAndroidMobile) {
                HorizontalDivider()
            }
        }
    }) { paddingValues ->
        if (collectionDetailPaneInfo.currentFolder?.localId == Constants.ARCHIVE_ID) {
            Column(modifier = Modifier.addEdgeToEdgeScaffoldPadding(paddingValues).fillMaxSize()) {
                TabRow(selectedTabIndex = pagerState.currentPage) {
                    listOf(
                        Localization.Key.Links.rememberLocalizedString(),
                        Localization.Key.Folders.rememberLocalizedString()
                    ).forEachIndexed { index, screenName ->
                        Tab(
                            modifier = Modifier.pointerHoverIcon(icon = PointerIcon.Hand),
                            selected = pagerState.currentPage == index,
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(index)
                                }.start()
                            }) {
                            Text(
                                text = screenName,
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
                HorizontalPager(state = pagerState) { pageIndex ->
                    when (pageIndex) {
                        0 -> {
                            CollectionLayoutManager(
                                screenType = ScreenType.LINKS_ONLY,
                                flatChildFolderDataState = null,
                                linksTagsPairsState = linkTagsPairs,
                                paddingValues = PaddingValues(0.dp),
                                linkMoreIconClick = {
                                    coroutineScope.pushUIEvent(
                                        UIEvent.Type.ShowMenuBtmSheet(
                                            menuBtmSheetFor = it.link.linkType.asMenuBtmSheetType(),
                                            selectedLinkForMenuBtmSheet = it,
                                            selectedFolderForMenuBtmSheet = null
                                        )
                                    )
                                },
                                folderMoreIconClick = {},
                                onFolderClick = {},
                                onLinkClick = {
                                    collectionDetailPaneVM.performAction(
                                        CollectionPaneAction.AddANewLink(
                                            link = it.link.copy(
                                                linkType = LinkType.HISTORY_LINK, localId = 0
                                            ),
                                            linkSaveConfig = LinkSaveConfig(
                                                forceAutoDetectTitle = false,
                                                forceSaveWithoutRetrievingData = true,
                                                useProxy = preferences.useProxy,
                                                skipSavingIfExists = preferences.skipSavingExistingLink,
                                                forceSaveIfRetrievalFails = preferences.forceSaveIfRetrievalFails,
                                            ),
                                            onCompletion = {},
                                            pushSnackbarOnSuccess = false,
                                            selectedTags = it.tags
                                        )
                                    )
                                    localUriHandler.openUri(it.link.url)
                                },
                                isCurrentlyInDetailsView = {
                                    collectionDetailPaneInfo.currentFolder.localId == it.localId
                                },
                                emptyDataText = Localization.Key.NoArchiveLinksFound.getLocalizedString(),
                                nestedScrollConnection = topAppBarScrollBehavior.nestedScrollConnection,
                                onAttachedTagClick = {
                                    if (collectionDetailPaneInfo.currentTag?.localId == it.localId) {
                                        return@CollectionLayoutManager
                                    }
                                    val collectionDetailPaneInfo = CollectionDetailPaneInfo(
                                        currentFolder = null,
                                        currentTag = it,
                                        collectionType = CollectionType.TAG,
                                    )
                                    onNavigate(collectionDetailPaneInfo)
                                },
                                tagMoreIconClick = {},
                                onTagClick = {},
                                onRetrieveNextPage = {
                                    collectionDetailPaneVM.performAction(CollectionPaneAction.RetrieveNextLinksPage)
                                },
                                onFirstVisibleItemIndexChange = {
                                    collectionDetailPaneVM.performAction(
                                        CollectionPaneAction.OnFirstVisibleItemIndexChangeOfLinkTagsPair(
                                            it
                                        )
                                    )
                                },
                                flatSearchResultState = null,
                                preferences = preferences
                            )
                        }

                        1 -> {
                            val state =
                                rememberScrollAreaState(lazyListState = rootArchiveFoldersListState)
                            ScrollArea(state = state) {
                                LazyColumn(
                                    state = rootArchiveFoldersListState,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    if (!rootArchiveFoldersState.isRetrieving && (rootArchiveFoldersState.data.isEmpty() || rootArchiveFoldersState.data.values.first()
                                            .isEmpty())
                                    ) {
                                        item {
                                            DataEmptyScreen(text = Localization.Key.NoFoldersFoundInArchive.getLocalizedString())
                                        }
                                        return@LazyColumn
                                    }
                                    rootArchiveFoldersState.data.forEach { (pageKey, rootArchiveFolders) ->
                                        items(rootArchiveFolders, key = {
                                            "LazyColumn-rootArchiveFolders-P$pageKey" + it.localId
                                        }) { rootArchiveFolder ->
                                            FolderComponent(
                                                FolderComponentParam(
                                                    name = rootArchiveFolder.name,
                                                    note = rootArchiveFolder.note,
                                                    onClick = {
                                                        if (CollectionsScreenVM.selectedFoldersViaLongClick.contains(
                                                                rootArchiveFolder
                                                            )
                                                        ) {
                                                            return@FolderComponentParam
                                                        }
                                                        val collectionDetailPaneInfo =
                                                            CollectionDetailPaneInfo(
                                                                currentFolder = rootArchiveFolder,
                                                                collectionType = CollectionType.FOLDER,
                                                                currentTag = null
                                                            )
                                                        onNavigate(collectionDetailPaneInfo)
                                                    },
                                                    onLongClick = {
                                                        if (CollectionsScreenVM.isSelectionEnabled.value.not()) {
                                                            CollectionsScreenVM.isSelectionEnabled.value =
                                                                true
                                                            CollectionsScreenVM.selectedFoldersViaLongClick.add(
                                                                rootArchiveFolder
                                                            )
                                                        }
                                                    },
                                                    onMoreIconClick = {
                                                        coroutineScope.pushUIEvent(
                                                            UIEvent.Type.ShowMenuBtmSheet(
                                                                menuBtmSheetFor = MenuBtmSheetType.Folder.RegularFolder,
                                                                selectedLinkForMenuBtmSheet = null,
                                                                selectedFolderForMenuBtmSheet = rootArchiveFolder
                                                            )
                                                        )
                                                    },
                                                    isCurrentlyInDetailsView = remember(
                                                        collectionDetailPaneInfo.currentFolder?.localId
                                                    ) {
                                                        mutableStateOf(collectionDetailPaneInfo.currentFolder?.localId == rootArchiveFolder.localId)
                                                    },
                                                    showMoreIcon = rememberSaveable {
                                                        mutableStateOf(true)
                                                    },
                                                    isSelectedForSelection = rememberSaveable(
                                                        CollectionsScreenVM.isSelectionEnabled.value,
                                                        CollectionsScreenVM.selectedFoldersViaLongClick.contains(
                                                            rootArchiveFolder
                                                        )
                                                    ) {
                                                        mutableStateOf(
                                                            CollectionsScreenVM.isSelectionEnabled.value && CollectionsScreenVM.selectedFoldersViaLongClick.contains(
                                                                rootArchiveFolder
                                                            )
                                                        )
                                                    },
                                                    showCheckBox = CollectionsScreenVM.isSelectionEnabled,
                                                    onCheckBoxChanged = { bool ->
                                                        if (bool) {
                                                            CollectionsScreenVM.selectedFoldersViaLongClick.add(
                                                                rootArchiveFolder
                                                            )
                                                        } else {
                                                            CollectionsScreenVM.selectedFoldersViaLongClick.remove(
                                                                rootArchiveFolder
                                                            )
                                                        }
                                                    },
                                                    path = null,
                                                    showPath = false,
                                                    onPathItemClick = {},
                                                )
                                            )
                                        }
                                    }
                                    if (!rootArchiveFoldersState.pagesCompleted) {
                                        item {
                                            Box(
                                                modifier = Modifier.fillMaxWidth().padding(15.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                ContainedLoadingIndicator()
                                            }
                                        }
                                    }
                                }
                                VerticalScrollbar()
                            }
                            LaunchedEffect(Unit) {
                                snapshotFlow {
                                    rootArchiveFoldersListState.firstVisibleItemIndex
                                }.debounce(500).distinctUntilChanged().collectLatest {
                                    collectionDetailPaneVM.performAction(
                                        CollectionPaneAction.OnFirstVisibleItemIndexChangeOfRootArchivedFolders(
                                            it.toLong()
                                        )
                                    )
                                }
                            }
                        }
                    }
                }
            }
            return@Scaffold
        }
        Column(modifier = Modifier.addEdgeToEdgeScaffoldPadding(paddingValues).fillMaxSize()) {
            if (collectionDetailPaneInfo.currentFolder?.localId == Constants.ALL_LINKS_ID) {
                Row(Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())) {
                    LinkType.entries.forEach {
                        key(it.name) {
                            FilterChip(
                                text = it.asLocalizedString(),
                                isSelected = collectionDetailPaneVM.appliedFiltersForAllLinks.contains(
                                    it
                                ),
                                onClick = {
                                    collectionDetailPaneVM.performAction(
                                        CollectionPaneAction.ToggleAllLinksFilter(
                                            filter = it
                                        )
                                    )
                                })
                        }
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                }
            }
            CollectionLayoutManager(
                screenType = if (collectionDetailPaneInfo.currentFolder?.localId != null && collectionDetailPaneInfo.currentFolder.localId >= 0) ScreenType.FOLDERS_AND_LINKS else ScreenType.LINKS_ONLY,
                flatChildFolderDataState = flatChildFolderDataState,
                linksTagsPairsState = linkTagsPairs,
                paddingValues = PaddingValues(0.dp),
                linkMoreIconClick = {
                    coroutineScope.pushUIEvent(
                        UIEvent.Type.ShowMenuBtmSheet(
                            menuBtmSheetFor = it.link.linkType.asMenuBtmSheetType(),
                            selectedLinkForMenuBtmSheet = it,
                            selectedFolderForMenuBtmSheet = null
                        )
                    )
                },
                folderMoreIconClick = {
                    coroutineScope.pushUIEvent(
                        UIEvent.Type.ShowMenuBtmSheet(
                            menuBtmSheetFor = MenuBtmSheetType.Folder.RegularFolder,
                            selectedLinkForMenuBtmSheet = null,
                            selectedFolderForMenuBtmSheet = it
                        )
                    )
                },
                onFolderClick = {
                    val collectionDetailPaneInfo = CollectionDetailPaneInfo(
                        currentFolder = it,
                        collectionType = CollectionType.FOLDER,
                        currentTag = null
                    )
                    onNavigate(collectionDetailPaneInfo)
                },
                onLinkClick = {
                    collectionDetailPaneVM.performAction(
                        CollectionPaneAction.AddANewLink(
                            link = it.link.copy(linkType = LinkType.HISTORY_LINK, localId = 0),
                            linkSaveConfig = LinkSaveConfig(
                                forceAutoDetectTitle = false, forceSaveWithoutRetrievingData = true,
                                useProxy = preferences.useProxy,
                                skipSavingIfExists = preferences.skipSavingExistingLink,
                                forceSaveIfRetrievalFails = preferences.forceSaveIfRetrievalFails
                            ),
                            onCompletion = {},
                            pushSnackbarOnSuccess = false,
                            selectedTags = it.tags
                        )
                    )
                    localUriHandler.openUri(it.link.url)
                },
                isCurrentlyInDetailsView = {
                    collectionDetailPaneInfo.currentFolder?.localId == it.localId
                },
                nestedScrollConnection = topAppBarScrollBehavior.nestedScrollConnection,
                emptyDataText = if (collectionDetailPaneInfo.currentTag != null) Localization.Key.NoAttachmentsToTags.rememberLocalizedString() else Localization.Key.NoLinksFound.rememberLocalizedString(),
                onAttachedTagClick = {
                    if (collectionDetailPaneInfo.currentTag?.localId == it.localId) {
                        return@CollectionLayoutManager
                    }
                    val collectionDetailPaneInfo = CollectionDetailPaneInfo(
                        currentFolder = null,
                        currentTag = it,
                        collectionType = CollectionType.TAG,
                    )
                    onNavigate(collectionDetailPaneInfo)
                },
                tagMoreIconClick = {},
                onTagClick = {},
                onRetrieveNextPage = {
                    collectionDetailPaneVM.performAction(CollectionPaneAction.RetrieveNextLinksPage)
                },
                onFirstVisibleItemIndexChange = {
                    collectionDetailPaneVM.performAction(
                        CollectionPaneAction.OnFirstVisibleItemIndexChangeOfLinkTagsPair(
                            it
                        )
                    )
                },
                flatSearchResultState = null,
                preferences = preferences
            )
        }
    }
}